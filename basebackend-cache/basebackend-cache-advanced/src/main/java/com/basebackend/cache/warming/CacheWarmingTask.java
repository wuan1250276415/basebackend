package com.basebackend.cache.warming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 缓存预热任务数据模型
 * 定义预热任务的配置和数据加载逻辑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheWarmingTask {

    /**
     * 任务名称（唯一标识）
     */
    private String name;

    /**
     * 任务优先级（数字越小优先级越高）
     */
    private int priority;

    /**
     * 数据加载器
     * 返回需要预热的键值对
     */
    private Supplier<Map<String, Object>> dataLoader;

    /**
     * 缓存 TTL
     */
    private Duration ttl;

    /**
     * 是否异步执行
     */
    private boolean async;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务状态
     */
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 预热数据条目数
     */
    private int itemCount;

    /**
     * 已加载条目数
     */
    private int loadedCount;

    /**
     * 失败条目数
     */
    private int failedCount;

    /**
     * 开始时间（毫秒）
     */
    private long startTime;

    /**
     * 结束时间（毫秒）
     */
    private long endTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /**
         * 等待执行
         */
        PENDING,

        /**
         * 执行中
         */
        RUNNING,

        /**
         * 执行成功
         */
        SUCCESS,

        /**
         * 执行失败
         */
        FAILED,

        /**
         * 部分成功
         */
        PARTIAL_SUCCESS
    }

    /**
     * 获取执行耗时（毫秒）
     */
    public long getExecutionTime() {
        if (startTime > 0 && endTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (itemCount > 0) {
            return (double) loadedCount / itemCount;
        }
        return 0.0;
    }

    /**
     * 判断任务是否完成
     */
    public boolean isCompleted() {
        return status == TaskStatus.SUCCESS 
            || status == TaskStatus.FAILED 
            || status == TaskStatus.PARTIAL_SUCCESS;
    }

    /**
     * 判断任务是否成功
     */
    public boolean isSuccess() {
        return status == TaskStatus.SUCCESS;
    }
}
