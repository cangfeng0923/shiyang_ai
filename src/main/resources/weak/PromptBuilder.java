package weak;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 职责：构建完整的 System Prompt
 */
@Slf4j
@Component
public class PromptBuilder {

    public String build(ConversationContext ctx) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一位资深的中医老专家，具有30年临床经验。你的回答要亲切自然，像长辈关心晚辈一样。\n\n");

        // 用户信息
        appendUserInfo(sb, ctx);

        // 饮食记录
        appendDietRecords(sb, ctx);

        // 节气养生
        appendSolarTerm(sb, ctx);

        // 食材信息
        appendFoodInfo(sb, ctx);

        // 症状
        appendSymptom(sb, ctx);

        // 历史上下文
        appendContext(sb, ctx);

        // 要求
        appendRequirements(sb, ctx);

        return sb.toString();
    }

    private void appendUserInfo(StringBuilder sb, ConversationContext ctx) {
        sb.append("【用户信息】\n");
        sb.append("- 中医体质：").append(ctx.getConstitution()).append("\n");

        HealthProfile profile = ctx.getProfile();
        if (profile != null) {
            if (profile.getAge() != null) sb.append("- 年龄：").append(profile.getAge()).append("岁\n");
            if (profile.getGender() != null) sb.append("- 性别：").append(profile.getGender().equals("MALE") ? "男" : "女").append("\n");
            if (profile.getHeight() != null && profile.getWeight() != null) {
                double heightM = profile.getHeight() / 100;
                double bmi = profile.getWeight() / (heightM * heightM);
                String bmiStatus;
                if (bmi < 18.5) bmiStatus = "偏瘦";
                else if (bmi < 24) bmiStatus = "正常";
                else if (bmi < 28) bmiStatus = "超重";
                else bmiStatus = "肥胖";
                sb.append(String.format("- BMI：%.1f（%s）\n", bmi, bmiStatus));
            }
        }

        if (profile != null) {
            // 注意：这里需要 HealthProfileService 的 parseJsonArray 方法
            // 如果不想引入，可以保持原样，在原 AIService 中处理
        }
    }

    private void appendDietRecords(StringBuilder sb, ConversationContext ctx) {
        List<DietRecord> todayRecords = ctx.getTodayRecords();
        if (todayRecords != null && !todayRecords.isEmpty()) {
            sb.append("\n【今日饮食记录】\n");
            for (DietRecord record : todayRecords) {
                String mealName = getMealName(record.getMealType());
                sb.append(String.format("- %s：%s %sg，健康评分：%d/100\n",
                        mealName, record.getFoodName(),
                        record.getGrams() != null ? record.getGrams() : 0,
                        record.getHealthScore()));
            }
        }

        List<DietRecord> weekRecords = ctx.getWeekRecords();
        if (weekRecords != null && !weekRecords.isEmpty()) {
            sb.append("\n【用户近7日详细饮食记录】\n");
            Map<LocalDate, List<DietRecord>> recordsByDay = weekRecords.stream()
                    .collect(Collectors.groupingBy(r -> r.getRecordDate().toLocalDate()));

            for (Map.Entry<LocalDate, List<DietRecord>> entry : recordsByDay.entrySet()) {
                sb.append(entry.getKey().toString()).append("：\n");
                for (DietRecord record : entry.getValue()) {
                    String mealName = getMealName(record.getMealType());
                    sb.append(String.format("  - %s：%s %sg（评分：%d/100）\n",
                            mealName, record.getFoodName(),
                            record.getGrams() != null ? record.getGrams() : 0,
                            record.getHealthScore()));
                }
            }

            double avgScore = weekRecords.stream().mapToInt(DietRecord::getHealthScore).average().orElse(0);
            sb.append(String.format("\n【饮食分析】平均健康评分：%.0f/100\n", avgScore));

            boolean hasBreakfast = weekRecords.stream().anyMatch(r -> "BREAKFAST".equals(r.getMealType()));
            if (!hasBreakfast) {
                sb.append("⚠️ 用户本周没有记录早餐，可能有不规律吃早餐的习惯\n");
            }
        }
    }

    private void appendSolarTerm(StringBuilder sb, ConversationContext ctx) {
        sb.append("\n【当前节气养生参考】\n");
        sb.append(ctx.getSolarTermAdvice()).append("\n");
    }

    private void appendFoodInfo(StringBuilder sb, ConversationContext ctx) {
        IngredientInfo foodInfo = ctx.getFoodInfo();
        if (foodInfo != null) {
            sb.append("\n【用户询问的食材 - 真实数据（必须基于此回答！）】\n");
            sb.append("- 食材名称：").append(foodInfo.getName()).append("\n");
            sb.append("- 中医属性：").append(foodInfo.getProperty() != null ? foodInfo.getProperty() : "平").append("性\n");
            sb.append("- 味道：").append(foodInfo.getFlavor() != null ? foodInfo.getFlavor() : "甘").append("味\n");
            sb.append("- 归经：").append(foodInfo.getMeridian() != null ? foodInfo.getMeridian() : "脾、胃").append("\n");
            sb.append("- 主要功效：").append(foodInfo.getEffect() != null ? foodInfo.getEffect() : "补益身体").append("\n");
            sb.append("- 禁忌人群：").append(foodInfo.getContraindication() != null ? foodInfo.getContraindication() : "无明显禁忌").append("\n");

            int score = getSuitabilityScore(foodInfo, ctx.getConstitution());
            if (score > 0) sb.append("- 对").append(ctx.getConstitution()).append("体质：✅ 适合食用\n");
            else if (score < 0) sb.append("- 对").append(ctx.getConstitution()).append("体质：❌ 不太适合，需要谨慎或少量食用\n");
            else sb.append("- 对").append(ctx.getConstitution()).append("体质：⚖️ 中性，可适量食用\n");
        }
    }

    private void appendSymptom(StringBuilder sb, ConversationContext ctx) {
        String symptom = ctx.getSymptom();
        if (symptom != null && !symptom.isEmpty()) {
            sb.append("\n【用户当前症状】").append(symptom).append("\n");
            sb.append("请根据症状给出调理建议，推荐适合的食疗方案。\n");
        }
    }

    private void appendContext(StringBuilder sb, ConversationContext ctx) {
        String previousContext = ctx.getPreviousContext();
        if (previousContext != null && !previousContext.isEmpty()) {
            sb.append("\n【上一轮对话摘要】").append(previousContext).append("\n");
            sb.append("请保持对话连贯性，不要重复之前说过的话。\n");
        }
    }

    private void appendRequirements(StringBuilder sb, ConversationContext ctx) {
        sb.append("""

            【核心回答要求 - 必须遵守】
            1. **优先检查过敏和忌口**：如果用户询问的食物在过敏/忌口列表中，必须明确警告并推荐替代品
            2. **使用真实食材数据**：如果上面有【用户询问的食材】，必须基于真实数据回答
            3. **结合体质**：根据用户的中医体质给出个性化建议
            4. **结合节气**：推荐食物时考虑当前节气
            5. **结合饮食记录**：根据用户最近的饮食给出改善建议
            6. **多轮对话连贯**：记住上一轮聊的内容
            7. **回答格式**：亲切自然，用"您"称呼，可以适当使用表情符号
            
            请开始回答：""");
    }

    private String getMealName(String mealType) {
        switch (mealType) {
            case "BREAKFAST": return "早餐";
            case "LUNCH": return "午餐";
            case "DINNER": return "晚餐";
            case "SNACK": return "加餐";
            default: return mealType;
        }
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
}