package com.basebackend.gateway.enums;

import com.basebackend.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 网关错误码枚举
 * <p>
 * 定义网关模块专用的错误码，继承自通用错误码接口。
 * 错误码范围：6000-6999
 * </p>
 *
 * <h3>错误码段划分：</h3>
 * <ul>
 * <li><b>6000-6099</b>: 认证错误（Token 验证、会话管理）</li>
 * <li><b>6100-6199</b>: 路由错误（服务不可达、路由失败）</li>
 * <li><b>6200-6299</b>: 限流错误（请求限流、熔断降级）</li>
 * <li><b>6300-6399</b>: 灰度发布错误</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Getter
public enum GatewayErrorCode implements ErrorCode {

    // ========== 认证错误（6000-6099）==========

    /**
     * Token 缺失
     */
    TOKEN_MISSING(6000, "认证失败，缺少Token", 401),

    /**
     * Token 无效
     */
    TOKEN_INVALID(6001, "认证失败，Token无效", 401),

    /**
     * Token 已过期（Redis 中不存在）
     */
    TOKEN_EXPIRED(6002, "认证失败，Token已失效", 401),

    /**
     * Token 不匹配（可能在其他设备登录）
     */
    TOKEN_MISMATCH(6003, "认证失败，Token已失效", 401),

    /**
     * 用户 ID 无效
     */
    USER_ID_INVALID(6004, "认证失败，用户信息无效", 401),

    /**
     * 认证服务繁忙
     */
    AUTH_SERVICE_BUSY(6005, "认证失败，服务繁忙，请稍后重试", 503),

    /**
     * 认证服务异常
     */
    AUTH_SERVICE_ERROR(6006, "认证失败，服务异常", 500),

    // ========== 路由错误（6100-6199）==========

    /**
     * 服务不可达
     */
    SERVICE_UNAVAILABLE(6100, "服务暂时不可用", 503),

    /**
     * 路由未找到
     */
    ROUTE_NOT_FOUND(6101, "请求的服务不存在", 404),

    /**
     * 服务实例未找到
     */
    SERVICE_INSTANCE_NOT_FOUND(6102, "没有可用的服务实例", 503),

    /**
     * 上游服务超时
     */
    UPSTREAM_TIMEOUT(6103, "上游服务响应超时", 504),

    /**
     * 上游服务返回错误
     */
    UPSTREAM_ERROR(6104, "上游服务返回错误", 502),

    // ========== 限流错误（6200-6299）==========

    /**
     * 请求被限流
     */
    RATE_LIMITED(6200, "请求过于频繁，请稍后再试", 429),

    /**
     * IP 被限流
     */
    IP_RATE_LIMITED(6201, "您的 IP 请求过于频繁，请稍后再试", 429),

    /**
     * 用户被限流
     */
    USER_RATE_LIMITED(6202, "您的请求过于频繁，请稍后再试", 429),

    /**
     * 接口被限流
     */
    API_RATE_LIMITED(6203, "该接口请求过于频繁，请稍后再试", 429),

    /**
     * 服务熔断
     */
    CIRCUIT_BREAKER_OPEN(6210, "服务熔断中，请稍后再试", 503),

    /**
     * 服务降级
     */
    SERVICE_DEGRADED(6211, "服务降级中，部分功能暂时不可用", 503),

    // ========== 灰度发布错误（6300-6399）==========

    /**
     * 灰度版本不存在
     */
    GRAY_VERSION_NOT_FOUND(6300, "灰度版本不存在", 503),

    /**
     * 灰度规则配置错误
     */
    GRAY_RULE_ERROR(6301, "灰度规则配置错误", 500),

    // ========== 请求验证错误（6400-6499）==========

    /**
     * 签名验证失败
     */
    SIGNATURE_INVALID(6400, "请求签名验证失败", 401),

    /**
     * 请求已过期（时间戳超时）
     */
    REQUEST_EXPIRED(6401, "请求已过期", 401),

    /**
     * 请求重复（防重放）
     */
    REQUEST_DUPLICATE(6402, "请求已处理，请勿重复提交", 409),

    /**
     * 缺少必要的请求参数
     */
    MISSING_REQUIRED_PARAM(6403, "缺少必要的请求参数", 400),

    /**
     * 幂等性键无效
     */
    IDEMPOTENCY_KEY_INVALID(6404, "幂等性键无效", 400);

    /**
     * 模块命名空间
     */
    private static final String MODULE = "gateway";

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * HTTP 状态码
     */
    private final Integer httpStatus;

    /**
     * 构造函数
     *
     * @param code       错误码
     * @param message    错误消息
     * @param httpStatus HTTP 状态码
     */
    GatewayErrorCode(Integer code, String message, Integer httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus != null ? httpStatus : ErrorCode.super.getHttpStatus();
    }

    @Override
    public String getModule() {
        return MODULE;
    }
}
