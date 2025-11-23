package com.basebackend.scheduler.core;

import com.basebackend.scheduler.metrics.MetricsCollector;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 重试执行模板，包装任务处理器并提供超时、重试与指标采集。
 */
public final class RetryTemplate {

    private final MetricsCollector metricsCollector;
    private final ExecutorService timeoutExecutor;

    /**
     * 无指标采集的重试模板。
     */
    public RetryTemplate() {
        this(null, null);
    }

    /**
     * 支持指标采集的重试模板。
     *
     * @param metricsCollector 指标收集器
     */
    public RetryTemplate(MetricsCollector metricsCollector) {
        this(metricsCollector, null);
    }

    public RetryTemplate(MetricsCollector metricsCollector, ExecutorService timeoutExecutor) {
        this.metricsCollector = metricsCollector;
        this.timeoutExecutor = timeoutExecutor != null ? timeoutExecutor : newTimeoutExecutor();
    }

    /**
     * 执行任务处理器并按策略自动重试。
     *
     * @param processor 处理器
     * @param context   上下文
     * @return 最终结果
     */
    public TaskResult execute(TaskProcessor processor, TaskContext context) {
        Objects.requireNonNull(processor, "processor");
        Objects.requireNonNull(context, "context");

        RetryPolicy policy = Optional.ofNullable(processor.retryPolicy()).orElse(RetryPolicy.noRetry());
        TaskContext currentContext = context.withRetryCount(context.getRetryCount());
        int currentRetry = currentContext.getRetryCount();

        while (true) {
            Instant start = Instant.now();
            try {
                TaskResult result = runWithTimeout(processor, currentContext, start);
                TaskResult enriched = enrichResult(result, currentContext, start);
                recordMetrics(processor.name(), enriched, currentRetry);

                if (shouldFinish(enriched, policy, currentRetry)) {
                    return enriched;
                }
            } catch (TimeoutException timeout) {
                TaskResult cancelled = TaskResult.builder(TaskResult.Status.CANCELLED)
                        .startTime(start)
                        .duration(Duration.between(start, Instant.now()))
                        .errorMessage("Task timed out after " + effectiveTimeout(processor, currentContext))
                        .error(timeout)
                        .idempotentKey(currentContext.getIdempotentKey())
                        .idempotentHit(currentContext.getIdempotentKey() != null)
                        .build();
                recordMetrics(processor.name(), cancelled, currentRetry);
                if (!policy.canRetry(currentRetry, cancelled, timeout)) {
                    return cancelled;
                }
            } catch (Exception ex) {
                TaskResult failed = TaskResult.builder(TaskResult.Status.FAILED)
                        .startTime(start)
                        .duration(Duration.between(start, Instant.now()))
                        .errorMessage(ex.getMessage())
                        .error(ex)
                        .idempotentKey(currentContext.getIdempotentKey())
                        .idempotentHit(currentContext.getIdempotentKey() != null)
                        .build();
                recordMetrics(processor.name(), failed, currentRetry);
                if (!policy.canRetry(currentRetry, failed, ex)) {
                    return failed;
                }
            }

            currentRetry++;
            currentContext = currentContext.withRetryCount(currentRetry);
            Duration delay = safeDelay(policy.nextDelay(currentRetry));
            sleep(delay);
        }
    }

    private TaskResult runWithTimeout(TaskProcessor processor, TaskContext context, Instant start) throws Exception {
        Duration timeout = effectiveTimeout(processor, context);
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return processor.process(context);
        }
        Callable<TaskResult> callable = () -> processor.process(context);
        Future<TaskResult> future = timeoutExecutor.submit(callable);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutEx) {
            future.cancel(true);
            throw timeoutEx;
        } catch (Exception ex) {
            future.cancel(true);
            throw ex;
        }
    }

    private Duration effectiveTimeout(TaskProcessor processor, TaskContext context) {
        Duration contextTimeout = context.getTimeout();
        if (contextTimeout != null) {
            return contextTimeout;
        }
        Duration processorTimeout = processor.timeout(context);
        return processorTimeout != null ? processorTimeout : Duration.ZERO;
    }

    private TaskResult enrichResult(TaskResult result, TaskContext context, Instant start) {
        if (result == null) {
            return TaskResult.builder(TaskResult.Status.FAILED)
                    .startTime(start)
                    .duration(Duration.ZERO)
                    .errorMessage("Task returned null result")
                    .idempotentKey(context.getIdempotentKey())
                    .idempotentHit(context.getIdempotentKey() != null)
                    .build();
        }
        Instant startTime = result.getStartTime() != null ? result.getStartTime() : start;
        Duration duration = result.getDuration() != null ? result.getDuration() : Duration.between(startTime, Instant.now());
        String idempotentKey = result.getIdempotentKey() != null ? result.getIdempotentKey() : context.getIdempotentKey();
        return TaskResult.builder(result.getStatus())
                .startTime(startTime)
                .duration(duration)
                .errorMessage(result.getErrorMessage())
                .error(result.getError())
                .output(result.getOutput())
                .idempotentKey(idempotentKey)
                .idempotentHit(result.isIdempotentHit() || idempotentKey != null)
                .build();
    }

    private boolean shouldFinish(TaskResult result, RetryPolicy policy, int retryCount) {
        if (result.getStatus() == TaskResult.Status.SUCCESS || result.getStatus() == TaskResult.Status.CANCELLED) {
            return true;
        }
        return !policy.canRetry(retryCount, result, null);
    }

    private Duration safeDelay(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }

    private void sleep(Duration delay) {
        if (delay.isZero()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void recordMetrics(String processorName, TaskResult result, int retryCount) {
        if (metricsCollector == null) {
            return;
        }
        metricsCollector.recordExecution(processorName);
        metricsCollector.recordResult(processorName, result);
        if (result.getDuration() != null) {
            metricsCollector.recordLatency(processorName, result.getDuration());
        }
        metricsCollector.recordRetries(processorName, retryCount);
    }

    @PreDestroy
    public void shutdown() {
        timeoutExecutor.shutdownNow();
    }

    private static ExecutorService newTimeoutExecutor() {
        return Executors.newCachedThreadPool();
    }
}
