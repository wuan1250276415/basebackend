package com.basebackend.logging.monitoring.config;

import com.basebackend.logging.monitoring.collector.CustomMetricsCollector;
import com.basebackend.logging.monitoring.endpoint.LoggingMetricsEndpoint;
import com.basebackend.logging.monitoring.health.MonitoringHealthIndicator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 监控系统 Spring Boot 自动配置
 *
 * 自动配置监控系统的所有核心组件。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@EnableConfigurationProperties(MonitoringProperties.class)
public class MonitoringAutoConfiguration {

    /**
     * 配置自定义指标采集器
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "basebackend.logging.monitoring.toggles",
        name = "performance-metrics",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnMissingBean
    public CustomMetricsCollector customMetricsCollector(MeterRegistry registry) {
        log.info("初始化自定义指标采集器");
        return new CustomMetricsCollector(registry);
    }

    /**
     * 配置日志指标端点
     */
    @Bean
    @ConditionalOnAvailableEndpoint(endpoint = LoggingMetricsEndpoint.class)
    @ConditionalOnMissingBean
    public LoggingMetricsEndpoint loggingMetricsEndpoint(MeterRegistry registry,
                                                         CustomMetricsCollector metricsCollector) {
        log.info("初始化日志指标端点");
        return new LoggingMetricsEndpoint(registry, metricsCollector);
    }

    /**
     * 配置 Grafana 仪表板配置
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "basebackend.logging.monitoring.toggles",
        name = "dashboard-enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnMissingBean
    public GrafanaDashboardConfig grafanaDashboardConfig() {
        log.info("初始化 Grafana 仪表板配置");
        return new GrafanaDashboardConfig();
    }

    /**
     * 配置监控健康检查
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "basebackend.logging.monitoring.toggles",
        name = "health-check-enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnMissingBean
    public MonitoringHealthIndicator monitoringHealthIndicator(MeterRegistry registry) {
        log.info("初始化监控健康检查");
        return new MonitoringHealthIndicator(registry);
    }

    /**
     * 注册关闭钩子
     */
    public void registerShutdownHook(CustomMetricsCollector metricsCollector) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM 关闭钩子执行，导出监控指标");
            try {
                String metricsJson = metricsCollector.exportToJson();
                log.info("监控指标导出完成:\n{}", metricsJson);
            } catch (Exception e) {
                log.error("导出监控指标失败", e);
            }
        }));
    }
}
