package com.basebackend.common.ratelimit;

import com.basebackend.common.ratelimit.impl.FixedWindowRateLimiter;
import com.basebackend.common.ratelimit.impl.SlidingWindowRateLimiter;
import com.basebackend.common.ratelimit.impl.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多算法限流器单元测试
 */
class RateLimiterTest {

    // ========== 滑动窗口 ==========

    @Test
    void slidingWindow_shouldAllowWithinLimit() {
        RateLimiter limiter = new SlidingWindowRateLimiter();
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("sw-allow", 5, 60));
        }
    }

    @Test
    void slidingWindow_shouldRejectExceedingLimit() {
        RateLimiter limiter = new SlidingWindowRateLimiter();
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire("sw-reject", 3, 60));
        }
        assertFalse(limiter.tryAcquire("sw-reject", 3, 60));
    }

    @Test
    void slidingWindow_shouldResetAfterWindowExpires() throws InterruptedException {
        RateLimiter limiter = new SlidingWindowRateLimiter();
        for (int i = 0; i < 2; i++) {
            assertTrue(limiter.tryAcquire("sw-expire", 2, 1));
        }
        assertFalse(limiter.tryAcquire("sw-expire", 2, 1));
        Thread.sleep(1100);
        assertTrue(limiter.tryAcquire("sw-expire", 2, 1));
    }

    // ========== 令牌桶 ==========

    @Test
    void tokenBucket_shouldAllowWithinCapacity() {
        RateLimiter limiter = new TokenBucketRateLimiter();
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("tb-allow", 5, 60));
        }
    }

    @Test
    void tokenBucket_shouldRejectWhenEmpty() {
        RateLimiter limiter = new TokenBucketRateLimiter();
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire("tb-reject", 10, 60));
        }
        assertFalse(limiter.tryAcquire("tb-reject", 10, 60));
    }

    @Test
    void tokenBucket_shouldRefillOverTime() throws InterruptedException {
        RateLimiter limiter = new TokenBucketRateLimiter();
        // capacity=2, windowSeconds=1 → rate=2 tokens/sec
        assertTrue(limiter.tryAcquire("tb-refill", 2, 1));
        assertTrue(limiter.tryAcquire("tb-refill", 2, 1));
        assertFalse(limiter.tryAcquire("tb-refill", 2, 1));
        Thread.sleep(1100);
        assertTrue(limiter.tryAcquire("tb-refill", 2, 1));
    }

    // ========== 固定窗口 ==========

    @Test
    void fixedWindow_shouldAllowWithinLimit() {
        RateLimiter limiter = new FixedWindowRateLimiter();
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire("fw-allow", 5, 60));
        }
    }

    @Test
    void fixedWindow_shouldRejectExceedingLimit() {
        RateLimiter limiter = new FixedWindowRateLimiter();
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire("fw-reject", 3, 60));
        }
        assertFalse(limiter.tryAcquire("fw-reject", 3, 60));
    }

    @Test
    void fixedWindow_shouldResetAfterWindowExpires() throws InterruptedException {
        RateLimiter limiter = new FixedWindowRateLimiter();
        for (int i = 0; i < 2; i++) {
            assertTrue(limiter.tryAcquire("fw-expire", 2, 1));
        }
        assertFalse(limiter.tryAcquire("fw-expire", 2, 1));
        Thread.sleep(1100);
        assertTrue(limiter.tryAcquire("fw-expire", 2, 1));
    }

    // ========== Key 隔离 ==========

    @Test
    void allAlgorithms_shouldIsolateKeys() {
        RateLimiter[] limiters = {
                new SlidingWindowRateLimiter(),
                new TokenBucketRateLimiter(),
                new FixedWindowRateLimiter()
        };
        for (RateLimiter limiter : limiters) {
            assertTrue(limiter.tryAcquire("iso-a", 1, 60));
            assertFalse(limiter.tryAcquire("iso-a", 1, 60));
            assertTrue(limiter.tryAcquire("iso-b", 1, 60));
        }
    }

    // ========== Registry ==========

    @Test
    void registry_shouldRouteByAlgorithm() {
        RateLimiter sw = new SlidingWindowRateLimiter();
        RateLimiter tb = new TokenBucketRateLimiter();
        RateLimiter fw = new FixedWindowRateLimiter();

        java.util.Map<RateLimitAlgorithm, RateLimiter> map = new java.util.EnumMap<>(RateLimitAlgorithm.class);
        map.put(RateLimitAlgorithm.SLIDING_WINDOW, sw);
        map.put(RateLimitAlgorithm.TOKEN_BUCKET, tb);
        map.put(RateLimitAlgorithm.FIXED_WINDOW, fw);

        RateLimiterRegistry registry = new RateLimiterRegistry(RateLimitAlgorithm.SLIDING_WINDOW, map);

        assertSame(sw, registry.getDefaultLimiter());
        assertSame(sw, registry.getLimiter(RateLimitAlgorithm.DEFAULT));
        assertSame(sw, registry.getLimiter(RateLimitAlgorithm.SLIDING_WINDOW));
        assertSame(tb, registry.getLimiter(RateLimitAlgorithm.TOKEN_BUCKET));
        assertSame(fw, registry.getLimiter(RateLimitAlgorithm.FIXED_WINDOW));
    }
}
