// MessageAssembler.java
package org.example.shiyangai.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MessageAssembler {

    /**
     * 组装完整消息列表
     */
    public List<Map<String, String>> assemble(ConversationContext ctx) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System 消息
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", ctx.getSystemPrompt());
        messages.add(systemMsg);

        // 历史消息
        int historyCount = 0;
        for (ChatHistory h : ctx.getHistory()) {
            if (historyCount >= 6) break;
            Map<String, String> historyMsg = new HashMap<>();
            String role = h.getRole();
            if ("ai".equals(role)) role = "assistant";
            historyMsg.put("role", role);
            historyMsg.put("content", h.getContent());
            messages.add(historyMsg);
            historyCount++;
        }

        // 当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", ctx.getUserMessage());
        messages.add(userMsg);

        return messages;
    }
}