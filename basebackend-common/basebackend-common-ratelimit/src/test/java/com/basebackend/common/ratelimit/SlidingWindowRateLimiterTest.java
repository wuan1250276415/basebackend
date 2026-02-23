package com.basebackend.common.ratelimit;

import com.basebackend.common.ratelimit.impl.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowRateLimiterTest {

    private SlidingWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new SlidingWindowRateLimiter();
    }

    @Test
    void shouldAllowRequestsWithinLimit() {
        String key = "test-key";
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire(key, 5, 60));
        }
    }

    @Test
    void shouldRejectRequestsExceedingLimit() {
        String key = "test-key-exceed";
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.tryAcquire(key, 3, 60));
        }
        assertFalse(rateLimiter.tryAcquire(key, 3, 60));
        assertFalse(rateLimiter.tryAcquire(key, 3, 60));
    }

    @Test
    void shouldResetAfterWindowExpires() throws InterruptedException {
        String key = "test-key-expire";
        for (int i = 0; i < 2; i++) {
            assertTrue(rateLimiter.tryAcquire(key, 2, 1));
        }
        assertFalse(rateLimiter.tryAcquire(key, 2, 1));

        Thread.sleep(1100);

        assertTrue(rateLimiter.tryAcquire(key, 2, 1));
    }

    @Test
    void shouldIsolateKeysBetweenDifferentKeys() {
        assertTrue(rateLimiter.tryAcquire("key-a", 1, 60));
        assertFalse(rateLimiter.tryAcquire("key-a", 1, 60));
        assertTrue(rateLimiter.tryAcquire("key-b", 1, 60));
    }
}
