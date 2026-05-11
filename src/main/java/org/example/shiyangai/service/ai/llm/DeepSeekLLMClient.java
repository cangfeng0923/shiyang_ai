// DeepSeekLLMClient.java
package org.example.shiyangai.service.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class DeepSeekLLMClient implements LLMClient {

    @Value("${deepseek.api.key}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekLLMClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String chatSync(List<Map<String, String>> messages, double temperature, int maxTokens) throws Exception {
        Map<String, Object> requestBody = buildRequestBody(messages, temperature, maxTokens, false);
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = buildRequest(jsonBody);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var jsonNode = objectMapper.readTree(response.body());
            return jsonNode.path("choices").path(0).path("message").path("content").asText();
        } else {
            throw new RuntimeException("API call failed: " + response.statusCode());
        }
    }

    @Override
    public void chatStream(List<Map<String, String>> messages, double temperature, int maxTokens,
                           Consumer<String> onChunk, Runnable onComplete) throws Exception {
        Map<String, Object> requestBody = buildRequestBody(messages, temperature, maxTokens, true);
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = buildRequest(jsonBody);
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) break;
                        try {
                            var jsonNode = objectMapper.readTree(data);
                            String content = jsonNode.path("choices").path(0).path("delta").path("content").asText();
                            if (!content.isEmpty()) {
                                onChunk.accept(content);
                            }
                        } catch (Exception e) {
                            log.warn("Parse error: {}", e.getMessage());
                        }
                    }
                }
            }
            onComplete.run();
        } else {
            throw new RuntimeException("API call failed: " + response.statusCode());
        }
    }

    @Override
    public String getModelName() {
        return "deepseek-chat";
    }

    private Map<String, Object> buildRequestBody(List<Map<String, String>> messages,
                                                 double temperature, int maxTokens, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", stream);
        return body;
    }

    private HttpRequest buildRequest(String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }
}