package com.basebackend.nacos.retry;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * 重试执行器
 * <p>
 * 提供通用的重试机制，支持指数退避策略。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class RetryExecutor {

    /** 默认最大重试次数 */
    public static final int DEFAULT_MAX_RETRIES = 3;

    /** 默认初始延迟（毫秒） */
    public static final long DEFAULT_INITIAL_DELAY_MS = 1000;

    /** 默认最大延迟（毫秒） */
    public static final long DEFAULT_MAX_DELAY_MS = 10000;

    /** 退避乘数 */
    public static final double DEFAULT_MULTIPLIER = 2.0;

    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double multiplier;

    public RetryExecutor() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS, DEFAULT_MAX_DELAY_MS, DEFAULT_MULTIPLIER);
    }

    public RetryExecutor(int maxRetries, long initialDelayMs, long maxDelayMs, double multiplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
    }

    /**
     * 执行可重试的操作
     *
     * @param operation 要执行的操作
     * @param retryOn   判断是否需要重试的条件
     * @param <T>       返回类型
     * @return 操作结果
     * @throws Exception 最终失败时抛出异常
     */
    public <T> T execute(Callable<T> operation, Predicate<Exception> retryOn) throws Exception {
        Exception lastException = null;
        long delay = initialDelayMs;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {} of {}", attempt, maxRetries);
                }
                return operation.call();
            } catch (Exception e) {
                lastException = e;

                if (attempt >= maxRetries || !retryOn.test(e)) {
                    log.error("Operation failed after {} attempts: {}", attempt + 1, e.getMessage());
                    throw e;
                }

                log.warn("Operation failed, will retry in {}ms: {}", delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                // 指数退避
                delay = Math.min((long) (delay * multiplier), maxDelayMs);
            }
        }

        throw lastException;
    }

    /**
     * 执行可重试的操作（对所有异常重试）
     *
     * @param operation 要执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     */
    public <T> T execute(Callable<T> operation) throws Exception {
        return execute(operation, e -> true);
    }

    /**
     * 执行可重试的操作（无返回值）
     *
     * @param operation 要执行的操作
     * @param retryOn   判断是否需要重试的条件
     */
    public void executeVoid(Runnable operation, Predicate<Exception> retryOn) throws Exception {
        execute(() -> {
            operation.run();
            return null;
        }, retryOn);
    }

    /**
     * 创建Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder类
     */
    public static class Builder {
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private long initialDelayMs = DEFAULT_INITIAL_DELAY_MS;
        private long maxDelayMs = DEFAULT_MAX_DELAY_MS;
        private double multiplier = DEFAULT_MULTIPLIER;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
            return this;
        }

        public Builder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public RetryExecutor build() {
            return new RetryExecutor(maxRetries, initialDelayMs, maxDelayMs, multiplier);
        }
    }
}
