package weak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.mapper.ChatHistoryMapper;
import org.example.shiyangai.service.RedisCacheService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 职责：保存对话记录 + 更新Redis上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationSaver {

    private final ChatHistoryMapper chatHistoryMapper;
    private final RedisCacheService redisCacheService;

    public void save(String userId, String role, String content) {
        try {
            ChatHistory chat = new ChatHistory();
            chat.setUserId(userId);
            chat.setRole(role);
            chat.setContent(content);
            chat.setCreatedAt(LocalDateTime.now());
            chatHistoryMapper.insert(chat);
        } catch (Exception e) {
            log.error("保存聊天记录失败: {}", e.getMessage());
        }
    }

    public void updateContext(String userId, String userMessage, String reply) {
        String context = extractContext(userMessage);
        redisCacheService.cacheChatContext(userId, context);
    }

    private String extractContext(String userMessage) {
        if (userMessage == null) return "";
        if (userMessage.length() > 50) return userMessage.substring(0, 50) + "...";
        return userMessage;
    }
}