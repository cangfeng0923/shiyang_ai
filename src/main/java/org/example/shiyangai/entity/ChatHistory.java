package org.example.shiyangai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_chat_history")
public class ChatHistory {
    @TableId
    private Long id;
    private String userId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}