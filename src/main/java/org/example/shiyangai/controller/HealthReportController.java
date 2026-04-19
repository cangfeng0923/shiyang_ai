// controller/HealthReportController.java
package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.HealthReportService;
import org.example.shiyangai.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@CrossOrigin
public class HealthReportController {

    private final HealthReportService healthReportService;
    private final UserService userService;

    /**
     * 获取综合健康报告
     */
    @GetMapping("/comprehensive/{userId}")
    public Map<String, Object> getComprehensiveReport(@PathVariable String userId) {
        String constitution = userService.getConstitution(userId);
        if (constitution == null) {
            constitution = "平和质";
        }

        String report = healthReportService.generateComprehensiveReport(userId, constitution);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("report", report);
        response.put("constitution", constitution);
        return response;
    }
}