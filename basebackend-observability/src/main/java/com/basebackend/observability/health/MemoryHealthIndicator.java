package com.basebackend.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JVM内存健康检查指标
 * <p>
 * 监控堆内存和非堆内存使用情况，检测内存泄漏风险。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class MemoryHealthIndicator implements HealthIndicator {

    /** 内存使用率警告阈值 */
    private static final double MEMORY_WARNING_THRESHOLD = 0.8;

    /** 内存使用率严重阈值 */
    private static final double MEMORY_CRITICAL_THRESHOLD = 0.9;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    @Override
    public Health health() {
        try {
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

            double heapUsedRatio = (double) heapUsage.getUsed() / heapUsage.getMax();

            Health.Builder builder;
            if (heapUsedRatio >= MEMORY_CRITICAL_THRESHOLD) {
                builder = Health.down();
                builder.withDetail("status", "CRITICAL");
            } else if (heapUsedRatio >= MEMORY_WARNING_THRESHOLD) {
                builder = Health.status("WARNING");
                builder.withDetail("status", "WARNING");
            } else {
                builder = Health.up();
            }

            // 堆内存
            Map<String, Object> heap = new HashMap<>();
            heap.put("init", formatBytes(heapUsage.getInit()));
            heap.put("used", formatBytes(heapUsage.getUsed()));
            heap.put("committed", formatBytes(heapUsage.getCommitted()));
            heap.put("max", formatBytes(heapUsage.getMax()));
            heap.put("usedRatio", String.format("%.2f%%", heapUsedRatio * 100));

            // 非堆内存
            Map<String, Object> nonHeap = new HashMap<>();
            nonHeap.put("init", formatBytes(nonHeapUsage.getInit()));
            nonHeap.put("used", formatBytes(nonHeapUsage.getUsed()));
            nonHeap.put("committed", formatBytes(nonHeapUsage.getCommitted()));

            // 内存池详情
            Map<String, Map<String, String>> pools = getMemoryPoolDetails();

            return builder
                    .withDetail("heap", heap)
                    .withDetail("nonHeap", nonHeap)
                    .withDetail("pools", pools)
                    .build();

        } catch (Exception e) {
            log.error("Error checking memory health", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }

    /**
     * 获取内存池详情
     */
    private Map<String, Map<String, String>> getMemoryPoolDetails() {
        Map<String, Map<String, String>> pools = new HashMap<>();
        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean pool : memoryPools) {
            Map<String, String> poolInfo = new HashMap<>();
            MemoryUsage usage = pool.getUsage();

            poolInfo.put("type", pool.getType().name());
            poolInfo.put("used", formatBytes(usage.getUsed()));
            poolInfo.put("max", formatBytes(usage.getMax()));

            if (usage.getMax() > 0) {
                double ratio = (double) usage.getUsed() / usage.getMax();
                poolInfo.put("usedRatio", String.format("%.2f%%", ratio * 100));
            }

            pools.put(pool.getName(), poolInfo);
        }
        return pools;
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "N/A";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
