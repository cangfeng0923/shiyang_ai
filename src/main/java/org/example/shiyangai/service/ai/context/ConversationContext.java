// ConversationContext.java（修改版 - 添加 agentResults 支持）
package org.example.shiyangai.service.ai.context;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.example.shiyangai.entity.ChatHistory;
import org.example.shiyangai.entity.HealthProfile;
import org.example.shiyangai.entity.DietRecord;
import org.example.shiyangai.entity.IngredientInfo;
import org.example.shiyangai.service.ai.agent.result.AgentResult;
import org.example.shiyangai.service.ai.agent.risk.RiskLevel;

import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    // 基础信息
    private String userId;
    private String userMessage;
    private String reply;
    private String constitution;

    // 用户数据（聚合）
    private UserData userData;

    // 健康数据
    private HealthProfile profile;
    private List<DietRecord> todayRecords;
    private List<DietRecord> weekRecords;

    // 食材和症状
    private IngredientInfo foodInfo;
    private String foodName;
    private String symptom;
    private String symptomAdvice;

    // 对话记忆
    private List<ChatHistory> history;
    private String previousContext;

    // 流程控制
    private String solarTermAdvice;
    private String systemPrompt;
    private boolean askingForReport;
    private boolean shortCircuit;
    private List<String> executedAgents;

    // 风险控制字段
    private RiskLevel riskLevel;
    private List<String> warnings;

    // ========== 新增：Agent 决策结果聚合 ==========
    private List<AgentResult> agentResults;

    // ========== 新增：汇总后的最终建议 ==========
    private String finalRecommendation;

    // 便捷方法
    public String getUserName() {
        return userData != null ? userData.getUserName() : null;
    }

    public Integer getUserAge() {
        return userData != null ? userData.getAge() : null;
    }

    public String getUserGender() {
        return userData != null ? userData.getGender() : null;
    }

    public void setUserName(String name) {
        if (userData == null) userData = new UserData();
        userData.setUserName(name);
    }

    public void setUserAge(Integer age) {
        if (userData == null) userData = new UserData();
        userData.setAge(age);
    }

    public void setUserGender(String gender) {
        if (userData == null) userData = new UserData();
        userData.setGender(gender);
    }

    public void addExecutedAgent(String agentName) {
        if (executedAgents == null) {
            executedAgents = new ArrayList<>();
        }
        executedAgents.add(agentName);
    }

    // 添加 Agent 决策结果
    public void addAgentResult(AgentResult result) {
        if (agentResults == null) {
            agentResults = new ArrayList<>();
        }
        agentResults.add(result);

        // 更新风险等级（取最高）
        if (result.isHighRisk()) {
            this.riskLevel = RiskLevel.HIGH;
            this.shortCircuit = true;
        }
    }

    // 获取所有高风险结果
    public List<AgentResult> getHighRiskResults() {
        if (agentResults == null) return new ArrayList<>();
        return agentResults.stream()
                .filter(AgentResult::isHighRisk)
                .toList();
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    public boolean isHighRisk() {
        return riskLevel != null && riskLevel.isHighOrAbove();
    }

    @Data
    public static class UserData {
        private String userName;
        private Integer age;
        private String gender;
    }
}