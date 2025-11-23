package com.basebackend.observability.logging.format;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Set;

/**
 * 日志属性填充器
 * <p>
 * 自动填充追踪上下文（traceId、spanId）和业务上下文（租户ID、用户ID等）到 MDC。
 * </p>
 * <p>
 * <b>功能：</b>
 * <ul>
 *     <li>从 OpenTelemetry 当前 Span 提取 traceId 和 spanId</li>
 *     <li>从 HTTP header 或 Baggage 提取业务上下文</li>
 *     <li>自动填充到 SLF4J MDC，供日志输出使用</li>
 * </ul>
 * </p>
 * <p>
 * <b>使用方式：</b>
 * <pre>{@code
 * @Component
 * public class MyFilter implements Filter {
 *     @Autowired
 *     private LogAttributeEnricher enricher;
 *
 *     public void doFilter(...) {
 *         enricher.enrichFromCurrentSpan();  // 填充追踪上下文
 *         enricher.enrichBusinessContext(request);  // 填充业务上下文
 *         try {
 *             chain.doFilter(request, response);
 *         } finally {
 *             enricher.clearAll();  // 清理 MDC
 *         }
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>注意：</b>此类通过 LoggingAutoConfiguration 注册为 Bean，不需要 @Component 注解。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class LogAttributeEnricher {

    /**
     * MDC key: traceId
     */
    public static final String TRACE_ID = "traceId";

    /**
     * MDC key: spanId
     */
    public static final String SPAN_ID = "spanId";

    /**
     * MDC key: 租户ID
     */
    public static final String TENANT_ID = "tenantId";

    /**
     * MDC key: 用户ID
     */
    public static final String USER_ID = "userId";

    /**
     * MDC key: 请求ID
     */
    public static final String REQUEST_ID = "requestId";

    /**
     * MDC key: 渠道ID
     */
    public static final String CHANNEL_ID = "channelId";

    /**
     * 本类管理的 MDC 键集合
     */
    private static final Set<String> MANAGED_KEYS = Set.of(
            TRACE_ID, SPAN_ID, TENANT_ID, USER_ID, REQUEST_ID, CHANNEL_ID
    );

    /**
     * 从当前 OpenTelemetry Span 填充追踪上下文
     * <p>
     * 自动提取 traceId 和 spanId 并填充到 MDC。
     * 如果当前没有有效的 Span，则清理追踪相关的 MDC 键。
     * </p>
     */
    public void enrichFromCurrentSpan() {
        try {
            Span currentSpan = Span.current();
            if (currentSpan != null) {
                SpanContext spanContext = currentSpan.getSpanContext();
                if (spanContext != null && spanContext.isValid()) {
                    MDC.put(TRACE_ID, spanContext.getTraceId());
                    MDC.put(SPAN_ID, spanContext.getSpanId());
                    return;
                }
            }

            // 无有效 Span 时清理残留的追踪信息
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
        } catch (Exception e) {
            // 静默失败，不影响业务逻辑
        }
    }

    /**
     * 填充业务上下文
     * <p>
     * 从 Map 中提取业务字段（租户ID、用户ID等）并填充到 MDC。
     * </p>
     *
     * @param context 业务上下文 Map
     */
    public void enrichBusinessContext(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            return;
        }

        putIfPresent(context, "X-Tenant-Id", TENANT_ID);
        putIfPresent(context, "X-User-Id", USER_ID);
        putIfPresent(context, "X-Request-Id", REQUEST_ID);
        putIfPresent(context, "X-Channel-Id", CHANNEL_ID);
    }

    /**
     * 填充单个属性到 MDC
     *
     * @param key   MDC key
     * @param value MDC value
     */
    public void put(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * 清理所有 MDC 上下文
     * <p>
     * <b>重要：</b>在请求结束时必须调用，避免 MDC 泄漏到线程池中的其他请求。
     * </p>
     */
    public void clearAll() {
        MDC.clear();
    }

    /**
     * 清理指定 MDC key
     *
     * @param key MDC key
     */
    public void remove(String key) {
        if (key != null) {
            MDC.remove(key);
        }
    }

    /**
     * 从 context 中提取指定 key 的值，如果存在则填充到 MDC
     *
     * @param context    上下文 Map
     * @param sourceKey  源 key（如 HTTP header 名）
     * @param targetKey  目标 MDC key
     */
    private void putIfPresent(Map<String, String> context, String sourceKey, String targetKey) {
        String value = context.get(sourceKey);
        if (value != null && !value.isEmpty()) {
            MDC.put(targetKey, value);
        }
    }
}
