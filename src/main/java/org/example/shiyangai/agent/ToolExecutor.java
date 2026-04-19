package org.example.shiyangai.agent;

import java.util.Map;

//工具执行器接口
public interface ToolExecutor {
    String execute(Map<String, Object> parameters);
    String getName();
    String getDescription();
}