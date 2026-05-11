package org.example.shiyangai.service.diet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.crawler.FoodDataCrawler;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.FoodCategoryService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietScoreService {

    private final FoodDataCrawler foodDataCrawler;

    private final FoodCategoryService foodCategoryService;

    public int calculateHealthScore(
            DietRecord record
    ) {

        String foodName =
                record.getFoodName();

        int score = 70;

        try {

            IngredientInfo nutrition =
                    foodDataCrawler.getFoodInfo(
                            foodName
                    );

            if (nutrition == null) {
                return fallbackScore(
                        foodName,
                        score
                );
            }

            score += calculateCaloriesScore(
                    nutrition
            );

            score += calculateProteinScore(
                    nutrition
            );

            score += calculateFatScore(
                    nutrition
            );

            score += calculateFoodTypeScore(
                    foodName
            );

        } catch (Exception e) {

            log.warn(
                    "获取食物营养失败: {}",
                    foodName,
                    e
            );

            score = fallbackScore(
                    foodName,
                    score
            );
        }

        return Math.max(
                0,
                Math.min(100, score)
        );
    }

    private int calculateCaloriesScore(
            IngredientInfo nutrition
    ) {

        Integer calories =
                nutrition.getCalories();

        if (calories == null) {
            return 0;
        }

        if (calories > 450) return -15;

        if (calories > 350) return -8;

        if (calories > 250) return -3;

        if (calories < 80) return 10;

        if (calories < 150) return 5;

        return 0;
    }

    private int calculateProteinScore(
            IngredientInfo nutrition
    ) {

        Double protein =
                nutrition.getProtein();

        if (protein == null) {
            return 0;
        }

        if (protein > 15) return 12;

        if (protein > 10) return 8;

        if (protein > 6) return 4;

        return 0;
    }

    private int calculateFatScore(
            IngredientInfo nutrition
    ) {

        Double fat =
                nutrition.getFat();

        if (fat == null) {
            return 0;
        }

        if (fat > 25) return -15;

        if (fat > 15) return -8;

        if (fat > 8) return -3;

        if (fat < 3) return 5;

        return 0;
    }

    private int calculateFoodTypeScore(
            String foodName
    ) {

        int score = 0;

        if (foodCategoryService.isVegetable(foodName)) {
            score += 10;
        }

        if (foodCategoryService.isFruit(foodName)) {
            score += 5;
        }

        if (foodCategoryService.isProtein(foodName)) {
            score += 5;
        }

        if (foodCategoryService.isUnhealthy(foodName)) {
            score -= 20;
        }

        return score;
    }

    private int fallbackScore(
            String foodName,
            int baseScore
    ) {

        int score = baseScore;

        if (foodCategoryService.isVegetable(foodName)
                || foodCategoryService.isFruit(foodName)) {

            score += 15;
        }

        if (foodCategoryService.isProtein(foodName)) {
            score += 10;
        }

        if (foodCategoryService.isUnhealthy(foodName)) {
            score -= 20;
        }

        return score;
    }
}