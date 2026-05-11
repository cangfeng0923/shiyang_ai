package org.example.shiyangai.service.health;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.HealthProfile;
import org.springframework.stereotype.Component;

/**
 * 基础信息章节构建器
 * 职责：构建报告中的用户基础信息部分
 */
@Slf4j
@Component
public class BasicInfoSectionBuilder {

    public String build(HealthProfile profile, String constitution) {
        StringBuilder sb = new StringBuilder();

        sb.append("---\n");
        sb.append("### 1️⃣ 基础信息\n\n");

        if (profile != null) {
            sb.append("- 中医体质：**").append(constitution).append("**\n");
            if (profile.getAge() != null) {
                sb.append("- 年龄：").append(profile.getAge()).append("岁\n");
            }
            if (profile.getGender() != null) {
                sb.append("- 性别：").append(profile.getGender().equals("MALE") ? "男" : "女").append("\n");
            }
            if (profile.getHeight() != null && profile.getWeight() != null) {
                double bmi = calculateBMI(profile.getHeight(), profile.getWeight());
                String bmiAdvice = getBMIAdvice(bmi);
                sb.append(String.format("- BMI：%.1f（%s）\n", bmi, bmiAdvice));
            }
        } else {
            sb.append("> ⚠️ 请先在「健康档案」中填写您的个人信息\n\n");
        }

        return sb.toString();
    }

    private double calculateBMI(Double height, Double weight) {
        return weight / Math.pow(height / 100, 2);
    }

    private String getBMIAdvice(double bmi) {
        if (bmi < 18.5) {
            return "偏瘦，建议增加营养摄入";
        } else if (bmi < 24) {
            return "正常范围，继续保持";
        } else if (bmi < 28) {
            return "超重，建议控制饮食、增加运动";
        } else {
            return "肥胖，建议咨询专业营养师";
        }
    }
}