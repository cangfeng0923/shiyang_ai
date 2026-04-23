package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.SleepRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

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
        List<DietRecord> dietRecords = dietRecordService.getWeekRecords(userId);
        List<SleepRecord> sleepRecords = sleepService.getWeekRecords(userId);

        if (dietRecords.isEmpty() && sleepRecords.isEmpty()) {
            return "暂无数据，请先记录饮食和睡眠";
        }

        // 计算评分
        int dietaryScore = calculateDietaryScore(dietRecords);
        int sleepScore = calculateSleepScore(sleepRecords);

        // 评分变化趋势（与上周对比）
        String dietTrend = getTrendIcon(dietaryScore);
        String sleepTrend = getTrendIcon(sleepScore);

        StringBuilder sb = new StringBuilder();
        sb.append("# 🌟 **本周健康画像**\n\n");
        sb.append("📅 报告周期：").append(getWeekDateRange()).append("\n\n");
        sb.append("🍽️ **饮食评分**：").append(dietaryScore).append("/100 (").append(dietTrend).append(")\n");
        sb.append("😴 **睡眠评分**：").append(sleepScore).append("/100 (").append(sleepTrend).append(")\n\n");

        // 关联洞察
        sb.append("## 🔍 **关联洞察**\n\n");
        sb.append(analyzeCorrelation(dietRecords, sleepRecords));

        // 个性化建议
        sb.append("## 💡 **个性化建议**\n\n");
        if (dietaryScore < 70) {
            sb.append("- 🥗 **饮食改善**：增加蔬菜摄入，减少高油高糖食物\n");
        }
        if (sleepScore < 70) {
            sb.append("- 🛌 **睡眠改善**：保持规律作息，睡前1小时远离电子设备\n");
        }

        // 下周小目标
        sb.append("\n## 🎯 **下周小目标**\n\n");
        sb.append("- 🌙 连续3天在23:00前入睡\n");
        sb.append("- 🥬 每天摄入至少300g蔬菜\n");

        return sb.toString();
    }

    private String getTrendIcon(int score) {
        return score >= 80 ? "↑" : (score >= 60 ? "→" : "↓");
    }

    private String getWeekDateRange() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        return start + " 至 " + end;
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

        // 按日期建立睡眠数据映射
        Map<LocalDate, SleepRecord> sleepByDate = sleepRecords.stream()
                .collect(Collectors.toMap(SleepRecord::getRecordDate, s -> s, (s1, s2) -> s1));

        // 1. 晚餐时间与睡眠质量分析
        List<LocalDate> lateDinnerDates = new ArrayList<>();
        Map<LocalDate, Double> dinnerSleepScoreMap = new HashMap<>();

        for (DietRecord record : dietRecords) {
            if ("DINNER".equals(record.getMealType())) {
                LocalDate date = record.getRecordDate().toLocalDate();
                int dinnerHour = record.getRecordDate().getHour();

                if (dinnerHour >= 20) { // 20:00后晚餐
                    lateDinnerDates.add(date);
                    SleepRecord sleep = sleepByDate.get(date);
                    if (sleep != null) {
                        dinnerSleepScoreMap.put(date, (double) calculateSleepScore((List<SleepRecord>) sleep));
                    }
                }
            }
        }

        if (lateDinnerDates.size() >= 2) {
            double avgSleepScore = dinnerSleepScoreMap.values().stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);

            if (avgSleepScore < 70) {
                sb.append("⚠️ **晚餐时间影响睡眠**：本周有").append(lateDinnerDates.size())
                        .append("天晚餐在20:00后，当晚睡眠评分较低（平均").append(String.format("%.0f", avgSleepScore))
                        .append("分）。\n   → 建议：晚餐提前至19:00前，如无法调整，晚餐量减少1/3。\n\n");
            }
        }

        // 2. 连续日期分析（找出连续晚晚餐的天数）
        if (lateDinnerDates.size() >= 3) {
            Collections.sort(lateDinnerDates);
            int consecutive = 1;
            for (int i = 1; i < lateDinnerDates.size(); i++) {
                if (lateDinnerDates.get(i).equals(lateDinnerDates.get(i-1).plusDays(1))) {
                    consecutive++;
                }
            }
            if (consecutive >= 3) {
                sb.append("⚠️ **连续晚晚餐提醒**：您有连续").append(consecutive)
                        .append("天晚餐在20:00后，可能形成习惯。\n   → 建议：设定21:00闹钟提醒该睡觉了。\n\n");
            }
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