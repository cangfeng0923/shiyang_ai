// CoordinatorAgent.java
package org.example.shiyangai.service.ai.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.example.shiyangai.service.ai.agent.result.AgentResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class CoordinatorAgent extends BaseAgent {

    public CoordinatorAgent() {
        super("CoordinatorAgent");
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return !ctx.isShortCircuit() && ctx.getAgentResults() != null && !ctx.getAgentResults().isEmpty();
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        List<AgentResult> results = ctx.getAgentResults();

        // 按优先级排序
        results.sort(Comparator.comparingInt(AgentResult::getPriority));

        StringBuilder promptEnhancement = new StringBuilder();
        promptEnhancement.append("\n\n【AI 内部决策分析 - 请据此回答】\n\n");

        for (AgentResult result : results) {
            promptEnhancement.append("## ").append(result.getAgentName()).append("\n\n");

            if (!result.getObservations().isEmpty()) {
                promptEnhancement.append("**观察到的事实：**\n");
                for (String obs : result.getObservations()) {
                    promptEnhancement.append("- ").append(obs).append("\n");
                }
                promptEnhancement.append("\n");
            }

            if (!result.getAnalysis().isEmpty()) {
                promptEnhancement.append("**分析结论：**\n");
                for (String ana : result.getAnalysis()) {
                    promptEnhancement.append("- ").append(ana).append("\n");
                }
                promptEnhancement.append("\n");
            }

            if (!result.getRecommendations().isEmpty()) {
                promptEnhancement.append("**建议：**\n");
                for (String rec : result.getRecommendations()) {
                    promptEnhancement.append("- ").append(rec).append("\n");
                }
                promptEnhancement.append("\n");
            }
        }

        // 追加到 system prompt
        String originalPrompt = ctx.getSystemPrompt();
        if (originalPrompt != null) {
            ctx.setSystemPrompt(originalPrompt + promptEnhancement.toString());
        } else {
            ctx.setSystemPrompt(promptEnhancement.toString());
        }

        log.info("CoordinatorAgent merged {} agent results", results.size());
        logEnd(ctx);
        return true;
    }
}