package org.example.shiyangai.service.diet;

import org.springframework.stereotype.Service;

@Service
public class DietAdviceService {

    public String generateSuggestions(
            int score
    ) {

        if (score >= 90) {
            return "🎉 非常健康";
        }

        if (score >= 80) {
            return "👍 营养结构良好";
        }

        if (score >= 70) {
            return "✅ 整体不错";
        }

        if (score >= 60) {
            return "⚠️ 基本达标";
        }

        return "💪 需要调整饮食结构";
    }
}