package org.example.shiyangai.controller;

import org.example.shiyangai.agent.ReActAgent;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.service.AIService;
import org.example.shiyangai.service.AIReportService;  // 新增
import org.example.shiyangai.service.diet.DietRecordService;
import org.example.shiyangai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private ReActAgent reactAgent;

    @Autowired
    private UserService userService;

    @Autowired
    private DietRecordService dietRecordService;

    @Autowired
    private AIService aiService;

    @Autowired
    private AIReportService aiReportService;  // 新增

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

    @PostMapping("/diet-report/{userId}")
    public ResponseEntity<?> generateDietReport(@PathVariable String userId,
                                                @RequestBody(required = false) Map<String, Object> params) {
        String constitution = params != null && params.containsKey("constitution") ?
                (String) params.get("constitution") : "平和质";

        List<DietRecord> weekRecords = dietRecordService.getWeekRecords(userId);

        if (weekRecords == null || weekRecords.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "aiReport", "📝 本周暂无饮食记录。开始记录您的每日饮食，AI将为您生成个性化分析和建议。",
                    "stats", Map.of("avgScore", 0, "totalRecords", 0, "recordDays", 0),
                    "records", List.of()
            ));
        }

        // 使用新的 AIReportService 生成报告
        String aiReport = aiReportService.generateDietReport(weekRecords, constitution);

        // 计算统计数据
        Map<String, Object> stats = calculateWeekStats(weekRecords);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "aiReport", aiReport,
                "stats", stats,
                "records", weekRecords
        ));
    }

    private Map<String, Object> calculateWeekStats(List<DietRecord> records) {
        Map<String, Object> stats = new HashMap<>();
        Set<String> uniqueDates = new HashSet<>();
        double totalScore = 0;

        for (DietRecord r : records) {
            totalScore += r.getHealthScore() != null ? r.getHealthScore() : 0;
            if (r.getRecordDate() != null) {
                uniqueDates.add(r.getRecordDate().toLocalDate().toString());
            }
        }

        stats.put("avgScore", records.isEmpty() ? 0 : Math.round(totalScore / records.size()));
        stats.put("totalRecords", records.size());
        stats.put("recordDays", uniqueDates.size());

        return stats;
    }
}