package com.basebackend.scheduler.camunda.exception;

/**
 * 工作流通用异常
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
public class WorkflowException extends RuntimeException {

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
    public WorkflowException(String message) {
        super(message);
        this.errorCode = "WORKFLOW_ERROR";
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WORKFLOW_ERROR";
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     */
    public WorkflowException(String errorCode, String message) {
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
    public WorkflowException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 设置错误代码
     *
     * @param errorCode 错误代码
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
