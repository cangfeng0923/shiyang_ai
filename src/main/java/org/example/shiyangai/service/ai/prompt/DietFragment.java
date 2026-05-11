// DietFragment.java
package org.example.shiyangai.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DietFragment implements PromptFragment {

    @Override
    public String getContent(ConversationContext ctx) {
        StringBuilder sb = new StringBuilder();

        List<DietRecord> todayRecords = ctx.getTodayRecords();
        if (todayRecords != null && !todayRecords.isEmpty()) {
            sb.append("【今日饮食记录】\n");
            for (DietRecord record : todayRecords) {
                sb.append(String.format("- %s：%s\n",
                        getMealName(record.getMealType()),
                        record.getFoodName()));
            }
            sb.append("\n");
        }

        List<DietRecord> weekRecords = ctx.getWeekRecords();
        if (weekRecords != null && !weekRecords.isEmpty()) {
            double avgScore = weekRecords.stream()
                    .mapToInt(r -> r.getHealthScore() != null ? r.getHealthScore() : 0)
                    .average().orElse(0);
            sb.append("【近7日饮食评分】\n");
            sb.append(String.format("- 平均健康评分：%.0f/100\n", avgScore));
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return true;
    }

    private String getMealName(String mealType) {
        switch (mealType) {
            case "BREAKFAST": return "早餐";
            case "LUNCH": return "午餐";
            case "DINNER": return "晚餐";
            default: return mealType;
        }
    }
}