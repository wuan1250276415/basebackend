package com.basebackend.scheduler.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 重试机制性能基准测试。
 *
 * <p>测试包括：
 * - 重试延迟准确性
 * - 并发重试性能
 * - 不同重试策略性能
 */
@Slf4j
class RetryMechanismPerformanceTest {

    /**
     * 测试固定延迟重试的性能。
     */
    @Test
    void testFixedDelayRetryPerformance() {
        log.info("开始固定延迟重试性能测试");

        AtomicInteger attemptCount = new AtomicInteger(0);
        long startTime = System.nanoTime();

        // 创建处理器：第1次失败，第2次成功
        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor-fixed";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                Instant startTime = Instant.now();
                int retryCount = context.getRetryCount();
                attemptCount.incrementAndGet();

                log.debug("第{}次尝试", retryCount + 1);

                // 第1次失败，第2次成功
                if (retryCount == 0) {
                    return TaskResult.builder(TaskResult.Status.FAILED)
                            .startTime(startTime)
                            .duration(Duration.between(startTime, Instant.now()))
                            .errorMessage("First attempt failed")
                            .build();
                } else {
                    return TaskResult.builder(TaskResult.Status.SUCCESS)
                            .startTime(startTime)
                            .duration(Duration.between(startTime, Instant.now()))
                            .output(java.util.Map.of("attemptCount", retryCount + 1))
                            .build();
                }
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            }
        };

        RetryTemplate retryTemplate = new RetryTemplate();
        TaskContext context = TaskContext.builder("performance-test").build();
        TaskResult result = retryTemplate.execute(processor, context);

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        log.info("固定延迟重试完成：耗时 {:.2f}ms，尝试次数：{}",
            durationMs, attemptCount.get());

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(2, attemptCount.get(), "应该尝试2次");

        // 性能断言：1次重试应该耗时约10ms
        assertTrue(durationMs >= 10, "固定延迟重试耗时过短：%.2fms".formatted(durationMs));
        assertTrue(durationMs < 50, "固定延迟重试耗时过长：%.2fms".formatted(durationMs));

        log.info("✅ 固定延迟重试性能测试通过");
    }

    /**
     * 测试指数退避重试的性能。
     */
    @Test
    void testExponentialBackoffRetryPerformance() {
        log.info("开始指数退避重试性能测试");

        AtomicInteger attemptCount = new AtomicInteger(0);
        long startTime = System.nanoTime();

        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor-exp";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                Instant startTime = Instant.now();
                int retryCount = context.getRetryCount();
                attemptCount.incrementAndGet();

                log.debug("第{}次尝试", retryCount + 1);

                // 第1次失败，第2次成功
                if (retryCount == 0) {
                    return TaskResult.builder(TaskResult.Status.FAILED)
                            .startTime(startTime)
                            .duration(Duration.between(startTime, Instant.now()))
                            .errorMessage("First attempt failed")
                            .build();
                } else {
                    return TaskResult.builder(TaskResult.Status.SUCCESS)
                            .startTime(startTime)
                            .duration(Duration.between(startTime, Instant.now()))
                            .output(java.util.Map.of("attemptCount", retryCount + 1))
                            .build();
                }
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.exponentialBackoff(3, Duration.ofMillis(10), Duration.ofSeconds(1), ex -> true);
            }
        };

        RetryTemplate retryTemplate = new RetryTemplate();
        TaskContext context = TaskContext.builder("performance-test-exp").build();
        TaskResult result = retryTemplate.execute(processor, context);

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        log.info("指数退避重试完成：耗时 {:.2f}ms，尝试次数：{}",
            durationMs, attemptCount.get());

        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(2, attemptCount.get(), "应该尝试2次");

        // 性能断言：指数退避总延迟约 10ms
        assertTrue(durationMs >= 10, "指数退避重试耗时过短：%.2fms".formatted(durationMs));
        assertTrue(durationMs < 50, "指数退避重试耗时过长：%.2fms".formatted(durationMs));

        log.info("✅ 指数退避重试性能测试通过");
    }

    /**
     * 测试快速失败重试的性能。
     */
    @Test
    void testNoRetryPerformance() {
        log.info("开始快速失败重试性能测试");

        AtomicInteger attemptCount = new AtomicInteger(0);
        long startTime = System.nanoTime();

        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor-no-retry";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                Instant startTime = Instant.now();
                attemptCount.incrementAndGet();

                return TaskResult.builder(TaskResult.Status.FAILED)
                        .startTime(startTime)
                        .duration(Duration.between(startTime, Instant.now()))
                        .errorMessage("Task failed")
                        .build();
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.noRetry();
            }
        };

        RetryTemplate retryTemplate = new RetryTemplate();
        TaskContext context = TaskContext.builder("performance-test-no-retry").build();
        TaskResult result = retryTemplate.execute(processor, context);

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;

        log.info("快速失败重试完成：耗时 {:.2f}ms，尝试次数：{}", durationMs, attemptCount.get());

        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals(1, attemptCount.get(), "只应该尝试1次");

        // 性能断言：快速失败应该几乎无延迟
        assertTrue(durationMs < 5, "快速失败耗时过长：%.2fms".formatted(durationMs));

        log.info("✅ 快速失败重试性能测试通过");
    }

    /**
     * 测试并发重试性能。
     */
    @Test
    void testConcurrentRetryPerformance() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long testStartTime = System.nanoTime();

        log.info("开始并发重试性能测试：{}个线程", threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    AtomicInteger attemptCount = new AtomicInteger(0);
                    long startTime = System.nanoTime();

                    TaskProcessor processor = new TaskProcessor() {
                        @Override
                        public String name() {
                            return "test-processor-concurrent-" + taskId;
                        }

                        @Override
                        public TaskResult process(TaskContext context) throws Exception {
                            Instant startTime = Instant.now();
                            int retryCount = context.getRetryCount();
                            attemptCount.incrementAndGet();

                            // 偶数任务成功，奇数任务失败
                            if (taskId % 2 == 0) {
                                // 第1次失败，第2次成功
                                if (retryCount == 0) {
                                    return TaskResult.builder(TaskResult.Status.FAILED)
                                            .startTime(startTime)
                                            .duration(Duration.between(startTime, Instant.now()))
                                            .errorMessage("First attempt failed")
                                            .build();
                                } else {
                                    return TaskResult.builder(TaskResult.Status.SUCCESS)
                                            .startTime(startTime)
                                            .duration(Duration.between(startTime, Instant.now()))
                                            .build();
                                }
                            } else {
                                return TaskResult.builder(TaskResult.Status.FAILED)
                                        .startTime(startTime)
                                        .duration(Duration.between(startTime, Instant.now()))
                                        .errorMessage("Task failed")
                                        .build();
                            }
                        }

                        @Override
                        public RetryPolicy retryPolicy() {
                            return RetryPolicy.fixedDelay(2, Duration.ofMillis(5));
                        }
                    };

                    RetryTemplate retryTemplate = new RetryTemplate();
                    TaskContext context = TaskContext.builder("concurrent-test-" + taskId).build();
                    TaskResult result = retryTemplate.execute(processor, context);
                    long endTime = System.nanoTime();

                    double durationMs = (endTime - startTime) / 1_000_000.0;

                    synchronized (this) {
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                        log.debug("线程-{}完成：耗时 {:.2f}ms，尝试次数：{}", taskId, durationMs, attemptCount.get());
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("线程-{}执行失败", taskId, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long testEndTime = System.nanoTime();
        double totalDurationMs = (testEndTime - testStartTime) / 1_000_000.0;

        log.info("并发重试测试完成：");
        log.info("  总耗时：{:.2f}ms", totalDurationMs);
        log.info("  成功：{}，失败：{}", successCount.get(), failureCount.get());
        log.info("  平均耗时：{:.2f}ms", totalDurationMs / threadCount);

        // 验证结果
        assertEquals(threadCount / 2, successCount.get(), "期望50%成功率");
        assertEquals(threadCount / 2, failureCount.get(), "期望50%失败率");

        // 性能断言：并发执行应该稳定
        assertTrue(totalDurationMs < 1000,
            "并发重试总耗时过长：%.2fms".formatted(totalDurationMs));

        log.info("✅ 并发重试性能测试通过");
    }

    /**
     * 测试重试策略切换性能。
     */
    @Test
    void testRetryStrategySwitchPerformance() {
        int iterationCount = 1000;
        AtomicInteger attemptCount = new AtomicInteger(0);

        log.info("开始重试策略切换性能测试：{}次迭代", iterationCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < iterationCount; i++) {
            final int currentIndex = i;  // 创建final副本
            TaskProcessor processor = new TaskProcessor() {
                @Override
                public String name() {
                    return "test-processor-switch-" + currentIndex;
                }

                @Override
                public TaskResult process(TaskContext context) throws Exception {
                    Instant startTime = Instant.now();
                    attemptCount.incrementAndGet();

                    return TaskResult.builder(TaskResult.Status.SUCCESS)
                            .startTime(startTime)
                            .duration(Duration.between(startTime, Instant.now()))
                            .build();
                }

                @Override
                public RetryPolicy retryPolicy() {
                    // 每次使用不同的重试策略
                    int mod = currentIndex % 3;
                    if (mod == 0) {
                        return RetryPolicy.noRetry();
                    } else if (mod == 1) {
                        return RetryPolicy.fixedDelay(2, Duration.ofMillis(1));
                    } else {
                        return RetryPolicy.exponentialBackoff(2, Duration.ofMillis(1), Duration.ofMillis(10), ex -> true);
                    }
                }
            };

            RetryTemplate retryTemplate = new RetryTemplate();
            TaskContext context = TaskContext.builder("test-task-" + i).build();

            retryTemplate.execute(processor, context);
        }

        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        double avgDurationMs = durationMs / iterationCount;

        log.info("重试策略切换完成：");
        log.info("  总耗时：{:.2f}ms", durationMs);
        log.info("  平均耗时：{:.4f}ms/次", avgDurationMs);
        log.info("  尝试次数：{}", attemptCount.get());

        // 性能断言：策略切换应该非常快
        assertTrue(avgDurationMs < 1.0,
            "平均策略切换耗时过长：%.4fms".formatted(avgDurationMs));

        log.info("✅ 重试策略切换性能测试通过");
    }

    /**
     * 测试重试延迟准确性。
     */
    @Test
    void testRetryDelayAccuracy() {
        log.info("开始重试延迟准确性测试");

        AtomicInteger attemptCount = new AtomicInteger(0);
        long[] attemptTimes = new long[6]; // 记录每次尝试的时间

        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor-delay";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                Instant startTime = Instant.now();
                int retryCount = context.getRetryCount();
                int attempt = attemptCount.incrementAndGet();
                attemptTimes[attempt] = System.nanoTime();

                log.debug("第{}次尝试", attempt);

                // 第4次成功
                if (attempt > 3) {
                    return TaskResult.builder(TaskResult.Status.SUCCESS)
                            .startTime(startTime)
                            .duration(Duration.between(startTime, Instant.now()))
                            .build();
                } else {
                    return TaskResult.builder(TaskResult.Status.FAILED)
                            .startTime(startTime)
                            .duration(Duration.between(startTime, Instant.now()))
                            .errorMessage("Attempt " + attempt + " failed")
                            .build();
                }
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.fixedDelay(5, Duration.ofMillis(100));
            }
        };

        RetryTemplate retryTemplate = new RetryTemplate();
        TaskContext context = TaskContext.builder("delay-test").build();

        long startTime = System.nanoTime();
        TaskResult result = retryTemplate.execute(processor, context);
        long endTime = System.nanoTime();

        double totalDurationMs = (endTime - startTime) / 1_000_000.0;

        log.info("重试延迟测试完成：");
        for (int i = 1; i <= 4; i++) {
            if (i > 1) {
                double delayMs = (attemptTimes[i] - attemptTimes[i - 1]) / 1_000_000.0;
                log.info("  第{}次到第{}次延迟：{:.2f}ms", i - 1, i, delayMs);
            }
        }

        // 验证延迟准确性：每次重试应该延迟约100ms（允许±20ms误差）
        for (int i = 2; i <= 4; i++) {
            double delayMs = (attemptTimes[i] - attemptTimes[i - 1]) / 1_000_000.0;
            assertTrue(delayMs >= 80 && delayMs <= 130,
                "第{}次重试延迟不准确：{:.2f}ms，期望100ms".formatted(i - 1, delayMs));
        }

        log.info("✅ 重试延迟准确性测试通过");
    }
}
