package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.SleepRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// FusionReportService.java - 完善版
@Service
@Slf4j
@RequiredArgsConstructor
public class FusionReportService {

    private final EnhancedHealthReportService dietReportService;
    private final SleepService sleepService;
    private final DietRecordService dietRecordService;

    /**
     * 生成饮食+睡眠融合报告
     */
    public String generateFusionReport(String userId, String constitution) {
        // 1. 获取原始数据
        List<DietRecord> dietRecords = dietRecordService.getWeekRecords(userId);
        List<SleepRecord> sleepRecords = sleepService.getWeekRecords(userId);

        // 2. 计算真实评分
        int dietaryScore = calculateDietaryScore(dietRecords);
        int sleepScore = calculateSleepScore(sleepRecords);

        // 3. 关联分析
        String correlationInsight = analyzeCorrelation(dietRecords, sleepRecords);

        // 4. 生成报告
        return buildReport(dietaryScore, sleepScore, correlationInsight, dietRecords, sleepRecords);
    }

    private int calculateDietaryScore(List<DietRecord> records) {
        if (records.isEmpty()) return 0;
        return (int) records.stream()
                .mapToInt(DietRecord::getHealthScore)
                .average()
                .orElse(0);
    }

    private int calculateSleepScore(List<SleepRecord> records) {
        if (records.isEmpty()) return 0;
        // 调用 SleepService 的评分方法
        return (int) records.stream()
                .mapToInt(sleepService::calculateSleepScore)
                .average()
                .orElse(0);
    }

    private String analyzeCorrelation(List<DietRecord> dietRecords, List<SleepRecord> sleepRecords) {
        StringBuilder sb = new StringBuilder();

        // 分析1：晚餐时间与睡眠质量的关系
        long lateDinnerCount = dietRecords.stream()
                .filter(r -> "DINNER".equals(r.getMealType()))
                .filter(r -> {
                    LocalDateTime time = r.getRecordDate();
                    return time != null && time.getHour() >= 20;
                })
                .count();

        if (lateDinnerCount > 0 && !sleepRecords.isEmpty()) {
            double avgSleepScore = sleepRecords.stream()
                    .mapToInt(sleepService::calculateSleepScore)
                    .average()
                    .orElse(0);

            if (avgSleepScore < 70) {
                sb.append("⚠️ **晚餐时间影响睡眠**：本周有").append(lateDinnerCount)
                        .append("次晚餐在20:00后，您的睡眠评分为").append(String.format("%.0f", avgSleepScore))
                        .append("分。建议晚餐提前至19:00前。\n\n");
            }
        }

        // 分析2：咖啡因摄入与入睡时间的关系
        long caffeineCount = dietRecords.stream()
                .filter(r -> {
                    String name = r.getFoodName();
                    return name != null && (name.contains("咖啡") || name.contains("茶") || name.contains("可乐"));
                })
                .count();

        if (caffeineCount > 3 && !sleepRecords.isEmpty()) {
            sb.append("☕ **咖啡因提醒**：本周摄入含咖啡因饮品").append(caffeineCount)
                    .append("次，可能影响入睡。建议下午16:00后避免摄入。\n\n");
        }

        if (sb.length() == 0) {
            sb.append("✅ **饮食-睡眠关联**：本周未发现明显的饮食影响睡眠的问题。\n");
            sb.append("继续保持良好的饮食习惯和作息规律。\n");
        }

        return sb.toString();
    }

    /**
     * 构建最终报告 - ✅ 新增方法
     */
    private String buildReport(int dietaryScore, int sleepScore, String correlationInsight,
                               List<DietRecord> dietRecords, List<SleepRecord> sleepRecords) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 🌟 **本周健康画像**\n\n");

        // 综合评分
        int totalScore = (dietaryScore + sleepScore) / 2;
        sb.append("## 📊 **综合健康评分**：").append(totalScore).append("/100\n\n");

        // 各维度评分
        sb.append("### 🍽️ 饮食评分：").append(dietaryScore).append("/100\n");
        sb.append("   ").append(getScoreComment(dietaryScore)).append("\n\n");

        sb.append("### 😴 睡眠评分：").append(sleepScore).append("/100\n");
        sb.append("   ").append(getScoreComment(sleepScore)).append("\n\n");

        // 雷达图数据（JSON格式，方便前端渲染）
        sb.append("## 📈 **健康雷达图数据**\n\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"diet\": ").append(dietaryScore).append(",\n");
        sb.append("  \"sleep\": ").append(sleepScore).append("\n");
        sb.append("}\n");
        sb.append("```\n\n");

        // 关联洞察
        sb.append("## 🔍 **关联洞察**\n\n");
        sb.append(correlationInsight);

        // 统计数据
        sb.append("## 📋 **本周统计**\n\n");
        sb.append("- 饮食记录数：").append(dietRecords.size()).append("条\n");
        sb.append("- 睡眠记录数：").append(sleepRecords.size()).append("条\n");

        if (!dietRecords.isEmpty()) {
            long breakfastCount = dietRecords.stream()
                    .filter(r -> "BREAKFAST".equals(r.getMealType()))
                    .count();
            long dinnerCount = dietRecords.stream()
                    .filter(r -> "DINNER".equals(r.getMealType()))
                    .count();
            sb.append("- 早餐次数：").append(breakfastCount).append("次\n");
            sb.append("- 晚餐次数：").append(dinnerCount).append("次\n");
        }

        // 改进建议
        sb.append("\n## 💡 **改进建议**\n\n");

        if (dietaryScore < 70) {
            sb.append("- 🥗 **饮食改善**：增加蔬菜摄入，减少高油高糖食物\n");
        }
        if (sleepScore < 70) {
            sb.append("- 🛌 **睡眠改善**：保持规律作息，睡前1小时远离电子设备\n");
        }
        if (dietaryScore >= 70 && sleepScore >= 70) {
            sb.append("- 🎉 表现优秀！继续保持当前的生活方式\n");
        }

        // 下周目标
        sb.append("\n## 🎯 **下周小目标**\n\n");
        if (dietaryScore < 70) {
            sb.append("- 🥬 每天摄入至少300g蔬菜\n");
        }
        if (sleepScore < 70) {
            sb.append("- 🌙 连续3天在23:00前入睡\n");
        }
        if (dietaryScore >= 70 && sleepScore >= 70) {
            sb.append("- 🌟 尝试增加运动，让健康更全面\n");
        }

        return sb.toString();
    }

    /**
     * 根据分数返回评语
     */
    private String getScoreComment(int score) {
        if (score >= 85) return "🎉 优秀！继续保持！";
        if (score >= 70) return "👍 良好，仍有提升空间";
        if (score >= 60) return "⚠️ 一般，需要关注改进";
        return "❌ 较差，建议尽快调整";
    }
}