package com.basebackend.scheduler.monitoring.config;

import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控配置
 * <p>
 * 使用 Spring Boot Actuator + Micrometer 提供 Prometheus 端点
 * 端点地址：/actuator/prometheus
 * <p>
 * 注意：不再手动创建 Prometheus HTTPServer，而是通过 Actuator 暴露指标
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Configuration
public class MonitoringConfig {

    /**
     * JVM 内存指标
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * JVM GC 指标
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    /**
     * 系统运行时间指标
     */
    @Bean
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }

    /**
     * 文件描述符指标
     */
    @Bean
    public FileDescriptorMetrics fileDescriptorMetrics() {
        return new FileDescriptorMetrics();
    }

    /**
     * CPU 处理器指标
     */
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
}
