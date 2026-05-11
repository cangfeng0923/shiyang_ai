package org.example.shiyangai.service.diet;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.service.FoodCategoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DietAnalyzeService {

    private final FoodCategoryService
            foodCategoryService;

    public DietAnalyzeService(
            FoodCategoryService foodCategoryService
    ) {

        this.foodCategoryService =
                foodCategoryService;
    }

    /**
     * 饮食结构分析
     */
    public DietAnalysis analyzeDietStructure(
            List<DietRecord> records
    ) {

        DietAnalysis analysis =
                new DietAnalysis();

        for (DietRecord record : records) {

            if (record == null
                    || record.getFoodName() == null) {

                continue;
            }

            String foodName =
                    record.getFoodName();

            Double grams =
                    record.getGrams() != null
                            ? record.getGrams()
                            : 0;

            // ====================
            // 蔬菜
            // ====================

            if (foodCategoryService
                    .isVegetable(foodName)) {

                analysis.vegetableCount++;

                analysis.vegetableGrams +=
                        grams.intValue();
            }

            // ====================
            // 水果
            // ====================

            else if (foodCategoryService
                    .isFruit(foodName)) {

                analysis.fruitCount++;

                analysis.fruitGrams +=
                        grams.intValue();
            }

            // ====================
            // 蛋白质
            // ====================

            else if (foodCategoryService
                    .isProtein(foodName)) {

                analysis.proteinCount++;

                analysis.proteinGrams +=
                        grams.intValue();
            }

            // ====================
            // 主食
            // ====================

            else if (foodCategoryService
                    .isGrain(foodName)) {

                analysis.grainCount++;
            }

            // ====================
            // 不健康食物
            // ====================

            if (foodCategoryService
                    .isUnhealthy(foodName)) {

                analysis.unhealthyCount++;

                analysis.unhealthyFoods
                        .add(foodName);
            }

            // ====================
            // 早餐
            // ====================

            if ("BREAKFAST".equals(
                    record.getMealType()
            )) {

                analysis.hasBreakfast =
                        true;
            }

            // ====================
            // 晚餐时间
            // ====================

            if ("DINNER".equals(
                    record.getMealType()
            )) {

                analysis.dinnerTime =
                        record.getRecordDate();
            }

            // ====================
            // 食物集合
            // ====================

            analysis.allFoods
                    .add(foodName);
        }

        analysis.foodTypeCount =
                analysis.allFoods.size();

        return analysis;
    }

    /**
     * 饮食结构分析结果
     */
    @Getter
    @Setter
    public static class DietAnalysis {

        /**
         * 蔬菜数量
         */
        private int vegetableCount = 0;

        /**
         * 蔬菜克重
         */
        private int vegetableGrams = 0;

        /**
         * 水果数量
         */
        private int fruitCount = 0;

        /**
         * 水果克重
         */
        private int fruitGrams = 0;

        /**
         * 蛋白质数量
         */
        private int proteinCount = 0;

        /**
         * 蛋白质克重
         */
        private int proteinGrams = 0;

        /**
         * 主食数量
         */
        private int grainCount = 0;

        /**
         * 不健康食物数量
         */
        private int unhealthyCount = 0;

        /**
         * 食物种类数量
         */
        private int foodTypeCount = 0;

        /**
         * 是否吃早餐
         */
        private boolean hasBreakfast = false;

        /**
         * 晚餐时间
         */
        private LocalDateTime dinnerTime;

        /**
         * 不健康食物列表
         */
        private Set<String> unhealthyFoods =
                new HashSet<>();

        /**
         * 所有食物
         */
        private Set<String> allFoods =
                new HashSet<>();
    }
}