package org.example.shiyangai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

@Service
public class ReActAgent {

    @Autowired
    private List<ToolExecutor> tools;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ReActAgent() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * ReAct 核心执行循环
     */
    public String execute(String userQuestion, String constitution) throws Exception {
        System.out.println("=== Agent开始思考 ===");
        System.out.println("用户问题: " + userQuestion);
        System.out.println("用户体质: " + constitution);

        String thought = "";
        String action = "";
        String observation = "";
        StringBuilder history = new StringBuilder();
        int maxSteps = 5;

        for (int step = 1; step <= maxSteps; step++) {
            System.out.println("\n--- Step " + step + " ---");

            // 1. Thought: 让LLM思考下一步
            thought = think(userQuestion, constitution, history.toString(), getToolsDescription());
            System.out.println("Thought: " + thought);

            // 2. Action: 解析LLM决定的操作
            action = parseAction(thought);

            if (action == null || action.equals("Final Answer")) {
                // 直接返回最终答案
                String finalAnswer = extractFinalAnswer(thought);
                System.out.println("Final Answer: " + finalAnswer);
                return finalAnswer;
            }

            // 3. Observation: 执行工具
            observation = executeAction(action, constitution);
            System.out.println("Observation: " + observation);

            // 记录历史
            history.append("Step ").append(step).append(":\n");
            history.append("Action: ").append(action).append("\n");
            history.append("Observation: ").append(observation).append("\n");
        }

        return generateFinalAnswer(userQuestion, constitution, history.toString());
    }

    /**
     * 调用LLM进行思考
     */
    private String think(String question, String constitution, String history, String toolsDesc) throws Exception {
        String systemPrompt = buildReActPrompt(constitution, toolsDesc);

        String userPrompt = String.format("""
            用户问题: %s
            
            历史执行记录:
            %s
            
            请按照 Thought/Action/Action Input/Observation 格式继续思考。
            如果你已经有足够信息回答问题，请输出 Final Answer。
            """, question, history);

        return callLLM(systemPrompt, userPrompt);
    }

    /**
     * 构建 ReAct 提示词
     */
    private String buildReActPrompt(String constitution, String toolsDesc) {
        return String.format("""
            你是一个中医健康助手Agent，使用 ReAct 模式工作。
            
            用户体质: %s
            
            你可以使用以下工具:
            %s
            
            请严格按照以下格式输出:
            Thought: 思考下一步应该做什么
            Action: 要使用的工具名称
            Action Input: 工具的输入参数（JSON格式）
            
            或者如果你已经有足够信息回答用户问题:
            Thought: 我现在可以回答用户问题了
            Final Answer: 最终的回答内容
            
            规则:
            1. 每次只能使用一个工具
            2. 观察工具返回结果后再决定下一步
            3. 要结合用户体质给出个性化建议
            4. 最多进行5步推理
            """, constitution, toolsDesc);
    }

    /**
     * 解析 LLM 输出中的 Action
     */
    private String parseAction(String llmOutput) {
        Pattern actionPattern = Pattern.compile("Action:\\s*(\\w+)");
        Matcher matcher = actionPattern.matcher(llmOutput);

        if (matcher.find()) {
            return matcher.group(1);
        }

        if (llmOutput.contains("Final Answer")) {
            return "Final Answer";
        }

        return null;
    }

    /**
     * 执行工具调用
     */
    private String executeAction(String actionName, String constitution) throws Exception {
        for (ToolExecutor tool : tools) {
            if (tool.getName().equals(actionName)) {
                // 构造参数
                Map<String, Object> params = new HashMap<>();
                params.put("constitution", constitution);
                // 可以从 Action Input 解析更多参数
                return tool.execute(params);
            }
        }
        return "错误: 未找到工具 '" + actionName + "'";
    }

    /**
     * 提取最终答案
     */
    private String extractFinalAnswer(String llmOutput) {
        Pattern finalPattern = Pattern.compile("Final Answer:\\s*(.*)", Pattern.DOTALL);
        Matcher matcher = finalPattern.matcher(llmOutput);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return llmOutput;
    }

    /**
     * 生成最终答案
     */
    private String generateFinalAnswer(String question, String constitution, String history) throws Exception {
        String prompt = String.format("""
            基于以下信息，回答用户的问题。
            
            用户问题: %s
            用户体质: %s
            执行历史: %s
            
            请给出简洁、实用的建议。
            """, question, constitution, history);

        return callLLM("你是中医健康助手", prompt);
    }

    /**
     * 获取工具描述
     */
    private String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        for (ToolExecutor tool : tools) {
            sb.append("- ").append(tool.getName()).append(": ")
                    .append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 调用 DeepSeek API
     */
    private String callLLM(String systemPrompt, String userPrompt) throws Exception {
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
        requestBody.put("max_tokens", 1000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + System.getenv("DEEPSEEK_API_KEY"))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var jsonNode = objectMapper.readTree(response.body());

        return jsonNode.path("choices").path(0).path("message").path("content").asText();
    }
}