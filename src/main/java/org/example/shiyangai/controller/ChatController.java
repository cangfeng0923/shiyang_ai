package org.example.shiyangai.controller;

import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.service.AIService;
import org.example.shiyangai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private AIService aiService;

    @Autowired
    private UserService userService;

    /**
     * 流式对话接口（SSE）
     * 使用方式：GET /api/chat/stream?userId=xxx&message=榴莲怎么样&constitution=阴虚质
     */
    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter chatStream(
            @RequestParam String userId,
            @RequestParam String message,
            @RequestParam(required = false) String constitution) {

        // 如果前端没有传体质，从数据库获取
        if (constitution == null || constitution.isEmpty()) {
            var user = userService.getUserById(userId);
            if (user != null && user.getConstitution() != null) {
                constitution = user.getConstitution();
            } else {
                constitution = "平和质"; // 默认体质
            }
        }

        return aiService.chatStream(userId, message, constitution);
    }

    /**
     * 普通对话接口（原有功能，保留兼容）
     * 使用方式：POST /api/chat/chat
     */
    @PostMapping("/chat")
    public Map<String, Object> sendMessage(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");

        String constitution = "平和质";
        var user = userService.getUserById(userId);
        if (user != null && user.getConstitution() != null) {
            constitution = user.getConstitution();
        }

        // 使用真正的 AI Agent
        String reply = aiService.chat(userId, message, constitution);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("reply", reply);
        response.put("constitution", constitution);

        return response;
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history/{userId}")
    public List<ChatHistory> getHistory(@PathVariable String userId, @RequestParam(defaultValue = "50") int limit) {
        return aiService.getChatHistory(userId, limit);
    }
}