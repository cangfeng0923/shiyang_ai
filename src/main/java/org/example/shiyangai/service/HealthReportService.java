package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.health.HealthReportBuilder;
import org.springframework.stereotype.Service;

/**
 * 健康报告服务（兼容层）
 * 委托给 HealthReportBuilder 处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportService {

    private final HealthReportBuilder healthReportBuilder;

    /**
     * 生成综合健康报告
     */
    public String generateComprehensiveReport(String userId, String constitution) {
        return healthReportBuilder.generateComprehensiveReport(userId, constitution);
    }
}