// AgentWorkflow.java（修复版）
package org.example.shiyangai.service.ai.workflow;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.agent.Agent;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Agent 工作流引擎 - 负责任务编排
 *
 * 标准流程：
 * 1. preProcess() - 执行预处理 Agents
 * 2. LLM 调用（外部）
 * 3. postProcess() - 执行后处理 Agents
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentWorkflow {

    private final List<Agent> agents;
    private List<Agent> sortedPreProcessAgents;
    private List<Agent> sortedPostProcessAgents;

    @PostConstruct
    public void init() {
        sortedPreProcessAgents = agents.stream()
                .filter(a -> !a.isPostProcess())
                .sorted(Comparator.comparingInt(Agent::getPriority))
                .toList();

        sortedPostProcessAgents = agents.stream()
                .filter(Agent::isPostProcess)
                .sorted(Comparator.comparingInt(Agent::getPriority))
                .toList();

        log.info("AgentWorkflow initialized: {} pre-process, {} post-process agents",
                sortedPreProcessAgents.size(), sortedPostProcessAgents.size());
    }

    /**
     * 执行预处理阶段（数据加载、意图识别、风险检测、Prompt 组装等）
     * @param ctx 对话上下文
     * @return 是否继续（false 表示请求中断）
     */
    public boolean preProcess(ConversationContext ctx) {
        log.debug("Starting pre-process phase");
        boolean shouldContinue = executeAgents(ctx, sortedPreProcessAgents);
        log.debug("Pre-process phase completed, continue={}", shouldContinue);
        return shouldContinue;
    }

    /**
     * 执行后处理阶段（保存历史、缓存更新、异步任务等）
     * @param ctx 对话上下文
     */
    public void postProcess(ConversationContext ctx) {
        log.debug("Starting post-process phase");
        executeAgents(ctx, sortedPostProcessAgents);
        log.debug("Post-process phase completed");
    }

    /**
     * [兼容旧代码] 执行所有 Agent（不区分前后）
     * @deprecated 请使用 preProcess() + postProcess()
     */
    @Deprecated
    public boolean execute(ConversationContext ctx) {
        boolean continueFlow = executeAgents(ctx, sortedPreProcessAgents);
        executeAgents(ctx, sortedPostProcessAgents);
        return continueFlow;
    }

    private boolean executeAgents(ConversationContext ctx, List<Agent> agentList) {
        for (Agent agent : agentList) {
            if (agent.supports(ctx)) {
                log.debug("Executing agent: {}", agent.getName());
                boolean shouldContinue = agent.execute(ctx);
                ctx.addExecutedAgent(agent.getName());
                if (!shouldContinue) {
                    log.debug("Agent {} requested stop", agent.getName());
                    return false;
                }
            } else {
                log.debug("Agent {} not supported, skipping", agent.getName());
            }
        }
        return true;
    }
}