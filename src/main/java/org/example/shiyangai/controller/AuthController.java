package org.example.shiyangai.controller;

import org.example.shiyangai.dto.LoginRequest;
import org.example.shiyangai.dto.RegisterRequest;
import org.example.shiyangai.entity.User;
import org.example.shiyangai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        String userId = userService.register(request.getUsername(), request.getPassword());

        if (userId != null) {
            response.put("success", true);
            response.put("userId", userId);
            response.put("message", "注册成功");
        } else {
            response.put("success", false);
            response.put("message", "用户名已存在");
        }
        return response;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        User user = userService.login(request.getUsername(), request.getPassword());

        if (user != null) {
            response.put("success", true);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("constitution", user.getConstitution());
            response.put("message", "登录成功");
        } else {
            response.put("success", false);
            response.put("message", "用户名或密码错误");
        }
        return response;
    }
}
