// AIService.java（修复版 - 使用正确的 Pre/Post 流程）
package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.mapper.ChatHistoryMapper;
import org.example.shiyangai.service.ai.MessageAssembler;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.example.shiyangai.service.ai.llm.LLMClient;
import org.example.shiyangai.service.ai.workflow.AgentWorkflow;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * AI 服务门面
 *
 * 正确流程：
 * 1. preProcess - 加载数据、识别意图、检测风险、组装 Prompt
 * 2. LLM 调用
 * 3. postProcess - 保存历史、更新缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AgentWorkflow workflow;
    private final LLMClient llmClient;
    private final MessageAssembler messageAssembler;
    private final ChatHistoryMapper chatHistoryMapper;

    /**
     * 同步对话
     */
    public String chat(String userId, String userMessage, String constitution) {
        log.info("Chat request: userId={}, message={}", userId, userMessage);

        ConversationContext ctx = ConversationContext.builder()
                .userId(userId)
                .userMessage(userMessage)
                .constitution(constitution)
                .build();

        // 1. 预处理（数据加载、意图识别、风险检测、Prompt生成）
        workflow.preProcess(ctx);

        // 2. 如果风险检测短路，直接返回警告
        if (ctx.isShortCircuit()) {
            log.info("Request short-circuited by risk control");
            // 后处理仍然需要（保存用户消息）
            workflow.postProcess(ctx);
            return ctx.getReply();
        }

        // 3. 调用 LLM
        if (ctx.getSystemPrompt() != null) {
            try {
                var messages = messageAssembler.assemble(ctx);
                String reply = llmClient.chatSync(messages, 0.7, 2000);
                ctx.setReply(reply);
            } catch (Exception e) {
                log.error("LLM调用失败", e);
                ctx.setReply("服务暂时不可用，请稍后再试。");
            }
        } else {
            log.warn("SystemPrompt is null, skip LLM call");
            ctx.setReply("抱歉，我暂时无法处理这个请求。");
        }

        // 4. 后处理（保存历史、更新缓存）
        workflow.postProcess(ctx);

        return ctx.getReply();
    }

    /**
     * 流式对话
     */
    public SseEmitter chatStream(String userId, String userMessage, String constitution) {
        SseEmitter emitter = new SseEmitter(120000L);

        ConversationContext ctx = ConversationContext.builder()
                .userId(userId)
                .userMessage(userMessage)
                .constitution(constitution)
                .build();

        // 1. 预处理
        workflow.preProcess(ctx);

        // 2. 短路处理
        if (ctx.isShortCircuit()) {
            try {
                emitter.send(SseEmitter.event().name("message").data(ctx.getReply()));
                emitter.send(SseEmitter.event().name("complete").data(""));
                emitter.complete();
                workflow.postProcess(ctx);  // 仍然执行后处理
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 3. 流式调用
        StringBuilder fullReply = new StringBuilder();
        try {
            var messages = messageAssembler.assemble(ctx);
            llmClient.chatStream(messages, 0.7, 2000,
                    chunk -> {
                        try {
                            fullReply.append(chunk);
                            emitter.send(SseEmitter.event().name("message").data(chunk));
                        } catch (IOException e) {
                            log.error("发送失败", e);
                        }
                    },
                    () -> {
                        try {
                            ctx.setReply(fullReply.toString());
                            // 4. 后处理
                            workflow.postProcess(ctx);
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

        return emitter;
    }

    public List<ChatHistory> getChatHistory(String userId, int limit) {
        return chatHistoryMapper.getByUserId(userId, limit);
    }
}