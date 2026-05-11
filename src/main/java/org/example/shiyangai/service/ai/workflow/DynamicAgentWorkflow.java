// DynamicAgentWorkflow.java
package org.example.shiyangai.service.ai.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.MessageAssembler;
import org.example.shiyangai.service.ai.agent.Agent;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.example.shiyangai.service.ai.llm.LLMClient;
import org.example.shiyangai.service.ai.router.AgentRouter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicAgentWorkflow {

    private final AgentRouter agentRouter;
    private final LLMClient llmClient;
    private final MessageAssembler messageAssembler;

    public boolean execute(ConversationContext ctx) {
        // 1. 动态路由选择 Agents
        List<Agent> selectedAgents = agentRouter.route(ctx);
        log.info("Selected {} agents for execution", selectedAgents.size());

        // 2. 执行预处理 Agents
        for (Agent agent : selectedAgents) {
            if (!agent.isPostProcess() && agent.supports(ctx)) {
                log.debug("Executing: {}", agent.getName());
                agent.execute(ctx);
                ctx.addExecutedAgent(agent.getName());
            }
        }

        // 3. 短路检查
        if (ctx.isShortCircuit()) {
            log.info("Short-circuited, skipping LLM");
        }

        // 4. 执行后处理 Agents
        for (Agent agent : selectedAgents) {
            if (agent.isPostProcess() && agent.supports(ctx)) {
                log.debug("Executing post-process: {}", agent.getName());
                agent.execute(ctx);
            }
        }

        return true;
    }
}