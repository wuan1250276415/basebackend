package com.basebackend.common.ratelimit.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限流器实现单元测试（FixedWindow / SlidingWindow / TokenBucket）
 */
class RateLimiterImplTest {

    // ========== FixedWindowRateLimiter ==========

    @Nested
    @DisplayName("FixedWindowRateLimiter")
    class FixedWindowTest {

        private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter();

        @Test
        @DisplayName("限制内的请求允许通过")
        void shouldAllowWithinLimit() {
            for (int i = 0; i < 10; i++) {
                assertThat(limiter.tryAcquire("fw-key", 10, 60)).isTrue();
            }
        }

        @Test
        @DisplayName("超出限制的请求被拒绝")
        void shouldRejectOverLimit() {
            for (int i = 0; i < 5; i++) {
                limiter.tryAcquire("fw-reject", 5, 60);
            }
            assertThat(limiter.tryAcquire("fw-reject", 5, 60)).isFalse();
        }

        @Test
        @DisplayName("不同 key 互不影响")
        void shouldNotInterfereKeys() {
            for (int i = 0; i < 3; i++) {
                limiter.tryAcquire("fw-a", 3, 60);
            }
            assertThat(limiter.tryAcquire("fw-a", 3, 60)).isFalse();
            assertThat(limiter.tryAcquire("fw-b", 3, 60)).isTrue();
        }

        @Test
        @DisplayName("limit=1 只允许一次")
        void shouldAllowOnlyOne() {
            assertThat(limiter.tryAcquire("fw-one", 1, 60)).isTrue();
            assertThat(limiter.tryAcquire("fw-one", 1, 60)).isFalse();
        }
    }

    // ========== SlidingWindowRateLimiter ==========

    @Nested
    @DisplayName("SlidingWindowRateLimiter")
    class SlidingWindowTest {

        private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(100, 60);

        @Test
        @DisplayName("限制内的请求允许通过")
        void shouldAllowWithinLimit() {
            for (int i = 0; i < 10; i++) {
                assertThat(limiter.tryAcquire("sw-key", 10, 60)).isTrue();
            }
        }

        @Test
        @DisplayName("超出限制的请求被拒绝")
        void shouldRejectOverLimit() {
            for (int i = 0; i < 5; i++) {
                limiter.tryAcquire("sw-reject", 5, 60);
            }
            assertThat(limiter.tryAcquire("sw-reject", 5, 60)).isFalse();
        }

        @Test
        @DisplayName("不同 key 互不影响")
        void shouldNotInterfereKeys() {
            for (int i = 0; i < 3; i++) {
                limiter.tryAcquire("sw-a", 3, 60);
            }
            assertThat(limiter.tryAcquire("sw-a", 3, 60)).isFalse();
            assertThat(limiter.tryAcquire("sw-b", 3, 60)).isTrue();
        }

        @Test
        @DisplayName("getKeyCount 正确计数")
        void shouldTrackKeyCount() {
            limiter.tryAcquire("sw-c1", 10, 60);
            limiter.tryAcquire("sw-c2", 10, 60);
            assertThat(limiter.getKeyCount()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("cleanup 清理空窗口")
        void shouldCleanupEmptyWindows() {
            limiter.tryAcquire("sw-empty", 10, 1);
            // 等窗口过期后 cleanup
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            limiter.cleanup();
            // cleanup 后空窗口被移除
        }

        @Test
        @DisplayName("窗口过期后可重新获取")
        void shouldAllowAfterWindowExpiry() throws Exception {
            for (int i = 0; i < 3; i++) {
                limiter.tryAcquire("sw-expire", 3, 1);
            }
            assertThat(limiter.tryAcquire("sw-expire", 3, 1)).isFalse();
            Thread.sleep(1100);
            assertThat(limiter.tryAcquire("sw-expire", 3, 1)).isTrue();
        }
    }

    // ========== TokenBucketRateLimiter ==========

    @Nested
    @DisplayName("TokenBucketRateLimiter")
    class TokenBucketTest {

        private final TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();

        @Test
        @DisplayName("桶满时允许全部通过")
        void shouldAllowFullBucket() {
            for (int i = 0; i < 10; i++) {
                assertThat(limiter.tryAcquire("tb-key", 10, 60)).isTrue();
            }
        }

        @Test
        @DisplayName("桶空时拒绝请求")
        void shouldRejectWhenEmpty() {
            for (int i = 0; i < 5; i++) {
                limiter.tryAcquire("tb-empty", 5, 60);
            }
            assertThat(limiter.tryAcquire("tb-empty", 5, 60)).isFalse();
        }

        @Test
        @DisplayName("令牌补充后恢复")
        void shouldRefillOverTime() throws Exception {
            // capacity=2, windowSeconds=1 → rate=2 tokens/s
            limiter.tryAcquire("tb-refill", 2, 1);
            limiter.tryAcquire("tb-refill", 2, 1);
            assertThat(limiter.tryAcquire("tb-refill", 2, 1)).isFalse();
            Thread.sleep(600); // 等 0.6s，补充 ~1.2 tokens
            assertThat(limiter.tryAcquire("tb-refill", 2, 1)).isTrue();
        }

        @Test
        @DisplayName("不同 key 互不影响")
        void shouldNotInterfereKeys() {
            for (int i = 0; i < 3; i++) {
                limiter.tryAcquire("tb-a", 3, 60);
            }
            assertThat(limiter.tryAcquire("tb-a", 3, 60)).isFalse();
            assertThat(limiter.tryAcquire("tb-b", 3, 60)).isTrue();
        }
    }
}
