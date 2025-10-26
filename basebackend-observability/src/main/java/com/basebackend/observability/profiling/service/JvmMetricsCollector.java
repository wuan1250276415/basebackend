package com.basebackend.observability.profiling.service;

import com.basebackend.observability.entity.JvmMetrics;
import com.basebackend.observability.mapper.JvmMetricsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.time.LocalDateTime;

/**
 * JVM性能指标采集器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JvmMetricsCollector {

    private final JvmMetricsMapper jvmMetricsMapper;
    
    private static final String INSTANCE_ID = getInstanceId();

    /**
     * 定时采集JVM指标
     */
    @Scheduled(fixedRate = 10000) // 每10秒采集一次
    public void collectJvmMetrics() {
        try {
            JvmMetrics metrics = collectMetrics();
            
            // 存储指标
            storeMetrics(metrics);
            
            // 检查告警
            checkAlerts(metrics);
            
        } catch (Exception e) {
            log.error("Failed to collect JVM metrics", e);
        }
    }

    /**
     * 采集JVM指标
     */
    private JvmMetrics collectMetrics() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        
        // 获取GC信息
        int gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += gc.getCollectionCount();
            gcTime += gc.getCollectionTime();
        }
        
        // 获取CPU使用率
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double cpuUsage = 0;
        double loadAverage = os.getSystemLoadAverage();
        
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOs = 
                    (com.sun.management.OperatingSystemMXBean) os;
            cpuUsage = sunOs.getProcessCpuLoad() * 100;
        }

        return JvmMetrics.builder()
                .instanceId(INSTANCE_ID)
                .timestamp(LocalDateTime.now())
                .heapUsed(memory.getHeapMemoryUsage().getUsed())
                .heapMax(memory.getHeapMemoryUsage().getMax())
                .heapCommitted(memory.getHeapMemoryUsage().getCommitted())
                .nonHeapUsed(memory.getNonHeapMemoryUsage().getUsed())
                .threadCount(thread.getThreadCount())
                .daemonThreadCount(thread.getDaemonThreadCount())
                .peakThreadCount(thread.getPeakThreadCount())
                .gcCount(gcCount)
                .gcTime(gcTime)
                .cpuUsage(cpuUsage)
                .loadAverage(loadAverage)
                .build();
    }

    /**
     * 存储指标
     */
    private void storeMetrics(JvmMetrics metrics) {
        try {
            jvmMetricsMapper.insert(metrics);
            
            log.debug("JVM Metrics - Heap: {}MB/{} MB, Threads: {}, CPU: {}%", 
                    metrics.getHeapUsed() / 1024 / 1024,
                    metrics.getHeapMax() / 1024 / 1024,
                    metrics.getThreadCount(),
                    String.format("%.2f", metrics.getCpuUsage()));
        } catch (Exception e) {
            log.error("Failed to store JVM metrics", e);
        }
    }

    /**
     * 检查告警条件
     */
    private void checkAlerts(JvmMetrics metrics) {
        // 检查堆内存使用率
        double heapUsagePercent = (double) metrics.getHeapUsed() / metrics.getHeapMax() * 100;
        if (heapUsagePercent > 90) {
            log.warn("High heap usage: {}%", String.format("%.2f", heapUsagePercent));
            // TODO: 发送告警
        }
        
        // 检查线程数
        if (metrics.getThreadCount() > 1000) {
            log.warn("High thread count: {}", metrics.getThreadCount());
            // TODO: 发送告警
        }
        
        // 检查CPU使用率
        if (metrics.getCpuUsage() > 80) {
            log.warn("High CPU usage: {}%", String.format("%.2f", metrics.getCpuUsage()));
            // TODO: 发送告警
        }
    }

    /**
     * 获取实例ID
     */
    private static String getInstanceId() {
        String hostName = System.getenv("HOSTNAME");
        if (hostName == null) {
            try {
                hostName = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                hostName = "unknown";
            }
        }
        return hostName + "-" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }
}
