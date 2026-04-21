package org.example.shiyangai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 睡眠记录实体 (Phase 2)
 */
@Data
@TableName("sleep_record")
public class SleepRecord {
    @TableId
    private String id;

    private String userId;
    private LocalDate recordDate;           // 记录日期
    private LocalTime bedtime;              // 就寝时间
    private LocalTime wakeupTime;           // 起床时间
    private Double sleepDuration;           // 睡眠时长（小时）
    private String quality;                 // 睡眠质量：EXCELLENT/GOOD/FAIR/POOR
    private Integer deepSleepMinutes;       // 深睡时长（分钟）
    private Integer lightSleepMinutes;      // 浅睡时长（分钟）
    private Integer remSleepMinutes;        // REM睡眠时长（分钟）
    private Integer wakeCount;              // 醒来次数
    private Integer snoreLevel;             // 打鼾程度（0-10）
    private Integer sleepApneaLevel;        // 呼吸暂停程度（0-10）
    private String notes;                   // 备注
    private LocalDateTime createTime;
}