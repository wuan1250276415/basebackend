//package com.basebackend.scheduler.web;
//
//import com.basebackend.common.enums.CommonErrorCode;
//import com.basebackend.scheduler.dto.BaseResponse;
//import com.basebackend.scheduler.exception.SchedulerErrorCode;
//import com.basebackend.scheduler.exception.SchedulerException;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.camunda.bpm.engine.OptimisticLockingException;
//import org.camunda.bpm.engine.ProcessEngineException;
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
// * 调度器模块全局异常处理器
// * <p>
// * 统一处理调度器REST API的异常，提供标准化的错误响应格式。
// * 拦截常见异常并转换为友好的错误信息，避免向用户暴露内部实现细节。
// * </p>
// *
// * @author BaseBackend Team
// * @version 1.0.0
// * @since 2025-01-24
// */
//@Slf4j
//@RestControllerAdvice
//public class SchedulerGlobalExceptionHandler {
//
//    /**
//     * 处理调度器业务异常
//     * <p>
//     * 优先级最高，专门处理SchedulerException，确保业务错误能正确映射HTTP状态码
//     * </p>
//     *
//     * @param ex      SchedulerException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(SchedulerException.class)
//    public ResponseEntity<BaseResponse<Object>> handleSchedulerException(
//            SchedulerException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        // 根据错误码决定日志级别
//        if (ex.getHttpStatus() >= 500) {
//            log.error("调度器服务端异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getCode(), ex.getErrorMessage(), traceId, request.getRequestURI(), ex);
//        } else {
//            log.warn("调度器业务异常 [code={}, message={}, traceId={}, uri={}]",
//                    ex.getCode(), ex.getErrorMessage(), traceId, request.getRequestURI(), ex);
//        }
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                ex.getCode().toString(),
//                ex.getErrorMessage(),
//                traceId);
//
//        // 如果有错误详情，添加到响应数据中
//        if (ex.getErrorDetails() != null) {
//            response.setData(ex.getErrorDetails());
//        }
//
//        // 使用SchedulerException自带的HTTP状态码，确保正确映射
//        return ResponseEntity.status(ex.getHttpStatus()).body(response);
//    }
//
//    /**
//     * 处理参数校验异常（@Valid注解）
//     *
//     * @param ex      MethodArgumentNotValidException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<BaseResponse<Object>> handleValidationException(
//            MethodArgumentNotValidException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        // 收集所有字段错误信息
//        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
//                .map(error -> error.getField() + ": " + error.getDefaultMessage())
//                .collect(Collectors.joining("; "));
//
//        log.warn("参数校验失败 [traceId={}, uri={}, errors={}]",
//                traceId, request.getRequestURI(), errorMessage);
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                SchedulerErrorCode.INVALID_ARGUMENT.getCode().toString(),
//                "参数校验失败: " + errorMessage,
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }
//
//    /**
//     * 处理绑定异常（表单绑定错误）
//     *
//     * @param ex      BindException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(BindException.class)
//    public ResponseEntity<BaseResponse<Object>> handleBindException(
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
//        BaseResponse<Object> response = BaseResponse.failure(
//                SchedulerErrorCode.INVALID_ARGUMENT.getCode().toString(),
//                "参数绑定失败: " + errorMessage,
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }
//
//    /**
//     * 处理参数类型不匹配异常
//     *
//     * @param ex      MethodArgumentTypeMismatchException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public ResponseEntity<BaseResponse<Object>> handleTypeMismatchException(
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
//        BaseResponse<Object> response = BaseResponse.failure(
//                SchedulerErrorCode.INVALID_ARGUMENT.getCode().toString(),
//                errorMessage,
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }
//
//    /**
//     * 处理缺少请求参数异常
//     *
//     * @param ex      MissingServletRequestParameterException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(MissingServletRequestParameterException.class)
//    public ResponseEntity<BaseResponse<Object>> handleMissingParameterException(
//            MissingServletRequestParameterException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        String errorMessage = String.format("缺少必需参数: %s (%s)",
//                ex.getParameterName(), ex.getParameterType());
//
//        log.warn("缺少请求参数 [traceId={}, uri={}, parameter={}]",
//                traceId, request.getRequestURI(), ex.getParameterName());
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                CommonErrorCode.PARAM_NOT_NULL.getCode().toString(),
//                errorMessage,
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }
//
//    /**
//     * 处理JSON解析异常
//     *
//     * @param ex      HttpMessageNotReadableException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<BaseResponse<Object>> handleMessageNotReadableException(
//            HttpMessageNotReadableException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("JSON解析失败 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                CommonErrorCode.JSON_PARSE_ERROR.getCode().toString(),
//                "请求体JSON格式错误或字段类型不匹配",
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }
//
//    /**
//     * 处理非法参数异常
//     *
//     * @param ex      IllegalArgumentException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<BaseResponse<Object>> handleIllegalArgumentException(
//            IllegalArgumentException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("非法参数异常 [traceId={}, uri={}, message={}]",
//                traceId, request.getRequestURI(), ex.getMessage());
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                SchedulerErrorCode.INVALID_ARGUMENT.getCode().toString(),
//                ex.getMessage() != null ? ex.getMessage() : "非法参数",
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }
//
//    /**
//     * 处理非法状态异常
//     *
//     * @param ex      IllegalStateException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(IllegalStateException.class)
//    public ResponseEntity<BaseResponse<Object>> handleIllegalStateException(
//            IllegalStateException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("状态冲突异常 [traceId={}, uri={}, message={}]",
//                traceId, request.getRequestURI(), ex.getMessage());
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                SchedulerErrorCode.STATE_TRANSITION_ERROR.getCode().toString(),
//                ex.getMessage() != null ? ex.getMessage() : "状态转换非法",
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
//    }
//
//    /**
//     * 处理Camunda流程引擎异常
//     *
//     * @param ex      ProcessEngineException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(ProcessEngineException.class)
//    public ResponseEntity<BaseResponse<Object>> handleProcessEngineException(
//            ProcessEngineException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.error("Camunda引擎异常 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                SchedulerErrorCode.ENGINE_ERROR.getCode().toString(),
//                "工作流引擎异常: " + ex.getMessage(),
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//    }
//
//    /**
//     * 处理Camunda乐观锁异常
//     *
//     * @param ex      OptimisticLockingException实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(OptimisticLockingException.class)
//    public ResponseEntity<BaseResponse<Object>> handleOptimisticLockingException(
//            OptimisticLockingException ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.warn("乐观锁冲突 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode().toString(),
//                "数据版本冲突，请刷新后重试",
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
//    }
//
//    /**
//     * 处理所有未捕获的异常
//     *
//     * @param ex      Exception实例
//     * @param request HttpServletRequest
//     * @return 标准错误响应
//     */
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<BaseResponse<Object>> handleException(
//            Exception ex, HttpServletRequest request) {
//        String traceId = extractTraceId(request);
//
//        log.error("系统异常 [traceId={}, uri={}]",
//                traceId, request.getRequestURI(), ex);
//
//        BaseResponse<Object> response = BaseResponse.failure(
//                CommonErrorCode.INTERNAL_SERVER_ERROR.getCode().toString(),
//                "系统内部错误，请稍后重试",
//                traceId);
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//    }
//
//    /**
//     * 从请求中提取traceId
//     * <p>
//     * 优先从请求头获取，其次从请求属性获取，最后生成新的UUID
//     * </p>
//     *
//     * @param request HttpServletRequest
//     * @return traceId
//     */
//    private String extractTraceId(HttpServletRequest request) {
//        // 1. 从请求头获取（网关或上游服务传递）
//        String traceId = request.getHeader("X-Trace-Id");
//        if (traceId != null && !traceId.trim().isEmpty()) {
//            return traceId;
//        }
//
//        // 2. 从请求属性获取（拦截器设置）
//        Object attributeTraceId = request.getAttribute("traceId");
//        if (attributeTraceId instanceof String) {
//            return (String) attributeTraceId;
//        }
//
//        // 3. 生成新的UUID作为traceId
//        return java.util.UUID.randomUUID().toString();
//    }
//}
