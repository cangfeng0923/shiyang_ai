package org.example.shiyangai.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDate;

@Data
@TableName("solar_term_advice")
public class SolarTermAdvice {
    @TableId
    private Integer id;

    private String solarTermName;   // 节气名称
    private Integer year;
    private LocalDate startDate;
    private LocalDate endDate;

    private String principle;       // 养生原则
    private String foodAdvice;      // 饮食建议
    private String recipe;          // 推荐食谱
    private String dailyAdvice;     // 起居建议
    private String exerciseAdvice;  // 运动建议

    private String constitution;    // 适配体质（可选）

    private LocalDate create_time;
}
