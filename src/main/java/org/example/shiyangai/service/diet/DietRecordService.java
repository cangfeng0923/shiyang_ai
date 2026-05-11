//package org.example.shiyangai.service;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import lombok.extern.slf4j.Slf4j;
//import org.example.shiyangai.crawler.FoodDataCrawler;
//import org.example.shiyangai.entity.DietRecord;
//import org.example.shiyangai.entity.IngredientInfo;
//import org.example.shiyangai.mapper.DietRecordMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//public class DietRecordService {
//
//    private final DietRecordMapper dietRecordMapper;
//    private final FoodDataCrawler foodDataCrawler;
//    private AIService aiService;
//    private FoodCategoryService foodCategoryService;
//
//    @Autowired
//    public DietRecordService(DietRecordMapper dietRecordMapper, FoodDataCrawler foodDataCrawler) {
//        this.dietRecordMapper = dietRecordMapper;
//        this.foodDataCrawler = foodDataCrawler;
//    }
//
//    @Autowired
//    public void setFoodCategoryService(FoodCategoryService foodCategoryService) {
//        this.foodCategoryService = foodCategoryService;
//    }
//
//    @Autowired
//    @Lazy
//    public void setAiService(AIService aiService) {
//        this.aiService = aiService;
//    }
//
//    // ==================== 营养成分分类关键词 ====================
//    private static final Set<String> VEGETABLE_KEYWORDS = Set.of(
//            "蔬菜", "青菜", "菠菜", "西兰花", "白菜", "生菜", "油麦菜", "空心菜",
//            "西红柿", "番茄", "黄瓜", "冬瓜", "南瓜", "苦瓜", "丝瓜", "萝卜", "胡萝卜",
//            "茄子", "青椒", "彩椒", "蘑菇", "香菇", "金针菇", "木耳"
//    );
//
//    private static final Set<String> FRUIT_KEYWORDS = Set.of(
//            "苹果", "梨", "香蕉", "橙子", "橘子", "柚子", "猕猴桃", "草莓",
//            "葡萄", "西瓜", "哈密瓜", "桃", "李子", "杏", "樱桃", "芒果", "火龙果"
//    );
//
//    private static final Set<String> PROTEIN_KEYWORDS = Set.of(
//            "肉", "猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉", "鱼肉", "虾", "蟹",
//            "鸡蛋", "鸭蛋", "鹌鹑蛋", "豆腐", "豆浆", "腐竹", "牛奶", "酸奶", "奶酪"
//    );
//
//    private static final Set<String> GRAIN_KEYWORDS = Set.of(
//            "米饭", "馒头", "面条", "面包", "粥", "玉米", "红薯", "紫薯",
//            "燕麦", "糙米", "小米", "黑米", "荞麦", "全麦"
//    );
//
//    private static final Set<String> UNHEALTHY_KEYWORDS = Set.of(
//            "炸", "烤", "烧烤", "串", "薯条", "薯片", "蛋糕", "饼干",
//            "奶茶", "可乐", "雪碧", "糖果", "巧克力", "冰淇淋", "汉堡", "披萨"
//    );
//
//    // ==================== 单位换算 ====================
//    private Double convertToGrams(Double amount, String unit) {
//        if (amount == null || unit == null) return null;
//        Map<String, Double> unitMap = Map.of(
//                "g", 1.0, "kg", 1000.0, "ml", 1.0,
//                "杯", 200.0, "碗", 250.0, "勺", 15.0,
//                "个", 50.0, "片", 10.0, "块", 30.0, "盘", 200.0
//        );
//        Double ratio = unitMap.getOrDefault(unit, 1.0);
//        return amount * ratio;
//    }
//
//    // ==================== 分类判断方法（委托给 FoodCategoryService 或关键词） ====================
//    private boolean isVegetable(String foodName) {
//        if (foodCategoryService != null) {
//            return foodCategoryService.isVegetable(foodName);
//        }
//        return VEGETABLE_KEYWORDS.stream().anyMatch(foodName::contains);
//    }
//
//    private boolean isFruit(String foodName) {
//        if (foodCategoryService != null) {
//            return foodCategoryService.isFruit(foodName);
//        }
//        return FRUIT_KEYWORDS.stream().anyMatch(foodName::contains);
//    }
//
//    private boolean isProtein(String foodName) {
//        if (foodCategoryService != null) {
//            return foodCategoryService.isProtein(foodName);
//        }
//        return PROTEIN_KEYWORDS.stream().anyMatch(foodName::contains);
//    }
//
//    private boolean isGrain(String foodName) {
//        if (foodCategoryService != null) {
//            return foodCategoryService.isGrain(foodName);
//        }
//        return GRAIN_KEYWORDS.stream().anyMatch(foodName::contains);
//    }
//
//    private boolean isUnhealthy(String foodName) {
//        if (foodCategoryService != null) {
//            return foodCategoryService.isUnhealthy(foodName);
//        }
//        return UNHEALTHY_KEYWORDS.stream().anyMatch(foodName::contains);
//    }
//
//    // ==================== 营养分析评分方法 ====================
//    public int calculateHealthScore(DietRecord record) {
//        String foodName = record.getFoodName();
//        int score = 70;
//
//        try {
//            IngredientInfo nutrition = foodDataCrawler.getFoodInfo(foodName);
//
//            if (nutrition == null) {
//                return fallbackScore(foodName, score);
//            }
//
//            score += calculateCaloriesScore(nutrition);
//            score += calculateProteinScore(nutrition);
//            score += calculateFatScore(nutrition);
//            score += calculateFoodTypeScore(foodName);
//
//            // 烹饪方式调整
//            String lowerName = foodName.toLowerCase();
//            if (lowerName.contains("蒸") || lowerName.contains("煮") || lowerName.contains("炖") || lowerName.contains("焯")) {
//                score += 8;
//            }
//            if (lowerName.contains("炒") && !lowerName.contains("爆炒")) {
//                score += 2;
//            }
//            if (lowerName.contains("炸") || lowerName.contains("煎") || lowerName.contains("烤") || lowerName.contains("熏")) {
//                score -= 15;
//            }
//            if (lowerName.contains("腌") || lowerName.contains("腊")) {
//                score -= 8;
//            }
//
//        } catch (Exception e) {
//            log.warn("获取食物营养信息失败: {}", foodName, e);
//            score = fallbackScore(foodName, score);
//        }
//
//        return Math.max(0, Math.min(100, score));
//    }
//
//    private int calculateCaloriesScore(IngredientInfo nutrition) {
//        Integer calories = nutrition.getCalories();
//        if (calories == null) return 0;
//        if (calories > 450) return -15;
//        if (calories > 350) return -8;
//        if (calories > 250) return -3;
//        if (calories < 80) return 10;
//        if (calories < 150) return 5;
//        return 0;
//    }
//
//    private int calculateProteinScore(IngredientInfo nutrition) {
//        Double protein = nutrition.getProtein();
//        if (protein == null) return 0;
//        if (protein > 15) return 12;
//        if (protein > 10) return 8;
//        if (protein > 6) return 4;
//        if (protein < 2 && !isVegetable(nutrition.getName()) && !isFruit(nutrition.getName())) return -5;
//        return 0;
//    }
//
//    private int calculateFatScore(IngredientInfo nutrition) {
//        Double fat = nutrition.getFat();
//        if (fat == null) return 0;
//        if (fat > 25) return -15;
//        if (fat > 15) return -8;
//        if (fat > 8) return -3;
//        if (fat < 3) return 5;
//        return 0;
//    }
//
//    private int calculateFoodTypeScore(String foodName) {
//        int score = 0;
//        if (isVegetable(foodName)) score += 10;
//        if (isFruit(foodName)) score += 5;
//        if (isProtein(foodName)) score += 5;
//        if (isUnhealthy(foodName)) score -= 20;
//        return score;
//    }
//
//    private int fallbackScore(String foodName, int baseScore) {
//        int score = baseScore;
//        String lowerName = foodName.toLowerCase();
//
//        if (lowerName.contains("蒸") || lowerName.contains("煮") || lowerName.contains("炖")) {
//            score += 10;
//        }
//        if (lowerName.contains("炸") || lowerName.contains("烤") || lowerName.contains("煎")) {
//            score -= 15;
//        }
//        if (isVegetable(foodName) || isFruit(foodName)) {
//            score += 15;
//        }
//        if (isProtein(foodName)) {
//            score += 10;
//        }
//        if (isUnhealthy(foodName)) {
//            score -= 20;
//        }
//        if (lowerName.contains("米饭") || lowerName.contains("馒头") || lowerName.contains("面条")) {
//            score -= 5;
//        }
//        if (lowerName.contains("糙米") || lowerName.contains("燕麦") || lowerName.contains("杂粮") || lowerName.contains("全麦")) {
//            score += 8;
//        }
//
//        return Math.max(0, Math.min(100, score));
//    }
//
//    // ==================== 建议生成方法 ====================
//    public String generateSuggestions(int score) {
//        if (score >= 90) {
//            return "🎉 非常健康！营养均衡，烹饪方式得当，继续保持！";
//        } else if (score >= 80) {
//            return "👍 营养结构良好，可以再增加一些蔬菜水果让饮食更完美。";
//        } else if (score >= 70) {
//            return "✅ 整体不错，注意减少油炸和高糖食物，多选择蒸煮方式。";
//        } else if (score >= 60) {
//            return "⚠️ 基本达标，建议增加优质蛋白和膳食纤维，控制高热量食物。";
//        } else if (score >= 50) {
//            return "📝 有待改善，今天的高热量/高脂肪食物偏多，明天试试清淡一些。";
//        } else {
//            return "💪 需要调整饮食结构，减少油炸、甜食，增加蔬菜和优质蛋白。";
//        }
//    }
//
//    public String generateDynamicAdvice(DietAnalysis analysis, List<DietRecord> records, String constitution) {
//        List<String> advices = new ArrayList<>();
//
//        // 1. 蔬菜摄入建议
//        if (analysis.vegetableCount == 0) {
//            advices.add("🥬 今天没有吃蔬菜，建议午餐或晚餐增加绿叶蔬菜（如菠菜、西兰花、青菜）");
//        } else if (analysis.vegetableGrams < 200) {
//            advices.add("🥬 今天蔬菜摄入偏少（约" + analysis.vegetableGrams + "g），建议增加到300g以上");
//        } else if (analysis.vegetableGrams >= 300) {
//            advices.add("✅ 蔬菜摄入充足（" + analysis.vegetableGrams + "g），继续保持！");
//        }
//
//        // 2. 水果摄入建议
//        if (analysis.fruitCount == 0) {
//            advices.add("🍎 今天没有吃水果，建议餐后吃一个苹果、香蕉或橙子");
//        } else if (analysis.fruitGrams < 150) {
//            advices.add("🍎 水果摄入偏少，建议每天吃200-300g新鲜水果");
//        }
//
//        // 3. 蛋白质摄入建议
//        if (analysis.proteinCount == 0) {
//            advices.add("🥩 今天没有摄入优质蛋白，建议增加鸡蛋、鱼肉或豆腐");
//        } else if (analysis.proteinCount == 1) {
//            advices.add("🥩 蛋白质来源较单一，可以尝试多样化（如早餐鸡蛋、午餐鱼肉、晚餐豆腐）");
//        }
//
//        // 4. 主食建议
//        if (analysis.grainCount == 0) {
//            advices.add("🍚 今天没有记录主食，建议适量摄入米饭、面条或杂粮");
//        }
//
//        // 5. 早餐建议
//        if (!analysis.hasBreakfast) {
//            advices.add("🌅 今天没有记录早餐，规律吃早餐对全天代谢很重要");
//        }
//
//        // 6. 不健康食物警告
//        if (analysis.unhealthyCount > 0) {
//            String unhealthyList = String.join("、", analysis.unhealthyFoods);
//            advices.add("⚠️ 今天吃了" + unhealthyList + "，建议减少油炸和高糖食物");
//        }
//
//        // 7. 食物多样性建议
//        if (analysis.foodTypeCount < 5) {
//            advices.add("🍽️ 今天食物种类偏少（仅" + analysis.foodTypeCount + "种），建议增加食物多样性");
//        } else if (analysis.foodTypeCount >= 8) {
//            advices.add("🎉 食物多样性很好（" + analysis.foodTypeCount + "种），营养均衡！");
//        }
//
//        // 8. 体质相关建议
//        if (constitution != null) {
//            String constitutionAdvice = getConstitutionAdvice(constitution, analysis);
//            if (!constitutionAdvice.isEmpty()) {
//                advices.add(constitutionAdvice);
//            }
//        }
//
//        if (advices.isEmpty()) {
//            return "👍 今天的饮食结构很好！继续保持均衡营养的好习惯！";
//        }
//
//        return String.join("<br>", advices);
//    }
//
//    private String getConstitutionAdvice(String constitution, DietAnalysis analysis) {
//        switch (constitution) {
//            case "气虚质":
//                if (!analysis.allFoods.contains("山药") && !analysis.allFoods.contains("大枣")) {
//                    return "🌿 您是气虚体质，建议多吃山药、大枣、黄芪炖鸡补气";
//                }
//                break;
//            case "阳虚质":
//                if (!analysis.allFoods.contains("生姜") && !analysis.allFoods.contains("羊肉")) {
//                    return "🔥 您是阳虚体质，建议多吃生姜、羊肉、韭菜温补阳气";
//                }
//                break;
//            case "阴虚质":
//                if (!analysis.allFoods.contains("百合") && !analysis.allFoods.contains("银耳")) {
//                    return "💧 您是阴虚体质，建议多吃百合、银耳、梨滋阴润燥";
//                }
//                break;
//            case "痰湿质":
//                if (analysis.unhealthyCount > 0) {
//                    return "💦 您是痰湿体质，建议少吃甜食油腻，多吃薏米、冬瓜祛湿";
//                }
//                break;
//            case "湿热质":
//                if (analysis.unhealthyCount > 0) {
//                    return "🌊 您是湿热体质，建议少吃辛辣烧烤，多吃绿豆、苦瓜清热";
//                }
//                break;
//        }
//        return "";
//    }
//
//    // ==================== 饮食结构分析 ====================
//    private DietAnalysis analyzeDietStructure(List<DietRecord> records) {
//        DietAnalysis analysis = new DietAnalysis();
//
//        for (DietRecord record : records) {
//            String foodName = record.getFoodName();
//            Double grams = record.getGrams() != null ? record.getGrams() : 0;
//
//            // 分类统计
//            if (isVegetable(foodName)) {
//                analysis.vegetableCount++;
//                analysis.vegetableGrams += grams;
//            } else if (isFruit(foodName)) {
//                analysis.fruitCount++;
//                analysis.fruitGrams += grams;
//            } else if (isProtein(foodName)) {
//                analysis.proteinCount++;
//                analysis.proteinGrams += grams;
//            } else if (isGrain(foodName)) {
//                analysis.grainCount++;
//            }
//
//            // 统计不健康食物
//            if (isUnhealthy(foodName)) {
//                analysis.unhealthyCount++;
//                analysis.unhealthyFoods.add(foodName);
//            }
//
//            // 记录餐次
//            if ("BREAKFAST".equals(record.getMealType())) {
//                analysis.hasBreakfast = true;
//            }
//            if ("DINNER".equals(record.getMealType())) {
//                analysis.dinnerTime = record.getRecordDate();
//            }
//
//            analysis.allFoods.add(foodName);
//        }
//
//        analysis.foodTypeCount = analysis.allFoods.size();
//        return analysis;
//    }
//
//    // ==================== CRUD 方法 ====================
//    public void addRecord(DietRecord record) {
//        record.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
//        record.setCreateTime(LocalDateTime.now());
//        record.setRecordDate(record.getRecordDate() != null ? record.getRecordDate() : LocalDateTime.now());
//
//        // 处理克重换算
//        if (record.getOriginalAmount() != null && record.getOriginalUnit() != null) {
//            Double grams = convertToGrams(record.getOriginalAmount(), record.getOriginalUnit());
//            record.setEstimatedGrams(grams);
//            record.setGrams(grams);
//        } else if (record.getGrams() != null) {
//            record.setEstimatedGrams(record.getGrams());
//            record.setOriginalAmount(record.getGrams());
//            record.setOriginalUnit("g");
//        }
//
//        int score = calculateHealthScore(record);
//        record.setHealthScore(score);
//        record.setSuggestions(generateSuggestions(score));
//
//        dietRecordMapper.insert(record);
//        log.info("添加饮食记录: userId={}, foodName={}, original={}{}",
//                record.getUserId(), record.getFoodName(),
//                record.getOriginalAmount(), record.getOriginalUnit());
//    }
//
//    public boolean updateRecord(DietRecord record) {
//        int score = calculateHealthScore(record);
//        record.setHealthScore(score);
//        record.setSuggestions(generateSuggestions(score));
//
//        int result = dietRecordMapper.updateById(record);
//        log.info("更新饮食记录: id={}, userId={}, foodName={}", record.getId(), record.getUserId(), record.getFoodName());
//        return result > 0;
//    }
//
//    public boolean deleteRecord(String id, String userId) {
//        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(DietRecord::getId, id)
//                .eq(DietRecord::getUserId, userId);
//        int result = dietRecordMapper.delete(wrapper);
//        log.info("删除饮食记录: id={}, userId={}, result={}", id, userId, result > 0);
//        return result > 0;
//    }
//
//    public DietRecord getRecordById(String id) {
//        return dietRecordMapper.selectById(id);
//    }
//
//    public List<DietRecord> getTodayRecords(String userId) {
//        return dietRecordMapper.selectByUserIdAndDate(userId, LocalDate.now());
//    }
//
//    public List<DietRecord> getRecordsByDate(String userId, LocalDate date) {
//        return dietRecordMapper.selectByUserIdAndDate(userId, date);
//    }
//
//    public List<DietRecord> getWeekRecords(String userId) {
//        LocalDate endDate = LocalDate.now();
//        LocalDate startDate = endDate.minusDays(7);
//        return dietRecordMapper.selectByDateRange(userId, startDate, endDate);
//    }
//
//    public Map<String, Object> generateDailyReport(String userId, String constitution) {
//        List<DietRecord> records = getTodayRecords(userId);
//
//        Map<String, Object> report = new HashMap<>();
//        report.put("date", LocalDate.now().toString());
//        report.put("totalRecords", records.size());
//
//        if (records.isEmpty()) {
//            report.put("message", "今天还没有饮食记录");
//            report.put("dynamicAdvice", "开始记录您的饮食，AI会为您提供个性化建议");
//            return report;
//        }
//
//        double totalCalories = records.stream()
//                .mapToDouble(r -> {
//                    if (r.getCalories() != null) return r.getCalories();
//                    if (r.getEstimatedGrams() != null && r.getEstimatedGrams() > 0) {
//                        return r.getEstimatedGrams() * 1.2;
//                    }
//                    return 0;
//                })
//                .sum();
//        double avgScore = records.stream()
//                .mapToInt(DietRecord::getHealthScore)
//                .average()
//                .orElse(0);
//
//        report.put("totalCalories", Math.round(totalCalories));
//        report.put("avgHealthScore", Math.round(avgScore));
//
//        if (avgScore >= 80) {
//            report.put("level", "excellent");
//            report.put("levelText", "优秀");
//        } else if (avgScore >= 60) {
//            report.put("level", "good");
//            report.put("levelText", "良好");
//        } else {
//            report.put("level", "needImprove");
//            report.put("levelText", "需改进");
//        }
//
//        DietAnalysis analysis = analyzeDietStructure(records);
//        String dynamicAdvice = generateDynamicAdvice(analysis, records, constitution);
//        report.put("dynamicAdvice", dynamicAdvice);
//        report.put("advice", dynamicAdvice);
//
//        return report;
//    }
//
//    public List<Map<String, Object>> getMostUsedFoods(String userId, int limit) {
//        return dietRecordMapper.selectMostUsedFoods(userId, limit);
//    }
//
//    public List<DietRecord> getMealRecordsByRecordId(String recordId, String userId) {
//        DietRecord targetRecord = dietRecordMapper.selectById(recordId);
//        if (targetRecord == null) return new ArrayList<>();
//
//        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(DietRecord::getUserId, userId)
//                .eq(DietRecord::getMealType, targetRecord.getMealType())
//                .apply("DATE(record_date) = DATE({0})", targetRecord.getRecordDate());
//
//        return dietRecordMapper.selectList(wrapper);
//    }
//
//    @Transactional
//    public boolean updateMealRecords(String userId, String mealType, LocalDateTime recordDate,
//                                     List<DietRecord> newRecords, List<String> deletedIds) {
//        if (deletedIds != null && !deletedIds.isEmpty()) {
//            LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
//            wrapper.eq(DietRecord::getUserId, userId)
//                    .in(DietRecord::getId, deletedIds);
//            dietRecordMapper.delete(wrapper);
//        }
//
//        for (DietRecord record : newRecords) {
//            if (record.getId() != null && !record.getId().isEmpty()) {
//                updateRecord(record);
//            } else {
//                addRecord(record);
//            }
//        }
//        return true;
//    }
//
//    // ==================== 内部类 ====================
//    public static class DietAnalysis {
//        int vegetableCount = 0;
//        int vegetableGrams = 0;
//        int fruitCount = 0;
//        int fruitGrams = 0;
//        int proteinCount = 0;
//        int proteinGrams = 0;
//        int grainCount = 0;
//        int unhealthyCount = 0;
//        int foodTypeCount = 0;
//        boolean hasBreakfast = false;
//        LocalDateTime dinnerTime = null;
//        Set<String> unhealthyFoods = new HashSet<>();
//        Set<String> allFoods = new HashSet<>();
//    }
//}

package org.example.shiyangai.service.diet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.mapper.DietRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietRecordService {

    private final DietRecordMapper
            dietRecordMapper;

    /**
     * 单位换算服务
     */
    private final DietConvertService
            dietConvertService;

    /**
     * 健康评分服务
     */
    private final DietScoreService
            dietScoreService;

    /**
     * 建议生成服务
     */
    private final DietAdviceService
            dietAdviceService;

    /**
     * 报告生成服务
     */
    private final DietReportService
            dietReportService;

    /**
     * 添加饮食记录
     */
    public void addRecord(
            DietRecord record
    ) {

        record.setId(
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        record.setRecordDate(
                record.getRecordDate() != null
                        ? record.getRecordDate()
                        : LocalDateTime.now()
        );

        // ====================
        // 克重换算
        // ====================

        if (record.getOriginalAmount() != null
                &&
                record.getOriginalUnit() != null) {

            Double grams =
                    dietConvertService
                            .convertToGrams(
                                    record.getOriginalAmount(),
                                    record.getOriginalUnit()
                            );

            record.setEstimatedGrams(
                    grams
            );

            record.setGrams(
                    grams
            );

        } else if (record.getGrams() != null) {

            record.setEstimatedGrams(
                    record.getGrams()
            );

            record.setOriginalAmount(
                    record.getGrams()
            );

            record.setOriginalUnit(
                    "g"
            );
        }

        // ====================
        // 健康评分
        // ====================

        int score =
                dietScoreService
                        .calculateHealthScore(
                                record
                        );

        record.setHealthScore(
                score
        );

        // ====================
        // 建议生成
        // ====================

        record.setSuggestions(
                dietAdviceService
                        .generateSuggestions(
                                score
                        )
        );

        // ====================
        // 保存数据库
        // ====================

        dietRecordMapper.insert(
                record
        );

        log.info(
                "添加饮食记录: userId={}, foodName={}",
                record.getUserId(),
                record.getFoodName()
        );
    }

    /**
     * 更新记录
     */
    public boolean updateRecord(
            DietRecord record
    ) {

        int score =
                dietScoreService
                        .calculateHealthScore(
                                record
                        );

        record.setHealthScore(
                score
        );

        record.setSuggestions(
                dietAdviceService
                        .generateSuggestions(
                                score
                        )
        );

        int result =
                dietRecordMapper
                        .updateById(record);

        log.info(
                "更新饮食记录: id={}, userId={}",
                record.getId(),
                record.getUserId()
        );

        return result > 0;
    }

    /**
     * 删除记录
     */
    public boolean deleteRecord(
            String id,
            String userId
    ) {

        LambdaQueryWrapper<DietRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                        DietRecord::getId,
                        id
                )
                .eq(
                        DietRecord::getUserId,
                        userId
                );

        int result =
                dietRecordMapper.delete(
                        wrapper
                );

        log.info(
                "删除饮食记录: id={}, userId={}",
                id,
                userId
        );

        return result > 0;
    }

    /**
     * 根据ID查询
     */
    public DietRecord getRecordById(
            String id
    ) {

        return dietRecordMapper
                .selectById(id);
    }

    /**
     * 获取今日记录
     */
    public List<DietRecord> getTodayRecords(
            String userId
    ) {

        return dietRecordMapper
                .selectByUserIdAndDate(
                        userId,
                        LocalDate.now()
                );
    }

    /**
     * 获取指定日期记录
     */
    public List<DietRecord> getRecordsByDate(
            String userId,
            LocalDate date
    ) {

        return dietRecordMapper
                .selectByUserIdAndDate(
                        userId,
                        date
                );
    }

    /**
     * 获取近7天记录
     */
    public List<DietRecord> getWeekRecords(
            String userId
    ) {

        LocalDate endDate =
                LocalDate.now();

        LocalDate startDate =
                endDate.minusDays(7);

        return dietRecordMapper
                .selectByDateRange(
                        userId,
                        startDate,
                        endDate
                );
    }

    /**
     * 生成日报
     */
    public Map<String, Object> generateDailyReport(
            String userId,
            String constitution
    ) {

        List<DietRecord> records =
                getTodayRecords(userId);

        return dietReportService
                .generateDailyReport(
                        records,
                        constitution
                );
    }

    /**
     * 获取最常吃食物
     */
    public List<Map<String, Object>>
    getMostUsedFoods(
            String userId,
            int limit
    ) {

        return dietRecordMapper
                .selectMostUsedFoods(
                        userId,
                        limit
                );
    }

    /**
     * 获取同一餐记录
     */
    public List<DietRecord>
    getMealRecordsByRecordId(
            String recordId,
            String userId
    ) {

        DietRecord targetRecord =
                dietRecordMapper
                        .selectById(recordId);

        if (targetRecord == null) {

            return new ArrayList<>();
        }

        LambdaQueryWrapper<DietRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(
                        DietRecord::getUserId,
                        userId
                )
                .eq(
                        DietRecord::getMealType,
                        targetRecord.getMealType()
                )
                .apply(
                        "DATE(record_date) = DATE({0})",
                        targetRecord.getRecordDate()
                );

        return dietRecordMapper
                .selectList(wrapper);
    }

    /**
     * 批量更新餐次记录
     */
    @Transactional
    public boolean updateMealRecords(
            String userId,
            String mealType,
            LocalDateTime recordDate,
            List<DietRecord> newRecords,
            List<String> deletedIds
    ) {

        // ====================
        // 删除记录
        // ====================

        if (deletedIds != null
                && !deletedIds.isEmpty()) {

            LambdaQueryWrapper<DietRecord> wrapper =
                    new LambdaQueryWrapper<>();

            wrapper.eq(
                            DietRecord::getUserId,
                            userId
                    )
                    .in(
                            DietRecord::getId,
                            deletedIds
                    );

            dietRecordMapper.delete(
                    wrapper
            );
        }

        // ====================
        // 更新/新增
        // ====================

        for (DietRecord record : newRecords) {

            if (record.getId() != null
                    &&
                    !record.getId().isEmpty()) {

                updateRecord(record);

            } else {

                addRecord(record);
            }
        }

        return true;
    }
}