package com.basebackend.security.zerotrust.device;

/**
 * 信任等级枚举
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public enum TrustLevel {
    LOW("低风险", "允许访问"),
    MEDIUM("中等风险", "需要额外验证"),
    HIGH("高风险", "限制访问"),
    CRITICAL("严重风险", "拒绝访问");

    private final String description;
    private final String action;

    TrustLevel(String description, String action) {
        this.description = description;
        this.action = action;
    }

    public String getDescription() { return description; }
    public String getAction() { return action; }

    /**
     * 根据分数获取信任等级
     *
     * @param score 信任度分数
     * @return TrustLevel
     */
    public static TrustLevel fromScore(int score) {
        if (score >= 80) return LOW;
        if (score >= 60) return MEDIUM;
        if (score >= 40) return HIGH;
        return CRITICAL;
    }
}
