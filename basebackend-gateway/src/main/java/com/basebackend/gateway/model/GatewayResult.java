package com.basebackend.gateway.model;

import com.alibaba.fastjson2.JSON;
import com.basebackend.common.enums.ErrorCode;
import com.basebackend.common.model.Result;
import com.basebackend.gateway.enums.GatewayErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网关响应结果
 * <p>
 * 继承自 {@link Result}，保持与其他模块的响应格式一致。
 * 提供额外的网关专用工具方法。
 * </p>
 * 
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * // 使用错误码枚举返回错误
 * GatewayResult.error(GatewayErrorCode.TOKEN_MISSING);
 * GatewayResult.error(GatewayErrorCode.RATE_LIMITED, "IP 限流");
 * 
 * // 转换为 JSON 字符串（用于响应体）
 * String json = gatewayResult.toJsonString();
 * }</pre>
 *
 * @param <T> 数据类型
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayResult<T> extends Result<T> {

    private static final long serialVersionUID = 1L;

    public GatewayResult() {
        super();
    }

    public GatewayResult(Integer code, String message) {
        super(code, message);
    }

    public GatewayResult(Integer code, String message, T data) {
        super(code, message, data);
    }

    // ========== 成功响应 ==========

    /**
     * 成功响应
     *
     * @return 成功结果
     */
    public static <T> GatewayResult<T> success() {
        return new GatewayResult<>(200, "操作成功");
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return 成功结果
     */
    public static <T> GatewayResult<T> success(T data) {
        return new GatewayResult<>(200, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息和数据）
     *
     * @param message 自定义消息
     * @param data    响应数据
     * @return 成功结果
     */
    public static <T> GatewayResult<T> success(String message, T data) {
        return new GatewayResult<>(200, message, data);
    }

    // ========== 失败响应（使用 ErrorCode 枚举）==========

    /**
     * 失败响应（使用 ErrorCode 枚举）
     * <p>
     * 推荐使用此方法，自动映射错误码和消息。
     * </p>
     *
     * @param errorCode 错误码枚举
     * @return 失败结果
     */
    public static <T> GatewayResult<T> error(ErrorCode errorCode) {
        return new GatewayResult<>(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 失败响应（使用 ErrorCode 枚举，自定义消息）
     * <p>
     * 使用枚举的错误码，但可以自定义更详细的错误消息。
     * </p>
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     * @return 失败结果
     */
    public static <T> GatewayResult<T> error(ErrorCode errorCode, String message) {
        return new GatewayResult<>(errorCode.getCode(), message);
    }

    /**
     * 失败响应（使用 GatewayErrorCode 枚举）
     * <p>
     * 网关专用的便捷方法。
     * </p>
     *
     * @param errorCode 网关错误码枚举
     * @return 失败结果
     */
    public static <T> GatewayResult<T> error(GatewayErrorCode errorCode) {
        return new GatewayResult<>(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 失败响应（使用 GatewayErrorCode 枚举，自定义消息）
     *
     * @param errorCode 网关错误码枚举
     * @param message   自定义错误消息
     * @return 失败结果
     */
    public static <T> GatewayResult<T> error(GatewayErrorCode errorCode, String message) {
        return new GatewayResult<>(errorCode.getCode(), message);
    }

    // ========== 失败响应（向后兼容）==========

    /**
     * 失败响应
     *
     * @return 失败结果
     */
    public static <T> GatewayResult<T> error() {
        return new GatewayResult<>(500, "服务器内部错误");
    }

    /**
     * 失败响应（自定义消息）
     *
     * @param message 错误消息
     * @return 失败结果
     */
    public static <T> GatewayResult<T> error(String message) {
        return new GatewayResult<>(500, message);
    }

    /**
     * 失败响应（自定义状态码和消息）
     *
     * @param code    状态码
     * @param message 错误消息
     * @return 失败结果
     * @deprecated 推荐使用 {@link #error(ErrorCode)} 或 {@link #error(GatewayErrorCode)}
     */
    @Deprecated
    public static <T> GatewayResult<T> error(Integer code, String message) {
        return new GatewayResult<>(code, message);
    }

    // ========== 工具方法 ==========

    /**
     * 转换为 JSON 字符串
     * <p>
     * 用于 WebFlux 响应体序列化。
     * </p>
     *
     * @return JSON 字符串
     */
    public String toJsonString() {
        return JSON.toJSONString(this);
    }
}
