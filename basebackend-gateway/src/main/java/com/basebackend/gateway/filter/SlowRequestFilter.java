package com.basebackend.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 慢请求追踪过滤器
 * <p>
 * 监控和记录慢请求，提供性能分析数据。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 * <li>记录请求耗时到 Micrometer</li>
 * <li>识别和记录慢请求（可配置阈值）</li>
 * <li>按路由/路径分组统计</li>
 * <li>慢请求详细日志</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class SlowRequestFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;
    private final Timer requestTimer;

    /**
     * 慢请求阈值（毫秒）
     */
    @Value("${gateway.monitor.slow-request-threshold:1000}")
    private long slowRequestThreshold;

    /**
     * 超慢请求阈值（毫秒）
     */
    @Value("${gateway.monitor.very-slow-request-threshold:3000}")
    private long verySlowRequestThreshold;

    /**
     * 是否启用慢请求详细日志
     */
    @Value("${gateway.monitor.slow-request-log-enabled:true}")
    private boolean slowRequestLogEnabled;

    /**
     * 慢请求计数
     */
    private final AtomicLong slowRequestCount = new AtomicLong(0);

    /**
     * 超慢请求计数
     */
    private final AtomicLong verySlowRequestCount = new AtomicLong(0);

    public SlowRequestFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 创建请求计时器
        this.requestTimer = Timer.builder("gateway.request.duration")
                .description("网关请求耗时")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        // 注册慢请求计数 gauge
        meterRegistry.gauge("gateway.request.slow.count", slowRequestCount);
        meterRegistry.gauge("gateway.request.very_slow.count", verySlowRequestCount);

        log.info("慢请求追踪过滤器初始化完成 - 阈值: {}ms, 超慢阈值: {}ms",
                slowRequestThreshold, verySlowRequestThreshold);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getPath().toString();
        String traceId = request.getHeaders().getFirst(TraceIdFilter.TRACE_ID_HEADER);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long durationNanos = System.nanoTime() - startTime;
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

                    // 记录到 Micrometer
                    recordMetrics(method, path, durationNanos, exchange);

                    // 检查是否为慢请求
                    if (slowRequestLogEnabled) {
                        checkSlowRequest(traceId, method, path, durationMs, exchange);
                    }
                });
    }

    /**
     * 记录请求指标
     */
    private void recordMetrics(String method, String path, long durationNanos,
            ServerWebExchange exchange) {
        int statusCode = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value()
                : 0;

        // 简化路径，避免高基数问题
        String normalizedPath = normalizePath(path);

        requestTimer.record(durationNanos, TimeUnit.NANOSECONDS);

        // 按路径和方法分组的计时器
        Timer.builder("gateway.request.duration.by_path")
                .tag("method", method)
                .tag("path", normalizedPath)
                .tag("status", String.valueOf(statusCode / 100) + "xx")
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 检查并记录慢请求
     */
    private void checkSlowRequest(String traceId, String method, String path,
            long durationMs, ServerWebExchange exchange) {
        int statusCode = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value()
                : 0;

        if (durationMs >= verySlowRequestThreshold) {
            // 超慢请求
            verySlowRequestCount.incrementAndGet();
            log.error("[VERY_SLOW] [{}] {} {} - {}ms - status={} - 超慢请求警告！",
                    traceId != null ? traceId : "-",
                    method, path, durationMs, statusCode);

            // 记录更多诊断信息
            logSlowRequestDetails(exchange, durationMs, "VERY_SLOW");

        } else if (durationMs >= slowRequestThreshold) {
            // 慢请求
            slowRequestCount.incrementAndGet();
            log.warn("[SLOW] [{}] {} {} - {}ms - status={}",
                    traceId != null ? traceId : "-",
                    method, path, durationMs, statusCode);

            logSlowRequestDetails(exchange, durationMs, "SLOW");
        }
    }

    /**
     * 记录慢请求详细信息
     */
    private void logSlowRequestDetails(ServerWebExchange exchange, long durationMs, String level) {
        ServerHttpRequest request = exchange.getRequest();

        // 获取客户端 IP
        String clientIp = request.getHeaders().getFirst("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.getRemoteAddress() != null
                    ? request.getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        }

        // 获取用户 ID
        String userId = request.getHeaders().getFirst("X-User-Id");

        // 获取 User-Agent
        String userAgent = request.getHeaders().getFirst("User-Agent");

        log.debug("[{}] 慢请求详情 - clientIp={}, userId={}, headers={}, queryParams={}",
                level,
                clientIp,
                userId != null ? userId : "-",
                userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "-",
                request.getQueryParams());
    }

    /**
     * 规范化路径（降低指标基数）
     * 将路径参数替换为占位符
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // 替换常见的 ID 模式
        // /api/users/12345 -> /api/users/{id}
        // /api/orders/abc-123 -> /api/orders/{id}
        String normalized = path;

        // 替换纯数字 ID
        normalized = normalized.replaceAll("/\\d+(?=/|$)", "/{id}");

        // 替换 UUID 格式的 ID
        normalized = normalized.replaceAll(
                "/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}(?=/|$)",
                "/{uuid}");

        // 限制路径长度
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100) + "...";
        }

        return normalized;
    }

    /**
     * 获取慢请求统计
     */
    public long getSlowRequestCount() {
        return slowRequestCount.get();
    }

    /**
     * 获取超慢请求统计
     */
    public long getVerySlowRequestCount() {
        return verySlowRequestCount.get();
    }

    /**
     * 重置统计计数
     */
    public void resetCounters() {
        slowRequestCount.set(0);
        verySlowRequestCount.set(0);
        log.info("慢请求计数已重置");
    }

    @Override
    public int getOrder() {
        // 优先级高于链路追踪，确保能记录完整耗时
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
