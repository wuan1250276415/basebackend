package com.basebackend.observability.export;

import com.basebackend.observability.entity.JvmMetrics;
import com.basebackend.observability.mapper.JvmMetricsMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Prometheus指标导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricsExporter {

    private final MeterRegistry meterRegistry;
    private final JvmMetricsMapper jvmMetricsMapper;

    /**
     * 定期导出JVM指标到Prometheus
     */
    @Scheduled(fixedRate = 30000) // 每30秒更新一次
    public void exportJvmMetrics() {
        try {
            String instanceId = getDefaultInstanceId();
            JvmMetrics latest = jvmMetricsMapper.selectLatest(instanceId);
            
            if (latest == null) {
                return;
            }
            
            Tags tags = Tags.of("instance", instanceId);
            
            // 堆内存
            meterRegistry.gauge("jvm.memory.heap.used", tags, latest.getHeapUsed());
            meterRegistry.gauge("jvm.memory.heap.max", tags, latest.getHeapMax());
            meterRegistry.gauge("jvm.memory.heap.committed", tags, latest.getHeapCommitted());
            
            // 非堆内存
            meterRegistry.gauge("jvm.memory.nonheap.used", tags, latest.getNonHeapUsed());
            
            // 线程
            meterRegistry.gauge("jvm.threads.count", tags, latest.getThreadCount());
            meterRegistry.gauge("jvm.threads.daemon", tags, latest.getDaemonThreadCount());
            meterRegistry.gauge("jvm.threads.peak", tags, latest.getPeakThreadCount());
            
            // GC
            meterRegistry.gauge("jvm.gc.count", tags, latest.getGcCount());
            meterRegistry.gauge("jvm.gc.time", tags, latest.getGcTime());
            
            // CPU
            meterRegistry.gauge("system.cpu.usage", tags, latest.getCpuUsage());
            meterRegistry.gauge("system.load.average", tags, latest.getLoadAverage());
            
            // 计算使用率
            double heapUsagePercent = calculatePercentage(latest.getHeapUsed(), latest.getHeapMax());
            meterRegistry.gauge("jvm.memory.heap.usage.percent", tags, heapUsagePercent);
            
            log.debug("Exported JVM metrics to Prometheus");
            
        } catch (Exception e) {
            log.error("Failed to export metrics to Prometheus", e);
        }
    }

    /**
     * 导出自定义指标
     */
    public void exportCustomMetric(String name, double value, Tags tags) {
        try {
            meterRegistry.gauge(name, tags, value);
        } catch (Exception e) {
            log.error("Failed to export custom metric: {}", name, e);
        }
    }

    private double calculatePercentage(Long used, Long max) {
        if (max == null || max == 0) return 0;
        return (double) used / max * 100;
    }

    private String getDefaultInstanceId() {
        String hostName = System.getenv("HOSTNAME");
        if (hostName == null) {
            try {
                hostName = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                hostName = "localhost";
            }
        }
        return hostName;
    }
}
