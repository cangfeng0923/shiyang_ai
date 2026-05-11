package org.example.shiyangai.service.health;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.DynamicSolarTermService;
import org.springframework.stereotype.Component;

/**
 * 节气养生章节构建器
 * 职责：构建报告中的节气养生提醒部分
 */
@Slf4j
@Component
public class SolarTermSectionBuilder {

    public String build(String constitution, DynamicSolarTermService solarTermService) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---\n");
        sb.append("### 5️⃣ 节气养生提醒\n\n");

        String currentTerm = solarTermService.getCurrentSolarTermName();
        String nextTerm = solarTermService.getNextSolarTermName();
        sb.append("📅 当前节气：**").append(currentTerm).append("**\n");
        sb.append("⏭️ 下一个节气：").append(nextTerm).append("\n\n");

        String termAdvice = solarTermService.getDynamicSolarTermAdvice(constitution);
        // 提取节气特点部分
        String extractedAdvice = extractSolarTermCharacteristic(termAdvice);
        if (extractedAdvice != null) {
            sb.append(extractedAdvice).append("\n");
        }

        return sb.toString();
    }

    private String extractSolarTermCharacteristic(String termAdvice) {
        if (termAdvice == null) {
            return null;
        }
        if (termAdvice.contains("【节气特点】")) {
            int start = termAdvice.indexOf("【节气特点】");
            int end = termAdvice.indexOf("【养生原则】");
            if (end > start && end <= termAdvice.length()) {
                return termAdvice.substring(start, end);
            }
        }
        return null;
    }
}