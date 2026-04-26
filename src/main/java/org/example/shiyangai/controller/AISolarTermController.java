package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.AIService;
import org.example.shiyangai.service.WeatherService;
import org.example.shiyangai.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/solar-term")
@RequiredArgsConstructor
@CrossOrigin
public class AISolarTermController {

    private final AIService aiService;
    private final UserService userService;
    private final WeatherService weatherService;

    /**
     * 获取节气养生建议（AI动态生成）
     */
    @GetMapping("/advice")
    public Map<String, Object> getSolarTermAdvice(
            @RequestParam String userId,
            @RequestParam(required = false) String constitution,
            @RequestParam(required = false, defaultValue = "北京") String city) {

        String constitutionType = constitution != null ? constitution : "平和质";
        var user = userService.getUserById(userId);
        if (user != null && user.getConstitution() != null) {
            constitutionType = user.getConstitution();
        }

        // 获取节气名称和天气信息
        String termName = getCurrentSolarTermName();
        String termClimate = getCurrentSolarTermClimate(termName);
        var weather = weatherService.getWeather(city);

        // 调用AI生成
        String advice = aiService.generateSolarTermAdvice(termName, termClimate, constitutionType, weather);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("advice", advice);
        response.put("term", termName);
        return response;
    }

    private String getCurrentSolarTermName() {
        // 根据当前日期返回节气名称
        int month = java.time.LocalDate.now().getMonthValue();
        int day = java.time.LocalDate.now().getDayOfMonth();
        if (month == 4 && day >= 4) return "清明";
        if (month == 4 && day >= 20) return "谷雨";
        if (month == 5 && day >= 5) return "立夏";
        if (month == 5 && day >= 21) return "小满";
        return "春分";
    }

    private String getCurrentSolarTermClimate(String termName) {
        Map<String, String> map = new HashMap<>();
        map.put("清明", "气清景明，万物皆显");
        map.put("谷雨", "雨生百谷，湿气加重");
        map.put("立夏", "夏季开始，万物繁茂");
        map.put("小满", "麦类灌浆，湿热渐起");
        map.put("春分", "昼夜平分，阴阳平衡");
        return map.getOrDefault(termName, "节气交替，注意养生");
    }
}