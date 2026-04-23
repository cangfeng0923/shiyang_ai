package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.crawler.FoodDataCrawler;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.enums.ConstitutionType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版健康报告服务
 * 支持多维度数据融合、概率性推断、可视化报告生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedHealthReportService {

    private final DietRecordService dietRecordService;
    private final HealthProfileService healthProfileService;
    private final DynamicSolarTermService solarTermService;
    private final FoodDataCrawler foodDataCrawler;
    private final NutritionAnalysisService nutritionAnalysisService;

    /**
     * 生成增强版综合健康报告（Phase 1: 饮食维度）
     */
    public String generateEnhancedReport(String userId, String constitution) {
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
                String bmiAdvice = getBMIClassification(bmi);
                sb.append(String.format("- BMI：%.1f（%s）\n", bmi, bmiAdvice));
            }
        } else {
            sb.append("> ⚠️ 请先在「健康档案」中填写您的个人信息\n\n");
        }

        // ========== 2. 饮食健康风险推断 (Phase 1) ==========
        sb.append("\n---\n");
        sb.append("### 2️⃣ 饮食健康风险推断\n\n");

        if (!weekRecords.isEmpty()) {
            Map<String, Object> riskAnalysis = analyzeDietaryRisks(weekRecords);
            sb.append(formatRiskAnalysis(riskAnalysis));
        } else {
            sb.append("> 📝 本周暂无饮食记录，无法进行风险分析\n\n");
        }

        // ========== 3. 时序模式分析 (Phase 1) ==========
        sb.append("\n---\n");
        sb.append("### 3️⃣ 时序模式分析\n\n");

        if (!weekRecords.isEmpty()) {
            Map<String, Object> patternAnalysis = analyzeDietaryPatterns(weekRecords);
            sb.append(formatPatternAnalysis(patternAnalysis));
        } else {
            sb.append("> 📝 本周暂无饮食记录，无法进行模式分析\n\n");
        }

        // ========== 4. 膳食指南对比 (Phase 1) ==========
        sb.append("\n---\n");
        sb.append("### 4️⃣ 膳食指南对比\n\n");

        if (!weekRecords.isEmpty()) {
            Map<String, Object> guidelineComparison = compareWithGuidelines(weekRecords);
            sb.append(formatGuidelineComparison(guidelineComparison));
        } else {
            sb.append("> 📝 本周暂无饮食记录，无法进行指南对比\n\n");
        }

        // ========== 5. 本周饮食总结 ==========
        sb.append("\n---\n");
        sb.append("### 5️⃣ 本周饮食总结\n\n");

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
            sb.append("> 📝 本周暂无饮食记录\n\n");
        }

        // ========== 6. 今日饮食 ==========
        sb.append("\n---\n");
        sb.append("### 6️⃣ 今日饮食\n\n");

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

        // ========== 7. 体质饮食建议 ==========
        sb.append("\n---\n");
        sb.append("### 7️⃣ 体质饮食建议\n\n");

        ConstitutionType type = ConstitutionType.fromName(constitution);
        sb.append("**").append(constitution).append("**：").append(type.getDescription()).append("\n\n");
        sb.append("✅ **适宜食物**：").append(String.join("、", type.getGoodFoods())).append("\n");
        sb.append("❌ **不宜食物**：").append(String.join("、", type.getBadFoods())).append("\n");

        // ========== 8. 节气养生提醒 ==========
        sb.append("\n---\n");
        sb.append("### 8️⃣ 节气养生提醒\n\n");

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

        // ========== 9. 改进建议 ==========
        sb.append("\n---\n");
        sb.append("### 9️⃣ 改进建议\n\n");

        List<String> suggestions = generateImprovementSuggestions(weekRecords, constitution, profile);
        for (int i = 0; i < suggestions.size(); i++) {
            sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
        }

        // ========== 10. 健康提醒 ==========
        sb.append("\n---\n");
        sb.append("### 1️⃣0️⃣ 健康提醒\n\n");
        sb.append(getHealthReminders(profile));

        sb.append("\n---\n");
        sb.append("*本报告基于您的饮食记录和健康档案生成，包含AI概率性推断结果。仅供参考，如有身体不适请咨询专业医师。*\n");
        sb.append("*报告生成时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("*");

        return sb.toString();
    }

    /**
     * 分析饮食健康风险 (Phase 1)
     */
    private Map<String, Object> analyzeDietaryRisks(List<DietRecord> records) {
        Map<String, Object> risks = new HashMap<>();

        // 计算记录天数
        int dayCount = (int) records.stream()
                .map(r -> r.getRecordDate().toLocalDate())
                .distinct()
                .count();
        if (dayCount == 0) dayCount = 1;

        // 使用营养分析服务
        NutritionAnalysisService.FiberAnalysisResult fiber = nutritionAnalysisService.analyzeFiberIntake(records, dayCount);
        NutritionAnalysisService.SodiumAnalysisResult sodium = nutritionAnalysisService.analyzeSodiumIntake(records, dayCount);
        NutritionAnalysisService.SugarAnalysisResult sugar = nutritionAnalysisService.analyzeSugarIntake(records, dayCount);
        NutritionAnalysisService.RedMeatAnalysisResult redMeat = nutritionAnalysisService.analyzeRedMeatIntake(records, dayCount);

        List<String> identifiedRisks = new ArrayList<>();

        // 膳食纤维风险
        if (fiber.isSevereInsufficient) {
            identifiedRisks.add("⚠️ **膳食纤维严重不足**（" + String.format("%.0f", fiber.avgFiber) + "g/天，建议≥25g/天）\n" +
                    "   - 健康风险：便秘、肠道菌群失调、结直肠癌风险增加\n" +
                    "   - 置信度：85%\n" +
                    "   - 建议：每天增加1/3全谷物，蔬菜摄入达300-500g/天");
        } else if (fiber.isInsufficient) {
            identifiedRisks.add("📌 **膳食纤维摄入不足**（" + String.format("%.0f", fiber.avgFiber) + "g/天，建议≥25g/天）\n" +
                    "   - 置信度：75%\n" +
                    "   - 建议：增加燕麦、菌菇、豆类、绿叶蔬菜摄入");
        }

        // 钠摄入风险
        if (sodium.isSevereExceeded) {
            identifiedRisks.add("⚠️ **钠摄入严重超标**（约" + String.format("%.0f", sodium.avgSodium) + "mg/天，相当于" +
                    String.format("%.1f", sodium.avgSodium / 400) + "g盐）\n" +
                    "   - 健康风险：高血压、心血管疾病、肾脏负担增加\n" +
                    "   - 参考依据：《中国高血压防治指南2018》推荐<2000mg/天\n" +
                    "   - 置信度：90%\n" +
                    "   - 建议：每天食盐控制在5g内，连续3天早晨测血压");
        } else if (sodium.isExceeded) {
            identifiedRisks.add("📌 **钠摄入偏高**（约" + String.format("%.0f", sodium.avgSodium) + "mg/天）\n" +
                    "   - 健康风险：血压健康需关注\n" +
                    "   - 置信度：80%\n" +
                    "   - 建议：减少外卖、加工食品，烹饪晚放盐");
        }

        // 糖摄入风险
        if (sugar.isExceeded) {
            identifiedRisks.add("⚠️ **添加糖摄入超标**（" + String.format("%.0f", sugar.avgSugar) + "g/天，WHO建议<50g/天）\n" +
                    "   - 健康风险：糖尿病前期、肥胖、龋齿风险增加\n" +
                    "   - 参考依据：WHO《成人和儿童糖摄入量指南》2015\n" +
                    "   - 置信度：85%\n" +
                    "   - 建议：避免含糖饮料，建议检查空腹血糖");
        } else if (sugar.isHigh) {
            identifiedRisks.add("📌 **添加糖摄入偏高**（" + String.format("%.0f", sugar.avgSugar) + "g/天，WHO建议最好<25g/天）\n" +
                    "   - 置信度：75%\n" +
                    "   - 建议：减少奶茶、可乐，选择无糖替代品");
        }

        // 红肉风险
        if (redMeat.riskLevel.equals("high")) {
            identifiedRisks.add("⚠️ **红肉摄入过多**（" + String.format("%.0f", redMeat.avgRedMeat) + "g/天）\n" +
                    "   - 健康风险：结直肠癌风险略增（参考依据：IARC）\n" +
                    "   - 置信度：70%\n" +
                    "   - 建议：每周红肉控制在500g内，增加膳食纤维平衡");
            if (redMeat.totalProcessedMeat > 0) {
                identifiedRisks.add("⚠️ **加工肉制品摄入**（本周共" + String.format("%.0f", redMeat.totalProcessedMeat) + "g）\n" +
                        "   - 健康风险：WHO将加工肉制品列为1类致癌物\n" +
                        "   - 建议：尽量少吃或不吃香肠、培根、火腿等");
            }
        } else if (redMeat.riskLevel.equals("medium")) {
            identifiedRisks.add("📌 **红肉摄入偏高**（" + String.format("%.0f", redMeat.avgRedMeat) + "g/天）\n" +
                    "   - 建议：适当减少红肉，增加鱼和禽肉");
        }

        // 计算其他营养指标
        double totalCalories = records.stream().mapToDouble(r -> {
            NutritionAnalysisService.FoodNutrition nutrition = nutritionAnalysisService.getFoodNutrition(r.getFoodName());
            return r.getGrams() != null ? nutrition.calories * r.getGrams() / 100 : 0;
        }).sum();
        double avgCaloriesPerDay = totalCalories / dayCount;

        risks.put("identifiedRisks", identifiedRisks);
        risks.put("totalCalories", totalCalories);
        risks.put("avgCaloriesPerDay", avgCaloriesPerDay);
        risks.put("fiberResult", fiber);
        risks.put("sodiumResult", sodium);
        risks.put("sugarResult", sugar);
        risks.put("redMeatResult", redMeat);

        return risks;
    }
    /**
     * 分析时序模式 (Phase 1)
     */
    private Map<String, Object> analyzeDietaryPatterns(List<DietRecord> records) {
        Map<String, Object> patterns = new HashMap<>();

        // 按天分组
        Map<LocalDate, List<DietRecord>> recordsByDay = records.stream()
                .collect(Collectors.groupingBy(r -> r.getRecordDate().toLocalDate()));

        // 计算周末效应（周末vs工作日热量摄入比）
        double weekendCalories = 0;
        double weekdayCalories = 0;
        int weekendDays = 0;
        int weekdayDays = 0;

        for (Map.Entry<LocalDate, List<DietRecord>> entry : recordsByDay.entrySet()) {
            double dailyCalories = entry.getValue().stream()
                    .mapToDouble(r -> r.getCalories() != null ? r.getCalories() : 0)
                    .sum();

            if (entry.getKey().getDayOfWeek().getValue() >= 6) { // 周六日
                weekendCalories += dailyCalories;
                weekendDays++;
            } else { // 工作日
                weekdayCalories += dailyCalories;
                weekdayDays++;
            }
        }

        double weekendAvg = weekendDays > 0 ? weekendCalories / weekendDays : 0;
        double weekdayAvg = weekdayDays > 0 ? weekdayCalories / weekdayDays : 0;
        double weekendEffect = weekdayAvg > 0 ? weekendAvg / weekdayAvg : 1.0;

        // 计算漏餐频率
        int missedMeals = 0;
        int totalExpectedMeals = recordsByDay.size() * 3; // 假设每天3餐

        for (List<DietRecord> dailyRecords : recordsByDay.values()) {
            Set<String> mealTypes = dailyRecords.stream()
                    .map(DietRecord::getMealType)
                    .collect(Collectors.toSet());

            if (!mealTypes.contains("BREAKFAST")) missedMeals++;
            if (!mealTypes.contains("LUNCH")) missedMeals++;
            if (!mealTypes.contains("DINNER")) missedMeals++;
        }

        // 计算夜宵频率
        long nightEating = records.stream()
                .filter(r -> r.getRecordDate() != null && r.getRecordDate().getHour() >= 21)
                .count();

        patterns.put("weekendEffect", weekendEffect);
        patterns.put("missedMeals", missedMeals);
        patterns.put("totalExpectedMeals", totalExpectedMeals);
        patterns.put("nightEatingCount", nightEating);
        patterns.put("dayCount", recordsByDay.size());

        return patterns;
    }

    /**
     * 与膳食指南对比 (Phase 1)
     */
    private Map<String, Object> compareWithGuidelines(List<DietRecord> records) {
        Map<String, Object> comparison = new HashMap<>();

        int dayCount = (int) records.stream()
                .map(r -> r.getRecordDate().toLocalDate())
                .distinct()
                .count();
        if (dayCount == 0) dayCount = 1;

        // 计算各项实际摄入
        double totalVegetables = 0;
        double totalFruit = 0;
        double totalProtein = 0;
        double totalWater = 0;
        Set<String> uniqueFoods = new HashSet<>();

        for (DietRecord record : records) {
            uniqueFoods.add(record.getFoodName());
            String foodName = record.getFoodName().toLowerCase();
            double grams = record.getGrams() != null ? record.getGrams() : 0;

            NutritionAnalysisService.FoodNutrition nutrition = nutritionAnalysisService.getFoodNutrition(record.getFoodName());

            // 估算蛋白质
            totalProtein += nutrition.protein * grams / 100;

            // 识别蔬菜
            if (foodName.contains("菜") || foodName.contains("蔬") ||
                    foodName.contains("西兰花") || foodName.contains("菠菜")) {
                totalVegetables += grams;
            }

            // 识别水果
            if (foodName.contains("果") || foodName.contains("梨") ||
                    foodName.contains("桃") || foodName.contains("莓")) {
                totalFruit += grams;
            }

            // 估算水分
            if (foodName.contains("水") || foodName.contains("汤")) {
                totalWater += grams * 0.9;
            }
        }

        double avgVegetables = totalVegetables / dayCount;
        double avgFruit = totalFruit / dayCount;
        double avgProtein = totalProtein / dayCount;
        double avgWater = totalWater / dayCount;
        int foodTypeCount = uniqueFoods.size();

        // 膳食指南推荐值
        Map<String, Double> recommendations = NutritionAnalysisService.RECOMMENDATIONS;

        Map<String, Double> actual = Map.of(
                "vegetables", avgVegetables,
                "fruit", avgFruit,
                "protein", avgProtein,
                "water", avgWater,
                "food_types", (double) foodTypeCount
        );

        comparison.put("actual", actual);
        comparison.put("recommended", recommendations);
        comparison.put("achievement_rate", calculateAchievementRate(
                avgVegetables, avgFruit, avgProtein, 0, avgWater, foodTypeCount
        ));

        return comparison;
    }

    /**
     * 计算达成率
     */
    private double calculateAchievementRate(double veg, double fruit, double protein,
                                            double fiber, double water, int types) {
        int achieved = 0;
        int total = 6;

        if (veg >= 300) achieved++;
        if (fruit >= 200) achieved++;
        if (protein >= 60) achieved++;
        if (fiber >= 25) achieved++;
        if (water >= 1500) achieved++; // 简化计算
        if (types >= 12) achieved++;

        return (double) achieved / total * 100;
    }

    /**
     * 格式化风险分析结果
     */
    private String formatRiskAnalysis(Map<String, Object> risks) {
        StringBuilder sb = new StringBuilder();

        @SuppressWarnings("unchecked")
        List<String> identifiedRisks = (List<String>) risks.get("identifiedRisks");

        sb.append("### 📊 基于《中国居民膳食指南(2022)》的营养评估\n\n");

        if (identifiedRisks.isEmpty()) {
            sb.append("✅ **健康风险评估**：未发现明显饮食相关健康风险\n");
            sb.append("您的饮食模式符合膳食指南建议，继续保持！\n");
        } else {
            sb.append("⚠️ **识别到的健康风险**：\n\n");
            for (String risk : identifiedRisks) {
                sb.append(risk).append("\n\n");
            }
        }

        // 具体数值
        sb.append("---\n");
        sb.append("### 📈 营养摄入详情\n\n");
        sb.append("| 指标 | 实际摄入 | 推荐值 | 状态 |\n");
        sb.append("|------|----------|--------|------|\n");

        NutritionAnalysisService.FiberAnalysisResult fiber = (NutritionAnalysisService.FiberAnalysisResult) risks.get("fiberResult");
        sb.append(String.format("| 膳食纤维 | %.0fg/天 | ≥25g/天 | %s |\n",
                fiber.avgFiber, fiber.avgFiber >= 25 ? "✅" : "⚠️"));

        NutritionAnalysisService.SodiumAnalysisResult sodium = (NutritionAnalysisService.SodiumAnalysisResult) risks.get("sodiumResult");
        sb.append(String.format("| 钠 | %.0fmg/天 | <2000mg/天 | %s |\n",
                sodium.avgSodium, !sodium.isExceeded ? "✅" : "⚠️"));

        NutritionAnalysisService.SugarAnalysisResult sugar = (NutritionAnalysisService.SugarAnalysisResult) risks.get("sugarResult");
        sb.append(String.format("| 添加糖 | %.0fg/天 | <50g/天 | %s |\n",
                sugar.avgSugar, !sugar.isExceeded ? "✅" : "⚠️"));

        NutritionAnalysisService.RedMeatAnalysisResult redMeat = (NutritionAnalysisService.RedMeatAnalysisResult) risks.get("redMeatResult");
        sb.append(String.format("| 红肉 | %.0fg/天 | <100g/天 | %s |\n\n",
                redMeat.avgRedMeat, !redMeat.isExceeded() ? "✅" : "⚠️"));

        sb.append("📖 **参考依据**：\n");
        sb.append("- 《中国居民膳食指南(2022)》中国营养学会\n");
        sb.append("- WHO《成人和儿童糖摄入量指南》(2015)\n");
        sb.append("- 《中国高血压防治指南(2018年修订版)》\n");
        sb.append("- 国际癌症研究机构(IARC)评估报告\n\n");

        return sb.toString();
    }
    /**
     * 格式化模式分析结果
     */
    private String formatPatternAnalysis(Map<String, Object> patterns) {
        StringBuilder sb = new StringBuilder();

        double weekendEffect = (Double) patterns.get("weekendEffect");
        int missedMeals = (Integer) patterns.get("missedMeals");
        int totalExpectedMeals = (Integer) patterns.get("totalExpectedMeals");
        long nightEatingCount = (Long) patterns.get("nightEatingCount");
        int dayCount = (Integer) patterns.get("dayCount");

        // 周末效应分析
        if (weekendEffect > 1.3) {
            sb.append("🌙 **周末暴食模式**：周末日均摄入热量是工作日的").append(String.format("%.1f", weekendEffect)).append("倍\n");
            sb.append("   *建议：周末饮食保持规律，避免暴饮暴食*\n");
        } else if (weekendEffect < 0.7) {
            sb.append("🥗 **周末节食模式**：周末日均摄入热量仅为工作日的").append(String.format("%.1f", weekendEffect)).append("倍\n");
            sb.append("   *建议：周末也要保证充足营养摄入*\n");
        } else {
            sb.append("✅ **饮食规律性**：周末与工作日饮食模式相对均衡\n");
        }

        // 漏餐分析
        double missRate = totalExpectedMeals > 0 ? (double) missedMeals / totalExpectedMeals : 0;
        if (missRate > 0.3) {
            sb.append("\n🍽️ **漏餐情况**：本周漏餐").append(missedMeals).append("次（占比").append(String.format("%.1f%%", missRate * 100)).append("）\n");
            sb.append("   *建议：保证一日三餐规律，避免长时间空腹*\n");
        } else {
            sb.append("\n✅ **用餐规律**：本周漏餐较少，用餐时间相对稳定\n");
        }

        // 夜宵分析
        if (nightEatingCount > 2) {
            sb.append("\n🌙 **夜宵习惯**：本周有").append(nightEatingCount).append("次夜间进食\n");
            sb.append("   *建议：21:00后避免进食，以免影响消化和睡眠*\n");
        } else {
            sb.append("\n✅ **作息规律**：夜间进食次数较少，作息相对健康\n");
        }

        return sb.toString();
    }

    /**
     * 格式化指南对比结果
     */
    private String formatGuidelineComparison(Map<String, Object> comparison) {
        StringBuilder sb = new StringBuilder();

        @SuppressWarnings("unchecked")
        Map<String, Double> actual = (Map<String, Double>) comparison.get("actual");
        @SuppressWarnings("unchecked")
        Map<String, Double> recommended = (Map<String, Double>) comparison.get("recommended");
        double achievementRate = (Double) comparison.get("achievement_rate");

        sb.append("🎯 **膳食指南达成率：").append(String.format("%.1f%%", achievementRate)).append("**\n\n");

        // 具体对比
        sb.append("📋 **各项指标对比**：\n");
        sb.append("- 蔬菜摄入：").append(actual.get("vegetables").intValue()).append("g/天 vs ").append(recommended.get("vegetables").intValue()).append("g/天 ");
        sb.append(actual.get("vegetables") >= recommended.get("vegetables") ? "✅" : "⚠️").append("\n");

        sb.append("- 水果摄入：").append(actual.get("fruit").intValue()).append("g/天 vs ").append(recommended.get("fruit").intValue()).append("g/天 ");
        sb.append(actual.get("fruit") >= recommended.get("fruit") ? "✅" : "⚠️").append("\n");

        sb.append("- 蛋白质摄入：").append(actual.get("protein").intValue()).append("g/天 vs ").append(recommended.get("protein").intValue()).append("g/天 ");
        sb.append(actual.get("protein") >= recommended.get("protein") ? "✅" : "⚠️").append("\n");

        sb.append("- 食物多样性：").append(actual.get("food_types").intValue()).append("种/周 vs ").append(recommended.get("food_types").intValue()).append("种/周 ");
        sb.append(actual.get("food_types") >= recommended.get("food_types") ? "✅" : "⚠️").append("\n");

        if (achievementRate < 60) {
            sb.append("\n💡 **改进建议**：\n");
            if (actual.get("vegetables") < recommended.get("vegetables")) {
                sb.append("- 增加蔬菜摄入，每日至少300g，深色蔬菜占一半\n");
            }
            if (actual.get("fruit") < recommended.get("fruit")) {
                sb.append("- 增加水果摄入，每日200-350g，种类丰富\n");
            }
            if (actual.get("food_types") < recommended.get("food_types")) {
                sb.append("- 增加食物种类，每周至少25种不同食物\n");
            }
        }

        return sb.toString();
    }

    /**
     * 生成改进建议
     */
    private List<String> generateImprovementSuggestions(List<DietRecord> records,
                                                        String constitution, HealthProfile profile) {
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
     * 获取BMI分类
     */
    private String getBMIClassification(double bmi) {
        if (bmi < 18.5) return "偏瘦，建议增加营养摄入";
        else if (bmi < 24) return "正常范围，继续保持";
        else if (bmi < 28) return "超重，建议控制饮食、增加运动";
        else return "肥胖，建议咨询专业营养师";
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