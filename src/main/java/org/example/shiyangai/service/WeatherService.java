package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WeatherService {

    @Value("${weather.api.key:}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public WeatherInfo getWeather(String city) {
        WeatherInfo info = new WeatherInfo();
        info.setCity(city);

        if (apiKey == null || apiKey.isEmpty()) {
            return getMockWeather(city);
        }

        try {
            String encodedCity = java.net.URLEncoder.encode(city, "UTF-8");
            String url = String.format("https://api.seniverse.com/v3/weather/daily.json?key=%s&location=%s&language=zh-Hans&unit=c&start=0&days=3",
                    apiKey, encodedCity);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("results") && json.get("results").size() > 0) {
                JsonNode result = json.get("results").get(0);
                info.setCity(result.path("location").path("name").asText());

                JsonNode daily = result.path("daily");
                List<DailyWeather> dailyList = new ArrayList<>();

                for (int i = 0; i < daily.size() && i < 3; i++) {
                    JsonNode day = daily.get(i);
                    DailyWeather dailyWeather = new DailyWeather();
                    dailyWeather.setDate(day.path("date").asText());
                    dailyWeather.setWeek(i == 0 ? "今天" : (i == 1 ? "明天" : "后天"));
                    dailyWeather.setDayTemp(day.path("high").asText() + "°C");
                    dailyWeather.setNightTemp(day.path("low").asText() + "°C");
                    dailyWeather.setDayWeather(translateWeather(day.path("text_day").asText()));
                    dailyWeather.setNightWeather(translateWeather(day.path("text_night").asText()));
                    dailyWeather.setDayWind(day.path("wind_direction").asText());
                    dailyList.add(dailyWeather);
                }
                info.setDailyWeather(dailyList);

                if (!dailyList.isEmpty()) {
                    info.setTodayTemp(dailyList.get(0).getDayTemp());
                    info.setTodayWeather(dailyList.get(0).getDayWeather());
                    info.setTodayWind(dailyList.get(0).getDayWind());
                }
                if (dailyList.size() > 1) {
                    info.setTomorrowTemp(dailyList.get(1).getDayTemp());
                    info.setTomorrowWeather(dailyList.get(1).getDayWeather());
                }
                if (dailyList.size() > 2) {
                    info.setAfterTomorrowTemp(dailyList.get(2).getDayTemp());
                    info.setAfterTomorrowWeather(dailyList.get(2).getDayWeather());
                }
                return info;
            }
        } catch (Exception e) {
            log.warn("获取天气失败: {}", e.getMessage());
        }

        return getMockWeather(city);
    }

    private String translateWeather(String weatherEn) {
        if (weatherEn == null) return "未知";
        switch (weatherEn) {
            case "Sunny": return "晴";
            case "Clear": return "晴";
            case "Partly Cloudy": return "多云";
            case "Cloudy": return "阴";
            case "Overcast": return "阴";
            case "Light Rain": return "小雨";
            case "Moderate Rain": return "中雨";
            case "Heavy Rain": return "大雨";
            case "Rain": return "雨";
            case "Thunderstorm": return "雷阵雨";
            case "Snow": return "雪";
            case "Light Snow": return "小雪";
            case "Moderate Snow": return "中雪";
            case "Heavy Snow": return "大雪";
            case "Fog": return "雾";
            case "Haze": return "霾";
            default: return weatherEn;
        }
    }

    private WeatherInfo getMockWeather(String city) {
        WeatherInfo info = new WeatherInfo();
        info.setCity(city);
        info.setTodayTemp("22°C");
        info.setTodayWeather("晴");
        info.setTodayWind("东南风2级");

        List<DailyWeather> dailyList = new ArrayList<>();
        String[] weekdays = {"今天", "明天", "后天"};
        String[] temps = {"22°C", "24°C", "20°C"};
        String[] weathers = {"晴", "多云", "小雨"};

        for (int i = 0; i < 3; i++) {
            DailyWeather daily = new DailyWeather();
            daily.setWeek(weekdays[i]);
            daily.setDayTemp(temps[i]);
            daily.setDayWeather(weathers[i]);
            dailyList.add(daily);
        }
        info.setDailyWeather(dailyList);
        return info;
    }

    public String getWeatherAdvice(WeatherInfo weather, String constitution) {
        if (weather == null || weather.getTodayWeather() == null) {
            return "天气信息获取中，建议根据实际天气调整饮食。";
        }

        String todayWeather = weather.getTodayWeather();
        StringBuilder sb = new StringBuilder();

        if (todayWeather.contains("雨")) {
            sb.append("🌧️ 今日有雨，湿气较重。");
            if ("痰湿质".equals(constitution) || "湿热质".equals(constitution)) {
                sb.append("建议喝薏米红豆汤祛湿。");
            } else {
                sb.append("建议喝姜枣茶驱寒祛湿。");
            }
        } else if (todayWeather.contains("雪")) {
            sb.append("❄️ 今日有雪，注意保暖。");
            if ("阳虚质".equals(constitution)) {
                sb.append("建议吃羊肉、生姜温补。");
            }
        } else if (todayWeather.contains("晴")) {
            sb.append("☀️ 天气晴朗，适合户外活动。");
        } else if (todayWeather.contains("多云")) {
            sb.append("⛅ 天气宜人，保持好心情。");
        } else {
            sb.append("🌡️ 请根据天气适当调整饮食。");
        }

        return sb.toString();
    }

    @lombok.Data
    public static class WeatherInfo {
        private String city;
        private String todayTemp;
        private String todayWeather;
        private String todayWind;
        private String tomorrowTemp;
        private String tomorrowWeather;
        private String afterTomorrowTemp;
        private String afterTomorrowWeather;
        private List<DailyWeather> dailyWeather;
    }

    @lombok.Data
    public static class DailyWeather {
        private String date;
        private String week;
        private String dayTemp;
        private String nightTemp;
        private String dayWeather;
        private String nightWeather;
        private String dayWind;
    }
}