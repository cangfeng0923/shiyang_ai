package org.example.shiyangai.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;

@Data
@TableName("health_profile")
public class HealthProfile {
    @TableId
    private String userId;

    // 基础信息
    private Integer age;
    private String gender;      // MALE/FEMALE
    private Double height;      // cm
    private Double weight;      // kg
    private Double bmi;

    // 健康信息
    private String bloodType;
    private String pastDiseases;    // JSON数组
    private String allergies;        // JSON数组
    private String foodAvoidance;    // JSON数组 - 忌口
    private String foodPreference;   // JSON数组 - 饮食偏好

    // 用药记录
    private String medications;      // JSON数组

    // 生活习惯
    private String sleepPattern;     // EARLY/LATE/IRREGULAR
    private String exerciseHabit;    // OFTEN/SOMETIMES/RARELY
    private String smoking;          // YES/NO
    private String drinking;         // YES/NO

    private LocalDateTime updateTime;
    private String constitution;
}