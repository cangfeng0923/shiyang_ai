package org.example.shiyangai.service.health;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.enums.ConstitutionType;
import org.springframework.stereotype.Component;

/**
 * 体质建议章节构建器
 * 职责：构建报告中的体质饮食建议部分
 */
@Slf4j
@Component
public class ConstitutionSectionBuilder {

    public String build(String constitution) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---\n");
        sb.append("### 4️⃣ 体质饮食建议\n\n");

        ConstitutionType type = ConstitutionType.fromName(constitution);
        sb.append("**").append(constitution).append("**：").append(type.getDescription()).append("\n\n");
        sb.append("✅ **适宜食物**：").append(String.join("、", type.getGoodFoods())).append("\n");
        sb.append("❌ **不宜食物**：").append(String.join("、", type.getBadFoods())).append("\n");

        return sb.toString();
    }
}