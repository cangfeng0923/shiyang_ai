package org.example.shiyangai.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDate;

@Data
@TableName("daily_task_record")//任务记录实体
public class DailyTaskRecord {
    @TableId
    private String id;

    private String userId;
    private String taskName;
    private String taskType;    // HEALTH/DIET/EXERCISE/SLEEP
    private Boolean completed;
    private Integer points;
    private LocalDate taskDate;
    private String notes;
}