package org.example.shiyangai.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class IngredientInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;              // 食材名称
    private String property;          // 性：寒/凉/平/温/热
    private String flavor;            // 味：酸/苦/甘/辛/咸
    private String meridian;          // 归经：心/肝/脾/肺/肾
    private String effect;            // 功效
    private String contraindication;  // 禁忌
    private Integer calories;         // 热量（千卡/100g）
    private Double protein;           // 蛋白质(g)
    private Double carbs;             // 碳水(g)
    private Double fat;               // 脂肪(g)
    private String source;            // 数据来源
    private Long updateTime;          // 更新时间
    private Integer confidence;       // 置信度(0-100)

    // 中医适宜性评分
    public int getSuitabilityScore(String constitution) {
        Map<String, Map<String, Integer>> scoreMap = Map.ofEntries(
                Map.entry("寒", Map.of("阳虚质", -10, "阴虚质", 5, "湿热质", 5)),
                Map.entry("凉", Map.of("阳虚质", -5, "阴虚质", 3, "湿热质", 3)),
                Map.entry("温", Map.of("阳虚质", 10, "阴虚质", -5, "湿热质", -3)),
                Map.entry("热", Map.of("阳虚质", 15, "阴虚质", -10, "湿热质", -8))
        );

        Map<String, Integer> propScore = scoreMap.getOrDefault(property, Map.of());
        return propScore.getOrDefault(constitution, 0);
    }
}