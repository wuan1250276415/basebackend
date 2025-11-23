package com.basebackend.security.zerotrust.risk;

/**
 * 风险因子类型
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public enum RiskType {
    EXCESSIVE_LOGIN_ATTEMPTS("登录尝试次数过多"),
    IP_CHANGE("IP地址发生变化"),
    GEOGRAPHIC_CHANGE("地理位置发生变化"),
    UNKNOWN_DEVICE("未知设备"),
    ACCESS_TIME_ANOMALY("访问时间异常"),
    HIGH_REQUEST_FREQUENCY("请求频率过高"),
    SUSPICIOUS_BEHAVIOR("可疑行为"),
    RATE_LIMIT_EXCEEDED("超过频率限制"),
    ANOMALOUS_LOCATION("异常位置"),
    MULTIPLE_FAILED_ATTEMPTS("多次失败尝试");

    private final String description;

    RiskType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
