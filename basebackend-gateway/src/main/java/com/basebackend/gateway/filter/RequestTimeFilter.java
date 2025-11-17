package com.basebackend.gateway.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Gateway 请求响应时间监控过滤器
 *
 * 功能：
 * 1. 记录每个请求的响应时间
 * 2. 统计慢请求（超过阈值）
 * 3. 集成 Prometheus 监控
 * 4. 添加响应头（X-Response-Time）
 *
 * @author 浮浮酱
 */
@Slf4j
@Component
public class RequestTimeFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_START_TIME = "requestStartTime";
    private static final long SLOW_REQUEST_THRESHOLD = 3000; // 慢请求阈值：3 秒

    private final MeterRegistry meterRegistry;
    private final Timer requestTimer;
    private final Counter slowRequestCounter;
    private final Counter totalRequestCounter;

    public RequestTimeFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化 Prometheus 指标
        this.requestTimer = Timer.builder("gateway.request.duration")
                .description("Gateway 请求响应时间")
                .tag("component", "gateway")
                .register(meterRegistry);

        this.slowRequestCounter = Counter.builder("gateway.slow.request.count")
                .description("Gateway 慢请求总数")
                .tag("component", "gateway")
                .register(meterRegistry);

        this.totalRequestCounter = Counter.builder("gateway.request.total")
                .description("Gateway 请求总数")
                .tag("component", "gateway")
                .register(meterRegistry);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 记录请求开始时间
        exchange.getAttributes().put(REQUEST_START_TIME, System.currentTimeMillis());

        // 增加总请求计数
        totalRequestCounter.increment();

        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    Long startTime = exchange.getAttribute(REQUEST_START_TIME);
                    if (startTime != null) {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;

                        // 记录到 Prometheus
                        requestTimer.record(duration, TimeUnit.MILLISECONDS);

                        // 检查是否为慢请求
                        if (duration >= SLOW_REQUEST_THRESHOLD) {
                            slowRequestCounter.increment();
                            logSlowRequest(exchange, duration);
                        }

                        // 添加响应头：响应时间
                        ServerHttpResponse response = exchange.getResponse();
                        response.getHeaders().add("X-Response-Time", duration + "ms");

                        // 记录请求日志（DEBUG 级别）
                        if (log.isDebugEnabled()) {
                            logRequestInfo(exchange, duration);
                        }
                    }
                })
        );
    }

    /**
     * 记录慢请求日志
     */
    private void logSlowRequest(ServerWebExchange exchange, long duration) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        log.warn("检测到慢请求 - URI: {}, 方法: {}, 响应时间: {}ms, 状态码: {}",
                request.getURI(),
                request.getMethod(),
                duration,
                response.getStatusCode()
        );
    }

    /**
     * 记录请求详细信息
     */
    private void logRequestInfo(ServerWebExchange exchange, long duration) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        log.debug("请求完成 - URI: {}, 方法: {}, 响应时间: {}ms, 状态码: {}, 客户端: {}",
                request.getURI(),
                request.getMethod(),
                duration,
                response.getStatusCode(),
                request.getRemoteAddress()
        );
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保最先执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
