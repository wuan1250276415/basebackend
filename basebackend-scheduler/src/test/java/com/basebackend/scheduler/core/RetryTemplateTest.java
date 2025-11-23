package com.basebackend.scheduler.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 重试模板测试。
 */
class RetryTemplateTest {

    @Test
    void testSuccessfulExecution() {
        RetryTemplate retryTemplate = new RetryTemplate();

        AtomicInteger attemptCounter = new AtomicInteger(0);
        TaskProcessor mockProcessor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                attemptCounter.incrementAndGet();
                return TaskResult.builder(TaskResult.Status.SUCCESS)
                        .output(java.util.Collections.singletonMap("result", "success"))
                        .build();
            }
        };

        TaskResult result = retryTemplate.execute(mockProcessor, TaskContext.builder("test").build());

        assertTrue(result.isSuccess());
        assertEquals(1, attemptCounter.get());  // 只执行一次
    }

    @Test
    void testFixedDelayRetry() {
        AtomicInteger attemptCounter = new AtomicInteger(0);
        RetryPolicy retryPolicy = RetryPolicy.fixedDelay(3, Duration.ofMillis(10));
        TaskProcessor mockProcessor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                attemptCounter.incrementAndGet();
                throw new RuntimeException("test error");
            }

            @Override
            public RetryPolicy retryPolicy() {
                return retryPolicy;
            }
        };

        RetryTemplate retryTemplate = new RetryTemplate();
        TaskResult result = retryTemplate.execute(mockProcessor, TaskContext.builder("test").build());

        // FixedDelay(3) 表示最多重试3次，初始执行1次 + 重试3次 = 总共4次
        assertFalse(result.isSuccess());
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(4, attemptCounter.get());  // 1次初始 + 3次重试
    }

    @Test
    void testNoRetry() {
        AtomicInteger attemptCounter = new AtomicInteger(0);
        TaskProcessor mockProcessor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                attemptCounter.incrementAndGet();
                throw new RuntimeException("test error");
            }

            @Override
            public RetryPolicy retryPolicy() {
                return RetryPolicy.noRetry();
            }
        };

        RetryTemplate retryTemplate = new RetryTemplate();
        TaskResult result = retryTemplate.execute(mockProcessor, TaskContext.builder("test").build());

        // RetryTemplate捕获异常并返回FAILED结果，不重试
        assertFalse(result.isSuccess());
        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(1, attemptCounter.get());  // 只执行一次，不重试
    }

    @Test
    void testSuccessfulAfterRetries() {
        AtomicInteger attemptCounter = new AtomicInteger(0);
        TaskProcessor mockProcessor = new TaskProcessor() {
            @Override
            public String name() {
                return "test-processor";
            }

            @Override
            public TaskResult process(TaskContext context) throws Exception {
                attemptCounter.incrementAndGet();
                if (attemptCounter.get() < 3) {
                    throw new RuntimeException("temporary error");
                }
                return TaskResult.builder(TaskResult.Status.SUCCESS)
                        .output(java.util.Collections.singletonMap("result", "success"))
                        .build();
            }
        };

        RetryTemplate retryTemplate = new RetryTemplate();
        TaskResult result = retryTemplate.execute(mockProcessor, TaskContext.builder("test").build());

        assertTrue(result.isSuccess());
        assertEquals(3, attemptCounter.get());  // 失败2次 + 第3次成功
    }
}
