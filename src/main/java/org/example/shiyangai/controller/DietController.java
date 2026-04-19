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
//饮食记录接口
public class DietController {

    private final DietRecordService dietRecordService;

    /**
     * 添加饮食记录
     */
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
     * 获取今日饮食记录
     */
    @GetMapping("/today/{userId}")
    public Map<String, Object> getTodayRecords(@PathVariable String userId) {
        List<DietRecord> records = dietRecordService.getTodayRecords(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("records", records);
        response.put("count", records.size());
        return response;
    }

    /**
     * 获取指定日期记录
     */
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

    /**
     * 获取一周记录
     */
    @GetMapping("/week/{userId}")
    public Map<String, Object> getWeekRecords(@PathVariable String userId) {
        List<DietRecord> records = dietRecordService.getWeekRecords(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("records", records);
        response.put("count", records.size());
        return response;
    }

    /**
     * 获取今日饮食报告
     */
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