package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service//调用 API 生成文本向量
public class EmbeddingService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取文本的向量表示
     */
    public float[] embed(String text) {
        try {
            // 调用 DeepSeek Embedding API（或其他 Embedding 服务）
            String jsonBody = String.format("""
                {
                    "model": "text-embedding-ada-002",
                    "input": "%s"
                }
                """, text.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/embeddings"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                JsonNode embedding = json.path("data").path(0).path("embedding");

                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = (float) embedding.get(i).asDouble();
                }
                return vector;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 返回模拟向量（实际使用时应该返回真实向量）
        return generateMockVector();
    }

    /**
     * 批量获取向量
     */
    public List<float[]> batchEmbed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(embed(text));
        }
        return vectors;
    }

    /**
     * 生成模拟向量（临时使用）
     */
    private float[] generateMockVector() {
        float[] vector = new float[128];
        for (int i = 0; i < 128; i++) {
            vector[i] = (float) Math.random();
        }
        return vector;
    }
}