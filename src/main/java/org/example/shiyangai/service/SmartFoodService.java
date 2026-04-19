package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

//智能食材服务（带缓存）
@Service
public class SmartFoodService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NutritionApiService nutritionApiService;


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    // 本地缓存（最快）
    private final Map<String, IngredientInfo> localCache = new ConcurrentHashMap<>();

    public SmartFoodService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(10);
    }

    /**
     * 获取食材信息（多层缓存）
     */
    public IngredientInfo getFoodInfo(String foodName) {
        // L1: 本地缓存
        if (localCache.containsKey(foodName)) {
            return localCache.get(foodName);
        }

        // L2: Redis缓存
        String redisKey = "food:" + foodName;
        IngredientInfo cached = (IngredientInfo) redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            localCache.put(foodName, cached);
            return cached;
        }

        // L3: 实时获取（异步）
        IngredientInfo info = fetchFromMultipleSources(foodName);

        // 存入缓存
        if (info != null && info.getConfidence() > 60) {
            redisTemplate.opsForValue().set(redisKey, info, Duration.ofDays(30));
            localCache.put(foodName, info);
        }

        return info;
    }

    /**
     * 批量获取食材（并行）
     */
    public Map<String, IngredientInfo> batchGetFoodInfo(List<String> foodNames) {
        Map<String, IngredientInfo> results = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String foodName : foodNames) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                results.put(foodName, getFoodInfo(foodName));
            }, executorService);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return results;
    }

    /**
     * 多源获取食材信息
     */
    private IngredientInfo fetchFromMultipleSources(String foodName) {
        // 并行查询多个来源
        CompletableFuture<IngredientInfo> usdaFuture = CompletableFuture.supplyAsync(
                () -> fetchFromUSDA(foodName), executorService);
        CompletableFuture<IngredientInfo> baikeFuture = CompletableFuture.supplyAsync(
                () -> fetchFromBaiduBaike(foodName), executorService);
        CompletableFuture<IngredientInfo> tcmFuture = CompletableFuture.supplyAsync(
                () -> fetchFromTCMWebsite(foodName), executorService);

        try {
            CompletableFuture.allOf(usdaFuture, baikeFuture, tcmFuture)
                    .get(10, TimeUnit.SECONDS);

            IngredientInfo usda = usdaFuture.getNow(null);
            IngredientInfo baike = baikeFuture.getNow(null);
            IngredientInfo tcm = tcmFuture.getNow(null);

            // 聚合结果（投票机制）
            return aggregateResults(foodName, usda, baike, tcm);

        } catch (Exception e) {
            // 降级：使用AI推断
            return inferByAI(foodName);
        }
    }

    /**
     * 从USDA获取营养数据
     */
    private IngredientInfo fetchFromUSDA(String foodName) {
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
            // 忽略
        }
        return null;
    }

    /**
     * 从百度百科获取中医属性
     */
    private IngredientInfo fetchFromBaiduBaike(String foodName) {
        try {
            String url = "https://baike.baidu.com/item/" + foodName;
            Document doc = Jsoup.connect(url).timeout(5000).get();

            IngredientInfo info = new IngredientInfo();
            info.setName(foodName);

            // 解析性味
            String content = doc.select(".lemma-summary").text();
            info.setProperty(extractProperty(content));
            info.setFlavor(extractFlavor(content));
            info.setEffect(extractEffect(content));
            info.setContraindication(extractContraindication(content));

            info.setSource("百度百科");
            info.setConfidence(85);
            return info;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从中医网站获取信息
     */
    private IngredientInfo fetchFromTCMWebsite(String foodName) {
        // 类似百度百科的逻辑
        return null;
    }

    /**
     * AI推断未知食材
     */
    private IngredientInfo inferByAI(String foodName) {
        try {
            String prompt = String.format("""
                请根据中医理论推断"%s"的以下属性，只返回JSON：
                {
                  "property": "寒/凉/平/温/热",
                  "flavor": "酸/苦/甘/辛/咸",
                  "meridian": "心/肝/脾/肺/肾",
                  "effect": "主要功效",
                  "contraindication": "禁忌人群"
                }
                """, foodName);

            String result = callLLMForInference(prompt);

            IngredientInfo info = objectMapper.readValue(result, IngredientInfo.class);
            info.setName(foodName);
            info.setSource("AI推断");
            info.setConfidence(60);
            return info;

        } catch (Exception e) {
            IngredientInfo info = new IngredientInfo();
            info.setName(foodName);
            info.setProperty("平");
            info.setFlavor("甘");
            info.setConfidence(30);
            return info;
        }
    }

    /**
     * 聚合多个来源的结果（投票机制）
     */
    private IngredientInfo aggregateResults(String foodName,
                                            IngredientInfo... sources) {
        IngredientInfo result = new IngredientInfo();
        result.setName(foodName);

        // 投票统计
        Map<String, Integer> propertyVotes = new HashMap<>();
        Map<String, Integer> flavorVotes = new HashMap<>();

        for (IngredientInfo source : sources) {
            if (source == null) continue;

            if (source.getProperty() != null) {
                propertyVotes.merge(source.getProperty(), source.getConfidence(), Integer::sum);
            }
            if (source.getFlavor() != null) {
                flavorVotes.merge(source.getFlavor(), source.getConfidence(), Integer::sum);
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

        // 从第一个有效来源获取详细数据
        for (IngredientInfo source : sources) {
            if (source != null) {
                if (result.getEffect() == null) result.setEffect(source.getEffect());
                if (result.getCalories() == null) result.setCalories(source.getCalories());
                if (result.getProtein() == null) result.setProtein(source.getProtein());
                break;
            }
        }

        result.setConfidence(calculateConfidence(sources));
        result.setUpdateTime(System.currentTimeMillis());

        return result;
    }

    private int calculateConfidence(IngredientInfo[] sources) {
        int validSources = (int) Arrays.stream(sources).filter(Objects::nonNull).count();
        return Math.min(100, validSources * 30 + 10);
    }

    // 辅助方法
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
        if (text.contains("性寒")) return "寒";
        if (text.contains("性凉")) return "凉";
        if (text.contains("性温")) return "温";
        if (text.contains("性热")) return "热";
        return "平";
    }

    private String extractFlavor(String text) {
        if (text.contains("味酸") || text.contains("酸味")) return "酸";
        if (text.contains("味苦") || text.contains("苦味")) return "苦";
        if (text.contains("味甘") || text.contains("甘味")) return "甘";
        if (text.contains("味辛") || text.contains("辛味")) return "辛";
        if (text.contains("味咸") || text.contains("咸味")) return "咸";
        return "甘";
    }

    private String extractEffect(String text) {
        // 简化提取
        if (text.contains("清热")) return "清热";
        if (text.contains("补气")) return "补气";
        if (text.contains("滋阴")) return "滋阴";
        if (text.contains("祛湿")) return "祛湿";
        return "调理";
    }

    private String extractContraindication(String text) {
        if (text.contains("忌")) {
            int idx = text.indexOf("忌");
            int end = Math.min(idx + 30, text.length());
            return text.substring(idx, end);
        }
        return "无明显禁忌";
    }

    private String callLLMForInference(String prompt) {
        // 调用 DeepSeek API
        // ... 已有代码
        return "{\"property\":\"平\",\"flavor\":\"甘\"}";
    }
}