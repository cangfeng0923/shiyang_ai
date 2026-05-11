// PostProcessAgent.java (修复版)
package org.example.shiyangai.service.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.mapper.ChatHistoryMapper;
import org.example.shiyangai.service.RedisCacheService;
import org.example.shiyangai.service.UserService;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class PostProcessAgent extends BaseAgent {

    private final ChatHistoryMapper chatHistoryMapper;
    private final RedisCacheService redisCacheService;
    private final UserService userService;
    private final ExecutorService executorService;

    public PostProcessAgent(ChatHistoryMapper chatHistoryMapper,
                            RedisCacheService redisCacheService,
                            UserService userService,
                            @Qualifier("aiExecutorService") ExecutorService executorService) {
        super("PostProcessAgent");
        this.chatHistoryMapper = chatHistoryMapper;
        this.redisCacheService = redisCacheService;
        this.userService = userService;
        this.executorService = executorService;
    }

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public boolean isPostProcess() {
        return true;
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String userId = ctx.getUserId();
        String userMessage = ctx.getUserMessage();
        String reply = ctx.getReply();

        if (userMessage != null && reply != null) {
            save(userId, "user", userMessage);
            save(userId, "assistant", reply);
            updateContext(userId, userMessage);
        }

        // 异步保存食材历史（使用有界线程池）
        if (ctx.getFoodInfo() != null && ctx.getFoodName() != null) {
            executorService.submit(() -> saveFoodHistory(userId, ctx));
        }

        logEnd(ctx);
        return true;
    }

    private void save(String userId, String role, String content) {
        try {
            ChatHistory chat = new ChatHistory();
            chat.setUserId(userId);
            chat.setRole(role);
            chat.setContent(content);
            chat.setCreatedAt(LocalDateTime.now());
            chatHistoryMapper.insert(chat);
        } catch (Exception e) {
            log.error("Save failed: {}", e.getMessage());
        }
    }

    private void updateContext(String userId, String userMessage) {
        String context = userMessage != null && userMessage.length() > 50
                ? userMessage.substring(0, 50) + "..."
                : userMessage;
        redisCacheService.cacheChatContext(userId, context);
    }

    private void saveFoodHistory(String userId, ConversationContext ctx) {
        try {
            userService.saveFoodHistory(userId, ctx.getFoodName(), ctx.getConstitution(),
                    "适合", "建议", "{}");
        } catch (Exception e) {
            log.error("Save food history failed: {}", e.getMessage());
        }
    }
}