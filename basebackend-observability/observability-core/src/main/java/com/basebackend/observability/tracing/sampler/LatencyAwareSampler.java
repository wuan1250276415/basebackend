package com.basebackend.observability.tracing.sampler;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;

/**
 * 延迟感知采样器（Latency-Aware Sampler）
 * <p>
 * 检测慢请求并强制100%采样，确保性能问题被追踪。
 * 符合 OpenTelemetry head-sampling 规范，仅依赖创建 Span 时可获得的属性。
 * </p>
 * <p>
 * <b>重要限制（Head-Sampling）：</b>
 * <ul>
 *     <li>采样决策在 Span 创建前完成，此时请求尚未处理完成</li>
 *     <li>必须在创建 Span 前将 {@code observability.latency_ms} 或预估延迟设置到属性中</li>
 *     <li>如果无法预先得知延迟，此采样器无法生效</li>
 *     <li>要100%捕获慢请求，需配合 Tail-Sampling（Collector 或自定义 SpanProcessor）</li>
 * </ul>
 * </p>
 * <p>
 * 延迟判定规则：
 * <ul>
 *     <li>{@code observability.latency_ms >= threshold} - 实际或预估延迟</li>
 *     <li>{@code observability.force_sample_slow=true} - 强制慢请求采样标记</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 在创建 Span 前设置延迟提示
 * Span span = tracer.spanBuilder("HTTP POST /api/heavy-computation")
 *     .setAttribute("observability.latency_ms", 1500L)  // 预估或实测延迟
 *     .setAttribute("observability.force_sample_slow", true)  // 或使用强制标记
 *     .startSpan();
 * }</pre>
 * </p>
 * <p>
 * <b>实际应用场景：</b>
 * <ul>
 *     <li><b>预估延迟</b>：某些复杂操作可预估耗时（如大数据查询、批量处理）</li>
 *     <li><b>重复请求</b>：对于幂等请求，可使用上次延迟作为提示</li>
 *     <li><b>队列深度</b>：根据队列长度预估延迟</li>
 *     <li><b>Tail-Sampling 补充</b>：配合后端 Collector 的 tail-sampling 实现完整慢请求追踪</li>
 * </ul>
 * </p>
 * <p>
 * 装饰器模式：此采样器包装一个委托采样器，慢请求时强制采样，否则委托给下一层。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/otel/trace/sdk/#sampler">OpenTelemetry Sampler Specification</a>
 */
public final class LatencyAwareSampler implements Sampler {

    // 自定义属性键：延迟提示（毫秒）
    private static final AttributeKey<Long> LATENCY_MS = AttributeKey.longKey("observability.latency_ms");
    // 自定义属性键：强制慢请求采样标记
    private static final AttributeKey<Boolean> FORCE_SLOW = AttributeKey.booleanKey("observability.force_sample_slow");

    private final long thresholdMs;
    private final boolean alwaysSampleSlowRequests;
    private final Sampler delegate;

    /**
     * 构造函数
     *
     * @param thresholdMs               延迟阈值（毫秒）
     * @param alwaysSampleSlowRequests  是否总是采样慢请求
     * @param delegate                  委托采样器
     */
    public LatencyAwareSampler(long thresholdMs, boolean alwaysSampleSlowRequests, Sampler delegate) {
        if (thresholdMs <= 0) {
            throw new IllegalArgumentException("thresholdMs must be positive, got: " + thresholdMs);
        }
        this.thresholdMs = thresholdMs;
        this.alwaysSampleSlowRequests = alwaysSampleSlowRequests;
        this.delegate = delegate;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
                                       Attributes attributes, List<LinkData> parentLinks) {
        // 尊重父级采样决策：与 ParentBasedSampler 等效
        SpanContext parent = Span.fromContext(parentContext).getSpanContext();
        if (parent.isValid()) {
            // 已采样父级强制采样，未采样父级直接丢弃（保持 trace 一致性）
            return parent.isSampled() ? SamplingResult.recordAndSample() : SamplingResult.drop();
        }

        // 如果启用慢请求采样且检测到慢请求，强制采样
        if (alwaysSampleSlowRequests && isSlowRequest(attributes)) {
            return SamplingResult.recordAndSample();
        }
        // 否则委托给下一层采样器
        return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "LatencyAwareSampler{thresholdMs=" + thresholdMs +
                ", alwaysSampleSlowRequests=" + alwaysSampleSlowRequests +
                ", delegate=" + delegate.getDescription() + "}";
    }

    /**
     * 判断是否为慢请求
     * <p>
     * 检测逻辑：
     * <ol>
     *     <li>检查强制慢请求采样标记</li>
     *     <li>检查延迟提示是否 >= 阈值</li>
     * </ol>
     * </p>
     * <p>
     * <b>注意：</b>此方法在 Span 创建前调用，只能使用预先设置的属性。
     * 如果无法预先得知延迟，建议使用 Tail-Sampling 或自定义 SpanProcessor。
     * </p>
     *
     * @param attributes Span 属性
     * @return true 如果是慢请求，false 否则
     */
    private boolean isSlowRequest(Attributes attributes) {
        // 检查强制慢请求标记
        Boolean force = attributes.get(FORCE_SLOW);
        if (Boolean.TRUE.equals(force)) {
            return true;
        }
        // 检查延迟提示
        Long latency = attributes.get(LATENCY_MS);
        return latency != null && latency >= thresholdMs;
    }
}
