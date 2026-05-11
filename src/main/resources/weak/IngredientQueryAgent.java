package weak;

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
@RequiredArgsConstructor
public class IngredientQueryAgent {

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

    public void process(ConversationContext ctx) {
        String foodName = extractFoodName(ctx.getUserMessage());
        if (foodName == null || foodName.isEmpty()) {
            return;
        }

        ctx.setFoodName(foodName);

        Object cached = redisCacheService.getFoodInfo(foodName);
        if (cached instanceof IngredientInfo) {
            ctx.setFoodInfo((IngredientInfo) cached);
            log.info("从缓存获取食材: {}", foodName);
            return;
        }

        try {
            IngredientInfo info = baiduBaikeService.getIngredientInfo(foodName);
            if (info != null) {
                redisCacheService.cacheFoodInfo(foodName, info);
                ctx.setFoodInfo(info);
                return;
            }
        } catch (Exception e) {
            log.warn("百度百科获取失败: {}", e.getMessage());
        }

        IngredientInfo info = onDemandFoodService.getFoodInfo(foodName);
        ctx.setFoodInfo(info);
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