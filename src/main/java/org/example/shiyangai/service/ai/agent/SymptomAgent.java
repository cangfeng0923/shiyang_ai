// SymptomAgent.java
package org.example.shiyangai.service.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SymptomAgent extends BaseAgent {

    private static final java.util.Map<String, String> SYMPTOM_ADVICE = java.util.Map.of(
            "上火", "建议：多喝水，少吃辛辣，可食用绿豆、梨、苦瓜",
            "便秘", "建议：增加膳食纤维，多喝水，食用火龙果、香蕉",
            "失眠", "建议：睡前泡脚，避免熬夜，可食用百合、莲子",
            "疲劳", "建议：适度运动，保证睡眠，可食用山药、红枣"
    );

    public SymptomAgent() {
        super("SymptomAgent");
    }

    @Override
    public int getPriority() {
        return 45;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return ctx.getSymptom() != null && !ctx.getSymptom().isEmpty();
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String symptom = ctx.getSymptom();
        String advice = SYMPTOM_ADVICE.getOrDefault(symptom, "建议咨询专业医师");

        // 将症状分析结果存入上下文，供 Prompt 使用
        ctx.setSymptomAdvice(advice);

        log.info("Symptom advice: {} -> {}", symptom, advice);
        logEnd(ctx);
        return true;
    }
}