package weak;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.UserService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 职责：异步保存食材查询历史
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FoodHistorySaver {

    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public void saveAsync(String userId, String foodName, String constitution, IngredientInfo foodInfo) {
        executorService.submit(() -> {
            try {
                String suitability = getSuitabilityText(foodInfo, constitution);
                String suggestion = buildSuggestion(foodInfo, constitution);

                Map<String, Object> nutrition = new HashMap<>();
                nutrition.put("property", foodInfo.getProperty());
                nutrition.put("flavor", foodInfo.getFlavor());
                nutrition.put("effect", foodInfo.getEffect());
                nutrition.put("meridian", foodInfo.getMeridian());
                String nutritionJson = objectMapper.writeValueAsString(nutrition);

                userService.saveFoodHistory(userId, foodName, constitution, suitability, suggestion, nutritionJson);
                log.info("已保存食材查询历史: userId={}, foodName={}, suitability={}", userId, foodName, suitability);
            } catch (Exception e) {
                log.error("保存食材历史失败: {}", e.getMessage());
            }
        });
    }

    private String getSuitabilityText(IngredientInfo foodInfo, String constitution) {
        int score = getSuitabilityScore(foodInfo, constitution);
        if (score > 0) return "适合";
        else if (score < 0) return "不适合";
        else return "慎食";
    }

    private String buildSuggestion(IngredientInfo foodInfo, String constitution) {
        String property = foodInfo.getProperty() != null ? foodInfo.getProperty() : "平";
        String effect = foodInfo.getEffect() != null ? foodInfo.getEffect() : "";

        if ("寒".equals(property) || "凉".equals(property)) {
            if ("阳虚质".equals(constitution)) {
                return "您为阳虚体质，此食物性偏寒凉，建议少食或搭配温性食材食用";
            }
        } else if ("温".equals(property) || "热".equals(property)) {
            if ("阴虚质".equals(constitution) || "湿热质".equals(constitution)) {
                return "您为" + constitution + "，此食物性偏温热，建议适量食用，避免上火";
            }
        }

        return effect.isEmpty() ? "适量食用" : effect;
    }

    private int getSuitabilityScore(IngredientInfo info, String constitution) {
        if (info == null || info.getProperty() == null) return 0;

        Map<String, Map<String, Integer>> scoreMap = Map.of(
                "寒", Map.of("阳虚质", -10, "阴虚质", 5, "湿热质", 5, "气虚质", -5),
                "凉", Map.of("阳虚质", -5, "阴虚质", 3, "湿热质", 3, "气虚质", -3),
                "平", Map.of("阳虚质", 0, "阴虚质", 0, "湿热质", 0, "气虚质", 5),
                "温", Map.of("阳虚质", 10, "阴虚质", -5, "湿热质", -3, "气虚质", 8),
                "热", Map.of("阳虚质", 15, "阴虚质", -10, "湿热质", -8, "气虚质", 5)
        );

        Map<String, Integer> scores = scoreMap.getOrDefault(info.getProperty(), Map.of());
        return scores.getOrDefault(constitution, 0);
    }
}