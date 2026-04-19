// service/DynamicSolarTermService.java
package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.SolarTermAdvice;
import org.example.shiyangai.mapper.SolarTermAdviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class DynamicSolarTermService {

    @Autowired
    private SolarTermAdviceMapper solarTermAdviceMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Value("${deepseek.api.key}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // 24节气固定日期（2025年）
    private static final Map<String, LocalDate> SOLAR_TERM_DATES = new LinkedHashMap<>();

    static {
        SOLAR_TERM_DATES.put("立春", LocalDate.of(2025, 2, 3));
        SOLAR_TERM_DATES.put("雨水", LocalDate.of(2025, 2, 18));
        SOLAR_TERM_DATES.put("惊蛰", LocalDate.of(2025, 3, 5));
        SOLAR_TERM_DATES.put("春分", LocalDate.of(2025, 3, 20));
        SOLAR_TERM_DATES.put("清明", LocalDate.of(2025, 4, 4));
        SOLAR_TERM_DATES.put("谷雨", LocalDate.of(2025, 4, 20));
        SOLAR_TERM_DATES.put("立夏", LocalDate.of(2025, 5, 5));
        SOLAR_TERM_DATES.put("小满", LocalDate.of(2025, 5, 21));
        SOLAR_TERM_DATES.put("芒种", LocalDate.of(2025, 6, 5));
        SOLAR_TERM_DATES.put("夏至", LocalDate.of(2025, 6, 21));
        SOLAR_TERM_DATES.put("小暑", LocalDate.of(2025, 7, 7));
        SOLAR_TERM_DATES.put("大暑", LocalDate.of(2025, 7, 22));
        SOLAR_TERM_DATES.put("立秋", LocalDate.of(2025, 8, 7));
        SOLAR_TERM_DATES.put("处暑", LocalDate.of(2025, 8, 23));
        SOLAR_TERM_DATES.put("白露", LocalDate.of(2025, 9, 7));
        SOLAR_TERM_DATES.put("秋分", LocalDate.of(2025, 9, 23));
        SOLAR_TERM_DATES.put("寒露", LocalDate.of(2025, 10, 8));
        SOLAR_TERM_DATES.put("霜降", LocalDate.of(2025, 10, 23));
        SOLAR_TERM_DATES.put("立冬", LocalDate.of(2025, 11, 7));
        SOLAR_TERM_DATES.put("小雪", LocalDate.of(2025, 11, 22));
        SOLAR_TERM_DATES.put("大雪", LocalDate.of(2025, 12, 7));
        SOLAR_TERM_DATES.put("冬至", LocalDate.of(2025, 12, 21));
        SOLAR_TERM_DATES.put("小寒", LocalDate.of(2026, 1, 5));
        SOLAR_TERM_DATES.put("大寒", LocalDate.of(2026, 1, 20));
    }

    public DynamicSolarTermService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取当前节气名称
     */
    public String getCurrentSolarTermName() {
        LocalDate today = LocalDate.now();
        String currentTerm = "立春";
        LocalDate currentDate = null;

        for (Map.Entry<String, LocalDate> entry : SOLAR_TERM_DATES.entrySet()) {
            LocalDate termDate = entry.getValue();
            if (!termDate.isAfter(today)) {
                if (currentDate == null || termDate.isAfter(currentDate)) {
                    currentDate = termDate;
                    currentTerm = entry.getKey();
                }
            }
        }
        return currentTerm;
    }

    /**
     * 获取下一个节气
     */
    public String getNextSolarTermName() {
        String current = getCurrentSolarTermName();
        boolean found = false;
        for (Map.Entry<String, LocalDate> entry : SOLAR_TERM_DATES.entrySet()) {
            if (found) return entry.getKey();
            if (entry.getKey().equals(current)) found = true;
        }
        return "立春";
    }

    /**
     * 获取动态节气养生建议（AI生成或缓存）
     */
    public String getDynamicSolarTermAdvice(String constitution) {
        String termName = getCurrentSolarTermName();
        String cacheKey = termName + ":" + constitution;

        // 1. 先查Redis缓存
        Object cached = redisCacheService.getSolarTerm(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取节气建议: {}", cacheKey);
            return cached.toString();
        }

        // 2. 查数据库
        SolarTermAdvice dbAdvice = solarTermAdviceMapper.findByTermAndConstitution(termName, constitution);
        if (dbAdvice != null) {
            String advice = formatSolarTermAdvice(dbAdvice);
            redisCacheService.cacheSolarTerm(cacheKey, advice);
            return advice;
        }

        // 3. 调用AI动态生成
        log.info("AI生成节气建议: term={}, constitution={}", termName, constitution);
        String aiAdvice = generateAdviceByAI(termName, constitution);

        // 4. 保存到缓存
        if (aiAdvice != null && !aiAdvice.isEmpty()) {
            redisCacheService.cacheSolarTerm(cacheKey, aiAdvice);
        }

        return aiAdvice != null ? aiAdvice : getFallbackAdvice(termName, constitution);
    }

    /**
     * 调用AI生成节气养生建议
     */
    private String generateAdviceByAI(String termName, String constitution) {
        try {
            String prompt = String.format("""
                请为%s节气生成一份详细的养生指南，用户体质是%s。
                
                请按以下格式返回（不要有多余内容）：
                
                【节气特点】
                描述这个节气的气候特点和人体变化。
                
                【养生原则】
                核心养生原则，一句话概括。
                
                【饮食建议】
                推荐吃的食物、原因，以及不推荐的食物。
                
                【推荐食谱】
                1-2个具体食谱，包含食材和简单做法。
                
                【起居建议】
                作息、睡眠、穿衣方面的建议。
                
                【运动建议】
                适合这个节气的运动方式和注意事项。
                
                【体质特别提醒】
                针对%s体质的特别注意事项和养生重点。
                
                要求：专业、实用、亲切，用中文回答。
                """, termName, constitution, constitution);

            String jsonBody = String.format("""
                {
                    "model": "deepseek-chat",
                    "messages": [
                        {"role": "system", "content": "你是一位资深中医养生专家，精通24节气养生和九大体质调理。"},
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.7,
                    "max_tokens": 1200
                }
                """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var json = objectMapper.readTree(response.body());
                String advice = json.path("choices").path(0).path("message").path("content").asText();
                log.info("AI生成节气建议成功: term={}", termName);

                // 异步保存到数据库
                saveToDatabaseAsync(termName, constitution, advice);

                return advice;
            } else {
                log.error("AI生成节气建议失败: status={}", response.statusCode());
            }
        } catch (Exception e) {
            log.error("AI生成节气建议异常", e);
        }
        return null;
    }

    /**
     * 格式化数据库节气建议
     */
    private String formatSolarTermAdvice(SolarTermAdvice advice) {
        StringBuilder sb = new StringBuilder();
        sb.append("📅 **").append(advice.getSolarTermName()).append("养生指南**\n\n");

        if (advice.getPrinciple() != null && !advice.getPrinciple().isEmpty()) {
            sb.append("🌿 **养生原则**：").append(advice.getPrinciple()).append("\n\n");
        }

        if (advice.getFoodAdvice() != null && !advice.getFoodAdvice().isEmpty()) {
            sb.append("🍽️ **饮食建议**：").append(advice.getFoodAdvice()).append("\n\n");
        }

        if (advice.getRecipe() != null && !advice.getRecipe().isEmpty()) {
            sb.append("🍲 **推荐食谱**：").append(advice.getRecipe()).append("\n\n");
        }

        if (advice.getDailyAdvice() != null && !advice.getDailyAdvice().isEmpty()) {
            sb.append("💤 **起居建议**：").append(advice.getDailyAdvice()).append("\n\n");
        }

        if (advice.getExerciseAdvice() != null && !advice.getExerciseAdvice().isEmpty()) {
            sb.append("🏃 **运动建议**：").append(advice.getExerciseAdvice()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 异步保存到数据库
     */
    private void saveToDatabaseAsync(String termName, String constitution, String advice) {
        try {
            SolarTermAdvice entity = new SolarTermAdvice();
            entity.setSolarTermName(termName);
            entity.setConstitution(constitution);
            entity.setPrinciple(extractSection(advice, "养生原则"));
            entity.setFoodAdvice(extractSection(advice, "饮食建议"));
            entity.setRecipe(extractSection(advice, "推荐食谱"));
            entity.setDailyAdvice(extractSection(advice, "起居建议"));
            entity.setExerciseAdvice(extractSection(advice, "运动建议"));
            entity.setYear(LocalDate.now().getYear());
            entity.setStartDate(SOLAR_TERM_DATES.get(termName));
            entity.setEndDate(getEndDateForTerm(termName));

            // 检查是否已存在
            SolarTermAdvice existing = solarTermAdviceMapper.findByTermAndConstitution(termName, constitution);
            if (existing == null) {
                solarTermAdviceMapper.insert(entity);
                log.info("保存节气建议到数据库: term={}, constitution={}", termName, constitution);
            }
        } catch (Exception e) {
            log.warn("保存节气建议到数据库失败: {}", e.getMessage());
        }
    }

    /**
     * 获取节气结束日期
     */
    private LocalDate getEndDateForTerm(String termName) {
        boolean found = false;
        for (Map.Entry<String, LocalDate> entry : SOLAR_TERM_DATES.entrySet()) {
            if (found) return entry.getValue().minusDays(1);
            if (entry.getKey().equals(termName)) found = true;
        }
        return SOLAR_TERM_DATES.get("立春").minusDays(1);
    }

    /**
     * 从文本中提取指定章节
     */
    private String extractSection(String text, String sectionName) {
        if (text == null) return "信息待补充";
        String pattern = "【" + sectionName + "】[：:]?\\s*([^【]+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            String result = m.group(1).trim();
            if (result.length() > 500) {
                result = result.substring(0, 500);
            }
            return result;
        }
        return "信息待补充";
    }

    /**
     * 降级方案
     */
    private String getFallbackAdvice(String termName, String constitution) {
        Map<String, String[]> seasonAdvice = new HashMap<>();
        seasonAdvice.put("立春", new String[]{"春季养肝", "多吃绿色蔬菜，少吃酸味食物", "枸杞猪肝汤"});
        seasonAdvice.put("立夏", new String[]{"夏季养心", "多吃苦味食物，午休养心", "绿豆百合汤"});
        seasonAdvice.put("立秋", new String[]{"秋季养肺", "多吃白色食物，滋阴润燥", "银耳雪梨汤"});
        seasonAdvice.put("立冬", new String[]{"冬季养肾", "多吃温热食物，早睡晚起", "当归生姜羊肉汤"});

        String[] advice = seasonAdvice.getOrDefault(termName, new String[]{"顺应节气", "均衡饮食", "养生汤"});

        StringBuilder sb = new StringBuilder();
        sb.append("📅 **").append(termName).append("养生指南**\n\n");
        sb.append("🌿 **养生原则**：").append(advice[0]).append("\n\n");
        sb.append("🍽️ **饮食建议**：").append(advice[1]).append("\n\n");
        sb.append("🍲 **推荐食谱**：").append(advice[2]).append("\n\n");
        sb.append("💡 **体质提醒**：").append(constitution).append("质者，请根据自身体质调整饮食\n");

        return sb.toString();
    }

    /**
     * 获取所有节气列表
     */
    public List<String> getAllSolarTerms() {
        return new ArrayList<>(SOLAR_TERM_DATES.keySet());
    }
}