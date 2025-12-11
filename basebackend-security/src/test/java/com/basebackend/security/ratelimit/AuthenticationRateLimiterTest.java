package com.basebackend.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthenticationRateLimiter 单元测试（使用本地缓存模式）
 */
@DisplayName("AuthenticationRateLimiter 单元测试")
class AuthenticationRateLimiterTest {

    private AuthenticationRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // 使用本地缓存模式（不注入Redis）
        rateLimiter = new AuthenticationRateLimiter(null);
    }

    @Test
    @DisplayName("首次尝试应该被允许")
    void testFirstAttemptAllowed() {
        assertTrue(rateLimiter.isAllowed("user@test.com"));
    }

    @Test
    @DisplayName("多次失败后应该被封禁")
    void testBlockAfterMaxAttempts() {
        String identifier = "blocked-user@test.com";

        // 记录5次失败尝试（默认最大次数）
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(identifier);
        }

        // 应该被封禁
        assertFalse(rateLimiter.isAllowed(identifier));
    }

    @Test
    @DisplayName("未达到最大次数时仍然允许")
    void testAllowedBeforeMaxAttempts() {
        String identifier = "partial-fail@test.com";

        // 记录4次失败尝试（少于默认最大次数5）
        for (int i = 0; i < 4; i++) {
            rateLimiter.recordFailedAttempt(identifier);
        }

        // 应该仍然允许
        assertTrue(rateLimiter.isAllowed(identifier));
    }

    @Test
    @DisplayName("清除失败记录后应该允许")
    void testClearFailedAttempts() {
        String identifier = "cleared-user@test.com";

        // 记录5次失败尝试
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(identifier);
        }

        // 确认被封禁
        assertFalse(rateLimiter.isAllowed(identifier));

        // 清除记录
        rateLimiter.clearFailedAttempts(identifier);

        // 应该允许
        assertTrue(rateLimiter.isAllowed(identifier));
    }

    @Test
    @DisplayName("null标识符应该被允许")
    void testNullIdentifierAllowed() {
        assertTrue(rateLimiter.isAllowed(null));
    }

    @Test
    @DisplayName("空标识符应该被允许")
    void testEmptyIdentifierAllowed() {
        assertTrue(rateLimiter.isAllowed(""));
        assertTrue(rateLimiter.isAllowed("   "));
    }

    @Test
    @DisplayName("不同标识符相互独立")
    void testIndependentIdentifiers() {
        String user1 = "user1@test.com";
        String user2 = "user2@test.com";

        // user1 被封禁
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(user1);
        }

        // user1 被封禁，user2 仍然允许
        assertFalse(rateLimiter.isAllowed(user1));
        assertTrue(rateLimiter.isAllowed(user2));
    }

    @Test
    @DisplayName("获取剩余封禁时间")
    void testGetRemainingBlockTime() {
        String identifier = "timed-user@test.com";

        // 未被封禁时返回0
        assertEquals(0, rateLimiter.getRemainingBlockTime(identifier));

        // 封禁用户
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailedAttempt(identifier);
        }

        // 应该有剩余时间
        long remaining = rateLimiter.getRemainingBlockTime(identifier);
        assertTrue(remaining > 0);
    }

    @Test
    @DisplayName("记录失败尝试对null标识符无效")
    void testRecordFailedAttemptNullIdentifier() {
        // 不应该抛出异常
        assertDoesNotThrow(() -> rateLimiter.recordFailedAttempt(null));
        assertDoesNotThrow(() -> rateLimiter.recordFailedAttempt(""));
    }
}
