// UserDataAgent.java（修复版 - 确保 allergyFoods 正确加载）
package org.example.shiyangai.service.ai.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.User;
import org.example.shiyangai.mapper.UserMapper;
import org.example.shiyangai.service.DynamicSolarTermService;
import org.example.shiyangai.service.HealthProfileService;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.example.shiyangai.service.diet.DietRecordService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Component
public class UserDataAgent extends BaseAgent {

    private final HealthProfileService healthProfileService;
    private final DietRecordService dietRecordService;
    private final DynamicSolarTermService dynamicSolarTermService;
    private final UserMapper userMapper;

    public UserDataAgent(HealthProfileService healthProfileService,
                         DietRecordService dietRecordService,
                         DynamicSolarTermService dynamicSolarTermService,
                         UserMapper userMapper) {
        super("UserDataAgent");
        this.healthProfileService = healthProfileService;
        this.dietRecordService = dietRecordService;
        this.dynamicSolarTermService = dynamicSolarTermService;
        this.userMapper = userMapper;
    }

    @Override
    public int getPriority() {
        return 10;
    }


    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String userId = ctx.getUserId();
        String constitution = ctx.getConstitution();

        // 1. 获取用户基本信息
        try {
            User user = userMapper.selectByUserId(userId);
            if (user != null) {
                ctx.setUserName(user.getUsername());
                if (constitution == null || constitution.isEmpty()) {
                    ctx.setConstitution(user.getConstitution());
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
        }

        // 2. 获取健康档案（包含过敏源、慢性病等）
        HealthProfile profile = healthProfileService.getProfile(userId);
        if (profile != null) {
            ctx.setProfile(profile);  // ← 关键：设置完整的 profile

            if (profile.getGender() != null) {
                ctx.setUserGender(profile.getGender());
            }

            if (profile.getAge() != null) {
                ctx.setUserAge(profile.getAge());
            } else if (profile.getBirthDate() != null) {
                int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
                ctx.setUserAge(age);
            }

            // 如果 profile 里有体质，优先使用
            if (profile.getConstitution() != null && (constitution == null || constitution.isEmpty())) {
                ctx.setConstitution(profile.getConstitution());
            }

            log.info("用户健康档案加载: allergyFoods={}, chronicDiseases={}",
                    profile.getAllergies(), profile.getPastDiseases());
        }

        // 3. 获取饮食记录
        ctx.setTodayRecords(dietRecordService.getTodayRecords(userId));
        ctx.setWeekRecords(dietRecordService.getWeekRecords(userId));

        // 4. 获取节气建议
        if (constitution != null && !constitution.isEmpty()) {
            ctx.setSolarTermAdvice(dynamicSolarTermService.getDynamicSolarTermAdvice(constitution));
        }

        log.info("UserDataAgent 加载完成: userName={}, age={}, constitution={}, allergyFoods={}",
                ctx.getUserName(), ctx.getUserAge(), ctx.getConstitution(),
                profile != null ? profile.getAllergies() : null);

        logEnd(ctx);
        return true;
    }
}