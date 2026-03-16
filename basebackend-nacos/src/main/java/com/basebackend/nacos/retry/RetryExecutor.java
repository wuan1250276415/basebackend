/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.basebackend.nacos.retry;

import java.util.concurrent.Callable;
import java.util.function.Predicate;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryExecutor {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final long DEFAULT_INITIAL_DELAY_MS = 1000L;
    public static final long DEFAULT_MAX_DELAY_MS = 10000L;
    public static final double DEFAULT_MULTIPLIER = 2.0;
    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double multiplier;

    public RetryExecutor() {
        this(3, 1000L, 10000L, 2.0);
    }

    public RetryExecutor(int maxRetries, long initialDelayMs, long maxDelayMs, double multiplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
    }

    public <T> T execute(Callable<T> operation, Predicate<Exception> retryOn) throws Exception {
        Exception lastException = null;
        long delay = this.initialDelayMs;
        for (int attempt = 0; attempt <= this.maxRetries; ++attempt) {
            try {
                if (attempt > 0) {
                    log.info("Retry attempt {} of {}", (Object)attempt, (Object)this.maxRetries);
                }
                return operation.call();
            }
            catch (Exception e) {
                lastException = e;
                if (attempt >= this.maxRetries || !retryOn.test(e)) {
                    log.error("Operation failed after {} attempts: {}", (Object)(attempt + 1), (Object)e.getMessage());
                    throw e;
                }
                log.warn("Operation failed, will retry in {}ms: {}", (Object)delay, (Object)e.getMessage());
                try {
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                delay = Math.min((long)((double)delay * this.multiplier), this.maxDelayMs);
                continue;
            }
        }
        throw lastException;
    }

    public <T> T execute(Callable<T> operation) throws Exception {
        return this.execute(operation, e -> true);
    }

    public void executeVoid(Runnable operation, Predicate<Exception> retryOn) throws Exception {
        this.execute(() -> {
            operation.run();
            return null;
        }, retryOn);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxRetries = 3;
        private long initialDelayMs = 1000L;
        private long maxDelayMs = 10000L;
        private double multiplier = 2.0;

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
            return new RetryExecutor(this.maxRetries, this.initialDelayMs, this.maxDelayMs, this.multiplier);
        }
    }
}

