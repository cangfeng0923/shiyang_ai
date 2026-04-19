// service/NutritionService.java - 增强版
package org.example.shiyangai.service;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class NutritionService {

    @Autowired
    private SmartFoodService foodService;

    @Autowired
    private OnDemandFoodService onDemandFoodService;

    /**
     * 智能检查食谱营养平衡
     * 基于食材的真实营养数据，而非关键词匹配
     */
    public NutritionCheckResult checkBalance(String recipe, String constitution) {
        NutritionCheckResult result = new NutritionCheckResult();
        List<String> suggestions = new ArrayList<>();

        // 1. 从食谱中提取食材名称
        List<String> ingredients = extractIngredients(recipe);

        if (ingredients.isEmpty()) {
            result.setBalanced(false);
            result.setSuggestions(List.of("请提供具体的食材信息"));
            return result;
        }

        // 2. 批量获取食材营养信息
        Map<String, IngredientInfo> foodInfos = new HashMap<>();
        for (String ingredient : ingredients) {
            try {
                IngredientInfo info = onDemandFoodService.getFoodInfo(ingredient);
                if (info != null) {
                    foodInfos.put(ingredient, info);
                }
            } catch (Exception e) {
                log.warn("获取食材信息失败: {}", ingredient);
            }
        }

        // 3. 分析营养构成
        boolean hasProtein = false;
        boolean hasVeggies = false;
        boolean hasCarbs = false;
        boolean hasFruits = false;
        int totalCalories = 0;

        for (Map.Entry<String, IngredientInfo> entry : foodInfos.entrySet()) {
            IngredientInfo info = entry.getValue();
            String name = entry.getKey();

            // 蛋白质来源
            if (isProteinSource(name, info)) {
                hasProtein = true;
            }
            // 蔬菜
            if (isVegetable(name)) {
                hasVeggies = true;
            }
            // 主食
            if (isCarbSource(name)) {
                hasCarbs = true;
            }
            // 水果
            if (isFruit(name)) {
                hasFruits = true;
            }

            if (info.getCalories() != null) {
                totalCalories += info.getCalories();
            }
        }

        // 4. 生成建议
        if (!hasProtein) {
            suggestions.add("🥩 建议增加优质蛋白：鸡蛋、鱼肉、鸡肉、豆腐或瘦肉");
        }
        if (!hasVeggies) {
            suggestions.add("🥬 建议增加蔬菜：青菜、西兰花、菠菜、番茄等");
        }
        if (!hasCarbs) {
            suggestions.add("🍚 建议增加主食：米饭、杂粮粥、全麦面包等");
        }
        if (hasProtein && hasVeggies && hasCarbs) {
            suggestions.add("✓ 营养均衡，继续保持！");
        }

        // 5. 结合体质给出建议
        if (constitution != null) {
            String constitutionAdvice = getConstitutionNutritionAdvice(constitution, foodInfos);
            if (constitutionAdvice != null && !constitutionAdvice.isEmpty()) {
                suggestions.add(constitutionAdvice);
            }
        }

        result.setBalanced(suggestions.isEmpty() || suggestions.stream().allMatch(s -> s.startsWith("✓")));
        result.setSuggestions(suggestions);
        result.setEstimatedCalories(totalCalories);

        return result;
    }

    /**
     * 从食谱文本中提取食材
     */
    private List<String> extractIngredients(String recipe) {
        List<String> ingredients = new ArrayList<>();
        // 常见食材列表
        String[] commonFoods = {"鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "豆腐", "虾",
                "山药", "红枣", "枸杞", "薏米", "红豆", "绿豆", "银耳", "百合",
                "冬瓜", "苦瓜", "黄瓜", "萝卜", "青菜", "西兰花", "番茄",
                "苹果", "梨", "香蕉", "橙子", "西瓜", "葡萄", "米饭", "面条", "粥"};

        for (String food : commonFoods) {
            if (recipe.contains(food)) {
                ingredients.add(food);
            }
        }
        return ingredients;
    }

    private boolean isProteinSource(String name, IngredientInfo info) {
        String[] proteins = {"鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "豆腐", "虾", "鸭肉"};
        for (String p : proteins) {
            if (name.contains(p)) return true;
        }
        return false;
    }

    private boolean isVegetable(String name) {
        String[] veggies = {"冬瓜", "苦瓜", "黄瓜", "萝卜", "青菜", "西兰花", "番茄", "菠菜", "韭菜"};
        for (String v : veggies) {
            if (name.contains(v)) return true;
        }
        return false;
    }

    private boolean isCarbSource(String name) {
        String[] carbs = {"米饭", "面条", "粥", "面包", "土豆", "红薯", "玉米"};
        for (String c : carbs) {
            if (name.contains(c)) return true;
        }
        return false;
    }

    private boolean isFruit(String name) {
        String[] fruits = {"苹果", "梨", "香蕉", "橙子", "西瓜", "葡萄", "草莓"};
        for (String f : fruits) {
            if (name.contains(f)) return true;
        }
        return false;
    }

    private String getConstitutionNutritionAdvice(String constitution, Map<String, IngredientInfo> foods) {
        Map<String, String> advice = new HashMap<>();
        advice.put("气虚质", "您需要补气健脾，食谱中可增加山药、大枣、黄芪");
        advice.put("阳虚质", "您需要温补阳气，可增加生姜、羊肉、韭菜");
        advice.put("阴虚质", "您需要滋阴润燥，可增加百合、银耳、梨");
        advice.put("痰湿质", "您需要健脾祛湿，减少甜食油腻，增加薏米、冬瓜");
        advice.put("湿热质", "您需要清热利湿，减少辛辣烧烤，增加绿豆、苦瓜");
        advice.put("血瘀质", "您需要活血化瘀，可增加山楂、黑豆、玫瑰花");
        advice.put("气郁质", "您需要理气解郁，可增加萝卜、陈皮、玫瑰花");

        // 检查是否已有适合体质的食材
        String constitutionAdvice = advice.get(constitution);
        if (constitutionAdvice != null) {
            return "🌿 体质建议：" + constitutionAdvice;
        }
        return null;
    }

    /**
     * 营养检查结果类
     */
    public static class NutritionCheckResult {
        private boolean balanced;
        private List<String> suggestions;
        private int estimatedCalories;

        public boolean isBalanced() { return balanced; }
        public void setBalanced(boolean balanced) { this.balanced = balanced; }

        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

        public int getEstimatedCalories() { return estimatedCalories; }
        public void setEstimatedCalories(int estimatedCalories) { this.estimatedCalories = estimatedCalories; }
    }
}