package org.example.shiyangai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.mapper.DietRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietRecordService {

    private final DietRecordMapper dietRecordMapper;

    /**
     * 添加饮食记录
     */
    public void addRecord(DietRecord record) {
        record.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        record.setCreateTime(LocalDateTime.now());
        record.setRecordDate(record.getRecordDate() != null ? record.getRecordDate() : LocalDate.now());

        // 自动计算健康评分
        int score = calculateHealthScore(record.getFoodName());
        record.setHealthScore(score);
        record.setSuggestions(generateSuggestions(score));

        dietRecordMapper.insert(record);
        log.info("添加饮食记录: userId={}, foodName={}", record.getUserId(), record.getFoodName());
    }

    /**
     * 获取今日饮食记录
     */
    public List<DietRecord> getTodayRecords(String userId) {
        return dietRecordMapper.selectByUserIdAndDate(userId, LocalDate.now());
    }

    /**
     * 获取指定日期记录
     */
    public List<DietRecord> getRecordsByDate(String userId, LocalDate date) {
        return dietRecordMapper.selectByUserIdAndDate(userId, date);
    }

    /**
     * 获取一周记录
     */
    public List<DietRecord> getWeekRecords(String userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        return dietRecordMapper.selectByDateRange(userId, startDate, endDate);
    }

    /**
     * 生成饮食报告
     */
    public Map<String, Object> generateDailyReport(String userId, String constitution) {
        List<DietRecord> records = getTodayRecords(userId);

        Map<String, Object> report = new HashMap<>();
        report.put("date", LocalDate.now().toString());
        report.put("totalRecords", records.size());

        if (records.isEmpty()) {
            report.put("message", "今天还没有饮食记录");
            return report;
        }

        double totalCalories = records.stream()
                .mapToDouble(r -> r.getCalories() != null ? r.getCalories() : 0)
                .sum();
        double avgScore = records.stream()
                .mapToInt(DietRecord::getHealthScore)
                .average()
                .orElse(0);

        report.put("totalCalories", Math.round(totalCalories));
        report.put("avgHealthScore", Math.round(avgScore));

        // 评分等级
        if (avgScore >= 80) {
            report.put("level", "excellent");
            report.put("levelText", "优秀");
            report.put("advice", "饮食非常健康，继续保持！");
        } else if (avgScore >= 60) {
            report.put("level", "good");
            report.put("levelText", "良好");
            report.put("advice", "饮食基本健康，可以适当增加蔬菜摄入");
        } else {
            report.put("level", "needImprove");
            report.put("levelText", "需改进");
            report.put("advice", getImprovementAdvice(records, constitution));
        }

        // 按餐次统计
        Map<String, Integer> mealCount = new HashMap<>();
        for (DietRecord record : records) {
            mealCount.merge(record.getMealType(), 1, Integer::sum);
        }
        report.put("mealCount", mealCount);

        return report;
    }

    /**
     * 计算健康评分
     */
    private int calculateHealthScore(String foodName) {
        int score = 70;
        String lowerFood = foodName.toLowerCase();

        // 加分项
        if (lowerFood.contains("蔬菜") || lowerFood.contains("水果")) score += 15;
        if (lowerFood.contains("粗粮") || lowerFood.contains("杂粮")) score += 10;
        if (lowerFood.contains("蒸") || lowerFood.contains("煮")) score += 5;

        // 减分项
        if (lowerFood.contains("炸") || lowerFood.contains("烤")) score -= 20;
        if (lowerFood.contains("甜") || lowerFood.contains("蛋糕") || lowerFood.contains("奶茶")) score -= 15;
        if (lowerFood.contains("辣") || lowerFood.contains("麻辣")) score -= 10;

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 生成建议
     */
    private String generateSuggestions(int score) {
        if (score >= 80) return "营养均衡，继续保持！";
        if (score >= 60) return "还不错，可以增加一些蔬菜水果";
        return "建议减少油炸和高糖食物，多选择清淡烹饪方式";
    }

    /**
     * 获取改进建议
     */
    private String getImprovementAdvice(List<DietRecord> records, String constitution) {
        boolean hasVeggies = records.stream().anyMatch(r ->
                r.getFoodName().contains("蔬菜") || r.getFoodName().contains("青菜"));
        boolean hasProtein = records.stream().anyMatch(r ->
                r.getFoodName().contains("肉") || r.getFoodName().contains("蛋") || r.getFoodName().contains("豆腐"));

        if (!hasVeggies) {
            return "建议增加蔬菜摄入，每天至少吃300g蔬菜";
        }
        if (!hasProtein) {
            return "建议增加优质蛋白，如鸡蛋、鱼肉、豆腐";
        }

        // 体质相关建议
        switch (constitution) {
            case "气虚质": return "建议多吃山药、大枣、鸡肉等补气食物";
            case "阳虚质": return "建议多吃生姜、羊肉、韭菜等温补食物";
            case "阴虚质": return "建议多吃百合、银耳、梨等滋阴食物";
            case "痰湿质": return "建议少吃甜食油腻，多吃薏米、冬瓜";
            case "湿热质": return "建议少吃辛辣烧烤，多吃绿豆、苦瓜";
            default: return "保持均衡饮食，适量运动";
        }
    }
}
