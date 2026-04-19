package org.example.shiyangai.agent.tools;

import org.example.shiyangai.agent.Tool;
import org.example.shiyangai.agent.ToolExecutor;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.service.HealthProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

// 方案1：使用 fastjson2（推荐）
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;

@Component
public class HealthProfileTool implements ToolExecutor {

    @Autowired
    private HealthProfileService profileService;

    @Override
    @Tool(name = "health_profile",
            description = "管理用户健康档案，包括年龄、过敏史、病史、忌口等",
            parameters = {"userId", "action", "field", "value"})
    public String execute(Map<String, Object> parameters) {
        String userId = (String) parameters.get("userId");
        String action = (String) parameters.getOrDefault("action", "get");

        switch (action) {
            case "get":
                return getProfile(userId);
            case "update":
                String field = (String) parameters.get("field");
                Object value = parameters.get("value");
                return updateProfile(userId, field, value);
            case "check_allergy":
                String food = (String) parameters.get("food");
                return checkAllergy(userId, food);
            case "check_conflict":
                String medicine = (String) parameters.get("medicine");
                String foodItem = (String) parameters.get("food");
                return checkDrugFoodConflict(medicine, foodItem);
            default:
                return "支持的操作：get(获取档案)、update(更新)、check_allergy(检查过敏)、check_conflict(检查药食冲突)";
        }
    }

    private String getProfile(String userId) {
        HealthProfile profile = profileService.getProfile(userId);
        if (profile == null) {
            return "尚未建立健康档案，请先完善个人信息";
        }
        return buildProfileString(profile);
    }

    private String updateProfile(String userId, String field, Object value) {
        profileService.updateField(userId, field, value);
        return String.format("已更新 %s 为: %s", field, value);
    }

    private String checkAllergy(String userId, String food) {
        HealthProfile profile = profileService.getProfile(userId);
        if (profile == null) {
            return "请先完善健康档案中的过敏史信息";
        }

        String allergiesJson = profile.getAllergies();
        if (allergiesJson == null || allergiesJson.trim().isEmpty() || "[]".equals(allergiesJson)) {
            return "您尚未填写过敏史，无法判断。建议先完善健康档案中的过敏信息。";
        }

        try {
            // 使用 fastjson2 解析
            JSONArray allergies = JSON.parseArray(allergiesJson);
            for (int i = 0; i < allergies.size(); i++) {
                String allergy = allergies.getString(i);
                if (food.contains(allergy) || allergy.contains(food)) {
                    return String.format("⚠️ 警告：%s 含有您过敏的食材「%s」，请勿食用！", food, allergy);
                }
            }
        } catch (Exception e) {
            return "解析过敏信息失败，请检查数据格式";
        }

        return String.format("✓ %s 不包含您已知的过敏原，可以适量食用", food);
    }

    private String checkDrugFoodConflict(String medicine, String food) {
        // 常见药食冲突数据库
        Map<String, String[]> conflicts = Map.of(
                "阿司匹林", new String[]{"酒", "银杏", "大蒜"},
                "华法林", new String[]{"菠菜", "西兰花", "动物肝脏"},
                "降压药", new String[]{"西柚", "香蕉", "甘草"},
                "降糖药", new String[]{"酒", "苦瓜", "肉桂"},
                "抗生素", new String[]{"牛奶", "酒", "咖啡"}
        );

        for (Map.Entry<String, String[]> entry : conflicts.entrySet()) {
            if (medicine.contains(entry.getKey())) {
                for (String conflict : entry.getValue()) {
                    if (food.contains(conflict)) {
                        return String.format("⚠️ 药食冲突提醒：服用「%s」期间不宜食用「%s」", medicine, food);
                    }
                }
            }
        }
        return "✓ 未发现明显药食冲突，建议咨询医生确认";
    }

    private String buildProfileString(HealthProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 **健康档案**\n\n");

        // 基础信息
        sb.append("基础信息：\n");
        sb.append("- 年龄：").append(profile.getAge() != null ? profile.getAge() : "未填写").append("岁\n");
        sb.append("- 性别：").append(formatGender(profile.getGender())).append("\n");
        sb.append("- 身高：").append(profile.getHeight() != null ? profile.getHeight() + "cm" : "未填写").append("\n");
        sb.append("- 体重：").append(profile.getWeight() != null ? profile.getWeight() + "kg" : "未填写").append("\n");
        sb.append("- BMI：").append(calculateBMI(profile)).append("\n");
        sb.append("- 血型：").append(profile.getBloodType() != null ? profile.getBloodType() : "未填写").append("\n\n");

        // 健康信息
        sb.append("🏥 健康信息：\n");

        // 过敏史 - JSON数组解析
        String allergiesJson = profile.getAllergies();
        if (allergiesJson != null && !allergiesJson.trim().isEmpty() && !"[]".equals(allergiesJson)) {
            try {
                JSONArray allergies = JSON.parseArray(allergiesJson);
                if (allergies != null && allergies.size() > 0) {
                    sb.append("⚠️ 过敏史：");
                    for (int i = 0; i < allergies.size(); i++) {
                        sb.append(allergies.getString(i));
                        if (i < allergies.size() - 1) sb.append("、");
                    }
                    sb.append("\n");
                }
            } catch (Exception e) {
                sb.append("⚠️ 过敏史：").append(allergiesJson).append("\n");
            }
        }

        // 忌口 - JSON数组解析
        String avoidJson = profile.getFoodAvoidance();
        if (avoidJson != null && !avoidJson.trim().isEmpty() && !"[]".equals(avoidJson)) {
            try {
                JSONArray avoids = JSON.parseArray(avoidJson);
                if (avoids != null && avoids.size() > 0) {
                    sb.append("🚫 忌口：");
                    for (int i = 0; i < avoids.size(); i++) {
                        sb.append(avoids.getString(i));
                        if (i < avoids.size() - 1) sb.append("、");
                    }
                    sb.append("\n");
                }
            } catch (Exception e) {
                sb.append("🚫 忌口：").append(avoidJson).append("\n");
            }
        }

        // 饮食偏好 - JSON数组解析
        String preferenceJson = profile.getFoodPreference();
        if (preferenceJson != null && !preferenceJson.trim().isEmpty() && !"[]".equals(preferenceJson)) {
            try {
                JSONArray preferences = JSON.parseArray(preferenceJson);
                if (preferences != null && preferences.size() > 0) {
                    sb.append("👍 饮食偏好：");
                    for (int i = 0; i < preferences.size(); i++) {
                        sb.append(preferences.getString(i));
                        if (i < preferences.size() - 1) sb.append("、");
                    }
                    sb.append("\n");
                }
            } catch (Exception e) {
                sb.append("👍 饮食偏好：").append(preferenceJson).append("\n");
            }
        }

        // 病史 - JSON数组解析
        String diseasesJson = profile.getPastDiseases();
        if (diseasesJson != null && !diseasesJson.trim().isEmpty() && !"[]".equals(diseasesJson)) {
            try {
                JSONArray diseases = JSON.parseArray(diseasesJson);
                if (diseases != null && diseases.size() > 0) {
                    sb.append("📝 病史：");
                    for (int i = 0; i < diseases.size(); i++) {
                        sb.append(diseases.getString(i));
                        if (i < diseases.size() - 1) sb.append("、");
                    }
                    sb.append("\n");
                }
            } catch (Exception e) {
                sb.append("📝 病史：").append(diseasesJson).append("\n");
            }
        }

        // 用药记录
        String medicationsJson = profile.getMedications();
        if (medicationsJson != null && !medicationsJson.trim().isEmpty() && !"[]".equals(medicationsJson)) {
            try {
                JSONArray medications = JSON.parseArray(medicationsJson);
                if (medications != null && medications.size() > 0) {
                    sb.append("💊 用药记录：");
                    for (int i = 0; i < medications.size(); i++) {
                        sb.append(medications.getString(i));
                        if (i < medications.size() - 1) sb.append("、");
                    }
                    sb.append("\n");
                }
            } catch (Exception e) {
                sb.append("💊 用药记录：").append(medicationsJson).append("\n");
            }
        }

        sb.append("\n");

        // 生活习惯
        sb.append("🏃 生活习惯：\n");
        sb.append("- 作息：").append(formatSleepPattern(profile.getSleepPattern())).append("\n");
        sb.append("- 运动：").append(formatExerciseHabit(profile.getExerciseHabit())).append("\n");
        sb.append("- 吸烟：").append(formatYesNo(profile.getSmoking())).append("\n");
        sb.append("- 饮酒：").append(formatYesNo(profile.getDrinking())).append("\n");

        // 中医体质
        if (profile.getConstitution() != null && !profile.getConstitution().isEmpty()) {
            sb.append("\n🌿 中医体质：").append(profile.getConstitution());
        }

        return sb.toString();
    }

    private String calculateBMI(HealthProfile profile) {
        if (profile.getHeight() == null || profile.getWeight() == null) {
            return "未填写身高体重";
        }
        double heightM = profile.getHeight() / 100;
        double bmi = profile.getWeight() / (heightM * heightM);
        return String.format("%.1f (%s)", bmi, getBMICategory(bmi));
    }

    private String getBMICategory(double bmi) {
        if (bmi < 18.5) return "偏瘦";
        if (bmi < 24) return "正常";
        if (bmi < 28) return "超重";
        return "肥胖";
    }

    private String formatGender(String gender) {
        if (gender == null) return "未填写";
        switch (gender) {
            case "MALE": return "男";
            case "FEMALE": return "女";
            default: return gender;
        }
    }

    private String formatSleepPattern(String pattern) {
        if (pattern == null) return "未填写";
        switch (pattern) {
            case "EARLY": return "早睡早起型";
            case "LATE": return "夜猫子型";
            case "IRREGULAR": return "不规律";
            default: return pattern;
        }
    }

    private String formatExerciseHabit(String habit) {
        if (habit == null) return "未填写";
        switch (habit) {
            case "OFTEN": return "经常运动";
            case "SOMETIMES": return "偶尔运动";
            case "RARELY": return "很少运动";
            default: return habit;
        }
    }

    private String formatYesNo(String value) {
        if (value == null) return "未填写";
        switch (value) {
            case "YES": return "是";
            case "NO": return "否";
            default: return value;
        }
    }

    @Override
    public String getName() { return "health_profile"; }

    @Override
    public String getDescription() {
        return "健康档案管理工具，可查询、更新健康档案，检查过敏和药食冲突";
    }
}