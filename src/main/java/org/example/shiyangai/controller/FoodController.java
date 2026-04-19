package org.example.shiyangai.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.dto.AssessmentRequest;
import org.example.shiyangai.dto.FoodAnalysisRequest;
import org.example.shiyangai.dto.FoodAnalysisResponse;
import org.example.shiyangai.service.FoodAnalysisService;
import org.example.shiyangai.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
// 如果需要限流，在方法上添加：
// @RateLimiter(name = "api-limiter", fallbackMethod = "fallback")
//@Tag(name = "食养AI接口", description = "饮食分析和体质测评相关接口")
public class FoodController {

    private final FoodAnalysisService analysisService;
    private final UserService userService;

    /**
     * 饮食分析接口
     */
    @PostMapping("/analyze")
//    @Operation(summary = "饮食分析", description = "根据用户体质和食物名称，返回饮食建议")
    public FoodAnalysisResponse analyze(@Valid @RequestBody FoodAnalysisRequest request) {
        log.info("收到分析请求: foodName={}", request.getFoodName());

        // 确保用户存在
        String userId = userService.getOrCreateUser(request.getUserId());
        request.setUserId(userId);

        return analysisService.analyze(request);
    }

    /**
     * 体质测评接口
     */
    @PostMapping("/assessment")
//    @Operation(summary = "体质测评", description = "完成10道题，返回体质类型并保存")
    public Map<String, Object> assessment(@RequestBody AssessmentRequest request) {
        log.info("收到测评请求: userId={}", request.getUserId());

        String userId = userService.getOrCreateUser(request.getUserId());
        String constitution = analysisService.quickAssessmentAndSave(userId, request.getAnswers());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("constitution", constitution);
        response.put("message", "测评完成！您的体质类型是：" + constitution);

        return response;
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user/{userId}")
//    @Operation(summary = "获取用户信息", description = "查询用户的体质类型")
    public Map<String, Object> getUserInfo(@PathVariable String userId) {
        String userIdValid = userService.getOrCreateUser(userId);
        String constitution = userService.getConstitution(userIdValid);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userIdValid);
        response.put("constitution", constitution != null ? constitution : "未测评");
        response.put("hasAssessment", constitution != null);

        return response;
    }

    /**
     * 获取用户历史记录
     */
    @GetMapping("/user/{userId}/history")
//    @Operation(summary = "获取饮食历史", description = "获取用户最近的饮食分析记录")
    public Map<String, Object> getHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {

        String userIdValid = userService.getOrCreateUser(userId);
        var history = userService.getFoodHistory(userIdValid, Math.min(limit, 50));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userIdValid);
        response.put("total", history.size());
        response.put("records", history);

        return response;
    }

    /**
     * 批量分析
     */
    @PostMapping("/batch-analyze")
//    @Operation(summary = "批量分析", description = "一次分析多种食物")
    public Map<String, FoodAnalysisResponse> batchAnalyze(
            @RequestParam String userId,
            @RequestParam String[] foodNames,
            @RequestParam(required = false) String constitution) {

        String userIdValid = userService.getOrCreateUser(userId);
        return analysisService.batchAnalyze(userIdValid, foodNames, constitution);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
//    @Operation(summary = "健康检查")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "食养AI");
        status.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return status;
    }
}
