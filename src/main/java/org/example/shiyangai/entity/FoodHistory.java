package org.example.shiyangai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_food_history")//记录用户的食材查询历史
public class FoodHistory {
    @TableId
    private Long id;
    private String userId;
    private String foodName;
    private String constitution; //用户的体质类型
    private String suitability;  //该食材是否适合用户体质
    private String suggestion;  //食用建议
    private String nutritionData; //	营养成分数据（JSON格式，存储热量、蛋白质等）
    private LocalDateTime createdAt;
}