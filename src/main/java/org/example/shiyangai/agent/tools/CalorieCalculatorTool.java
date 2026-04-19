package org.example.shiyangai.agent.tools;

import org.example.shiyangai.agent.Tool;
import org.example.shiyangai.agent.ToolExecutor;
import org.springframework.stereotype.Component;
import java.util.Map;

//热量计算工具
@Component
public class CalorieCalculatorTool implements ToolExecutor {

    private static final Map<String, Double> FOOD_CALORIES = Map.ofEntries(
            Map.entry("炸鸡", 320.0),
            Map.entry("苹果", 52.0),
            Map.entry("米饭", 130.0),
            Map.entry("牛肉", 250.0),
            Map.entry("鸡肉", 165.0),
            Map.entry("鱼肉", 150.0),
            Map.entry("鸡蛋", 70.0),
            Map.entry("牛奶", 60.0),
            Map.entry("西瓜", 30.0),
            Map.entry("蛋糕", 350.0),
            Map.entry("巧克力", 550.0)
    );
    @Override
    @Tool(name = "calculate_calorie",
            description = "计算食物的热量（每100克）",
            parameters = {"foodName", "grams"})
    public String execute(Map<String, Object> parameters) {
        String foodName = (String) parameters.get("foodName");
        int grams = parameters.containsKey("grams") ?
                ((Number) parameters.get("grams")).intValue() : 100;

        Double caloriePer100g = FOOD_CALORIES.getOrDefault(foodName, 100.0);
        double totalCalories = caloriePer100g * grams / 100;

        return String.format("%s%d克的热量约为%.0f千卡，每100克约%.0f千卡",
                foodName, grams, totalCalories, caloriePer100g);
    }

    @Override
    public String getName() { return "calculate_calorie"; }

    @Override
    public String getDescription() {
        return "计算指定食物的热量，需要食物名称和重量（克）";
    }
}