package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.SolarTermService;
import org.example.shiyangai.service.WeatherService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/solar-term")
@RequiredArgsConstructor
@CrossOrigin
public class SolarTermController {

    private final SolarTermService solarTermService;
    private final WeatherService weatherService;  // ✅ 添加这行

    /**
     * 获取节气养生建议（AI动态生成）
     */
    @GetMapping("/ai-advice")
    public Map<String, Object> getAISolarTermAdvice(
            @RequestParam String userId,
            @RequestParam(required = false) String constitution,
            @RequestParam(required = false, defaultValue = "北京") String city) {

        String constitutionType = constitution != null ? constitution : "平和质";
        String advice = solarTermService.getSolarTermAdviceAI(userId, constitutionType, city);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("advice", advice);
        response.put("term", solarTermService.getCurrentSolarTermName());
        return response;
    }

    /**
     * 获取节气信息
     */
    @GetMapping("/info")
    public Map<String, Object> getSolarTermInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("name", solarTermService.getCurrentSolarTermName());
        response.put("nextTerm", solarTermService.getNextSolarTermName());
        response.put("daysToNext", solarTermService.getDaysToNextSolarTerm());
        return response;
    }

    /**
     * ✅ 新增：获取天气预报接口
     */
    @GetMapping("/weather")
    public Map<String, Object> getWeather(@RequestParam String city) {
        Map<String, Object> response = new HashMap<>();
        try {
            WeatherService.WeatherInfo weather = weatherService.getWeather(city);
            response.put("success", true);
            response.put("city", weather.getCity());
            response.put("today", Map.of(
                    "temp", weather.getTodayTemp(),
                    "weather", weather.getTodayWeather(),
                    "wind", weather.getTodayWind()
            ));
            response.put("daily", weather.getDailyWeather());
        } catch (Exception e) {
            log.error("获取天气失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }
}