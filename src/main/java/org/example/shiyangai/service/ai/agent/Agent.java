// Agent.java
package org.example.shiyangai.service.ai.agent;

import org.example.shiyangai.service.ai.context.ConversationContext;

/**
 * Agent 统一接口
 */
public interface Agent {

    /**
     * Agent 唯一标识
     */
    String getName();

    /**
     * 是否支持处理当前上下文
     * @param ctx 对话上下文
     * @return true=支持，false=不支持
     */
    boolean supports(ConversationContext ctx);

    /**
     * 执行 Agent 逻辑
     * @param ctx 对话上下文（可读可写）
     * @return 是否继续下一个 Agent
     */
    boolean execute(ConversationContext ctx);

    /**
     * Agent 优先级（数字越小越先执行）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否需要后处理（如调用 LLM 后执行）
     */
    default boolean isPostProcess() {
        return false;
    }
}