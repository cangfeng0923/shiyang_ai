package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.service.HealthProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin
public class ProfileController {

    private final HealthProfileService profileService;

    /**
     * 获取健康档案
     */
    @GetMapping("/{userId}")
    public Map<String, Object> getProfile(@PathVariable String userId) {
        HealthProfile profile = profileService.getProfile(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        if (profile != null) {
            response.put("profile", profile);
            response.put("allergies", profileService.parseJsonArray(profile.getAllergies()));
            response.put("foodAvoidance", profileService.parseJsonArray(profile.getFoodAvoidance()));
            // ✅ 修改：使用 pastDiseases 替代 chronicDiseases
            response.put("pastDiseases", profileService.parseJsonArray(profile.getPastDiseases()));
        } else {
            response.put("profile", null);
            response.put("message", "尚未建立健康档案");
        }

        return response;
    }

    /**
     * 保存健康档案
     */
    @PostMapping("/save")
    public Map<String, Object> saveProfile(@RequestBody HealthProfile profile) {
        log.info("收到保存请求: userId={}", profile.getUserId());
        log.info("allergies 原始值: {}", profile.getAllergies());
        log.info("foodAvoidance 原始值: {}", profile.getFoodAvoidance());
        log.info("pastDiseases 原始值: {}", profile.getPastDiseases());

        profileService.saveProfile(profile);

        // ✅ 验证保存后的数据
        HealthProfile saved = profileService.getProfile(profile.getUserId());
        log.info("保存后验证: allergies={}, foodAvoidance={}, pastDiseases={}",
                saved != null ? saved.getAllergies() : null,
                saved != null ? saved.getFoodAvoidance() : null,
                saved != null ? saved.getPastDiseases() : null);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "健康档案保存成功");
        return response;
    }
    /**
     * 更新单个字段
     */
    @PutMapping("/{userId}/{field}")
    public Map<String, Object> updateField(@PathVariable String userId,
                                           @PathVariable String field,
                                           @RequestBody Map<String, Object> body) {
        Object value = body.get("value");
        profileService.updateField(userId, field, value);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "更新成功");
        return response;
    }

    /**
     * 检查食物过敏
     */
    @GetMapping("/check-allergy")
    public Map<String, Object> checkAllergy(@RequestParam String userId, @RequestParam String food) {
        String allergy = profileService.checkAllergy(userId, food);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("hasAllergy", allergy != null);
        if (allergy != null) {
            response.put("allergy", allergy);
            response.put("warning", "⚠️ 该食物含有您过敏的食材：" + allergy);
        } else {
            response.put("message", "未发现过敏原");
        }
        return response;
    }
}