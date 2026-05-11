// ReportAgent.java
package org.example.shiyangai.service.ai.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.HealthReportService;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportAgent extends BaseAgent {

    private final HealthReportService healthReportService;

    public ReportAgent(HealthReportService healthReportService) {
        super("ReportAgent");
        this.healthReportService = healthReportService;
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return ctx.isAskingForReport();
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String report = healthReportService.generateComprehensiveReport(
                ctx.getUserId(), ctx.getConstitution());
        ctx.setReply(report);
        ctx.setShortCircuit(true);  // 标记短路，跳过 LLM

        logEnd(ctx);
        return true;
    }
}