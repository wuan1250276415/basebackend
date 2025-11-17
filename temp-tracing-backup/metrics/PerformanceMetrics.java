package com.basebackend.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;

/**
 * 性能指标收集器
 * 收集应用性能相关的自定义指标数据
 *
 * 主要指标:
 * 1. 响应时间指标 (HTTP 请求、API 调用)
 * 2. 吞吐量指标 (QPS、RPS)
 * 3. 资源利用率指标 (CPU、内存、线程池)
 * 4. 数据库性能指标 (连接池、查询时间)
 * 5. 缓存性能指标 (命中率、响应时间)
 * 6. JVM 性能指标 (GC、堆内存)
 *
 * @author basebackend team
 * @version 1.0
 */
public class PerformanceMetrics {

    private final MeterRegistry registry;
    private final List<Tag> commonTags;

    // HTTP 性能指标
    private final Timer httpRequestTimer;
    private final Counter httpRequestCounter;
    private final Counter httpRequestErrorCounter;
    private final DistributionSummary httpRequestSizeSummary;
    private final DistributionSummary httpResponseSizeSummary;

    // API 性能指标
    private final Timer apiRequestTimer;
    private final Counter apiRequestCounter;
    private final DistributionSummary apiRequestSummary;

    // 数据库性能指标
    private final Timer databaseQueryTimer;
    private final Counter databaseQueryCounter;
    private final AtomicInteger databaseConnectionPool = new AtomicInteger(0);

    // 缓存性能指标
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer cacheOperationTimer;

    // 线程池指标
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger poolThreads = new AtomicInteger(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);

    // 吞吐量指标
    private final AtomicLong requestPerSecond = new AtomicLong(0);
    private final AtomicLong operationPerSecond = new AtomicLong(0);

    // 错误率指标
    private final AtomicLong errorCounter = new AtomicLong(0);

    // JVM 指标
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    // 定时器采样器
    private Timer.Sample httpSample;
    private Timer.Sample apiSample;
    private Timer.Sample dbSample;

    public PerformanceMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 添加通用标签
        this.commonTags = new ArrayList<>();
        commonTags.add(Tag.of("category", "performance"));

        // HTTP 性能指标
        this.httpRequestTimer = Timer.builder("http.request.duration")
            .description("HTTP request duration")
            .tags(commonTags)
            .register(registry);

        this.httpRequestCounter = Counter.builder("http.request.total")
            .description("Total HTTP requests")
            .tags(commonTags)
            .register(registry);

        this.httpRequestErrorCounter = Counter.builder("http.request.error")
            .description("HTTP request errors")
            .tags(commonTags)
            .register(registry);

        this.httpRequestSizeSummary = DistributionSummary.builder("http.request.size")
            .description("HTTP request size")
            .tags(commonTags)
            .register(registry);

        this.httpResponseSizeSummary = DistributionSummary.builder("http.response.size")
            .description("HTTP response size")
            .tags(commonTags)
            .register(registry);

        // API 性能指标
        this.apiRequestTimer = Timer.builder("api.request.duration")
            .description("API request duration")
            .tags(commonTags)
            .register(registry);

        this.apiRequestCounter = Counter.builder("api.request.total")
            .description("Total API requests")
            .tags(commonTags)
            .register(registry);

        this.apiRequestSummary = DistributionSummary.builder("api.request.summary")
            .description("API request summary")
            .tags(commonTags)
            .register(registry);

        // 数据库性能指标
        this.databaseQueryTimer = Timer.builder("database.query.duration")
            .description("Database query duration")
            .tags(commonTags)
            .register(registry);

        this.databaseQueryCounter = Counter.builder("database.query.total")
            .description("Total database queries")
            .tags(commonTags)
            .register(registry);

        // 连接池指标
        Gauge.builder("database.connection.pool")
            .description("Database connection pool size")
            .tags(commonTags)
            .register(registry, databaseConnectionPool, AtomicInteger::get);

        // 缓存性能指标
        this.cacheHitCounter = Counter.builder("cache.hit")
            .description("Cache hits")
            .tags(commonTags)
            .register(registry);

        this.cacheMissCounter = Counter.builder("cache.miss")
            .description("Cache misses")
            .tags(commonTags)
            .register(registry);

        this.cacheOperationTimer = Timer.builder("cache.operation.duration")
            .description("Cache operation duration")
            .tags(commonTags)
            .register(registry);

        // 线程池指标
        Gauge.builder("thread.active")
            .description("Active threads")
            .tags(commonTags)
            .register(registry, activeThreads, AtomicInteger::get);

        Gauge.builder("thread.pool.size")
            .description("Thread pool size")
            .tags(commonTags)
            .register(registry, poolThreads, AtomicInteger::get);

        Gauge.builder("thread.queue.size")
            .description("Thread queue size")
            .tags(commonTags)
            .register(registry, queueSize, AtomicInteger::get);

        // 吞吐量指标
        Gauge.builder("throughput.requests_per_second")
            .description("Requests per second")
            .tags(commonTags)
            .register(registry, requestPerSecond, AtomicLong::get);

        Gauge.builder("throughput.operations_per_second")
            .description("Operations per second")
            .tags(commonTags)
            .register(registry, operationPerSecond, AtomicLong::get);

        // 错误率指标
        Gauge.builder("error.rate")
            .description("Error rate")
            .tags(commonTags)
            .register(registry, errorCounter, AtomicLong::get);

        // JVM 指标
        setupJVMMetrics();
    }

    // ==================== HTTP 性能指标 ====================

    /**
     * 开始 HTTP 请求计时
     */
    public void startHttpRequest() {
        httpSample = Timer.start(registry);
    }

    /**
     * 结束 HTTP 请求计时并记录指标
     */
    public void endHttpRequest(String method, String endpoint, int statusCode, long responseSize) {
        if (httpSample != null) {
            httpSample.stop(httpRequestTimer);

            // 添加标签
            httpRequestTimer.record(Tag.of("method", method));
            httpRequestTimer.record(Tag.of("endpoint", endpoint));
            httpRequestTimer.record(Tag.of("status", String.valueOf(statusCode)));

            httpRequestCounter.increment();
            httpRequestCounter.increment(Tag.of("method", method));
            httpRequestCounter.increment(Tag.of("endpoint", endpoint));

            // 记录响应大小
            if (responseSize > 0) {
                httpResponseSizeSummary.record(responseSize);
                httpResponseSizeSummary.record(Tag.of("endpoint", endpoint), responseSize);
            }

            // 记录错误
            if (statusCode >= 400) {
                httpRequestErrorCounter.increment();
                httpRequestErrorCounter.increment(Tag.of("status", String.valueOf(statusCode)));
            }

            httpSample = null;
        }
    }

    /**
     * 记录 HTTP 请求 (简化版本)
     */
    public void recordHttpRequest(String method, String endpoint, int statusCode,
                                long durationMs, long requestSize, long responseSize) {
        httpRequestTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        httpRequestTimer.record(Tag.of("method", method));
        httpRequestTimer.record(Tag.of("endpoint", endpoint));
        httpRequestTimer.record(Tag.of("status", String.valueOf(statusCode)));

        httpRequestCounter.increment();

        if (requestSize > 0) {
            httpRequestSizeSummary.record(requestSize);
        }

        if (responseSize > 0) {
            httpResponseSizeSummary.record(responseSize);
        }

        if (statusCode >= 400) {
            httpRequestErrorCounter.increment();
        }
    }

    // ==================== API 性能指标 ====================

    /**
     * 开始 API 请求计时
     */
    public void startApiRequest() {
        apiSample = Timer.start(registry);
    }

    /**
     * 结束 API 请求计时
     */
    public void endApiRequest(String endpoint) {
        if (apiSample != null) {
            apiSample.stop(apiRequestTimer);
            apiRequestTimer.record(Tag.of("endpoint", endpoint));
            apiRequestCounter.increment();
            apiSample = null;
        }
    }

    /**
     * 记录 API 请求
     */
    public void recordApiRequest(String endpoint, long durationMs) {
        apiRequestTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        apiRequestTimer.record(Tag.of("endpoint", endpoint));
        apiRequestCounter.increment();
        apiRequestSummary.record(durationMs);
    }

    // ==================== 数据库性能指标 ====================

    /**
     * 开始数据库查询计时
     */
    public void startDatabaseQuery() {
        dbSample = Timer.start(registry);
    }

    /**
     * 结束数据库查询计时
     */
    public void endDatabaseQuery(String queryType) {
        if (dbSample != null) {
            dbSample.stop(databaseQueryTimer);
            databaseQueryTimer.record(Tag.of("query_type", queryType));
            databaseQueryCounter.increment();
            dbSample = null;
        }
    }

    /**
     * 记录数据库查询
     */
    public void recordDatabaseQuery(String queryType, long durationMs) {
        databaseQueryTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        databaseQueryTimer.record(Tag.of("query_type", queryType));
        databaseQueryCounter.increment();
    }

    /**
     * 更新数据库连接池大小
     */
    public void setDatabaseConnectionPool(int size) {
        databaseConnectionPool.set(size);
    }

    // ==================== 缓存性能指标 ====================

    /**
     * 记录缓存命中
     */
    public void recordCacheHit(String cacheName) {
        cacheHitCounter.increment();
        cacheHitCounter.increment(Tag.of("cache", cacheName));
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss(String cacheName) {
        cacheMissCounter.increment();
        cacheMissCounter.increment(Tag.of("cache", cacheName));
    }

    /**
     * 记录缓存操作耗时
     */
    public void recordCacheOperation(String cacheName, String operation, long durationMs) {
        cacheOperationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        cacheOperationTimer.record(Tag.of("cache", cacheName));
        cacheOperationTimer.record(Tag.of("operation", operation));
    }

    // ==================== 线程池指标 ====================

    /**
     * 更新线程池指标
     */
    public void updateThreadPoolMetrics(int active, int pool, int queue) {
        activeThreads.set(active);
        poolThreads.set(pool);
        queueSize.set(queue);
    }

    // ==================== 吞吐量指标 ====================

    /**
     * 更新请求吞吐量
     */
    public void updateRequestThroughput(long rps) {
        requestPerSecond.set(rps);
    }

    /**
     * 更新操作吞吐量
     */
    public void updateOperationThroughput(long ops) {
        operationPerSecond.set(ops);
    }

    /**
     * 增加错误计数
     */
    public void incrementErrorCount() {
        errorCounter.incrementAndGet();
    }

    // ==================== JVM 指标 ====================

    private void setupJVMMetrics() {
        // 堆内存使用量
        Gauge.builder("jvm.memory.used")
            .description("JVM memory used")
            .tags(commonTags)
            .register(registry, this::getHeapMemoryUsed);

        // 堆内存最大大小
        Gauge.builder("jvm.memory.max")
            .description("JVM memory max")
            .tags(commonTags)
            .register(registry, this::getHeapMemoryMax);

        // GC 次数
        for (int i = 0; i < gcBeans.size(); i++) {
            final int index = i;
            Gauge.builder("jvm.gc.count")
                .description("JVM GC count")
                .tags(commonTags)
                .register(registry, () -> gcBeans.get(index).getCollectionCount());
        }

        // GC 时间
        for (int i = 0; i < gcBeans.size(); i++) {
            final int index = i;
            Gauge.builder("jvm.gc.time")
                .description("JVM GC time")
                .tags(commonTags)
                .register(registry, () -> gcBeans.get(index).getCollectionTime());
        }

        // 活动线程数
        Gauge.builder("jvm.threads.active")
            .description("JVM active threads")
            .tags(commonTags)
            .register(registry, () -> Thread.getAllStackTraces().keySet().size());
    }

    private double getHeapMemoryUsed() {
        return memoryBean.getHeapMemoryUsage().getUsed() / 1024.0 / 1024.0; // MB
    }

    private double getHeapMemoryMax() {
        return memoryBean.getHeapMemoryUsage().getMax() / 1024.0 / 1024.0; // MB
    }

    // ==================== 工具方法 ====================

    /**
     * 重置所有指标
     */
    public void resetMetrics() {
        // Micrometer 不支持重置计数器，这里只是示例
        requestPerSecond.set(0);
        operationPerSecond.set(0);
        errorCounter.set(0);
    }

    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate(String cacheName) {
        try {
            double hits = cacheHitCounter.count();
            double misses = cacheMissCounter.count();
            double total = hits + misses;
            return total > 0 ? (hits / total) * 100 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
