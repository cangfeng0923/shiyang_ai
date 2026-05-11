package org.example.shiyangai.service.health;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 饮食总结章节构建器
 * 职责：构建报告中的饮食统计和总结部分
 */
@Slf4j
@Component
public class DietSummarySectionBuilder {

    /**
     * 构建本周饮食总结
     */
    public String buildWeekSummary(List<DietRecord> weekRecords) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---\n");
        sb.append("### 2️⃣ 本周饮食总结\n\n");

        if (weekRecords.isEmpty()) {
            sb.append("> 📝 本周暂无饮食记录，开始记录您的饮食吧！\n\n");
            sb.append("记录饮食后，我可以为您：\n");
            sb.append("- 分析营养均衡度\n");
            sb.append("- 给出健康评分\n");
            sb.append("- 推荐适合您体质的食物\n");
            return sb.toString();
        }

        // 统计各餐次
        Map<String, Long> mealCount = weekRecords.stream()
                .collect(Collectors.groupingBy(DietRecord::getMealType, Collectors.counting()));

        sb.append("📈 **统计数据**：\n");
        sb.append("- 总记录餐数：").append(weekRecords.size()).append("餐\n");
        sb.append("- 早餐：").append(mealCount.getOrDefault("BREAKFAST", 0L)).append("次\n");
        sb.append("- 午餐：").append(mealCount.getOrDefault("LUNCH", 0L)).append("次\n");
        sb.append("- 晚餐：").append(mealCount.getOrDefault("DINNER", 0L)).append("次\n");
        sb.append("- 加餐：").append(mealCount.getOrDefault("SNACK", 0L)).append("次\n");

        // 平均评分
        double avgScore = weekRecords.stream()
                .mapToInt(DietRecord::getHealthScore)
                .average()
                .orElse(0);
        sb.append(String.format("\n⭐ **平均健康评分**：%.0f/100\n", avgScore));

        sb.append(getScoreComment(avgScore));

        // 高频食物统计
        appendTopFoods(sb, weekRecords);

        return sb.toString();
    }

    /**
     * 构建今日饮食总结
     */
    public String buildTodaySummary(List<DietRecord> todayRecords) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---\n");
        sb.append("### 3️⃣ 今日饮食\n\n");

        if (todayRecords.isEmpty()) {
            sb.append("> 📝 今日尚未记录饮食\n\n");
            return sb.toString();
        }

        for (DietRecord record : todayRecords) {
            String mealName = getMealName(record.getMealType());
            sb.append(String.format("**%s**：%s %sg（评分：%d/100）\n",
                    mealName, record.getFoodName(),
                    record.getGrams() != null ? record.getGrams() : 0,
                    record.getHealthScore()));
        }

        return sb.toString();
    }

    private String getScoreComment(double avgScore) {
        if (avgScore >= 80) {
            return "🎉 优秀！您的饮食习惯非常好，请继续保持！\n";
        } else if (avgScore >= 60) {
            return "👍 良好！还有一些提升空间，可以进一步优化饮食结构。\n";
        } else {
            return "⚠️ 需要改进！请参考下面的建议调整饮食习惯。\n";
        }
    }

    private void appendTopFoods(StringBuilder sb, List<DietRecord> weekRecords) {
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