package com.basebackend.scheduler.camunda.exception;

import com.basebackend.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Camunda工作流模块异常处理器
 * <p>
 * 专门处理Camunda工作流相关的异常,统一返回{@link Result}格式。
 * 优先级高于全局异常处理器,确保Camunda异常被正确处理。
 * </p>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-24
 * @see CamundaServiceException
 * @see WorkflowException
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.basebackend.scheduler.camunda")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkflowGlobalExceptionHandler {

    @ExceptionHandler(CamundaServiceException.class)
    public ResponseEntity<Result<Object>> handleCamundaServiceException(
            CamundaServiceException ex, HttpServletRequest request) {

        String traceId = extractTraceId(request);
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : "CAMUNDA_SERVICE_ERROR";

        if (isSevereError(errorCode)) {
            log.error("Camunda服务异常 [code={}, message={}, traceId={}, uri={}]",
                    errorCode, ex.getMessage(), traceId, request.getRequestURI(), ex);
        } else {
            log.warn("Camunda业务异常 [code={}, message={}, traceId={}, uri={}]",
                    errorCode, ex.getMessage(), traceId, request.getRequestURI());
        }

        Result<Object> response = Result.error(errorCode + ": " + ex.getMessage());
        HttpStatus status = determineHttpStatus(errorCode);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<Result<Object>> handleWorkflowException(
            WorkflowException ex, HttpServletRequest request) {

        String traceId = extractTraceId(request);
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : "WORKFLOW_ERROR";

        if (isSevereError(errorCode)) {
            log.error("工作流异常 [code={}, message={}, traceId={}, uri={}]",
                    errorCode, ex.getMessage(), traceId, request.getRequestURI(), ex);
        } else {
            log.warn("工作流业务异常 [code={}, message={}, traceId={}, uri={}]",
                    errorCode, ex.getMessage(), traceId, request.getRequestURI());
        }

        Result<Object> response = Result.error(errorCode + ": " + ex.getMessage());
        HttpStatus status = determineHttpStatus(errorCode);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 从请求中提取追踪ID
     * <p>
     * 优先从请求头获取,如果没有则生成一个简单的ID。
     * </p>
     *
     * @param request HTTP请求
     * @return 追踪ID
     */
    private String extractTraceId(HttpServletRequest request) {
        // 尝试从请求头获取traceId
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = request.getHeader("X-Request-Id");
        }
        if (traceId == null || traceId.isEmpty()) {
            // 生成简单的traceId
            traceId = "WF-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10000);
        }
        return traceId;
    }

    /**
     * 判断是否为严重错误
     * <p>
     * 严重错误使用ERROR级别日志并打印堆栈,非严重错误使用WARN级别。
     * </p>
     *
     * @param errorCode 错误码
     * @return true如果是严重错误
     */
    private boolean isSevereError(String errorCode) {
        // 以下错误码视为严重错误
        return errorCode != null && (
                errorCode.contains("INTERNAL") ||
                errorCode.contains("SYSTEM") ||
                errorCode.contains("DATABASE") ||
                errorCode.contains("CONNECTION") ||
                errorCode.equals("CAMUNDA_SERVICE_ERROR") ||
                errorCode.equals("WORKFLOW_ERROR")
        );
    }

    /**
     * 根据错误码决定HTTP状态码
     * <p>
     * 映射规则:
     * <ul>
     *   <li>NOT_FOUND相关 -> 404</li>
     *   <li>VALIDATION/INVALID相关 -> 400</li>
     *   <li>CONFLICT相关 -> 409</li>
     *   <li>其他 -> 500</li>
     * </ul>
     * </p>
     *
     * @param errorCode 错误码
     * @return HTTP状态码
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (errorCode.contains("NOT_FOUND") || errorCode.contains("不存在")) {
            return HttpStatus.NOT_FOUND;
        }
        if (errorCode.contains("VALIDATION") || errorCode.contains("INVALID") ||
            errorCode.contains("ILLEGAL") || errorCode.contains("BAD_REQUEST")) {
            return HttpStatus.BAD_REQUEST;
        }
        if (errorCode.contains("CONFLICT") || errorCode.contains("DUPLICATE") ||
            errorCode.contains("ALREADY_EXISTS")) {
            return HttpStatus.CONFLICT;
        }
        if (errorCode.contains("UNAUTHORIZED") || errorCode.contains("FORBIDDEN")) {
            return HttpStatus.FORBIDDEN;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
