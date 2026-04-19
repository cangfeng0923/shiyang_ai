package org.example.shiyangai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//用于封装前端→后端的食物分析请求参数。
public class FoodAnalysisRequest {
    private String userId;        // 用户ID（可选）
    private String foodName;      // 食物名称（必填）
    private String constitution;  // 体质类型（可选，不传则从数据库读）
}