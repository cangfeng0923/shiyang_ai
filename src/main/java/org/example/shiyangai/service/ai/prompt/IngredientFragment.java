// IngredientFragment.java
package org.example.shiyangai.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IngredientFragment implements PromptFragment {

    @Override
    public String getContent(ConversationContext ctx) {
        IngredientInfo foodInfo = ctx.getFoodInfo();
        if (foodInfo == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【用户询问的食材信息】\n");
        sb.append("- 食材：").append(foodInfo.getName()).append("\n");
        sb.append("- 属性：").append(foodInfo.getProperty()).append("性\n");
        sb.append("- 功效：").append(foodInfo.getEffect()).append("\n");

        if (ctx.getConstitution() != null) {
            int score = foodInfo.getSuitabilityScore(ctx.getConstitution());
            if (score > 0) {
                sb.append("- 建议：✅ 适宜食用\n");
            } else if (score < 0) {
                sb.append("- 建议：⚠️ 需谨慎食用\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public int getPriority() {
        return 25;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return ctx.getFoodInfo() != null;
    }
}