// RuleBasedAgentRouter.java（完整版 - 添加 CoordinatorAgent）
package org.example.shiyangai.service.ai.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.agent.*;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedAgentRouter implements AgentRouter {

    // 基础必备 Agents
    private final UserDataAgent userDataAgent;
    private final MemoryAgent memoryAgent;
    private final IntentAgent intentAgent;
    private final PromptAssemblerAgent promptAssemblerAgent;
    private final PostProcessAgent postProcessAgent;

    // 新增 Agents
    private final RiskControlAgent riskControlAgent;
    private final CoordinatorAgent coordinatorAgent;

    // 可选 Agents
    private final IngredientAgent ingredientAgent;
    private final ReportAgent reportAgent;
    private final SymptomAgent symptomAgent;

    @Override
    public List<Agent> route(ConversationContext ctx) {
        List<Agent> agents = new ArrayList<>();

        String message = ctx.getUserMessage();

        // ========== 1. 基础必备 Agents（所有请求都执行） ==========
        agents.add(userDataAgent);      // priority 10 - 加载用户数据
        agents.add(memoryAgent);        // priority 20 - 加载历史记忆
        agents.add(intentAgent);        // priority 30 - 识别意图

        // ========== 2. 报告请求（短路，不需要后续 Agent） ==========
        if (ctx.isAskingForReport()) {
            log.info("Router: 报告请求，添加 ReportAgent");
            agents.add(reportAgent);            // priority 50 - 生成报告
            agents.add(postProcessAgent);       // priority 1000 - 后处理
            return agents;
        }

        // ========== 3. 食材查询请求 ==========
        if (containsFoodKeyword(message)) {
            log.info("Router: 食材查询请求，添加 IngredientAgent 和 RiskControlAgent");
            agents.add(ingredientAgent);        // priority 40 - 查询食材信息
            agents.add(riskControlAgent);       // priority 42 - 风险检测
        }

        // ========== 4. 症状咨询请求 ==========
        if (containsSymptomKeyword(message)) {
            log.info("Router: 症状咨询请求，添加 SymptomAgent");
            agents.add(symptomAgent);           // priority 45 - 症状分析
        }

        // ========== 5. 最终汇总 Agents（所有非短路请求都执行） ==========
        agents.add(coordinatorAgent);           // priority 90 - 汇总所有 Agent 决策结果
        agents.add(promptAssemblerAgent);       // priority 80 - 组装 Prompt
        agents.add(postProcessAgent);           // priority 1000 - 后处理保存

        log.info("Router 选择了 {} 个 Agents: {}", agents.size(),
                agents.stream().map(Agent::getName).toList());

        return agents;
    }

    /**
     * 判断是否包含食物关键词
     */
    private boolean containsFoodKeyword(String message) {
        if (message == null) return false;
        String[] keywords = {
                "花生", "花生酱", "花生蛋糕",
                "苹果", "香蕉", "西瓜", "葡萄", "草莓", "橙子", "梨",
                "鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "鸭肉", "羊肉", "虾",
                "豆腐", "豆浆", "牛奶", "酸奶", "山药", "红枣", "枸杞", "薏米",
                "火锅", "烧烤", "麻辣烫", "奶茶", "炸鸡", "冰淇淋", "芒果"
        };
        for (String kw : keywords) {
            if (message.contains(kw)) {
                log.debug("containsFoodKeyword: 匹配到关键词 '{}'", kw);
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否包含症状关键词
     */
    private boolean containsSymptomKeyword(String message) {
        if (message == null) return false;
        String[] keywords = {"上火", "便秘", "失眠", "疲劳", "怕冷", "口干", "胃痛", "咳嗽", "长痘", "头晕"};
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }
}