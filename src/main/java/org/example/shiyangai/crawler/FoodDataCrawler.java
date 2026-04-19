package org.example.shiyangai.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class FoodDataCrawler {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    // 完整的食材数据库（内置）
    private final Map<String, FoodData> foodDatabase;

    public FoodDataCrawler() {
        this.objectMapper = new ObjectMapper();
        this.foodDatabase = new HashMap<>();
        initFoodDatabase();
    }

    /**
     * 初始化完整的食材数据库
     */
    private void initFoodDatabase() {
        // 蔬菜类
        addFood("苦瓜", "寒", "苦", "清热解暑，明目解毒", "心、脾、胃", "脾胃虚寒者慎食");
        addFood("冬瓜", "寒", "甘", "清热利水，消肿解毒", "肺、大肠、膀胱", "脾胃虚寒者不宜多食");
        addFood("黄瓜", "寒", "甘", "清热利水，解毒消肿", "肺、胃", "脾胃虚寒者少食");
        addFood("丝瓜", "凉", "甘", "清热化痰，凉血解毒", "肺、肝、胃", null);
        addFood("南瓜", "温", "甘", "温中平喘，补中益气", "脾、胃", null);
        addFood("苦瓜", "寒", "苦", "清热解暑，明目解毒", "心、脾、胃", "脾胃虚寒者慎食");
        addFood("冬瓜", "寒", "甘", "清热利水，消肿解毒", "肺、大肠、膀胱", "脾胃虚寒者不宜多食");
        addFood("黄瓜", "寒", "甘", "清热利水，解毒消肿", "肺、胃", "脾胃虚寒者少食");
        addFood("丝瓜", "凉", "甘", "清热化痰，凉血解毒", "肺、肝、胃", null);
        addFood("南瓜", "温", "甘", "温中平喘，补中益气", "脾、胃", null);

        // 根茎类
        addFood("山药", "平", "甘", "健脾益胃，补肾益精", "脾、肺、肾", null);
        addFood("白萝卜", "凉", "辛甘", "消食化痰，下气宽中", "肺、脾", null);
        addFood("胡萝卜", "平", "甘", "健脾化滞，润燥明目", "肺、脾", null);
        addFood("土豆", "平", "甘", "健脾益气，调和脾胃", "脾、胃", null);
        addFood("红薯", "平", "甘", "补中和血，益气生津", "脾、肾", null);
        addFood("芋头", "平", "甘", "健脾补虚，散结解毒", "脾、胃", null);
        addFood("莲藕", "寒(生)", "甘", "清热凉血，止血散瘀", "心、脾、胃", null);
        addFood("荸荠", "寒", "甘", "清热生津，化痰明目", "肺、胃", null);

        // 叶菜类
        addFood("白菜", "平", "甘", "清热除烦，通利肠胃", "胃、大肠", null);
        addFood("菠菜", "凉", "甘", "养血润燥，清热除烦", "大肠、小肠、胃", null);
        addFood("芹菜", "凉", "甘", "平肝清热，祛风利湿", "肝、胃", null);
        addFood("韭菜", "温", "辛", "补肾助阳，行气活血", "肝、肾", null);
        addFood("香菜", "温", "辛", "发汗透疹，消食下气", "脾、胃", null);
        addFood("生菜", "凉", "甘", "清热利尿，通乳", "胃、大肠", null);
        addFood("油菜", "凉", "甘", "活血化瘀，解毒消肿", "脾、胃", null);

        // 菌菇类
        addFood("香菇", "平", "甘", "扶正补虚，健脾开胃", "胃", null);
        addFood("木耳", "平", "甘", "补气养血，润肺止咳", "肺、脾", null);
        addFood("银耳", "平", "甘", "滋阴润肺，养胃生津", "肺、胃", null);
        addFood("蘑菇", "平", "甘", "健脾开胃，化痰理气", "脾、胃", null);
        addFood("金针菇", "凉", "甘", "补肝益肠，抗癌", "肝、胃", null);

        // 水果类
        addFood("苹果", "凉", "甘酸", "生津止渴，健脾益胃", "脾、肺", null);
        addFood("梨", "凉", "甘酸", "润肺止咳，清热化痰", "肺、胃", null);
        addFood("香蕉", "寒", "甘", "清热润肠，解毒生津", "肺、大肠", null);
        addFood("西瓜", "寒", "甘", "清热解暑，生津止渴", "心、胃", null);
        addFood("葡萄", "平", "甘酸", "补气血，强筋骨", "肝、肾", null);
        addFood("桃子", "温", "甘酸", "补气生津，活血消积", "肺、大肠", null);
        addFood("李子", "平", "甘酸", "清热生津，利水", "肝、肾", null);
        addFood("杏", "温", "甘酸", "润肺定喘，生津止渴", "肺、大肠", null);
        addFood("樱桃", "温", "甘", "补中益气，祛风胜湿", "脾、肾", null);
        addFood("草莓", "凉", "甘酸", "润肺生津，健脾和胃", "肺、脾", null);
        addFood("橙子", "凉", "甘酸", "生津止渴，开胃下气", "肺、胃", null);
        addFood("橘子", "凉", "甘酸", "润肺止咳，开胃理气", "肺、胃", null);
        addFood("哈密瓜", "寒", "甘", "清热解暑，生津止渴", "心、胃", null);

        // 谷物豆类
        addFood("大米", "平", "甘", "补中益气，健脾和胃", "脾、胃", null);
        addFood("小米", "凉", "甘", "健脾和胃，滋阴安神", "脾、胃", null);
        addFood("玉米", "平", "甘", "调中开胃，利尿消肿", "脾、胃", null);
        addFood("黄豆", "平", "甘", "健脾宽中，润燥消水", "脾、胃", null);
        addFood("绿豆", "寒", "甘", "清热解毒，消暑利尿", "心、胃", null);
        addFood("红豆", "平", "甘酸", "利水消肿，解毒排脓", "心、小肠", null);
        addFood("黑豆", "平", "甘", "补肾益阴，健脾利湿", "肾", null);
        addFood("薏米", "凉", "甘淡", "健脾祛湿，清热排脓", "脾、肺", null);

        // 干果类
        addFood("红枣", "温", "甘", "补中益气，养血安神", "脾、胃", null);
        addFood("枸杞", "平", "甘", "滋补肝肾，益精明目", "肝、肾", null);
        addFood("桂圆", "温", "甘", "补益心脾，养血安神", "心、脾", null);
        addFood("莲子", "平", "甘涩", "补脾止泻，益肾固精", "脾、肾", null);
        addFood("百合", "微寒", "甘", "润肺止咳，清心安神", "心、肺", null);
        addFood("花生", "平", "甘", "健脾养胃，润肺化痰", "脾、肺", null);
        addFood("黑芝麻", "平", "甘", "补肝肾，润五脏", "肝、肾", null);

        // 肉类
        addFood("猪肉", "平", "甘咸", "滋阴润燥，补肾养血", "脾、胃、肾", null);
        addFood("牛肉", "温", "甘", "补脾胃，益气血", "脾、胃", null);
        addFood("羊肉", "热", "甘", "温中暖肾，益气补虚", "脾、肾", null);
        addFood("鸡肉", "温", "甘", "温中益气，补精填髓", "脾、胃", null);
        addFood("鸭肉", "凉", "甘", "滋阴补虚，清热利水", "脾、胃、肺、肾", null);
        addFood("鹅肉", "平", "甘", "益气补虚，和胃止渴", "脾、肺", null);

        // 水产品
        addFood("鲤鱼", "平", "甘", "健脾利湿，和中开胃", "脾、胃", null);
        addFood("鲫鱼", "平", "甘", "健脾利湿，和中开胃", "脾、胃", null);
        addFood("虾", "温", "甘", "补肾壮阳，理气通乳", "肝、肾", null);
        addFood("螃蟹", "寒", "咸", "益阴补髓，清热利湿", "肝、胃", "脾胃虚寒者忌食");

        // 调味品
        addFood("生姜", "温", "辛", "解表散寒，温中止呕", "肺、脾、胃", null);
        addFood("大蒜", "温", "辛", "解毒杀虫，温中健胃", "脾、胃", null);
        addFood("大葱", "温", "辛", "发汗解表，散寒通阳", "肺、胃", null);
        addFood("花椒", "热", "辛", "温中止痛，杀虫止痒", "脾、胃", null);
        addFood("八角", "温", "辛", "温中理气，健胃止呕", "脾、肾", null);
        addFood("肉桂", "热", "辛甘", "补火助阳，散寒止痛", "肾、脾", "阴虚火旺者忌用");
        addFood("陈皮", "温", "辛苦", "理气健脾，燥湿化痰", "脾、肺", null);
        addFood("山楂", "微温", "酸甘", "消食健胃，行气散瘀", "脾、胃、肝", null);

        // 蛋奶
        addFood("鸡蛋", "平", "甘", "滋阴润燥，养血安胎", "脾、胃", null);
        addFood("牛奶", "平", "甘", "补虚损，益肺胃", "脾、肺", null);
        addFood("豆腐", "凉", "甘", "清热润燥，生津解毒", "脾、胃", null);
        addFood("豆浆", "平", "甘", "补虚润燥，清肺化痰", "脾、胃", null);
        addFood("蜂蜜", "平", "甘", "补中润燥，止痛解毒", "脾、肺", null);
    }

    private void addFood(String name, String property, String flavor, String effect, String meridian, String contraindication) {
        FoodData data = new FoodData();
        data.name = name;
        data.property = property;
        data.flavor = flavor;
        data.effect = effect;
        data.meridian = meridian;
        data.contraindication = contraindication;
        foodDatabase.put(name, data);
    }

    /**
     * 定时任务：将食材数据导入Redis
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void importAllToRedis() {
        if (redisTemplate == null) {
            System.err.println("Redis未配置，无法导入数据");
            return;
        }

        int count = 0;
        for (Map.Entry<String, FoodData> entry : foodDatabase.entrySet()) {
            String name = entry.getKey();
            FoodData data = entry.getValue();

            IngredientInfo info = new IngredientInfo();
            info.setName(name);
            info.setProperty(data.property);
            info.setFlavor(data.flavor);
            info.setEffect(data.effect);
            info.setMeridian(data.meridian);
            info.setContraindication(data.contraindication);
            info.setSource("食材数据库");
            info.setConfidence(95);
            info.setUpdateTime(System.currentTimeMillis());

            redisTemplate.opsForValue().set("food:" + name, info, 30, TimeUnit.DAYS);
            count++;

            if (count % 20 == 0) {
                System.out.println("已导入 " + count + " 种食材");
            }
        }

        System.out.println("批量导入完成！共导入 " + count + " 种食材到Redis");
    }

    /**
     * 手动触发导入
     */
    public void manualImport() {
        importAllToRedis();
    }

    /**
     * 获取食材信息（直接从本地数据库）
     */
    public IngredientInfo getFoodInfo(String foodName) {
        FoodData data = foodDatabase.get(foodName);
        if (data == null) {
            return null;
        }

        IngredientInfo info = new IngredientInfo();
        info.setName(foodName);
        info.setProperty(data.property);
        info.setFlavor(data.flavor);
        info.setEffect(data.effect);
        info.setMeridian(data.meridian);
        info.setContraindication(data.contraindication);
        info.setSource("食材数据库");
        info.setConfidence(95);
        info.setUpdateTime(System.currentTimeMillis());

        return info;
    }

    /**
     * 获取所有食材名称
     */
    public List<String> getAllFoodNames() {
        return new ArrayList<>(foodDatabase.keySet());
    }

    /**
     * 按性味分类获取食材
     */
    public Map<String, List<String>> getFoodsByProperty() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("寒", new ArrayList<>());
        result.put("凉", new ArrayList<>());
        result.put("平", new ArrayList<>());
        result.put("温", new ArrayList<>());
        result.put("热", new ArrayList<>());

        for (Map.Entry<String, FoodData> entry : foodDatabase.entrySet()) {
            String property = entry.getValue().property;
            // 处理微寒、微温等
            String key = property.contains("寒") ? "寒" :
                    property.contains("凉") ? "凉" :
                            property.contains("温") ? "温" :
                                    property.contains("热") ? "热" : "平";
            result.get(key).add(entry.getKey());
        }

        return result;
    }

    /**
     * 食材数据内部类
     */
    private static class FoodData {
        String name;
        String property;
        String flavor;
        String effect;
        String meridian;
        String contraindication;
    }
}