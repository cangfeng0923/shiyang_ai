package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shiyangai.entity.IngredientInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class OnDemandFoodService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NutritionApiService nutritionApiService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OnDemandFoodService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 用户查询时实时获取（首次查询慢，后续缓存）
     */
    public IngredientInfo getFoodInfo(String foodName) {
        // 1. 先查缓存
        Object cached = redisTemplate.opsForValue().get("food:" + foodName);
        if (cached instanceof IngredientInfo) {
            return (IngredientInfo) cached;
        }

        // 2. 缓存未命中，实时爬取
        IngredientInfo info = crawlAndAggregate(foodName);

        // 3. 存入缓存
        if (info != null) {
            redisTemplate.opsForValue().set("food:" + foodName, info, 30, TimeUnit.DAYS);
        }

        return info;
    }

    /**
     * 多源聚合（提高准确性）
     */
    private IngredientInfo crawlAndAggregate(String foodName) {
        List<IngredientInfo> sources = new ArrayList<>();

        // 并行爬取多个来源
        CompletableFuture<IngredientInfo> usda = CompletableFuture.supplyAsync(
                () -> getFromUSDA(foodName));
        CompletableFuture<IngredientInfo> baike = CompletableFuture.supplyAsync(
                () -> getFromBaiduBaike(foodName));
        CompletableFuture<IngredientInfo> tcm = CompletableFuture.supplyAsync(
                () -> getFromTCMWebsite(foodName));

        CompletableFuture.allOf(usda, baike, tcm).join();

        // 收集非空结果
        if (usda.join() != null) sources.add(usda.join());
        if (baike.join() != null) sources.add(baike.join());
        if (tcm.join() != null) sources.add(tcm.join());

        // 投票机制：多数来源一致的结果为准
        return aggregateResults(foodName, sources);
    }

    /**
     * 从USDA获取营养数据
     */
    private IngredientInfo getFromUSDA(String foodName) {
        try {
            Map<String, Object> nutrition = nutritionApiService.getNutrition(foodName);
            if (nutrition != null && !nutrition.containsKey("error")) {
                IngredientInfo info = new IngredientInfo();
                info.setName(foodName);
                info.setCalories(parseCalories(nutrition.get("calories")));
                info.setProtein(parseDouble(nutrition.get("protein")));
                info.setCarbs(parseDouble(nutrition.get("carbs")));
                info.setFat(parseDouble(nutrition.get("fat")));
                info.setSource("USDA");
                info.setConfidence(90);
                return info;
            }
        } catch (Exception e) {
            System.err.println("USDA获取失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 从百度百科获取信息
     */
    private IngredientInfo getFromBaiduBaike(String foodName) {
        try {
            String url = "https://baike.baidu.com/item/" + java.net.URLEncoder.encode(foodName, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Document doc = Jsoup.parse(response.body());
                String summary = doc.select(".lemma-summary").text();

                IngredientInfo info = new IngredientInfo();
                info.setName(foodName);
                info.setProperty(extractProperty(summary));
                info.setFlavor(extractFlavor(summary));
                info.setEffect(extractEffect(summary));
                info.setContraindication(extractContraindication(summary));
                info.setSource("百度百科");
                info.setConfidence(85);
                return info;
            }
        } catch (Exception e) {
            System.err.println("百度百科获取失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 从中医网站获取信息
     */
    private IngredientInfo getFromTCMWebsite(String foodName) {
        try {
            // 使用中医食疗网站
            String url = "https://www.zyys.com/shipu/" + java.net.URLEncoder.encode(foodName, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Document doc = Jsoup.parse(response.body());
                String content = doc.select(".content, .article").text();

                IngredientInfo info = new IngredientInfo();
                info.setName(foodName);
                info.setProperty(extractProperty(content));
                info.setFlavor(extractFlavor(content));
                info.setEffect(extractEffect(content));
                info.setSource("中医食疗网");
                info.setConfidence(80);
                return info;
            }
        } catch (Exception e) {
            System.err.println("中医网站获取失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 聚合多个来源的结果（投票机制）
     */
    private IngredientInfo aggregateResults(String foodName, List<IngredientInfo> sources) {
        if (sources.isEmpty()) {
            return createDefaultInfo(foodName);
        }

        IngredientInfo result = new IngredientInfo();
        result.setName(foodName);

        // 投票统计
        Map<String, Integer> propertyVotes = new HashMap<>();
        Map<String, Integer> flavorVotes = new HashMap<>();
        Map<String, Integer> effectVotes = new HashMap<>();

        for (IngredientInfo source : sources) {
            if (source == null) continue;

            if (source.getProperty() != null) {
                propertyVotes.merge(source.getProperty(), source.getConfidence(), Integer::sum);
            }
            if (source.getFlavor() != null) {
                flavorVotes.merge(source.getFlavor(), source.getConfidence(), Integer::sum);
            }
            if (source.getEffect() != null && source.getEffect().length() > 0) {
                effectVotes.merge(source.getEffect(), source.getConfidence(), Integer::sum);
            }
        }

        // 取票数最高的
        result.setProperty(propertyVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("平"));

        result.setFlavor(flavorVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("甘"));

        result.setEffect(effectVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("补益身体"));

        // 从第一个有效来源获取详细数据
        for (IngredientInfo source : sources) {
            if (source != null) {
                if (result.getCalories() == null) result.setCalories(source.getCalories());
                if (result.getProtein() == null) result.setProtein(source.getProtein());
                if (result.getCarbs() == null) result.setCarbs(source.getCarbs());
                if (result.getFat() == null) result.setFat(source.getFat());
                if (result.getContraindication() == null) result.setContraindication(source.getContraindication());
                break;
            }
        }

        result.setSource("多源聚合");
        result.setConfidence(calculateConfidence(sources));
        result.setUpdateTime(System.currentTimeMillis());

        return result;
    }

    /**
     * 创建默认信息
     */
    private IngredientInfo createDefaultInfo(String foodName) {
        IngredientInfo info = new IngredientInfo();
        info.setName(foodName);
        info.setProperty("平");
        info.setFlavor("甘");
        info.setEffect("信息待补充");
        info.setContraindication("暂无特殊禁忌");
        info.setSource("默认数据");
        info.setConfidence(50);
        info.setUpdateTime(System.currentTimeMillis());
        return info;
    }

    /**
     * 计算综合置信度
     */
    private int calculateConfidence(List<IngredientInfo> sources) {
        int validSources = (int) sources.stream().filter(s -> s != null).count();
        return Math.min(100, validSources * 30 + 10);
    }

    // ========== 辅助方法 ==========

    private Integer parseCalories(Object value) {
        if (value == null) return null;
        String str = value.toString();
        try {
            return Integer.parseInt(str.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(Object value) {
        if (value == null) return null;
        String str = value.toString();
        try {
            return Double.parseDouble(str.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractProperty(String text) {
        if (text == null) return "平";
        if (text.contains("性寒") || text.contains("性大寒")) return "寒";
        if (text.contains("性凉") || text.contains("微寒")) return "凉";
        if (text.contains("性温") || text.contains("微温")) return "温";
        if (text.contains("性热") || text.contains("大热")) return "热";
        return "平";
    }

    private String extractFlavor(String text) {
        if (text == null) return "甘";
        if (text.contains("味酸") || text.contains("酸味")) return "酸";
        if (text.contains("味苦") || text.contains("苦味")) return "苦";
        if (text.contains("味甘") || text.contains("甘味")) return "甘";
        if (text.contains("味辛") || text.contains("辛味")) return "辛";
        if (text.contains("味咸") || text.contains("咸味")) return "咸";
        return "甘";
    }

    private String extractEffect(String text) {
        if (text == null) return "补益身体";
        // 常见功效关键词
        if (text.contains("清热")) return "清热";
        if (text.contains("补气")) return "补气";
        if (text.contains("滋阴")) return "滋阴";
        if (text.contains("祛湿")) return "祛湿";
        if (text.contains("健脾")) return "健脾";
        if (text.contains("养胃")) return "养胃";
        return "补益身体";
    }

    private String extractContraindication(String text) {
        if (text == null) return "无明显禁忌";
        if (text.contains("忌")) {
            int idx = text.indexOf("忌");
            int end = Math.min(idx + 30, text.length());
            return text.substring(idx, end);
        }
        return "无明显禁忌";
    }
}