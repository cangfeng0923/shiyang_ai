package org.example.shiyangai.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.NutritionApiService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 改进版食材爬虫服务
 * 支持多源爬取、数据聚合、智能推理、定时任务
 */
@Slf4j
@Component
public class FoodDataCrawler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NutritionApiService nutritionApiService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    // 本地缓存（最快）
    private final Map<String, IngredientInfo> localCache = new ConcurrentHashMap<>();

    public FoodDataCrawler() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newFixedThreadPool(10);
    }

    /**
     * 定时任务：每天凌晨2点爬取热门食材
     */
    @Scheduled(cron = "0 16 20 * * ?")
    public void crawlDailyFoods() {
        log.info("开始执行定时爬取任务...");
        List<String> hotFoods = getHotFoodsList();

        for (String food : hotFoods) {
            try {
                // 强制刷新缓存
                String cacheKey = "food:" + food.toLowerCase().trim();
                redisTemplate.delete(cacheKey);
                localCache.remove(food.toLowerCase().trim());

                IngredientInfo info = getFoodInfo(food);
                if (info != null && info.getConfidence() > 50) {
                    log.info("定时爬取成功: {}, 置信度: {}", food, info.getConfidence());
                }
                Thread.sleep(1000); // 避免请求过快
            } catch (Exception e) {
                log.error("定时爬取失败: {}", food, e);
            }
        }
        log.info("定时爬取任务完成");
    }

    private List<String> getHotFoodsList() {
        return Arrays.asList(
                "山药", "红枣", "枸杞", "薏米", "红豆", "绿豆",
                "黑芝麻", "百合", "银耳", "莲子", "生姜", "大蒜",
                "韭菜", "冬瓜", "苦瓜", "黄瓜", "萝卜", "西红柿",
                "苹果", "梨", "香蕉", "西瓜", "葡萄",
                "鸡肉", "鸭肉", "猪肉", "牛肉", "羊肉",
                "鸡蛋", "牛奶", "豆腐", "豆浆", "蜂蜜"
        );
    }

    /**
     * 获取食材信息（多层缓存 + 多源爬取）
     */
    public IngredientInfo getFoodInfo(String foodName) {
        if (foodName == null || foodName.trim().isEmpty()) {
            return createDefaultInfo(foodName);
        }

        String cacheKey = foodName.toLowerCase().trim();

        // L1: 本地缓存
        if (localCache.containsKey(cacheKey)) {
            log.debug("从本地缓存获取: {}", foodName);
            return localCache.get(cacheKey);
        }

        // L2: Redis缓存
        String redisKey = "food:" + cacheKey;
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof IngredientInfo) {
                log.debug("从Redis缓存获取: {}", foodName);
                localCache.put(cacheKey, (IngredientInfo) cached);
                return (IngredientInfo) cached;
            }
        } catch (Exception e) {
            log.warn("Redis读取失败: {}", e.getMessage());
        }

        // L3: 实时爬取（多源聚合）
        IngredientInfo info = crawlAndAggregate(foodName);

        // 存入缓存
        if (info != null && info.getConfidence() > 50) {
            try {
                redisTemplate.opsForValue().set(redisKey, info, 30, TimeUnit.DAYS);
            } catch (Exception e) {
                log.warn("Redis写入失败: {}", e.getMessage());
            }
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
                    () -> retryOnFailure(() -> getFromUSDA(foodName), 2), executor);
            CompletableFuture<IngredientInfo> baiduBaikeFuture = CompletableFuture.supplyAsync(
                    () -> retryOnFailure(() -> getFromBaiduBaike(foodName), 2), executor);
            CompletableFuture<IngredientInfo> medicalFuture = CompletableFuture.supplyAsync(
                    () -> retryOnFailure(() -> getFromMedicalWebsites(foodName), 2), executor);

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    usdaFuture, baiduBaikeFuture, medicalFuture);

            allFutures.get(15, TimeUnit.SECONDS);

            Optional.ofNullable(usdaFuture.getNow(null)).ifPresent(sources::add);
            Optional.ofNullable(baiduBaikeFuture.getNow(null)).ifPresent(sources::add);
            Optional.ofNullable(medicalFuture.getNow(null)).ifPresent(sources::add);

        } catch (Exception e) {
            log.error("多源爬取失败: {}", e.getMessage());
        }

        IngredientInfo aggregated = aggregateResults(foodName, sources);

        if (aggregated == null || aggregated.getConfidence() < 40) {
            log.info("多源爬取失败，使用AI推理: {}", foodName);
            aggregated = inferByAI(foodName);
        }

        return aggregated;
    }

    /**
     * 重试机制
     */
    private <T> T retryOnFailure(Supplier<T> supplier, int maxRetries) {
        for (int i = 0; i <= maxRetries; i++) {
            try {
                T result = supplier.get();
                if (result != null) return result;
            } catch (Exception e) {
                log.warn("第{}次尝试失败: {}", i + 1, e.getMessage());
                if (i < maxRetries) {
                    try { Thread.sleep(1000 * (i + 1)); } catch (InterruptedException ignored) {}
                }
            }
        }
        return null;
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
     * 从百度百科获取中医属性（修正版）
     */
    /**
     * 从百度百科获取中医属性（增强版）
     */
    private IngredientInfo getFromBaiduBaike(String foodName) {
        try {
            String encodedName = java.net.URLEncoder.encode(foodName, "UTF-8");
            // 使用百度百科的移动版，反爬较弱
            String url = "https://baike.baidu.com/item/" + encodedName;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Document doc = Jsoup.parse(response.body());

                // 尝试多种选择器获取摘要
                String summary = "";
                Element summaryElem = doc.selectFirst(".lemma-summary, .basic-info, .lemmaWgt-lemmaSummary");
                if (summaryElem != null) {
                    summary = summaryElem.text();
                } else {
                    // 获取正文前200字作为摘要
                    Element contentElem = doc.selectFirst(".lemma-content, .para, .lemma-text");
                    if (contentElem != null) {
                        summary = contentElem.text();
                        if (summary.length() > 200) summary = summary.substring(0, 200);
                    }
                }

                if (summary.isEmpty()) {
                    log.warn("百度百科未找到摘要: {}", foodName);
                    return null;
                }

                IngredientInfo info = new IngredientInfo();
                info.setName(foodName);

                // 更精确的性味提取
                String property = extractPropertyAdvanced(summary);
                String flavor = extractFlavorAdvanced(summary);
                String effect = extractEffectAdvanced(summary);
                String contraindication = extractContraindicationAdvanced(summary);

                info.setProperty(property);
                info.setFlavor(flavor);
                info.setEffect(effect);
                info.setContraindication(contraindication);
                info.setSource("百度百科");
                info.setConfidence(85);
                info.setUpdateTime(System.currentTimeMillis());

                log.info("百度百科解析成功: {} -> 性:{}, 味:{}, 功效:{}", foodName, property, flavor, effect);
                return info;
            } else {
                log.warn("百度百科请求失败: status={}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("百度百科爬取异常: {}", e.getMessage());
        }
        return null;
    }

    // 增强的性味提取
    private String extractPropertyAdvanced(String text) {
        if (text == null) return "平";
        // 更精确的匹配
        if (text.contains("性寒") || text.contains("性大寒") || text.contains("性微寒")) return "寒";
        if (text.contains("性凉") || text.contains("性微凉")) return "凉";
        if (text.contains("性温") || text.contains("性微温") || text.contains("性大温")) return "温";
        if (text.contains("性热") || text.contains("性大热")) return "热";
        if (text.contains("性平")) return "平";

        // 尝试匹配"【性味】"格式
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("[性味|性]\\s*[：:]\\s*([寒凉平温热])");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);

        return "平";
    }

    private String extractFlavorAdvanced(String text) {
        if (text == null) return "甘";
        if (text.contains("味酸") || text.contains("酸味")) return "酸";
        if (text.contains("味苦") || text.contains("苦味")) return "苦";
        if (text.contains("味甘") || text.contains("甘味")) return "甘";
        if (text.contains("味辛") || text.contains("辛味")) return "辛";
        if (text.contains("味咸") || text.contains("咸味")) return "咸";

        // 尝试匹配"【性味】"格式
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("[性味]\\s*[：:]\\s*[寒凉平温热，、]*([酸甜苦辣咸甘辛])");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);

        return "甘";
    }

    private String extractEffectAdvanced(String text) {
        if (text == null) return "补益身体";

        // 常见功效关键词映射
        String[] effectKeywords = {
                "清热解毒", "清热", "降火", "祛湿", "利尿", "消肿",
                "补气", "补血", "滋阴", "壮阳", "温补",
                "健脾", "养胃", "助消化", "开胃",
                "润肺", "止咳", "化痰",
                "安神", "助眠", "养心",
                "活血", "化瘀", "通络"
        };

        for (String keyword : effectKeywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }

        return "补益身体";
    }

    private String extractContraindicationAdvanced(String text) {
        if (text == null) return "无明显禁忌";

        // 查找禁忌相关描述
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("([^。]*?(忌|不宜|禁食|慎食)[^。]*。)");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            String result = m.group(1);
            if (result.length() > 50) result = result.substring(0, 50);
            return result;
        }

        return "无明显禁忌";
    }
    /**
     * 从医学网站获取信息
     */
    private IngredientInfo getFromMedicalWebsites(String foodName) {
        try {
            String url = "https://www.boohee.com/food/search?keyword=" +
                    java.net.URLEncoder.encode(foodName, "UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Document doc = Jsoup.parse(response.body());
                Element firstResult = doc.selectFirst(".food-item, .search-result");

                if (firstResult != null) {
                    IngredientInfo info = new IngredientInfo();
                    info.setName(foodName);
                    String text = firstResult.text();
                    Integer calories = extractCalories(text);
                    if (calories != null) {
                        info.setCalories(calories);
                    }
                    info.setSource("医学网站");
                    info.setConfidence(65);
                    info.setUpdateTime(System.currentTimeMillis());
                    return info;
                }
            }
        } catch (Exception e) {
            log.warn("医学网站爬取失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * AI推理未知食材
     */
    private IngredientInfo inferByAI(String foodName) {
        log.info("使用AI推理食材信息: {}", foodName);

        IngredientInfo info = new IngredientInfo();
        info.setName(foodName);
        info.setSource("AI推理");
        info.setConfidence(55);
        info.setUpdateTime(System.currentTimeMillis());

        String lowerName = foodName.toLowerCase();

        // 基于名称的推理规则
        if (lowerName.contains("菜") || lowerName.contains("蔬") ||
                lowerName.contains("瓜") || lowerName.contains("豆")) {
            info.setProperty("凉");
            info.setFlavor("甘");
            info.setEffect("清热解毒");
        } else if (lowerName.contains("肉") || lowerName.contains("鸡") ||
                lowerName.contains("鱼") || lowerName.contains("虾")) {
            info.setProperty("温");
            info.setFlavor("甘");
            info.setEffect("补益气血");
        } else if (lowerName.contains("米") || lowerName.contains("面") ||
                lowerName.contains("麦") || lowerName.contains("薯")) {
            info.setProperty("平");
            info.setFlavor("甘");
            info.setEffect("健脾养胃");
        } else if (lowerName.contains("果") || lowerName.contains("梨") ||
                lowerName.contains("桃") || lowerName.contains("莓")) {
            info.setProperty("平");
            info.setFlavor("甘酸");
            info.setEffect("生津止渴");
        } else {
            info.setProperty("平");
            info.setFlavor("甘");
            info.setEffect("补益身体");
        }

        info.setContraindication("无特殊禁忌");

        return info;
    }

    /**
     * 聚合多个来源的结果
     */
    private IngredientInfo aggregateResults(String foodName, List<IngredientInfo> sources) {
        if (sources.isEmpty()) {
            return createDefaultInfo(foodName);
        }

        IngredientInfo result = new IngredientInfo();
        result.setName(foodName);

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
            if (source.getEffect() != null && !source.getEffect().isEmpty()) {
                effectVotes.merge(source.getEffect(), source.getConfidence(), Integer::sum);
            }
        }

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

        result.setSource("多源聚合");
        result.setConfidence(calculateConfidence(sources));
        result.setUpdateTime(System.currentTimeMillis());

        return result;
    }

    private int calculateConfidence(List<IngredientInfo> sources) {
        int validSources = (int) sources.stream().filter(Objects::nonNull).count();
        double avgConfidence = sources.stream()
                .filter(Objects::nonNull)
                .mapToInt(IngredientInfo::getConfidence)
                .average()
                .orElse(50);
        return Math.min(100, (int)(validSources * 15 + avgConfidence * 0.5));
    }

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
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+(\\.\\d+)?\\s*(kcal|卡路里|卡)");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String numStr = matcher.group(0).replaceAll("[^0-9.]", "");
                return (int) Double.parseDouble(numStr);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}