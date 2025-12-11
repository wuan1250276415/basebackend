package com.basebackend.scheduler.core;

/**
 * 指标收集器接口。
 * 在core模块中定义，metrics模块提供实现。
 */
public interface MetricsCollector {

    /**
     * 记录执行次数
     */
    default void recordExecution(String processorName) {}

    /**
     * 记录执行结果
     */
    default void recordResult(String processorName, TaskResult result) {}

    /**
     * 记录延迟
     */
    default void recordLatency(String processorName, java.time.Duration duration) {}

    /**
     * 记录重试次数
     */
    default void recordRetries(String processorName, int retryCount) {}
}
