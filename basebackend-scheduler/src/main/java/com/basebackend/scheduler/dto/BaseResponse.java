package com.basebackend.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度模块统一响应包装类
 * <p>
 * 提供标准化的API响应格式，包含成功标识、错误码、消息、时间戳等信息。
 * 所有对外API应使用此类包装返回结果，确保接口输出一致性。
 * </p>
 *
 * @param <T> 响应数据载荷类型
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求是否成功
     */
    private boolean success;

    /**
     * 错误码
     * <p>
     * 成功时为"200"或"OK"，失败时为具体错误码（如"SCH-404"）
     * </p>
     */
    private String code;

    /**
     * 响应消息
     * <p>
     * 成功时为"操作成功"，失败时为具体错误描述
     * </p>
     */
    private String message;

    /**
     * 链路追踪ID
     * <p>
     * 与请求的traceId对应，用于问题排查和日志关联
     * </p>
     */
    private String traceId;

    /**
     * 响应时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 响应数据载荷
     */
    private T data;

    /**
     * 创建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(true);
        response.setCode("200");
        response.setMessage("操作成功");
        response.setTimestamp(LocalDateTime.now());
        response.setData(data);
        return response;
    }

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> BaseResponse<T> success() {
        return success(null);
    }

    /**
     * 创建成功响应（带消息）
     *
     * @param message 成功消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 成功响应对象
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        BaseResponse<T> response = success(data);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建失败响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> failure(String code, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * 创建失败响应（带traceId）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param traceId 链路追踪ID
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> failure(String code, String message, String traceId) {
        BaseResponse<T> response = failure(code, message);
        response.setTraceId(traceId);
        return response;
    }

    /**
     * 从ErrorCode创建失败响应
     * <p>
     * 使用ErrorCode接口统一错误码和消息，支持国际化和模块化管理。
     * </p>
     *
     * @param errorCode ErrorCode接口实现
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> failure(com.basebackend.common.enums.ErrorCode errorCode) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(false);
        response.setCode(errorCode.getCode().toString());
        response.setMessage(errorCode.getMessage());
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * 从ErrorCode创建失败响应（带traceId）
     * <p>
     * 使用ErrorCode接口统一错误码和消息，支持国际化和模块化管理。
     * </p>
     *
     * @param errorCode ErrorCode接口实现
     * @param traceId   链路追踪ID
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> failure(com.basebackend.common.enums.ErrorCode errorCode, String traceId) {
        BaseResponse<T> response = failure(errorCode);
        response.setTraceId(traceId);
        return response;
    }

    /**
     * 从ErrorCode创建失败响应（带自定义消息）
     * <p>
     * 使用ErrorCode的错误码，但覆盖默认消息，适用于需要动态错误描述的场景。
     * </p>
     *
     * @param errorCode     ErrorCode接口实现
     * @param customMessage 自定义错误消息
     * @param traceId       链路追踪ID
     * @param <T>           数据类型
     * @return 失败响应对象
     */
    public static <T> BaseResponse<T> failure(com.basebackend.common.enums.ErrorCode errorCode, String customMessage, String traceId) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(false);
        response.setCode(errorCode.getCode().toString());
        response.setMessage(customMessage);
        response.setTraceId(traceId);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
