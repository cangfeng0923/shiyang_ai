package org.example.shiyangai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.SleepRecord;
import org.example.shiyangai.mapper.SleepRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 睡眠数据服务 (Phase 2)
 * 支持睡眠记录的增删改查和分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SleepService {

    private final SleepRecordMapper sleepRecordMapper;

    /**
     * 添加睡眠记录
     */
    public void addSleepRecord(SleepRecord record) {
        record.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        record.setCreateTime(LocalDateTime.now());
        record.setRecordDate(record.getRecordDate() != null ? record.getRecordDate() : LocalDate.now());

        sleepRecordMapper.insert(record);
        log.info("添加睡眠记录: userId={}, quality={}", record.getUserId(), record.getQuality());
    }

    /**
     * 获取今日睡眠记录
     */
    public List<SleepRecord> getTodayRecords(String userId) {
        return sleepRecordMapper.selectByUserIdAndDate(userId, LocalDate.now());
    }

    /**
     * 获取指定日期记录
     */
    public List<SleepRecord> getRecordsByDate(String userId, LocalDate date) {
        return sleepRecordMapper.selectByUserIdAndDate(userId, date);
    }

    /**
     * 获取一周睡眠记录
     */
    public List<SleepRecord> getWeekRecords(String userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);
        return sleepRecordMapper.selectByDateRange(userId, startDate, endDate);
    }

    /**
     * 计算睡眠评分
     */
    public int calculateSleepScore(SleepRecord record) {
        int score = 70; // 基础分

        // 根据睡眠时长加分
        if (record.getSleepDuration() != null) {
            if (record.getSleepDuration() >= 7 && record.getSleepDuration() <= 9) {
                score += 15; // 理想时长
            } else if (record.getSleepDuration() >= 6 || record.getSleepDuration() <= 10) {
                score += 5; // 可接受范围
            } else {
                score -= 10; // 不足或过长
            }
        }

        // 根据睡眠质量加分
        if (record.getQuality() != null) {
            switch (record.getQuality()) {
                case "EXCELLENT":
                    score += 20;
                    break;
                case "GOOD":
                    score += 10;
                    break;
                case "FAIR":
                    break;
                case "POOR":
                    score -= 15;
                    break;
            }
        }

        // 根据入睡时间加分
        if (record.getBedtime() != null) {
            int hour = record.getBedtime().getHour();
            if (hour >= 21 && hour <= 23) { // 21:00-23:00
                score += 10;
            } else if (hour >= 23 || hour <= 1) { // 23:00-01:00
                score += 5;
            } else {
                score -= 10; // 过晚或过早
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 生成睡眠报告
     */
    public String generateSleepReport(String userId) {
        List<SleepRecord> records = getWeekRecords(userId);

        if (records.isEmpty()) {
            return "本周暂无睡眠记录";
        }

        double avgDuration = records.stream()
                .mapToDouble(r -> r.getSleepDuration() != null ? r.getSleepDuration() : 0)
                .average()
                .orElse(0);

        double avgScore = records.stream()
                .mapToInt(this::calculateSleepScore)
                .average()
                .orElse(0);

        long excellentSleeps = records.stream()
                .filter(r -> calculateSleepScore(r) >= 80)
                .count();

        StringBuilder sb = new StringBuilder();
        sb.append("😴 **本周睡眠报告**\n\n");
        sb.append("- 平均睡眠时长：").append(String.format("%.1f", avgDuration)).append("小时\n");
        sb.append("- 平均睡眠评分：").append(String.format("%.0f", avgScore)).append("/100\n");
        sb.append("- 优质睡眠天数：").append(excellentSleeps).append("/").append(records.size()).append("天\n\n");

        if (avgDuration < 7) {
            sb.append("⚠️ 睡眠时长不足，建议延长睡眠时间至7-9小时\n");
        } else if (avgDuration > 9) {
            sb.append("💡 睡眠时长充足，但注意不要过长\n");
        } else {
            sb.append("✅ 睡眠时长理想\n");
        }

        return sb.toString();
    }
}