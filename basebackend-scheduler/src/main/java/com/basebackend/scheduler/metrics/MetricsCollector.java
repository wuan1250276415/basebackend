package com.basebackend.scheduler.metrics;

import com.basebackend.scheduler.core.TaskResult;

import java.time.Duration;

/**
 * 统一指标收集接口，便于与 Micrometer 等监控系统集成。
 */
public interface MetricsCollector {

    /**
     * 记录一次任务执行请求。
     *
     * @param processorName 处理器名称
     */
    void recordExecution(String processorName);

    /**
     * 记录任务结果，用于成功率/失败率等统计。
     *
     * @param processorName 处理器名称
     * @param result        任务结果
     */
    void recordResult(String processorName, TaskResult result);

    /**
     * 记录耗时数据，便于计算 P50/P95/P99。
     *
     * @param processorName 处理器名称
     * @param duration      耗时
     */
    void recordLatency(String processorName, Duration duration);

    /**
     * 记录重试次数。
     *
     * @param processorName 处理器名称
     * @param retries       重试次数
     */
    void recordRetries(String processorName, int retries);

    /**
     * 与 Micrometer 注册表绑定的可选扩展。
     * 实现方可在存在 Micrometer 依赖时覆盖该方法。
     *
     * @param micrometerRegistry 预期为 io.micrometer.core.instrument.MeterRegistry
     */
    default void bindTo(Object micrometerRegistry) {
        // no-op 默认实现，避免强依赖 Micrometer
    }
}
