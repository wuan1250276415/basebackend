package com.basebackend.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 链路追踪过滤器
 * <p>
 * 为每个请求生成唯一的追踪 ID，并传递到下游服务。
 * 支持从上游请求中继承追踪 ID（用于分布式追踪场景）。
 * </p>
 *
 * <h3>追踪头说明：</h3>
 * <ul>
 * <li><b>X-Trace-Id</b>: 全局追踪 ID，贯穿整个请求链路</li>
 * <li><b>X-Span-Id</b>: 当前跨度 ID</li>
 * <li><b>X-Parent-Span-Id</b>: 父跨度 ID</li>
 * <li><b>X-Request-Id</b>: 请求 ID（兼容旧系统）</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraceIdFilter implements GlobalFilter, Ordered {

    /**
     * 追踪 ID 头名称
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 跨度 ID 头名称
     */
    public static final String SPAN_ID_HEADER = "X-Span-Id";

    /**
     * 父跨度 ID 头名称
     */
    public static final String PARENT_SPAN_ID_HEADER = "X-Parent-Span-Id";

    /**
     * 请求 ID 头名称（兼容）
     */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * 请求开始时间属性名
     */
    public static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        // 保存请求开始时间
        exchange.getAttributes().put(REQUEST_START_TIME, startTime);

        // 获取或生成追踪 ID
        String traceId = getOrGenerateTraceId(request);

        // 生成当前跨度 ID
        String spanId = generateShortId();

        // 获取父跨度 ID
        String parentSpanId = request.getHeaders().getFirst(SPAN_ID_HEADER);

        // 构建新请求，添加追踪头
        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .header(TRACE_ID_HEADER, traceId)
                .header(SPAN_ID_HEADER, spanId)
                .header(REQUEST_ID_HEADER, traceId); // 兼容旧系统

        if (parentSpanId != null && !parentSpanId.isEmpty()) {
            requestBuilder.header(PARENT_SPAN_ID_HEADER, parentSpanId);
        }

        ServerHttpRequest mutatedRequest = requestBuilder.build();
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // 添加追踪 ID 到响应头
        mutatedExchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

        if (log.isDebugEnabled()) {
            log.debug("[{}] 请求开始 - {} {}", traceId,
                    request.getMethod(), request.getPath());
        }

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = mutatedExchange.getResponse().getStatusCode() != null
                            ? mutatedExchange.getResponse().getStatusCode().value()
                            : 0;

                    // 根据耗时选择日志级别
                    if (duration > 3000) {
                        log.warn("[{}] 请求完成(慢) - {} {} - {}ms - {}",
                                traceId, request.getMethod(), request.getPath(),
                                duration, statusCode);
                    } else if (log.isDebugEnabled()) {
                        log.debug("[{}] 请求完成 - {} {} - {}ms - {}",
                                traceId, request.getMethod(), request.getPath(),
                                duration, statusCode);
                    }
                });
    }

    /**
     * 获取或生成追踪 ID
     * 优先使用上游传递的追踪 ID
     */
    private String getOrGenerateTraceId(ServerHttpRequest request) {
        // 优先检查 X-Trace-Id
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // 兼容 X-Request-Id
        traceId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // 生成新的追踪 ID
        return generateTraceId();
    }

    /**
     * 生成追踪 ID（32位小写十六进制）
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成短 ID（16位，用于 Span ID）
     */
    private String generateShortId() {
        return Long.toHexString(System.nanoTime()) +
                Long.toHexString((long) (Math.random() * 0xFFFFFFL));
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保追踪 ID 在其他过滤器之前设置
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
