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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final HealthProfileMapper healthProfileMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HealthProfile getProfile(String userId) {
        return healthProfileMapper.selectByUserId(userId);
    }

    @Transactional
    public void saveProfile(HealthProfile profile) {
        // 计算年龄
        if (profile.getBirthDate() != null) {
            profile.setAge(Period.between(profile.getBirthDate(), LocalDate.now()).getYears());
        }

        // 计算BMI
        if (profile.getHeight() != null && profile.getWeight() != null) {
            double heightM = profile.getHeight() / 100;
            profile.setBmi(Math.round(profile.getWeight() / (heightM * heightM) * 10) / 10.0);
        }

        // 判断档案完整性
        profile.setIsComplete(isProfileComplete(profile));
        profile.setUpdateTime(LocalDateTime.now());

        HealthProfile existing = getProfile(profile.getUserId());
        if (existing != null) {
            healthProfileMapper.updateById(profile);
        } else {
            healthProfileMapper.insert(profile);
        }
        log.info("保存健康档案: userId={}, isComplete={}", profile.getUserId(), profile.getIsComplete());
    }

    private boolean isProfileComplete(HealthProfile profile) {
        // 基础信息必须完整
        if (profile.getGender() == null) return false;
        if (profile.getBirthDate() == null) return false;
        if (profile.getHeight() == null || profile.getWeight() == null) return false;
        return true;
    }

    public void updateField(String userId, String field, Object value) {
        healthProfileMapper.updateField(userId, field, value != null ? value.toString() : null);
    }

    public String checkAllergy(String userId, String foodName) {
        HealthProfile profile = getProfile(userId);
        if (profile == null || profile.getAllergies() == null) return null;

        try {
            List<String> allergies = objectMapper.readValue(profile.getAllergies(),
                    new TypeReference<List<String>>() {});
            for (String allergy : allergies) {
                if (foodName.contains(allergy) || allergy.contains(foodName)) return allergy;
            }
        } catch (JsonProcessingException e) {
            log.error("解析过敏数据失败", e);
        }
        return null;
    }

    public List<String> parseJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) return Arrays.asList();
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Arrays.asList();
        }
    }
}