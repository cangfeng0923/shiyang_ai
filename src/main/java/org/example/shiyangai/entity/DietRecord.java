package org.example.shiyangai.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("diet_record")
//饮食记录实体
public class DietRecord {
    @TableId
    private String id;

    private String userId;
    private String mealType;    // BREAKFAST/LUNCH/DINNER/SNACK
    private String foodName;
    private Integer grams;
    private String imageUrl;
    private String notes;

    private Double calories;
    private Integer healthScore;
    private String suggestions;

    private LocalDate recordDate;
    private LocalDateTime createTime;
}
