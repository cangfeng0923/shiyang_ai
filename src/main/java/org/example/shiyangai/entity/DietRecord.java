package org.example.shiyangai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("diet_record")
public class DietRecord {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;
    private String mealType;        // BREAKFAST/LUNCH/DINNER/SNACK
    private String foodName;

    // ========== 重量相关字段 ==========
    private Double grams;            // 克数（改为 Double，兼容旧数据）
    private Double originalAmount;   // 用户填写的数量
    private String originalUnit;     // 用户选择的单位（碗/个/杯等）
    private Double estimatedGrams;   // 后台换算的标准克数

    // ========== 营养相关字段 ==========
    private Double calories;         // 热量（千卡）
    private Double protein;          // 蛋白质（g）
    private Double carbs;            // 碳水化合物（g）
    private Double fat;              // 脂肪（g）
    private Double fiber;            // 膳食纤维（g）

    // ========== 其他字段 ==========
    private String imageUrl;
    private String notes;
    private Integer healthScore;
    private String suggestions;

    private LocalDateTime recordDate;
    private LocalDateTime createTime;
}