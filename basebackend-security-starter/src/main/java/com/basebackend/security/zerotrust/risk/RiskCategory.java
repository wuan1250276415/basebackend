package com.basebackend.security.zerotrust.risk;

/**
 * 风险因子分类
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public enum RiskCategory {
    BEHAVIOR("行为风险"),
    NETWORK("网络风险"),
    DEVICE("设备风险"),
    TIME("时间风险"),
    LOCATION("位置风险");

    private final String description;

    RiskCategory(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
