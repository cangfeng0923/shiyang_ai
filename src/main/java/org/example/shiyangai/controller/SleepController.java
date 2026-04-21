package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.SleepRecord;
import org.example.shiyangai.service.SleepService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 睡眠记录控制器 (Phase 2)
 */
@Slf4j
@RestController
@RequestMapping("/api/sleep")
@RequiredArgsConstructor
@CrossOrigin
public class SleepController {

    private final SleepService sleepService;

    /**
     * 添加睡眠记录
     */
    @PostMapping("/record")
    public Map<String, Object> addRecord(@RequestBody SleepRecord record) {
        sleepService.addSleepRecord(record);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "记录成功");
        response.put("sleepScore", sleepService.calculateSleepScore(record));
        return response;
    }

    /**
     * 获取今日睡眠记录
     */
    @GetMapping("/today/{userId}")
    public Map<String, Object> getTodayRecords(@PathVariable String userId) {
        List<SleepRecord> records = sleepService.getTodayRecords(userId);

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
        List<SleepRecord> records = sleepService.getRecordsByDate(userId, recordDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("date", date);
        response.put("records", records);
        return response;
    }

    /**
     * 获取一周睡眠记录
     */
    @GetMapping("/week/{userId}")
    public Map<String, Object> getWeekRecords(@PathVariable String userId) {
        List<SleepRecord> records = sleepService.getWeekRecords(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("records", records);
        response.put("count", records.size());
        return response;
    }

    /**
     * 获取睡眠报告
     */
    @GetMapping("/report/{userId}")
    public Map<String, Object> getSleepReport(@PathVariable String userId) {
        String report = sleepService.generateSleepReport(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("report", report);
        return response;
    }
}