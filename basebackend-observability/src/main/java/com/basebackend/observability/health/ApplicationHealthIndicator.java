package com.basebackend.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 应用程序健康检查器
 * 检查应用程序的整体健康状态（线程、内存、系统负载等）
 */
@Slf4j
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

    // 内存使用率阈值
    private static final double MEMORY_THRESHOLD = 0.9; // 90%

    // 线程数阈值
    private static final int THREAD_COUNT_THRESHOLD = 1000;

    // 死锁线程阈值
    private static final int DEADLOCKED_THREADS_THRESHOLD = 0;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // 检查内存使用情况
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

            long usedMemory = heapMemoryUsage.getUsed();
            long maxMemory = heapMemoryUsage.getMax();
            double memoryUsagePercent = (double) usedMemory / maxMemory;

            Map<String, Object> memoryDetails = new HashMap<>();
            memoryDetails.put("used", formatBytes(usedMemory));
            memoryDetails.put("max", formatBytes(maxMemory));
            memoryDetails.put("usagePercent", String.format("%.2f%%", memoryUsagePercent * 100));
            details.put("memory", memoryDetails);

            // 检查线程情况
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            int daemonThreadCount = threadMXBean.getDaemonThreadCount();

            Map<String, Object> threadDetails = new HashMap<>();
            threadDetails.put("count", threadCount);
            threadDetails.put("peak", peakThreadCount);
            threadDetails.put("daemon", daemonThreadCount);

            // 检查死锁
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
            int deadlockedThreadCount = deadlockedThreads != null ? deadlockedThreads.length : 0;
            threadDetails.put("deadlocked", deadlockedThreadCount);

            details.put("threads", threadDetails);

            // 检查系统负载（如果可用）
            try {
                double systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
                if (systemLoadAverage >= 0) {
                    details.put("systemLoadAverage", String.format("%.2f", systemLoadAverage));
                }
            } catch (Exception e) {
                log.debug("Failed to get system load average", e);
            }

            // 获取运行时间
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            details.put("uptime", formatUptime(uptime));

            // 健康检查逻辑
            if (memoryUsagePercent > MEMORY_THRESHOLD) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("message",
                                String.format("Memory usage too high (%.2f%% used, %.2f%% threshold)",
                                        memoryUsagePercent * 100, MEMORY_THRESHOLD * 100))
                        .build();
            }

            if (threadCount > THREAD_COUNT_THRESHOLD) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("message",
                                String.format("Thread count too high (%d threads, %d threshold)",
                                        threadCount, THREAD_COUNT_THRESHOLD))
                        .build();
            }

            if (deadlockedThreadCount > DEADLOCKED_THREADS_THRESHOLD) {
                return Health.down()
                        .withDetails(details)
                        .withDetail("message",
                                String.format("Deadlocked threads detected (%d threads)",
                                        deadlockedThreadCount))
                        .build();
            }

            return Health.up()
                    .withDetails(details)
                    .build();

        } catch (Exception e) {
            log.error("Application health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * 格式化运行时间为人类可读格式
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
