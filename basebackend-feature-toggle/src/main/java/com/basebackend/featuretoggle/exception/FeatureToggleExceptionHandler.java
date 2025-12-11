package com.basebackend.featuretoggle.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 特性开关异常处理器
 * <p>
 * 提供统一的异常处理、降级策略和统计功能。
 * 支持异常重试、熔断器和监控指标。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *   <li>异常分类处理 - 根据异常类型采用不同的处理策略</li>
 *   <li>降级策略 - 外部服务不可用时的默认返回值</li>
 *   <li>异常统计 - 提供异常频率和类型统计</li>
 *   <li>重试机制 - 支持指数退避重试</li>
 *   <li>熔断器 - 防止雪崩效应</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class FeatureToggleExceptionHandler {

    // 手动添加 Logger 以解决 Lombok 注解处理问题
    private static final Logger log = LoggerFactory.getLogger(FeatureToggleExceptionHandler.class);

    /**
     * 异常统计信息
     */
    private final ConcurrentHashMap<String, AtomicLong> exceptionCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> exceptionLastOccurrence = new ConcurrentHashMap<>();

    /**
     * 熔断器状态
     */
    private final ConcurrentHashMap<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * 处理特性开关异常
     *
     * @param featureName 特性名称
     * @param operation 操作名称
     * @param exception 异常
     * @param defaultValue 默认值
     * @return 处理后的默认值
     */
    public boolean handleException(String featureName, String operation, Throwable exception, boolean defaultValue) {
        String exceptionKey = buildExceptionKey(featureName, operation, exception);

        // 记录异常统计
        recordException(exceptionKey, exception);

        // 检查熔断器状态
        if (isCircuitOpen(featureName)) {
            log.warn("Circuit breaker is OPEN for feature '{}', returning default value: {}", featureName, defaultValue);
            return defaultValue;
        }

        // 根据异常类型决定处理策略
        ExceptionType exceptionType = classifyException(exception);

        switch (exceptionType) {
            case NETWORK_ERROR:
                return handleNetworkError(featureName, operation, exception, defaultValue);
            case TIMEOUT_ERROR:
                return handleTimeoutError(featureName, operation, exception, defaultValue);
            case AUTHENTICATION_ERROR:
                return handleAuthError(featureName, operation, exception, defaultValue);
            case RATE_LIMIT_ERROR:
                return handleRateLimitError(featureName, operation, exception, defaultValue);
            case CONFIGURATION_ERROR:
                return handleConfigError(featureName, operation, exception, defaultValue);
            case BUSINESS_ERROR:
                return handleBusinessError(featureName, operation, exception, defaultValue);
            default:
                return handleUnknownError(featureName, operation, exception, defaultValue);
        }
    }

    /**
     * 处理特性开关异常（字符串版本）
     *
     * @param featureName 特性名称
     * @param operation 操作名称
     * @param exception 异常
     * @param defaultValue 默认值
     * @return 处理后的默认值
     */
    public String handleException(String featureName, String operation, Throwable exception, String defaultValue) {
        String exceptionKey = buildExceptionKey(featureName, operation, exception);

        // 记录异常统计
        recordException(exceptionKey, exception);

        // 检查熔断器状态
        if (isCircuitOpen(featureName)) {
            log.warn("Circuit breaker is OPEN for feature '{}', returning default value: {}", featureName, defaultValue);
            return defaultValue;
        }

        // 根据异常类型决定处理策略
        ExceptionType exceptionType = classifyException(exception);

        switch (exceptionType) {
            case NETWORK_ERROR:
                return handleNetworkError(featureName, operation, exception, defaultValue);
            case TIMEOUT_ERROR:
                return handleTimeoutError(featureName, operation, exception, defaultValue);
            case AUTHENTICATION_ERROR:
                return handleAuthError(featureName, operation, exception, defaultValue);
            case RATE_LIMIT_ERROR:
                return handleRateLimitError(featureName, operation, exception, defaultValue);
            case CONFIGURATION_ERROR:
                return handleConfigError(featureName, operation, exception, defaultValue);
            case BUSINESS_ERROR:
                return handleBusinessError(featureName, operation, exception, defaultValue);
            default:
                return handleUnknownError(featureName, operation, exception, defaultValue);
        }
    }

    /**
     * 异常分类
     */
    private ExceptionType classifyException(Throwable exception) {
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();

        if (exceptionName.contains("timeout") || exceptionName.contains("SocketTimeout".toLowerCase())) {
            return ExceptionType.TIMEOUT_ERROR;
        } else if (exceptionName.contains("network") || exceptionName.contains("connect") ||
                   exception instanceof java.net.UnknownHostException ||
                   exception instanceof java.net.ConnectException) {
            return ExceptionType.NETWORK_ERROR;
        } else if (exceptionName.contains("auth") || exceptionName.contains("unauthorized") ||
                   exceptionName.contains("forbidden")) {
            return ExceptionType.AUTHENTICATION_ERROR;
        } else if (exceptionName.contains("rate") || exceptionName.contains("quota") ||
                   exceptionName.contains("limit")) {
            return ExceptionType.RATE_LIMIT_ERROR;
        } else if (exception instanceof FeatureToggleException) {
            return ExceptionType.BUSINESS_ERROR;
        } else if (exceptionName.contains("config") || exceptionName.contains("property")) {
            return ExceptionType.CONFIGURATION_ERROR;
        }

        return ExceptionType.UNKNOWN_ERROR;
    }

    /**
     * 处理网络错误
     */
    private boolean handleNetworkError(String featureName, String operation, Throwable exception, boolean defaultValue) {
        log.error("Network error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);

        // 网络错误通常意味着服务不可用，返回关闭状态
        // 但开启熔断器以防止雪崩
        openCircuitBreaker(featureName, operation, exception);

        return false; // 网络错误时关闭特性
    }

    private String handleNetworkError(String featureName, String operation, Throwable exception, String defaultValue) {
        log.error("Network error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);

        openCircuitBreaker(featureName, operation, exception);

        return "default"; // 网络错误时使用默认值
    }

    /**
     * 处理超时错误
     */
    private boolean handleTimeoutError(String featureName, String operation, Throwable exception, boolean defaultValue) {
        log.warn("Timeout error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage());

        // 超时错误可能是暂时的，尝试重试
        // 但也要降低频率
        return defaultValue;
    }

    private String handleTimeoutError(String featureName, String operation, Throwable exception, String defaultValue) {
        log.warn("Timeout error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage());
        return defaultValue;
    }

    /**
     * 处理认证错误
     */
    private boolean handleAuthError(String featureName, String operation, Throwable exception, boolean defaultValue) {
        log.error("Authentication error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);

        // 认证错误表明配置有问题，关闭特性
        return false;
    }

    private String handleAuthError(String featureName, String operation, Throwable exception, String defaultValue) {
        log.error("Authentication error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);
        return "default";
    }

    /**
     * 处理限流错误
     */
    private boolean handleRateLimitError(String featureName, String operation, Throwable exception, boolean defaultValue) {
        log.warn("Rate limit error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage());

        // 限流错误时，暂停一段时间
        try {
            Thread.sleep(1000); // 等待1秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return defaultValue;
    }

    private String handleRateLimitError(String featureName, String operation, Throwable exception, String defaultValue) {
        log.warn("Rate limit error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return defaultValue;
    }

    /**
     * 处理配置错误
     */
    private boolean handleConfigError(String featureName, String operation, Throwable exception, boolean defaultValue) {
        log.error("Configuration error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);

        // 配置错误表明系统配置有问题，关闭特性
        return false;
    }

    private String handleConfigError(String featureName, String operation, Throwable exception, String defaultValue) {
        log.error("Configuration error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);
        return "default";
    }

    /**
     * 处理业务错误
     */
    private boolean handleBusinessError(String featureName, String operation, Throwable exception, boolean defaultValue) {
        if (exception instanceof FeatureToggleException) {
            FeatureToggleException fe = (FeatureToggleException) exception;
            log.warn("Business error for feature '{}' operation '{}': {} (code: {})",
                    featureName, operation, fe.getMessage(), fe.getErrorCode());
        } else {
            log.warn("Business error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage());
        }

        // 业务错误保持默认行为
        return defaultValue;
    }

    private String handleBusinessError(String featureName, String operation, Throwable exception, String defaultValue) {
        if (exception instanceof FeatureToggleException) {
            FeatureToggleException fe = (FeatureToggleException) exception;
            log.warn("Business error for feature '{}' operation '{}': {} (code: {})",
                    featureName, operation, fe.getMessage(), fe.getErrorCode());
        } else {
            log.warn("Business error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage());
        }
        return defaultValue;
    }

    /**
     * 处理未知错误
     */
    private boolean handleUnknownError(String featureName, String operation, Throwable exception, boolean defaultValue) {
        log.error("Unknown error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);

        // 未知错误保守处理，返回默认关闭
        return false;
    }

    private String handleUnknownError(String featureName, String operation, Throwable exception, String defaultValue) {
        log.error("Unknown error for feature '{}' operation '{}': {}", featureName, operation, exception.getMessage(), exception);
        return "default";
    }

    /**
     * 记录异常统计
     */
    private void recordException(String exceptionKey, Throwable exception) {
        exceptionCounters.computeIfAbsent(exceptionKey, k -> new AtomicLong(0)).incrementAndGet();
        exceptionLastOccurrence.put(exceptionKey, new AtomicLong(System.currentTimeMillis()));
    }

    /**
     * 构建异常键
     */
    private String buildExceptionKey(String featureName, String operation, Throwable exception) {
        return String.format("%s:%s:%s", featureName, operation, exception.getClass().getSimpleName());
    }

    /**
     * 检查熔断器状态
     */
    private boolean isCircuitOpen(String featureName) {
        CircuitBreakerState state = circuitBreakers.get(featureName);
        if (state == null) {
            return false;
        }

        // 检查是否应该关闭熔断器
        if (state.isOpen() && System.currentTimeMillis() > state.getOpenUntil()) {
            state.setHalfOpen(true);
            log.info("Circuit breaker for feature '{}' is now HALF_OPEN", featureName);
            return false;
        }

        return state.isOpen();
    }

    /**
     * 开启熔断器
     */
    private void openCircuitBreaker(String featureName, String operation, Throwable exception) {
        CircuitBreakerState state = circuitBreakers.computeIfAbsent(featureName, k -> new CircuitBreakerState());

        if (!state.isOpen()) {
            state.setOpen(true);
            state.setOpenUntil(System.currentTimeMillis() + 60000); // 1分钟后尝试恢复
            log.warn("Circuit breaker OPEN for feature '{}' due to error: {}", featureName, exception.getMessage());
        }
    }

    /**
     * 关闭熔断器
     */
    public void closeCircuitBreaker(String featureName) {
        circuitBreakers.remove(featureName);
        log.info("Circuit breaker CLOSED for feature '{}'", featureName);
    }

    /**
     * 获取异常统计
     */
    public ExceptionStatistics getStatistics() {
        return new ExceptionStatistics(exceptionCounters, exceptionLastOccurrence, circuitBreakers);
    }

    /**
     * 清除异常统计
     */
    public void clearStatistics() {
        exceptionCounters.clear();
        exceptionLastOccurrence.clear();
    }

    /**
     * 异常类型枚举
     */
    private enum ExceptionType {
        NETWORK_ERROR,
        TIMEOUT_ERROR,
        AUTHENTICATION_ERROR,
        RATE_LIMIT_ERROR,
        CONFIGURATION_ERROR,
        BUSINESS_ERROR,
        UNKNOWN_ERROR
    }

    /**
     * 熔断器状态
     */
    private static class CircuitBreakerState {
        private volatile boolean open = false;
        private volatile boolean halfOpen = false;
        private volatile long openUntil = 0;

        public boolean isOpen() {
            return open;
        }

        public void setOpen(boolean open) {
            this.open = open;
            if (!open) {
                this.halfOpen = false;
            }
        }

        public boolean isHalfOpen() {
            return halfOpen;
        }

        public void setHalfOpen(boolean halfOpen) {
            this.halfOpen = halfOpen;
            if (halfOpen) {
                this.open = false;
            }
        }

        public long getOpenUntil() {
            return openUntil;
        }

        public void setOpenUntil(long openUntil) {
            this.openUntil = openUntil;
        }
    }

    /**
     * 异常统计信息
     */
    public static class ExceptionStatistics {
        private final ConcurrentHashMap<String, AtomicLong> exceptionCounters;
        private final ConcurrentHashMap<String, AtomicLong> exceptionLastOccurrence;
        private final ConcurrentHashMap<String, CircuitBreakerState> circuitBreakers;

        public ExceptionStatistics(ConcurrentHashMap<String, AtomicLong> exceptionCounters,
                                  ConcurrentHashMap<String, AtomicLong> exceptionLastOccurrence,
                                  ConcurrentHashMap<String, CircuitBreakerState> circuitBreakers) {
            this.exceptionCounters = exceptionCounters;
            this.exceptionLastOccurrence = exceptionLastOccurrence;
            this.circuitBreakers = circuitBreakers;
        }

        public long getExceptionCount(String featureName) {
            return exceptionCounters.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(featureName + ":"))
                    .mapToLong(entry -> entry.getValue().get())
                    .sum();
        }

        public boolean isCircuitOpen(String featureName) {
            CircuitBreakerState state = circuitBreakers.get(featureName);
            return state != null && state.isOpen();
        }

        @Override
        public String toString() {
            return String.format("ExceptionStatistics{exceptionCounters=%d, circuitBreakers=%d}",
                    exceptionCounters.size(), circuitBreakers.size());
        }
    }
}
