package org.example.shiyangai.service;

import org.example.shiyangai.dto.VectorSearchResult;
import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service//食材替代推荐（核心业务）
public class FoodSubstitutionService {

    @Autowired
    private VectorStoreService vectorStore;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private SmartFoodService foodService;

    /**
     * 基于向量相似度找替代食材
     */
    public List<String> findAlternatives(String foodName, String constitution) {
        // 1. 获取食材的向量表示
        float[] foodVector = getFoodVector(foodName);

        // 2. 在向量库中搜索相似的食材
        List<VectorSearchResult> results = vectorStore.search(foodVector, 10);

        // 3. 过滤：排除不适合当前体质的食材
        return results.stream()
                .filter(r -> isSuitableForConstitution(r.getFoodName(), constitution))
                .map(VectorSearchResult::getFoodName)
                .collect(Collectors.toList());
    }

    /**
     * 判断食材是否适合当前体质
     */
    private boolean isSuitableForConstitution(String foodName, String constitution) {
        IngredientInfo info = foodService.getFoodInfo(foodName);
        if (info == null) return false;

        int score = info.getSuitabilityScore(constitution);
        return score >= 0;  // 分数>=0表示适合
    }

    /**
     * 构建食材向量（使用Embedding）
     */
    private float[] getFoodVector(String foodName) {
        // 调用 Embedding API
        String prompt = String.format("""
            请为食材"%s"生成一个128维的向量，
            需要考虑：中医属性、营养成分、烹饪方式、口感特点。
            """, foodName);

        return embeddingService.embed(prompt);
    }

    /**
     * 获取替代建议（带理由）
     */
    public String getSubstitutionAdvice(String foodName, String constitution) {
        List<String> alternatives = findAlternatives(foodName, constitution);

        if (alternatives.isEmpty()) {
            return String.format("抱歉，没有找到适合您体质（%s）的%s替代食材。", constitution, foodName);
        }

        StringBuilder advice = new StringBuilder();
        advice.append(String.format("由于您的体质是【%s】，%s可能不太适合。建议用以下食材替代：\n", constitution, foodName));

        for (int i = 0; i < Math.min(5, alternatives.size()); i++) {
            String alt = alternatives.get(i);
            IngredientInfo info = foodService.getFoodInfo(alt);
            int score = info.getSuitabilityScore(constitution);
            advice.append(String.format("%d. %s（适宜评分：%d）\n", i + 1, alt, score));
        }

        return advice.toString();
    }
}