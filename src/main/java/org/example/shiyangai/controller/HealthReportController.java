// controller/HealthReportController.java
package org.example.shiyangai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.EnhancedHealthReportService;
import org.example.shiyangai.service.HealthReportService;
import org.example.shiyangai.service.SleepService;
import org.example.shiyangai.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@CrossOrigin
public class HealthReportController {

    private final HealthReportService healthReportService;
    private final EnhancedHealthReportService enhancedHealthReportService;
    private final UserService userService;
    private final SleepService sleepService;

    /**
     * 获取综合健康报告 (基础版)
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
        response.put("reportType", "comprehensive");
        response.put("generatedAt", LocalDateTime.now().toString());
        return response;
    }

    /**
     * 获取增强版综合健康报告 (Phase 1 - 饮食深度分析)
     */
    @GetMapping("/enhanced/{userId}")
    public Map<String, Object> getEnhancedReport(@PathVariable String userId) {
        String constitution = userService.getConstitution(userId);
        if (constitution == null) {
            constitution = "平和质";
        }

        String report = enhancedHealthReportService.generateEnhancedReport(userId, constitution);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("report", report);
        response.put("constitution", constitution);
        response.put("reportType", "enhanced");
        response.put("generatedAt", LocalDateTime.now().toString());
        return response;
    }

    /**
     * 获取融合报告（饮食+睡眠） (Phase 2)
     */
    @GetMapping("/fusion/{userId}")
    public Map<String, Object> getFusionReport(@PathVariable String userId) {
        String constitution = userService.getConstitution(userId);
        if (constitution == null) {
            constitution = "平和质";
        }

        String dietaryReport = enhancedHealthReportService.generateEnhancedReport(userId, constitution);
        String sleepReport = sleepService.generateSleepReport(userId);

        String fusionReport = generateFusionReport(dietaryReport, sleepReport, userId, constitution);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("report", fusionReport);
        response.put("constitution", constitution);
        response.put("reportType", "fusion_diet_sleep");
        response.put("generatedAt", LocalDateTime.now().toString());
        return response;
    }

    /**
     * 获取完整融合报告（饮食+睡眠+运动） (Phase 3)
     */
    @GetMapping("/full/{userId}")
    public Map<String, Object> getFullReport(@PathVariable String userId) {
        String constitution = userService.getConstitution(userId);
        if (constitution == null) {
            constitution = "平和质";
        }

        String dietaryReport = enhancedHealthReportService.generateEnhancedReport(userId, constitution);
        String sleepReport = sleepService.generateSleepReport(userId);
        // TODO: 运动报告将在 Phase 3 添加
        String activityReport = "运动数据待接入";

        String fullReport = generateFullReport(dietaryReport, sleepReport, activityReport, userId, constitution);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("report", fullReport);
        response.put("constitution", constitution);
        response.put("reportType", "full_diet_sleep_activity");
        response.put("generatedAt", LocalDateTime.now().toString());
        return response;
    }

    /**
     * 生成融合报告（饮食+睡眠）
     */
    private String generateFusionReport(String dietaryReport, String sleepReport, String userId, String constitution) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 🌟 **本周健康画像**\n\n");

        // 添加饮食评分
        int dietaryScore = extractScoreFromReport(dietaryReport, "平均健康评分");
        sb.append("🍽️ **饮食评分**：").append(dietaryScore).append("/100");
        sb.append(" (").append(getTrendIcon(dietaryScore)).append(")\n");

        // 添加睡眠评分
        int sleepScore = extractScoreFromReport(sleepReport, "平均睡眠评分");
        sb.append("😴 **睡眠评分**：").append(sleepScore).append("/100");
        sb.append(" (").append(getTrendIcon(sleepScore)).append(")\n\n");

        // 关联洞察
        sb.append("## 🔍 **关联洞察**\n\n");

        // 示例关联分析
        if (sleepScore < 70 && dietaryScore < 70) {
            sb.append("⚠️ **饮食-睡眠关联**：本周您的饮食和睡眠质量都偏低，可能存在相互影响\n");
            sb.append("   → 建议：改善晚餐时间，避免高脂高糖食物影响睡眠质量\n\n");
        } else if (sleepScore < 70 && dietaryScore >= 70) {
            sb.append("⚠️ **睡眠影响饮食**：睡眠质量不佳可能影响次日食欲和饮食选择\n");
            sb.append("   → 建议：关注睡眠环境，睡前2小时避免进食\n\n");
        } else if (sleepScore >= 70 && dietaryScore < 70) {
            sb.append("✅ **睡眠促进恢复**：良好的睡眠有助于身体恢复，但饮食仍需改善\n");
            sb.append("   → 建议：在充足睡眠基础上，进一步优化饮食结构\n\n");
        } else {
            sb.append("🎉 **饮食-睡眠协同**：饮食和睡眠都处于良好状态，形成良性循环\n");
            sb.append("   → 建议：继续保持当前的生活方式\n\n");
        }

        // 个性化建议
        sb.append("## 💡 **个性化建议**\n\n");
        if (sleepScore < 70) {
            sb.append("- 🛌 **睡眠优化**：尝试在22:30前入睡，避免睡前使用电子设备\n");
        }
        if (dietaryScore < 70) {
            sb.append("- 🥗 **饮食调整**：增加蔬菜摄入，减少加工食品\n");
        }

        sb.append("\n## 🎯 **下周小目标**\n\n");
        if (sleepScore < 70) {
            sb.append("- 🌙 连续3天在23:00前入睡\n");
        }
        if (dietaryScore < 70) {
            sb.append("- 🥬 每天摄入至少300g蔬菜\n");
        }

        return sb.toString();
    }

    /**
     * 生成完整报告（饮食+睡眠+运动）
     */
    private String generateFullReport(String dietaryReport, String sleepReport, String activityReport,
                                      String userId, String constitution) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 🌟 **本周完整健康画像**\n\n");

        // 添加饮食评分
        int dietaryScore = extractScoreFromReport(dietaryReport, "平均健康评分");
        sb.append("🍽️ **饮食评分**：").append(dietaryScore).append("/100\n");

        // 添加睡眠评分
        int sleepScore = extractScoreFromReport(sleepReport, "平均睡眠评分");
        sb.append("😴 **睡眠评分**：").append(sleepScore).append("/100\n");

        // 添加运动评分（待实现）
        int activityScore = 75; // 临时默认值
        sb.append("🏃 **运动评分**：").append(activityScore).append("/100\n\n");

        // 综合评分
        int totalScore = (dietaryScore + sleepScore + activityScore) / 3;
        sb.append("## 📊 **综合健康评分**：").append(totalScore).append("/100\n\n");

        // 雷达图数据（用于前端可视化）
        sb.append("## 📈 **健康雷达图数据**\n\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"diet\": ").append(dietaryScore).append(",\n");
        sb.append("  \"sleep\": ").append(sleepScore).append(",\n");
        sb.append("  \"activity\": ").append(activityScore).append("\n");
        sb.append("}\n");
        sb.append("```\n\n");

        // 多维度关联分析
        sb.append("## 🔍 **多维度关联分析**\n\n");

        // 三个维度的综合判断
        if (totalScore >= 80) {
            sb.append("🎉 **优秀**：三个维度表现优秀，健康状态良好！\n");
        } else if (totalScore >= 60) {
            sb.append("👍 **良好**：整体状态不错，仍有提升空间。\n");
        } else {
            sb.append("⚠️ **需改善**：多个维度需要关注，建议从最容易改善的维度开始。\n");
        }

        sb.append("\n## 💡 **综合建议**\n\n");
        if (sleepScore < 70) {
            sb.append("- 🛌 **睡眠优先**：睡眠是健康基础，建议优先改善睡眠质量\n");
        }
        if (dietaryScore < 70) {
            sb.append("- 🥗 **饮食次之**：在睡眠改善基础上，逐步调整饮食结构\n");
        }
        if (activityScore < 70) {
            sb.append("- 🏃 **运动辅助**：从每天5000步开始，循序渐进\n");
        }

        return sb.toString();
    }

    /**
     * 从报告中提取评分（简化版，待优化）
     */
    private int extractScoreFromReport(String report, String keyword) {
        if (report == null) return 70;

        // 使用正则表达式提取评分
        // 报告格式示例："- 平均健康评分：75/100"
        String pattern = keyword + "[:：]\\s*(\\d+)";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(report);

        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }

        return 70;  // 默认值
    }

    /**
     * 获取趋势图标
     */
    private String getTrendIcon(int score) {
        if (score >= 80) {
            return "↑";
        } else if (score >= 60) {
            return "→";
        } else {
            return "↓";
        }
    }
}