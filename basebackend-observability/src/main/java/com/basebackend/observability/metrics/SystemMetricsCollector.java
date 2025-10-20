package com.basebackend.observability.metrics;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JVM 和系统指标采集器
 * 采集 JVM 内存、GC、线程、系统 CPU、文件描述符等指标
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SystemMetricsCollector {

    /**
     * 配置 JVM 指标
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            // JVM 内存指标
            new JvmMemoryMetrics().bindTo(registry);

            // JVM GC 指标
            new JvmGcMetrics().bindTo(registry);

            // JVM 线程指标
            new JvmThreadMetrics().bindTo(registry);

            // JVM 类加载指标
            new ClassLoaderMetrics().bindTo(registry);

            // JVM Compilation 指标（JIT编译）
            new JvmCompilationMetrics().bindTo(registry);

            // 进程内存指标（更详细）
            new ProcessMemoryMetrics().bindTo(registry);

            // 进程线程指标（更详细）
            new ProcessThreadMetrics().bindTo(registry);

            // 系统 CPU 指标
            new ProcessorMetrics().bindTo(registry);

            // 系统正常运行时间
            new UptimeMetrics().bindTo(registry);

            // 文件描述符指标
            new FileDescriptorMetrics().bindTo(registry);

            // 磁盘空间指标
            new io.micrometer.core.instrument.binder.system.DiskSpaceMetrics(new java.io.File("/")).bindTo(registry);

            log.info("JVM and System metrics configured successfully");
        };
    }
}
