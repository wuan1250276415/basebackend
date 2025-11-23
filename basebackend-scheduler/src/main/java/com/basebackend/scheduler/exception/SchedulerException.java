package com.basebackend.scheduler.exception;

import com.basebackend.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 调度器业务异常
 * <p>
 * 用于封装调度器模块的业务异常，携带标准化的错误码和错误消息。
 * 所有业务异常都应使用此类或其子类抛出，便于统一异常处理和日志记录。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 */
@Getter
public class SchedulerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码对象
     */
    private final ErrorCode errorCode;

    /**
     * 错误消息（可覆盖错误码默认消息）
     */
    private final String errorMessage;

    /**
     * 额外的错误详情（可选）
     */
    private final Object errorDetails;

    /**
     * 使用错误码构造异常
     *
     * @param errorCode 错误码
     */
    public SchedulerException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null, null);
    }

    /**
     * 使用错误码和自定义消息构造异常
     *
     * @param errorCode 错误码
     * @param message   自定义错误消息
     */
    public SchedulerException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    /**
     * 使用错误码、自定义消息和原因构造异常
     *
     * @param errorCode 错误码
     * @param message   自定义错误消息
     * @param cause     异常原因
     */
    public SchedulerException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    /**
     * 使用错误码、自定义消息、错误详情和原因构造异常（完整构造器）
     *
     * @param errorCode    错误码
     * @param message      自定义错误消息
     * @param errorDetails 错误详情
     * @param cause        异常原因
     */
    public SchedulerException(ErrorCode errorCode, String message, Object errorDetails, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorMessage = message;
        this.errorDetails = errorDetails;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public Integer getCode() {
        return errorCode != null ? errorCode.getCode() : null;
    }

    /**
     * 获取HTTP状态码
     *
     * @return HTTP状态码
     */
    public int getHttpStatus() {
        return errorCode != null ? errorCode.getHttpStatus() : 500;
    }

    /**
     * 快速创建异常（使用错误码）
     *
     * @param errorCode 错误码
     * @return SchedulerException实例
     */
    public static SchedulerException of(ErrorCode errorCode) {
        return new SchedulerException(errorCode);
    }

    /**
     * 快速创建异常（使用错误码和自定义消息）
     *
     * @param errorCode 错误码
     * @param message   自定义消息
     * @return SchedulerException实例
     */
    public static SchedulerException of(ErrorCode errorCode, String message) {
        return new SchedulerException(errorCode, message);
    }

    /**
     * 快速创建异常（使用错误码、自定义消息和原因）
     *
     * @param errorCode 错误码
     * @param message   自定义消息
     * @param cause     异常原因
     * @return SchedulerException实例
     */
    public static SchedulerException of(ErrorCode errorCode, String message, Throwable cause) {
        return new SchedulerException(errorCode, message, cause);
    }

    @Override
    public String toString() {
        return "SchedulerException{" +
                "code=" + (errorCode != null ? errorCode.getCode() : null) +
                ", message='" + errorMessage + '\'' +
                ", httpStatus=" + getHttpStatus() +
                ", details=" + errorDetails +
                '}';
    }
}
