package com.basebackend.observability.logging.format;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Set;

/**
 * 日志属性填充器
 * <p>
 * 自动填充追踪上下文（traceId、spanId）和业务上下文（租户ID、用户ID等）到 MDC。
 * 通过 Micrometer Tracing 的 {@link Tracer} 获取当前 Span，替代直接依赖 OTel API。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class LogAttributeEnricher {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String REQUEST_ID = "requestId";
    public static final String CHANNEL_ID = "channelId";

    private static final Set<String> MANAGED_KEYS = Set.of(
            TRACE_ID, SPAN_ID, TENANT_ID, USER_ID, REQUEST_ID, CHANNEL_ID
    );

    private final Tracer tracer;

    public LogAttributeEnricher(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 从当前 Micrometer Tracing Span 填充追踪上下文到 MDC
     */
    public void enrichFromCurrentSpan() {
        try {
            Span currentSpan = tracer != null ? tracer.currentSpan() : null;
            if (currentSpan != null) {
                var context = currentSpan.context();
                if (context != null) {
                    MDC.put(TRACE_ID, context.traceId());
                    MDC.put(SPAN_ID, context.spanId());
                    return;
                }
            }

            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
        } catch (Exception e) {
            // 静默失败，不影响业务逻辑
        }
    }

    /**
     * 填充业务上下文
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

    public void put(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    public void clearAll() {
        MDC.clear();
    }

    public void remove(String key) {
        if (key != null) {
            MDC.remove(key);
        }
    }

    private void putIfPresent(Map<String, String> context, String sourceKey, String targetKey) {
        String value = context.get(sourceKey);
        if (value != null && !value.isEmpty()) {
            MDC.put(targetKey, value);
        }
    }
}
