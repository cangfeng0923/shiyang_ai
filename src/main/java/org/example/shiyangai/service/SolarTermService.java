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
public class SolarTermService {

    private final WeatherService weatherService;
    private final AIService aiService;

    private static final Map<String, String[]> SOLAR_TERM_DATES = new HashMap<>();

    static {
        SOLAR_TERM_DATES.put("立春", new String[]{"2", "3"});
        SOLAR_TERM_DATES.put("雨水", new String[]{"2", "18"});
        SOLAR_TERM_DATES.put("惊蛰", new String[]{"3", "5"});
        SOLAR_TERM_DATES.put("春分", new String[]{"3", "20"});
        SOLAR_TERM_DATES.put("清明", new String[]{"4", "4"});
        SOLAR_TERM_DATES.put("谷雨", new String[]{"4", "20"});
        SOLAR_TERM_DATES.put("立夏", new String[]{"5", "5"});
        SOLAR_TERM_DATES.put("小满", new String[]{"5", "21"});
        SOLAR_TERM_DATES.put("芒种", new String[]{"6", "5"});
        SOLAR_TERM_DATES.put("夏至", new String[]{"6", "21"});
        SOLAR_TERM_DATES.put("小暑", new String[]{"7", "7"});
        SOLAR_TERM_DATES.put("大暑", new String[]{"7", "22"});
        SOLAR_TERM_DATES.put("立秋", new String[]{"8", "7"});
        SOLAR_TERM_DATES.put("处暑", new String[]{"8", "23"});
        SOLAR_TERM_DATES.put("白露", new String[]{"9", "7"});
        SOLAR_TERM_DATES.put("秋分", new String[]{"9", "22"});
        SOLAR_TERM_DATES.put("寒露", new String[]{"10", "8"});
        SOLAR_TERM_DATES.put("霜降", new String[]{"10", "23"});
        SOLAR_TERM_DATES.put("立冬", new String[]{"11", "7"});
        SOLAR_TERM_DATES.put("小雪", new String[]{"11", "22"});
        SOLAR_TERM_DATES.put("大雪", new String[]{"12", "7"});
        SOLAR_TERM_DATES.put("冬至", new String[]{"12", "21"});
        SOLAR_TERM_DATES.put("小寒", new String[]{"1", "5"});
        SOLAR_TERM_DATES.put("大寒", new String[]{"1", "20"});
    }

    public SolarTermInfo getCurrentSolarTerm() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();

        String[] order = {"立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
                "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
                "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
                "立冬", "小雪", "大雪", "冬至", "小寒", "大寒"};

        for (int i = 0; i < order.length; i++) {
            String name = order[i];
            String[] dates = SOLAR_TERM_DATES.get(name);
            if (dates == null) continue;

            int termMonth = Integer.parseInt(dates[0]);
            int termDay = Integer.parseInt(dates[1]);

            if (month == termMonth && day == termDay) {
                return new SolarTermInfo(name, getClimateDesc(name));
            }
        }
        return new SolarTermInfo(getSeasonName(month), getSeasonClimate(month));
    }

    private String getClimateDesc(String termName) {
        Map<String, String> map = new HashMap<>();
        map.put("立春", "东风解冻，万物复苏");
        map.put("雨水", "降水增多，湿气渐重");
        map.put("惊蛰", "春雷始鸣，万物复苏");
        map.put("春分", "昼夜平分，阴阳平衡");
        map.put("清明", "气清景明，万物皆显");
        map.put("谷雨", "雨生百谷，湿气加重");
        map.put("立夏", "夏季开始，万物繁茂");
        map.put("小满", "麦类灌浆，湿热渐起");
        map.put("芒种", "麦类成熟，湿热交加");
        map.put("夏至", "白昼最长，阳气最盛");
        map.put("小暑", "开始炎热，入伏");
        map.put("大暑", "最热时节，湿热交蒸");
        map.put("立秋", "秋季开始，暑去凉来");
        map.put("处暑", "暑气渐消，秋燥渐起");
        map.put("白露", "天气转凉，露水变白");
        map.put("秋分", "昼夜平分，阴阳平衡");
        map.put("寒露", "露水变寒，天气转冷");
        map.put("霜降", "开始降霜，寒意渐浓");
        map.put("立冬", "冬季开始，万物收藏");
        map.put("小雪", "开始降雪，寒气渐重");
        map.put("大雪", "雪量增大，寒气更重");
        map.put("冬至", "白昼最短，一阳初生");
        map.put("小寒", "开始寒冷，三九寒天");
        map.put("大寒", "最冷时节，寒气至极");
        return map.getOrDefault(termName, "节气交替，注意养生");
    }

    private String getSeasonName(int month) {
        if (month >= 3 && month <= 5) return "春季";
        if (month >= 6 && month <= 8) return "夏季";
        if (month >= 9 && month <= 11) return "秋季";
        return "冬季";
    }

    private String getSeasonClimate(int month) {
        if (month >= 3 && month <= 5) return "春回大地，阳气升发";
        if (month >= 6 && month <= 8) return "夏日炎炎，阳气最盛";
        if (month >= 9 && month <= 11) return "秋高气爽，燥邪当令";
        return "天寒地冻，寒气袭人";
    }

    public String getCurrentSolarTermName() {
        return getCurrentSolarTerm().getName();
    }

    public String getNextSolarTermName() {
        String current = getCurrentSolarTermName();
        String[] order = {"立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
                "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
                "立秋", "处暑", "白露", "秋分", "寒露", "霜降",
                "立冬", "小雪", "大雪", "冬至", "小寒", "大寒"};
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(current) && i + 1 < order.length) {
                return order[i + 1];
            }
        }
        return "立春";
    }

    public int getDaysToNextSolarTerm() {
        LocalDate now = LocalDate.now();
        String nextName = getNextSolarTermName();
        String[] dates = SOLAR_TERM_DATES.get(nextName);
        if (dates == null) return 0;

        int targetMonth = Integer.parseInt(dates[0]);
        int targetDay = Integer.parseInt(dates[1]);
        int currentYear = now.getYear();

        LocalDate targetDate = LocalDate.of(currentYear, targetMonth, targetDay);
        if (targetDate.isBefore(now)) {
            targetDate = LocalDate.of(currentYear + 1, targetMonth, targetDay);
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(now, targetDate);
    }

    public String getSolarTermAdviceAI(String userId, String constitution, String city) {
        SolarTermInfo term = getCurrentSolarTerm();
        WeatherService.WeatherInfo weather = weatherService.getWeather(city);
        return aiService.generateSolarTermAdvice(term.getName(), term.getClimateDesc(), constitution, weather);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SolarTermInfo {
        private String name;
        private String climateDesc;
    }
}