package org.example.shiyangai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConstitutionType {

    PEACE("平和质", "阴阳平衡，气血调和，饮食宜多样化",
            new String[]{"大部分食物", "五谷杂粮", "蔬菜水果", "鱼虾"},
            new String[]{"过度辛辣", "过度油腻", "过度寒凉"}),

    QI_DEFICIENCY("气虚质", "元气不足，疲乏气短，宜补气健脾",
            new String[]{"山药", "黄芪", "大枣", "小米", "鸡肉", "牛肉", "黄豆", "蜂蜜"},
            new String[]{"生萝卜", "空心菜", "槟榔", "生冷瓜果", "苦瓜", "绿茶"}),

    YANG_DEFICIENCY("阳虚质", "阳气不足，畏寒怕冷，宜温补阳气",
            new String[]{"生姜", "羊肉", "韭菜", "核桃", "桂圆", "肉桂", "花椒", "虾"},
            new String[]{"冷饮", "西瓜", "苦瓜", "绿豆", "螃蟹", "柿子", "冰淇淋"}),

    YIN_DEFICIENCY("阴虚质", "阴液亏少，口燥咽干，宜滋阴润燥",
            new String[]{"百合", "银耳", "梨", "枸杞", "黑芝麻", "桑葚", "鸭肉", "燕窝"},
            new String[]{"烧烤", "辣椒", "羊肉", "油炸食品", "生姜", "肉桂", "白酒"}),

    PHLEGM_DAMPNESS("痰湿质", "痰湿凝聚，形体肥胖，宜健脾祛湿",
            new String[]{"薏米", "赤小豆", "冬瓜", "白萝卜", "陈皮", "荷叶", "海带", "茯苓"},
            new String[]{"肥肉", "甜食", "糯米", "油炸", "奶茶", "奶油蛋糕"}),

    DAMP_HEAT("湿热质", "湿热内蕴，面垢油光，宜清热利湿",
            new String[]{"绿豆", "冬瓜", "苦瓜", "薏米", "莲子", "红豆", "黄瓜", "芹菜"},
            new String[]{"烧烤", "辣椒", "油炸", "酒", "羊肉", "火锅", "榴莲"}),

    BLOOD_STASIS("血瘀质", "血行不畅，肤色晦暗，宜活血化瘀",
            new String[]{"山楂", "黑豆", "玫瑰花", "木耳", "红糖", "桃仁", "油菜", "茄子"},
            new String[]{"冷饮", "肥肉", "蛋黄", "奶油", "寒凉食物"}),

    QI_STAGNATION("气郁质", "气机郁滞，情绪低落，宜理气解郁",
            new String[]{"玫瑰花", "陈皮", "萝卜", "海带", "佛手", "橙子", "香蕉", "燕麦"},
            new String[]{"咖啡", "浓茶", "油腻", "辛辣", "巧克力"}),

    SPECIAL("特禀质", "先天失常，容易过敏，宜避免过敏原",
            new String[]{"蜂蜜", "大枣", "金针菇", "胡萝卜", "糙米", "西兰花"},
            new String[]{"海鲜", "芒果", "辛辣", "酒", "鸡蛋", "牛奶", "花生"});

    private final String name;           // 体质名称
    private final String description;    // 体质描述
    private final String[] goodFoods;    // 适宜食物
    private final String[] badFoods;     // 不宜食物

    /**
     * 评估食物对当前体质的适宜性
     * @param foodName 食物名称
     * @return 评估结果
     */
    public SuitabilityResult evaluate(String foodName) {
        if (foodName == null || foodName.isEmpty()) {
            return new SuitabilityResult("⚠️ 无效输入", "请提供食物名称", "无");
        }

        String lowerFood = foodName.toLowerCase();

        // 检查不宜食物
        for (String bad : badFoods) {
            if (lowerFood.contains(bad.toLowerCase()) || bad.toLowerCase().contains(lowerFood)) {
                String suggestion = "根据" + this.name + "体质特点，" + foodName + "可能加重体质偏颇。";
                return new SuitabilityResult("❌ 不宜食用", suggestion, "建议减少食用，可尝试：" + String.join("、", goodFoods));
            }
        }

        // 检查适宜食物
        for (String good : goodFoods) {
            if (lowerFood.contains(good.toLowerCase()) || good.toLowerCase().contains(lowerFood)) {
                String reason = foodName + "符合" + this.name + "质调养原则，" + this.description;
                return new SuitabilityResult("✅ 适宜食用", reason, "可以适量食用，有助于调理体质");
            }
        }

        // 中性食物
        return new SuitabilityResult("⚖️ 中性", foodName + "对" + this.name + "质影响不显著",
                "可适量食用，建议多摄入：" + String.join("、", goodFoods));
    }

    /**
     * 根据体质名称获取枚举
     */
    public static ConstitutionType fromName(String name) {
        for (ConstitutionType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return PEACE;  // 默认返回平和质
    }

    /**
     * 评估结果内部类
     */
    @Getter
    @AllArgsConstructor
    public static class SuitabilityResult {
        private final String suitability;  // 适宜性标签
        private final String reason;       // 分析理由
        private final String suggestion;   // 建议

        @Override
        public String toString() {
            return String.format("%s\n%s\n💡 %s", suitability, reason, suggestion);
        }
    }
}