package weak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.service.DynamicSolarTermService;
import org.example.shiyangai.service.HealthProfileService;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.example.shiyangai.service.diet.DietRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 职责：聚合用户健康档案和饮食记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataAggregator {

    private final HealthProfileService healthProfileService;
    private final DietRecordService dietRecordService;
    @Autowired
    private DynamicSolarTermService dynamicSolarTermService;

    public void aggregate(ConversationContext ctx) {
        String userId = ctx.getUserId();

        ctx.setProfile(healthProfileService.getProfile(userId));
        ctx.setTodayRecords(dietRecordService.getTodayRecords(userId));
        ctx.setWeekRecords(dietRecordService.getWeekRecords(userId));
        ctx.setSolarTermAdvice(dynamicSolarTermService.getDynamicSolarTermAdvice(ctx.getConstitution()));
    }

    // 注意：这里需要注入 DynamicSolarTermService，原 AIService 中有
    // 如果不想改任何注入，可以把这行删掉，在原 AIService 中单独调用
}