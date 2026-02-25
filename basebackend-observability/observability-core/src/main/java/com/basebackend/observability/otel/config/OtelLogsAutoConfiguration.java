package com.basebackend.observability.otel.config;

import com.basebackend.observability.otel.exporter.OtlpLogsExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OpenTelemetry 日志自动配置
 * <p>
 * 独立的配置类，仅在 {@code opentelemetry-sdk-logs} 存在于 classpath 时由 Spring 加载。
 * 使用类级别的 {@code @ConditionalOnClass} 确保当日志 SDK 缺失时，
 * JVM 不会尝试解析 {@code SdkLoggerProvider} 等类型，从而避免 {@code NoClassDefFoundError}。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "io.opentelemetry.sdk.logs.SdkLoggerProvider")
@ConditionalOnProperty(value = "observability.otel.enabled", havingValue = "true")
public class OtelLogsAutoConfiguration implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(OtelLogsAutoConfiguration.class);

    private SdkLoggerProvider loggerProvider;

    /**
     * 创建日志导出器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.logs", name = "enabled", havingValue = "true")
    public LogRecordExporter logRecordExporter(OtlpLogsExporter factory) {
        return factory.create();
    }

    /**
     * 创建 LoggerProvider
     * <p>
     * 配置日志数据的批量导出到 OTLP Collector。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "observability.otel.otlp.logs", name = "enabled", havingValue = "true")
    public SdkLoggerProvider sdkLoggerProvider(LogRecordExporter logRecordExporter, Resource resource) {
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

    @Override
    public void destroy() {
        if (loggerProvider != null) {
            try {
                var result = loggerProvider.forceFlush().join(5, TimeUnit.SECONDS);
                if (!result.isSuccess()) {
                    log.warn("SdkLoggerProvider 强制刷新超时或失败，可能有日志数据丢失");
                }
                loggerProvider.close();
                log.info("SdkLoggerProvider 已关闭");
            } catch (Exception e) {
                log.error("关闭 SdkLoggerProvider 失败", e);
            }
        }
    }
}
