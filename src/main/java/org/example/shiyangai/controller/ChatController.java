package org.example.shiyangai.controller;

import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.service.AIService;
import org.example.shiyangai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/history/{userId}")
    public List<ChatHistory> getHistory(@PathVariable String userId, @RequestParam(defaultValue = "50") int limit) {
        return aiService.getChatHistory(userId, limit);
    }
}