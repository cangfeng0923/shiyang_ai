package org.example.shiyangai.dto;

import lombok.Data;

@Data
public class AssessmentRequest {
    private String userId;
    private boolean[] answers;  // 10道题的答案
}