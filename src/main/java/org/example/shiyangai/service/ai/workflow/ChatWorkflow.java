// ChatWorkflow.java
package org.example.shiyangai.service.ai.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.DeepSeekClient;
import org.example.shiyangai.service.ai.MessageAssembler;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWorkflow {

    private final AgentWorkflow workflow;
    private final DeepSeekClient deepSeekClient;
    private final MessageAssembler messageAssembler;

    public String execute(String userId, String userMessage, String constitution) {
        // 1. 初始化上下文
        ConversationContext ctx = ConversationContext.builder()
                .userId(userId)
                .userMessage(userMessage)
                .constitution(constitution)
                .build();

        // 2. 执行 Agent 工作流（预处理）
        workflow.execute(ctx);

        // 3. 如果短路了，直接返回预先生成的内容
        if (ctx.isShortCircuit()) {
            log.info("Workflow short-circuited, returning pre-generated content");
            return ctx.getReply();
        }

        // 4. 调用 LLM
        try {
            var messages = messageAssembler.assemble(ctx);
            String reply = deepSeekClient.chatSync(messages, 0.7, 2000);
            ctx.setReply(reply);
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            ctx.setReply("抱歉，AI服务暂时不可用，请稍后再试。");
        }

        // 5. 后处理已经在 workflow.execute 中执行了
        return ctx.getReply();
    }
}