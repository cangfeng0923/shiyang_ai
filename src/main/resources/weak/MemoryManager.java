package weak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.mapper.ChatHistoryMapper;
import org.example.shiyangai.service.RedisCacheService;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

/**
 * 职责：历史对话记录 + Redis上下文缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryManager {

    private final ChatHistoryMapper chatHistoryMapper;
    private final RedisCacheService redisCacheService;

    public void loadHistory(ConversationContext ctx) {
        String userId = ctx.getUserId();
        ctx.setHistory(chatHistoryMapper.getByUserId(userId, 6));
        ctx.setPreviousContext(redisCacheService.getChatContext(userId));
    }
}