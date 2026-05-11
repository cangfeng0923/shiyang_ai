// PromptFragment.java
package org.example.shiyangai.service.ai.prompt;

import org.example.shiyangai.service.ai.context.ConversationContext;

public interface PromptFragment {
    String getContent(ConversationContext ctx);
    int getPriority();
    boolean supports(ConversationContext ctx);
}