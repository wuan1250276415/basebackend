package com.basebackend.security.event;

/**
 * 安全审计事件类型枚举
 */
public enum SecurityEventType {

    /** 认证成功 */
    AUTHENTICATION_SUCCESS,

    /** 认证失败（Token 无效/过期） */
    AUTHENTICATION_FAILURE,

    /** Token 被黑名单拒绝 */
    TOKEN_BLACKLISTED,

    /** Token 加入黑名单 */
    TOKEN_ADDED_TO_BLACKLIST,

    /** 用户被强制下线 */
    FORCE_LOGOUT,

    /** 权限校验被拒绝 */
    ACCESS_DENIED,

    /** 速率限制触发 */
    RATE_LIMIT_EXCEEDED,

    /** Origin 校验失败 */
    ORIGIN_REJECTED,

    /** 认证服务不可用（Redis 等依赖故障） */
    AUTH_SERVICE_UNAVAILABLE
}
