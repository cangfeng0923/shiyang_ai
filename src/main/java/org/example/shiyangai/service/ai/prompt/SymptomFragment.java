// SymptomFragment.java
package org.example.shiyangai.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SymptomFragment implements PromptFragment {

    @Override
    public String getContent(ConversationContext ctx) {
        String symptom = ctx.getSymptom();
        if (symptom == null || symptom.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【用户当前症状】\n");
        sb.append("- ").append(symptom).append("\n\n");
        sb.append("请根据症状给出调理建议，推荐适合的食疗方案。\n\n");
        return sb.toString();
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return ctx.getSymptom() != null && !ctx.getSymptom().isEmpty();
    }
}