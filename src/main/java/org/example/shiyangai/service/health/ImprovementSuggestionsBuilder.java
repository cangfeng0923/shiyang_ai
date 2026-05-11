package org.example.shiyangai.service.health;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 改进建议生成器
 * 职责：基于饮食记录生成个性化的改进建议
 */
@Slf4j
@Component
public class ImprovementSuggestionsBuilder {

    public String build(List<DietRecord> weekRecords, String constitution) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---\n");
        sb.append("### 6️⃣ 改进建议\n\n");

        List<String> suggestions = generateImprovementSuggestions(weekRecords, constitution);
        for (int i = 0; i < suggestions.size(); i++) {
            sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 生成改进建议列表
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
        String constitutionAdvice = getConstitutionAdvice(constitution);
        if (constitutionAdvice != null) {
            suggestions.add(constitutionAdvice);
        }

        // 通用建议
        suggestions.add("💧 每天喝够1.5-2升水，可以喝养生茶代替白开水");
        suggestions.add("😴 保证7-8小时睡眠，晚上11点前入睡最佳");

        return suggestions;
    }

    private String getConstitutionAdvice(String constitution) {
        Map<String, String> constitutionAdviceMap = new HashMap<>();
        constitutionAdviceMap.put("气虚质", "🌿 建议多吃山药、大枣、黄芪炖鸡，补气健脾");
        constitutionAdviceMap.put("阳虚质", "🔥 建议多吃生姜、羊肉、韭菜，温补阳气");
        constitutionAdviceMap.put("阴虚质", "💧 建议多吃百合、银耳、梨，滋阴润燥");
        constitutionAdviceMap.put("痰湿质", "🌾 建议少吃甜食油腻，多吃薏米、冬瓜祛湿");
        constitutionAdviceMap.put("湿热质", "🥒 建议少吃辛辣烧烤，多吃绿豆、苦瓜清热");
        constitutionAdviceMap.put("血瘀质", "🍒 建议多吃山楂、黑豆、玫瑰花茶，活血化瘀");
        constitutionAdviceMap.put("气郁质", "🌹 建议喝玫瑰花茶、多交朋友，疏肝解郁");

        return constitutionAdviceMap.get(constitution);
    }
}