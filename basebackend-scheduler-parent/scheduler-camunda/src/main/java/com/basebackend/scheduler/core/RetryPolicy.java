package com.basebackend.scheduler.core;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 重试策略接口，负责定义可重试性判定与下一次重试延迟。
 */
public interface RetryPolicy {

    /**
     * 判断当前是否允许重试。
     *
     * @param currentRetryCount 已经尝试的重试次数（从 0 开始）
     * @param lastResult        上一次尝试的结果，可为空
     * @param lastException     上一次尝试的异常，可为空
     * @return 是否允许重试
     */
    boolean canRetry(int currentRetryCount, TaskResult lastResult, Exception lastException);

    /**
     * 计算下一次重试前的等待时间。
     *
     * @param currentRetryCount 已经尝试的重试次数（从 0 开始）
     * @return 等待时间，非正值表示立即重试
     */
    Duration nextDelay(int currentRetryCount);

    /**
     * 无重试策略。
     *
     * @return 策略实例
     */
    static RetryPolicy noRetry() {
        return new NoRetry();
    }

    /**
     * 固定间隔重试策略。
     *
     * @param maxRetries   最大重试次数
     * @param delay        固定延迟
     * @return 策略实例
     */
    static RetryPolicy fixedDelay(int maxRetries, Duration delay) {
        return new FixedDelay(maxRetries, delay, delay, ex -> true);
    }

    /**
     * 固定间隔重试策略，带最大间隔与重试条件。
     *
     * @param maxRetries     最大重试次数
     * @param delay          固定延迟
     * @param maxInterval    最大延迟
     * @param retryCondition 重试条件判断
     * @return 策略实例
     */
    static RetryPolicy fixedDelay(int maxRetries, Duration delay, Duration maxInterval, Predicate<Exception> retryCondition) {
        return new FixedDelay(maxRetries, delay, maxInterval, retryCondition);
    }

    /**
     * 指数退避策略。
     *
     * @param maxRetries     最大重试次数
     * @param baseDelay      基础延迟
     * @param maxInterval    最大间隔
     * @param retryCondition 重试条件判断
     * @return 策略实例
     */
    static RetryPolicy exponentialBackoff(int maxRetries, Duration baseDelay, Duration maxInterval, Predicate<Exception> retryCondition) {
        return new ExponentialBackoff(maxRetries, baseDelay, maxInterval, retryCondition);
    }

    final class NoRetry implements RetryPolicy {
        @Override
        public boolean canRetry(int currentRetryCount, TaskResult lastResult, Exception lastException) {
            return false;
        }

        @Override
        public Duration nextDelay(int currentRetryCount) {
            return Duration.ZERO;
        }
    }

    final class FixedDelay implements RetryPolicy {
        private final int maxRetries;
        private final Duration delay;
        private final Duration maxInterval;
        private final Predicate<Exception> retryCondition;

        FixedDelay(int maxRetries, Duration delay, Duration maxInterval, Predicate<Exception> retryCondition) {
            this.maxRetries = Math.max(0, maxRetries);
            this.delay = Objects.requireNonNull(delay, "delay");
            this.maxInterval = Objects.requireNonNull(maxInterval, "maxInterval");
            this.retryCondition = Objects.requireNonNull(retryCondition, "retryCondition");
        }

        @Override
        public boolean canRetry(int currentRetryCount, TaskResult lastResult, Exception lastException) {
            if (currentRetryCount >= maxRetries) {
                return false;
            }
            if (lastResult != null && lastResult.getStatus() == TaskResult.Status.CANCELLED) {
                return false;
            }
            return lastException == null || retryCondition.test(lastException);
        }

        @Override
        public Duration nextDelay(int currentRetryCount) {
            Duration effective = delay;
            if (effective.compareTo(maxInterval) > 0) {
                effective = maxInterval;
            }
            return effective.isNegative() ? Duration.ZERO : effective;
        }
    }

    final class ExponentialBackoff implements RetryPolicy {
        private final int maxRetries;
        private final Duration baseDelay;
        private final Duration maxInterval;
        private final Predicate<Exception> retryCondition;

        ExponentialBackoff(int maxRetries, Duration baseDelay, Duration maxInterval, Predicate<Exception> retryCondition) {
            this.maxRetries = Math.max(0, maxRetries);
            this.baseDelay = Objects.requireNonNull(baseDelay, "baseDelay");
            this.maxInterval = Objects.requireNonNull(maxInterval, "maxInterval");
            this.retryCondition = Objects.requireNonNull(retryCondition, "retryCondition");
        }

        @Override
        public boolean canRetry(int currentRetryCount, TaskResult lastResult, Exception lastException) {
            if (currentRetryCount >= maxRetries) {
                return false;
            }
            if (lastResult != null && lastResult.getStatus() == TaskResult.Status.CANCELLED) {
                return false;
            }
            return lastException == null || retryCondition.test(lastException);
        }

        @Override
        public Duration nextDelay(int currentRetryCount) {
            if (currentRetryCount <= 0) {
                return baseDelay;
            }
            long multiplier = 1L << Math.min(30, currentRetryCount - 1);
            Duration computed = baseDelay.multipliedBy(multiplier);
            if (computed.compareTo(maxInterval) > 0) {
                computed = maxInterval;
            }
            return computed.isNegative() ? Duration.ZERO : computed;
        }
    }
}
