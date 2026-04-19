package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NutritionApiService {

    @Value("${usda.api.key}")
    private String apiKey;

    @Value("${usda.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getNutrition(String foodName) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 构建请求URL
            String url = apiUrl + "?api_key=" + apiKey + "&query=" + foodName + "&pageSize=1";
            log.info("调用USDA API: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode foods = root.get("foods");

                if (foods != null && foods.size() > 0) {
                    JsonNode nutrients = foods.get(0).get("foodNutrients");
                    String description = foods.get(0).get("description").asText();
                    result.put("foodName", description);

                    for (JsonNode nutrient : nutrients) {
                        String name = nutrient.get("nutrientName").asText();
                        double value = nutrient.get("value").asDouble();
                        String unit = nutrient.get("unitName").asText();

                        if (name.equals("Energy")) {
                            result.put("calories", value + " " + unit);
                        } else if (name.equals("Protein")) {
                            result.put("protein", value + " " + unit);
                        } else if (name.equals("Carbohydrate, by difference")) {
                            result.put("carbs", value + " " + unit);
                        } else if (name.equals("Total Fat")) {
                            result.put("fat", value + " " + unit);
                        }
                    }

                    if (result.isEmpty()) {
                        result.put("message", "该食物暂无详细营养数据");
                    }
                } else {
                    result.put("error", "未找到该食物");
                }
            }
        } catch (Exception e) {
            log.error("USDA API调用失败: {}", e.getMessage());
            result.put("error", "营养数据获取失败: " + e.getMessage());
        }

        return result;
    }
}