package com.basebackend.scheduler.camunda.exception;

/**
 * 工作流统一异常
 */
public class WorkflowException extends RuntimeException {

    private final int errorCode;
    private final String errorMessage;

    public WorkflowException(WorkflowErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    public WorkflowException(WorkflowErrorCode errorCode, String detailMessage) {
        super(errorCode.getMessage() + ": " + detailMessage);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage() + ": " + detailMessage;
    }

    public WorkflowException(WorkflowErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    public WorkflowException(WorkflowErrorCode errorCode, String detailMessage, Throwable cause) {
        super(errorCode.getMessage() + ": " + detailMessage, cause);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage() + ": " + detailMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
