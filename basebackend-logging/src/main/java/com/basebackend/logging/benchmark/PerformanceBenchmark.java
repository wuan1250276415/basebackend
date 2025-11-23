package com.basebackend.logging.benchmark;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能基准测试框架
 *
 * 提供统一的性能测试能力：
 * - 吞吐量测试 (TPS)
 * - 延迟测试 (Latency)
 * - 并发测试 (Concurrency)
 * - 内存使用测试 (Memory)
 * - 资源占用测试 (Resource)
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Slf4j
@Component
public class PerformanceBenchmark {

    /**
     * 执行吞吐率测试
     *
     * @param testCase 测试用例
     * @param options 测试选项
     * @return 测试结果
     */
    public ThroughputResult runThroughputTest(BenchmarkTestCase testCase, TestOptions options) {
        log.info("开始吞吐率测试: {}", testCase.getName());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(options.getThreadCount());
        CountDownLatch latch = new CountDownLatch(options.getTotalRequests());

        for (int i = 0; i < options.getTotalRequests(); i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    long requestStart = System.nanoTime();
                    testCase.execute(requestId);
                    long requestEnd = System.nanoTime();

                    long latency = requestEnd - requestStart;
                    totalLatency.addAndGet(latency);
                    latencies.add(latency);

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("请求执行失败: id={}", requestId, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(options.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("测试被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        // 计算统计指标
        double tps = successCount.get() / (durationMs / 1000.0);
        double avgLatencyMs = totalLatency.get() / (successCount.get() * 1_000_000.0);

        Collections.sort(latencies);
        double p50LatencyMs = getPercentile(latencies, 50);
        double p95LatencyMs = getPercentile(latencies, 95);
        double p99LatencyMs = getPercentile(latencies, 99);

        log.info("吞吐率测试完成: TPS={}, P95={}ms, 成功率={}%",
                tps, p95LatencyMs, getSuccessRate(successCount.get(), errorCount.get()));

        return ThroughputResult.builder()
                .testName(testCase.getName())
                .totalRequests(options.getTotalRequests())
                .successCount(successCount.get())
                .errorCount(errorCount.get())
                .durationMs(durationMs)
                .tps(tps)
                .avgLatencyMs(avgLatencyMs)
                .p50LatencyMs(p50LatencyMs)
                .p95LatencyMs(p95LatencyMs)
                .p99LatencyMs(p99LatencyMs)
                .successRate(getSuccessRate(successCount.get(), errorCount.get()))
                .build();
    }

    /**
     * 执行延迟测试
     *
     * @param testCase 测试用例
     * @param requests 请求数量
     * @return 测试结果
     */
    public LatencyResult runLatencyTest(BenchmarkTestCase testCase, int requests) {
        log.info("开始延迟测试: {}, requests={}", testCase.getName(), requests);

        List<Long> latencies = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        for (int i = 0; i < requests; i++) {
            try {
                long start = System.nanoTime();
                testCase.execute(i);
                long end = System.nanoTime();

                latencies.add((end - start) / 1_000_000); // 转换为毫秒
                successCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("延迟测试请求失败: id={}", i, e);
            }
        }

        Collections.sort(latencies);

        log.info("延迟测试完成: 平均={}ms, P95={}ms, P99={}ms",
                getAverage(latencies), getPercentile(latencies, 95), getPercentile(latencies, 99));

        return LatencyResult.builder()
                .testName(testCase.getName())
                .totalRequests(requests)
                .successCount(successCount)
                .errorCount(errorCount)
                .avgLatencyMs(getAverage(latencies))
                .minLatencyMs(latencies.isEmpty() ? 0 : latencies.get(0))
                .maxLatencyMs(latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1))
                .p50LatencyMs(getPercentile(latencies, 50))
                .p95LatencyMs(getPercentile(latencies, 95))
                .p99LatencyMs(getPercentile(latencies, 99))
                .successRate(getSuccessRate(successCount, errorCount))
                .build();
    }

    /**
     * 执行并发测试
     *
     * @param testCase 测试用例
     * @param threadCount 线程数
     * @param durationSeconds 持续时间（秒）
     * @return 测试结果
     */
    public ConcurrencyResult runConcurrencyTest(BenchmarkTestCase testCase, int threadCount, int durationSeconds) {
        log.info("开始并发测试: {}, threads={}, duration={}s",
                testCase.getName(), threadCount, durationSeconds);

        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    while (System.currentTimeMillis() - startTime < durationSeconds * 1000L) {
                        try {
                            totalRequests.incrementAndGet();
                            testCase.execute(threadId);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(durationSeconds + 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("并发测试被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        double tps = successCount.get() / (durationMs / 1000.0);
        int avgRequestsPerThread = totalRequests.get() / threadCount;

        log.info("并发测试完成: TPS={}, avgPerThread={}, 成功率={}%",
                tps, avgRequestsPerThread, getSuccessRate(successCount.get(), errorCount.get()));

        return ConcurrencyResult.builder()
                .testName(testCase.getName())
                .threadCount(threadCount)
                .durationMs(durationMs)
                .totalRequests(totalRequests.get())
                .successCount(successCount.get())
                .errorCount(errorCount.get())
                .tps(tps)
                .avgRequestsPerThread(avgRequestsPerThread)
                .successRate(getSuccessRate(successCount.get(), errorCount.get()))
                .build();
    }

    /**
     * 执行内存测试
     *
     * @param testCase 测试用例
     * @param dataSize 数据大小
     * @return 测试结果
     */
    public MemoryResult runMemoryTest(BenchmarkTestCase testCase, long dataSize) {
        log.info("开始内存测试: {}, dataSize={}MB", testCase.getName(), dataSize / (1024 * 1024));

        Runtime runtime = Runtime.getRuntime();
        System.gc();

        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        try {
            testCase.execute(0);
        } finally {
            long endTime = System.currentTimeMillis();
            System.gc();

            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            long usedMemory = afterMemory - beforeMemory;
            long durationMs = endTime - startTime;

            log.info("内存测试完成: used={}MB, duration={}ms",
                    usedMemory / (1024 * 1024), durationMs);

            return MemoryResult.builder()
                    .testName(testCase.getName())
                    .dataSize(dataSize)
                    .usedMemory(usedMemory)
                    .durationMs(durationMs)
                    .memoryEfficiency((double) dataSize / usedMemory)
                    .build();
        }
    }

    // ==================== 私有辅助方法 ====================

    private double getAverage(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private double getPercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0.0;
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }

    private double getSuccessRate(int success, int error) {
        int total = success + error;
        if (total == 0) return 0.0;
        return (double) success / total * 100.0;
    }

    // ==================== 数据模型 ====================

    /**
     * 测试用例接口
     */
    public interface BenchmarkTestCase {
        String getName();
        void execute(int requestId) throws Exception;
    }

    /**
     * 测试选项
     */
    public static class TestOptions {
        private int threadCount = 10;
        private int totalRequests = 1000;
        private long timeoutMs = 30000;

        public TestOptions() {}

        public TestOptions(int threadCount, int totalRequests, long timeoutMs) {
            this.threadCount = threadCount;
            this.totalRequests = totalRequests;
            this.timeoutMs = timeoutMs;
        }

        public int getThreadCount() { return threadCount; }
        public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }

        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    /**
     * 吞吐率测试结果
     */
    public static class ThroughputResult {
        private String testName;
        private int totalRequests;
        private int successCount;
        private int errorCount;
        private long durationMs;
        private double tps;
        private double avgLatencyMs;
        private double p50LatencyMs;
        private double p95LatencyMs;
        private double p99LatencyMs;
        private double successRate;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ThroughputResult result = new ThroughputResult();

            public Builder testName(String name) { result.testName = name; return this; }
            public Builder totalRequests(int requests) { result.totalRequests = requests; return this; }
            public Builder successCount(int count) { result.successCount = count; return this; }
            public Builder errorCount(int count) { result.errorCount = count; return this; }
            public Builder durationMs(long ms) { result.durationMs = ms; return this; }
            public Builder tps(double tps) { result.tps = tps; return this; }
            public Builder avgLatencyMs(double ms) { result.avgLatencyMs = ms; return this; }
            public Builder p50LatencyMs(double ms) { result.p50LatencyMs = ms; return this; }
            public Builder p95LatencyMs(double ms) { result.p95LatencyMs = ms; return this; }
            public Builder p99LatencyMs(double ms) { result.p99LatencyMs = ms; return this; }
            public Builder successRate(double rate) { result.successRate = rate; return this; }

            public ThroughputResult build() { return result; }
        }
    }

    /**
     * 延迟测试结果
     */
    public static class LatencyResult {
        private String testName;
        private int totalRequests;
        private int successCount;
        private int errorCount;
        private double avgLatencyMs;
        private double minLatencyMs;
        private double maxLatencyMs;
        private double p50LatencyMs;
        private double p95LatencyMs;
        private double p99LatencyMs;
        private double successRate;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private LatencyResult result = new LatencyResult();

            public Builder testName(String name) { result.testName = name; return this; }
            public Builder totalRequests(int requests) { result.totalRequests = requests; return this; }
            public Builder successCount(int count) { result.successCount = count; return this; }
            public Builder errorCount(int count) { result.errorCount = count; return this; }
            public Builder avgLatencyMs(double ms) { result.avgLatencyMs = ms; return this; }
            public Builder minLatencyMs(double ms) { result.minLatencyMs = ms; return this; }
            public Builder maxLatencyMs(double ms) { result.maxLatencyMs = ms; return this; }
            public Builder p50LatencyMs(double ms) { result.p50LatencyMs = ms; return this; }
            public Builder p95LatencyMs(double ms) { result.p95LatencyMs = ms; return this; }
            public Builder p99LatencyMs(double ms) { result.p99LatencyMs = ms; return this; }
            public Builder successRate(double rate) { result.successRate = rate; return this; }

            public LatencyResult build() { return result; }
        }
    }

    /**
     * 并发测试结果
     */
    public static class ConcurrencyResult {
        private String testName;
        private int threadCount;
        private long durationMs;
        private int totalRequests;
        private int successCount;
        private int errorCount;
        private double tps;
        private int avgRequestsPerThread;
        private double successRate;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ConcurrencyResult result = new ConcurrencyResult();

            public Builder testName(String name) { result.testName = name; return this; }
            public Builder threadCount(int count) { result.threadCount = count; return this; }
            public Builder durationMs(long ms) { result.durationMs = ms; return this; }
            public Builder totalRequests(int requests) { result.totalRequests = requests; return this; }
            public Builder successCount(int count) { result.successCount = count; return this; }
            public Builder errorCount(int count) { result.errorCount = count; return this; }
            public Builder tps(double tps) { result.tps = tps; return this; }
            public Builder avgRequestsPerThread(int avg) { result.avgRequestsPerThread = avg; return this; }
            public Builder successRate(double rate) { result.successRate = rate; return this; }

            public ConcurrencyResult build() { return result; }
        }
    }

    /**
     * 内存测试结果
     */
    public static class MemoryResult {
        private String testName;
        private long dataSize;
        private long usedMemory;
        private long durationMs;
        private double memoryEfficiency;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private MemoryResult result = new MemoryResult();

            public Builder testName(String name) { result.testName = name; return this; }
            public Builder dataSize(long size) { result.dataSize = size; return this; }
            public Builder usedMemory(long memory) { result.usedMemory = memory; return this; }
            public Builder durationMs(long ms) { result.durationMs = ms; return this; }
            public Builder memoryEfficiency(double efficiency) { result.memoryEfficiency = efficiency; return this; }

            public MemoryResult build() { return result; }
        }
    }
}
