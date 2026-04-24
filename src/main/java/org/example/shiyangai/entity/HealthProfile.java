package org.example.shiyangai.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_profile")
public class HealthProfile {
    @TableId
    private String userId;

    // ========== 1. 基础人口学数据 ==========
    private String nickname;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private String occupation;
    private String occupationType;

    // ========== 2. 人体测量数据 ==========
    private Double height;
    private Double weight;
    private Double bmi;
    private Double bodyFat;
    private Double waistline;
    private String physiologicalStage;

    // ========== 3. 临床健康数据 ==========
    private String pastDiseases;       // 慢病标签（JSON数组）- 统一用旧名
    private String medications;

    // 生化指标
    private Double fastingGlucose;
    private Double hba1c;
    private Double totalCholesterol;
    private Double triglycerides;
    private Double ldl;
    private Double uricAcid;
    private Double alt;
    private Integer systolicBp;
    private Integer diastolicBp;

    // ========== 4. 饮食偏好与过敏禁忌 ==========
    private String allergies;
    private String foodAvoidance;
    private String foodPreference;     // 饮食偏好 - 统一用旧名
    private String tastePreference;    // 新字段保留
    private String dietType;
    private String cookingCondition;
    private String diningHabit;

    // ========== 5. 中医体质与症状 ==========
    private String constitution;
    private String tongueDescription;
    private String symptoms;

    // ========== 6. 生活方式 ==========
    private String exerciseHabit;      // 运动习惯 - 统一用旧名
    private String exerciseFrequency;
    private String exerciseIntensity;
    private String exerciseType;
    private String sleepPattern;
    private Double sleepDuration;
    private String sleepQuality;
    private String smoking;
    private String drinking;
    private Integer waterIntake;

    // 元数据
    private LocalDateTime updateTime;
    private Boolean isComplete;

    // 血型
    private String bloodType;
}