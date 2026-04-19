package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
//节气服务
public class SolarTermService {

    // 24节气数据（2024-2025年简化版）
    private static final Map<String, SolarTermInfo> SOLAR_TERMS = new HashMap<>();

    static {
        // 春季
        SOLAR_TERMS.put("立春", new SolarTermInfo("立春", "2月3-5日", "春季开始",
                "宜升发阳气，多吃豆芽、韭菜、春笋",
                "春饼、春卷",
                "早睡早起，舒展筋骨"));
        SOLAR_TERMS.put("雨水", new SolarTermInfo("雨水", "2月18-20日", "降水增多，湿气渐重",
                "宜健脾祛湿，吃山药、薏米、红枣",
                "山药薏米粥",
                "注意保暖，避免受寒"));
        SOLAR_TERMS.put("惊蛰", new SolarTermInfo("惊蛰", "3月5-7日", "春雷始鸣，万物复苏",
                "宜吃梨润肺，吃菠菜、荠菜",
                "冰糖炖雪梨",
                "适当运动，疏肝理气"));
        SOLAR_TERMS.put("春分", new SolarTermInfo("春分", "3月20-21日", "昼夜平分",
                "宜平衡阴阳，吃香椿、豆芽",
                "香椿炒鸡蛋",
                "保持心情舒畅"));
        SOLAR_TERMS.put("清明", new SolarTermInfo("清明", "4月4-5日", "清明时节雨纷纷",
                "宜吃青团、艾叶，祛湿排毒",
                "艾叶青团",
                "踏青散步，疏解压力"));
        SOLAR_TERMS.put("谷雨", new SolarTermInfo("谷雨", "4月19-21日", "雨生百谷",
                "宜健脾祛湿，吃薏米、赤小豆",
                "薏米赤小豆汤",
                "避免淋雨受潮"));

        // 夏季
        SOLAR_TERMS.put("立夏", new SolarTermInfo("立夏", "5月5-6日", "夏季开始",
                "宜养心，吃苦瓜、莲子、鸭蛋",
                "苦瓜炒蛋",
                "午休养心"));
        SOLAR_TERMS.put("小满", new SolarTermInfo("小满", "5月20-22日", "麦类灌浆",
                "宜清热祛湿，吃冬瓜、薏米",
                "冬瓜薏米汤",
                "避免贪凉"));
        SOLAR_TERMS.put("芒种", new SolarTermInfo("芒种", "6月5-7日", "麦类成熟",
                "宜清淡饮食，吃绿豆、黄瓜",
                "绿豆汤",
                "多喝水，防中暑"));
        SOLAR_TERMS.put("夏至", new SolarTermInfo("夏至", "6月21-22日", "白天最长",
                "宜养心，吃面食、苦瓜、绿豆",
                "夏至面",
                "午睡养心"));
        SOLAR_TERMS.put("小暑", new SolarTermInfo("小暑", "7月6-8日", "开始炎热",
                "宜清热解暑，吃西瓜、冬瓜",
                "西瓜翠衣汤",
                "避免暴晒"));
        SOLAR_TERMS.put("大暑", new SolarTermInfo("大暑", "7月22-24日", "最热时节",
                "宜清热祛湿，吃绿豆、薏米",
                "绿豆薏米粥",
                "防暑降温"));

        // 秋季
        SOLAR_TERMS.put("立秋", new SolarTermInfo("立秋", "8月7-8日", "秋季开始",
                "宜滋阴润肺，吃银耳、百合、梨",
                "银耳百合汤",
                "早睡早起"));
        SOLAR_TERMS.put("处暑", new SolarTermInfo("处暑", "8月22-24日", "暑气渐消",
                "宜润燥，吃山药、莲子",
                "山药莲子粥",
                "适当添衣"));
        SOLAR_TERMS.put("白露", new SolarTermInfo("白露", "9月7-9日", "天气转凉",
                "宜润肺，吃龙眼、红薯",
                "桂圆红枣茶",
                "注意保暖"));
        SOLAR_TERMS.put("秋分", new SolarTermInfo("秋分", "9月22-24日", "昼夜平分",
                "宜滋阴，吃芝麻、核桃",
                "芝麻糊",
                "保持情绪稳定"));
        SOLAR_TERMS.put("寒露", new SolarTermInfo("寒露", "10月8-9日", "露水变寒",
                "宜温补，吃羊肉、生姜",
                "姜枣茶",
                "脚部保暖"));
        SOLAR_TERMS.put("霜降", new SolarTermInfo("霜降", "10月23-24日", "开始降霜",
                "宜平补，吃柿子、栗子",
                "栗子粥",
                "预防感冒"));

        // 冬季
        SOLAR_TERMS.put("立冬", new SolarTermInfo("立冬", "11月7-8日", "冬季开始",
                "宜温补肾阳，吃羊肉、当归",
                "当归生姜羊肉汤",
                "早睡晚起"));
        SOLAR_TERMS.put("小雪", new SolarTermInfo("小雪", "11月22-23日", "开始降雪",
                "宜温补，吃黑芝麻、核桃",
                "黑芝麻糊",
                "注意保暖"));
        SOLAR_TERMS.put("大雪", new SolarTermInfo("大雪", "12月6-8日", "雪量增大",
                "宜温补，吃红枣、桂圆",
                "红枣桂圆茶",
                "避免受寒"));
        SOLAR_TERMS.put("冬至", new SolarTermInfo("冬至", "12月21-23日", "白天最短",
                "宜温补，吃饺子、羊肉",
                "冬至饺子",
                "艾灸养生"));
        SOLAR_TERMS.put("小寒", new SolarTermInfo("小寒", "1月5-7日", "开始寒冷",
                "宜温补，吃腊八粥",
                "腊八粥",
                "注意防寒"));
        SOLAR_TERMS.put("大寒", new SolarTermInfo("大寒", "1月20-21日", "最冷时节",
                "宜温补，吃糯米饭",
                "八宝饭",
                "养精蓄锐"));
    }

    /**
     * 获取当前节气
     */
    public SolarTermInfo getCurrentSolarTerm() {
        // 简化版：根据月份和日期返回节气
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();

        // 立春大约在2月4日
        if (month == 2 && day >= 4) return SOLAR_TERMS.get("立春");
        if (month == 2 && day >= 19) return SOLAR_TERMS.get("雨水");
        if (month == 3 && day >= 5) return SOLAR_TERMS.get("惊蛰");
        if (month == 3 && day >= 20) return SOLAR_TERMS.get("春分");
        if (month == 4 && day >= 4) return SOLAR_TERMS.get("清明");
        if (month == 4 && day >= 20) return SOLAR_TERMS.get("谷雨");

        if (month == 5 && day >= 5) return SOLAR_TERMS.get("立夏");
        if (month == 5 && day >= 21) return SOLAR_TERMS.get("小满");
        if (month == 6 && day >= 5) return SOLAR_TERMS.get("芒种");
        if (month == 6 && day >= 21) return SOLAR_TERMS.get("夏至");
        if (month == 7 && day >= 7) return SOLAR_TERMS.get("小暑");
        if (month == 7 && day >= 22) return SOLAR_TERMS.get("大暑");

        if (month == 8 && day >= 7) return SOLAR_TERMS.get("立秋");
        if (month == 8 && day >= 23) return SOLAR_TERMS.get("处暑");
        if (month == 9 && day >= 7) return SOLAR_TERMS.get("白露");
        if (month == 9 && day >= 23) return SOLAR_TERMS.get("秋分");
        if (month == 10 && day >= 8) return SOLAR_TERMS.get("寒露");
        if (month == 10 && day >= 23) return SOLAR_TERMS.get("霜降");

        if (month == 11 && day >= 7) return SOLAR_TERMS.get("立冬");
        if (month == 11 && day >= 22) return SOLAR_TERMS.get("小雪");
        if (month == 12 && day >= 7) return SOLAR_TERMS.get("大雪");
        if (month == 12 && day >= 21) return SOLAR_TERMS.get("冬至");
        if (month == 1 && day >= 5) return SOLAR_TERMS.get("小寒");
        if (month == 1 && day >= 20) return SOLAR_TERMS.get("大寒");

        // 默认返回当前季节的建议
        return getSeasonDefault(month);
    }

    private SolarTermInfo getSeasonDefault(int month) {
        if (month >= 3 && month <= 5) {
            return new SolarTermInfo("春季养生", "", "春养肝",
                    "多吃绿色蔬菜，少吃酸味", "枸杞猪肝汤", "早睡早起，适当运动");
        }
        if (month >= 6 && month <= 8) {
            return new SolarTermInfo("夏季养生", "", "夏养心",
                    "多吃苦味食物，多喝水", "绿豆百合汤", "午休养心，避免暴晒");
        }
        if (month >= 9 && month <= 11) {
            return new SolarTermInfo("秋季养生", "", "秋养肺",
                    "多吃白色食物，滋阴润燥", "银耳雪梨汤", "早睡早起，保持湿润");
        }
        return new SolarTermInfo("冬季养生", "", "冬养肾",
                "多吃温热食物，适当进补", "当归生姜羊肉汤", "早睡晚起，注意保暖");
    }

    /**
     * 获取节气养生建议
     */
    public String getSolarTermAdvice(String constitution) {
        SolarTermInfo term = getCurrentSolarTerm();

        StringBuilder sb = new StringBuilder();
        sb.append("📅 当前节气：【").append(term.name).append("】\n\n");
        sb.append("🌿 养生原则：").append(term.principle).append("\n\n");
        sb.append("🍽️ 饮食建议：").append(term.foodAdvice).append("\n");  // 修复：添加点号
        sb.append("🍲 推荐食谱：").append(term.recipe).append("\n");
        sb.append("💤 起居建议：").append(term.dailyAdvice).append("\n\n");

        // 结合体质的建议
        sb.append(getConstitutionAdviceForSolarTerm(constitution, term.name));

        return sb.toString();
    }
    private String getConstitutionAdviceForSolarTerm(String constitution, String termName) {
        Map<String, Map<String, String>> advice = Map.of(
                "气虚质", Map.of("立春", "可适当增加黄芪、党参炖汤"),
                "阳虚质", Map.of("冬至", "是温补最佳时机，多吃羊肉、生姜"),
                "阴虚质", Map.of("秋分", "秋燥易伤阴，多喝银耳百合汤"),
                "痰湿质", Map.of("谷雨", "湿气重，多吃薏米红豆祛湿"),
                "湿热质", Map.of("夏至", "湿热加重，多喝绿豆汤清热")
        );

        String specific = advice.getOrDefault(constitution, Map.of()).get(termName);
        if (specific != null) {
            return "💡 针对" + constitution + "质的特别提醒：" + specific;
        }
        return "";
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SolarTermInfo {
        private String name;
        private String date;
        private String principle;
        private String foodAdvice;
        private String recipe;
        private String dailyAdvice;
    }
}