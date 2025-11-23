package com.basebackend.observability.otel.bridge;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Brave 到 OpenTelemetry 桥接器
 * <p>
 * 将 Brave 完成的 Span 镜像到 OpenTelemetry，实现追踪数据的双栈导出。
 * 保持原始的 TraceID/SpanID，确保追踪连续性。
 * </p>
 * <p>
 * 该桥接器作为 Brave 的 {@link SpanHandler} 实现，在 Span 完成时自动触发。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public class BraveToOtelBridge extends SpanHandler {

    private static final Logger log = LoggerFactory.getLogger(BraveToOtelBridge.class);

    private final Tracer tracer;

    /**
     * 创建桥接器实例
     *
     * @param tracer OpenTelemetry Tracer，用于创建镜像 Span
     */
    public BraveToOtelBridge(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 处理 Brave Span 完成事件
     * <p>
     * 该方法会被 Brave 在 Span 完成时调用，负责将 Span 数据镜像到 OpenTelemetry。
     * </p>
     *
     * @param context Brave 追踪上下文
     * @param span    完成的 Brave Span
     * @param cause   Span 完成的原因
     * @return 始终返回 true，允许其他 Handler 继续处理
     */
    @Override
    public boolean end(TraceContext context, MutableSpan span, Cause cause) {
        if (tracer == null) {
            return true;
        }

        try {
            // 构建 OpenTelemetry SpanContext
            SpanContext otelSpanContext = buildOtelSpanContext(context);
            Context parentContext = Context.root().with(Span.wrap(otelSpanContext));

            // 创建 OpenTelemetry Span
            SpanKind spanKind = mapKind(span.kind());
            long startNanos = TimeUnit.MICROSECONDS.toNanos(span.startTimestamp());

            Span otelSpan = tracer.spanBuilder(span.name())
                    .setParent(parentContext)
                    .setSpanKind(spanKind)
                    .setStartTimestamp(startNanos, TimeUnit.NANOSECONDS)
                    .startSpan();

            // 附加属性
            attachAttributes(otelSpan, span);

            // 处理错误状态
            handleStatus(otelSpan, span, cause);

            // 结束 Span
            long finishTimestamp = span.finishTimestamp();
            if (finishTimestamp > 0) {
                otelSpan.end(TimeUnit.MICROSECONDS.toNanos(finishTimestamp), TimeUnit.NANOSECONDS);
            } else {
                otelSpan.end();
            }

            log.debug("已镜像 Brave Span: {} (traceId={}, spanId={})",
                    span.name(), context.traceIdString(), context.spanIdString());

        } catch (Exception ex) {
            log.warn("镜像 Brave Span 失败: {}", span, ex);
        }

        return true;
    }

    /**
     * 构建 OpenTelemetry SpanContext
     */
    private SpanContext buildOtelSpanContext(TraceContext context) {
        TraceFlags flags = context.sampled()
                ? TraceFlags.getSampled()
                : TraceFlags.getDefault();

        if (context.shared()) {
            return SpanContext.createFromRemoteParent(
                    context.traceIdString(),
                    context.spanIdString(),
                    flags,
                    TraceState.getDefault()
            );
        } else {
            return SpanContext.create(
                    context.traceIdString(),
                    context.spanIdString(),
                    flags,
                    TraceState.getDefault()
            );
        }
    }

    /**
     * 附加 Brave Span 属性到 OpenTelemetry Span
     */
    private void attachAttributes(Span otelSpan, MutableSpan span) {
        AttributesBuilder builder = Attributes.builder();

        // 复制所有标签
        for (Map.Entry<String, String> entry : span.tags().entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }

        // 添加 Brave 特定属性（用于追溯）
        if (span.remoteServiceName() != null) {
            builder.put("brave.remoteService", span.remoteServiceName());
        }
        if (span.localServiceName() != null) {
            builder.put("brave.localService", span.localServiceName());
        }
        builder.put("brave.traceId", span.traceId());
        builder.put("brave.spanId", span.id());
        if (span.parentId() != null) {
            builder.put("brave.parentId", span.parentId());
        }

        // 网络相关属性
        if (span.remoteIp() != null) {
            builder.put("net.peer.ip", span.remoteIp());
        }
        if ( span.remotePort() > 0) {
            builder.put("net.peer.port", (long) span.remotePort());
        }

        otelSpan.setAllAttributes(builder.build());
    }

    /**
     * 处理 Span 状态（成功/错误）
     * <p>
     * 根据 Span 的结束原因和异常信息设置 OpenTelemetry Span 状态：
     * <ul>
     *     <li>有明确异常：ERROR + 记录异常</li>
     *     <li>Cause 为 null：OK（正常完成）</li>
     *     <li>任何非 null 的 Cause（ABANDONED, DROPPED, ORPHANED, CANCELED 等）：ERROR</li>
     * </ul>
     * </p>
     */
    private void handleStatus(Span otelSpan, MutableSpan span, Cause cause) {
        if (span.error() != null) {
            // 有明确的异常
            otelSpan.recordException(span.error());
            otelSpan.setStatus(StatusCode.ERROR, span.error().getMessage());
        } else if (cause != null) {
            // 任何非正常的 Cause 都标记为错误
            // 包括：ABANDONED（被放弃）、DROPPED（被丢弃）、ORPHANED（孤儿）、CANCELED（被取消）等
            otelSpan.setStatus(StatusCode.ERROR, "Span ended abnormally with cause: " + cause);
            log.debug("Span 异常结束: name={}, cause={}", span.name(), cause);
        } else {
            // Cause 为 null，表示正常完成
            otelSpan.setStatus(StatusCode.OK);
        }
    }

    /**
     * 映射 Brave SpanKind 到 OpenTelemetry SpanKind
     */
    private SpanKind mapKind(brave.Span.Kind braveKind) {
        if (braveKind == null) {
            return SpanKind.INTERNAL;
        }

        return switch (braveKind) {
            case CLIENT -> SpanKind.CLIENT;
            case SERVER -> SpanKind.SERVER;
            case PRODUCER -> SpanKind.PRODUCER;
            case CONSUMER -> SpanKind.CONSUMER;
            default -> SpanKind.INTERNAL;
        };
    }
}
