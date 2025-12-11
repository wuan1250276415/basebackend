package com.basebackend.common.model;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果封装
 * <p>
 * 支持 {@link ErrorCode} 枚举，自动映射错误码和消息。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 成功响应
 * Result.success(data);
 *
 * // 使用 ErrorCode 枚举的失败响应
 * Result.error(CommonErrorCode.DATA_NOT_FOUND);
 * Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED, "用户名不能为空");
 * }</pre>
 *
 * @param <T> 数据类型
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 成功响应 ==========

    /**
     * 成功响应
     *
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage());
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 自定义消息
     * @param data    响应数据
     * @return 成功结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), message, data);
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
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage());
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
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message);
    }

    // ========== 失败响应（向后兼容）==========

    /**
     * 失败响应
     *
     * @return 失败结果
     */
    public static <T> Result<T> error() {
        return new Result<>(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    /**
     * 失败响应（自定义消息）
     *
     * @param message 错误消息
     * @return 失败结果
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
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
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    // ========== 便捷方法 ==========

    /**
     * 判断响应是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return CommonErrorCode.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 判断响应是否失败
     *
     * @return 是否失败
     */
    public boolean isFailed() {
        return !isSuccess();
    }
}
