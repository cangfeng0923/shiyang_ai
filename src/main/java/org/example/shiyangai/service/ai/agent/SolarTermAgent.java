// SolarTermAgent.java
package org.example.shiyangai.service.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.WeatherService;
import org.example.shiyangai.service.ai.DeepSeekClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolarTermAgent {

    private final DeepSeekClient deepSeekClient;

    public String generateAdvice(String termName, String termClimate, String constitution,
                                 WeatherService.WeatherInfo weather) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(termName, termClimate, constitution, weather);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            return deepSeekClient.chatSync(messages, 0.7, 800);
        } catch (Exception e) {
            log.error("生成节气建议失败", e);
            return getDefaultAdvice(termName, constitution);
        }
    }

    private String buildSystemPrompt() {
        return """
        你是一位资深的中医养生专家，有30年临床经验。
        你的回答要亲切自然，像长辈关心晚辈一样。
        回答要简洁实用，不要啰嗦。
        直接输出内容，不要用markdown格式。
        """;
    }

    private String buildUserPrompt(String termName, String termClimate, String constitution,
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

    private String getDefaultAdvice(String termName, String constitution) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌿 养生原则：顺应时节，调和阴阳\n\n");
        sb.append("🍽️ 饮食建议：选择当季新鲜食材，饮食均衡\n\n");
        sb.append("💤 起居运动：规律作息，适度运动\n\n");
        sb.append("💡 专属提醒：").append(constitution).append("体质注意饮食调理\n\n");
        sb.append("📖 经典引用：《黄帝内经》：法于阴阳，和于术数，食饮有节，起居有常。");
        return sb.toString();
    }
}