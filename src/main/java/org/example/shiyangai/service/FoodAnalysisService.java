package org.example.shiyangai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.dto.FoodAnalysisRequest;
import org.example.shiyangai.dto.FoodAnalysisResponse;
import org.example.shiyangai.enums.ConstitutionType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodAnalysisService {

    private final NutritionApiService nutritionApiService;
    private final UserService userService;
    private final ConstitutionService constitutionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 核心分析逻辑
     */
    public FoodAnalysisResponse analyze(FoodAnalysisRequest request) {
        String userId = request.getUserId();
        String foodName = request.getFoodName();
        String constitution = request.getConstitution();

        log.info("开始分析: userId={}, foodName={}, constitution={}", userId, foodName, constitution);

        // 1. 参数校验
        if (foodName == null || foodName.trim().isEmpty()) {
            return FoodAnalysisResponse.builder()
                    .needAssessment(false)
                    .message("请提供食物名称")
                    .disclaimer("⚠️ 本建议仅供参考，不构成医疗诊断")
                    .build();
        }

        // 2. 获取用户体质（如果请求中没有则从数据库获取）
        if (constitution == null || constitution.isEmpty()) {
            constitution = userService.getConstitution(userId);
        }

        // 3. 如果还是没有，返回需要测评
        if (constitution == null || constitution.isEmpty()) {
            log.info("用户未完成测评: userId={}", userId);
            return FoodAnalysisResponse.builder()
                    .userId(userId)
                    .needAssessment(true)
                    .message("请先完成体质测评，以获得个性化饮食建议")
                    .disclaimer("⚠️ 本建议仅供参考，不构成医疗诊断")
                    .build();
        }

        // 4. 获取营养数据
        Map<String, Object> nutrition = nutritionApiService.getNutrition(foodName);

        // 5. 获取中医建议
        ConstitutionType constitutionType = ConstitutionType.fromName(constitution);
        ConstitutionType.SuitabilityResult advice = constitutionType.evaluate(foodName);

        // 6. 构建响应
        FoodAnalysisResponse response = FoodAnalysisResponse.builder()
                .userId(userId)
                .constitution(constitution)
                .foodName(foodName)
                .nutrition(nutrition)
                .suitability(advice.getSuitability())
                .reason(advice.getReason())
                .suggestion(advice.getSuggestion())
                .needAssessment(false)
                .disclaimer("⚠️ 本建议仅供参考，不构成医疗诊断。如有身体不适请咨询专业医师。")
                .build();

        // 7. 异步保存历史（同步保存也行，这里简化）
        saveHistoryAsync(userId, foodName, constitution, advice, nutrition);

        log.info("分析完成: foodName={}, suitability={}", foodName, advice.getSuitability());
        return response;
    }

    /**
     * 保存历史记录
     */
    private void saveHistoryAsync(String userId, String foodName, String constitution,
                                  ConstitutionType.SuitabilityResult advice, Map<String, Object> nutrition) {
        try {
            String nutritionJson = objectMapper.writeValueAsString(nutrition);
            userService.saveFoodHistory(userId, foodName, constitution,
                    advice.getSuitability(), advice.getSuggestion(), nutritionJson);
        } catch (JsonProcessingException e) {
            log.error("序列化营养数据失败: {}", e.getMessage());
            userService.saveFoodHistory(userId, foodName, constitution,
                    advice.getSuitability(), advice.getSuggestion(), "{}");
        }
    }

    /**
     * 批量分析（一次分析多种食物）
     */
    public Map<String, FoodAnalysisResponse> batchAnalyze(String userId, String[] foodNames, String constitution) {
        Map<String, FoodAnalysisResponse> results = new HashMap<>();

        for (String foodName : foodNames) {
            FoodAnalysisRequest request = FoodAnalysisRequest.builder()
                    .userId(userId)
                    .foodName(foodName)
                    .constitution(constitution)
                    .build();
            results.put(foodName, analyze(request));
        }

        return results;
    }

    /**
     * 快速测评并保存
     */
    public String quickAssessmentAndSave(String userId, boolean[] answers) {
        String constitution = constitutionService.quickAssessment(answers);
        userService.saveConstitution(userId, constitution);
        return constitution;
    }
}