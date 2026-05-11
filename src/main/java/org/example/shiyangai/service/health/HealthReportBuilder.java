package org.example.shiyangai.service.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.service.DynamicSolarTermService;
import org.example.shiyangai.service.HealthProfileService;
import org.example.shiyangai.service.diet.DietRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 健康报告构建器 - 主入口
 * 职责：编排各章节构建器，生成完整报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportBuilder {

    private final DietRecordService dietRecordService;
    private final HealthProfileService healthProfileService;
    private final DynamicSolarTermService solarTermService;

    // 注入各章节构建器
    private final BasicInfoSectionBuilder basicInfoSectionBuilder;
    private final DietSummarySectionBuilder dietSummarySectionBuilder;
    private final ConstitutionSectionBuilder constitutionSectionBuilder;
    private final SolarTermSectionBuilder solarTermSectionBuilder;
    private final ImprovementSuggestionsBuilder improvementSuggestionsBuilder;
    private final HealthRemindersBuilder healthRemindersBuilder;

    /**
     * 生成综合健康报告
     */
    public String generateComprehensiveReport(String userId, String constitution) {
        log.info("生成综合健康报告: userId={}, constitution={}", userId, constitution);

        HealthProfile profile = healthProfileService.getProfile(userId);
        List<DietRecord> weekRecords = dietRecordService.getWeekRecords(userId);
        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);

        StringBuilder sb = new StringBuilder();

        // 标题
        sb.append("📊 **您的专属健康报告**\n\n");
        sb.append("📅 报告时间：").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))).append("\n\n");

        // 1. 基础信息章节
        sb.append(basicInfoSectionBuilder.build(profile, constitution));

        // 2. 本周饮食总结章节
        sb.append(dietSummarySectionBuilder.buildWeekSummary(weekRecords));

        // 3. 今日饮食章节
        sb.append(dietSummarySectionBuilder.buildTodaySummary(todayRecords));

        // 4. 体质饮食建议章节
        sb.append(constitutionSectionBuilder.build(constitution));

        // 5. 节气养生提醒章节
        sb.append(solarTermSectionBuilder.build(constitution, solarTermService));

        // 6. 改进建议章节
        sb.append(improvementSuggestionsBuilder.build(weekRecords, constitution));

        // 7. 健康提醒章节
        sb.append(healthRemindersBuilder.build(profile));

        // 页脚
        sb.append("\n---\n");
        sb.append("*本报告基于您的饮食记录和健康档案生成，仅供参考。如有身体不适，请及时就医。*");

        return sb.toString();
    }
}