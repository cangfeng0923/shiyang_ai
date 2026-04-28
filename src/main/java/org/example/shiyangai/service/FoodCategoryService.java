package org.example.shiyangai.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class FoodCategoryService {

    // 蔬菜类关键词
    private static final Set<String> VEGETABLE_KEYWORDS = new HashSet<>();

    // 水果类关键词
    private static final Set<String> FRUIT_KEYWORDS = new HashSet<>();

    // 蛋白质类关键词
    private static final Set<String> PROTEIN_KEYWORDS = new HashSet<>();

    // 主食类关键词
    private static final Set<String> GRAIN_KEYWORDS = new HashSet<>();

    // 不健康食物关键词
    private static final Set<String> UNHEALTHY_KEYWORDS = new HashSet<>();

    @PostConstruct
    public void init() {
        // 初始化蔬菜类
        VEGETABLE_KEYWORDS.addAll(Arrays.asList(
                "菠菜", "青菜", "小白菜", "大白菜", "娃娃菜", "生菜", "油麦菜", "空心菜", "苋菜",
                "西兰花", "菜花", "花椰菜", "卷心菜", "包菜", "紫甘蓝", "芥蓝", "芦笋", "竹笋",
                "番茄", "西红柿", "黄瓜", "冬瓜", "南瓜", "苦瓜", "丝瓜", "西葫芦", "节瓜",
                "萝卜", "白萝卜", "胡萝卜", "红萝卜", "青萝卜", "土豆", "马铃薯", "茄子",
                "青椒", "彩椒", "甜椒", "尖椒", "豆角", "四季豆", "豇豆", "蘑菇", "香菇",
                "金针菇", "杏鲍菇", "平菇", "茶树菇", "木耳", "银耳", "洋葱", "大蒜", "大葱",
                "韭菜", "蒜苗", "芹菜", "香菜", "茼蒿", "蔬菜"
        ));

        // 初始化水果类
        FRUIT_KEYWORDS.addAll(Arrays.asList(
                "苹果", "梨", "香蕉", "橙子", "橘子", "柑", "柚子", "柠檬", "金桔",
                "猕猴桃", "草莓", "蓝莓", "树莓", "桑葚", "樱桃", "车厘子",
                "葡萄", "提子", "西瓜", "哈密瓜", "甜瓜", "香瓜", "木瓜",
                "桃", "李子", "杏", "梅", "柿子", "石榴", "山楂", "枣", "冬枣",
                "芒果", "火龙果", "菠萝", "凤梨", "榴莲", "山竹", "荔枝", "龙眼", "桂圆",
                "杨梅", "枇杷", "橄榄", "无花果", "百香果", "椰子", "甘蔗"
        ));

        // 初始化蛋白质类
        PROTEIN_KEYWORDS.addAll(Arrays.asList(
                "猪肉", "牛肉", "羊肉", "鸡肉", "鸭肉", "鹅肉", "兔肉", "鸽子肉",
                "五花肉", "里脊肉", "排骨", "鸡腿", "鸡胸肉", "鸭腿", "牛腩", "牛腱",
                "鱼肉", "鱼片", "三文鱼", "鳕鱼", "鲈鱼", "带鱼", "鲫鱼", "草鱼", "鲤鱼",
                "虾", "螃蟹", "龙虾", "扇贝", "蛤蜊", "牡蛎", "生蚝",
                "鸡蛋", "鸭蛋", "鹅蛋", "鹌鹑蛋", "皮蛋", "咸蛋",
                "豆腐", "豆浆", "腐竹", "豆干", "豆皮", "豆腐脑", "素鸡",
                "牛奶", "酸奶", "奶酪", "黄油", "奶油", "肉", "蛋", "鱼", "虾", "蟹"
        ));

        // 初始化主食类
        GRAIN_KEYWORDS.addAll(Arrays.asList(
                "米饭", "大米", "小米", "糙米", "黑米", "紫米", "糯米", "粳米", "籼米",
                "馒头", "包子", "饺子", "馄饨", "面条", "米粉", "米线", "河粉",
                "面包", "吐司", "蛋糕", "饼干", "曲奇", "派", "蛋挞",
                "粥", "稀饭", "羹", "糊", "燕麦", "麦片", "玉米", "玉米棒",
                "红薯", "紫薯", "山药", "芋头", "南瓜",
                "全麦", "荞麦", "藜麦", "薏米", "红豆", "绿豆", "黑豆", "黄豆"
        ));

        // 初始化不健康食物
        UNHEALTHY_KEYWORDS.addAll(Arrays.asList(
                "炸鸡", "炸薯条", "炸串", "烧烤", "烤串", "烤肠", "烤鱼",
                "蛋糕", "奶油蛋糕", "芝士蛋糕", "慕斯", "提拉米苏",
                "饼干", "曲奇", "蛋挞", "泡芙", "甜甜圈", "马卡龙",
                "奶茶", "波霸奶茶", "奶盖茶", "果汁奶茶",
                "可乐", "雪碧", "芬达", "七喜", "汽水", "碳酸饮料",
                "糖果", "巧克力", "冰淇淋", "雪糕", "冰棒", "圣代",
                "汉堡", "炸鸡汉堡", "披萨", "热狗", "三明治", "卷饼",
                "薯片", "薯条", "虾条", "爆米花", "辣条",
                "月饼", "汤圆", "年糕", "麻团", "炸", "烤", "烧烤", "甜"
        ));

        log.info("食物分类服务初始化完成 - 蔬菜:{}种, 水果:{}种, 蛋白质:{}种, 主食:{}种, 不健康:{}种",
                VEGETABLE_KEYWORDS.size(), FRUIT_KEYWORDS.size(), PROTEIN_KEYWORDS.size(),
                GRAIN_KEYWORDS.size(), UNHEALTHY_KEYWORDS.size());
    }

    /**
     * 判断是否为蔬菜
     */
    public boolean isVegetable(String foodName) {
        if (foodName == null || foodName.isEmpty()) return false;
        String lowerName = foodName.toLowerCase();
        for (String keyword : VEGETABLE_KEYWORDS) {
            if (lowerName.contains(keyword.toLowerCase())) return true;
        }
        // 降级匹配：含"菜"字
        return lowerName.contains("菜");
    }

    /**
     * 判断是否为水果
     */
    public boolean isFruit(String foodName) {
        if (foodName == null || foodName.isEmpty()) return false;
        String lowerName = foodName.toLowerCase();
        for (String keyword : FRUIT_KEYWORDS) {
            if (lowerName.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * 判断是否为蛋白质
     */
    public boolean isProtein(String foodName) {
        if (foodName == null || foodName.isEmpty()) return false;
        String lowerName = foodName.toLowerCase();
        for (String keyword : PROTEIN_KEYWORDS) {
            if (lowerName.contains(keyword.toLowerCase())) return true;
        }
        // 降级匹配：含"肉"、"蛋"、"鱼"、"虾"、"豆腐"、"奶"
        return lowerName.contains("肉") || lowerName.contains("蛋") ||
                lowerName.contains("鱼") || lowerName.contains("虾") ||
                lowerName.contains("豆腐") || lowerName.contains("奶");
    }

    /**
     * 判断是否为主食
     */
    public boolean isGrain(String foodName) {
        if (foodName == null || foodName.isEmpty()) return false;
        String lowerName = foodName.toLowerCase();
        for (String keyword : GRAIN_KEYWORDS) {
            if (lowerName.contains(keyword.toLowerCase())) return true;
        }
        // 降级匹配：含"饭"、"面"、"包"、"饼"、"粥"、"米"
        return lowerName.contains("饭") || lowerName.contains("面") ||
                lowerName.contains("包") || lowerName.contains("饼") ||
                lowerName.contains("粥") || lowerName.contains("米");
    }

    /**
     * 判断是否为不健康食物
     */
    public boolean isUnhealthy(String foodName) {
        if (foodName == null || foodName.isEmpty()) return false;
        String lowerName = foodName.toLowerCase();
        for (String keyword : UNHEALTHY_KEYWORDS) {
            if (lowerName.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * 获取食物分类
     */
    public String getCategory(String foodName) {
        if (isVegetable(foodName)) return "蔬菜";
        if (isFruit(foodName)) return "水果";
        if (isProtein(foodName)) return "蛋白质";
        if (isGrain(foodName)) return "主食";
        if (isUnhealthy(foodName)) return "需控制";
        return "其他";
    }

    /**
     * 获取食物分类统计（用于报告）
     */
    public Map<String, Integer> getCategoryStats(List<String> foodNames) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("蔬菜", 0);
        stats.put("水果", 0);
        stats.put("蛋白质", 0);
        stats.put("主食", 0);
        stats.put("需控制", 0);
        stats.put("其他", 0);

        for (String foodName : foodNames) {
            String category = getCategory(foodName);
            stats.put(category, stats.getOrDefault(category, 0) + 1);
        }

        return stats;
    }
}