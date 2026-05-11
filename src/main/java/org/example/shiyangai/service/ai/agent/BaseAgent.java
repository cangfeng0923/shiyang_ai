// BaseAgent.java
package org.example.shiyangai.service.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.context.ConversationContext;

@Slf4j
public abstract class BaseAgent implements Agent {

    protected final String name;

    protected BaseAgent(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return true;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isPostProcess() {
        return false;
    }

    protected void logStart(ConversationContext ctx) {
        log.debug("[Agent:{}] start for userId={}", name, ctx.getUserId());
    }

    protected void logEnd(ConversationContext ctx) {
        log.debug("[Agent:{}] end", name);
    }
}