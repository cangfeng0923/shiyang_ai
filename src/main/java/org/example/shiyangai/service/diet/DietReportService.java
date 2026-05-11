package org.example.shiyangai.service.diet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietReportService {

    private final DietAnalyzeService
            dietAnalyzeService;

    /**
     * 生成每日报告
     */
    public Map<String, Object> generateDailyReport(
            List<DietRecord> records,
            String constitution
    ) {

        Map<String, Object> report =
                new HashMap<>();

        report.put(
                "date",
                LocalDate.now().toString()
        );

        report.put(
                "totalRecords",
                records.size()
        );

        // ====================
        // 无记录
        // ====================

        if (records.isEmpty()) {

            report.put(
                    "message",
                    "今天还没有饮食记录"
            );

            report.put(
                    "dynamicAdvice",
                    "开始记录您的饮食，AI会为您提供个性化建议"
            );

            return report;
        }

        // ====================
        // 总热量
        // ====================

        double totalCalories =
                records.stream()
                        .mapToDouble(record -> {

                            if (record.getCalories()
                                    != null) {

                                return record
                                        .getCalories();
                            }

                            if (record.getEstimatedGrams()
                                    != null
                                    &&
                                    record.getEstimatedGrams()
                                            > 0) {

                                return record
                                        .getEstimatedGrams()
                                        * 1.2;
                            }

                            return 0;
                        })
                        .sum();

        // ====================
        // 平均健康分
        // ====================

        double avgScore =
                records.stream()
                        .mapToInt(
                                DietRecord::getHealthScore
                        )
                        .average()
                        .orElse(0);

        report.put(
                "totalCalories",
                Math.round(totalCalories)
        );

        report.put(
                "avgHealthScore",
                Math.round(avgScore)
        );

        // ====================
        // 健康等级
        // ====================

        if (avgScore >= 80) {

            report.put(
                    "level",
                    "excellent"
            );

            report.put(
                    "levelText",
                    "优秀"
            );

        } else if (avgScore >= 60) {

            report.put(
                    "level",
                    "good"
            );

            report.put(
                    "levelText",
                    "良好"
            );

        } else {

            report.put(
                    "level",
                    "needImprove"
            );

            report.put(
                    "levelText",
                    "需改进"
            );
        }

        // ====================
        // 饮食结构分析
        // ====================

        DietAnalyzeService.DietAnalysis analysis =
                dietAnalyzeService
                        .analyzeDietStructure(
                                records
                        );

        // ====================
        // 动态建议
        // ====================

        String dynamicAdvice =
                generateDynamicAdvice(
                        analysis,
                        records,
                        constitution
                );

        report.put(
                "dynamicAdvice",
                dynamicAdvice
        );

        report.put(
                "advice",
                dynamicAdvice
        );

        return report;
    }

    /**
     * 动态建议生成
     */
    public String generateDynamicAdvice(
            DietAnalyzeService.DietAnalysis analysis,
            List<DietRecord> records,
            String constitution
    ) {

        List<String> advices =
                new ArrayList<>();

        // ====================
        // 蔬菜建议
        // ====================

        if (analysis.getVegetableCount() == 0) {

            advices.add(
                    "🥬 今天没有吃蔬菜，建议增加绿叶蔬菜"
            );

        } else if (
                analysis.getVegetableGrams()
                        < 200
        ) {

            advices.add(
                    "🥬 蔬菜摄入偏少，建议增加到300g以上"
            );

        } else if (
                analysis.getVegetableGrams()
                        >= 300
        ) {

            advices.add(
                    "✅ 蔬菜摄入充足，继续保持"
            );
        }

        // ====================
        // 水果建议
        // ====================

        if (analysis.getFruitCount() == 0) {

            advices.add(
                    "🍎 今天没有吃水果"
            );

        } else if (
                analysis.getFruitGrams()
                        < 150
        ) {

            advices.add(
                    "🍎 水果摄入偏少"
            );
        }

        // ====================
        // 蛋白质建议
        // ====================

        if (analysis.getProteinCount() == 0) {

            advices.add(
                    "🥩 缺少优质蛋白摄入"
            );

        } else if (
                analysis.getProteinCount() == 1
        ) {

            advices.add(
                    "🥩 蛋白质来源较单一"
            );
        }

        // ====================
        // 主食建议
        // ====================

        if (analysis.getGrainCount() == 0) {

            advices.add(
                    "🍚 今天没有记录主食"
            );
        }

        // ====================
        // 早餐建议
        // ====================

        if (!analysis.isHasBreakfast()) {

            advices.add(
                    "🌅 今天没有记录早餐"
            );
        }

        // ====================
        // 不健康食物
        // ====================

        if (analysis.getUnhealthyCount() > 0) {

            String unhealthyFoods =
                    String.join(
                            "、",
                            analysis.getUnhealthyFoods()
                    );

            advices.add(
                    "⚠️ 今天摄入了："
                            + unhealthyFoods
            );
        }

        // ====================
        // 食物多样性
        // ====================

        if (analysis.getFoodTypeCount() < 5) {

            advices.add(
                    "🍽️ 食物种类偏少"
            );

        } else if (
                analysis.getFoodTypeCount()
                        >= 8
        ) {

            advices.add(
                    "🎉 食物多样性很好"
            );
        }

        // ====================
        // 体质建议
        // ====================

        if (constitution != null
                && !constitution.isBlank()) {

            String constitutionAdvice =
                    getConstitutionAdvice(
                            constitution,
                            analysis
                    );

            if (!constitutionAdvice.isEmpty()) {

                advices.add(
                        constitutionAdvice
                );
            }
        }

        // ====================
        // 默认建议
        // ====================

        if (advices.isEmpty()) {

            return "👍 今天饮食结构很好，继续保持";
        }

        return String.join(
                "<br>",
                advices
        );
    }

    /**
     * 体质建议
     */
    private String getConstitutionAdvice(
            String constitution,
            DietAnalyzeService.DietAnalysis analysis
    ) {

        switch (constitution) {

            case "气虚质":

                if (!analysis.getAllFoods()
                        .contains("山药")
                        &&
                        !analysis.getAllFoods()
                                .contains("大枣")) {

                    return "🌿 您是气虚体质，建议多吃山药、大枣补气";
                }

                break;

            case "阳虚质":

                if (!analysis.getAllFoods()
                        .contains("生姜")
                        &&
                        !analysis.getAllFoods()
                                .contains("羊肉")) {

                    return "🔥 您是阳虚体质，建议适当温补";
                }

                break;

            case "阴虚质":

                if (!analysis.getAllFoods()
                        .contains("银耳")
                        &&
                        !analysis.getAllFoods()
                                .contains("百合")) {

                    return "💧 您是阴虚体质，建议滋阴润燥";
                }

                break;

            case "痰湿质":

                if (analysis.getUnhealthyCount() > 0) {

                    return "💦 您是痰湿体质，应减少甜食油腻";
                }

                break;

            case "湿热质":

                if (analysis.getUnhealthyCount() > 0) {

                    return "🌊 您是湿热体质，应减少辛辣烧烤";
                }

                break;
        }

        return "";
    }
}