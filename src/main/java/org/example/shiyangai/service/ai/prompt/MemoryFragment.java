// MemoryFragment.java
package org.example.shiyangai.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemoryFragment implements PromptFragment {

    @Override
    public String getContent(ConversationContext ctx) {
        String previousContext = ctx.getPreviousContext();
        if (previousContext == null || previousContext.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【上一轮对话】\n");
        sb.append(previousContext).append("\n\n");
        sb.append("请保持对话连贯性。\n\n");
        return sb.toString();
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return ctx.getPreviousContext() != null && !ctx.getPreviousContext().isEmpty();
    }
}