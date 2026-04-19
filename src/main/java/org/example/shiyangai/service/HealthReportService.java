// service/HealthReportService.java
package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.enums.ConstitutionType;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor//健康报告服务
public class HealthReportService {

    private final DietRecordService dietRecordService;
    private final HealthProfileService healthProfileService;
    private final DynamicSolarTermService solarTermService;

    /**
     * 生成综合健康报告
     */
    public String generateComprehensiveReport(String userId, String constitution) {
        HealthProfile profile = healthProfileService.getProfile(userId);
        List<DietRecord> weekRecords = dietRecordService.getWeekRecords(userId);
        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 **您的专属健康报告**\n\n");
        sb.append("📅 报告时间：").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))).append("\n\n");

        // ========== 1. 基础信息 ==========
        sb.append("---\n");
        sb.append("### 1️⃣ 基础信息\n\n");
        if (profile != null) {
            sb.append("- 中医体质：**").append(constitution).append("**\n");
            if (profile.getAge() != null) sb.append("- 年龄：").append(profile.getAge()).append("岁\n");
            if (profile.getGender() != null) sb.append("- 性别：").append(profile.getGender().equals("MALE") ? "男" : "女").append("\n");
            if (profile.getHeight() != null && profile.getWeight() != null) {
                double bmi = profile.getWeight() / Math.pow(profile.getHeight() / 100, 2);
                String bmiAdvice;
                if (bmi < 18.5) bmiAdvice = "偏瘦，建议增加营养摄入";
                else if (bmi < 24) bmiAdvice = "正常范围，继续保持";
                else if (bmi < 28) bmiAdvice = "超重，建议控制饮食、增加运动";
                else bmiAdvice = "肥胖，建议咨询专业营养师";
                sb.append(String.format("- BMI：%.1f（%s）\n", bmi, bmiAdvice));
            }
        } else {
            sb.append("> ⚠️ 请先在「健康档案」中填写您的个人信息\n\n");
        }

        // ========== 2. 本周饮食总结 ==========
        sb.append("\n---\n");
        sb.append("### 2️⃣ 本周饮食总结\n\n");

        if (!weekRecords.isEmpty()) {
            // 统计各餐次
            Map<String, Long> mealCount = weekRecords.stream()
                    .collect(Collectors.groupingBy(DietRecord::getMealType, Collectors.counting()));

            sb.append("📈 **统计数据**：\n");
            sb.append("- 总记录餐数：").append(weekRecords.size()).append("餐\n");
            sb.append("- 早餐：").append(mealCount.getOrDefault("BREAKFAST", 0L)).append("次\n");
            sb.append("- 午餐：").append(mealCount.getOrDefault("LUNCH", 0L)).append("次\n");
            sb.append("- 晚餐：").append(mealCount.getOrDefault("DINNER", 0L)).append("次\n");
            sb.append("- 加餐：").append(mealCount.getOrDefault("SNACK", 0L)).append("次\n");

            double avgScore = weekRecords.stream().mapToInt(DietRecord::getHealthScore).average().orElse(0);
            sb.append(String.format("\n⭐ **平均健康评分**：%.0f/100\n", avgScore));

            if (avgScore >= 80) {
                sb.append("🎉 优秀！您的饮食习惯非常好，请继续保持！\n");
            } else if (avgScore >= 60) {
                sb.append("👍 良好！还有一些提升空间，可以进一步优化饮食结构。\n");
            } else {
                sb.append("⚠️ 需要改进！请参考下面的建议调整饮食习惯。\n");
            }

            // 高频食物统计
            Map<String, Long> foodFrequency = weekRecords.stream()
                    .collect(Collectors.groupingBy(DietRecord::getFoodName, Collectors.counting()));

            List<Map.Entry<String, Long>> topFoods = foodFrequency.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(5)
                    .collect(Collectors.toList());

            if (!topFoods.isEmpty()) {
                sb.append("\n🍽️ **高频食物TOP5**：\n");
                for (Map.Entry<String, Long> entry : topFoods) {
                    sb.append("- ").append(entry.getKey()).append("（").append(entry.getValue()).append("次）\n");
                }
            }
        } else {
            sb.append("> 📝 本周暂无饮食记录，开始记录您的饮食吧！\n\n");
            sb.append("记录饮食后，我可以为您：\n");
            sb.append("- 分析营养均衡度\n");
            sb.append("- 给出健康评分\n");
            sb.append("- 推荐适合您体质的食物\n");
        }

        // ========== 3. 今日饮食 ==========
        sb.append("\n---\n");
        sb.append("### 3️⃣ 今日饮食\n\n");

        if (!todayRecords.isEmpty()) {
            for (DietRecord record : todayRecords) {
                String mealName = getMealName(record.getMealType());
                sb.append(String.format("**%s**：%s %sg（评分：%d/100）\n",
                        mealName, record.getFoodName(),
                        record.getGrams() != null ? record.getGrams() : 0,
                        record.getHealthScore()));
            }
        } else {
            sb.append("> 📝 今日尚未记录饮食\n\n");
        }

        // ========== 4. 体质饮食建议 ==========
        sb.append("\n---\n");
        sb.append("### 4️⃣ 体质饮食建议\n\n");

        ConstitutionType type = ConstitutionType.fromName(constitution);
        sb.append("**").append(constitution).append("**：").append(type.getDescription()).append("\n\n");
        sb.append("✅ **适宜食物**：").append(String.join("、", type.getGoodFoods())).append("\n");
        sb.append("❌ **不宜食物**：").append(String.join("、", type.getBadFoods())).append("\n");

        // ========== 5. 节气养生提醒 ==========
        sb.append("\n---\n");
        sb.append("### 5️⃣ 节气养生提醒\n\n");

        String currentTerm = solarTermService.getCurrentSolarTermName();
        String nextTerm = solarTermService.getNextSolarTermName();
        sb.append("📅 当前节气：**").append(currentTerm).append("**\n");
        sb.append("⏭️ 下一个节气：").append(nextTerm).append("\n\n");

        String termAdvice = solarTermService.getDynamicSolarTermAdvice(constitution);
        // 提取节气特点部分
        if (termAdvice.contains("【节气特点】")) {
            int start = termAdvice.indexOf("【节气特点】");
            int end = termAdvice.indexOf("【养生原则】");
            if (end > start && end <= termAdvice.length()) {
                sb.append(termAdvice.substring(start, end)).append("\n");
            }
        }

        // ========== 6. 改进建议 ==========
        sb.append("\n---\n");
        sb.append("### 6️⃣ 改进建议\n\n");

        List<String> suggestions = generateImprovementSuggestions(weekRecords, constitution);
        for (int i = 0; i < suggestions.size(); i++) {
            sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
        }

        // ========== 7. 健康提醒 ==========
        sb.append("\n---\n");
        sb.append("### 7️⃣ 健康提醒\n\n");
        sb.append(getHealthReminders(profile));

        sb.append("\n---\n");
        sb.append("*本报告基于您的饮食记录和健康档案生成，仅供参考。如有身体不适，请及时就医。*");

        return sb.toString();
    }

    /**
     * 生成改进建议
     */
    private List<String> generateImprovementSuggestions(List<DietRecord> records, String constitution) {
        List<String> suggestions = new ArrayList<>();

        if (records.isEmpty()) {
            suggestions.add("开始记录您的日常饮食，这样才能获得个性化的饮食建议");
            suggestions.add("建议完成体质测评，获取更精准的饮食指导");
            return suggestions;
        }

        // 检查早餐
        boolean hasBreakfast = records.stream().anyMatch(r -> "BREAKFAST".equals(r.getMealType()));
        if (!hasBreakfast) {
            suggestions.add("🍳 建议规律吃早餐，早餐是一天中最重要的一餐");
        }

        // 检查蔬菜摄入
        boolean hasVeggies = records.stream().anyMatch(r ->
                r.getFoodName().contains("蔬菜") || r.getFoodName().contains("青菜") ||
                        r.getFoodName().contains("西兰花") || r.getFoodName().contains("菠菜"));
        if (!hasVeggies) {
            suggestions.add("🥬 本周蔬菜摄入不足，建议每天至少吃300g绿叶蔬菜");
        }

        // 检查蛋白质
        boolean hasProtein = records.stream().anyMatch(r ->
                r.getFoodName().contains("肉") || r.getFoodName().contains("蛋") ||
                        r.getFoodName().contains("豆腐") || r.getFoodName().contains("鱼"));
        if (!hasProtein) {
            suggestions.add("🥩 蛋白质摄入不足，建议增加鸡蛋、鱼肉、豆腐等优质蛋白");
        }

        // 体质相关建议
        Map<String, String> constitutionAdvice = new HashMap<>();
        constitutionAdvice.put("气虚质", "🌿 建议多吃山药、大枣、黄芪炖鸡，补气健脾");
        constitutionAdvice.put("阳虚质", "🔥 建议多吃生姜、羊肉、韭菜，温补阳气");
        constitutionAdvice.put("阴虚质", "💧 建议多吃百合、银耳、梨，滋阴润燥");
        constitutionAdvice.put("痰湿质", "🌾 建议少吃甜食油腻，多吃薏米、冬瓜祛湿");
        constitutionAdvice.put("湿热质", "🥒 建议少吃辛辣烧烤，多吃绿豆、苦瓜清热");
        constitutionAdvice.put("血瘀质", "🍒 建议多吃山楂、黑豆、玫瑰花茶，活血化瘀");
        constitutionAdvice.put("气郁质", "🌹 建议喝玫瑰花茶、多交朋友，疏肝解郁");

        String advice = constitutionAdvice.get(constitution);
        if (advice != null) {
            suggestions.add(advice);
        }

        // 水分提醒
        suggestions.add("💧 每天喝够1.5-2升水，可以喝养生茶代替白开水");
        suggestions.add("😴 保证7-8小时睡眠，晚上11点前入睡最佳");

        return suggestions;
    }

    /**
     * 获取健康提醒
     */
    private String getHealthReminders(HealthProfile profile) {
        StringBuilder sb = new StringBuilder();

        if (profile != null) {
            List<String> allergies = healthProfileService.parseJsonArray(profile.getAllergies());
            if (!allergies.isEmpty()) {
                sb.append("⚠️ **过敏提醒**：您对").append(String.join("、", allergies)).append("过敏，请避免食用\n\n");
            }

            List<String> diseases = healthProfileService.parseJsonArray(profile.getPastDiseases());
            if (!diseases.isEmpty()) {
                sb.append("📝 **病史提醒**：您有").append(String.join("、", diseases)).append("，请注意定期复查\n\n");
            }
        }

        sb.append("🏥 如出现持续不适症状，请及时就医，不要仅依赖食疗");

        return sb.toString();
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
}