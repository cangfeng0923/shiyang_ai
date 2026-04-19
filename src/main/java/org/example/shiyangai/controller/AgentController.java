package org.example.shiyangai.controller;

import org.example.shiyangai.agent.ReActAgent;
import org.example.shiyangai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private ReActAgent reactAgent;

    @Autowired
    private UserService userService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");

        String constitution = "平和质";
        var user = userService.getUserById(userId);
        if (user != null && user.getConstitution() != null) {
            constitution = user.getConstitution();
        }

        Map<String, Object> response = new HashMap<>();

        try {
            // 使用 ReAct Agent 处理
            String reply = reactAgent.execute(message, constitution);
            response.put("success", true);
            response.put("reply", reply);
            response.put("constitution", constitution);
        } catch (Exception e) {
            response.put("success", false);
            response.put("reply", "处理失败: " + e.getMessage());
        }

        return response;
    }
}
