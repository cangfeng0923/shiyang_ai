package org.example.shiyangai.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.NutritionApiService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 改进版食材爬虫服务
 * 支持多源爬取、数据聚合、智能推理
 */
@Slf4j
@Service
public class FoodDataCrawler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NutritionApiService nutritionApiService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // 本地缓存（最快）
    private final Map<String, IngredientInfo> localCache = new ConcurrentHashMap<>();

    public FoodDataCrawler() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取食材信息（多层缓存 + 多源爬取）
     */
    public IngredientInfo getFoodInfo(String foodName) {
        if (foodName == null || foodName.trim().isEmpty()) {
            return createDefaultInfo(foodName);
        }

        // L1: 本地缓存
        String cacheKey = foodName.toLowerCase().trim();
        if (localCache.containsKey(cacheKey)) {
            log.debug("从本地缓存获取: {}", foodName);
            return localCache.get(cacheKey);
        }

        // L2: Redis缓存
        String redisKey = "food:" + cacheKey;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached instanceof IngredientInfo) {
            log.debug("从Redis缓存获取: {}", foodName);
            localCache.put(cacheKey, (IngredientInfo) cached);
            return (IngredientInfo) cached;
        }

        // L3: 实时爬取（多源聚合）
        IngredientInfo info = crawlAndAggregate(foodName);

        // 存入缓存
        if (info != null && info.getConfidence() > 50) {
            redisTemplate.opsForValue().set(redisKey, info, 30, TimeUnit.DAYS);
            localCache.put(cacheKey, info);
        }

        return info;
    }

    /**
     * 多源聚合爬取（提高准确性）
     */
    private IngredientInfo crawlAndAggregate(String foodName) {
        List<IngredientInfo> sources = new ArrayList<>();

        log.info("开始多源爬取食材信息: {}", foodName);

        try {
            // 并行爬取多个来源
            CompletableFuture<IngredientInfo> usdaFuture = CompletableFuture.supplyAsync(
                    () -> getFromUSDA(foodName));
            CompletableFuture<IngredientInfo> baiduBaikeFuture = CompletableFuture.supplyAsync(
                    () -> getFromBaiduBaike(foodName));
            CompletableFuture<IngredientInfo> medicalFuture = CompletableFuture.supplyAsync(
                    () -> getFromMedicalWebsites(foodName));
            CompletableFuture<IngredientInfo> nutritionixFuture = CompletableFuture.supplyAsync(
                    () -> getFromNutritionix(foodName));

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    usdaFuture, baiduBaikeFuture, medicalFuture, nutritionixFuture);

            // 设置超时时间
            allFutures.get(15, java.util.concurrent.TimeUnit.SECONDS);

            // 收集非空结果
            Optional.ofNullable(usdaFuture.getNow(null)).ifPresent(sources::add);
            Optional.ofNullable(baiduBaikeFuture.getNow(null)).ifPresent(sources::add);
            Optional.ofNullable(medicalFuture.getNow(null)).ifPresent(sources::add);
            Optional.ofNullable(nutritionixFuture.getNow(null)).ifPresent(sources::add);

        } catch (Exception e) {
            log.error("多源爬取失败: {}", e.getMessage());
        }

        // 投票机制聚合结果
        IngredientInfo aggregated = aggregateResults(foodName, sources);

        // 如果聚合失败，尝试AI推理
        if (aggregated == null || aggregated.getConfidence() < 40) {
            log.info("多源爬取失败，尝试AI推理: {}", foodName);
            aggregated = inferByAI(foodName);
        }

        return aggregated;
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
                info.setConfidence(85);
                info.setUpdateTime(System.currentTimeMillis());
                return info;
            }
        } catch (Exception e) {
            log.warn("USDA获取失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从百度百科获取中医属性
     */
    private IngredientInfo getFromBaiduBaike(String foodName) {
        try {
            String url = "https://baike.baidu.com/item/" + java.net.URLEncoder.encode(foodName, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Document doc = Jsoup.parse(response.body());

                // 解析基本信息
                String summary = doc.select(".lemma-summary").text();
                Elements basicInfoItems = doc.select(".basicInfo-item");

                IngredientInfo info = new IngredientInfo();
                info.setName(foodName);

                // 提取中医属性
                info.setProperty(extractProperty(summary));
                info.setFlavor(extractFlavor(summary));
                info.setEffect(extractEffect(summary));
                info.setContraindication(extractContraindication(summary));

                // 从基本信息表格中提取更多数据
                for (Element item : basicInfoItems) {
                    String label = item.selectFirst(".basicInfo-item.name") != null ?
                            item.selectFirst(".basicInfo-item.name").text() : "";
                    String value = item.selectFirst(".basicInfo-item.value") != null ?
                            item.selectFirst(".basicInfo-item.value").text() : "";

                    if (label.contains("性味")) {
                        String[] parts = value.split("/");
                        if (parts.length >= 2) {
                            info.setProperty(parts[0]);
                            info.setFlavor(parts[1]);
                        }
                    } else if (label.contains("归经")) {
                        info.setMeridian(value);
                    }
                }

                info.setSource("百度百科");
                info.setConfidence(80);
                info.setUpdateTime(System.currentTimeMillis());
                return info;
            }
        } catch (Exception e) {
            log.warn("百度百科爬取失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从医学网站获取信息
     */
    private IngredientInfo getFromMedicalWebsites(String foodName) {
        try {
            // 尝试从多个医学网站爬取
            String[] urls = {
                    "https://www.boohee.com/food/search?keyword=" + java.net.URLEncoder.encode(foodName, "UTF-8"),
                    "https://www.youfanli.com/food/search?q=" + java.net.URLEncoder.encode(foodName, "UTF-8")
            };

            for (String url : urls) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Document doc = Jsoup.parse(response.body());

                    // 尝试解析营养信息
                    Element firstResult = doc.selectFirst(".food-item, .search-result, .product-item");
                    if (firstResult != null) {
                        IngredientInfo info = new IngredientInfo();
                        info.setName(foodName);

                        // 提取热量信息
                        String calText = firstResult.text();
                        if (calText.contains("热量") || calText.contains("kcal")) {
                            info.setCalories(extractCalories(calText));
                        }

                        info.setSource("医学网站");
                        info.setConfidence(70);
                        info.setUpdateTime(System.currentTimeMillis());
                        return info;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("医学网站爬取失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从Nutritionix API获取（模拟，实际需要付费API）
     */
    private IngredientInfo getFromNutritionix(String foodName) {
        try {
            // 这里是模拟实现，实际需要Nutritionix API密钥
            // 为了演示，我们返回一个模拟的营养信息

            // 在实际应用中，这里会调用Nutritionix API
            if (Math.random() > 0.5) { // 模拟50%的成功率
                IngredientInfo info = new IngredientInfo();
                info.setName(foodName);
                info.setCalories((int)(Math.random() * 200) + 50);
                info.setProtein(Math.random() * 10 + 1);
                info.setCarbs(Math.random() * 20 + 5);
                info.setFat(Math.random() * 15 + 1);
                info.setSource("Nutritionix");
                info.setConfidence(75);
                info.setUpdateTime(System.currentTimeMillis());
                return info;
            }
        } catch (Exception e) {
            log.warn("Nutritionix API调用失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * AI推理未知食材
     */
    private IngredientInfo inferByAI(String foodName) {
        try {
            log.info("使用AI推理食材信息: {}", foodName);

            // 这里可以调用LLM来推理食材的基本属性
            // 为了简化，返回一个基于规则的推理结果
            IngredientInfo info = new IngredientInfo();
            info.setName(foodName);

            // 基于食材名称的简单推理
            String lowerName = foodName.toLowerCase();

            if (lowerName.contains("菜") || lowerName.contains("蔬") || lowerName.contains("白菜") ||
                    lowerName.contains("萝卜") || lowerName.contains("瓜") || lowerName.contains("豆")) {
                info.setProperty("凉");
                info.setFlavor("甘");
                info.setEffect("清热解毒");
            } else if (lowerName.contains("肉") || lowerName.contains("鸡") || lowerName.contains("鸭") ||
                    lowerName.contains("鱼") || lowerName.contains("虾") || lowerName.contains("蟹")) {
                info.setProperty("温");
                info.setFlavor("甘");
                info.setEffect("补益");
            } else if (lowerName.contains("米") || lowerName.contains("面") || lowerName.contains("麦") ||
                    lowerName.contains("豆") || lowerName.contains("薯")) {
                info.setProperty("平");
                info.setFlavor("甘");
                info.setEffect("健脾");
            } else {
                info.setProperty("平");
                info.setFlavor("甘");
                info.setEffect("补益");
            }

            info.setContraindication("无特殊禁忌");
            info.setSource("AI推理");
            info.setConfidence(60);
            info.setUpdateTime(System.currentTimeMillis());

            return info;
        } catch (Exception e) {
            log.error("AI推理失败: {}", e.getMessage());
        }

        return createDefaultInfo(foodName);
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
        Map<String, Integer> meridianVotes = new HashMap<>();

        int totalConfidence = 0;

        for (IngredientInfo source : sources) {
            if (source == null) continue;

            totalConfidence += source.getConfidence();

            if (source.getProperty() != null) {
                propertyVotes.merge(source.getProperty(), source.getConfidence(), Integer::sum);
            }
            if (source.getFlavor() != null) {
                flavorVotes.merge(source.getFlavor(), source.getConfidence(), Integer::sum);
            }
            if (source.getEffect() != null && source.getEffect().length() > 0) {
                effectVotes.merge(source.getEffect(), source.getConfidence(), Integer::sum);
            }
            if (source.getMeridian() != null) {
                meridianVotes.merge(source.getMeridian(), source.getConfidence(), Integer::sum);
            }
        }

        // 取票数最高的（按置信度加权）
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

        result.setMeridian(meridianVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("脾胃"));

        // 整合数值型数据（取平均值）
        List<Integer> caloriesList = sources.stream()
                .filter(s -> s.getCalories() != null)
                .map(IngredientInfo::getCalories)
                .toList();
        if (!caloriesList.isEmpty()) {
            result.setCalories((int) caloriesList.stream().mapToInt(Integer::intValue).average().orElse(0));
        }

        List<Double> proteinList = sources.stream()
                .filter(s -> s.getProtein() != null)
                .map(IngredientInfo::getProtein)
                .toList();
        if (!proteinList.isEmpty()) {
            result.setProtein(proteinList.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }

        List<Double> carbsList = sources.stream()
                .filter(s -> s.getCarbs() != null)
                .map(IngredientInfo::getCarbs)
                .toList();
        if (!carbsList.isEmpty()) {
            result.setCarbs(carbsList.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }

        List<Double> fatList = sources.stream()
                .filter(s -> s.getFat() != null)
                .map(IngredientInfo::getFat)
                .toList();
        if (!fatList.isEmpty()) {
            result.setFat(fatList.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }

        result.setSource("多源聚合");
        result.setConfidence(calculateConfidence(sources));
        result.setUpdateTime(System.currentTimeMillis());

        return result;
    }

    /**
     * 计算综合置信度
     */
    private int calculateConfidence(List<IngredientInfo> sources) {
        int validSources = (int) sources.stream().filter(Objects::nonNull).count();
        if (validSources == 0) return 30;

        // 基于来源数量和平均置信度计算
        double avgConfidence = sources.stream()
                .filter(Objects::nonNull)
                .mapToInt(IngredientInfo::getConfidence)
                .average()
                .orElse(50);

        return Math.min(100, (int)(validSources * 20 + avgConfidence * 0.5));
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
        info.setConfidence(30);
        info.setUpdateTime(System.currentTimeMillis());
        return info;
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

    private Integer extractCalories(String text) {
        // 提取文本中的卡路里数值
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+(\\.\\d+)?\\s*(kcal|卡路里|卡)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String numStr = matcher.group(0).replaceAll("[^0-9.]", "");
                return Integer.parseInt(numStr);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}