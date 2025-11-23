package com.basebackend.common.enums;

/**
 * 错误码接口
 * <p>
 * 所有业务错误码枚举都应实现此接口，提供统一的错误码和错误消息规范。
 * </p>
 *
 * <h3>错误码段划分规范：</h3>
 * <ul>
 *   <li><b>100-599</b>: HTTP 标准状态码（如 200 成功、400 参数错误、401 未授权、500 服务器错误）</li>
 *   <li><b>1000-1999</b>: 通用业务错误（参数校验、数据状态、并发冲突、序列化等）</li>
 *   <li><b>2000-2999</b>: 认证/授权错误（Token、JWT、刷新令牌等）</li>
 *   <li><b>3000-3999</b>: 业务模块自定义错误（各业务模块按需定义）</li>
 *   <li><b>4000-4999</b>: 租户相关错误（租户不存在、已禁用、已过期等）</li>
 *   <li><b>5000-5999</b>: 文件/存储错误（文件操作、存储服务异常等）</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 业务模块自定义错误码枚举
 * public enum OrderErrorCode implements ErrorCode {
 *     ORDER_NOT_FOUND(3001, "订单不存在"),
 *     ORDER_CLOSED(3002, "订单已关闭"),
 *     ORDER_PAID(3003, "订单已支付");
 *
 *     private final Integer code;
 *     private final String message;
 *
 *     UserErrorCode(Integer code, String message) {
 *         this.code = code;
 *         this.message = message;
 *     }
 *
 *     @Override
 *     public Integer getCode() { return code; }
 *
 *     @Override
 *     public String getMessage() { return message; }
 * }
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see CommonErrorCode
 */
public interface ErrorCode {

    /**
     * 获取错误码
     * <p>
     * 错误码应该是唯一的，建议按模块分段管理。
     * </p>
     *
     * @return 错误码（如 400, 2001 等）
     */
    Integer getCode();

    /**
     * 获取错误消息
     * <p>
     * 错误消息应该对用户友好，清晰描述错误原因。
     * 避免暴露敏感的内部实现细节。
     * </p>
     *
     * @return 错误消息（如 "用户不存在"、"参数校验失败" 等）
     */
    String getMessage();

    /**
     * 获取 HTTP 状态码
     * <p>
     * 提供错误码到 HTTP 状态码的映射，用于 REST API 响应。
     * 默认映射规则：
     * </p>
     * <ul>
     *     <li><b>HTTP 标准范围 (100-599)</b>: 直接返回错误码本身</li>
     *     <li><b>业务错误码 (1000-1999)</b>: 统一映射为 400 (客户端请求错误)</li>
     *     <li><b>其他情况</b>: 映射为 500 (服务端内部错误)</li>
     * </ul>
     * <p>
     * 业务枚举可以覆盖此方法以提供更精确的 HTTP 状态码映射。
     * 例如，并发冲突可映射为 409，外部服务错误可映射为 502。
     * </p>
     *
     * @return HTTP 状态码 (如 400, 401, 500 等)
     * @since 1.0.0
     */
    default int getHttpStatus() {
        Integer code = getCode();
        if (code == null) {
            return 500;
        }
        // HTTP 标准状态码范围，直接返回
        if (code >= 100 && code < 600) {
            return code;
        }
        // 业务错误码范围 (1000-1999)，统一映射为客户端错误
        if (code >= 1000 && code < 2000) {
            return 400;
        }
        // 其他情况，映射为服务端内部错误
        return 500;
    }

    /**
     * 获取错误所属模块/命名空间
     * <p>
     * 用于在多模块系统中标识错误码的来源，避免不同模块间错误码冲突。
     * 默认返回 "global"，表示全局通用错误码。
     * </p>
     * <p>
     * 业务枚举可覆盖此方法以返回具体的模块标识，例如：
     * </p>
     * <ul>
     *     <li><b>user</b>: 用户模块错误码</li>
     *     <li><b>order</b>: 订单模块错误码</li>
     *     <li><b>payment</b>: 支付模块错误码</li>
     * </ul>
     *
     * @return 错误码所属模块标识 (如 "global", "user", "order" 等)
     * @since 1.0.0
     */
    default String getModule() {
        return "global";
    }

    /**
     * 获取国际化消息键
     * <p>
     * 用于支持多语言错误消息的国际化 (i18n)。
     * 返回的消息键可以在资源文件 (如 messages.properties) 中查找对应的本地化消息。
     * </p>
     * <p>
     * 默认生成规则: <b>error.{module}.{code}</b>
     * </p>
     * <ul>
     *     <li><b>module</b>: 由 {@link #getModule()} 返回</li>
     *     <li><b>code</b>: 由 {@link #getCode()} 返回</li>
     * </ul>
     * <p>
     * 业务枚举可覆盖此方法以与实际的 i18n 资源文件键保持一致。
     * </p>
     *
     * <h3>示例：</h3>
     * <pre>
     * # messages_zh_CN.properties
     * error.common.400=请求参数错误
     * error.user.2001=用户不存在
     * error.order.3001=订单已关闭
     * </pre>
     *
     * @return 国际化消息键 (如 "error.common.400", "error.user.2001" 等)
     * @since 1.0.0
     */
    default String getMessageKey() {
        String module = getModule();
        String modulePart = (module == null || module.trim().isEmpty()) ? "global" : module.trim().toLowerCase();
        String codePart = (getCode() == null) ? "unknown" : getCode().toString();
        return String.format("error.%s.%s", modulePart, codePart);
    }
}
