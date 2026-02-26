package com.basebackend.observability.tracing.context;

import com.basebackend.observability.tracing.config.TracingProperties;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 追踪传播器配置
 * <p>
 * 构建组合的 {@link TextMapPropagator} 链，用于在分布式系统中传播追踪上下文。
 * </p>
 * <p>
 * 传播器链包括：
 * <ol>
 *     <li>{@link W3CTraceContextPropagator} - W3C TraceContext 标准传播</li>
 *     <li>{@link W3CBaggagePropagator} - W3C Baggage 标准传播</li>
 *     <li>{@link BusinessContextPropagator} - 业务上下文传播</li>
 * </ol>
 * </p>
 * <p>
 * 传播的 HTTP Header：
 * <ul>
 *     <li>traceparent - 包含 trace-id, span-id, trace-flags</li>
 *     <li>tracestate - 包含供应商特定的追踪状态</li>
 *     <li>baggage - 包含跨服务的键值对</li>
 *     <li>X-Tenant-Id, X-Channel-Id 等 - 业务上下文字段（可配置）</li>
 * </ul>
 * </p>
 * <p>
 * <b>条件加载：</b>仅在 {@code observability.tracing.enabled=true} 且
 * {@code observability.tracing.propagation.enabled=true} 时生效。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 * @see <a href="https://www.w3.org/TR/baggage/">W3C Baggage</a>
 */
@Configuration
@ConditionalOnProperty(prefix = "observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = false)
public class TracePropagatorConfiguration {

    /**
     * 创建业务上下文传播器
     * <p>
     * 根据配置的白名单创建传播器，只有白名单中的 header 字段才会被传播。
     * </p>
     * <p>
     * 条件加载：仅在 {@code observability.tracing.propagation.enabled=true} 时创建。
     * </p>
     *
     * @param tracingProperties 追踪配置属性
     * @return BusinessContextPropagator 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "observability.tracing.propagation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public BusinessContextPropagator businessContextPropagator(TracingProperties tracingProperties) {
        List<String> keys = tracingProperties.getPropagation().getBusinessKeys();
        return new BusinessContextPropagator(keys);
    }

    /**
     * 创建组合的文本映射传播器
     * <p>
     * 将多个传播器组合成一个链，按顺序执行注入和提取操作。
     * </p>
     * <p>
     * 注入顺序：
     * <ol>
     *     <li>W3C TraceContext（traceparent, tracestate）</li>
     *     <li>W3C Baggage（baggage）</li>
     *     <li>Business Context（X-Tenant-Id, X-Channel-Id 等）- 如果启用</li>
     * </ol>
     * </p>
     * <p>
     * 提取顺序相同，但每个传播器都会在前一个传播器的基础上增量更新 Context。
     * </p>
     *
     * @param businessContextPropagator 业务上下文传播器（可选）
     * @return 组合的 TextMapPropagator
     */
    @Bean
    public TextMapPropagator textMapPropagator(
            @org.springframework.beans.factory.annotation.Autowired(required = false) BusinessContextPropagator businessContextPropagator) {

        // 如果业务上下文传播器未启用，只使用 W3C 标准传播器
        if (businessContextPropagator == null) {
            return TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance()
            );
        }

        // 否则，使用完整的传播器链
        return TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance(),
                businessContextPropagator
        );
    }

    /**
     * 创建上下文传播器包装器
     * <p>
     * OpenTelemetry SDK 使用 {@link ContextPropagators} 来访问传播器。
     * </p>
     * <p>
     * <b>重要：</b>此 Bean 需要在 TracingAutoConfiguration 中被注入到 OpenTelemetrySdk，
     * 否则传播器不会生效。
     * </p>
     *
     * @param textMapPropagator 文本映射传播器
     * @return ContextPropagators 实例
     */
    @Bean
    public ContextPropagators contextPropagators(TextMapPropagator textMapPropagator) {
        return ContextPropagators.create(textMapPropagator);
    }
}
