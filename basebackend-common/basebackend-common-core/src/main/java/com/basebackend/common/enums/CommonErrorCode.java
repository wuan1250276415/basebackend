package com.basebackend.common.enums;

import lombok.Getter;

/**
 * 通用错误码枚举
 * <p>
 * 定义系统级别的通用错误码，适用于所有模块。
 * 各业务模块可以定义自己的错误码枚举（实现 {@link ErrorCode} 接口）。
 * </p>
 *
 * <h3>错误码段划分：</h3>
 * <ul>
 *   <li><b>100-599</b>: HTTP 标准错误码（如 200 成功、400 参数错误、401 未授权、500 服务器错误）</li>
 *   <li><b>1000-1999</b>: 通用业务错误（参数校验、数据状态、并发冲突、序列化等）</li>
 *   <li><b>2000-2999</b>: 认证/JWT 错误（Token 过期、无效、刷新令牌等）</li>
 *   <li><b>4000-4999</b>: 租户错误（租户不存在、已禁用、已过期等）</li>
 *   <li><b>5000-5999</b>: 文件/存储错误（文件操作、存储服务异常等）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Getter
public enum CommonErrorCode implements ErrorCode {

    // ========== HTTP 标准错误码 ==========
    /**
     * 请求成功
     */
    SUCCESS(200, "请求成功"),

    /**
     * 请求参数错误
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 未授权访问，需要登录
     */
    UNAUTHORIZED(401, "未授权访问，请先登录"),

    /**
     * 禁止访问，权限不足
     */
    FORBIDDEN(403, "权限不足，禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "请求的资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 资源冲突（如重复创建）
     */
    CONFLICT(409, "资源冲突"),

    /**
     * 请求频率过高（限流）
     */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 服务暂时不可用
     */
    SERVICE_UNAVAILABLE(503, "服务暂时不可用，请稍后再试"),

    /**
     * 网关超时
     */
    GATEWAY_TIMEOUT(504, "网关超时"),

    // ========== 业务通用错误码（1000-1999）==========
    /**
     * 操作失败
     */
    OPERATION_FAILED(1000, "操作失败"),

    /**
     * 参数校验失败
     */
    PARAM_VALIDATION_FAILED(1001, "参数校验失败"),

    /**
     * 参数不能为空
     */
    PARAM_NOT_NULL(1002, "参数不能为空"),

    /**
     * 参数格式错误
     */
    PARAM_FORMAT_ERROR(1003, "参数格式错误"),

    /**
     * 参数值超出范围
     */
    PARAM_OUT_OF_RANGE(1004, "参数值超出有效范围"),

    /**
     * 数据不存在
     */
    DATA_NOT_FOUND(1010, "数据不存在", 404),

    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(1011, "数据已存在", 409),

    /**
     * 数据状态异常
     */
    DATA_STATUS_ERROR(1012, "数据状态异常"),

    /**
     * 数据已被删除
     */
    DATA_DELETED(1013, "数据已被删除", 410),

    /**
     * 资源不存在（别名，用于 RESTful API）
     */
    RESOURCE_NOT_FOUND(1014, "资源不存在", 404),

    /**
     * 资源已存在（别名，用于 RESTful API）
     */
    RESOURCE_ALREADY_EXISTS(1015, "资源已存在", 409),

    /**
     * 业务规则校验失败
     */
    BUSINESS_RULE_VIOLATION(1020, "业务规则校验失败"),

    /**
     * 并发冲突
     */
    CONCURRENT_CONFLICT(1021, "并发操作冲突，请重试", 409),

    /**
     * 幂等性检查失败
     */
    IDEMPOTENT_CHECK_FAILED(1022, "重复请求，操作已执行", 409),

    /**
     * 系统配置错误
     */
    CONFIG_ERROR(1030, "系统配置错误", 500),

    /**
     * 外部服务调用失败
     */
    EXTERNAL_SERVICE_ERROR(1040, "外部服务调用失败", 502),

    /**
     * 网络请求失败
     */
    NETWORK_ERROR(1041, "网络请求失败", 503),

    /**
     * 第三方 API 返回错误
     */
    THIRD_PARTY_API_ERROR(1042, "第三方服务返回错误", 502),

    /**
     * 序列化失败
     */
    SERIALIZATION_ERROR(1050, "序列化失败", 500),

    /**
     * 反序列化失败
     */
    DESERIALIZATION_ERROR(1051, "反序列化失败", 500),

    /**
     * JSON 解析错误
     */
    JSON_PARSE_ERROR(1052, "JSON 解析错误", 400),

    /**
     * 乐观锁冲突
     */
    OPTIMISTIC_LOCK_CONFLICT(1053, "数据版本冲突，请重试", 409),

    // ========== 认证/JWT 错误码（2000-2999）==========

    /**
     * Token 已过期
     */
    TOKEN_EXPIRED(2001, "登录状态已过期，请重新登录", 401),

    /**
     * Token 无效
     */
    TOKEN_INVALID(2002, "无效的认证令牌", 401),

    /**
     * Token 缺失
     */
    TOKEN_MISSING(2003, "未提供认证令牌", 401),

    /**
     * Token 在黑名单内
     */
    TOKEN_BLACKLISTED(2004, "令牌已失效，请重新登录", 401),

    /**
     * 刷新 Token 已过期
     */
    REFRESH_TOKEN_EXPIRED(2005, "刷新令牌已过期，请重新登录", 401),

    /**
     * 刷新 Token 无效
     */
    REFRESH_TOKEN_INVALID(2006, "刷新令牌无效，请重新登录", 401),

    /**
     * 刷新 Token 缺失
     */
    REFRESH_TOKEN_MISSING(2007, "未提供刷新令牌", 401),

    // ========== 租户错误码（4000-4999）==========

    /**
     * 租户不存在
     */
    TENANT_NOT_FOUND(4001, "租户不存在", 404),

    /**
     * 租户已禁用
     */
    TENANT_DISABLED(4002, "租户已被禁用", 403),

    /**
     * 租户已过期
     */
    TENANT_EXPIRED(4003, "租户服务已过期", 403),

    // ========== 文件/存储错误码（5000-5999）==========

    /**
     * 文件不存在
     */
    FILE_NOT_FOUND(5001, "文件不存在", 404),

    /**
     * 文件过大
     */
    FILE_TOO_LARGE(5002, "文件大小超出限制", 413),

    /**
     * 文件类型不支持
     */
    FILE_TYPE_NOT_SUPPORTED(5003, "文件类型不支持", 415),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED(5004, "文件上传失败", 500),

    /**
     * 文件下载失败
     */
    FILE_DOWNLOAD_FAILED(5005, "文件下载失败", 500),

    /**
     * 存储服务异常
     */
    STORAGE_SERVICE_ERROR(5006, "存储服务异常", 503);

    /**
     * 默认模块命名空间
     */
    private static final String DEFAULT_MODULE = "common";

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
     * 构造函数 - 使用默认 HTTP 状态码
     * <p>
     * HTTP 状态码将根据错误码自动推导。
     * </p>
     *
     * @param code    错误码
     * @param message 错误消息
     */
    CommonErrorCode(Integer code, String message) {
        this(code, message, null);
    }

    /**
     * 构造函数 - 指定 HTTP 状态码
     * <p>
     * 用于为特定错误码指定更精确的 HTTP 状态码。
     * </p>
     *
     * @param code       错误码
     * @param message    错误消息
     * @param httpStatus HTTP 状态码，为 null 时使用默认推导规则
     */
    CommonErrorCode(Integer code, String message, Integer httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 获取 HTTP 状态码
     * <p>
     * 如果构造时指定了 HTTP 状态码，则直接返回；
     * 否则使用父接口的默认推导规则。
     * </p>
     *
     * @return HTTP 状态码
     */
    @Override
    public int getHttpStatus() {
        if (httpStatus != null) {
            return httpStatus;
        }
        return ErrorCode.super.getHttpStatus();
    }

    /**
     * 获取错误所属模块
     * <p>
     * CommonErrorCode 属于通用错误码，固定返回 "common"。
     * </p>
     *
     * @return 模块标识 "common"
     */
    @Override
    public String getModule() {
        return DEFAULT_MODULE;
    }
}
