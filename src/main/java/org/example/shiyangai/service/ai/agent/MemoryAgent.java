// MemoryAgent.java
package org.example.shiyangai.service.ai.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.mapper.ChatHistoryMapper;
import org.example.shiyangai.service.RedisCacheService;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemoryAgent extends BaseAgent {

    private final ChatHistoryMapper chatHistoryMapper;
    private final RedisCacheService redisCacheService;

    public MemoryAgent(ChatHistoryMapper chatHistoryMapper,
                       RedisCacheService redisCacheService) {
        super("MemoryAgent");
        this.chatHistoryMapper = chatHistoryMapper;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String userId = ctx.getUserId();
        ctx.setHistory(chatHistoryMapper.getByUserId(userId, 10));
        ctx.setPreviousContext(redisCacheService.getChatContext(userId));

        logEnd(ctx);
        return true;
    }
}