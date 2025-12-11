package com.basebackend.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TraceIdFilter 单元测试
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TraceIdFilter 单元测试")
class TraceIdFilterTest {

    @Mock
    private GatewayFilterChain filterChain;

    private final TraceIdFilter traceIdFilter = new TraceIdFilter();

    @Nested
    @DisplayName("追踪 ID 生成测试")
    class TraceIdGenerationTests {

        @Test
        @DisplayName("应该为没有追踪 ID 的请求生成新 ID")
        void shouldGenerateTraceIdForNewRequest() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            // When
            Mono<Void> result = traceIdFilter.filter(exchange, filterChain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            // 验证响应头包含追踪 ID
            assertNotNull(exchange.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER));
        }

        @Test
        @DisplayName("应该继承上游传递的追踪 ID")
        void shouldInheritUpstreamTraceId() {
            // Given
            String upstreamTraceId = "upstream-trace-id-12345";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header(TraceIdFilter.TRACE_ID_HEADER, upstreamTraceId)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            // When
            Mono<Void> result = traceIdFilter.filter(exchange, filterChain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            // 验证使用了上游的追踪 ID
            assertEquals(upstreamTraceId,
                    exchange.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER));
        }

        @Test
        @DisplayName("应该兼容 X-Request-Id 头")
        void shouldFallbackToRequestIdHeader() {
            // Given
            String requestId = "request-id-12345";
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .header(TraceIdFilter.REQUEST_ID_HEADER, requestId)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            // When
            Mono<Void> result = traceIdFilter.filter(exchange, filterChain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            // 验证使用了 X-Request-Id
            assertEquals(requestId,
                    exchange.getResponse().getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER));
        }
    }

    @Nested
    @DisplayName("过滤器顺序测试")
    class FilterOrderTests {

        @Test
        @DisplayName("过滤器应该具有最高优先级")
        void shouldHaveHighestPrecedence() {
            assertEquals(Integer.MIN_VALUE, traceIdFilter.getOrder());
        }
    }

    @Nested
    @DisplayName("请求时间记录测试")
    class RequestTimeTests {

        @Test
        @DisplayName("应该记录请求开始时间")
        void shouldRecordRequestStartTime() {
            // Given
            MockServerHttpRequest request = MockServerHttpRequest
                    .get("/api/test")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            // When
            Mono<Void> result = traceIdFilter.filter(exchange, filterChain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            // 验证记录了请求开始时间
            assertNotNull(exchange.getAttributes().get(TraceIdFilter.REQUEST_START_TIME));
        }
    }
}
