package com.basebackend.scheduler.camunda.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Camunda 工作流服务异常
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CamundaServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public CamundaServiceException(String message) {
        super(message);
        this.errorCode = "CAMUNDA_SERVICE_ERROR";
    }

    /**
     * 构造函数
     *
     * @param message  错误消息
     * @param cause    原因
     */
    public CamundaServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CAMUNDA_SERVICE_ERROR";
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     */
    public CamundaServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因
     */
    public CamundaServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
