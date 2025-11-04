package com.basebackend.scheduler.camunda.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流全局异常处理器
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.basebackend.scheduler.camunda.controller")
public class WorkflowGlobalExceptionHandler {

    /**
     * 处理工作流异常
     */
    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowException(WorkflowException e) {
        log.error("工作流异常: errorCode={}, message={}", e.getErrorCode(), e.getErrorMessage(), e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", e.getErrorCode());
        response.put("message", e.getErrorMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理通用运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", WorkflowErrorCode.WORKFLOW_INTERNAL_ERROR.getCode());
        response.put("message", e.getMessage() != null ? e.getMessage() : "工作流内部错误");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("参数验证异常: ", e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", WorkflowErrorCode.WORKFLOW_INVALID_PARAMETER.getCode());
        response.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("未知异常: ", e);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", WorkflowErrorCode.WORKFLOW_INTERNAL_ERROR.getCode());
        response.put("message", "系统异常，请联系管理员");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
