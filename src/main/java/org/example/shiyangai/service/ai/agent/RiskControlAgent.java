// RiskControlAgent.java（完整修复版）
package org.example.shiyangai.service.ai.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.example.shiyangai.service.ai.agent.result.AgentResult;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class RiskControlAgent extends BaseAgent {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 体质-高风险食物映射（中医理论）
    private static final Map<String, List<String>> CONSTITUTION_HIGH_RISK = new HashMap<>();
    private static final Map<String, List<String>> CONSTITUTION_MEDIUM_RISK = new HashMap<>();

    static {
        // 阴虚体质：忌燥热、辛辣
        CONSTITUTION_HIGH_RISK.put("阴虚", List.of(
                "火锅", "烧烤", "麻辣烫", "辣椒", "花椒", "生姜", "羊肉", "狗肉",
                "龙眼", "荔枝", "榴莲", "炸鸡", "薯片", "白酒"
        ));
        CONSTITUTION_MEDIUM_RISK.put("阴虚", List.of(
                "牛肉", "鸡肉", "韭菜", "大蒜", "咖啡", "浓茶"
        ));

        // 湿热体质：忌油腻、甜腻
        CONSTITUTION_HIGH_RISK.put("湿热", List.of(
                "奶茶", "炸鸡", "肥肉", "烧烤", "火锅", "辣椒", "甜点",
                "蛋糕", "巧克力", "冰淇淋", "糯米"
        ));
        CONSTITUTION_MEDIUM_RISK.put("湿热", List.of(
                "红肉", "蛋黄", "花生", "坚果", "蜂蜜"
        ));

        // 阳虚体质：忌生冷、寒凉
        CONSTITUTION_HIGH_RISK.put("阳虚", List.of(
                "冰淇淋", "冰水", "冷饮", "西瓜", "苦瓜", "黄瓜", "绿豆",
                "螃蟹", "田螺", "生蚝", "香蕉", "梨"
        ));
        CONSTITUTION_MEDIUM_RISK.put("阳虚", List.of(
                "牛奶", "豆腐", "绿茶", "橙子", "柚子"
        ));

        // 气虚体质：忌耗气、难消化
        CONSTITUTION_HIGH_RISK.put("气虚", List.of(
                "萝卜", "山楂", "槟榔", "浓茶", "咖啡", "肥肉", "油炸食品"
        ));
        CONSTITUTION_MEDIUM_RISK.put("气虚", List.of(
                "大蒜", "韭菜", "螃蟹", "冷饮"
        ));

        // 痰湿体质：忌甜腻、生冷
        CONSTITUTION_HIGH_RISK.put("痰湿", List.of(
                "甜点", "奶茶", "肥肉", "糯米", "冰淇淋", "啤酒", "碳酸饮料"
        ));
        CONSTITUTION_MEDIUM_RISK.put("痰湿", List.of(
                "红肉", "蛋黄", "花生", "牛奶"
        ));
    }

    public RiskControlAgent() {
        super("RiskControlAgent");
    }

    @Override
    public int getPriority() {
        return 42;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return extractFoodName(ctx.getUserMessage()) != null;
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String foodName = ctx.getFoodName();
        if (foodName == null || foodName.isEmpty()) {
            foodName = extractFoodName(ctx.getUserMessage());
            ctx.setFoodName(foodName);
        }

        if (foodName == null) {
            log.debug("No food detected, skip risk control");
            return true;
        }

        // 创建 Agent 决策结果
        AgentResult result = new AgentResult("RiskControlAgent", 10);
        boolean hasHighRisk = false;

        // ========== 1. 过敏风险检测（从 HealthProfile 读取） ==========
        boolean allergyRisk = checkAllergyRisk(ctx, foodName, result);
        if (allergyRisk) {
            hasHighRisk = true;
        }

        // ========== 2. 忌口风险检测（从 HealthProfile 读取） ==========
        boolean avoidanceRisk = checkAvoidanceRisk(ctx, foodName, result);
        if (avoidanceRisk) {
            hasHighRisk = true;
        }

        // ========== 3. 慢性病风险检测 ==========
        boolean chronicRisk = checkChronicDiseaseRisk(ctx, foodName, result);
        if (chronicRisk) {
            hasHighRisk = true;
        }

        // ========== 4. 体质风险检测 ==========
        boolean constitutionRisk = checkConstitutionRisk(ctx, foodName, result);
        if (constitutionRisk) {
            hasHighRisk = true;
        }

        // ========== 5. 食物属性匹配检测 ==========
        checkFoodPropertyMatch(ctx, foodName, result);

        // 保存决策结果
        ctx.addAgentResult(result);

        // 如果有高风险，短路并生成回复
        if (hasHighRisk) {
            ctx.setShortCircuit(true);
            ctx.setReply(buildRiskReply(result));
            log.warn("RiskControlAgent triggered short-circuit for food: {}", foodName);
        }

        log.info("Risk assessment completed: food={}, hasHighRisk={}, observations={}, analysis={}",
                foodName, hasHighRisk, result.getObservations().size(), result.getAnalysis().size());

        logEnd(ctx);
        return true;
    }

    /**
     * 检查过敏风险 - 从 HealthProfile 读取过敏源
     */
    private boolean checkAllergyRisk(ConversationContext ctx, String foodName, AgentResult result) {
        HealthProfile profile = ctx.getProfile();

        // 添加观察：用户是否有健康档案
        if (profile == null) {
            result.addObservation("用户尚未建立健康档案，无法获取过敏信息");
            return false;
        }

        // 读取过敏源字段（支持多种格式）
        List<String> allergies = getAllergiesFromProfile(profile);

        if (allergies.isEmpty()) {
            result.addObservation("用户健康档案中未记录食物过敏史");
            return false;
        }

        result.addObservation(String.format("用户健康档案记录的过敏食物：%s", String.join("、", allergies)));

        // 检查当前食物是否在过敏列表中（模糊匹配）
        for (String allergy : allergies) {
            if (isFoodMatch(foodName, allergy)) {
                result.addAnalysis(String.format(
                        "🚨 严重警告：【%s】是您的已知过敏食物。过敏反应可能包括：皮疹、呼吸困难、肠胃不适，严重时可致过敏性休克。",
                        allergy));
                result.addRecommendation("🚫 禁止食用！请立即停止食用。");
                result.addRecommendation(String.format("💡 替代建议：可以尝试%s等其他安全的食材", getAlternativeFood(foodName)));
                result.setRiskLevel("CRITICAL");
                return true;
            }
        }

        result.addObservation(String.format("当前食物【%s】不在过敏列表中", foodName));
        return false;
    }

    /**
     * 从 HealthProfile 中提取过敏源列表
     * 支持多种存储格式：
     * 1. JSON 数组：["花生","芒果","虾"]
     * 2. 逗号分隔：花生,芒果,虾
     * 3. 单个字符串：花生
     */
    private List<String> getAllergiesFromProfile(HealthProfile profile) {
        List<String> allergies = new ArrayList<>();

        // 尝试从 allergies 字段读取（JSON 数组格式）
        String allergiesJson = profile.getAllergies();
        if (allergiesJson != null && !allergiesJson.isEmpty()) {
            try {
                List<String> parsed = objectMapper.readValue(allergiesJson, new TypeReference<List<String>>() {});
                if (parsed != null) {
                    allergies.addAll(parsed);
                }
            } catch (Exception e) {
                // 解析失败，尝试按逗号分隔
                if (allergiesJson.contains(",")) {
                    allergies.addAll(Arrays.stream(allergiesJson.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList());
                } else if (!allergiesJson.trim().isEmpty()) {
                    allergies.add(allergiesJson.trim());
                }
            }
        }

        // 兼容旧字段 allergyFoods（如果有）
        String allergyFoods = profile.getAllergies();
        if (allergyFoods != null && !allergyFoods.isEmpty()) {
            if (allergyFoods.contains(",")) {
                allergies.addAll(Arrays.stream(allergyFoods.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList());
            } else if (!allergyFoods.trim().isEmpty()) {
                allergies.add(allergyFoods.trim());
            }
        }

        return allergies.stream().distinct().toList();
    }

    /**
     * 检查忌口风险
     */
    private boolean checkAvoidanceRisk(ConversationContext ctx, String foodName, AgentResult result) {
        HealthProfile profile = ctx.getProfile();

        if (profile == null) {
            return false;
        }

        String avoidanceJson = profile.getFoodAvoidance();
        if (avoidanceJson == null || avoidanceJson.isEmpty()) {
            return false;
        }

        List<String> avoidances = parseJsonArray(avoidanceJson);
        if (avoidances.isEmpty()) {
            return false;
        }

        result.addObservation(String.format("用户记录的忌口食物：%s", String.join("、", avoidances)));

        for (String avoidance : avoidances) {
            if (isFoodMatch(foodName, avoidance)) {
                result.addAnalysis(String.format("【%s】在您的忌口列表中，可能对您的身体状况造成不适。", avoidance));
                result.addRecommendation("⚠️ 建议避免食用该食物");
                result.addRecommendation("💡 可以选择性质温和的替代食材");
                result.setRiskLevel("HIGH");
                return true;
            }
        }

        return false;
    }

    /**
     * 检查慢性病相关风险
     */
    private boolean checkChronicDiseaseRisk(ConversationContext ctx, String foodName, AgentResult result) {
        HealthProfile profile = ctx.getProfile();

        if (profile == null) {
            return false;
        }

        String diseases = profile.getPastDiseases();
        if (diseases == null || diseases.isEmpty()) {
            return false;
        }

        result.addObservation(String.format("用户慢性病史：%s", diseases));

        // 糖尿病风险
        if (diseases.contains("糖尿病") || diseases.contains("高血糖") || diseases.contains("血糖高")) {
            if (foodName.contains("糖") || foodName.contains("蛋糕") || foodName.contains("奶茶") ||
                    foodName.contains("甜点") || foodName.contains("冰淇淋") || foodName.contains("巧克力")) {
                result.addAnalysis(String.format("您有糖尿病/高血糖史，【%s】含糖量较高，食用后可能导致血糖波动。", foodName));
                result.addRecommendation("⚠️ 糖尿病患者建议控制高糖食物摄入");
                result.addRecommendation("💡 如需甜味，可适量使用代糖或选择低GI水果");
                result.setRiskLevel("HIGH");
                return true;
            }
        }

        // 高血压风险
        if (diseases.contains("高血压") || diseases.contains("血压高")) {
            if (foodName.contains("咸") || foodName.contains("腌") || foodName.contains("腊") ||
                    foodName.equals("火锅") || foodName.equals("烧烤")) {
                result.addAnalysis(String.format("您有高血压史，【%s】含盐量可能较高，不利于血压控制。", foodName));
                result.addRecommendation("⚠️ 高血压患者建议控制盐分摄入");
                result.setRiskLevel("MEDIUM");
            }
        }

        // 痛风风险
        if (diseases.contains("痛风") || diseases.contains("尿酸高")) {
            if (foodName.contains("海鲜") || foodName.contains("虾") || foodName.contains("蟹") ||
                    foodName.contains("贝类") || foodName.contains("动物内脏") || foodName.contains("啤酒")) {
                result.addAnalysis(String.format("您有痛风/高尿酸史，【%s】嘌呤含量较高，可能诱发痛风发作。", foodName));
                result.addRecommendation("⚠️ 痛风患者建议避免高嘌呤食物");
                result.addRecommendation("💡 建议多喝水，促进尿酸排泄");
                result.setRiskLevel("HIGH");
                return true;
            }
        }

        return false;
    }

    /**
     * 检查体质风险
     */
    private boolean checkConstitutionRisk(ConversationContext ctx, String foodName, AgentResult result) {
        String constitution = ctx.getConstitution();
        if (constitution == null || constitution.isEmpty()) {
            result.addObservation("用户尚未确定中医体质类型");
            return false;
        }

        result.addObservation(String.format("用户中医体质：%s", constitution));

        // 检查高风险食物
        List<String> highRiskFoods = CONSTITUTION_HIGH_RISK.get(constitution);
        if (highRiskFoods != null) {
            for (String riskFood : highRiskFoods) {
                if (isFoodMatch(foodName, riskFood)) {
                    result.addAnalysis(String.format(
                            "根据中医理论，【%s】体质不宜食用【%s】。食用后会加重体质偏颇，可能导致口干、上火、失眠等症状。",
                            constitution, foodName));
                    result.addRecommendation("⚠️ 强烈建议避免食用");
                    result.addRecommendation(getConstitutionAlternative(constitution, foodName));
                    result.setRiskLevel("HIGH");
                    return true;
                }
            }
        }

        // 检查中风险食物
        List<String> mediumRiskFoods = CONSTITUTION_MEDIUM_RISK.get(constitution);
        if (mediumRiskFoods != null) {
            for (String riskFood : mediumRiskFoods) {
                if (isFoodMatch(foodName, riskFood)) {
                    result.addAnalysis(String.format(
                            "根据中医理论，【%s】体质食用【%s】需要适量控制，过多食用可能引起不适。",
                            constitution, foodName));
                    result.addRecommendation("⚠️ 可以少量食用，但不宜频繁");
                    result.setRiskLevel("MEDIUM");
                    return false;
                }
            }
        }

        result.addAnalysis(String.format("根据【%s】体质分析，【%s】在可接受范围内", constitution, foodName));
        return false;
    }

    /**
     * 检查食物属性与体质匹配度
     */
    private void checkFoodPropertyMatch(ConversationContext ctx, String foodName, AgentResult result) {
        IngredientInfo foodInfo = ctx.getFoodInfo();
        String constitution = ctx.getConstitution();

        if (foodInfo == null || constitution == null) {
            return;
        }

        String property = foodInfo.getProperty();
        if (property == null || property.isEmpty()) {
            return;
        }

        result.addObservation(String.format("中医视角：【%s】属性为【%s】性", foodName, property));

        // 体质与适宜食物属性映射
        Map<String, List<String>> suitableProperty = Map.of(
                "阴虚", List.of("凉", "寒", "平"),
                "阳虚", List.of("温", "热", "平"),
                "气虚", List.of("温", "平"),
                "湿热", List.of("凉", "寒"),
                "痰湿", List.of("温", "平"),
                "血瘀", List.of("温", "平"),
                "气郁", List.of("温", "平")
        );

        List<String> suitable = suitableProperty.getOrDefault(constitution, List.of("平"));
        if (suitable.contains(property)) {
            result.addAnalysis(String.format("【%s】性食物适合您的【%s】体质", property, constitution));
        } else if (("热".equals(property) || "温".equals(property)) && "阴虚".equals(constitution)) {
            result.addAnalysis(String.format("【%s】性食物偏燥热，与您的【%s】体质不太匹配，食用后可能加重阴虚症状", property, constitution));
            result.addRecommendation("💡 建议选择凉性或平性食物，如：梨、百合、银耳、绿豆");
        } else if (("凉".equals(property) || "寒".equals(property)) && "阳虚".equals(constitution)) {
            result.addAnalysis(String.format("【%s】性食物偏寒凉，与您的【%s】体质不太匹配，可能损伤阳气", property, constitution));
            result.addRecommendation("💡 建议选择温性或热性食物，如：生姜、红枣、桂圆、羊肉");
        }
    }

    /**
     * 判断食物是否匹配关键词
     */
    private boolean isFoodMatch(String foodName, String keyword) {
        if (foodName == null || keyword == null) return false;
        // 完整匹配或包含匹配
        return foodName.equals(keyword) ||
                foodName.contains(keyword) ||
                keyword.contains(foodName);
    }

    /**
     * 解析 JSON 数组字段
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            if (json.contains(",") && !json.startsWith("[")) {
                return Arrays.stream(json.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            return new ArrayList<>();
        }
    }

    private String getAlternativeFood(String foodName) {
        if (foodName.contains("花生") || foodName.contains("花生酱")) return "杏仁、腰果、芝麻酱";
        if (foodName.contains("鸡蛋")) return "豆腐、豆浆、素肉";
        if (foodName.contains("虾") || foodName.contains("海鲜")) return "鱼肉、鸡肉、豆腐";
        if (foodName.contains("牛奶")) return "豆浆、杏仁奶、燕麦奶";
        if (foodName.contains("芒果")) return "苹果、梨、橙子";
        return "其他安全的替代食材";
    }

    private String getConstitutionAlternative(String constitution, String foodName) {
        if ("阴虚".equals(constitution)) {
            return "💡 替代建议：百合、银耳、雪梨、莲子、蜂蜜、山药";
        }
        if ("湿热".equals(constitution)) {
            return "💡 替代建议：绿豆、薏米、冬瓜、苦瓜、赤小豆、荷叶";
        }
        if ("阳虚".equals(constitution)) {
            return "💡 替代建议：生姜、红枣、桂圆、核桃、羊肉、韭菜";
        }
        if ("气虚".equals(constitution)) {
            return "💡 替代建议：山药、黄芪、党参、大枣、鸡肉、小米";
        }
        if ("痰湿".equals(constitution)) {
            return "💡 替代建议：薏米、赤小豆、茯苓、白萝卜、陈皮";
        }
        return "💡 建议选择清淡、易消化、性质平和的食材";
    }

    private String buildRiskReply(AgentResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ **饮食安全提醒**\n\n");

        sb.append("🔍 **风险评估**\n");
        for (String analysis : result.getAnalysis()) {
            sb.append("• ").append(analysis).append("\n");
        }

        sb.append("\n💡 **安全建议**\n");
        for (String rec : result.getRecommendations()) {
            sb.append("• ").append(rec).append("\n");
        }

        sb.append("\n---\n");
        sb.append("💚 **温馨提示**：健康饮食，安全第一。如有不适，请及时就医。");

        return sb.toString();
    }

    private String extractFoodName(String message) {
        if (message == null) return null;
        String[] keywords = {
                "玉米", "苹果", "梨", "香蕉", "西瓜", "葡萄", "草莓", "橙子",
                "鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "鸭肉", "羊肉", "虾",
                "豆腐", "豆浆", "牛奶", "酸奶", "山药", "红枣", "枸杞", "薏米",
                "生姜", "大蒜", "韭菜", "冬瓜", "苦瓜", "黄瓜", "萝卜", "胡萝卜",
                "菠菜", "青菜", "西兰花", "番茄", "茄子", "土豆", "红薯",
                "火锅", "烧烤", "麻辣烫", "奶茶", "炸鸡", "冰淇淋",
                "花生", "花生酱", "芒果", "海鲜", "螃蟹", "田螺"
        };
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }
}