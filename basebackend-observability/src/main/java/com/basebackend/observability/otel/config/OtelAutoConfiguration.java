package com.basebackend.observability.otel.config;

import com.basebackend.observability.otel.exporter.OtlpMetricsExporter;
import com.basebackend.observability.otel.exporter.OtlpTracesExporter;
import com.basebackend.observability.otel.resource.ResourceProvider;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
// LoggerProvider 相关导入已移除，因为项目排除了 opentelemetry-sdk-logs 依赖
// import io.opentelemetry.sdk.logs.SdkLoggerProvider;
// import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
// import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OpenTelemetry 自动配置
 * <p>
 * 该配置类负责实例化 OpenTelemetry SDK 组件并配置 OTLP 导出器。
 * 设计为与现有的 Micrometer 和 Brave 共存，支持双栈遥测数据导出。
 * </p>
 * <p>
 * 配置项通过 {@link OtelProperties} 控制，包括：
 * <ul>
 *     <li>服务标识（name、version、environment）</li>
 *     <li>OTLP 导出端点</li>
 *     <li>各类导出器开关</li>
 *     <li>采样率配置</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OtelProperties.class)
@ConditionalOnProperty(value = "observability.otel.enabled", havingValue = "true")
@AutoConfigureAfter(name = {
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration"
})
public class OtelAutoConfiguration implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(OtelAutoConfiguration.class);

    private SdkTracerProvider tracerProvider;
    private SdkMeterProvider meterProvider;
    // LoggerProvider 已被排除，暂不支持 logs 功能
    // private SdkLoggerProvider loggerProvider;

    /**
     * 创建 Telemetry Resource
     * <p>
     * Resource 包含服务元数据，会被附加到所有导出的遥测数据中。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public Resource telemetryResource(ResourceProvider resourceProvider) {
        return resourceProvider.resource();
    }

    /**
     * 创建上下文传播器
     * <p>
     * 使用 W3C Trace Context 和 Baggage 标准，确保跨服务追踪的互操作性。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ContextPropagators contextPropagators() {
        TextMapPropagator propagator = TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance()
        );
        return ContextPropagators.create(propagator);
    }

    /**
     * 创建 TracerProvider
     * <p>
     * 配置追踪数据的采样策略和批量导出。
     * </p>
     * <p>
     * <b>Phase 3 集成：</b>
     * <ul>
     *     <li>如果存在自定义 Sampler Bean（如 SamplerConfiguration 创建的采样器链），则使用自定义采样器</li>
     *     <li>否则使用默认的 TraceIdRatioBased 采样器</li>
     *     <li>注册所有可用的 SpanProcessor Bean（包括 BatchSpanProcessor 和 SamplingCountingSpanProcessor）</li>
     * </ul>
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.traces", name = "enabled", havingValue = "true")
    public SdkTracerProvider sdkTracerProvider(
            SpanExporter spanExporter,
            Resource resource,
            OtelProperties properties,
            @org.springframework.beans.factory.annotation.Autowired(required = false) Sampler customSampler,
            @org.springframework.beans.factory.annotation.Autowired(required = false) java.util.List<io.opentelemetry.sdk.trace.SpanProcessor> customSpanProcessors) {

        // 采样策略：优先使用自定义采样器（Phase 3 采样器链），否则使用默认采样率
        Sampler sampler;
        if (customSampler != null) {
            log.info("使用自定义采样器: {}", customSampler.getDescription());
            sampler = customSampler;
        } else {
            // 默认：基于 TraceID 的概率采样，尊重父级采样决策
            sampler = Sampler.parentBased(
                    Sampler.traceIdRatioBased(properties.getSamplingRatio())
            );
            log.info("使用默认采样器，采样率: {}", properties.getSamplingRatio());
        }

        // 批量 Span 处理器（用于导出到 OTLP）
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(spanExporter)
                .setExporterTimeout(Duration.ofSeconds(5))
                .setMaxQueueSize(2048)
                .setMaxExportBatchSize(512)
                .setScheduleDelay(Duration.ofSeconds(1))
                .build();

        // 构建 TracerProvider
        var builder = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(batchSpanProcessor)  // 始终添加批量处理器
                .setSampler(sampler);

        // 注册自定义 SpanProcessor Bean（如 SamplingCountingSpanProcessor）
        if (customSpanProcessors != null && !customSpanProcessors.isEmpty()) {
            for (io.opentelemetry.sdk.trace.SpanProcessor processor : customSpanProcessors) {
                builder.addSpanProcessor(processor);
                log.info("注册自定义 SpanProcessor: {}", processor.getClass().getSimpleName());
            }
        }

        this.tracerProvider = builder.build();

        log.info("SdkTracerProvider 已创建，SpanProcessor 数量: {}",
                1 + (customSpanProcessors != null ? customSpanProcessors.size() : 0));
        return this.tracerProvider;
    }

    /**
     * 创建 MeterProvider
     * <p>
     * 配置指标数据的周期性导出。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.metrics", name = "enabled", havingValue = "true")
    public SdkMeterProvider sdkMeterProvider(MetricExporter metricExporter, Resource resource) {
        // 周期性指标读取器（每 30 秒导出一次）
        PeriodicMetricReader reader = PeriodicMetricReader.builder(metricExporter)
                .setInterval(Duration.ofSeconds(30))
                .build();

        this.meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(reader)
                .build();

        log.info("SdkMeterProvider 已创建，导出间隔: 30s");
        return this.meterProvider;
    }

    /**
     * 创建 LoggerProvider
     * <p>
     * 配置日志数据的批量导出。
     * </p>
     * <p>
     * <b>注意：</b>由于项目排除了 opentelemetry-sdk-logs 依赖，此功能暂不支持
     * </p>
     */
    /*
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.logs", name = "enabled", havingValue = "true")
    public SdkLoggerProvider sdkLoggerProvider(LogRecordExporter logRecordExporter, Resource resource) {
        // 批量日志处理器
        BatchLogRecordProcessor processor = BatchLogRecordProcessor.builder(logRecordExporter)
                .setExporterTimeout(Duration.ofSeconds(5))
                .setMaxQueueSize(2048)
                .setMaxExportBatchSize(512)
                .setScheduleDelay(Duration.ofSeconds(1))
                .build();

        this.loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(processor)
                .build();

        log.info("SdkLoggerProvider 已创建");
        return this.loggerProvider;
    }
    */

    /**
     * 创建 OpenTelemetry SDK 实例
     * <p>
     * 组装所有组件并注册为全局实例。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetry openTelemetry(
            ContextPropagators propagators,
            Resource resource,
            ObjectProvider<SdkTracerProvider> tracerProviderProvider,
            ObjectProvider<SdkMeterProvider> meterProviderProvider) {
            // LoggerProvider 已被排除，不再注入
            // ObjectProvider<SdkLoggerProvider> loggerProviderProvider

        var builder = OpenTelemetrySdk.builder()
                .setPropagators(propagators);

        // 可选地设置各个 Provider
        Optional.ofNullable(tracerProviderProvider.getIfAvailable())
                .ifPresent(builder::setTracerProvider);

        Optional.ofNullable(meterProviderProvider.getIfAvailable())
                .ifPresent(builder::setMeterProvider);

        // LoggerProvider 已被排除，不再设置
        // Optional.ofNullable(loggerProviderProvider.getIfAvailable())
        //         .ifPresent(builder::setLoggerProvider);

        // 构建并设置为全局实例
        OpenTelemetrySdk sdk = builder.build();

        // 设置为全局实例（仅在未设置时）
        try {
            GlobalOpenTelemetry.get();
            log.warn("GlobalOpenTelemetry 已经被设置，跳过重复设置");
        } catch (IllegalStateException e) {
            GlobalOpenTelemetry.set(sdk);
            log.info("OpenTelemetry SDK 已初始化并设置为全局实例");
        }

        log.info("Resource 属性: {}", resource.getAttributes());

        return sdk;
    }

    /**
     * 创建 Tracer
     * <p>
     * 从 OpenTelemetry 实例获取 Tracer，用于创建和管理 Span。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public io.opentelemetry.api.trace.Tracer tracer(OpenTelemetry openTelemetry, OtelProperties properties) {
        String serviceName = properties.getService().getName();
        if (serviceName == null || serviceName.isEmpty()) {
            serviceName = "unknown-service";
        }

        String instrumentationName = "com.basebackend." + serviceName;
        String instrumentationVersion = properties.getService().getVersion();
        if (instrumentationVersion == null || instrumentationVersion.isEmpty()) {
            instrumentationVersion = "1.0.0";
        }

        io.opentelemetry.api.trace.Tracer tracer = openTelemetry.getTracer(instrumentationName, instrumentationVersion);
        log.info("Tracer 已创建，instrumentation: {} ({})", instrumentationName, instrumentationVersion);

        return tracer;
    }

    /**
     * 创建 Span 导出器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.traces", name = "enabled", havingValue = "true")
    public SpanExporter spanExporter(OtlpTracesExporter factory) {
        return factory.create();
    }

    /**
     * 创建指标导出器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.metrics", name = "enabled", havingValue = "true")
    public MetricExporter metricExporter(OtlpMetricsExporter factory) {
        return factory.create();
    }

    /**
     * 创建日志导出器
     * <p>
     * <b>注意：</b>由于项目排除了 opentelemetry-sdk-logs 依赖，此 Bean 已禁用
     * </p>
     */
    /*
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.logs", name = "enabled", havingValue = "true")
    public LogRecordExporter logExporter(OtlpLogsExporter factory) {
        return factory.create();
    }
    */

    /**
     * 优雅关闭 OpenTelemetry SDK
     * <p>
     * 在应用关闭时，确保所有待处理的遥测数据被导出，并释放相关资源。
     * </p>
     */
    @Override
    @PreDestroy
    public void destroy() {
        log.info("开始关闭 OpenTelemetry SDK...");

        if (tracerProvider != null) {
            try {
                var result = tracerProvider.forceFlush().join(5, TimeUnit.SECONDS);
                if (!result.isSuccess()) {
                    log.warn("SdkTracerProvider 强制刷新超时或失败，可能有遥测数据丢失");
                }
                tracerProvider.close();
                log.info("SdkTracerProvider 已关闭");
            } catch (Exception e) {
                log.error("关闭 SdkTracerProvider 失败", e);
            }
        }

        if (meterProvider != null) {
            try {
                var result = meterProvider.forceFlush().join(5, TimeUnit.SECONDS);
                if (!result.isSuccess()) {
                    log.warn("SdkMeterProvider 强制刷新超时或失败，可能有遥测数据丢失");
                }
                meterProvider.close();
                log.info("SdkMeterProvider 已关闭");
            } catch (Exception e) {
                log.error("关闭 SdkMeterProvider 失败", e);
            }
        }

        // LoggerProvider 已被排除
        /*
        if (loggerProvider != null) {
            try {
                loggerProvider.forceFlush().join(5, TimeUnit.SECONDS);
                loggerProvider.close();
                log.info("SdkLoggerProvider 已关闭");
            } catch (Exception e) {
                log.error("关闭 SdkLoggerProvider 失败", e);
            }
        }
        */

        log.info("OpenTelemetry SDK 关闭完成");
    }
}
