package org.example.shiyangai.dto;

import lombok.Data;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.IngredientInfo;

import java.util.List;

@Data
public class AIContext {

    private String userId;

    private String userMessage;

    private String constitution;

    private HealthProfile profile;

    private List<DietRecord> todayRecords;

    private List<DietRecord> weekRecords;

    private String solarTermAdvice;

    private IngredientInfo foodInfo;

    private String symptom;

    private String previousContext;
}