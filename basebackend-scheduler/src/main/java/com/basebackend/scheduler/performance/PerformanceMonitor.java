package com.basebackend.scheduler.performance;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 性能监控器
 *
 * <p>提供全方位的系统性能监控：
 * <ul>
 *   <li>应用性能指标（响应时间、吞吐量）</li>
 *   <li>数据库性能指标（查询时间、连接数）</li>
 *   <li>缓存性能指标（命中率、命中率）</li>
 *   <li>系统资源指标（CPU、内存、线程）</li>
 * </ul>
 *
 * <p>监控指标：
 * <ul>
 *   <li>请求响应时间 P50/P90/P99</li>
 *   <li>每秒请求数（QPS）</li>
 *   <li>数据库连接池使用率</li>
 *   <li>缓存命中率</li>
 *   <li>线程池使用情况</li>
 *   <li>JVM 内存使用情况</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Component
public class PerformanceMonitor {

    private final MeterRegistry meterRegistry;
    private final ThreadMXBean threadMXBean;
    private final MemoryMXBean memoryMXBean;

    // 性能指标存储
    private final Map<String, Timer> responseTimeMetrics = new ConcurrentHashMap<>();
    private final Map<String, Counter> requestCountMetrics = new ConcurrentHashMap<>();
    private final Map<String, Long> customMetrics = new ConcurrentHashMap<>();

    // 性能数据
    private long startTime = System.currentTimeMillis();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    // CPU 监控缓存
    private volatile double lastValidCpuUsage = 0.0;

    // 自定义指标Map容量控制
    private static final int MAX_METRIC_ENTRIES = 1000;

    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    @PostConstruct
    public void init() {
        log.info("Initializing performance monitor...");

        // 注册系统资源指标
        registerSystemMetrics();
    }

    /**
     * 记录请求响应时间
     *
     * @param endpoint 端点名称
     * @param responseTime 响应时间（毫秒）
     */
    public void recordResponseTime(String endpoint, long responseTime) {
        totalResponseTime.addAndGet(responseTime);

        // 更新 Micrometer Timer
        Timer timer = responseTimeMetrics.computeIfAbsent(endpoint, this::createTimer);
        timer.record(responseTime, TimeUnit.MILLISECONDS);

        // 慢请求告警（超过 1 秒）
        if (responseTime > 1000) {
            log.warn("Slow request detected [endpoint={}, responseTime={}ms]", endpoint, responseTime);
        }
    }

    /**
     * 记录请求计数
     *
     * @param endpoint 端点名称
     * @param success 是否成功
     */
    public void recordRequest(String endpoint, boolean success) {
        totalRequests.incrementAndGet();

        if (!success) {
            totalErrors.incrementAndGet();
        }

        Counter counter = requestCountMetrics.computeIfAbsent(endpoint, this::createCounter);
        counter.increment();

        // 错误率告警（超过 5%）
        long errors = totalErrors.get();
        long requests = totalRequests.get();
        double errorRate = (double) errors / requests * 100;
        if (errorRate > 5) {
            log.warn("High error rate detected [endpoint={}, errorRate={}%, totalRequests={}, totalErrors={}]",
                    endpoint, errorRate, requests, errors);
        }
    }

    /**
     * 记录数据库查询时间
     *
     * @param queryName 查询名称
     * @param queryTime 查询时间（毫秒）
     */
    public void recordDatabaseQueryTime(String queryName, long queryTime) {
        String metricName = "database.query.time." + queryName;
        customMetrics.merge(metricName, queryTime, Long::sum);

        // 慢查询告警（超过 500 毫秒）
        if (queryTime > 500) {
            log.warn("Slow database query detected [query={}, time={}ms]", queryName, queryTime);
        }
    }

    /**
     * 记录缓存操作
     *
     * @param cacheName 缓存名称
     * @param hit 是否命中
     */
    public void recordCacheOperation(String cacheName, boolean hit) {
        String hitMetricName = "cache.hit." + cacheName;
        String missMetricName = "cache.miss." + cacheName;

        customMetrics.merge(hitMetricName, hit ? 1L : 0L, Long::sum);
        customMetrics.merge(missMetricName, hit ? 0L : 1L, Long::sum);

        // 计算命中率
        long hits = customMetrics.getOrDefault(hitMetricName, 0L);
        long misses = customMetrics.getOrDefault(missMetricName, 0L);
        long total = hits + misses;

        if (total > 0) {
            double hitRate = (double) hits / total * 100;
            // 命中率告警（低于 80%）
            if (total > 100 && hitRate < 80) {
                log.warn("Low cache hit rate detected [cache={}, hitRate={}%, hits={}, misses={}]",
                        cacheName, hitRate, hits, misses);
            }
        }
    }

    /**
     * 获取当前性能指标
     *
     * @return 性能指标
     */
    public PerformanceMetrics getCurrentMetrics() {
        long now = System.currentTimeMillis();
        long uptime = now - startTime;

        // 获取原子计数器的值
        long totalReq = totalRequests.get();
        long totalErr = totalErrors.get();
        long totalRespTime = totalResponseTime.get();

        // 计算平均值
        double avgResponseTime = totalReq > 0 ? (double) totalRespTime / totalReq : 0;
        double requestsPerSecond = (double) totalReq / uptime * 1000;
        double errorRate = totalReq > 0 ? (double) totalErr / totalReq * 100 : 0;

        // 获取系统资源信息
        SystemResourceMetrics systemMetrics = getSystemResourceMetrics();

        return PerformanceMetrics.builder()
                .uptime(uptime)
                .totalRequests(totalReq)
                .totalErrors(totalErr)
                .avgResponseTime(avgResponseTime)
                .requestsPerSecond(requestsPerSecond)
                .errorRate(errorRate)
                .systemMetrics(systemMetrics)
                .build();
    }

    /**
     * 打印性能报告
     */
    public void printPerformanceReport() {
        PerformanceMetrics metrics = getCurrentMetrics();

        log.info("=== Performance Report ===");
        log.info("Uptime: {} seconds", metrics.getUptime() / 1000);
        log.info("Total Requests: {}", metrics.getTotalRequests());
        log.info("Total Errors: {}", metrics.getTotalErrors());
        log.info("Avg Response Time: {} ms", String.format("%.2f", metrics.getAvgResponseTime()));
        log.info("Requests Per Second: {}", String.format("%.2f", metrics.getRequestsPerSecond()));
        log.info("Error Rate: {}%", String.format("%.2f", metrics.getErrorRate()));

        log.info("--- System Resources ---");
        log.info("CPU Usage: {}%", String.format("%.2f", metrics.getSystemMetrics().getCpuUsage()));
        log.info("Memory Usage: {} MB / {} MB",
                metrics.getSystemMetrics().getUsedMemoryMB(),
                metrics.getSystemMetrics().getTotalMemoryMB());
        log.info("Memory Usage: {}%",
                String.format("%.2f", metrics.getSystemMetrics().getMemoryUsagePercent()));
        log.info("Active Threads: {}", metrics.getSystemMetrics().getActiveThreadCount());

        log.info("--- Request Metrics ---");
        responseTimeMetrics.forEach((endpoint, timer) -> {
            double avgTime = timer.mean(TimeUnit.MILLISECONDS);
            log.info("Endpoint: {}, Avg Response Time: {} ms", endpoint, String.format("%.2f", avgTime));
        });

        log.info("=== End of Report ===");
    }

    /**
     * 创建 Timer 指标
     */
    private Timer createTimer(String endpoint) {
        return Timer.builder("app.endpoint.response_time")
                .description("Endpoint response time")
                .tag("endpoint", endpoint)
                .register(meterRegistry);
    }

    /**
     * 创建 Counter 指标
     */
    private Counter createCounter(String endpoint) {
        return Counter.builder("app.endpoint.requests")
                .description("Endpoint request count")
                .tag("endpoint", endpoint)
                .register(meterRegistry);
    }

    /**
     * 注册系统资源指标
     */
    private void registerSystemMetrics() {
        // CPU 使用率指标
        Gauge.builder("system.cpu.usage", this, PerformanceMonitor::getCpuUsage)
                .description("CPU usage percentage")
                .register(meterRegistry);

        // 内存使用指标
        Gauge.builder("system.memory.usage", this, PerformanceMonitor::getMemoryUsagePercent)
                .description("Memory usage percentage")
                .register(meterRegistry);

        // 线程数指标
        Gauge.builder("system.threads.active", this, PerformanceMonitor::getActiveThreadCount)
                .description("Active thread count")
                .register(meterRegistry);
    }

    /**
     * 获取系统资源指标
     */
    private SystemResourceMetrics getSystemResourceMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        return SystemResourceMetrics.builder()
                .cpuUsage(getCpuUsage())
                .totalMemoryMB(totalMemory / 1024 / 1024)
                .usedMemoryMB(usedMemory / 1024 / 1024)
                .maxMemoryMB(maxMemory / 1024 / 1024)
                .memoryUsagePercent((double) usedMemory / totalMemory * 100)
                .activeThreadCount(threadMXBean.getThreadCount())
                .build();
    }

    /**
     * 获取 CPU 使用率
     */
    private double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double load = osBean.getSystemCpuLoad();
        if (load >= 0) {
            lastValidCpuUsage = load * 100;
        }
        return lastValidCpuUsage;
    }

    /**
     * 获取内存使用率
     */
    private double getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return (double) usedMemory / totalMemory * 100;
    }

    /**
     * 获取活跃线程数
     */
    private int getActiveThreadCount() {
        return threadMXBean.getThreadCount();
    }

    // ========== 结果类定义 ==========

    /**
     * 性能指标
     */
    public static class PerformanceMetrics {
        private long uptime;
        private long totalRequests;
        private long totalErrors;
        private double avgResponseTime;
        private double requestsPerSecond;
        private double errorRate;
        private SystemResourceMetrics systemMetrics;

        private PerformanceMetrics() {}

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public long getUptime() { return uptime; }
        public long getTotalRequests() { return totalRequests; }
        public long getTotalErrors() { return totalErrors; }
        public double getAvgResponseTime() { return avgResponseTime; }
        public double getRequestsPerSecond() { return requestsPerSecond; }
        public double getErrorRate() { return errorRate; }
        public SystemResourceMetrics getSystemMetrics() { return systemMetrics; }

        /**
         * Builder
         */
        public static class Builder {
            private PerformanceMetrics metrics = new PerformanceMetrics();

            public Builder uptime(long uptime) { metrics.uptime = uptime; return this; }
            public Builder totalRequests(long totalRequests) { metrics.totalRequests = totalRequests; return this; }
            public Builder totalErrors(long totalErrors) { metrics.totalErrors = totalErrors; return this; }
            public Builder avgResponseTime(double avgResponseTime) { metrics.avgResponseTime = avgResponseTime; return this; }
            public Builder requestsPerSecond(double requestsPerSecond) { metrics.requestsPerSecond = requestsPerSecond; return this; }
            public Builder errorRate(double errorRate) { metrics.errorRate = errorRate; return this; }
            public Builder systemMetrics(SystemResourceMetrics systemMetrics) { metrics.systemMetrics = systemMetrics; return this; }

            public PerformanceMetrics build() { return metrics; }
        }
    }

    /**
     * 系统资源指标
     */
    public static class SystemResourceMetrics {
        private double cpuUsage;
        private long totalMemoryMB;
        private long usedMemoryMB;
        private long maxMemoryMB;
        private double memoryUsagePercent;
        private int activeThreadCount;

        private SystemResourceMetrics() {}

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public double getCpuUsage() { return cpuUsage; }
        public long getTotalMemoryMB() { return totalMemoryMB; }
        public long getUsedMemoryMB() { return usedMemoryMB; }
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public int getActiveThreadCount() { return activeThreadCount; }

        /**
         * Builder
         */
        public static class Builder {
            private SystemResourceMetrics metrics = new SystemResourceMetrics();

            public Builder cpuUsage(double cpuUsage) { metrics.cpuUsage = cpuUsage; return this; }
            public Builder totalMemoryMB(long totalMemoryMB) { metrics.totalMemoryMB = totalMemoryMB; return this; }
            public Builder usedMemoryMB(long usedMemoryMB) { metrics.usedMemoryMB = usedMemoryMB; return this; }
            public Builder maxMemoryMB(long maxMemoryMB) { metrics.maxMemoryMB = maxMemoryMB; return this; }
            public Builder memoryUsagePercent(double memoryUsagePercent) { metrics.memoryUsagePercent = memoryUsagePercent; return this; }
            public Builder activeThreadCount(int activeThreadCount) { metrics.activeThreadCount = activeThreadCount; return this; }

            public SystemResourceMetrics build() { return metrics; }
        }
    }
}
