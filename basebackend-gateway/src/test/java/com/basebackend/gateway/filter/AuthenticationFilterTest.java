package com.basebackend.gateway.filter;

import com.basebackend.gateway.config.GatewaySecurityProperties;
import com.basebackend.gateway.enums.GatewayErrorCode;
import com.basebackend.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthenticationFilter 单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationFilter 单元测试")
class AuthenticationFilterTest {

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

        @Mock
        private ReactiveValueOperations<String, Object> valueOperations;

        @Mock
        private GatewayFilterChain filterChain;

        private GatewaySecurityProperties securityProperties;
        private AuthenticationFilter authenticationFilter;

        @BeforeEach
        void setUp() {
                securityProperties = new GatewaySecurityProperties();
                securityProperties.setRedisTimeout(Duration.ofSeconds(2));
                securityProperties.setStrictMode(true);
                securityProperties.setDebugLogging(false);

                // 设置默认白名单
                securityProperties.setWhitelist(Arrays.asList(
                                "/basebackend-user-api/api/user/auth/**",
                                "/api/public/**"));
                securityProperties.setActuatorWhitelist(Arrays.asList(
                                "/actuator/health",
                                "/actuator/info"));

                authenticationFilter = new AuthenticationFilter(
                                jwtUtil,
                                reactiveRedisTemplate,
                                securityProperties);
        }

        @Nested
        @DisplayName("白名单路径测试")
        class WhitelistTests {

                @Test
                @DisplayName("白名单路径应该跳过认证")
                void shouldSkipAuthenticationForWhitelistPath() {
                        // Given
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/basebackend-user-api/api/user/auth/login")
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(any())).thenReturn(Mono.empty());

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        verify(filterChain).filter(exchange);
                        verify(jwtUtil, never()).validateToken(anyString());
                }

                @Test
                @DisplayName("受限的 actuator 端点应该可以访问")
                void shouldAllowHealthEndpoint() {
                        // Given
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/actuator/health")
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(filterChain.filter(any())).thenReturn(Mono.empty());

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        verify(filterChain).filter(exchange);
                }

                @Test
                @DisplayName("未配置的 actuator 端点应该需要认证")
                void shouldRequireAuthForUnauthorizedActuatorEndpoint() {
                        // Given - 未在白名单中的 actuator 端点
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/actuator/env")
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        // When & Then - 没有 token，应该返回 401
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        StepVerifier.create(result)
                                        .verifyComplete();

                        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                }
        }

        @Nested
        @DisplayName("Token 验证测试")
        class TokenValidationTests {

                @Test
                @DisplayName("缺少 Token 应该返回 401")
                void shouldReturn401WhenTokenMissing() {
                        // Given
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/protected/resource")
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                        verify(filterChain, never()).filter(any());
                }

                @Test
                @DisplayName("无效的 Token 应该返回 401")
                void shouldReturn401WhenTokenInvalid() {
                        // Given
                        String invalidToken = "invalid.jwt.token";
                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/protected/resource")
                                        .header("Authorization", "Bearer " + invalidToken)
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                }

                @Test
                @DisplayName("有效的 Token 并且 Redis 验证通过应该放行")
                void shouldPassWhenTokenValidAndRedisMatch() {
                        // Given
                        String validToken = "valid.jwt.token";
                        Long userId = 12345L;

                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/protected/resource")
                                        .header("Authorization", "Bearer " + validToken)
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtUtil.validateToken(validToken)).thenReturn(true);
                        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(userId);
                        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
                        when(valueOperations.get("login_tokens:" + userId)).thenReturn(Mono.just(validToken));
                        when(filterChain.filter(any())).thenReturn(Mono.empty());

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        verify(filterChain).filter(any());
                }

                @Test
                @DisplayName("Token 与 Redis 中不一致应该返回 401")
                void shouldReturn401WhenTokenMismatchWithRedis() {
                        // Given
                        String validToken = "valid.jwt.token";
                        String differentToken = "different.jwt.token";
                        Long userId = 12345L;

                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/protected/resource")
                                        .header("Authorization", "Bearer " + validToken)
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtUtil.validateToken(validToken)).thenReturn(true);
                        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(userId);
                        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
                        when(valueOperations.get("login_tokens:" + userId)).thenReturn(Mono.just(differentToken));

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                }

                @Test
                @DisplayName("Redis 中不存在 Token 应该返回 401（强制下线场景）")
                void shouldReturn401WhenTokenNotInRedis() {
                        // Given
                        String validToken = "valid.jwt.token";
                        Long userId = 12345L;

                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/protected/resource")
                                        .header("Authorization", "Bearer " + validToken)
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtUtil.validateToken(validToken)).thenReturn(true);
                        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(userId);
                        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
                        when(valueOperations.get("login_tokens:" + userId)).thenReturn(Mono.empty());

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                }
        }

        @Nested
        @DisplayName("Redis 超时测试")
        class RedisTimeoutTests {

                @Test
                @DisplayName("Redis 操作超时应该返回服务繁忙错误 (503)")
                void shouldReturn503WhenRedisTimeout() {
                        // Given
                        String validToken = "valid.jwt.token";
                        Long userId = 12345L;

                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/api/protected/resource")
                                        .header("Authorization", "Bearer " + validToken)
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        when(jwtUtil.validateToken(validToken)).thenReturn(true);
                        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(userId);
                        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
                        // 模拟 Redis 延迟响应（超过配置的 2 秒）
                        when(valueOperations.get("login_tokens:" + userId))
                                        .thenReturn(Mono.delay(Duration.ofSeconds(5)).then(Mono.just(validToken)));

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        // Redis 超时应该返回 503（服务繁忙），而不是 401（未授权）
                        // 这是更合适的语义：表示认证服务暂时不可用
                        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
                }
        }

        @Nested
        @DisplayName("过滤器顺序测试")
        class FilterOrderTests {

                @Test
                @DisplayName("过滤器顺序应该是 -100")
                void shouldHaveCorrectOrder() {
                        assertEquals(-100, authenticationFilter.getOrder());
                }
        }

        @Nested
        @DisplayName("配置属性测试")
        class ConfigurationTests {

                @Test
                @DisplayName("空白名单在严格模式下应该要求所有路径认证")
                void shouldRequireAuthForAllPathsWhenWhitelistEmptyInStrictMode() {
                        // Given
                        securityProperties.setWhitelist(Collections.emptyList());
                        securityProperties.setActuatorWhitelist(Collections.emptyList());
                        securityProperties.setStrictMode(true);

                        MockServerHttpRequest request = MockServerHttpRequest
                                        .get("/any/path")
                                        .build();
                        MockServerWebExchange exchange = MockServerWebExchange.from(request);

                        // When
                        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

                        // Then
                        StepVerifier.create(result)
                                        .verifyComplete();

                        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
                }
        }
}
