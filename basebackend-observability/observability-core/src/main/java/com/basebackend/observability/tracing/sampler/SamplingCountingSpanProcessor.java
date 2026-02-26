package com.basebackend.observability.tracing.sampler;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 采样计数 SpanProcessor
 * <p>
 * 监听根 Span 的采样事件，并通知 {@link DynamicSamplingManager} 进行计数。
 * 用于支持动态采样率调整功能。
 * </p>
 * <p>
 * 核心功能：
 * <ul>
 *     <li><b>根 Span 识别</b>：只计数根 Span（无有效父级的 Span）</li>
 *     <li><b>采样状态检查</b>：只计数被采样的 Span</li>
 *     <li><b>轻量级处理</b>：onStart 回调中执行简单计数，不影响性能</li>
 *     <li><b>线程安全</b>：DynamicSamplingManager 内部使用 AtomicLong 计数</li>
 * </ul>
 * </p>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *     <li>此 SpanProcessor 应在 BatchSpanProcessor 之前注册</li>
 *     <li>只在启用动态采样时注册此处理器</li>
 *     <li>不会阻塞 Span 创建，性能开销可忽略</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class SamplingCountingSpanProcessor implements SpanProcessor {

    private static final Logger log = LoggerFactory.getLogger(SamplingCountingSpanProcessor.class);

    private final DynamicSamplingManager dynamicSamplingManager;

    /**
     * 构造函数
     *
     * @param dynamicSamplingManager 动态采样管理器
     */
    public SamplingCountingSpanProcessor(DynamicSamplingManager dynamicSamplingManager) {
        if (dynamicSamplingManager == null) {
            throw new IllegalArgumentException("dynamicSamplingManager cannot be null");
        }
        this.dynamicSamplingManager = dynamicSamplingManager;
        log.info("采样计数 SpanProcessor 已初始化");
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        try {
            // 只计数被采样的根 Span
            if (isRootSpan(span) && span.getSpanContext().isSampled()) {
                dynamicSamplingManager.recordSpan();
                log.trace("记录根 Span 采样: spanId={}", span.getSpanContext().getSpanId());
            }
        } catch (Exception ex) {
            // 异常安全：SpanProcessor 失败不应影响业务逻辑
            log.debug("记录 Span 采样失败", ex);
        }
    }

    @Override
    public boolean isStartRequired() {
        // 需要在 onStart 中计数
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // 不需要在 onEnd 中处理
    }

    @Override
    public boolean isEndRequired() {
        // 不需要在 onEnd 中处理
        return false;
    }

    @Override
    public CompletableResultCode shutdown() {
        log.info("采样计数 SpanProcessor 已关闭");
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode forceFlush() {
        // 无需刷新（无缓冲）
        return CompletableResultCode.ofSuccess();
    }

    /**
     * 判断是否为根 Span
     * <p>
     * 根 Span 的定义：父级 SpanContext 无效。
     * </p>
     *
     * @param span 当前 Span
     * @return true 如果是根 Span，false 否则
     */
    private boolean isRootSpan(ReadableSpan span) {
        SpanContext parent = span.getParentSpanContext();
        return !parent.isValid();
    }
}
