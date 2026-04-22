package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.service.DietRecordService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/diet")
@RequiredArgsConstructor
@CrossOrigin
public class DietController {

    private final DietRecordService dietRecordService;

    @PostMapping("/record")
    public Map<String, Object> addRecord(@RequestBody DietRecord record) {
        dietRecordService.addRecord(record);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "记录成功");
        response.put("healthScore", record.getHealthScore());
        response.put("suggestions", record.getSuggestions());
        return response;
    }

    /**
     * 更新饮食记录
     */
    @PutMapping("/record/{id}")
    public Map<String, Object> updateRecord(@PathVariable String id, @RequestBody DietRecord record) {
        record.setId(id);
        boolean success = dietRecordService.updateRecord(record);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "更新成功" : "更新失败");
        if (success) {
            response.put("healthScore", record.getHealthScore());
            response.put("suggestions", record.getSuggestions());
        }
        return response;
    }

    /**
     * 删除饮食记录
     */
    @DeleteMapping("/record/{id}")
    public Map<String, Object> deleteRecord(@PathVariable String id, @RequestParam String userId) {
        boolean success = dietRecordService.deleteRecord(id, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "删除成功" : "删除失败");
        return response;
    }

    @GetMapping("/today/{userId}")
    public Map<String, Object> getTodayRecords(@PathVariable String userId) {
        List<DietRecord> records = dietRecordService.getTodayRecords(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("records", records);
        response.put("count", records.size());
        return response;
    }

    @GetMapping("/records/{userId}")
    public Map<String, Object> getRecordsByDate(@PathVariable String userId,
                                                @RequestParam String date) {
        LocalDate recordDate = LocalDate.parse(date);
        List<DietRecord> records = dietRecordService.getRecordsByDate(userId, recordDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("date", date);
        response.put("records", records);
        return response;
    }

    @GetMapping("/week/{userId}")
    public Map<String, Object> getWeekRecords(@PathVariable String userId) {
        List<DietRecord> records = dietRecordService.getWeekRecords(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("records", records);
        response.put("count", records.size());
        return response;
    }

    @GetMapping("/report/{userId}")
    public Map<String, Object> getDailyReport(@PathVariable String userId,
                                              @RequestParam(required = false) String constitution) {
        String constitutionType = constitution != null ? constitution : "平和质";
        Map<String, Object> report = dietRecordService.generateDailyReport(userId, constitutionType);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("report", report);
        return response;
    }
}