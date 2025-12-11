package com.basebackend.common.web;

import com.basebackend.common.enums.ErrorCode;
import com.basebackend.common.model.Result;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一响应结果封装 - Web层专用别名
 * <p>
 * 这是 {@link Result} 的别名，主要用于Web层（Controller），
 * 提供更清晰的语义。功能上完全等同于 {@link Result}。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 成功响应
 * ResponseResult.success(data);
 *
 * // 使用 ErrorCode 枚举的失败响应
 * ResponseResult.error(CommonErrorCode.DATA_NOT_FOUND);
 * ResponseResult.error(CommonErrorCode.PARAM_VALIDATION_FAILED, "用户名不能为空");
 * }</pre>
 *
 * @param <T> 数据类型
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResponseResult<T> extends Result<T> {

    private static final long serialVersionUID = 1L;

    public ResponseResult() {
        super();
    }

    public ResponseResult(Integer code, String message) {
        super(code, message);
    }

    public ResponseResult(Integer code, String message, T data) {
        super(code, message, data);
    }

    // ========== 成功响应 ==========

    /**
     * 成功响应
     *
     * @return 成功结果
     */
    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>();
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return 成功结果
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(null, null, data);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 自定义消息
     * @param data    响应数据
     * @return 成功结果
     */
    public static <T> ResponseResult<T> success(String message, T data) {
        return new ResponseResult<>(null, message, data);
    }

    // ========== 失败响应（ErrorCode 枚举）==========

    /**
     * 失败响应（使用 ErrorCode 枚举）
     * <p>
     * 推荐使用此方法，自动映射错误码和消息。
     * </p>
     *
     * @param errorCode 错误码枚举
     * @return 失败结果
     */
    public static <T> ResponseResult<T> error(ErrorCode errorCode) {
        return new ResponseResult<>(errorCode.getCode(), errorCode.getMessage());
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
    public static <T> ResponseResult<T> error(ErrorCode errorCode, String message) {
        return new ResponseResult<>(errorCode.getCode(), message);
    }

    // ========== 失败响应（向后兼容）==========

    /**
     * 失败响应
     *
     * @return 失败结果
     */
    public static <T> ResponseResult<T> error() {
        return new ResponseResult<>();
    }

    /**
     * 失败响应（自定义消息）
     *
     * @param message 错误消息
     * @return 失败结果
     */
    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>(null, message);
    }

    /**
     * 失败响应（自定义状态码和消息）
     *
     * @param code    状态码
     * @param message 错误消息
     * @return 失败结果
     * @deprecated 推荐使用 {@link #error(ErrorCode)} 或 {@link #error(ErrorCode, String)}
     */
    @Deprecated
    public static <T> ResponseResult<T> error(Integer code, String message) {
        return new ResponseResult<>(code, message);
    }
}
