package org.example.shiyangai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_food_history")
public class FoodHistory {
    @TableId
    private Long id;
    private String userId;
    private String foodName;
    private String constitution;
    private String suitability;
    private String suggestion;
    private String nutritionData;
    private LocalDateTime createdAt;
}