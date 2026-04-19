// service/SmartRecipeGenerator.java - 完整版（替换原有内容）
package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmartRecipeGenerator {

    @Autowired
    private SmartFoodService foodService;

    @Autowired
    private OnDemandFoodService onDemandFoodService;

    @Value("${deepseek.api.key}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SmartRecipeGenerator() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 基于食材库生成食谱
     */
    public String generateRecipe(String constitution, List<String> preferences, String mealType) {
        log.info("生成食谱: constitution={}, mealType={}, preferences={}", constitution, mealType, preferences);

        // 1. 获取候选食材（根据体质筛选）
        List<String> candidateFoods = getCandidateFoods(constitution, preferences);

        // 2. 批量获取食材详情
        Map<String, IngredientInfo> foodDetails = foodService.batchGetFoodInfo(candidateFoods);

        // 3. 按适宜性排序
        List<Map.Entry<String, IngredientInfo>> sorted = foodDetails.entrySet().stream()
                .sorted((a, b) -> Integer.compare(
                        getSuitabilityScore(b.getValue(), constitution),
                        getSuitabilityScore(a.getValue(), constitution)))
                .limit(20)
                .collect(Collectors.toList());

        // 4. 调用AI生成食谱
        return generateWithAI(constitution, sorted, mealType);
    }

    private int getSuitabilityScore(IngredientInfo info, String constitution) {
        if (info == null || info.getProperty() == null) return 0;

        Map<String, Map<String, Integer>> scoreMap = Map.of(
                "寒", Map.of("阳虚质", -10, "阴虚质", 5, "湿热质", 5, "气虚质", -5),
                "凉", Map.of("阳虚质", -5, "阴虚质", 3, "湿热质", 3, "气虚质", -3),
                "平", Map.of("阳虚质", 0, "阴虚质", 0, "湿热质", 0, "气虚质", 5),
                "温", Map.of("阳虚质", 10, "阴虚质", -5, "湿热质", -3, "气虚质", 8),
                "热", Map.of("阳虚质", 15, "阴虚质", -10, "湿热质", -8, "气虚质", 5)
        );

        Map<String, Integer> scores = scoreMap.getOrDefault(info.getProperty(), Map.of());
        return scores.getOrDefault(constitution, 0);
    }

    private List<String> getCandidateFoods(String constitution, List<String> preferences) {
        Set<String> candidates = new HashSet<>();

        if (preferences != null && !preferences.isEmpty()) {
            candidates.addAll(preferences);
        }

        candidates.addAll(getSeasonalFoods());
        candidates.addAll(getCommonFoodsByConstitution(constitution));

        // 移除空值
        candidates.removeIf(String::isEmpty);

        return new ArrayList<>(candidates);
    }

    private List<String> getSeasonalFoods() {
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (month >= 3 && month <= 5) {
            return Arrays.asList("春笋", "荠菜", "菠菜", "韭菜", "豆芽");
        } else if (month >= 6 && month <= 8) {
            return Arrays.asList("冬瓜", "苦瓜", "绿豆", "西瓜", "黄瓜", "薏米");
        } else if (month >= 9 && month <= 11) {
            return Arrays.asList("山药", "百合", "银耳", "梨", "莲藕", "白萝卜");
        } else {
            return Arrays.asList("羊肉", "红枣", "桂圆", "生姜", "萝卜", "核桃");
        }
    }

    private List<String> getCommonFoodsByConstitution(String constitution) {
        Map<String, List<String>> foodsByConstitution = new HashMap<>();
        foodsByConstitution.put("气虚质", Arrays.asList("山药", "红枣", "黄芪", "小米", "鸡肉", "蜂蜜"));
        foodsByConstitution.put("阳虚质", Arrays.asList("生姜", "羊肉", "韭菜", "核桃", "桂圆", "肉桂"));
        foodsByConstitution.put("阴虚质", Arrays.asList("百合", "银耳", "梨", "枸杞", "黑芝麻", "鸭肉"));
        foodsByConstitution.put("痰湿质", Arrays.asList("薏米", "赤小豆", "冬瓜", "陈皮", "茯苓", "白萝卜"));
        foodsByConstitution.put("湿热质", Arrays.asList("绿豆", "冬瓜", "苦瓜", "薏米", "莲子", "红豆"));
        foodsByConstitution.put("血瘀质", Arrays.asList("山楂", "黑豆", "玫瑰花", "木耳", "红糖", "桃仁"));
        foodsByConstitution.put("气郁质", Arrays.asList("玫瑰花", "陈皮", "萝卜", "海带", "香蕉", "燕麦"));
        foodsByConstitution.put("特禀质", Arrays.asList("蜂蜜", "大枣", "金针菇", "胡萝卜", "糙米", "西兰花"));
        foodsByConstitution.put("平和质", Arrays.asList("五谷杂粮", "蔬菜水果", "鱼虾", "鸡蛋", "牛奶"));

        return foodsByConstitution.getOrDefault(constitution, Arrays.asList("山药", "红枣", "枸杞"));
    }

    /**
     * 调用AI生成食谱
     */
    private String generateWithAI(String constitution,
                                  List<Map.Entry<String, IngredientInfo>> foods,
                                  String mealType) {

        if (foods.isEmpty()) {
            return generateFallbackRecipe(constitution, mealType);
        }

        StringBuilder foodsDesc = new StringBuilder();
        for (int i = 0; i < Math.min(15, foods.size()); i++) {
            Map.Entry<String, IngredientInfo> entry = foods.get(i);
            IngredientInfo info = entry.getValue();
            foodsDesc.append(String.format(
                    "- %s：%s性、%s味、功效：%s\n",
                    entry.getKey(),
                    info.getProperty() != null ? info.getProperty() : "平",
                    info.getFlavor() != null ? info.getFlavor() : "甘",
                    info.getEffect() != null && info.getEffect().length() > 50 ?
                            info.getEffect().substring(0, 50) : (info.getEffect() != null ? info.getEffect() : "补益身体")
            ));
        }

        String prompt = String.format("""
            你是一位中医食疗专家。请基于以下可用食材为用户设计一份%s食谱。
            
            【用户体质】%s
            【餐次】%s
            【可用食材】（已按体质适宜性排序）
            %s
            
            【生成要求】
            1. 从上述食材中选择3-5种进行搭配
            2. 给出菜名、详细食材用量、烹饪步骤
            3. 解释为什么这个搭配适合%s体质
            4. 考虑季节因素，推荐当季食材
            5. 如果用户偏好中有特殊要求，请尽量满足
            
            【输出格式】
            ### 🥣 菜名
            
            🌟 **为什么适合**：
            （解释这个食谱为什么适合您的体质）
            
            🥬 **食材清单**：
            - 食材1：XX克
            - 食材2：XX克
            - 食材3：XX克
            
            👨‍🍳 **烹饪步骤**：
            1. ...
            2. ...
            3. ...
            
            💡 **养生小贴士**：
            （1-2条实用建议）
            """,
                mealType, constitution, mealType, foodsDesc.toString(), constitution);

        return callLLMForRecipe(prompt, constitution, mealType);
    }

    /**
     * 调用DeepSeek API生成食谱
     */
    private String callLLMForRecipe(String prompt, String constitution, String mealType) {
        try {
            String jsonBody = String.format("""
                {
                    "model": "deepseek-chat",
                    "messages": [
                        {"role": "system", "content": "你是一个中医食疗专家，擅长根据体质和季节设计养生食谱。回答要专业、实用、亲切。"},
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.7,
                    "max_tokens": 1500
                }
                """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var json = objectMapper.readTree(response.body());
                String recipe = json.path("choices").path(0).path("message").path("content").asText();
                log.info("AI生成食谱成功");
                return recipe;
            } else {
                log.error("AI生成食谱失败: status={}", response.statusCode());
                return generateFallbackRecipe(constitution, mealType);
            }
        } catch (Exception e) {
            log.error("AI生成食谱异常", e);
            return generateFallbackRecipe(constitution, mealType);
        }
    }

    /**
     * 降级方案：返回基础食谱建议
     */
    private String generateFallbackRecipe(String constitution, String mealType) {
        Map<String, String[]> recipes = new HashMap<>();
        recipes.put("气虚质", new String[]{"山药红枣小米粥", "山药100g、红枣5颗、小米50g", "补气健脾"});
        recipes.put("阳虚质", new String[]{"生姜羊肉汤", "羊肉200g、生姜3片、当归5g", "温补阳气"});
        recipes.put("阴虚质", new String[]{"银耳百合雪梨羹", "银耳1朵、百合20g、雪梨1个", "滋阴润燥"});
        recipes.put("痰湿质", new String[]{"薏米赤小豆粥", "薏米30g、赤小豆30g、大米50g", "健脾祛湿"});
        recipes.put("湿热质", new String[]{"绿豆薏米汤", "绿豆30g、薏米30g、冰糖适量", "清热利湿"});
        recipes.put("平和质", new String[]{"五谷杂粮饭", "大米50g、小米30g、糙米30g", "营养均衡"});

        String[] recipe = recipes.getOrDefault(constitution, new String[]{"养生粥", "山药50g、红枣5颗、大米50g", "调理身体"});

        String mealName = mealType.equals("早餐") ? "早餐" : (mealType.equals("午餐") ? "午餐" : "晚餐");

        return String.format("""
            ### 🥣 %s推荐：%s
            
            🌟 **为什么适合**：
            根据您的%s体质，%s有助于%s。
            
            🥬 **食材清单**：
            - %s
            
            👨‍🍳 **简单做法**：
            1. 将所有食材洗净
            2. 放入锅中加水煮至熟烂
            3. 根据口味适量调味
            
            💡 **小贴士**：
            - 建议%s食用，效果更佳
            - 可根据口味适当调整食材用量
            """,
                mealName, recipe[0], constitution, recipe[0], recipe[2],
                recipe[1], mealName.equals("早餐") ? "早晨空腹" : "正餐时间");
    }
}