package com.basebackend.scheduler.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.stream.Collectors;

/**
 * 异常处理工具类
 *
 * <p>提供异常处理的常用工具方法，简化异常处理逻辑。
 * 包括异常信息提取、错误消息构建、HTTP状态码判断等功能。
 *
 * <p>主要功能：
 * <ul>
 *   <li>提取异常的根本原因</li>
 *   <li>构建用户友好的错误消息</li>
 *   <li>判断异常的严重程度</li>
 *   <li>异常信息的脱敏处理</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
public final class ExceptionUtils {

    /**
     * 私有构造函数，防止实例化工具类
     */
    private ExceptionUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== 异常信息提取 ==========

    /**
     * 获取异常的根本原因（最内层的异常）
     *
     * <p>遍历异常链，找到最原始的异常信息。
     *
     * @param throwable 异常对象
     * @return 根本原因异常
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 获取异常的根本原因消息
     *
     * @param throwable 异常对象
     * @return 根本原因消息
     */
    public static String getRootCauseMessage(Throwable throwable) {
        Throwable rootCause = getRootCause(throwable);
        return rootCause != null ? rootCause.getMessage() : null;
    }

    /**
     * 截取异常消息，限制长度
     *
     * @param message 原始消息
     * @param maxLength 最大长度
     * @return 截取后的消息
     */
    public static String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return null;
        }

        if (message.length() <= maxLength) {
            return message;
        }

        return message.substring(0, maxLength - 3) + "...";
    }

    // ========== 错误消息构建 ==========

    /**
     * 构建验证异常的用户友好消息
     *
     * @param ex MethodArgumentNotValidException
     * @return 用户友好的错误消息
     */
    public static String buildValidationErrorMessage(MethodArgumentNotValidException ex) {
        if (ex == null || ex.getBindingResult() == null) {
            return "参数验证失败";
        }

        return ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    String field = error.getField();
                    String defaultMessage = error.getDefaultMessage();
                    String rejectedValue = error.getRejectedValue() != null ?
                            "，实际值: " + error.getRejectedValue() : "";
                    return field + " " + defaultMessage + rejectedValue;
                })
                .collect(Collectors.joining("; "));
    }

    /**
     * 构建异常的用户友好消息
     *
     * <p>对不同类型的异常采用不同的消息提取策略：
     * <ul>
     *   <li>业务异常：直接使用异常消息</li>
     *   <li>系统异常：提取根本原因，过滤敏感信息</li>
     *   <li>验证异常：使用专门的验证消息构建方法</li>
     * </ul>
     *
     * @param throwable 异常对象
     * @param errorCode 错误码
     * @return 用户友好消息
     */
    public static String buildUserFriendlyMessage(Throwable throwable, String errorCode) {
        if (throwable == null) {
            return "未知错误";
        }

        // 验证异常特殊处理
        if (throwable instanceof MethodArgumentNotValidException) {
            return buildValidationErrorMessage((MethodArgumentNotValidException) throwable);
        }

        // 获取原始消息
        String originalMessage = getRootCauseMessage(throwable);

        if (originalMessage == null) {
            return "系统内部错误";
        }

        // 过滤敏感信息
        String sanitizedMessage = sanitizeMessage(originalMessage, errorCode);

        // 限制消息长度
        return truncateMessage(sanitizedMessage, 500);
    }

    /**
     * 过滤敏感信息
     *
     * <p>对异常消息进行脱敏处理，避免泄露敏感信息：
     * <ul>
     *   <li>数据库连接信息</li>
     *   <li>SQL语句（包含敏感字段）</li>
     *   <li>文件路径</li>
     *   <li>内部系统信息</li>
     * </ul>
     *
     * @param message 原始消息
     * @param errorCode 错误码
     * @return 脱敏后的消息
     */
    private static String sanitizeMessage(String message, String errorCode) {
        if (message == null) {
            return null;
        }

        String sanitized = message;

        // 过滤数据库连接信息
        sanitized = sanitized.replaceAll("jdbc:[^\\s]+", "jdbc:***");
        sanitized = sanitized.replaceAll("password[^=]*=[^&;\\s]+", "password=***");
        sanitized = sanitized.replaceAll("username[^=]*=[^&;\\s]+", "username=***");

        // 过滤SQL语句（保留关键信息）
        if (sanitized.contains("SQL")) {
            sanitized = "数据库操作失败，请检查参数格式";
        }

        // 过滤堆栈跟踪信息（用于生产环境）
        if (sanitized.contains("at ") && sanitized.contains(".java:")) {
            sanitized = sanitized.split("\\n")[0]; // 只保留第一行
        }

        // 过滤文件路径
        sanitized = sanitized.replaceAll("[A-Za-z]:\\\\[^:\\s]+\\\\", "***\\");

        // 如果是服务器内部错误，隐藏详细技术信息
        if (ExceptionMapping.isServerError(errorCode)) {
            if (sanitized.length() > 100) {
                sanitized = "系统内部错误，请联系管理员";
            }
        }

        return sanitized;
    }

    // ========== 异常严重程度判断 ==========

    /**
     * 判断异常是否为高优先级（需要立即关注）
     *
     * <p>高优先级异常包括：
     * <ul>
     *   <li>服务器内部错误（5xx）</li>
     *   <li>数据库相关错误</li>
     *   <li>工作流引擎错误</li>
     *   <li>网络连接错误</li>
     * </ul>
     *
     * @param errorCode 错误码
     * @return true 如果是高优先级异常
     */
    public static boolean isHighPriority(String errorCode) {
        if (errorCode == null) {
            return true; // 默认为高优先级
        }

        return ExceptionMapping.isServerError(errorCode) ||
                errorCode.contains("DATABASE") ||
                errorCode.contains("ENGINE") ||
                errorCode.contains("CONNECTION") ||
                errorCode.contains("TIMEOUT");
    }

    /**
     * 判断异常是否需要记录堆栈跟踪
     *
     * @param errorCode 错误码
     * @param throwable 异常对象
     * @return true 如果需要记录堆栈
     */
    public static boolean shouldLogStackTrace(String errorCode, Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        // 服务器内部错误总是记录堆栈
        if (ExceptionMapping.isServerError(errorCode)) {
            return true;
        }

        // OutOfMemoryError、StackOverflowError 等严重错误
        if (throwable instanceof OutOfMemoryError ||
                throwable instanceof StackOverflowError) {
            return true;
        }

        // 数据库连接、SQL执行错误
        if (errorCode != null &&
                (errorCode.contains("DATABASE") ||
                 errorCode.contains("SQL") ||
                 errorCode.contains("CONNECTION"))) {
            return true;
        }

        return false;
    }

    // ========== HTTP状态码相关 ==========

    /**
     * 根据异常获取推荐的HTTP状态码
     *
     * @param errorCode 错误码
     * @param throwable 异常对象
     * @return HTTP状态码
     */
    public static HttpStatus getRecommendedHttpStatus(String errorCode, Throwable throwable) {
        // 先从错误码映射获取
        HttpStatus status = ExceptionMapping.getHttpStatusEnum(errorCode);
        if (status != HttpStatus.INTERNAL_SERVER_ERROR) {
            return status;
        }

        // 根据异常类型进行微调
        if (throwable instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        } else if (throwable instanceof IllegalStateException) {
            return HttpStatus.CONFLICT;
        } else if (throwable instanceof MethodArgumentNotValidException) {
            return HttpStatus.BAD_REQUEST;
        }

        return status;
    }

    /**
     * 获取异常的分类描述
     *
     * @param errorCode 错误码
     * @return 异常分类描述
     */
    public static String getExceptionCategory(String errorCode) {
        if (errorCode == null) {
            return "UNKNOWN";
        }

        if (errorCode.contains("VALIDATION") || errorCode.contains("PARAM")) {
            return "VALIDATION";
        } else if (errorCode.contains("AUTH") || errorCode.contains("UNAUTHORIZED")) {
            return "AUTHENTICATION";
        } else if (errorCode.contains("PERMISSION") || errorCode.contains("FORBIDDEN")) {
            return "AUTHORIZATION";
        } else if (errorCode.contains("NOT_FOUND")) {
            return "NOT_FOUND";
        } else if (errorCode.contains("CONFLICT") || errorCode.contains("DUPLICATE")) {
            return "CONFLICT";
        } else if (ExceptionMapping.isServerError(errorCode)) {
            return "INTERNAL";
        } else {
            return "BUSINESS";
        }
    }
}
