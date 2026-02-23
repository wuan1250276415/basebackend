package com.basebackend.jwt;

/**
 * JWT 审计事件类型枚举
 */
public enum JwtAuditEvent {

    /** Token 签发 */
    TOKEN_GENERATED,

    /** Token 刷新 */
    TOKEN_REFRESHED,

    /** Token 吊销 */
    TOKEN_REVOKED,

    /** Token 过期被拒 */
    TOKEN_EXPIRED,

    /** 验证失败（含失败原因） */
    VALIDATION_FAILED,

    /** 设备注册 */
    DEVICE_REGISTERED,

    /** 设备被踢下线 */
    DEVICE_KICKED,

    /** 可疑活动（异地登录、频繁刷新等） */
    SUSPICIOUS_ACTIVITY
}
