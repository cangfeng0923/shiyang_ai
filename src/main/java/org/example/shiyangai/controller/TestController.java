package org.example.shiyangai.controller;

import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.enums.ConstitutionType;
import org.example.shiyangai.service.BaiduBaikeService;
import org.example.shiyangai.service.ConstitutionService;
import org.example.shiyangai.service.NutritionApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @Autowired
    private NutritionApiService nutritionApiService;;
    @Autowired
    private ConstitutionService constitutionService;

    @GetMapping("/test")
    public String test() {
        return "食养AI后端运行成功！";
    }

    // 测试USDA API
    @GetMapping("/test/nutrition")
    public Map<String, Object> testNutrition(@RequestParam String food) {
        return nutritionApiService.getNutrition(food);
    }

    // 测试体质匹配
    @GetMapping("/test/constitution")
    public Map<String, Object> testConstitution(
            @RequestParam String constitution,
            @RequestParam String food) {

        ConstitutionType type = ConstitutionType.fromName(constitution);
        ConstitutionType.SuitabilityResult result = type.evaluate(food);

        Map<String, Object> response = new HashMap<>();
        response.put("constitution", constitution);
        response.put("food", food);
        response.put("suitability", result.getSuitability());
        response.put("reason", result.getReason());
        response.put("suggestion", result.getSuggestion());

        return response;
    }

    // 测试体质测评算法
    @GetMapping("/test/assessment")
    public String testAssessment() {
        // 模拟一个阳虚质的答案
        boolean[] answers = new boolean[10];
        answers[1] = true;  // 怕冷 → 阳虚
        answers[2] = false;
        answers[3] = false;

        return constitutionService.quickAssessment(answers);
    }

    // 获取所有体质规则
    @GetMapping("/test/rules")
    public Map<String, Object> testRules(@RequestParam String constitution) {
        return constitutionService.getConstitutionRules(constitution);
    }

    // 在 TestController.java 中添加
    @Autowired
    private BaiduBaikeService baiduBaikeService;

    @GetMapping("/test/baike")
    public Map<String, Object> testBaiduBaike(@RequestParam String food) {
        Map<String, Object> result = new HashMap<>();
        try {
            IngredientInfo info = baiduBaikeService.getIngredientInfo(food);
            result.put("success", true);
            result.put("foodName", info.getName());
            result.put("property", info.getProperty());
            result.put("flavor", info.getFlavor());
            result.put("effect", info.getEffect());
            result.put("contraindication", info.getContraindication());
            result.put("source", info.getSource());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}