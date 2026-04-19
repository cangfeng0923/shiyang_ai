package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.SolarTermService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/solar-term")
@RequiredArgsConstructor
@CrossOrigin
//节气接口
public class SolarTermController {

    private final SolarTermService solarTermService;

    /**
     * 获取当前节气养生建议
     */
    @GetMapping("/current")
    public Map<String, Object> getCurrentSolarTerm(@RequestParam(required = false) String constitution) {
        String constitutionType = constitution != null ? constitution : "平和质";
        String advice = solarTermService.getSolarTermAdvice(constitutionType);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("advice", advice);
        return response;
    }

    /**
     * 获取节气信息
     */
    @GetMapping("/info")
    public Map<String, Object> getSolarTermInfo() {
        SolarTermService.SolarTermInfo term = solarTermService.getCurrentSolarTerm();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("name", term.getName());
        response.put("principle", term.getPrinciple());
        response.put("foodAdvice", term.getFoodAdvice());
        response.put("recipe", term.getRecipe());
        response.put("dailyAdvice", term.getDailyAdvice());
        return response;
    }
}