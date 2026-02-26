package com.basebackend.gateway.dashboard;

import com.basebackend.gateway.util.IpAddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 流量统计全局过滤器
 * <p>
 * 在请求完成后自动记录路由、状态码、耗时等指标到 {@link GatewayMetricsCollector}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "gateway.metrics.startTime";

    private final GatewayMetricsCollector metricsCollector;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> recordMetrics(exchange)));
    }

    private void recordMetrics(ServerWebExchange exchange) {
        Long startTime = exchange.getAttribute(START_TIME_ATTR);
        long latencyMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";

        ServerHttpResponse response = exchange.getResponse();
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

        String path = exchange.getRequest().getURI().getPath();

        metricsCollector.record(routeId, statusCode, latencyMs, path);
    }

    @Override
    public int getOrder() {
        // 在最外层，最早开始计时，最晚记录结果
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
