package org.example.shiyangai.agent.tools;

import org.example.shiyangai.agent.Tool;
import org.example.shiyangai.agent.ToolExecutor;
import org.springframework.stereotype.Component;
import java.util.*;

//中医知识查询工具
@Component
public class ChineseMedicineTool implements ToolExecutor {

    private static final Map<String, String> KNOWLEDGE = new HashMap<>();

    static {
        KNOWLEDGE.put("上火", "上火分实火和虚火。实火可吃西瓜、苦瓜；虚火可吃梨、百合");
        KNOWLEDGE.put("湿气", "湿气重可吃薏米红豆粥，配合运动出汗");
        KNOWLEDGE.put("便秘", "阴虚便秘吃蜂蜜、黑芝麻；气虚便秘吃山药、黄芪");
        KNOWLEDGE.put("失眠", "心肾不交可吃酸枣仁、百合；肝火旺可喝菊花茶");
        KNOWLEDGE.put("疲劳", "气虚疲劳吃黄芪、大枣；湿困疲劳吃薏米、陈皮");
    }

    @Override
    @Tool(name = "tcm_knowledge",
            description = "查询中医知识，如症状对应的调理方法",
            parameters = {"symptom"})
    public String execute(Map<String, Object> parameters) {
        String symptom = (String) parameters.get("symptom");

        for (Map.Entry<String, String> entry : KNOWLEDGE.entrySet()) {
            if (symptom.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "关于'" + symptom + "'的建议：建议咨询专业中医师进行辨证论治";
    }

    @Override
    public String getName() { return "tcm_knowledge"; }

    @Override
    public String getDescription() {
        return "查询中医相关知识，输入症状名称返回调理建议";
    }
}