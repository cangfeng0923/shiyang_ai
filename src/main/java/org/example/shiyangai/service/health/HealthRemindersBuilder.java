package org.example.shiyangai.service.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.service.HealthProfileService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 健康提醒生成器
 * 职责：生成报告中的健康提醒部分（过敏、病史等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthRemindersBuilder {

    private final HealthProfileService healthProfileService;

    public String build(HealthProfile profile) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---\n");
        sb.append("### 7️⃣ 健康提醒\n\n");

        if (profile != null) {
            // 过敏提醒
            List<String> allergies = healthProfileService.parseJsonArray(profile.getAllergies());
            if (!allergies.isEmpty()) {
                sb.append("⚠️ **过敏提醒**：您对").append(String.join("、", allergies)).append("过敏，请避免食用\n\n");
            }

            // 病史提醒
            List<String> diseases = healthProfileService.parseJsonArray(profile.getPastDiseases());
            if (!diseases.isEmpty()) {
                sb.append("📝 **病史提醒**：您有").append(String.join("、", diseases)).append("，请注意定期复查\n\n");
            }
        }

        sb.append("🏥 如出现持续不适症状，请及时就医，不要仅依赖食疗");

        return sb.toString();
    }
}