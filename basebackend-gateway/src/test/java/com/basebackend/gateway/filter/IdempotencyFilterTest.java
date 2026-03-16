package com.basebackend.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyFilter 单元测试")
class IdempotencyFilterTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    @Mock
    private GatewayFilterChain filterChain;

    private IdempotencyFilter idempotencyFilter;

    @BeforeEach
    void setUp() {
        idempotencyFilter = new IdempotencyFilter(reactiveRedisTemplate);

        ReflectionTestUtils.setField(idempotencyFilter, "idempotencyEnabled", true);
        ReflectionTestUtils.setField(idempotencyFilter, "idempotencyTtl", 300L);
        ReflectionTestUtils.setField(idempotencyFilter, "idempotencyPaths", List.of("/api/orders/**"));

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("同一客户端幂等键在不同用户下应映射为不同 Redis 键")
    void shouldUseDifferentRedisKeyForDifferentUsersWithSameClientKey() {
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));
        when(valueOperations.set(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(idempotencyFilter.filter(createExchange("tenant-a", "user-a"), filterChain))
                .verifyComplete();
        StepVerifier.create(idempotencyFilter.filter(createExchange("tenant-a", "user-b"), filterChain))
                .verifyComplete();

        ArgumentCaptor<String> redisKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(2)).setIfAbsent(redisKeyCaptor.capture(), any(), any(Duration.class));
        List<String> redisKeys = redisKeyCaptor.getAllValues();

        assertThat(redisKeys).hasSize(2);
        assertThat(redisKeys.get(0)).isNotEqualTo(redisKeys.get(1));
        assertThat(redisKeys.get(0)).contains("tenant-a:user-a:POST:/api/orders:client-key-001");
        assertThat(redisKeys.get(1)).contains("tenant-a:user-b:POST:/api/orders:client-key-001");
    }

    @Test
    @DisplayName("同一用户同一路径重复请求应返回 409")
    void shouldReturnConflictWhenDuplicateRequestForSameUserAndPath() {
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE), Mono.just(Boolean.FALSE));
        when(valueOperations.get(anyString())).thenReturn(Mono.just("PROCESSING:trace-1"));
        when(valueOperations.set(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(idempotencyFilter.filter(createExchange("tenant-a", "user-a"), filterChain))
                .verifyComplete();

        MockServerWebExchange duplicateExchange = createExchange("tenant-a", "user-a");
        StepVerifier.create(idempotencyFilter.filter(duplicateExchange, filterChain))
                .verifyComplete();

        assertThat(duplicateExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(filterChain, times(1)).filter(any());
    }

    @Test
    @DisplayName("匿名场景下不同 Authorization 应映射为不同 Redis 键")
    void shouldUseDifferentRedisKeyForDifferentAuthorizationWhenUserMissing() {
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));
        when(valueOperations.set(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(idempotencyFilter.filter(
                        createExchange("tenant-a", null, "Bearer token-A"), filterChain))
                .verifyComplete();
        StepVerifier.create(idempotencyFilter.filter(
                        createExchange("tenant-a", null, "Bearer token-B"), filterChain))
                .verifyComplete();

        ArgumentCaptor<String> redisKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(2)).setIfAbsent(redisKeyCaptor.capture(), any(), any(Duration.class));
        List<String> redisKeys = redisKeyCaptor.getAllValues();

        assertThat(redisKeys).hasSize(2);
        assertThat(redisKeys.get(0)).isNotEqualTo(redisKeys.get(1));
        assertThat(redisKeys.get(0)).containsPattern("tenant-a:auth:[a-f0-9]{32}:POST:/api/orders:client-key-001");
        assertThat(redisKeys.get(1)).containsPattern("tenant-a:auth:[a-f0-9]{32}:POST:/api/orders:client-key-001");
        assertThat(redisKeys.get(0)).doesNotContain("token-A");
        assertThat(redisKeys.get(1)).doesNotContain("token-B");
    }

    @Test
    @DisplayName("匿名场景下同 Authorization 重复请求应返回 409")
    void shouldReturnConflictForDuplicateAnonymousRequestsWithSameAuthorization() {
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE), Mono.just(Boolean.FALSE));
        when(valueOperations.get(anyString())).thenReturn(Mono.just("PROCESSING:trace-1"));
        when(valueOperations.set(anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(idempotencyFilter.filter(
                        createExchange("tenant-a", null, "Bearer same-token"), filterChain))
                .verifyComplete();

        MockServerWebExchange duplicateExchange = createExchange("tenant-a", null, "Bearer same-token");
        StepVerifier.create(idempotencyFilter.filter(duplicateExchange, filterChain))
                .verifyComplete();

        assertThat(duplicateExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(filterChain, times(1)).filter(any());
    }

    private MockServerWebExchange createExchange(String tenantId, String userId) {
        return createExchange(tenantId, userId, null);
    }

    private MockServerWebExchange createExchange(String tenantId, String userId, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.post("/api/orders")
                .header("X-Tenant-Id", tenantId)
                .header("X-Idempotency-Key", "client-key-001");
        if (userId != null) {
            requestBuilder.header("X-User-Id", userId);
        }
        if (authorization != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MockServerWebExchange.from(requestBuilder.build());
    }
}
