// PromptAssemblerAgent.java（修复版 - 添加 Coordinator 集成）
package org.example.shiyangai.service.ai.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.example.shiyangai.service.ai.prompt.PromptFragment;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class PromptAssemblerAgent extends BaseAgent {

    private final List<PromptFragment> fragments;

    public PromptAssemblerAgent(List<PromptFragment> fragments) {
        super("PromptAssemblerAgent");
        this.fragments = fragments;
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return !ctx.isShortCircuit();
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        // 注意：如果 CoordinatorAgent 已经执行过，systemPrompt 已经包含了 Agent 决策分析
        // 这里只添加基础框架，不覆盖已有的内容
        String existingPrompt = ctx.getSystemPrompt();

        if (existingPrompt == null || existingPrompt.isEmpty()) {
            // 没有 Coordinator 结果，正常组装
            StringBuilder sb = new StringBuilder();

            sb.append("你是一位资深的中医老专家，具有30年临床经验。")
                    .append("你的回答要亲切自然，像长辈关心晚辈一样。\n\n");

            fragments.stream()
                    .sorted(Comparator.comparingInt(PromptFragment::getPriority))
                    .filter(f -> f.supports(ctx))
                    .forEach(f -> sb.append(f.getContent(ctx)));

            sb.append("""
                【回答要求】
                1. 用"您"称呼用户，回答亲切自然
                2. 结合用户的体质给出个性化建议
                3. 回答简洁实用，适当使用表情符号
                4. 如果知道用户姓名，请用姓名称呼
                
                请开始回答：""");

            ctx.setSystemPrompt(sb.toString());
        }
        // 如果已有 prompt（来自 CoordinatorAgent），保持原样，不做修改

        log.info("Prompt assembled, length={}",
                ctx.getSystemPrompt() != null ? ctx.getSystemPrompt().length() : 0);
        logEnd(ctx);
        return true;
    }
}