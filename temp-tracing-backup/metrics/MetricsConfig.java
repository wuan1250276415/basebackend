package com.basebackend.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusExportConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collections;

/**
 * 自定义指标监控配置
 * 配置 Micrometer 和 Prometheus 指标收集
 *
 * 主要功能:
 * 1. 统一指标注册表配置
 * 2. 自定义指标标签
 * 3. 指标数据采样和聚合配置
 * 4. Prometheus 导出器配置
 * 5. 指标过滤和重命名
 * 6. 分布式追踪指标关联
 *
 * @author basebackend team
 * @version 1.0
 */
@Configuration
public class MetricsConfig {

    @Value("${otel.service.name:basebackend-service}")
    private String serviceName;

    @Value("${otel.service.version:1.0.0}")
    private String serviceVersion;

    @Value("${spring.profiles.active:production}")
    private String environment;

    /**
     * 配置 Prometheus 指标注册表
     */
    @Bean
    public MeterRegistry meterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // 配置指标采样
        registry.config()
            .meterFilter(MeterFilter.ignoreTags("networking", "peer.hostname"))
            .meterFilter(MeterFilter.ignoreTags("networking", "peer.ip"))
            .meterFilter(MeterFilter.ignoreTags("client", "peer.hostname"))
            .meterFilter(MeterFilter.replaceTagWith("application", "basebackend"));

        // 配置定时器统计
        registry.config().defaultDistributionStatisticConfig(
            DistributionStatisticConfig.builder()
                .percentiles(0.5, 0.75, 0.95, 0.99)
                .percentilesHistogram(true)
                .sla(Duration.ofMillis(100), Duration.ofMillis(1000))
                .build()
        );

        return registry;
    }

    /**
     * 自定义 MeterRegistry 配置
     * 添加全局标签
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer() {
        return registry -> {
            // 添加全局标签
            registry.config()
                .commonTags(
                    "service", serviceName,
                    "version", serviceVersion,
                    "environment", environment,
                    "team", "backend"
                );

            // 配置指标采样
            registry.config().defaultDistributionStatisticConfig(
                DistributionStatisticConfig.builder()
                    .percentiles(0.5, 0.75, 0.95, 0.99)
                    .percentilesHistogram(true)
                    .sla(Duration.ofMillis(50), Duration.ofMillis(200), Duration.ofMillis(500))
                    .build()
            );

            // 配置指标过滤
            registry.config().meterFilter(
                MeterFilter.renameTag("http.server.requests", "uri", "redacted")
            );
        };
    }

    /**
     * 配置指标导出器
     */
    @Bean
    public PrometheusExportConfiguration prometheusExportConfiguration() {
        return new PrometheusExportConfiguration();
    }

    /**
     * 自定义业务指标计数器
     */
    @Bean
    public BusinessMetrics businessMetrics(MeterRegistry registry) {
        return new BusinessMetrics(registry);
    }

    /**
     * 自定义性能指标计数器
     */
    @Bean
    public PerformanceMetrics performanceMetrics(MeterRegistry registry) {
        return new PerformanceMetrics(registry);
    }

    /**
     * 自定义系统指标计数器
     */
    @Bean
    public SystemMetrics systemMetrics(MeterRegistry registry) {
        return new SystemMetrics(registry);
    }

    /**
     * HTTP 指标配置
     */
    @Bean
    public HttpMetrics httpMetrics(MeterRegistry registry) {
        return new HttpMetrics(registry);
    }

    /**
     * 数据库指标配置
     */
    @Bean
    public DatabaseMetrics databaseMetrics(MeterRegistry registry) {
        return new DatabaseMetrics(registry);
    }

    /**
     * 缓存指标配置
     */
    @Bean
    public CacheMetrics cacheMetrics(MeterRegistry registry) {
        return new CacheMetrics(registry);
    }

    /**
     * 消息队列指标配置
     */
    @Bean
    public MessageQueueMetrics messageQueueMetrics(MeterRegistry registry) {
        return new MessageQueueMetrics(registry);
    }

    /**
     * 自定义 HTTP Server 指标导出器
     */
    @Bean
    public HTTPServer prometheusServer(CollectorRegistry collectorRegistry) {
        try {
            return new HTTPServer("0.0.0.0", 8090, collectorRegistry);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Prometheus HTTP server", e);
        }
    }

    /**
     * 指标采样配置
     */
    @Bean
    public MeterFilter samplingMeterFilter() {
        return MeterFilter.duplicateTagSamples(false);
    }
}
