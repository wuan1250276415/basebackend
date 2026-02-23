package com.basebackend.scheduler.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RetryTemplate 单元测试。
 * 覆盖重试逻辑、超时处理、指标采集等核心功能。
 */
@DisplayName("RetryTemplate 单元测试")
class RetryTemplateTest {

    private RetryTemplate retryTemplate;
    private TestMetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        metricsCollector = new TestMetricsCollector();
        retryTemplate = new RetryTemplate(metricsCollector);
    }

    @Test
    @DisplayName("成功执行任务 - 无需重试")
    void testSuccessfulExecution() {
        // 准备
        TaskProcessor processor = new SuccessProcessor();
        TaskContext context = TaskContext.builder("test-task").build();

        // 执行
        TaskResult result = retryTemplate.execute(processor, context);

        // 验证
        assertNotNull(result);
        assertEquals(TaskResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, metricsCollector.getExecutionCount());
    }

    @Test
    @DisplayName("失败后重试成功")
    void testRetryOnFailure() {
        // 准备：前2次失败，第3次成功
        AtomicInteger attempts = new AtomicInteger(0);
        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "retry-test";
            }

            @Override
            public TaskResult process(TaskContext context) {
                int attempt = attempts.incrementAndGet();
                if (attempt < 3) {
                    return TaskResult.builder(TaskResult.Status.FAILED)
                            .errorMessage("Attempt " + attempt + " failed")
                            .build();
                }
                return TaskResult.builder(TaskResult.Status.SUCCESS).build();
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
            }
        };
        TaskContext context = TaskContext.builder("test-task").build();

        // 执行
        TaskResult result = retryTemplate.execute(processor, context);

        // 验证
        assertEquals(TaskResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("达到最大重试次数后失败")
    void testMaxRetriesExceeded() {
        // 准备：始终失败
        AtomicInteger attempts = new AtomicInteger(0);
        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "always-fail";
            }

            @Override
            public TaskResult process(TaskContext context) {
                attempts.incrementAndGet();
                return TaskResult.builder(TaskResult.Status.FAILED)
                        .errorMessage("Always fails")
                        .build();
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.fixedDelay(2, Duration.ofMillis(10));
            }
        };
        TaskContext context = TaskContext.builder("test-task").build();

        // 执行
        TaskResult result = retryTemplate.execute(processor, context);

        // 验证
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(3, attempts.get()); // 初始1次 + 重试2次
    }

    @Test
    @DisplayName("无重试策略 - 失败后立即返回")
    void testNoRetryPolicy() {
        // 准备
        AtomicInteger attempts = new AtomicInteger(0);
        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "no-retry";
            }

            @Override
            public TaskResult process(TaskContext context) {
                attempts.incrementAndGet();
                return TaskResult.builder(TaskResult.Status.FAILED)
                        .errorMessage("Failed")
                        .build();
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.noRetry();
            }
        };
        TaskContext context = TaskContext.builder("test-task").build();

        // 执行
        TaskResult result = retryTemplate.execute(processor, context);

        // 验证
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, attempts.get()); // 只执行1次
    }

    @Test
    @DisplayName("任务超时处理")
    @Timeout(5)
    void testTaskTimeout() {
        // 准备：任务执行时间超过超时时间
        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "slow-task";
            }

            @Override
            public TaskResult process(TaskContext context) {
                try {
                    Thread.sleep(5000); // 5秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return TaskResult.builder(TaskResult.Status.SUCCESS).build();
            }

            @Override
            public Duration timeout(TaskContext context) {
                return Duration.ofMillis(100); // 100ms超时
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.noRetry();
            }
        };
        TaskContext context = TaskContext.builder("test-task").build();

        // 执行
        TaskResult result = retryTemplate.execute(processor, context);

        // 验证
        assertEquals(TaskResult.Status.CANCELLED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("timed out"));
    }

    @Test
    @DisplayName("异常处理 - 抛出异常时返回失败结果")
    void testExceptionHandling() {
        // 准备
        TaskProcessor processor = new TaskProcessor() {
            @Override
            public String name() {
                return "exception-task";
            }

            @Override
            public TaskResult process(TaskContext context) {
                throw new RuntimeException("Unexpected error");
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.noRetry();
            }
        };
        TaskContext context = TaskContext.builder("test-task").build();

        // 执行
        TaskResult result = retryTemplate.execute(processor, context);

        // 验证
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("Unexpected error", result.getErrorMessage());
    }

    @Test
    @DisplayName("指标采集验证")
    void testMetricsCollection() {
        // 准备
        TaskProcessor processor = new SuccessProcessor();
        TaskContext context = TaskContext.builder("test-task").build();

        // 执行
        retryTemplate.execute(processor, context);

        // 验证
        assertEquals(1, metricsCollector.getExecutionCount());
        assertEquals(1, metricsCollector.getResultCount());
        assertTrue(metricsCollector.getLatencyCount() > 0);
    }

    // ========== 辅助类 ==========

    /**
     * 成功处理器
     */
    private static class SuccessProcessor implements TaskProcessor {
        @Override
        public String name() {
            return "success-processor";
        }

        @Override
        public TaskResult process(TaskContext context) {
            return TaskResult.builder(TaskResult.Status.SUCCESS).build();
        }
    }

    /**
     * 测试用指标收集器
     */
    private static class TestMetricsCollector implements MetricsCollector {
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private final AtomicInteger resultCount = new AtomicInteger(0);
        private final AtomicInteger latencyCount = new AtomicInteger(0);
        private final AtomicInteger retryCount = new AtomicInteger(0);

        @Override
        public void recordExecution(String processorName) {
            executionCount.incrementAndGet();
        }

        @Override
        public void recordResult(String processorName, TaskResult result) {
            resultCount.incrementAndGet();
        }

        @Override
        public void recordLatency(String processorName, Duration latency) {
            latencyCount.incrementAndGet();
        }

        @Override
        public void recordRetries(String processorName, int retries) {
            retryCount.addAndGet(retries);
        }

        public int getExecutionCount() {
            return executionCount.get();
        }

        public int getResultCount() {
            return resultCount.get();
        }

        public int getLatencyCount() {
            return latencyCount.get();
        }

        public int getRetryCount() {
            return retryCount.get();
        }
    }
}
