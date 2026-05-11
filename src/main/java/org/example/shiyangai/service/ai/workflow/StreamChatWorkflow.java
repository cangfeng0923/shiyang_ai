// StreamChatWorkflow.java
package org.example.shiyangai.service.ai.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.DeepSeekClient;
import org.example.shiyangai.service.ai.MessageAssembler;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamChatWorkflow {

    private final AgentWorkflow workflow;
    private final DeepSeekClient deepSeekClient;
    private final MessageAssembler messageAssembler;

    public void execute(String userId, String userMessage, String constitution, SseEmitter emitter) {
        // 1. 初始化上下文
        ConversationContext ctx = ConversationContext.builder()
                .userId(userId)
                .userMessage(userMessage)
                .constitution(constitution)
                .build();

        // 2. 执行 Agent 工作流（预处理）
        workflow.execute(ctx);

        // 3. 如果短路了，直接发送预先生成的内容
        if (ctx.isShortCircuit()) {
            try {
                emitter.send(SseEmitter.event().name("message").data(ctx.getReply()));
                emitter.send(SseEmitter.event().name("complete").data(""));
                emitter.complete();
            } catch (IOException e) {
                log.error("发送短路回复失败", e);
                emitter.completeWithError(e);
            }
            return;
        }

        // 4. 流式调用 LLM
        StringBuilder fullReply = new StringBuilder();
        try {
            var messages = messageAssembler.assemble(ctx);
            deepSeekClient.chatStream(messages, 0.7, 2000,
                    chunk -> {
                        try {
                            fullReply.append(chunk);
                            emitter.send(SseEmitter.event().name("message").data(chunk));
                        } catch (IOException e) {
                            log.error("发送流式数据失败", e);
                        }
                    },
                    () -> {
                        try {
                            ctx.setReply(fullReply.toString());
                            emitter.send(SseEmitter.event().name("complete").data(""));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("流式调用失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("服务异常：" + e.getMessage()));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}