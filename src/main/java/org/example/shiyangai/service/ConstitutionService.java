package org.example.shiyangai.service;


import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.enums.ConstitutionType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ConstitutionService {

    /**
     * 10题快速体质测评算法
     * 题目对应体质类型索引：
     * 0气虚 1阳虚 2阴虚 3痰湿 4湿热 5血瘀 6气郁 7特禀
     */
    public String quickAssessment(boolean[] answers) {
        if (answers == null || answers.length < 10) {
            log.warn("测评答案不足10题");
            return "平和质";
        }

        // 8种偏颇体质的得分
        int[] scores = new int[8];

        // 题目映射（每题对应哪种体质）
        int[] questionMapping = {
                0,  // 0: 您是否经常感觉疲劳、气短？ → 气虚质
                1,  // 1: 您是否怕冷、手脚冰凉？ → 阳虚质
                2,  // 2: 您是否经常感觉口干舌燥、手脚心热？ → 阴虚质
                3,  // 3: 您是否感觉身体沉重、容易犯困？ → 痰湿质
                4,  // 4: 您是否脸上容易出油、长痘？ → 湿热质
                5,  // 5: 您是否面色晦暗、容易长斑？ → 血瘀质
                6,  // 6: 您是否容易情绪低落、焦虑？ → 气郁质
                7,  // 7: 您是否容易过敏（打喷嚏、皮疹）？ → 特禀质
                0,  // 8: 您是否怕风、容易感冒？ → 气虚质
                2   // 9: 您是否腰膝酸软、记忆力下降？ → 阴虚质
        };

        // 计分
        for (int i = 0; i < answers.length && i < questionMapping.length; i++) {
            if (answers[i]) {
                scores[questionMapping[i]]++;
            }
        }

        // 找出最高分
        int maxScore = 0;
        int maxIndex = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxIndex = i;
            }
        }

        // 如果所有得分都是0，返回平和质
        if (maxIndex == -1 || maxScore == 0) {
            return "平和质";
        }

        String[] typeNames = {"气虚质", "阳虚质", "阴虚质", "痰湿质",
                "湿热质", "血瘀质", "气郁质", "特禀质"};

        log.info("测评结果: {} (得分: {})", typeNames[maxIndex], maxScore);
        return typeNames[maxIndex];
    }

    /**
     * 获取体质的所有宜忌规则（用于前端展示）
     */
    public Map<String, Object> getConstitutionRules(String constitutionName) {
        ConstitutionType type = ConstitutionType.fromName(constitutionName);

        Map<String, Object> rules = new HashMap<>();
        rules.put("name", type.getName());
        rules.put("description", type.getDescription());
        rules.put("goodFoods", type.getGoodFoods());
        rules.put("badFoods", type.getBadFoods());

        return rules;
    }

    /**
     * 获取所有体质类型列表
     */
    public String[] getAllConstitutions() {
        return new String[]{"平和质", "气虚质", "阳虚质", "阴虚质",
                "痰湿质", "湿热质", "血瘀质", "气郁质", "特禀质"};
    }
}
