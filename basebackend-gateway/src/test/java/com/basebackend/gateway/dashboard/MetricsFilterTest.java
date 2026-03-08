package com.basebackend.gateway.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsFilter 单元测试")
class MetricsFilterTest {

    @Mock
    private GatewayMetricsCollector metricsCollector;

    @Mock
    private GatewayFilterChain filterChain;

    @Mock
    private Route route;

    @Test
    @DisplayName("成功请求完成后应记录指标")
    void shouldRecordMetricsOnSuccess() {
        MetricsFilter metricsFilter = new MetricsFilter(metricsCollector);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users").build());
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
        when(route.getId()).thenReturn("route-user");

        when(filterChain.filter(any(ServerWebExchange.class)))
                .thenAnswer(invocation -> {
                    ServerWebExchange currentExchange = invocation.getArgument(0);
                    currentExchange.getResponse().setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                });

        StepVerifier.create(metricsFilter.filter(exchange, filterChain))
                .verifyComplete();

        ArgumentCaptor<Long> latencyCaptor = ArgumentCaptor.forClass(Long.class);
        verify(metricsCollector).record(eq("route-user"), eq(200), latencyCaptor.capture(), eq("/api/users"));
        assertThat(latencyCaptor.getValue()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("下游异常时也应记录指标")
    void shouldRecordMetricsOnError() {
        MetricsFilter metricsFilter = new MetricsFilter(metricsCollector);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders").build());
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);
        when(route.getId()).thenReturn("route-order");

        RuntimeException error = new RuntimeException("boom");
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.error(error));

        StepVerifier.create(metricsFilter.filter(exchange, filterChain))
                .expectErrorSatisfies(throwable -> assertThat(throwable).isSameAs(error))
                .verify();

        ArgumentCaptor<Long> latencyCaptor = ArgumentCaptor.forClass(Long.class);
        verify(metricsCollector).record(eq("route-order"), eq(0), latencyCaptor.capture(), eq("/api/orders"));
        assertThat(latencyCaptor.getValue()).isGreaterThanOrEqualTo(0L);
    }
}
