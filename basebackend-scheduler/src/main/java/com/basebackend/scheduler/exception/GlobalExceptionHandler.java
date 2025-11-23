//package com.basebackend.scheduler.exception;
//
//import com.basebackend.common.enums.CommonErrorCode;
//import com.basebackend.common.model.Result;
//import com.basebackend.scheduler.camunda.exception.CamundaServiceException;
//import com.basebackend.scheduler.camunda.exception.WorkflowException;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.camunda.bpm.engine.OptimisticLockingException;
//import org.camunda.bpm.engine.ProcessEngineException;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.validation.BindException;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.MissingServletRequestParameterException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//
//import java.util.stream.Collectors;
//
///**
// * 全局统一异常处理器
// *
// * <p>统一处理 basebackend-scheduler 模块的所有异常，提供标准化的错误响应格式。
// * 整合了之前分离的异常处理器，确保响应格式的一致性。
// *
// * <p>设计原则：
// * <ul>
// *   <li>统一使用 {@link Result} 格式作为响应</li>
// *   <li>完整的 traceId 追踪机制</li>
// *   <li>智能的日志级别控制（ERROR vs WARN）</li>
// *   <li>友好的错误消息，避免暴露内部实现</li>
// *   <li>覆盖常见异常场景</li>
// * </ul>
// *
// * <p>异常处理优先级：
// * <ol>
// *   <li>特定业务异常（CamundaServiceException, WorkflowException）</li>
// *   <li>参数验证异常</li>
// *   <li>系统级异常（ProcessEngineException, OptimisticLockingException）</li>
// *   <li>通用异常兜底</li>
// * </ol>
// *
// * @author BaseBackend Team
// * @version 1.0.0
// * @since 2025-01-01
// */
//@Slf4j
//@RestControllerAdvice
//@Order(Ordered.HIGHEST_PRECEDENCE)
//public class GlobalExceptionHandler {
//
//    // ========== 业务异常处理 ==========
//
//    /**
//     * 处理 Camunda 服务层异常
//     */
//    @ExceptionHandler(CamundaServiceException.class)
//    public Result<Object> handleCamundaServiceException(
//            CamundaServiceException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        // 根据错误严重程度选择日志级别
//        if (isServerError(ex.getErrorCode())) {
//            log.error("Camunda服务异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getErrorCode(), ex.getMessage(), traceId, request.getRequestURI(), ex);
//        } else {
//            log.warn("Camunda业务异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getErrorCode(), ex.getMessage(), traceId, request.getRequestURI());
//        }
//
//        // 使用OPERATION_FAILED错误码，错误码放入消息中
//        return Result.error(CommonErrorCode.OPERATION_FAILED.getCode(),
//                String.format("[%s] %s", ex.getErrorCode(), ex.getMessage()));
//    }
//
//    /**
//     * 处理工作流通用异常
//     */
//    @ExceptionHandler(WorkflowException.class)
//    public Result<Object> handleWorkflowException(
//            WorkflowException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        if (isServerError(ex.getErrorCode())) {
//            log.error("工作流异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getErrorCode(), ex.getMessage(), traceId, request.getRequestURI(), ex);
//        } else {
//            log.warn("工作流业务异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getErrorCode(), ex.getMessage(), traceId, request.getRequestURI());
//        }
//
//        // 使用OPERATION_FAILED错误码，错误码放入消息中
//        return Result.error(CommonErrorCode.OPERATION_FAILED.getCode(),
//                String.format("[%s] %s", ex.getErrorCode(), ex.getMessage()));
//    }
//
//    /**
//     * 处理调度器业务异常
//     */
//    @ExceptionHandler(SchedulerException.class)
//    public Result<Object> handleSchedulerException(
//            SchedulerException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(ex.getCode().toString(), request);
//
//        if (ex.getHttpStatus() >= 500) {
//            log.error("调度器服务端异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getCode(), ex.getErrorMessage(), traceId, request.getRequestURI(), ex);
//        } else {
//            log.warn("调度器业务异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getCode(), ex.getErrorMessage(), traceId, request.getRequestURI());
//        }
//
//        // 使用ex.getHttpStatus()作为错误码
//        return Result.error(ex.getHttpStatus(), ex.getErrorMessage());
//    }
//
//    // ========== 参数验证异常处理 ==========
//
//    /**
//     * 处理参数校验异常（@Valid注解）
//     */
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public Result<Object> handleValidationException(
//            MethodArgumentNotValidException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
//                .map(error -> error.getField() + ": " + error.getDefaultMessage())
//                .collect(Collectors.joining("; "));
//
//        log.warn("参数校验失败 [traceId={}, uri={}, errors={}]",
//                traceId, request.getRequestURI(), errorMessage);
//
//        // 使用BAD_REQUEST错误码
//        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED.getCode(),
//                "参数校验失败: " + errorMessage);
//    }
//
//    /**
//     * 处理绑定异常（表单绑定错误）
//     */
//    @ExceptionHandler(BindException.class)
//    public Result<Object> handleBindException(
//            BindException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        String errorMessage = ex.getFieldErrors().stream()
//                .map(FieldError::getDefaultMessage)
//                .collect(Collectors.joining("; "));
//
//        log.warn("参数绑定失败 [traceId={}, uri={}, errors={}]",
//                traceId, request.getRequestURI(), errorMessage);
//
//        // 使用BAD_REQUEST错误码
//        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED.getCode(),
//                "参数绑定失败: " + errorMessage);
//    }
//
//    /**
//     * 处理参数类型不匹配异常
//     */
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public Result<Object> handleTypeMismatchException(
//            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        String errorMessage = String.format("参数 '%s' 类型不正确，期望类型: %s",
//                ex.getName(),
//                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
//
//        log.warn("参数类型不匹配 [traceId={}, uri={}, parameter={}, expected={}]",
//                traceId, request.getRequestURI(), ex.getName(),
//                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
//
//        // 使用BAD_REQUEST错误码
//        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED.getCode(), errorMessage);
//    }
//
//    /**
//     * 处理缺少请求参数异常
//     */
//    @ExceptionHandler(MissingServletRequestParameterException.class)
//    public Result<Object> handleMissingParameterException(
//            MissingServletRequestParameterException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        String errorMessage = String.format("缺少必需参数: %s (%s)",
//                ex.getParameterName(), ex.getParameterType());
//
//        log.warn("缺少请求参数 [traceId={}, uri={}, parameter={}]",
//                traceId, request.getRequestURI(), ex.getParameterName());
//
//        // 使用BAD_REQUEST错误码
//        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED.getCode(), errorMessage);
//    }
//
//    /**
//     * 处理JSON解析异常
//     */
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public Result<Object> handleMessageNotReadableException(
//            HttpMessageNotReadableException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("JSON解析失败 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        // 使用BAD_REQUEST错误码
//        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED.getCode(),
//                "请求体JSON格式错误或字段类型不匹配");
//    }
//
//    // ========== 业务逻辑异常处理 ==========
//
//    /**
//     * 处理非法参数异常
//     */
//    @ExceptionHandler(IllegalArgumentException.class)
//    public Result<Object> handleIllegalArgumentException(
//            IllegalArgumentException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("非法参数异常 [traceId={}, uri={}, message={}]",
//                traceId, request.getRequestURI(), ex.getMessage());
//
//        // 使用BAD_REQUEST错误码
//        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED.getCode(),
//                ex.getMessage() != null ? ex.getMessage() : "非法参数");
//    }
//
//    /**
//     * 处理非法状态异常
//     */
//    @ExceptionHandler(IllegalStateException.class)
//    public Result<Object> handleIllegalStateException(
//            IllegalStateException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("状态冲突异常 [traceId={}, uri={}, message={}]",
//                traceId, request.getRequestURI(), ex.getMessage());
//
//        // 使用OPERATION_FAILED错误码
//        return Result.error(CommonErrorCode.OPERATION_FAILED.getCode(),
//                ex.getMessage() != null ? ex.getMessage() : "状态转换非法");
//    }
//
//    // ========== 系统级异常处理 ==========
//
//    /**
//     * 处理Camunda流程引擎异常
//     */
//    @ExceptionHandler(ProcessEngineException.class)
//    public Result<Object> handleProcessEngineException(
//            ProcessEngineException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.error("Camunda引擎异常 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        // 使用INTERNAL_SERVER_ERROR错误码
//        return Result.error(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
//                "工作流引擎异常: " + ex.getMessage());
//    }
//
//    /**
//     * 处理Camunda乐观锁异常
//     */
//    @ExceptionHandler(OptimisticLockingException.class)
//    public Result<Object> handleOptimisticLockingException(
//            OptimisticLockingException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("乐观锁冲突 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        // 使用OPTIMISTIC_LOCK_CONFLICT错误码
//        return Result.error(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode(),
//                "数据版本冲突，请刷新后重试");
//    }
//
//    /**
//     * 处理所有未捕获的异常（兜底）
//     */
//    @ExceptionHandler(Exception.class)
//    public Result<Object> handleException(
//            Exception ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.error("系统异常 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        return Result.error(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
//                "系统内部错误，请稍后重试");
//    }
//
//    // ========== 私有辅助方法 ==========
//
//    /**
//     * 从请求中提取追踪ID
//     *
//     * <p>优先级：
//     * <ol>
//     *   <li>X-Trace-Id 请求头（推荐）</li>
//     *   <li>X-Request-Id 请求头（备用）</li>
//     *   <li>请求属性 traceId</li>
//     *   <li>生成新的UUID</li>
//     * </ol>
//     */
//    private String extractTraceId(HttpServletRequest request) {
//        return extractTraceId(null, request);
//    }
//
//    /**
//     * 从请求中提取追踪ID（带默认值）
//     */
//    private String extractTraceId(String defaultValue, HttpServletRequest request) {
//        // 1. 从请求头获取（网关或上游服务传递）
//        String traceId = request.getHeader("X-Trace-Id");
//        if (traceId != null && !traceId.trim().isEmpty()) {
//            return traceId;
//        }
//
//        // 2. 从备用请求头获取
//        traceId = request.getHeader("X-Request-Id");
//        if (traceId != null && !traceId.trim().isEmpty()) {
//            return traceId;
//        }
//
//        // 3. 从请求属性获取（拦截器设置）
//        Object attributeTraceId = request.getAttribute("traceId");
//        if (attributeTraceId instanceof String && !((String) attributeTraceId).trim().isEmpty()) {
//            return (String) attributeTraceId;
//        }
//
//        // 4. 返回默认值或生成新的UUID
//        return defaultValue != null ? defaultValue : java.util.UUID.randomUUID().toString();
//    }
//
//    /**
//     * 判断是否为服务器内部错误
//     *
//     * <p>服务器内部错误使用 ERROR 级别日志，其他错误使用 WARN 级别。
//     */
//    private boolean isServerError(String errorCode) {
//        if (errorCode == null) {
//            return true; // 默认视为服务器错误
//        }
//
//        return errorCode.contains("INTERNAL") ||
//                errorCode.contains("SYSTEM") ||
//                errorCode.contains("DATABASE") ||
//                errorCode.contains("CONNECTION") ||
//                errorCode.contains("ENGINE") ||
//                errorCode.equals("CAMUNDA_SERVICE_ERROR") ||
//                errorCode.equals("WORKFLOW_ERROR");
//    }
//}
