package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
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
public class AIReportService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Autowired
    private FoodCategoryService foodCategoryService;  // 注入分类服务

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成AI饮食报告
     */
    public String generateDietReport(List<DietRecord> records, String constitution) {
        if (records == null || records.isEmpty()) {
            return "📝 暂无饮食记录。开始记录您的每日饮食，AI将为您生成个性化分析和建议。";
        }

        try {
            String dietData = buildDietData(records);

            String systemPrompt = """
                你是一位资深的中医营养师，有20年临床经验。
                请根据用户真实的饮食记录，给出专业、具体、可执行的建议。
                你的回答要亲切自然，像朋友聊天一样。
                """;

            String userPrompt = String.format("""
                请根据以下饮食记录给出个性化饮食建议：

                【用户体质】%s

                【近7日饮食记录】
                %s

                要求：
                1. 先简单肯定用户做得好的地方（1-2句）
                2. 然后指出需要改进的地方（2-3点）
                3. 给出具体可执行的建议（点名食物、分量、做法）
                4. 结合用户体质给出针对性建议
                5. **必须严格按照以下格式输出，不要多余的内容：**

                🌟 亮点：
                [肯定用户做得好的地方，1-2句话]

                📌 改进点：
                • [问题1]：[具体建议]
                • [问题2]：[具体建议]
                • [问题3]：[具体建议]

                💡 体质专属提醒：
                [针对用户体质的1-2条建议]

                📖 养生金句：
                [一句鼓励的话或养生名言]

                注意：每部分之间用空行分隔，不要使用"首先、其次、最后"这类词。
                """, constitution, dietData);

            String aiResponse = callDeepSeek(systemPrompt, userPrompt);
            log.info("AI报告生成成功");
            return aiResponse;

        } catch (Exception e) {
            log.error("AI报告生成失败", e);
            return getFallbackReport(records, constitution);
        }
    }

    /**
     * 构建饮食数据
     */
    private String buildDietData(List<DietRecord> records) {
        // 按日期分组
        Map<String, List<DietRecord>> byDate = records.stream()
                .collect(Collectors.groupingBy(r -> r.getRecordDate().toLocalDate().toString()));

        StringBuilder sb = new StringBuilder();

        // 按日期倒序排列
        List<String> dates = new ArrayList<>(byDate.keySet());
        dates.sort(Collections.reverseOrder());

        for (String date : dates) {
            List<DietRecord> dayRecords = byDate.get(date);
            sb.append("\n").append(date).append("：\n");

            // 按餐次分组
            Map<String, List<DietRecord>> byMeal = dayRecords.stream()
                    .collect(Collectors.groupingBy(r -> r.getMealType() != null ? r.getMealType() : "其他"));

            String[] mealOrder = {"BREAKFAST", "LUNCH", "DINNER", "SNACK"};
            String[] mealNames = {"早餐", "午餐", "晚餐", "加餐"};

            for (int i = 0; i < mealOrder.length; i++) {
                List<DietRecord> meals = byMeal.get(mealOrder[i]);
                if (meals != null && !meals.isEmpty()) {
                    sb.append("  ").append(mealNames[i]).append("：");
                    List<String> foodStrs = new ArrayList<>();
                    for (DietRecord r : meals) {
                        String food = r.getFoodName();
                        if (r.getGrams() != null && r.getGrams() > 0) {
                            food += "(" + r.getGrams().intValue() + "g)";
                        }
                        foodStrs.add(food);
                    }
                    sb.append(String.join("、", foodStrs)).append("\n");
                }
            }
        }

        // 统计信息 - 使用 FoodCategoryService
        double avgScore = records.stream()
                .mapToInt(r -> r.getHealthScore() != null ? r.getHealthScore() : 0)
                .average()
                .orElse(0);

        Set<String> uniqueFoods = records.stream()
                .map(DietRecord::getFoodName)
                .collect(Collectors.toSet());

        long breakfastCount = records.stream()
                .filter(r -> "BREAKFAST".equals(r.getMealType()))
                .count();

        // 使用 FoodCategoryService 进行分类统计
        long vegCount = records.stream()
                .filter(r -> foodCategoryService.isVegetable(r.getFoodName()))
                .count();

        long proteinCount = records.stream()
                .filter(r -> foodCategoryService.isProtein(r.getFoodName()))
                .count();

        long fruitCount = records.stream()
                .filter(r -> foodCategoryService.isFruit(r.getFoodName()))
                .count();

        long unhealthyCount = records.stream()
                .filter(r -> foodCategoryService.isUnhealthy(r.getFoodName()))
                .count();

        sb.append("\n【统计】\n");
        sb.append("- 平均健康评分：").append(Math.round(avgScore)).append("/100\n");
        sb.append("- 食物种类数：").append(uniqueFoods.size()).append("种\n");
        sb.append("- 早餐记录次数：").append(breakfastCount).append("次\n");
        sb.append("- 蔬菜类记录次数：").append(vegCount).append("次\n");
        sb.append("- 蛋白质类记录次数：").append(proteinCount).append("次\n");
        sb.append("- 水果类记录次数：").append(fruitCount).append("次\n");
        sb.append("- 不健康食物记录次数：").append(unhealthyCount).append("次\n");

        return sb.toString();
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeek(String systemPrompt, String userPrompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty() || "your-deepseek-api-key".equals(apiKey)) {
            log.warn("DeepSeek API Key未配置");
            return null;
        }

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 800);
        requestBody.put("stream", false);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var jsonNode = objectMapper.readTree(response.body());
            return jsonNode.path("choices").path(0).path("message").path("content").asText();
        } else {
            log.error("DeepSeek API调用失败: {}", response.statusCode());
            return null;
        }
    }

    /**
     * 降级报告（AI不可用时使用）
     */
    private String getFallbackReport(List<DietRecord> records, String constitution) {
        // 分析饮食数据 - 使用 FoodCategoryService
        boolean hasBreakfast = records.stream().anyMatch(r -> "BREAKFAST".equals(r.getMealType()));
        long vegCount = records.stream().filter(r -> foodCategoryService.isVegetable(r.getFoodName())).count();
        long proteinCount = records.stream().filter(r -> foodCategoryService.isProtein(r.getFoodName())).count();
        long unhealthyCount = records.stream().filter(r -> foodCategoryService.isUnhealthy(r.getFoodName())).count();
        long fruitCount = records.stream().filter(r -> foodCategoryService.isFruit(r.getFoodName())).count();
        Set<String> uniqueFoods = records.stream().map(DietRecord::getFoodName).collect(Collectors.toSet());

        double avgScore = records.stream()
                .mapToInt(r -> r.getHealthScore() != null ? r.getHealthScore() : 0)
                .average()
                .orElse(0);

        StringBuilder sb = new StringBuilder();

        // 亮点
        sb.append("🌟 亮点：\n");
        if (avgScore >= 70) {
            sb.append("您近期的饮食整体评分不错，继续保持！");
        } else if (fruitCount > 0 || vegCount > 0) {
            sb.append("您有记录水果和蔬菜，这是很好的饮食习惯！");
        } else {
            sb.append("您已经开始记录饮食了，这是健康管理的第一步！");
        }
        sb.append("\n\n");

        // 改进点
        sb.append("📌 改进点：\n");

        if (!hasBreakfast) {
            sb.append("• 🌅 早餐缺失：本周没有记录早餐，建议每天吃早餐（如：小米粥+鸡蛋、豆浆+全麦面包）。\n");
        } else if (records.stream().filter(r -> "BREAKFAST".equals(r.getMealType())).count() < 5) {
            long breakfastRecords = records.stream().filter(r -> "BREAKFAST".equals(r.getMealType())).count();
            sb.append("• 🌅 早餐次数偏少（").append(breakfastRecords).append("次/周），规律早餐有助于全天代谢。\n");
        }

        if (vegCount == 0) {
            sb.append("• 🥬 蔬菜摄入不足：本周没有吃蔬菜！建议每天午餐和晚餐各加一份绿叶蔬菜（如：清炒菠菜、蒜蓉西兰花）。\n");
        } else if (vegCount < 3) {
            sb.append("• 🥬 蔬菜摄入偏少：建议每餐增加一份绿叶蔬菜，每天吃够300g。\n");
        }

        if (proteinCount == 0) {
            sb.append("• 🥩 蛋白质摄入不足：本周没有优质蛋白！建议每天吃1个鸡蛋，午餐加一份鱼肉或豆腐。\n");
        } else if (proteinCount < 3) {
            sb.append("• 🥩 蛋白质来源单一：建议多样化摄入，如鸡蛋、鱼肉、鸡肉、豆腐交替食用。\n");
        }

        if (fruitCount == 0) {
            sb.append("• 🍎 水果缺失：建议每天吃200-300g新鲜水果作为加餐（如：苹果、香蕉、橙子）。\n");
        }

        if (unhealthyCount > 0) {
            sb.append("• ⚠️ 高热量食物：本周记录了").append(unhealthyCount).append("次油炸/高糖食物，建议减少摄入，选择蒸、煮、炖等清淡烹饪方式。\n");
        }

        if (uniqueFoods.size() < 8) {
            sb.append("• 🍽️ 食物种类偏少（").append(uniqueFoods.size()).append("种），建议每周吃够12种以上不同食物。\n");
        }

        sb.append("\n");

        // 体质专属提醒
        sb.append("💡 体质专属提醒：\n");
        String constitutionAdvice = getConstitutionAdvice(constitution);
        if (!constitutionAdvice.isEmpty()) {
            sb.append(constitutionAdvice);
        } else {
            sb.append("保持均衡饮食，顺应时节调理身体。");
        }
        sb.append("\n\n");

        // 养生金句
        sb.append("📖 养生金句：\n");
        sb.append("食饮有节，起居有常，不妄作劳。");

        return sb.toString();
    }

    private String getConstitutionAdvice(String constitution) {
        switch (constitution) {
            case "气虚质": return "🌿 您是气虚体质，建议多吃山药、大枣、黄芪炖鸡补气，少吃生冷食物。";
            case "阳虚质": return "🔥 您是阳虚体质，建议多吃生姜、羊肉、韭菜温补阳气，注意保暖。";
            case "阴虚质": return "💧 您是阴虚体质，建议多吃百合、银耳、梨滋阴润燥，少吃辛辣。";
            case "痰湿质": return "💦 您是痰湿体质，建议少吃甜食油腻，多吃薏米、冬瓜、赤小豆祛湿。";
            case "湿热质": return "🌊 您是湿热体质，建议少吃辛辣烧烤，多吃绿豆、苦瓜、黄瓜清热。";
            default: return "";
        }
    }
}