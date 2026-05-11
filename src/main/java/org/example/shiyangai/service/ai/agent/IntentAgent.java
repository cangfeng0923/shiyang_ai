// IntentAgent.java
package org.example.shiyangai.service.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IntentAgent extends BaseAgent {

    public IntentAgent() {
        super("IntentAgent");
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String message = ctx.getUserMessage();
        if (message != null) {
            // 识别症状
            ctx.setSymptom(extractSymptom(message));
            // 识别是否为报告请求
            ctx.setAskingForReport(isAskingForReport(message));
        }

        logEnd(ctx);
        return true;
    }

    private String extractSymptom(String message) {
        if (message.contains("痘") || message.contains("痘痘")) return "长痘";
        if (message.contains("上火")) return "上火";
        if (message.contains("便秘")) return "便秘";
        if (message.contains("失眠") || message.contains("睡不好")) return "失眠";
        if (message.contains("疲劳") || message.contains("累")) return "疲劳";
        if (message.contains("怕冷") || message.contains("手脚冰凉")) return "怕冷";
        if (message.contains("口干") || message.contains("口渴")) return "口干";
        if (message.contains("胃痛") || message.contains("胃胀")) return "胃不适";
        if (message.contains("头痛") || message.contains("头晕")) return "头痛";
        if (message.contains("咳嗽")) return "咳嗽";
        return "";
    }

    private boolean isAskingForReport(String message) {
        String lower = message.toLowerCase();
        return lower.contains("报告") || lower.contains("健康报告") ||
                lower.contains("饮食报告") || lower.contains("本周报告") ||
                lower.contains("健康总结");
    }
}