package org.example.shiyangai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 营养分析服务 - 基于《中国居民膳食指南(2022)》和WHO建议
 * 参考来源：
 * - 《中国居民膳食指南(2022)》中国营养学会
 * - WHO《成人和儿童糖摄入量指南》(2015)
 * - 《中国高血压防治指南(2018年修订版)》
 * - 《中国2型糖尿病防治指南(2020年版)》
 */
@Slf4j
@Service
public class NutritionAnalysisService {

    // ========== 膳食指南推荐值（基于《中国居民膳食指南2022》） ==========
    public static final Map<String, Double> RECOMMENDATIONS = Map.of(
            "vegetables", 300.0,      // 蔬菜 300-500g/天
            "fruit", 200.0,           // 水果 200-350g/天
            "protein", 60.0,          // 蛋白质 60-75g/天
            "fiber", 25.0,            // 膳食纤维 25-30g/天
            "water", 1500.0,          // 饮水 1500-1700ml/天
            "food_types", 12.0,       // 食物种类 12种/周（建议25种）
            "sodium", 2000.0,         // 钠 <2000mg/天（约5g盐）
            "sugar", 50.0,            // 添加糖 <50g/天（WHO建议）
            "red_meat", 100.0,        // 红肉 <100g/天（中国居民膳食指南）
            "whole_grain", 50.0       // 全谷物 50-150g/天
    );

    // ========== 食物营养成分数据库（基于《中国食物成分表》） ==========
    private static final Map<String, FoodNutrition> FOOD_NUTRITION_DB = new HashMap<>();

    static {
        // 初始化常见食物的营养成分（基于《中国食物成分表》标准版第6版）
        // 蔬菜类
        addNutrition("西兰花", 34, 2.8, 0.5, 2.6, 0.4, 10);
        addNutrition("菠菜", 28, 2.6, 0.3, 2.2, 0.5, 20);
        addNutrition("西红柿", 20, 0.9, 0.2, 3.4, 0.2, 8);
        addNutrition("黄瓜", 15, 0.8, 0.1, 2.5, 0.3, 5);
        addNutrition("胡萝卜", 41, 1.0, 0.2, 8.8, 0.5, 15);
        addNutrition("白菜", 18, 1.5, 0.1, 2.8, 0.4, 12);
        addNutrition("茄子", 25, 1.1, 0.2, 4.5, 0.5, 8);
        addNutrition("冬瓜", 12, 0.4, 0.2, 2.4, 0.3, 6);

        // 水果类
        addNutrition("苹果", 53, 0.3, 0.2, 13.5, 0.8, 15);
        addNutrition("香蕉", 93, 1.1, 0.3, 22.0, 2.6, 12);
        addNutrition("橙子", 47, 0.9, 0.2, 11.1, 1.4, 10);
        addNutrition("猕猴桃", 61, 1.1, 0.5, 13.5, 2.5, 12);

        // 蛋白质类
        addNutrition("鸡蛋", 143, 13.3, 8.8, 1.5, 0, 0);
        addNutrition("鸡腿", 181, 16.0, 12.2, 0, 0, 0);
        addNutrition("鸡胸肉", 133, 24.6, 3.0, 0, 0, 0);
        addNutrition("牛肉", 125, 20.0, 4.2, 2.0, 0, 0);
        addNutrition("猪肉", 395, 14.6, 37.0, 0, 0, 0);
        addNutrition("鱼肉", 108, 18.0, 3.5, 0, 0, 0);
        addNutrition("豆腐", 82, 8.1, 4.2, 3.5, 1.5, 5);
        addNutrition("豆浆", 31, 3.0, 1.6, 1.8, 0.5, 3);
        addNutrition("牛奶", 54, 3.0, 3.2, 3.4, 0, 0);

        // 谷物类
        addNutrition("米饭", 116, 2.6, 0.3, 25.6, 0.5, 5);
        addNutrition("馒头", 223, 7.0, 1.0, 47.0, 1.5, 8);
        addNutrition("面条", 138, 4.5, 0.5, 29.0, 1.0, 6);
        addNutrition("燕麦", 368, 15.0, 6.5, 66.0, 10.5, 15);
        addNutrition("玉米", 112, 4.0, 1.5, 19.0, 2.5, 10);
        addNutrition("红薯", 99, 1.2, 0.2, 23.0, 2.0, 8);

        // 饮品类
        addNutrition("咖啡", 2, 0.2, 0, 0.3, 0, 0);
        addNutrition("茶", 1, 0.1, 0, 0.2, 0, 0);
        addNutrition("可乐", 41, 0, 0, 10.4, 0, 0);

        // 调味品
        addNutrition("盐", 0, 0, 0, 0, 0, 38758); // 钠含量mg/g
        addNutrition("糖", 387, 0, 0, 99.5, 0, 0);
        addNutrition("酱油", 60, 2.0, 0, 10.0, 0, 5500); // 钠含量mg/100g
    }

    private static void addNutrition(String name, int calories, double protein, double fat, double carbs, double fiber, int sodium) {
        FOOD_NUTRITION_DB.put(name, new FoodNutrition(calories, protein, fat, carbs, fiber, sodium));
    }

    /**
     * 获取食物营养信息
     */
    public FoodNutrition getFoodNutrition(String foodName) {
        // 精确匹配
        if (FOOD_NUTRITION_DB.containsKey(foodName)) {
            return FOOD_NUTRITION_DB.get(foodName);
        }
        // 模糊匹配
        for (Map.Entry<String, FoodNutrition> entry : FOOD_NUTRITION_DB.entrySet()) {
            if (foodName.contains(entry.getKey()) || entry.getKey().contains(foodName)) {
                return entry.getValue();
            }
        }
        // 返回默认值
        return new FoodNutrition(100, 5.0, 5.0, 15.0, 2.0, 100);
    }

    /**
     * 分析膳食纤维摄入（基于《中国居民膳食指南》）
     * 推荐值：25-30g/天
     */
    public FiberAnalysisResult analyzeFiberIntake(List<DietRecord> records, int dayCount) {
        double totalFiber = 0;
        List<String> fiberSources = new ArrayList<>();

        for (DietRecord record : records) {
            FoodNutrition nutrition = getFoodNutrition(record.getFoodName());
            if (nutrition.fiber > 0 && record.getGrams() != null) {
                double fiberGram = nutrition.fiber * record.getGrams() / 100;
                totalFiber += fiberGram;
                if (fiberGram > 0.5) {
                    fiberSources.add(record.getFoodName());
                }
            }
        }

        double avgFiber = dayCount > 0 ? totalFiber / dayCount : 0;

        FiberAnalysisResult result = new FiberAnalysisResult();
        result.avgFiber = avgFiber;
        result.isInsufficient = avgFiber < 25;
        result.isSevereInsufficient = avgFiber < 15;
        result.fiberSources = fiberSources.stream().distinct().limit(5).collect(Collectors.toList());

        if (avgFiber < 15) {
            result.riskLevel = "high";
            result.riskDescription = "膳食纤维严重不足（<15g/天），肠道健康风险较高";
            result.suggestion = "膳食纤维严重不足，建议：\n- 每天主食中增加1/3全谷物（燕麦、糙米、玉米）\n- 每天吃500g蔬菜，其中深色蔬菜占一半\n- 每天吃200-350g水果";
        } else if (avgFiber < 25) {
            result.riskLevel = "medium";
            result.riskDescription = "膳食纤维摄入不足，建议增加到25g/天";
            result.suggestion = "膳食纤维略低于推荐量，建议：\n- 增加蔬菜摄入量至300-500g/天\n- 用杂粮饭代替白米饭\n- 每天吃一个水果";
        } else {
            result.riskLevel = "low";
            result.riskDescription = "膳食纤维摄入充足，继续保持";
            result.suggestion = "膳食纤维摄入达标，继续保持良好的饮食习惯！";
        }

        return result;
    }

    /**
     * 分析钠摄入（基于《中国高血压防治指南2018》）
     * 推荐：<2000mg/天（约5g盐）
     */
    public SodiumAnalysisResult analyzeSodiumIntake(List<DietRecord> records, int dayCount) {
        double totalSodium = 0;
        List<String> highSodiumFoods = new ArrayList<>();

        for (DietRecord record : records) {
            FoodNutrition nutrition = getFoodNutrition(record.getFoodName());
            if (nutrition.sodium > 0 && record.getGrams() != null) {
                double sodiumMg = nutrition.sodium * record.getGrams() / 100;
                totalSodium += sodiumMg;
                if (sodiumMg > 500) { // 单次超过500mg钠
                    highSodiumFoods.add(record.getFoodName());
                }
            }
        }

        double avgSodium = dayCount > 0 ? totalSodium / dayCount : 0;

        SodiumAnalysisResult result = new SodiumAnalysisResult();
        result.avgSodium = avgSodium;
        result.isExceeded = avgSodium > 2000;
        result.isSevereExceeded = avgSodium > 3000;
        result.highSodiumFoods = highSodiumFoods.stream().distinct().limit(5).collect(Collectors.toList());

        if (avgSodium > 3000) {
            result.riskLevel = "high";
            result.riskDescription = String.format("钠摄入严重超标（%.0fmg/天），高血压风险显著升高", avgSodium);
            result.suggestion = "钠摄入严重超标，建议：\n- 每天食盐控制在5g以内（约一个啤酒瓶盖）\n- 减少酱油、味精、咸菜、加工肉制品\n- 使用香料（葱姜蒜、花椒）替代部分盐调味\n- 建议连续3天早晨测血压";
        } else if (avgSodium > 2000) {
            result.riskLevel = "medium";
            result.riskDescription = String.format("钠摄入超标（%.0fmg/天），血压健康需关注", avgSodium);
            result.suggestion = "钠摄入偏高，建议：\n- 减少外卖和加工食品\n- 烹饪时晚放盐，减少用盐量\n- 建议监测血压变化";
        } else {
            result.riskLevel = "low";
            result.riskDescription = "钠摄入控制在合理范围，对血压健康有利";
            result.suggestion = "钠摄入控制良好，继续坚持低盐饮食！";
        }

        return result;
    }

    /**
     * 分析糖摄入（基于WHO《成人和儿童糖摄入量指南》2015）
     * 推荐：添加糖 <50g/天，最好 <25g/天
     */
    public SugarAnalysisResult analyzeSugarIntake(List<DietRecord> records, int dayCount) {
        double totalSugar = 0;
        List<String> sugarSources = new ArrayList<>();

        for (DietRecord record : records) {
            String foodName = record.getFoodName().toLowerCase();
            double sugarGram = 0;

            // 估算添加糖含量
            if (foodName.contains("可乐") || foodName.contains("雪碧")) {
                sugarGram = 10.6 * (record.getGrams() != null ? record.getGrams() / 330 : 1);
                sugarSources.add(record.getFoodName());
            } else if (foodName.contains("奶茶")) {
                sugarGram = 30;
                sugarSources.add(record.getFoodName());
            } else if (foodName.contains("蛋糕") || foodName.contains("甜点")) {
                sugarGram = 20;
                sugarSources.add(record.getFoodName());
            } else if (foodName.contains("糖果") || foodName.contains("巧克力")) {
                sugarGram = 15;
                sugarSources.add(record.getFoodName());
            } else if (foodName.contains("饼干")) {
                sugarGram = 10;
                sugarSources.add(record.getFoodName());
            } else if (foodName.contains("糖") && record.getGrams() != null) {
                sugarGram = record.getGrams();
                sugarSources.add(record.getFoodName());
            }

            totalSugar += sugarGram;
        }

        double avgSugar = dayCount > 0 ? totalSugar / dayCount : 0;

        SugarAnalysisResult result = new SugarAnalysisResult();
        result.avgSugar = avgSugar;
        result.isExceeded = avgSugar > 50;
        result.isHigh = avgSugar > 25;
        result.sugarSources = sugarSources.stream().distinct().limit(5).collect(Collectors.toList());

        if (avgSugar > 50) {
            result.riskLevel = "high";
            result.riskDescription = String.format("添加糖摄入严重超标（%.0fg/天），糖尿病前期风险显著", avgSugar);
            result.suggestion = "糖分摄入已超过WHO建议上限，建议：\n- 避免含糖饮料，改喝白水或无糖茶\n- 减少甜点、糖果、蛋糕的摄入\n- 建议检查空腹血糖和糖化血红蛋白\n- 选择天然水果替代甜食";
        } else if (avgSugar > 25) {
            result.riskLevel = "medium";
            result.riskDescription = String.format("添加糖摄入偏高（%.0fg/天），建议控制在25g以内", avgSugar);
            result.suggestion = "糖分摄入偏高，建议：\n- 减少奶茶、可乐等含糖饮料\n- 选择无糖替代品\n- 注意隐形糖（酱料、加工食品）";
        } else {
            result.riskLevel = "low";
            result.riskDescription = "糖摄入控制在合理范围，对血糖健康有利";
            result.suggestion = "糖摄入控制良好，继续保持！";
        }

        return result;
    }

    /**
     * 分析红肉摄入（基于《中国居民膳食指南》和IARC建议）
     * 推荐：红肉 <100g/天，加工肉制品尽量少吃
     */
    public RedMeatAnalysisResult analyzeRedMeatIntake(List<DietRecord> records, int dayCount) {
        double totalRedMeat = 0;
        double totalProcessedMeat = 0;
        List<String> redMeatItems = new ArrayList<>();

        for (DietRecord record : records) {
            String foodName = record.getFoodName().toLowerCase();
            double weight = record.getGrams() != null ? record.getGrams() : 0;

            if (foodName.contains("猪肉") || foodName.contains("牛肉") || foodName.contains("羊肉")) {
                totalRedMeat += weight;
                redMeatItems.add(record.getFoodName());
            }
            if (foodName.contains("香肠") || foodName.contains("火腿") || foodName.contains("培根") || foodName.contains("腊肉")) {
                totalProcessedMeat += weight;
                redMeatItems.add(record.getFoodName());
            }
        }

        double avgRedMeat = dayCount > 0 ? totalRedMeat / dayCount : 0;

        RedMeatAnalysisResult result = new RedMeatAnalysisResult();
        result.avgRedMeat = avgRedMeat;
        result.totalProcessedMeat = totalProcessedMeat;
        result.redMeatItems = redMeatItems.stream().distinct().limit(5).collect(Collectors.toList());

        if (avgRedMeat > 100) {
            result.riskLevel = "high";
            result.riskDescription = String.format("红肉摄入过多（%.0fg/天），结直肠癌风险略增", avgRedMeat);
            result.suggestion = "红肉摄入偏高，建议：\n- 每周红肉控制在500g以内\n- 增加鱼、禽、豆制品替代部分红肉\n- 增加膳食纤维摄入（蔬菜、全谷物）\n- 避免加工肉制品（香肠、培根等已被WHO列为1类致癌物）";
        } else if (avgRedMeat > 70) {
            result.riskLevel = "medium";
            result.riskDescription = String.format("红肉摄入偏高（%.0fg/天），建议控制", avgRedMeat);
            result.suggestion = "红肉摄入略高，建议：\n- 适当减少红肉，增加鱼和禽肉\n- 每周安排1-2天素食";
        } else {
            result.riskLevel = "low";
            result.riskDescription = "红肉摄入控制在推荐范围";
            result.suggestion = "红肉摄入合理，继续保持！";
        }

        if (totalProcessedMeat > 0) {
            result.suggestion += "\n- ⚠️ 加工肉制品（香肠、培根、火腿）已被WHO列为1类致癌物，建议尽量少吃或不吃";
        }

        return result;
    }

    /**
     * 计算营养膳食评分（基于膳食指南）
     */
    public DietaryScoreResult calculateDietaryScore(List<DietRecord> records, int dayCount) {
        DietaryScoreResult result = new DietaryScoreResult();

        // 计算各指标
        FiberAnalysisResult fiber = analyzeFiberIntake(records, dayCount);
        SodiumAnalysisResult sodium = analyzeSodiumIntake(records, dayCount);
        SugarAnalysisResult sugar = analyzeSugarIntake(records, dayCount);
        RedMeatAnalysisResult redMeat = analyzeRedMeatIntake(records, dayCount);

        result.fiberResult = fiber;
        result.sodiumResult = sodium;
        result.sugarResult = sugar;
        result.redMeatResult = redMeat;

        // 计算总分（满分100）
        int totalScore = 70;

        if (!fiber.isInsufficient) totalScore += 10;
        else if (fiber.isSevereInsufficient) totalScore -= 10;

        if (!sodium.isExceeded) totalScore += 10;
        else if (sodium.isSevereExceeded) totalScore -= 10;

        if (!sugar.isExceeded) totalScore += 10;
        else if (sugar.isHigh) totalScore -= 5;

        if (!redMeat.isExceeded()) totalScore += 5;
        else if (redMeat.riskLevel.equals("high")) totalScore -= 10;

        result.totalScore = Math.max(0, Math.min(100, totalScore));

        if (result.totalScore >= 80) {
            result.grade = "excellent";
            result.gradeText = "优秀";
            result.summary = "您的饮食结构非常健康，符合膳食指南建议，请继续保持！";
        } else if (result.totalScore >= 60) {
            result.grade = "good";
            result.gradeText = "良好";
            result.summary = "饮食基本健康，有优化空间，关注上述改进建议。";
        } else {
            result.grade = "needImprove";
            result.gradeText = "需改进";
            result.summary = "饮食结构需要改善，请重点关注膳食纤维、钠、糖的摄入。";
        }

        return result;
    }

    // ========== 内部类 ==========

    public static class FoodNutrition {
        public final int calories;
        public final double protein;
        public final double fat;
        public final double carbs;
        public final double fiber;
        public final int sodium;

        public FoodNutrition(int calories, double protein, double fat, double carbs, double fiber, int sodium) {
            this.calories = calories;
            this.protein = protein;
            this.fat = fat;
            this.carbs = carbs;
            this.fiber = fiber;
            this.sodium = sodium;
        }
    }

    public static class FiberAnalysisResult {
        public double avgFiber;
        public boolean isInsufficient;
        public boolean isSevereInsufficient;
        public String riskLevel;
        public String riskDescription;
        public String suggestion;
        public List<String> fiberSources;
    }

    public static class SodiumAnalysisResult {
        public double avgSodium;
        public boolean isExceeded;
        public boolean isSevereExceeded;
        public String riskLevel;
        public String riskDescription;
        public String suggestion;
        public List<String> highSodiumFoods;
    }

    public static class SugarAnalysisResult {
        public double avgSugar;
        public boolean isExceeded;
        public boolean isHigh;
        public String riskLevel;
        public String riskDescription;
        public String suggestion;
        public List<String> sugarSources;
    }

    public static class RedMeatAnalysisResult {
        public double avgRedMeat;
        public double totalProcessedMeat;
        public String riskLevel;
        public String riskDescription;
        public String suggestion;
        public List<String> redMeatItems;

        public boolean isExceeded() {
            return "high".equals(riskLevel) || "medium".equals(riskLevel);
        }
    }

    public static class DietaryScoreResult {
        public int totalScore;
        public String grade;
        public String gradeText;
        public String summary;
        public FiberAnalysisResult fiberResult;
        public SodiumAnalysisResult sodiumResult;
        public SugarAnalysisResult sugarResult;
        public RedMeatAnalysisResult redMeatResult;
    }
}
