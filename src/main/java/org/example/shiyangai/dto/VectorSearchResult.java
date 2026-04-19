package org.example.shiyangai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//存储向量搜索结果
public class VectorSearchResult {
    private String foodName;      // 食材名称
    private float similarity;     // 相似度（0-1）
    private float[] vector;       // 向量值

    public String getFoodName() {
        return foodName;
    }
}
