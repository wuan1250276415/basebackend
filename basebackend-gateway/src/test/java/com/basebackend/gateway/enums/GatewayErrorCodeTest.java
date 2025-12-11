package com.basebackend.gateway.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GatewayErrorCode 单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@DisplayName("GatewayErrorCode 单元测试")
class GatewayErrorCodeTest {

    @Nested
    @DisplayName("错误码基本属性测试")
    class BasicPropertiesTests {

        @ParameterizedTest
        @EnumSource(GatewayErrorCode.class)
        @DisplayName("所有错误码应该有有效的 code")
        void shouldHaveValidCode(GatewayErrorCode errorCode) {
            assertNotNull(errorCode.getCode());
            assertTrue(errorCode.getCode() >= 6000 && errorCode.getCode() < 7000,
                    "错误码应该在 6000-6999 范围内: " + errorCode.name());
        }

        @ParameterizedTest
        @EnumSource(GatewayErrorCode.class)
        @DisplayName("所有错误码应该有非空的 message")
        void shouldHaveNonEmptyMessage(GatewayErrorCode errorCode) {
            assertNotNull(errorCode.getMessage());
            assertFalse(errorCode.getMessage().isEmpty());
        }

        @ParameterizedTest
        @EnumSource(GatewayErrorCode.class)
        @DisplayName("所有错误码应该有有效的 HTTP 状态码")
        void shouldHaveValidHttpStatus(GatewayErrorCode errorCode) {
            int httpStatus = errorCode.getHttpStatus();
            assertTrue(httpStatus >= 400 && httpStatus < 600,
                    "HTTP 状态码应该在 400-599 范围内: " + errorCode.name());
        }

        @Test
        @DisplayName("模块应该是 gateway")
        void shouldHaveGatewayModule() {
            assertEquals("gateway", GatewayErrorCode.TOKEN_MISSING.getModule());
        }
    }

    @Nested
    @DisplayName("认证错误码测试")
    class AuthErrorCodeTests {

        @Test
        @DisplayName("TOKEN_MISSING 应该返回 401 状态码")
        void tokenMissingShouldReturn401() {
            assertEquals(6000, GatewayErrorCode.TOKEN_MISSING.getCode());
            assertEquals(401, GatewayErrorCode.TOKEN_MISSING.getHttpStatus());
        }

        @Test
        @DisplayName("TOKEN_INVALID 应该返回 401 状态码")
        void tokenInvalidShouldReturn401() {
            assertEquals(6001, GatewayErrorCode.TOKEN_INVALID.getCode());
            assertEquals(401, GatewayErrorCode.TOKEN_INVALID.getHttpStatus());
        }

        @Test
        @DisplayName("TOKEN_EXPIRED 应该返回 401 状态码")
        void tokenExpiredShouldReturn401() {
            assertEquals(6002, GatewayErrorCode.TOKEN_EXPIRED.getCode());
            assertEquals(401, GatewayErrorCode.TOKEN_EXPIRED.getHttpStatus());
        }

        @Test
        @DisplayName("AUTH_SERVICE_BUSY 应该返回 503 状态码")
        void authServiceBusyShouldReturn503() {
            assertEquals(6005, GatewayErrorCode.AUTH_SERVICE_BUSY.getCode());
            assertEquals(503, GatewayErrorCode.AUTH_SERVICE_BUSY.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("限流错误码测试")
    class RateLimitErrorCodeTests {

        @Test
        @DisplayName("RATE_LIMITED 应该返回 429 状态码")
        void rateLimitedShouldReturn429() {
            assertEquals(6200, GatewayErrorCode.RATE_LIMITED.getCode());
            assertEquals(429, GatewayErrorCode.RATE_LIMITED.getHttpStatus());
        }

        @Test
        @DisplayName("CIRCUIT_BREAKER_OPEN 应该返回 503 状态码")
        void circuitBreakerOpenShouldReturn503() {
            assertEquals(6210, GatewayErrorCode.CIRCUIT_BREAKER_OPEN.getCode());
            assertEquals(503, GatewayErrorCode.CIRCUIT_BREAKER_OPEN.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("路由错误码测试")
    class RouteErrorCodeTests {

        @Test
        @DisplayName("SERVICE_UNAVAILABLE 应该返回 503 状态码")
        void serviceUnavailableShouldReturn503() {
            assertEquals(6100, GatewayErrorCode.SERVICE_UNAVAILABLE.getCode());
            assertEquals(503, GatewayErrorCode.SERVICE_UNAVAILABLE.getHttpStatus());
        }

        @Test
        @DisplayName("UPSTREAM_TIMEOUT 应该返回 504 状态码")
        void upstreamTimeoutShouldReturn504() {
            assertEquals(6103, GatewayErrorCode.UPSTREAM_TIMEOUT.getCode());
            assertEquals(504, GatewayErrorCode.UPSTREAM_TIMEOUT.getHttpStatus());
        }
    }
}
