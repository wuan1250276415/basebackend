package com.basebackend.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * 线程池健康检查指标
 * <p>
 * 监控线程状态，检测死锁和线程数量异常。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ThreadPoolHealthIndicator implements HealthIndicator {

    /** 最大线程数阈值 */
    private static final int MAX_THREAD_COUNT = 500;

    /** 死锁检测 */
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public Health health() {
        try {
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

            Health.Builder builder = Health.up();

            // 检测死锁
            if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                builder = Health.down();
                builder.withDetail("deadlock", true);
                builder.withDetail("deadlockedThreadCount", deadlockedThreads.length);
                builder.withDetail("deadlockedThreadIds", deadlockedThreads);

                // 记录死锁线程信息
                logDeadlockedThreads(deadlockedThreads);
            }

            // 检测线程数量
            if (threadCount > MAX_THREAD_COUNT) {
                builder = Health.down();
                builder.withDetail("warning", "Thread count exceeds threshold");
            }

            return builder
                    .withDetail("threadCount", threadCount)
                    .withDetail("peakThreadCount", peakThreadCount)
                    .withDetail("daemonThreadCount", threadMXBean.getDaemonThreadCount())
                    .withDetail("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount())
                    .build();

        } catch (Exception e) {
            log.error("Error checking thread pool health", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }

    /**
     * 记录死锁线程信息
     */
    private void logDeadlockedThreads(long[] threadIds) {
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds, true, true);
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null) {
                log.error("Deadlocked thread detected: {} ({})",
                        threadInfo.getThreadName(), threadInfo.getThreadId());
                log.error("Lock info: {}", threadInfo.getLockInfo());
                for (StackTraceElement element : threadInfo.getStackTrace()) {
                    log.error("    at {}", element);
                }
            }
        }
    }
}
