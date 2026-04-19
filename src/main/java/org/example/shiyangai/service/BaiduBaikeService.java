package org.example.shiyangai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shiyangai.entity.IngredientInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class BaiduBaikeService {

    private final HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    public BaiduBaikeService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 获取食材的中医属性
     */
    public IngredientInfo getIngredientInfo(String foodName) throws Exception {
        // 获取百度百科词条ID
        String lemmaId = getLemmaId(foodName);
        if (lemmaId == null) {
            // 返回默认信息
            IngredientInfo defaultInfo = new IngredientInfo();
            defaultInfo.setName(foodName);
            defaultInfo.setProperty("平");
            defaultInfo.setFlavor("甘");
            defaultInfo.setEffect("信息待补充");
            defaultInfo.setContraindication("暂无特殊禁忌");
            return defaultInfo;
        }

        // 调用百度百科API
        String url = "https://baike.baidu.com/api/lemma?lemma_id=" + lemmaId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());

        // 解析中医属性
        IngredientInfo info = new IngredientInfo();
        info.setName(foodName);
        info.setProperty(extractProperty(json));        // 寒/凉/平/温/热
        info.setFlavor(extractFlavor(json));            // 酸/苦/甘/辛/咸
        info.setEffect(extractEffect(json));            // 功效
        info.setContraindication(extractContraindication(json)); // 禁忌

        return info;
    }

    /**
     * 获取百度百科词条ID
     */
    private String getLemmaId(String foodName) throws Exception {
        // 搜索词条
        String searchUrl = "https://baike.baidu.com/api/lemma?lemma_id=&search=" +
                java.net.URLEncoder.encode(foodName, "UTF-8");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());

        // 获取第一个匹配的词条ID
        JsonNode lemmaList = json.path("lemmaList");
        if (lemmaList.isArray() && lemmaList.size() > 0) {
            return lemmaList.get(0).path("lemmaId").asText();
        }

        // 尝试直接搜索
        JsonNode lemmaSummary = json.path("lemmaSummary");
        if (!lemmaSummary.isMissingNode() && lemmaSummary.has("lemmaId")) {
            return lemmaSummary.path("lemmaId").asText();
        }

        return null;
    }

    /**
     * 提取食物属性（寒/凉/平/温/热）
     */
    private String extractProperty(JsonNode json) {
        String content = getContentText(json);

        if (content.contains("性寒") || content.contains("性大寒")) return "寒";
        if (content.contains("性凉") || content.contains("微寒")) return "凉";
        if (content.contains("性温") || content.contains("微温")) return "温";
        if (content.contains("性热") || content.contains("大热")) return "热";

        return "平";
    }

    /**
     * 提取食物味道（酸/苦/甘/辛/咸）
     */
    private String extractFlavor(JsonNode json) {
        String content = getContentText(json);

        if (content.contains("味酸") || content.contains("酸味")) return "酸";
        if (content.contains("味苦") || content.contains("苦味")) return "苦";
        if (content.contains("味甘") || content.contains("甘味")) return "甘";
        if (content.contains("味辛") || content.contains("辛味")) return "辛";
        if (content.contains("味咸") || content.contains("咸味")) return "咸";

        return "甘"; // 默认甘味
    }

    /**
     * 提取功效
     */
    private String extractEffect(JsonNode json) {
        String content = getContentText(json);

        // 常见功效关键词
        if (content.contains("功效")) {
            int start = content.indexOf("功效");
            int end = Math.min(content.indexOf("。", start) + 1, content.length());
            if (end > start) {
                return content.substring(start, end);
            }
        }

        // 从摘要中提取
        String summary = json.path("lemmaSummary").asText();
        if (summary.length() > 100) {
            return summary.substring(0, 100);
        }

        return "信息待补充";
    }

    /**
     * 提取禁忌
     */
    private String extractContraindication(JsonNode json) {
        String content = getContentText(json);

        if (content.contains("禁忌")) {
            int start = content.indexOf("禁忌");
            int end = Math.min(content.indexOf("。", start) + 1, content.length());
            if (end > start) {
                return content.substring(start, end);
            }
        }

        if (content.contains("不宜")) {
            int start = content.indexOf("不宜");
            int end = Math.min(content.indexOf("。", start) + 1, content.length());
            if (end > start) {
                return content.substring(start, end);
            }
        }

        return "暂无特殊禁忌";
    }

    /**
     * 获取百科内容文本
     */
    private String getContentText(JsonNode json) {
        // 尝试多个可能的字段
        String content = json.path("lemmaSummary").asText();
        if (!content.isEmpty()) return content;

        content = json.path("abstract").asText();
        if (!content.isEmpty()) return content;

        content = json.path("description").asText();
        if (!content.isEmpty()) return content;

        return "";
    }
}