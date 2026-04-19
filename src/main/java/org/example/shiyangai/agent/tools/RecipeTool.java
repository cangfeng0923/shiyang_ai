package org.example.shiyangai.agent.tools;

import org.example.shiyangai.agent.Tool;
import org.example.shiyangai.agent.ToolExecutor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class RecipeTool implements ToolExecutor {

    private static final Map<String, List<DetailedRecipe>> RECIPES = new HashMap<>();

    static {
        // 详细食谱数据
        List<DetailedRecipe> yinXuRecipes = Arrays.asList(
                new DetailedRecipe(
                        "百合银耳雪梨羹",
                        "早餐",
                        "滋阴润肺、清热生津",
                        "银耳1朵（提前泡发2小时）、百合20g、雪梨1个、冰糖15g、枸杞10g、红枣5颗",
                        "1. 银耳撕成小朵，放入砂锅加1.5L水\n2. 大火煮沸后转小火炖30分钟\n3. 加入百合、红枣继续炖20分钟\n4. 加入切块的雪梨和枸杞炖10分钟\n5. 最后加入冰糖融化即可",
                        "晨起空腹食用效果最佳，可连续食用3-5天"
                ),
                new DetailedRecipe(
                        "冬瓜薏米鸭肉煲",
                        "午餐",
                        "清热利湿、滋阴降火",
                        "鸭肉500g、冬瓜500g、薏米50g、陈皮1片、生姜3片、盐适量",
                        "1. 薏米提前浸泡2小时\n2. 鸭肉焯水去腥\n3. 所有材料放入砂锅，加2L水\n4. 大火煮沸转小火炖1.5小时\n5. 出锅前10分钟加盐调味",
                        "鸭肉性凉，阴虚体质最适合；薏米祛湿不伤阴"
                ),
                new DetailedRecipe(
                        "荞麦饭配凉拌黄瓜",
                        "午餐主食",
                        "清热降火、补充膳食纤维",
                        "荞麦米100g、大米50g、黄瓜1根、蒜末、醋、香油",
                        "1. 荞麦米和大米混合洗净\n2. 正常煮饭程序\n3. 黄瓜拍碎切块\n4. 加入蒜末、醋、香油拌匀",
                        "荞麦性凉，适合阴虚；黄瓜清热生津"
                ),
                new DetailedRecipe(
                        "枸杞菊花茶",
                        "下午茶",
                        "清肝明目、滋阴降火",
                        "枸杞10g、菊花5朵、冰糖少许、热水500ml",
                        "1. 枸杞和菊花放入杯中\n2. 用90度热水冲泡\n3. 盖上盖子焖5分钟\n4. 加入冰糖调味",
                        "适合下午3-5点饮用，清肝火、滋肾阴"
                ),
                new DetailedRecipe(
                        "银耳百合粥",
                        "晚餐",
                        "滋阴润燥、安神助眠",
                        "银耳1朵、百合30g、大米50g、莲子15g、冰糖适量",
                        "1. 银耳泡发撕小朵\n2. 所有材料放入电饭煲\n3. 加水1.5L，煮粥模式\n4. 出锅前加冰糖",
                        "晚餐吃清淡，有助于睡眠"
                ),
                new DetailedRecipe(
                        "蜂蜜水",
                        "睡前",
                        "润肠通便、安神",
                        "蜂蜜15ml、温水200ml",
                        "1. 水温不超过60度\n2. 蜂蜜用温水化开\n3. 睡前1小时饮用",
                        "蜂蜜滋阴润燥，改善阴虚便秘"
                )
        );

        RECIPES.put("阴虚质", yinXuRecipes);
        // 可以继续添加其他体质的详细食谱
    }

    @Override
    @Tool(name = "recommend_recipe",
            description = "根据体质推荐详细的养生食谱，包含食材、步骤、功效",
            parameters = {"constitution", "mealType"})
    public String execute(Map<String, Object> parameters) {
        String constitution = (String) parameters.get("constitution");
        String mealType = (String) parameters.getOrDefault("mealType", "全天");

        List<DetailedRecipe> recipes = RECIPES.getOrDefault(constitution, RECIPES.get("阴虚质"));

        if ("全天".equals(mealType)) {
            return buildFullDayMealPlan(recipes, constitution);
        } else {
            return buildMealByType(recipes, mealType);
        }
    }

    private String buildFullDayMealPlan(List<DetailedRecipe> recipes, String constitution) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 **").append(constitution).append("一日养生食谱**\n\n");

        // 按餐次分类
        Map<String, List<DetailedRecipe>> byMeal = new HashMap<>();
        for (DetailedRecipe recipe : recipes) {
            byMeal.computeIfAbsent(recipe.mealType, k -> new ArrayList<>()).add(recipe);
        }

        String[] mealOrder = {"早餐", "午餐", "下午茶", "晚餐", "睡前"};
        for (String meal : mealOrder) {
            if (byMeal.containsKey(meal)) {
                sb.append("🍽️ **").append(meal).append("**\n");
                for (DetailedRecipe recipe : byMeal.get(meal)) {
                    sb.append(recipe.toDetailedString()).append("\n");
                }
                sb.append("\n");
            }
        }

        sb.append("💡 **养生小贴士**：\n");
        sb.append("- 三餐定时，细嚼慢咽，每餐20分钟以上\n");
        sb.append("- 多喝温水，避免冷饮\n");
        sb.append("- 保持心情愉快，适当运动\n");

        return sb.toString();
    }

    private String buildMealByType(List<DetailedRecipe> recipes, String mealType) {
        for (DetailedRecipe recipe : recipes) {
            if (recipe.mealType.equals(mealType)) {
                return recipe.toDetailedString();
            }
        }
        return "未找到" + mealType + "的推荐食谱";
    }

    @Override
    public String getName() { return "recommend_recipe"; }

    @Override
    public String getDescription() {
        return "根据体质推荐详细的养生食谱，可以指定早餐/午餐/晚餐/下午茶/睡前";
    }

    // 内部类：详细食谱
    static class DetailedRecipe {
        String name;
        String mealType;
        String effect;
        String ingredients;
        String steps;
        String tips;

        DetailedRecipe(String name, String mealType, String effect,
                       String ingredients, String steps, String tips) {
            this.name = name;
            this.mealType = mealType;
            this.effect = effect;
            this.ingredients = ingredients;
            this.steps = steps;
            this.tips = tips;
        }

        String toDetailedString() {
            return String.format("""
                **%s**（%s）
                🌟 功效：%s
                🥬 食材：%s
                👨‍🍳 做法：
                %s
                💡 小贴士：%s
                """, name, mealType, effect, ingredients, steps, tips);
        }
    }
}