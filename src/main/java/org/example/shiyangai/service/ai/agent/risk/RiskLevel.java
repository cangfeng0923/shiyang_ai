// RiskLevel.java
package org.example.shiyangai.service.ai.agent.risk;

public enum RiskLevel {
    LOW("低风险", 0),
    MEDIUM("中风险", 1),
    HIGH("高风险", 2),
    CRITICAL("严重风险", 3);

    private final String description;
    private final int code;

    RiskLevel(String description, int code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() { return description; }
    public int getCode() { return code; }

    public boolean isHighOrAbove() {
        return this == HIGH || this == CRITICAL;
    }
}