// UserProfileFragment.java
package org.example.shiyangai.service.ai.prompt;

import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserProfileFragment implements PromptFragment {

    @Override
    public String getContent(ConversationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户个人信息】\n");

        // 用户名
        if (ctx.getUserName() != null && !ctx.getUserName().isEmpty()) {
            sb.append("- 姓名：").append(ctx.getUserName()).append("\n");
        }

        // 年龄
        if (ctx.getUserAge() != null) {
            sb.append("- 年龄：").append(ctx.getUserAge()).append("岁\n");
        }

        // 性别
        if (ctx.getUserGender() != null) {
            String gender = "MALE".equals(ctx.getUserGender()) ? "男" : "女";
            sb.append("- 性别：").append(gender).append("\n");
        }

        // 身高体重
        HealthProfile profile = ctx.getProfile();
        if (profile != null) {
            if (profile.getHeight() != null) {
                sb.append("- 身高：").append(profile.getHeight()).append("cm\n");
            }
            if (profile.getWeight() != null) {
                sb.append("- 体重：").append(profile.getWeight()).append("kg\n");
            }
        }

        // 体质
        String constitution = ctx.getConstitution();
        if (constitution != null) {
            sb.append("- 中医体质：").append(constitution).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        return true;
    }
}