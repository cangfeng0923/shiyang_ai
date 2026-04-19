// service/AgentFoodService.java - 修复版本
package org.example.shiyangai.service;

import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentFoodService {

    @Autowired
    private SmartFoodService foodService;

    @Autowired
    private SmartRecipeGenerator recipeGenerator;

    @Autowired
    private NutritionService nutritionService;

    /**
     * 生成智能膳食计划
     */
    public String generateSmartMealPlan(String userId, String userMessage, String constitution) {

        // 1. 解析用户意图
        UserIntent intent = parseIntent(userMessage);

        // 2. 获取可用食材
        List<String> availableFoods = getAvailableFoods(intent.getPreferences());

        // 3. 获取食材知识
        Map<String, IngredientInfo> foodKnowledge = new HashMap<>();
        for (String food : availableFoods) {
            foodKnowledge.put(food, foodService.getFoodInfo(food));
        }

        // 4. 生成食谱
        String recipe = recipeGenerator.generateRecipe(
                constitution, availableFoods, intent.getMealType()
        );

        // 5. 验证营养平衡 - 修复：传入两个参数（recipe 和 constitution）
        NutritionService.NutritionCheckResult nutritionCheck = nutritionService.checkBalance(recipe, constitution);
        if (!nutritionCheck.isBalanced()) {
            recipe = adjustRecipe(recipe, nutritionCheck.getSuggestions());
        }

        return recipe;
    }

    /**
     * 解析用户意图
     */
    private UserIntent parseIntent(String userMessage) {
        UserIntent intent = new UserIntent();

        String lowerMsg = userMessage.toLowerCase();

        // 识别动作
        if (lowerMsg.contains("食谱") || lowerMsg.contains("推荐") || lowerMsg.contains("吃")) {
            intent.setAction("generate_recipe");
        } else if (lowerMsg.contains("分析") || lowerMsg.contains("能不能吃")) {
            intent.setAction("analyze_food");
        } else if (lowerMsg.contains("替代") || lowerMsg.contains("换")) {
            intent.setAction("substitute_food");
        } else {
            intent.setAction("chat");
        }

        // 识别餐型
        if (lowerMsg.contains("早餐")) {
            intent.setMealType("早餐");
        } else if (lowerMsg.contains("午餐")) {
            intent.setMealType("午餐");
        } else if (lowerMsg.contains("晚餐")) {
            intent.setMealType("晚餐");
        } else {
            intent.setMealType("正餐");
        }

        // 提取偏好食材
        List<String> preferences = extractPreferences(userMessage);
        intent.setPreferences(preferences);

        return intent;
    }

    /**
     * 提取偏好食材
     */
    private List<String> extractPreferences(String message) {
        List<String> preferences = new ArrayList<>();

        // 常见食材关键词
        String[] commonFoods = {"鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "豆腐",
                "山药", "红枣", "枸杞", "薏米", "红豆", "绿豆",
                "苹果", "香蕉", "橙子", "西瓜", "葡萄"};

        for (String food : commonFoods) {
            if (message.contains(food)) {
                preferences.add(food);
            }
        }

        // 口味偏好
        if (message.contains("清淡")) {
            preferences.add("清淡");
        }
        if (message.contains("辣") || message.contains("麻辣")) {
            preferences.add("辣");
        }

        return preferences;
    }

    /**
     * 获取可用食材
     */
    private List<String> getAvailableFoods(List<String> preferences) {
        Set<String> foods = new HashSet<>();

        // 添加用户偏好的食材
        if (preferences != null) {
            foods.addAll(preferences);
        }

        // 添加基础食材库
        foods.addAll(getBaseFoods());

        // 添加当季食材
        foods.addAll(getSeasonalFoods());

        return new ArrayList<>(foods);
    }

    /**
     * 基础食材库
     */
    private List<String> getBaseFoods() {
        return Arrays.asList(
                "山药", "红枣", "枸杞", "薏米", "红豆", "绿豆", "黑芝麻",
                "鸡肉", "鸭肉", "鱼肉", "鸡蛋", "豆腐", "豆浆",
                "冬瓜", "苦瓜", "黄瓜", "萝卜", "青菜", "西兰花",
                "苹果", "梨", "香蕉", "橙子"
        );
    }

    /**
     * 当季食材
     */
    private List<String> getSeasonalFoods() {
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (month >= 3 && month <= 5) {
            return Arrays.asList("春笋", "荠菜", "菠菜", "韭菜");
        } else if (month >= 6 && month <= 8) {
            return Arrays.asList("冬瓜", "苦瓜", "绿豆", "西瓜", "黄瓜");
        } else if (month >= 9 && month <= 11) {
            return Arrays.asList("山药", "百合", "银耳", "梨", "莲藕");
        } else {
            return Arrays.asList("羊肉", "红枣", "桂圆", "生姜", "萝卜");
        }
    }

    /**
     * 调整食谱
     */
    private String adjustRecipe(String recipe, List<String> suggestions) {
        StringBuilder adjusted = new StringBuilder();
        adjusted.append(recipe);
        adjusted.append("\n\n【营养优化建议】\n");
        for (String suggestion : suggestions) {
            adjusted.append("• ").append(suggestion).append("\n");
        }
        return adjusted.toString();
    }

    // ========== 内部类 ==========

    /**
     * 用户意图类
     */
    public static class UserIntent {
        private String action;
        private String mealType;
        private List<String> preferences;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }

        public List<String> getPreferences() { return preferences; }
        public void setPreferences(List<String> preferences) { this.preferences = preferences; }
    }
}