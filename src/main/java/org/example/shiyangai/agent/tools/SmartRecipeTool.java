package org.example.shiyangai.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shiyangai.agent.Tool;
import org.example.shiyangai.agent.ToolExecutor;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
public class SmartRecipeTool implements ToolExecutor {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SmartRecipeTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Tool(name = "recommend_recipe",
            description = "根据体质、症状、场景智能推荐食谱",
            parameters = {"constitution", "symptom", "scenario", "mealType"})
    public String execute(Map<String, Object> parameters) {
        String constitution = (String) parameters.getOrDefault("constitution", "平和质");
        String symptom = (String) parameters.getOrDefault("symptom", "");
        String scenario = (String) parameters.getOrDefault("scenario", "");
        String mealType = (String) parameters.getOrDefault("mealType", "全天");

        return generateRecipeWithAI(constitution, symptom, scenario, mealType);
    }

    /**
     * AI 动态生成食谱 - 不写任何固定答案
     */
    private String generateRecipeWithAI(String constitution, String symptom,
                                        String scenario, String mealType) {

        // 不再写死 analyzeSymptoms 和 getScenarioAdvice
        // 直接把原始信息传给 AI，让 AI 自己推理

        String prompt = String.format("""
            你是一位资深中医食疗专家。

            【用户信息】
            - 体质：%s
            - 症状描述：%s
            - 生活场景：%s（请根据这个场景推荐方便获取的食物）
            - 需求：%s

            【重要规则】
            1. 不要使用任何固定模板，根据实际情况灵活回答
            2. 如果你不知道某种食材的属性，请基于中医常识推理
            3. 考虑场景限制：如果是学校食堂，推荐食堂能买到的食物；如果是宿舍，推荐不用烹饪的食物
            4. 每推荐一种食物，都要简单说明为什么适合这个症状和体质
            5. 回答要亲切自然，用"您"称呼用户

            请开始回答：
            """,
                constitution,
                symptom.isEmpty() ? "无明显症状，日常养生" : symptom,
                scenario.isEmpty() ? "无特殊限制" : scenario,
                mealType.equals("全天") ? "设计全天的饮食方案" : "推荐" + mealType + "的食物"
        );

        return callDeepSeekAPI(prompt);
    }

    /**
     * 调用 DeepSeek API
     */
    private String callDeepSeekAPI(String prompt) {
        try {
            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey == null) apiKey = "你的API_KEY";

            String jsonBody = String.format("""
                {
                    "model": "deepseek-chat",
                    "messages": [
                        {"role": "system", "content": "你是一个中医食疗专家。你没有任何固定模板，每次都要根据用户的具体情况动态生成个性化建议。"},
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.8,
                    "max_tokens": 1500
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
                return json.path("choices").path(0).path("message").path("content").asText();
            } else {
                return "抱歉，AI服务暂时不可用。建议您：多吃蔬菜水果，少吃辛辣油腻，保持作息规律。";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，AI服务暂时不可用。建议您：多吃蔬菜水果，少吃辛辣油腻，保持作息规律。";
        }
    }

    @Override
    public String getName() { return "recommend_recipe"; }

    @Override
    public String getDescription() {
        return "智能食谱推荐工具。根据体质、症状、场景动态生成个性化饮食方案。";
    }
}