package com.basebackend.scheduler.core.circuitbreaker;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 断路器服务
 * <p>
 * 实现断路器模式，防止级联故障。支持三种状态：
 * <ul>
 * <li>CLOSED - 正常状态，允许请求通过</li>
 * <li>OPEN - 打开状态，请求被拒绝，直接返回降级结果</li>
 * <li>HALF_OPEN - 半开状态，允许有限请求通过以测试服务是否恢复</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
public class CircuitBreakerService {

    /**
     * 断路器状态
     */
    public enum State {
        CLOSED, // 关闭状态 - 正常工作
        OPEN, // 打开状态 - 拒绝请求
        HALF_OPEN // 半开状态 - 允许有限请求测试
    }

    /**
     * 断路器配置
     */
    private final CircuitBreakerConfig config;

    /**
     * 每个名称对应的断路器实例
     */
    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param config 断路器配置
     */
    public CircuitBreakerService(CircuitBreakerConfig config) {
        this.config = config;
        log.info("CircuitBreakerService initialized with config: {}", config);
    }

    /**
     * 默认构造函数
     */
    public CircuitBreakerService() {
        this(new CircuitBreakerConfig(
                50, // 50% 失败率阈值
                100, // 滑动窗口大小
                10, // 最小调用次数
                Duration.ofSeconds(60), // 等待时间60秒
                5 // 半开状态允许5次调用
        ));
    }

    /**
     * 执行带断路器保护的操作
     *
     * @param name     断路器名称
     * @param supplier 要执行的操作
     * @param <T>      返回类型
     * @return 执行结果
     * @throws CircuitBreakerOpenException 断路器打开时抛出
     */
    public <T> T execute(String name, Supplier<T> supplier) {
        CircuitBreaker cb = getOrCreateCircuitBreaker(name);

        if (!cb.allowRequest()) {
            throw new CircuitBreakerOpenException("Circuit breaker '" + name + "' is open");
        }

        try {
            T result = supplier.get();
            cb.recordSuccess();
            return result;
        } catch (Exception e) {
            cb.recordFailure();
            throw e;
        }
    }

    /**
     * 执行带断路器保护和降级的操作
     *
     * @param name     断路器名称
     * @param supplier 要执行的操作
     * @param fallback 降级操作
     * @param <T>      返回类型
     * @return 执行结果或降级结果
     */
    public <T> T executeWithFallback(String name, Supplier<T> supplier, Supplier<T> fallback) {
        CircuitBreaker cb = getOrCreateCircuitBreaker(name);

        if (!cb.allowRequest()) {
            log.debug("Circuit breaker '{}' is open, executing fallback", name);
            return fallback.get();
        }

        try {
            T result = supplier.get();
            cb.recordSuccess();
            return result;
        } catch (Exception e) {
            cb.recordFailure();
            log.debug("Execution failed for circuit breaker '{}', executing fallback: {}", name, e.getMessage());
            return fallback.get();
        }
    }

    /**
     * 获取断路器状态
     *
     * @param name 断路器名称
     * @return 当前状态
     */
    public State getState(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        return cb != null ? cb.getState() : State.CLOSED;
    }

    /**
     * 重置断路器
     *
     * @param name 断路器名称
     */
    public void reset(String name) {
        CircuitBreaker cb = circuitBreakers.get(name);
        if (cb != null) {
            cb.reset();
            log.info("Circuit breaker '{}' has been reset", name);
        }
    }

    /**
     * 获取或创建断路器
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String name) {
        return circuitBreakers.computeIfAbsent(name, k -> new CircuitBreaker(name, config));
    }

    /**
     * 断路器配置
     */
    @Data
    public static class CircuitBreakerConfig {
        /**
         * 失败率阈值（百分比）
         */
        private final int failureRateThreshold;

        /**
         * 滑动窗口大小
         */
        private final int slidingWindowSize;

        /**
         * 最小调用次数（达到此数量后才计算失败率）
         */
        private final int minimumNumberOfCalls;

        /**
         * 断路器打开后的等待时间
         */
        private final Duration waitDurationInOpenState;

        /**
         * 半开状态允许的调用次数
         */
        private final int permittedNumberOfCallsInHalfOpenState;

        public CircuitBreakerConfig(int failureRateThreshold, int slidingWindowSize,
                int minimumNumberOfCalls, Duration waitDurationInOpenState,
                int permittedNumberOfCallsInHalfOpenState) {
            this.failureRateThreshold = failureRateThreshold;
            this.slidingWindowSize = slidingWindowSize;
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            this.waitDurationInOpenState = waitDurationInOpenState;
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
        }
    }

    /**
     * 断路器打开异常
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    /**
     * 断路器实例
     */
    private static class CircuitBreaker {
        private final String name;
        private final CircuitBreakerConfig config;
        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private final AtomicInteger failedCalls = new AtomicInteger(0);
        private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
        private volatile Instant openTime;

        CircuitBreaker(String name, CircuitBreakerConfig config) {
            this.name = name;
            this.config = config;
        }

        /**
         * 检查是否允许请求通过
         */
        boolean allowRequest() {
            State currentState = state.get();

            switch (currentState) {
                case CLOSED:
                    return true;

                case OPEN:
                    // 检查是否可以转换到半开状态
                    if (shouldTransitionToHalfOpen()) {
                        if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                            halfOpenCalls.set(0);
                            log.debug("Circuit breaker '{}' transitioned from OPEN to HALF_OPEN", name);
                        }
                        return halfOpenCalls.incrementAndGet() <= config.getPermittedNumberOfCallsInHalfOpenState();
                    }
                    return false;

                case HALF_OPEN:
                    return halfOpenCalls.incrementAndGet() <= config.getPermittedNumberOfCallsInHalfOpenState();

                default:
                    return false;
            }
        }

        /**
         * 记录成功调用
         */
        void recordSuccess() {
            totalCalls.incrementAndGet();

            State currentState = state.get();
            if (currentState == State.HALF_OPEN) {
                // 半开状态下成功，转回关闭状态
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    resetCounters();
                    log.debug("Circuit breaker '{}' transitioned from HALF_OPEN to CLOSED", name);
                }
            }
        }

        /**
         * 记录失败调用
         */
        void recordFailure() {
            totalCalls.incrementAndGet();
            failedCalls.incrementAndGet();

            State currentState = state.get();

            if (currentState == State.HALF_OPEN) {
                // 半开状态下失败，重新打开
                if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                    openTime = Instant.now();
                    log.debug("Circuit breaker '{}' transitioned from HALF_OPEN to OPEN", name);
                }
            } else if (currentState == State.CLOSED && shouldOpen()) {
                // 关闭状态下失败率超过阈值，打开断路器
                if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                    openTime = Instant.now();
                    log.info("Circuit breaker '{}' opened due to high failure rate", name);
                }
            }
        }

        /**
         * 检查是否应该打开断路器
         */
        private boolean shouldOpen() {
            int total = totalCalls.get();
            int failed = failedCalls.get();

            if (total < config.getMinimumNumberOfCalls()) {
                return false;
            }

            double failureRate = (failed * 100.0) / total;
            return failureRate >= config.getFailureRateThreshold();
        }

        /**
         * 检查是否应该转换到半开状态
         */
        private boolean shouldTransitionToHalfOpen() {
            if (openTime == null) {
                return true;
            }
            Duration elapsed = Duration.between(openTime, Instant.now());
            return elapsed.compareTo(config.getWaitDurationInOpenState()) >= 0;
        }

        /**
         * 获取当前状态
         */
        State getState() {
            return state.get();
        }

        /**
         * 重置断路器
         */
        void reset() {
            state.set(State.CLOSED);
            resetCounters();
            openTime = null;
        }

        /**
         * 重置计数器
         */
        private void resetCounters() {
            totalCalls.set(0);
            failedCalls.set(0);
            halfOpenCalls.set(0);
        }
    }
}
