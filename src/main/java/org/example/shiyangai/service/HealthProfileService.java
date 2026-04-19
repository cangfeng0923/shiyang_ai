package org.example.shiyangai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.mapper.HealthProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final HealthProfileMapper healthProfileMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取用户健康档案
     */
    public HealthProfile getProfile(String userId) {
        return healthProfileMapper.selectByUserId(userId);
    }

    /**
     * 创建或更新健康档案
     */
    @Transactional
    public void saveProfile(HealthProfile profile) {
        HealthProfile existing = getProfile(profile.getUserId());

        if (existing != null) {
            profile.setUpdateTime(LocalDateTime.now());
            healthProfileMapper.updateById(profile);
            log.info("更新健康档案: userId={}", profile.getUserId());
        } else {
            profile.setUpdateTime(LocalDateTime.now());
            healthProfileMapper.insert(profile);
            log.info("创建健康档案: userId={}", profile.getUserId());
        }
    }

    /**
     * 更新单个字段
     */
    @Transactional
    public void updateField(String userId, String field, Object value) {
        String strValue = value != null ? value.toString() : "";
        healthProfileMapper.updateField(userId, field, strValue);

        // 如果是体重/身高，重新计算BMI
        if ("weight".equals(field) || "height".equals(field)) {
            updateBMI(userId);
        }
    }

    /**
     * 更新BMI
     */
    private void updateBMI(String userId) {
        HealthProfile profile = getProfile(userId);
        if (profile != null && profile.getHeight() != null && profile.getWeight() != null) {
            double heightM = profile.getHeight() / 100;
            double bmi = profile.getWeight() / (heightM * heightM);
            profile.setBmi(Math.round(bmi * 10) / 10.0);
            healthProfileMapper.updateById(profile);
        }
    }

    /**
     * 检查食物过敏
     */
    public String checkAllergy(String userId, String foodName) {
        HealthProfile profile = getProfile(userId);
        if (profile == null || profile.getAllergies() == null) {
            return null;
        }

        try {
            List<String> allergies = objectMapper.readValue(profile.getAllergies(),
                    new TypeReference<List<String>>() {});
            for (String allergy : allergies) {
                if (foodName.contains(allergy) || allergy.contains(foodName)) {
                    return allergy;
                }
            }
        } catch (JsonProcessingException e) {
            log.error("解析过敏数据失败", e);
        }
        return null;
    }

    /**
     * 检查药食冲突
     */
    public String checkDrugFoodConflict(String medicine, String food) {
        // 药食冲突数据库
        if (medicine.contains("阿司匹林") && (food.contains("酒") || food.contains("银杏"))) {
            return "阿司匹林与" + food + "同服可能增加出血风险";
        }
        if (medicine.contains("华法林") && (food.contains("菠菜") || food.contains("西兰花"))) {
            return "华法林与富含维生素K的" + food + "同服可能影响药效";
        }
        if (medicine.contains("降压药") && food.contains("西柚")) {
            return "降压药与西柚同服可能导致血压过低";
        }
        if (medicine.contains("降糖药") && food.contains("酒")) {
            return "降糖药与酒精同服可能导致低血糖";
        }
        return null;
    }

    /**
     * 解析JSON数组字段
     */
    public List<String> parseJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return Arrays.asList();
        }
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Arrays.asList();
        }
    }
}
