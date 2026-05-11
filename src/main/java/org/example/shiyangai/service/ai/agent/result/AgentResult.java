// AgentResult.java
package org.example.shiyangai.service.ai.agent.result;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AgentResult {
    private String agentName;
    private int priority;
    private List<String> observations;
    private List<String> analysis;
    private List<String> recommendations;
    private String riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL

    public AgentResult(String agentName, int priority) {
        this.agentName = agentName;
        this.priority = priority;
        this.observations = new ArrayList<>();
        this.analysis = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.riskLevel = "LOW";
    }

    public void addObservation(String obs) {
        observations.add(obs);
    }

    public void addAnalysis(String ana) {
        analysis.add(ana);
    }

    public void addRecommendation(String rec) {
        recommendations.add(rec);
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);
    }
}