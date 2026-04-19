package org.example.shiyangai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId
    private String id;
    private String username;      // 用户名
    private String password;      // 密码（明文存储，演示用）
    private String constitution;  // 体质类型
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}