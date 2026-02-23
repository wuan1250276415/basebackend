package com.basebackend.common.ratelimit.impl;

import com.basebackend.common.ratelimit.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 固定窗口限流器（内存版）
 * <p>
 * 按固定时间窗口计数，窗口结束后计数器重置。
 * 实现简单，但存在临界突发问题（两个窗口交界处可能通过 2 倍限制的请求）。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class FixedWindowRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(windowSeconds));
        return counter.tryIncrement(limit, windowSeconds);
    }

    private static class WindowCounter {
        private final AtomicLong count = new AtomicLong(0);
        private volatile long windowStart;
        private final Object resetLock = new Object();

        WindowCounter(int windowSeconds) {
            this.windowStart = currentWindowStart(windowSeconds);
        }

        boolean tryIncrement(int limit, int windowSeconds) {
            long currentWindow = currentWindowStart(windowSeconds);

            if (currentWindow != windowStart) {
                synchronized (resetLock) {
                    if (currentWindow != windowStart) {
                        count.set(0);
                        windowStart = currentWindow;
                    }
                }
            }

            return count.incrementAndGet() <= limit;
        }

        private static long currentWindowStart(int windowSeconds) {
            long nowSeconds = System.currentTimeMillis() / 1000;
            return nowSeconds - (nowSeconds % windowSeconds);
        }
    }
}
