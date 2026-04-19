package org.example.shiyangai.agent.tools;

import org.example.shiyangai.agent.Tool;
import org.example.shiyangai.agent.ToolExecutor;
import org.example.shiyangai.enums.ConstitutionType;
import org.example.shiyangai.service.NutritionApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class FoodSearchTool implements ToolExecutor {

    @Autowired
    private NutritionApiService nutritionApiService;

    @Override
    @Tool(name = "search_food",
            description = "搜索食物的营养信息和中医属性",
            parameters = {"foodName", "constitution"})
    public String execute(Map<String, Object> parameters) {
        String foodName = (String) parameters.get("foodName");
        String constitution = (String) parameters.get("constitution");

        // 1. 获取营养数据
        Map<String, Object> nutrition = nutritionApiService.getNutrition(foodName);

        // 2. 获取中医属性
        ConstitutionType type = ConstitutionType.fromName(constitution);
        var result = type.evaluate(foodName);

        // 3. 构建返回结果
        StringBuilder sb = new StringBuilder();
        sb.append("食物: ").append(foodName).append("\n");
        sb.append("营养: ").append(nutrition.getOrDefault("calories", "未知")).append("\n");
        sb.append("中医评价: ").append(result.getSuitability()).append("\n");
        sb.append("分析: ").append(result.getReason()).append("\n");
        sb.append("建议: ").append(result.getSuggestion());

        return sb.toString();
    }

    @Override
    public String getName() { return "search_food"; }

    @Override
    public String getDescription() {
        return "搜索食物的营养信息和中医属性，返回该食物对特定体质的适宜性";
    }
}