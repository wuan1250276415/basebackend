package com.basebackend.common.starter.exception;

import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.model.Result;
import com.basebackend.common.starter.properties.CommonProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理所有未捕获的异常，将其转换为标准的 {@link Result} 响应。
 * 支持业务异常、参数校验异常、系统异常等多种异常类型。
 * </p>
 *
 * <h3>异常处理优先级：</h3>
 * <ol>
 *   <li>{@link BusinessException} - 业务异常，使用异常中的错误码和消息</li>
 *   <li>参数校验异常 - 转换为 BAD_REQUEST，包含详细校验信息</li>
 *   <li>HTTP 相关异常 - 转换为对应的 HTTP 状态码</li>
 *   <li>其他未知异常 - 转换为 INTERNAL_SERVER_ERROR</li>
 * </ol>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "basebackend.common.exception", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {

    private final CommonProperties commonProperties;

    // ========== 业务异常 ==========

    /**
     * 处理业务异常
     * <p>
     * 业务异常通常由业务逻辑主动抛出，包含明确的错误码和消息。
     * </p>
     *
     * @param e       业务异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        if (shouldLogException()) {
            log.warn("Business exception: code={}, message={}, uri={}",
                    e.getCode(), e.getMessage(), getRequestUri(request));
        }
        return Result.error(e.getCode(), e.getMessage());
    }

    // ========== 参数校验异常 ==========

    /**
     * 处理 @Validated 注解的参数校验异常（表单对象）
     *
     * @param e       参数校验异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                               HttpServletRequest request) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (shouldLogException()) {
            log.warn("Validation failed: {}, uri={}", errors, getRequestUri(request));
        }

        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED, errors);
    }

    /**
     * 处理 @Validated 注解的参数校验异常（绑定异常）
     *
     * @param e       绑定异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String errors = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (shouldLogException()) {
            log.warn("Bind exception: {}, uri={}", errors, getRequestUri(request));
        }

        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED, errors);
    }

    /**
     * 处理 @Validated 注解的参数校验异常（方法参数）
     *
     * @param e       约束违反异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e,
                                                            HttpServletRequest request) {
        String errors = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        if (shouldLogException()) {
            log.warn("Constraint violation: {}, uri={}", errors, getRequestUri(request));
        }

        return Result.error(CommonErrorCode.PARAM_VALIDATION_FAILED, errors);
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param e       缺少参数异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                       HttpServletRequest request) {
        String message = String.format("缺少必需参数: %s", e.getParameterName());

        if (shouldLogException()) {
            log.warn("Missing parameter: {}, uri={}", e.getParameterName(), getRequestUri(request));
        }

        return Result.error(CommonErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理参数类型不匹配异常
     *
     * @param e       类型不匹配异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                                   HttpServletRequest request) {
        String message = String.format("参数类型错误: %s", e.getName());

        if (shouldLogException()) {
            log.warn("Type mismatch: {}, uri={}", e.getName(), getRequestUri(request));
        }

        return Result.error(CommonErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理 HTTP 消息不可读异常（通常是 JSON 解析失败）
     *
     * @param e       消息不可读异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
                                                               HttpServletRequest request) {
        if (shouldLogException()) {
            log.warn("Message not readable, uri={}", getRequestUri(request), e);
        }

        return Result.error(CommonErrorCode.JSON_PARSE_ERROR, "请求体解析失败");
    }

    // ========== HTTP 相关异常 ==========

    /**
     * 处理请求方法不支持异常
     *
     * @param e       方法不支持异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e,
                                                                      HttpServletRequest request) {
        String message = String.format("不支持 %s 请求方法", e.getMethod());

        if (shouldLogException()) {
            log.warn("Method not allowed: {}, uri={}", e.getMethod(), getRequestUri(request));
        }

        return Result.error(CommonErrorCode.METHOD_NOT_ALLOWED, message);
    }

    /**
     * 处理媒体类型不支持异常
     *
     * @param e       媒体类型不支持异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e,
                                                                  HttpServletRequest request) {
        String message = String.format("不支持的媒体类型: %s", e.getContentType());

        if (shouldLogException()) {
            log.warn("Media type not supported: {}, uri={}", e.getContentType(), getRequestUri(request));
        }

        return Result.error(CommonErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理 404 异常
     *
     * @param e       404 异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e,
                                                       HttpServletRequest request) {
        if (shouldLogException()) {
            log.warn("No handler found, uri={}", getRequestUri(request));
        }

        return Result.error(CommonErrorCode.NOT_FOUND, "请求的资源不存在");
    }

    // ========== 系统异常 ==========

    /**
     * 处理非法参数异常
     *
     * @param e       非法参数异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e,
                                                        HttpServletRequest request) {
        if (shouldLogException()) {
            log.warn("Illegal argument, uri={}", getRequestUri(request), e);
        }

        return Result.error(CommonErrorCode.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理非法状态异常
     *
     * @param e       非法状态异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleIllegalStateException(IllegalStateException e,
                                                     HttpServletRequest request) {
        if (shouldLogException()) {
            log.error("Illegal state, uri={}", getRequestUri(request), e);
        }

        return Result.error(CommonErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    /**
     * 处理未知异常
     * <p>
     * 兜底处理所有未被上述方法捕获的异常。
     * </p>
     *
     * @param e       未知异常
     * @param request HTTP 请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected exception, uri={}", getRequestUri(request), e);

        String message = commonProperties.getException().getIncludeStackTrace()
                ? e.getMessage()
                : CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage();

        return Result.error(CommonErrorCode.INTERNAL_SERVER_ERROR, message);
    }

    // ========== 私有方法 ==========

    /**
     * 获取请求 URI
     */
    private String getRequestUri(HttpServletRequest request) {
        if (!commonProperties.getException().getLogRequestInfo()) {
            return null;
        }
        return request != null ? request.getRequestURI() : "unknown";
    }

    /**
     * 是否应该记录异常日志
     */
    private boolean shouldLogException() {
        return commonProperties.getException().getLogEnabled();
    }
}
