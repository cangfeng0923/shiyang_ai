package org.example.shiyangai.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodAnalysisResponse {
    private String userId;
    private String constitution;
    private String foodName;
    private Map<String, Object> nutrition;
    private String suitability;
    private String reason;
    private String suggestion;
    private String disclaimer;
    private Boolean needAssessment;
    private String message;
}