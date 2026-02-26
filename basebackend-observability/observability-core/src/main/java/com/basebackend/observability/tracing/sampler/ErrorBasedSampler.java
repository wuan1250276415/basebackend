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
 * 错误感知采样器（Error-Aware Sampler）
 * <p>
 * 检测错误请求并强制100%采样，确保所有错误都被追踪。
 * 符合 OpenTelemetry head-sampling 规范，仅依赖创建 Span 时可获得的属性。
 * </p>
 * <p>
 * <b>重要限制（Head-Sampling）：</b>
 * <ul>
 *     <li>采样决策在 Span 创建前完成，此时 HTTP 响应尚未产生</li>
 *     <li>必须在创建 Span 前将 {@code http.status_code} 或 {@code observability.force_sample_error} 设置到属性中</li>
 *     <li>如果无法预先得知错误状态，此采样器无法生效</li>
 *     <li>要100%捕获错误，需配合 Tail-Sampling（Collector 或自定义 SpanProcessor）</li>
 * </ul>
 * </p>
 * <p>
 * 错误判定规则：
 * <ul>
 *     <li>{@code observability.force_sample_error=true} - 强制错误采样标记</li>
 *     <li>{@code http.status_code >= 400} - HTTP 4xx/5xx 错误状态码</li>
 * </ul>
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 在创建 Span 前设置错误提示
 * Span span = tracer.spanBuilder("HTTP POST /api/users")
 *     .setAttribute("http.status_code", 500L)  // 如果可以预判
 *     .setAttribute("observability.force_sample_error", true)  // 或使用强制标记
 *     .startSpan();
 * }</pre>
 * </p>
 * <p>
 * 装饰器模式：此采样器包装一个委托采样器，错误时强制采样，否则委托给下一层。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/otel/trace/sdk/#sampler">OpenTelemetry Sampler Specification</a>
 */
public final class ErrorBasedSampler implements Sampler {

    // OpenTelemetry 语义约定属性键
    private static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("http.status_code");
    // 自定义属性键：强制错误采样标记
    private static final AttributeKey<Boolean> FORCE_ERROR = AttributeKey.booleanKey("observability.force_sample_error");

    private final boolean alwaysSampleErrors;
    private final Sampler delegate;

    /**
     * 构造函数
     *
     * @param alwaysSampleErrors 是否总是采样错误请求
     * @param delegate           委托采样器
     */
    public ErrorBasedSampler(boolean alwaysSampleErrors, Sampler delegate) {
        this.alwaysSampleErrors = alwaysSampleErrors;
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

        // 如果启用错误采样且检测到错误，强制采样
        if (alwaysSampleErrors && isError(attributes)) {
            return SamplingResult.recordAndSample();
        }
        // 否则委托给下一层采样器
        return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "ErrorBasedSampler{alwaysSampleErrors=" + alwaysSampleErrors + ", delegate=" + delegate.getDescription() + "}";
    }

    /**
     * 判断是否为错误请求
     * <p>
     * 检测逻辑：
     * <ol>
     *     <li>检查强制错误采样标记</li>
     *     <li>检查 HTTP 状态码是否 >= 400</li>
     * </ol>
     * </p>
     *
     * @param attributes Span 属性
     * @return true 如果是错误请求，false 否则
     */
    private boolean isError(Attributes attributes) {
        // 检查强制错误标记
        Boolean force = attributes.get(FORCE_ERROR);
        if (Boolean.TRUE.equals(force)) {
            return true;
        }
        // 检查 HTTP 状态码
        Long status = attributes.get(HTTP_STATUS);
        return status != null && status >= 400;
    }
}
