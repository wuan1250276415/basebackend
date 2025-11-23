package com.basebackend.security.zerotrust.risk;

/**
 * 风险严重程度
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public enum RiskSeverity {
    LOW("低", 10),
    MEDIUM("中", 30),
    HIGH("高", 60),
    CRITICAL("严重", 90);

    private final String description;
    private final int defaultScore;

    RiskSeverity(String description, int defaultScore) {
        this.description = description;
        this.defaultScore = defaultScore;
    }

    public String getDescription() { return description; }
    public int getDefaultScore() { return defaultScore; }
}
