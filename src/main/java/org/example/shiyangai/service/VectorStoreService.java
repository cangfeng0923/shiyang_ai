package org.example.shiyangai.service;

import jakarta.annotation.PostConstruct;
import org.example.shiyangai.dto.VectorSearchResult;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service//向量数据库服务（存储和检索）
public class VectorStoreService {

    // 模拟向量数据库（实际应该使用 FAISS、Milvus 或 Pinecone）
    private final Map<String, float[]> foodVectors = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化常见食材的向量（模拟数据）
        initFoodVectors();
    }

    private void initFoodVectors() {
        // 这里应该是真实的向量数据
        // 目前用模拟数据，实际需要调用 Embedding API 生成

        // 温性食材（适合阳虚质）
        foodVectors.put("南瓜", generateRandomVector());
        foodVectors.put("桂圆", generateRandomVector());
        foodVectors.put("红枣", generateRandomVector());
        foodVectors.put("生姜", generateRandomVector());
        foodVectors.put("羊肉", generateRandomVector());

        // 寒性食材（不适合阳虚质）
        foodVectors.put("西瓜", generateRandomVector());
        foodVectors.put("苦瓜", generateRandomVector());
        foodVectors.put("绿豆", generateRandomVector());
    }

    /**
     * 搜索相似食材
     * @param queryVector 查询向量
     * @param topK 返回前K个结果
     * @return 相似食材列表
     */
    public List<VectorSearchResult> search(float[] queryVector, int topK) {
        List<VectorSearchResult> results = new ArrayList<>();

        for (Map.Entry<String, float[]> entry : foodVectors.entrySet()) {
            float similarity = cosineSimilarity(queryVector, entry.getValue());
            results.add(new VectorSearchResult(entry.getKey(), similarity, entry.getValue()));
        }

        // 按相似度降序排序
        results.sort((a, b) -> Float.compare(b.getSimilarity(), a.getSimilarity()));

        // 返回前 topK 个
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * 添加食材向量
     */
    public void addFoodVector(String foodName, float[] vector) {
        foodVectors.put(foodName, vector);
    }

    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;

        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 生成随机向量（模拟）
     */
    private float[] generateRandomVector() {
        float[] vector = new float[128];
        Random random = new Random();
        for (int i = 0; i < 128; i++) {
            vector[i] = random.nextFloat();
        }
        return vector;
    }
}