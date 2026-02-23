package com.basebackend.scheduler.core;

import java.time.Duration;
import java.util.Optional;

/**
 * 统一的任务处理器接口。
 * <p>
 * 实现者只需专注任务本身的处理逻辑，幂等键、重试策略与超时控制由模板与上下文提供。
 */
public interface TaskProcessor {

    /**
     * 处理器名称，用于日志与指标标识。
     *
     * @return 唯一处理器名称
     */
    String name();

    /**
     * 处理任务并返回结果。
     *
     * @param context 任务上下文（包含幂等键、标签、参数等）
     * @return 任务结果
     * @throws Exception 处理过程中发生的任何受检或非受检异常
     */
    TaskResult process(TaskContext context) throws Exception;

    /**
     * 提供重试策略，默认为不重试。
     *
     * @return 重试策略
     */
    default RetryPolicy retryPolicy() {
        // 默认最多重试两次，过滤瞬时抖动
        return RetryPolicy.fixedDelay(2, Duration.ofMillis(50));
    }

    /**
     * 为当前任务提供超时时间，默认优先使用上下文定义的超时。
     *
     * @param context 任务上下文
     * @return 超时时间，非正值表示不启用超时
     */
    default Duration timeout(TaskContext context) {
        Duration timeout = context.getTimeout();
        return timeout != null ? timeout : Duration.ZERO;
    }

    /**
     * 返回用于幂等校验的键值，默认从上下文获取。
     *
     * @param context 任务上下文
     * @return 可选的幂等键
     */
    default Optional<String> idempotentKey(TaskContext context) {
        return Optional.ofNullable(context.getIdempotentKey());
    }
}
