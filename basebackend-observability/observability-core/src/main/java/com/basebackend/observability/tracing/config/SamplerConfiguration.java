package com.basebackend.observability.tracing.config;

import com.basebackend.observability.tracing.sampler.DynamicSamplingManager;
import com.basebackend.observability.tracing.sampler.ErrorBasedSampler;
import com.basebackend.observability.tracing.sampler.LatencyAwareSampler;
import com.basebackend.observability.tracing.sampler.RuleBasedSampler;
import com.basebackend.observability.tracing.sampler.SamplingCountingSpanProcessor;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.function.Supplier;

/**
 * 采样器自动配置
 * <p>
 * 创建采样器链，按以下顺序组装：
 * <pre>
 * ErrorBasedSampler (如果启用)
 *   └─> LatencyAwareSampler (如果启用)
 *       └─> RuleBasedSampler (如果有规则)
 *           └─> DynamicSampler (如果启用动态采样)
 *               └─> TraceIdRatioBased (默认固定采样率)
 * </pre>
 * </p>
 * <p>
 * 采样策略执行顺序（装饰器链）：
 * <ol>
 *     <li><b>错误采样</b>：检测到错误（HTTP 4xx/5xx）时强制100%采样</li>
 *     <li><b>慢请求采样</b>：检测到慢请求（延迟 >= 阈值）时强制100%采样</li>
 *     <li><b>规则采样</b>：匹配 URL/方法/用户规则，使用规则定义的采样率</li>
 *     <li><b>动态采样</b>：根据实际 Span 速率自动调整采样率</li>
 *     <li><b>默认采样</b>：使用固定采样率</li>
 * </ol>
 * </p>
 * <p>
 * <b>条件加载：</b>
 * <ul>
 *     <li>{@code observability.tracing.enabled=true}（全局追踪开关，默认启用）</li>
 *     <li>{@code observability.tracing.sampler.enabled=true}（采样器开关，默认启用）- Bean 级别</li>
 * </ul>
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * observability:
 *   tracing:
 *     enabled: true
 *     sampler:
 *       enabled: true
 *       default-rate: 0.1           # 默认 10% 采样
 *       always-sample-errors: true   # 强制采样错误
 *       always-sample-slow: true     # 强制采样慢请求
 *       latency-threshold-ms: 1000   # 慢请求阈值 1 秒
 *       rules:
 *         - url-pattern: "/api/admin/.*"
 *           rate: 1.0                 # 管理 API 100% 采样
 *         - url-pattern: "/api/public/.*"
 *           rate: 0.01                # 公共 API 1% 采样
 *       dynamic:
 *         enabled: false              # 是否启用动态采样
 *         min-rate: 0.01
 *         max-rate: 1.0
 *         target-spans-per-minute: 1000
 *         adjust-interval: 30s
 * </pre>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 * @see <a href="https://opentelemetry.io/docs/specs/otel/trace/sdk/#sampler">OpenTelemetry Sampler Specification</a>
 */
@Configuration
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnProperty(
        prefix = "observability.tracing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SamplerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SamplerConfiguration.class);

    private final TracingProperties tracingProperties;

    public SamplerConfiguration(TracingProperties tracingProperties) {
        this.tracingProperties = tracingProperties;
    }

    /**
     * 创建动态采样管理器（仅在启用动态采样时）
     * <p>
     * 独立的 Bean，供采样器和计数处理器使用。
     * </p>
     *
     * @return DynamicSamplingManager 实例
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(
            prefix = "observability.tracing.sampler.dynamic",
            name = "enabled",
            havingValue = "true"
    )
    public DynamicSamplingManager dynamicSamplingManager() {
        TracingProperties.Sampler.Dynamic dynamicConfig = tracingProperties.getSampler().getDynamic();
        DynamicSamplingManager manager = new DynamicSamplingManager(dynamicConfig);
        manager.start();
        log.info("动态采样管理器已创建并启动");
        return manager;
    }

    /**
     * 创建采样器链
     * <p>
     * 按装饰器模式组装采样器，每一层包装下一层。
     * </p>
     *
     * @param dynamicSamplingManager 动态采样管理器（如果启用）
     * @return 最外层采样器
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "observability.tracing.sampler",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public Sampler sampler(
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            DynamicSamplingManager dynamicSamplingManager) {
        TracingProperties.Sampler samplerProps = tracingProperties.getSampler();

        // 第 5 层（最内层）：默认/动态采样器
        Supplier<Sampler> baseSamplerSupplier = createBaseSamplerSupplier(samplerProps, dynamicSamplingManager);

        // 第 4 层：规则采样器
        Sampler sampler = new RuleBasedSampler(samplerProps, baseSamplerSupplier);
        log.info("已创建规则采样器: rules={}", samplerProps.getRules().size());

        // 第 3 层：延迟感知采样器
        if (samplerProps.isAlwaysSampleSlow() && samplerProps.getLatencyThresholdMs() > 0) {
            sampler = new LatencyAwareSampler(
                    samplerProps.getLatencyThresholdMs(),
                    true,
                    sampler
            );
            log.info("已创建延迟感知采样器: thresholdMs={}", samplerProps.getLatencyThresholdMs());
        }

        // 第 2 层（最外层）：错误感知采样器
        if (samplerProps.isAlwaysSampleErrors()) {
            sampler = new ErrorBasedSampler(true, sampler);
            log.info("已创建错误感知采样器");
        }

        log.info("采样器链创建完成: {}", sampler.getDescription());
        return sampler;
    }

    /**
     * 创建基础采样器提供者
     * <p>
     * 如果启用动态采样，返回动态采样器提供者；
     * 否则返回固定采样率提供者。
     * </p>
     *
     * @param samplerProps           采样配置
     * @param dynamicSamplingManager 动态采样管理器（可能为 null）
     * @return 基础采样器提供者
     */
    private Supplier<Sampler> createBaseSamplerSupplier(
            TracingProperties.Sampler samplerProps,
            DynamicSamplingManager dynamicSamplingManager) {

        // 检查是否启用动态采样
        if (dynamicSamplingManager != null) {
            log.info("使用动态采样");
            return dynamicSamplingManager::getCurrentSampler;
        } else {
            // 使用固定采样率
            double defaultRate = Math.max(0.0, Math.min(1.0, samplerProps.getDefaultRate()));
            Sampler fixedSampler = Sampler.traceIdRatioBased(defaultRate);

            log.info("使用固定采样率: defaultRate={}", defaultRate);

            // 返回固定采样器提供者
            return () -> fixedSampler;
        }
    }

    /**
     * 创建采样计数 SpanProcessor（仅在启用动态采样时）
     * <p>
     * 该处理器监听根 Span 的采样事件，并通知 DynamicSamplingManager 进行计数，
     * 以支持动态采样率调整功能。
     * </p>
     * <p>
     * <b>注意：</b>此 SpanProcessor Bean 会被 {@link com.basebackend.observability.otel.config.OtelAutoConfiguration}
     * 自动注册到 SdkTracerProvider。
     * </p>
     *
     * @param dynamicSamplingManager 动态采样管理器
     * @return SamplingCountingSpanProcessor 实例
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "observability.tracing.sampler.dynamic",
            name = "enabled",
            havingValue = "true"
    )
    public SpanProcessor samplingCountingSpanProcessor(DynamicSamplingManager dynamicSamplingManager) {
        log.info("创建采样计数 SpanProcessor");
        return new SamplingCountingSpanProcessor(dynamicSamplingManager);
    }
}
