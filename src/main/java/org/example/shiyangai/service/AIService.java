// service/AIService.java - 完整增强版
package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.enums.ConstitutionType;
import org.example.shiyangai.mapper.ChatHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    @Autowired
    private BaiduBaikeService baiduBaikeService;

    @Autowired
    private OnDemandFoodService onDemandFoodService;

    @Autowired
    private HealthProfileService healthProfileService;

    @Autowired
    private DietRecordService dietRecordService;

    @Autowired
    private DynamicSolarTermService dynamicSolarTermService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private HealthReportService healthReportService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 核心聊天方法 - 增强版
     */
    public String chat(String userId, String userMessage, String constitution) {
        log.info("AI对话: userId={}, message={}, constitution={}", userId, userMessage, constitution);

        try {
            // 1. 获取用户完整信息
            HealthProfile profile = healthProfileService.getProfile(userId);
            List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
            List<DietRecord> weekRecords = dietRecordService.getWeekRecords(userId);

            // 2. 获取当前节气
            String solarTermAdvice = dynamicSolarTermService.getDynamicSolarTermAdvice(constitution);

            // 3. 获取聊天历史（最近6条）
            List<ChatHistory> history = chatHistoryMapper.getByUserId(userId, 6);

            // 4. 提取用户询问的食物名称
            String foodName = extractFoodName(userMessage);
            IngredientInfo foodInfo = null;

            // 5. 如果用户询问具体食物，调用百度百科获取真实信息
            if (foodName != null && !foodName.isEmpty()) {
                // 先查缓存
                Object cached = redisCacheService.getFoodInfo(foodName);
                if (cached instanceof IngredientInfo) {
                    foodInfo = (IngredientInfo) cached;
                    log.info("从缓存获取食材: {}", foodName);
                } else {
                    try {
                        foodInfo = baiduBaikeService.getIngredientInfo(foodName);
                        log.info("从百度百科获取食材: {}, 属性={}, 功效={}",
                                foodName,
                                foodInfo != null ? foodInfo.getProperty() : "null",
                                foodInfo != null ? foodInfo.getEffect() : "null");
                        if (foodInfo != null) {
                            redisCacheService.cacheFoodInfo(foodName, foodInfo);
                        }
                    } catch (Exception e) {
                        log.warn("百度百科获取失败，尝试本地服务: {}", e.getMessage());
                        foodInfo = onDemandFoodService.getFoodInfo(foodName);
                    }
                }
            }

            // 6. 提取症状
            String symptom = extractSymptom(userMessage);

            // 7. 检查是否在询问健康报告
            if (isAskingForReport(userMessage)) {
                return healthReportService.generateComprehensiveReport(userId, constitution);
            }

            // 8. 获取上次对话上下文
            String previousContext = redisCacheService.getChatContext(userId);

            // 9. 构建增强的系统提示词
            String systemPrompt = buildEnhancedSystemPrompt(
                    userId, constitution, profile, todayRecords, weekRecords,
                    solarTermAdvice, foodInfo, symptom, previousContext
            );

            // 10. 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            // 添加历史消息（最近6条，排除当前问题）
            int historyCount = 0;
            for (ChatHistory h : history) {
                if (historyCount >= 6) break;
                Map<String, String> historyMessage = new HashMap<>();
                String role = h.getRole();
                if ("ai".equals(role)) role = "assistant";
                historyMessage.put("role", role);
                historyMessage.put("content", h.getContent());
                messages.add(historyMessage);
                historyCount++;
            }

            // 添加当前用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            // 11. 调用API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", false);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var jsonNode = objectMapper.readTree(response.body());
                String reply = jsonNode
                        .path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText();

                // 保存聊天记录
                saveChat(userId, "user", userMessage);
                saveChat(userId, "assistant", reply);

                // 保存对话上下文到Redis（用于下一轮）
                redisCacheService.cacheChatContext(userId, extractContextFromConversation(userMessage, reply));

                log.info("AI回复成功: userId={}, 回复长度={}", userId, reply.length());
                return reply;
            } else {
                log.error("API调用失败: status={}", response.statusCode());
                return "抱歉，AI服务暂时不可用，请稍后再试。";
            }

        } catch (Exception e) {
            log.error("AI对话异常", e);
            return "抱歉，服务出现异常：" + e.getMessage();
        }
    }

    /**
     * 从用户消息中提取食物名称
     */
    private String extractFoodName(String message) {
        if (message == null) return null;

        // 常见食物关键词
        String[] foodKeywords = {
                "玉米", "甜玉米", "糯玉米", "苹果", "梨", "香蕉", "西瓜", "葡萄", "草莓", "橙子",
                "鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "鸭肉", "羊肉", "虾", "螃蟹",
                "豆腐", "豆浆", "牛奶", "酸奶",
                "山药", "红枣", "枸杞", "薏米", "红豆", "绿豆", "银耳", "百合", "莲子",
                "生姜", "大蒜", "韭菜", "冬瓜", "苦瓜", "黄瓜", "萝卜", "胡萝卜",
                "菠菜", "青菜", "西兰花", "番茄", "茄子", "土豆", "红薯"
        };

        for (String keyword : foodKeywords) {
            if (message.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    /**
     * 提取症状
     */
    private String extractSymptom(String userMessage) {
        if (userMessage == null) return "";
        if (userMessage.contains("痘") || userMessage.contains("痘痘")) return "长痘";
        if (userMessage.contains("上火")) return "上火";
        if (userMessage.contains("便秘")) return "便秘";
        if (userMessage.contains("失眠") || userMessage.contains("睡不好")) return "失眠";
        if (userMessage.contains("疲劳") || userMessage.contains("累")) return "疲劳";
        if (userMessage.contains("怕冷") || userMessage.contains("手脚冰凉")) return "怕冷";
        if (userMessage.contains("口干") || userMessage.contains("口渴")) return "口干";
        if (userMessage.contains("胃痛") || userMessage.contains("胃胀")) return "胃不适";
        if (userMessage.contains("头痛") || userMessage.contains("头晕")) return "头痛";
        if (userMessage.contains("咳嗽")) return "咳嗽";
        return "";
    }

    /**
     * 判断是否在询问报告
     */
    private boolean isAskingForReport(String message) {
        String lower = message.toLowerCase();
        return lower.contains("报告") || lower.contains("健康报告") ||
                lower.contains("饮食报告") || lower.contains("本周报告") ||
                lower.contains("健康总结");
    }

    /**
     * 构建增强的系统提示词
     */
    private String buildEnhancedSystemPrompt(String userId, String constitution, HealthProfile profile,
                                             List<DietRecord> todayRecords, List<DietRecord> weekRecords,
                                             String solarTermAdvice, IngredientInfo foodInfo,
                                             String symptom, String previousContext) {

        StringBuilder sb = new StringBuilder();

        sb.append("你是一位资深的中医老专家，具有30年临床经验。你的回答要亲切自然，像长辈关心晚辈一样。\n\n");

        // ========== 用户信息 ==========
        sb.append("【用户信息】\n");
        sb.append("- 中医体质：").append(constitution).append("\n");

        if (profile != null) {
            if (profile.getAge() != null) sb.append("- 年龄：").append(profile.getAge()).append("岁\n");
            if (profile.getGender() != null) sb.append("- 性别：").append(profile.getGender().equals("MALE") ? "男" : "女").append("\n");
            if (profile.getHeight() != null && profile.getWeight() != null) {
                double heightM = profile.getHeight() / 100;
                double bmi = profile.getWeight() / (heightM * heightM);
                String bmiStatus;
                if (bmi < 18.5) bmiStatus = "偏瘦";
                else if (bmi < 24) bmiStatus = "正常";
                else if (bmi < 28) bmiStatus = "超重";
                else bmiStatus = "肥胖";
                sb.append(String.format("- BMI：%.1f（%s）\n", bmi, bmiStatus));
            }
        }

        // ========== 过敏和忌口信息（重要！） ==========
        if (profile != null) {
            List<String> allergies = healthProfileService.parseJsonArray(profile.getAllergies());
            if (!allergies.isEmpty()) {
                sb.append("- ⚠️ 过敏史：").append(String.join("、", allergies));
                sb.append("（绝对不能推荐这些食物！如果用户询问，必须明确警告！）\n");
            }

            List<String> avoidance = healthProfileService.parseJsonArray(profile.getFoodAvoidance());
            if (!avoidance.isEmpty()) {
                sb.append("- 🚫 忌口食物：").append(String.join("、", avoidance));
                sb.append("（避免推荐这些食物）\n");
            }

            List<String> diseases = healthProfileService.parseJsonArray(profile.getPastDiseases());
            if (!diseases.isEmpty()) {
                sb.append("- 📝 既往病史：").append(String.join("、", diseases));
                sb.append("（推荐食物时需考虑病史影响）\n");
            }
        }

        // ========== 今日饮食 ==========
        if (!todayRecords.isEmpty()) {
            sb.append("\n【今日饮食记录】\n");
            for (DietRecord record : todayRecords) {
                String mealName = getMealName(record.getMealType());
                sb.append(String.format("- %s：%s %sg，健康评分：%d/100\n",
                        mealName, record.getFoodName(),
                        record.getGrams() != null ? record.getGrams() : 0,
                        record.getHealthScore()));
            }
        }

        // ========== 一周饮食统计 ==========
        if (!weekRecords.isEmpty()) {
            double avgScore = weekRecords.stream().mapToInt(DietRecord::getHealthScore).average().orElse(0);
            sb.append(String.format("\n【最近一周饮食健康评分】平均：%.0f/100\n", avgScore));
            if (avgScore < 60) {
                sb.append("⚠️ 用户最近饮食不太健康，需要给出明确的改善建议。\n");
            } else if (avgScore < 80) {
                sb.append("✓ 用户饮食基本健康，可以给出优化建议。\n");
            } else {
                sb.append("🎉 用户饮食很健康，继续保持并给予鼓励。\n");
            }
        }

        // ========== 节气养生 ==========
        sb.append("\n【当前节气养生参考】\n");
        sb.append(solarTermAdvice).append("\n");

        // ========== 用户询问的具体食材信息 ==========
        if (foodInfo != null) {
            sb.append("\n【用户询问的食材 - 真实数据（必须基于此回答！）】\n");
            sb.append("- 食材名称：").append(foodInfo.getName()).append("\n");
            sb.append("- 中医属性：").append(foodInfo.getProperty() != null ? foodInfo.getProperty() : "平").append("性\n");
            sb.append("- 味道：").append(foodInfo.getFlavor() != null ? foodInfo.getFlavor() : "甘").append("味\n");
            sb.append("- 归经：").append(foodInfo.getMeridian() != null ? foodInfo.getMeridian() : "脾、胃").append("\n");
            sb.append("- 主要功效：").append(foodInfo.getEffect() != null ? foodInfo.getEffect() : "补益身体").append("\n");
            sb.append("- 禁忌人群：").append(foodInfo.getContraindication() != null ? foodInfo.getContraindication() : "无明显禁忌").append("\n");

            // 判断是否适合当前体质
            int suitabilityScore = getSuitabilityScoreForFood(foodInfo, constitution);
            if (suitabilityScore > 0) {
                sb.append("- 对").append(constitution).append("体质：✅ 适合食用\n");
            } else if (suitabilityScore < 0) {
                sb.append("- 对").append(constitution).append("体质：❌ 不太适合，需要谨慎或少量食用\n");
            } else {
                sb.append("- 对").append(constitution).append("体质：⚖️ 中性，可适量食用\n");
            }
        }

        // ========== 症状 ==========
        if (!symptom.isEmpty()) {
            sb.append("\n【用户当前症状】").append(symptom).append("\n");
            sb.append("请根据症状给出调理建议，推荐适合的食疗方案。\n");
        }

        // ========== 历史对话上下文 ==========
        if (previousContext != null && !previousContext.isEmpty()) {
            sb.append("\n【上一轮对话摘要】").append(previousContext).append("\n");
            sb.append("请保持对话连贯性，不要重复之前说过的话。\n");
        }

        // ========== 核心要求 ==========
        sb.append("""

            【核心回答要求 - 必须遵守】
            1. **优先检查过敏和忌口**：如果用户询问的食物在过敏/忌口列表中，必须明确警告并推荐替代品
            2. **使用真实食材数据**：如果上面有【用户询问的食材】，必须基于真实数据回答（属性、功效、禁忌）
            3. **结合体质**：根据用户的中医体质给出个性化建议
            4. **结合节气**：推荐食物时考虑当前节气
            5. **结合饮食记录**：根据用户最近的饮食给出改善建议
            6. **多轮对话连贯**：记住上一轮聊的内容
            7. **回答格式**：亲切自然，用"您"称呼，可以适当使用表情符号
            8. **实用建议**：给出具体可操作的建议，如具体食物名称、简单做法
            9. **避免重复**：不要重复之前已经给过的建议
            
            请开始回答：""");

        return sb.toString();
    }

    /**
     * 判断食物对体质的适宜性
     */
    private int getSuitabilityScoreForFood(IngredientInfo info, String constitution) {
        if (info == null || info.getProperty() == null) return 0;

        Map<String, Map<String, Integer>> scoreMap = Map.of(
                "寒", Map.of("阳虚质", -10, "阴虚质", 5, "湿热质", 5, "气虚质", -5),
                "凉", Map.of("阳虚质", -5, "阴虚质", 3, "湿热质", 3, "气虚质", -3),
                "平", Map.of("阳虚质", 0, "阴虚质", 0, "湿热质", 0, "气虚质", 5),
                "温", Map.of("阳虚质", 10, "阴虚质", -5, "湿热质", -3, "气虚质", 8),
                "热", Map.of("阳虚质", 15, "阴虚质", -10, "湿热质", -8, "气虚质", 5)
        );

        Map<String, Integer> scores = scoreMap.getOrDefault(info.getProperty(), Map.of());
        return scores.getOrDefault(constitution, 0);
    }

    private String getMealName(String mealType) {
        switch (mealType) {
            case "BREAKFAST": return "早餐";
            case "LUNCH": return "午餐";
            case "DINNER": return "晚餐";
            case "SNACK": return "加餐";
            default: return mealType;
        }
    }

    /**
     * 提取对话上下文
     */
    private String extractContextFromConversation(String userMsg, String aiReply) {
        if (userMsg == null) return "";
        if (userMsg.length() > 50) {
            return userMsg.substring(0, 50) + "...";
        }
        return userMsg;
    }

    private void saveChat(String userId, String role, String content) {
        try {
            ChatHistory chat = new ChatHistory();
            chat.setUserId(userId);
            chat.setRole(role);
            chat.setContent(content);
            chat.setCreatedAt(LocalDateTime.now());
            chatHistoryMapper.insert(chat);
        } catch (Exception e) {
            log.error("保存聊天记录失败: {}", e.getMessage());
        }
    }

    public List<ChatHistory> getChatHistory(String userId, int limit) {
        return chatHistoryMapper.getByUserId(userId, limit);
    }
}