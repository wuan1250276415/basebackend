package com.basebackend.security.zerotrust.risk;

/**
 * 风险等级
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public enum RiskLevel {
    LOW(0, 30, "低风险"),
    MEDIUM(31, 60, "中等风险"),
    HIGH(61, 80, "高风险"),
    CRITICAL(81, 100, "极高风险");

    private final int minScore;
    private final int maxScore;
    private final String description;

    RiskLevel(int minScore, int maxScore, String description) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.description = description;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public String getDescription() {
        return description;
    }

    public static RiskLevel fromScore(int score) {
        for (RiskLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return CRITICAL;
    }
}
