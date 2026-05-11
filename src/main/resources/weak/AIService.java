package weak;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.mapper.ChatHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Autowired
    private UserService userService;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool();
    }

    public SseEmitter chatStream(String userId, String userMessage, String constitution) {
        SseEmitter emitter = new SseEmitter(120000L);

        executorService.execute(() -> {
            try {
                streamChat(userId, userMessage, constitution, emitter);
            } catch (Exception e) {
                log.error("流式对话异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("服务异常：" + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    private void streamChat(String userId, String userMessage, String constitution, SseEmitter emitter) throws Exception {
        log.info("AI流式对话: userId={}, message={}, constitution={}", userId, userMessage, constitution);

        HealthProfile profile = healthProfileService.getProfile(userId);
        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
        List<DietRecord> weekRecords = dietRecordService.getWeekRecords(userId);

        String solarTermAdvice = dynamicSolarTermService.getDynamicSolarTermAdvice(constitution);
        List<ChatHistory> history = chatHistoryMapper.getByUserId(userId, 6);

        String foodName = extractFoodName(userMessage);
        IngredientInfo foodInfo = null;

        if (foodName != null && !foodName.isEmpty()) {
            Object cached = redisCacheService.getFoodInfo(foodName);
            if (cached instanceof IngredientInfo) {
                foodInfo = (IngredientInfo) cached;
                log.info("从缓存获取食材: {}", foodName);
            } else {
                try {
                    foodInfo = baiduBaikeService.getIngredientInfo(foodName);
                    if (foodInfo != null) {
                        redisCacheService.cacheFoodInfo(foodName, foodInfo);
                    }
                } catch (Exception e) {
                    log.warn("百度百科获取失败: {}", e.getMessage());
                    foodInfo = onDemandFoodService.getFoodInfo(foodName);
                }
            }

            if (foodInfo != null) {
                saveFoodHistoryAsync(userId, foodName, constitution, foodInfo);
            }
        }

        String symptom = extractSymptom(userMessage);

        if (isAskingForReport(userMessage)) {
            String report = healthReportService.generateComprehensiveReport(userId, constitution);
            emitter.send(SseEmitter.event().name("message").data(report));
            emitter.send(SseEmitter.event().name("complete").data(""));
            emitter.complete();
            return;
        }

        String previousContext = redisCacheService.getChatContext(userId);

        String systemPrompt = buildEnhancedSystemPrompt(
                userId, constitution, profile, todayRecords, weekRecords,
                solarTermAdvice, foodInfo, symptom, previousContext
        );

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

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

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        requestBody.put("stream", true);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            StringBuilder fullReply = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        try {
                            var jsonNode = objectMapper.readTree(data);
                            String content = jsonNode
                                    .path("choices")
                                    .path(0)
                                    .path("delta")
                                    .path("content")
                                    .asText();

                            if (!content.isEmpty()) {
                                fullReply.append(content);
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(content));
                            }
                        } catch (Exception e) {
                            log.warn("解析流式数据失败: {}", e.getMessage());
                        }
                    }
                }
            }

            String reply = fullReply.toString();

            saveChat(userId, "user", userMessage);
            saveChat(userId, "assistant", reply);
            redisCacheService.cacheChatContext(userId, extractContextFromConversation(userMessage, reply));

            emitter.send(SseEmitter.event().name("complete").data(""));
            emitter.complete();

            log.info("AI流式回复成功: userId={}, 回复长度={}", userId, reply.length());
        } else {
            String errorBody = new String(response.body().readAllBytes());
            log.error("API调用失败: status={}, body={}", response.statusCode(), errorBody);
            emitter.send(SseEmitter.event().name("error").data("AI服务暂时不可用"));
            emitter.complete();
        }
    }

    public String chat(String userId, String userMessage, String constitution) {
        log.info("AI对话: userId={}, message={}, constitution={}", userId, userMessage, constitution);

        try {
            HealthProfile profile = healthProfileService.getProfile(userId);
            List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
            List<DietRecord> weekRecords = dietRecordService.getWeekRecords(userId);

            String solarTermAdvice = dynamicSolarTermService.getDynamicSolarTermAdvice(constitution);
            List<ChatHistory> history = chatHistoryMapper.getByUserId(userId, 6);

            String foodName = extractFoodName(userMessage);
            IngredientInfo foodInfo = null;

            if (foodName != null && !foodName.isEmpty()) {
                Object cached = redisCacheService.getFoodInfo(foodName);
                if (cached instanceof IngredientInfo) {
                    foodInfo = (IngredientInfo) cached;
                } else {
                    try {
                        foodInfo = baiduBaikeService.getIngredientInfo(foodName);
                        if (foodInfo != null) {
                            redisCacheService.cacheFoodInfo(foodName, foodInfo);
                        }
                    } catch (Exception e) {
                        foodInfo = onDemandFoodService.getFoodInfo(foodName);
                    }
                }

                if (foodInfo != null) {
                    saveFoodHistory(userId, foodName, constitution, foodInfo);
                }
            }

            String symptom = extractSymptom(userMessage);
            if (isAskingForReport(userMessage)) {
                return healthReportService.generateComprehensiveReport(userId, constitution);
            }

            String previousContext = redisCacheService.getChatContext(userId);

            String systemPrompt = buildEnhancedSystemPrompt(
                    userId, constitution, profile, todayRecords, weekRecords,
                    solarTermAdvice, foodInfo, symptom, previousContext
            );

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

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

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

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

                saveChat(userId, "user", userMessage);
                saveChat(userId, "assistant", reply);
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

    private void saveFoodHistoryAsync(String userId, String foodName, String constitution, IngredientInfo foodInfo) {
        executorService.submit(() -> {
            try {
                String suitability = getSuitabilityText(foodInfo, constitution);
                String suggestion = buildSuggestion(foodInfo, constitution);

                Map<String, Object> nutrition = new HashMap<>();
                nutrition.put("property", foodInfo.getProperty());
                nutrition.put("flavor", foodInfo.getFlavor());
                nutrition.put("effect", foodInfo.getEffect());
                nutrition.put("meridian", foodInfo.getMeridian());
                String nutritionJson = objectMapper.writeValueAsString(nutrition);

                userService.saveFoodHistory(userId, foodName, constitution, suitability, suggestion, nutritionJson);
                log.info("已保存食材查询历史: userId={}, foodName={}, suitability={}", userId, foodName, suitability);
            } catch (Exception e) {
                log.error("保存食材历史失败: {}", e.getMessage());
            }
        });
    }

    private String extractFoodName(String message) {
        if (message == null) return null;
        String[] foodKeywords = {
                "玉米", "苹果", "梨", "香蕉", "西瓜", "葡萄", "草莓", "橙子",
                "鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "鸭肉", "羊肉", "虾",
                "豆腐", "豆浆", "牛奶", "酸奶", "山药", "红枣", "枸杞", "薏米",
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

    private boolean isAskingForReport(String message) {
        String lower = message.toLowerCase();
        return lower.contains("报告") || lower.contains("健康报告") ||
                lower.contains("饮食报告") || lower.contains("本周报告") ||
                lower.contains("健康总结");
    }

    private String buildEnhancedSystemPrompt(String userId, String constitution, HealthProfile profile,
                                             List<DietRecord> todayRecords, List<DietRecord> weekRecords,
                                             String solarTermAdvice, IngredientInfo foodInfo,
                                             String symptom, String previousContext) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一位资深的中医老专家，具有30年临床经验。你的回答要亲切自然，像长辈关心晚辈一样。\n\n");

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

        if (!weekRecords.isEmpty()) {
            sb.append("\n【用户近7日详细饮食记录】\n");
            Map<LocalDate, List<DietRecord>> recordsByDay = weekRecords.stream()
                    .collect(Collectors.groupingBy(r -> r.getRecordDate().toLocalDate()));

            for (Map.Entry<LocalDate, List<DietRecord>> entry : recordsByDay.entrySet()) {
                sb.append(entry.getKey().toString()).append("：\n");
                for (DietRecord record : entry.getValue()) {
                    String mealName = getMealName(record.getMealType());
                    sb.append(String.format("  - %s：%s %sg（评分：%d/100）\n",
                            mealName, record.getFoodName(),
                            record.getGrams() != null ? record.getGrams() : 0,
                            record.getHealthScore()));
                }
            }

            double avgScore = weekRecords.stream().mapToInt(DietRecord::getHealthScore).average().orElse(0);
            sb.append(String.format("\n【饮食分析】平均健康评分：%.0f/100\n", avgScore));

            boolean hasBreakfast = weekRecords.stream().anyMatch(r -> "BREAKFAST".equals(r.getMealType()));
            if (!hasBreakfast) {
                sb.append("⚠️ 用户本周没有记录早餐，可能有不规律吃早餐的习惯\n");
            }
        }

        sb.append("\n【当前节气养生参考】\n");
        sb.append(solarTermAdvice).append("\n");

        if (foodInfo != null) {
            sb.append("\n【用户询问的食材 - 真实数据（必须基于此回答！）】\n");
            sb.append("- 食材名称：").append(foodInfo.getName()).append("\n");
            sb.append("- 中医属性：").append(foodInfo.getProperty() != null ? foodInfo.getProperty() : "平").append("性\n");
            sb.append("- 味道：").append(foodInfo.getFlavor() != null ? foodInfo.getFlavor() : "甘").append("味\n");
            sb.append("- 归经：").append(foodInfo.getMeridian() != null ? foodInfo.getMeridian() : "脾、胃").append("\n");
            sb.append("- 主要功效：").append(foodInfo.getEffect() != null ? foodInfo.getEffect() : "补益身体").append("\n");
            sb.append("- 禁忌人群：").append(foodInfo.getContraindication() != null ? foodInfo.getContraindication() : "无明显禁忌").append("\n");

            int suitabilityScore = getSuitabilityScoreForFood(foodInfo, constitution);
            if (suitabilityScore > 0) {
                sb.append("- 对").append(constitution).append("体质：✅ 适合食用\n");
            } else if (suitabilityScore < 0) {
                sb.append("- 对").append(constitution).append("体质：❌ 不太适合，需要谨慎或少量食用\n");
            } else {
                sb.append("- 对").append(constitution).append("体质：⚖️ 中性，可适量食用\n");
            }
        }

        if (!symptom.isEmpty()) {
            sb.append("\n【用户当前症状】").append(symptom).append("\n");
            sb.append("请根据症状给出调理建议，推荐适合的食疗方案。\n");
        }

        if (previousContext != null && !previousContext.isEmpty()) {
            sb.append("\n【上一轮对话摘要】").append(previousContext).append("\n");
            sb.append("请保持对话连贯性，不要重复之前说过的话。\n");
        }

        sb.append("""

            【核心回答要求 - 必须遵守】
            1. **优先检查过敏和忌口**：如果用户询问的食物在过敏/忌口列表中，必须明确警告并推荐替代品
            2. **使用真实食材数据**：如果上面有【用户询问的食材】，必须基于真实数据回答
            3. **结合体质**：根据用户的中医体质给出个性化建议
            4. **结合节气**：推荐食物时考虑当前节气
            5. **结合饮食记录**：根据用户最近的饮食给出改善建议
            6. **多轮对话连贯**：记住上一轮聊的内容
            7. **回答格式**：亲切自然，用"您"称呼，可以适当使用表情符号
            
            请开始回答：""");

        return sb.toString();
    }

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

    private void saveFoodHistory(String userId, String foodName, String constitution, IngredientInfo foodInfo) {
        try {
            String suitability = getSuitabilityText(foodInfo, constitution);
            String suggestion = buildSuggestion(foodInfo, constitution);

            Map<String, Object> nutrition = new HashMap<>();
            nutrition.put("property", foodInfo.getProperty());
            nutrition.put("flavor", foodInfo.getFlavor());
            nutrition.put("effect", foodInfo.getEffect());
            nutrition.put("meridian", foodInfo.getMeridian());
            String nutritionJson = objectMapper.writeValueAsString(nutrition);

            userService.saveFoodHistory(userId, foodName, constitution, suitability, suggestion, nutritionJson);
            log.info("已保存食材查询历史: userId={}, foodName={}, suitability={}", userId, foodName, suitability);
        } catch (Exception e) {
            log.error("保存食材历史失败: {}", e.getMessage());
        }
    }

    private String getSuitabilityText(IngredientInfo foodInfo, String constitution) {
        int score = getSuitabilityScoreForFood(foodInfo, constitution);
        if (score > 0) return "适合";
        else if (score < 0) return "不适合";
        else return "慎食";
    }

    private String buildSuggestion(IngredientInfo foodInfo, String constitution) {
        String property = foodInfo.getProperty() != null ? foodInfo.getProperty() : "平";
        String effect = foodInfo.getEffect() != null ? foodInfo.getEffect() : "";

        if ("寒".equals(property) || "凉".equals(property)) {
            if ("阳虚质".equals(constitution)) {
                return "您为阳虚体质，此食物性偏寒凉，建议少食或搭配温性食材食用";
            }
        } else if ("温".equals(property) || "热".equals(property)) {
            if ("阴虚质".equals(constitution) || "湿热质".equals(constitution)) {
                return "您为" + constitution + "，此食物性偏温热，建议适量食用，避免上火";
            }
        }

        return effect.isEmpty() ? "适量食用" : effect;
    }

    public String generateSolarTermAdvice(String termName, String termClimate, String constitution,
                                          WeatherService.WeatherInfo weather) {
        log.info("AI生成节气建议: term={}, constitution={}, city={}", termName, constitution, weather.getCity());

        try {
            String systemPrompt = buildSolarTermSystemPrompt();
            String userPrompt = buildSolarTermUserPrompt(termName, termClimate, constitution, weather);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 800);
            requestBody.put("stream", false);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var jsonNode = objectMapper.readTree(response.body());
                return jsonNode.path("choices").path(0).path("message").path("content").asText();
            } else {
                log.error("AI调用失败: {}", response.statusCode());
                return getDefaultSolarTermAdvice(termName, constitution);
            }

        } catch (Exception e) {
            log.error("AI生成节气建议失败", e);
            return getDefaultSolarTermAdvice(termName, constitution);
        }
    }

    private String buildSolarTermSystemPrompt() {
        return """
        你是一位资深的中医养生专家，有30年临床经验。
        你的回答要亲切自然，像长辈关心晚辈一样。
        回答要简洁实用，不要啰嗦。
        直接输出内容，不要用markdown格式。
        """;
    }

    private String buildSolarTermUserPrompt(String termName, String termClimate, String constitution,
                                            WeatherService.WeatherInfo weather) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为用户生成一份个性化的节气养生建议。\n\n");
        sb.append("【当前节气】").append(termName).append("\n");
        sb.append("【节气特点】").append(termClimate).append("\n");
        sb.append("【用户体质】").append(constitution != null ? constitution : "平和质").append("\n");
        sb.append("【今日天气】").append(weather.getCity()).append("，");
        sb.append(weather.getTodayWeather()).append("，");
        sb.append(weather.getTodayTemp()).append("\n\n");

        sb.append("请按以下格式输出（每段用空行分隔）：\n");
        sb.append("🌿 养生原则：[简洁描述当前节气的养生核心]\n\n");
        sb.append("🍽️ 饮食建议：[推荐3-5种食物和简单理由]\n\n");
        sb.append("💤 起居运动：[生活作息和运动建议]\n\n");
        sb.append("💡 专属提醒：[针对用户体质的个性化提醒]\n\n");
        sb.append("📖 经典引用：[一句《黄帝内经》或中医经典原文]");

        return sb.toString();
    }

    private String getDefaultSolarTermAdvice(String termName, String constitution) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌿 养生原则：顺应时节，调和阴阳\n\n");
        sb.append("🍽️ 饮食建议：选择当季新鲜食材，饮食均衡\n\n");
        sb.append("💤 起居运动：规律作息，适度运动\n\n");
        sb.append("💡 专属提醒：").append(constitution).append("体质注意饮食调理\n\n");
        sb.append("📖 经典引用：《黄帝内经》：法于阴阳，和于术数，食饮有节，起居有常。");
        return sb.toString();
    }
}