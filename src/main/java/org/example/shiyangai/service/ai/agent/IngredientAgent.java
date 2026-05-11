// IngredientAgent.java
package org.example.shiyangai.service.ai.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.BaiduBaikeService;
import org.example.shiyangai.service.OnDemandFoodService;
import org.example.shiyangai.service.RedisCacheService;
import org.example.shiyangai.service.ai.context.ConversationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IngredientAgent extends BaseAgent {

    private final BaiduBaikeService baiduBaikeService;
    private final OnDemandFoodService onDemandFoodService;
    private final RedisCacheService redisCacheService;

    private static final String[] FOOD_KEYWORDS = {
            "玉米", "苹果", "梨", "香蕉", "西瓜", "葡萄", "草莓", "橙子",
            "鸡肉", "牛肉", "猪肉", "鱼肉", "鸡蛋", "鸭肉", "羊肉", "虾",
            "豆腐", "豆浆", "牛奶", "酸奶", "山药", "红枣", "枸杞", "薏米",
            "生姜", "大蒜", "韭菜", "冬瓜", "苦瓜", "黄瓜", "萝卜", "胡萝卜",
            "菠菜", "青菜", "西兰花", "番茄", "茄子", "土豆", "红薯"
    };

    // 手动构造函数，调用父类 BaseAgent(String name)
    public IngredientAgent(BaiduBaikeService baiduBaikeService,
                           OnDemandFoodService onDemandFoodService,
                           RedisCacheService redisCacheService) {
        super("IngredientAgent");
        this.baiduBaikeService = baiduBaikeService;
        this.onDemandFoodService = onDemandFoodService;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    public boolean supports(ConversationContext ctx) {
        String foodName = extractFoodName(ctx.getUserMessage());
        return foodName != null && !foodName.isEmpty();
    }

    @Override
    public boolean execute(ConversationContext ctx) {
        logStart(ctx);

        String foodName = extractFoodName(ctx.getUserMessage());
        ctx.setFoodName(foodName);

        // 检查缓存
        Object cached = redisCacheService.getFoodInfo(foodName);
        if (cached instanceof IngredientInfo) {
            ctx.setFoodInfo((IngredientInfo) cached);
            log.debug("从缓存获取食材: {}", foodName);
            logEnd(ctx);
            return true;
        }

        // 查询百科
        try {
            IngredientInfo info = baiduBaikeService.getIngredientInfo(foodName);
            if (info != null) {
                redisCacheService.cacheFoodInfo(foodName, info);
                ctx.setFoodInfo(info);
                logEnd(ctx);
                return true;
            }
        } catch (Exception e) {
            log.warn("百度百科获取失败: {}", e.getMessage());
        }

        // 兜底查询
        IngredientInfo info = onDemandFoodService.getFoodInfo(foodName);
        ctx.setFoodInfo(info);

        logEnd(ctx);
        return true;
    }

    private String extractFoodName(String message) {
        if (message == null) return null;
        for (String keyword : FOOD_KEYWORDS) {
            if (message.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }
}