package com.basebackend.scheduler.retry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重试策略配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicy {

    /**
     * 最大重试次数
     */
    @Builder.Default
    private Integer maxRetryTimes = 3;

    /**
     * 重试间隔(秒)
     */
    @Builder.Default
    private Integer retryInterval = 60;

    /**
     * 是否启用指数退避
     */
    @Builder.Default
    private Boolean exponentialBackoff = true;

    /**
     * 退避倍数
     */
    @Builder.Default
    private Double backoffMultiplier = 2.0;

    /**
     * 最大退避时间(秒)
     */
    @Builder.Default
    private Integer maxBackoffInterval = 3600;

    /**
     * 计算重试延迟时间
     *
     * @param retryCount 当前重试次数(从0开始)
     * @return 延迟秒数
     */
    public long calculateDelay(int retryCount) {
        if (!exponentialBackoff) {
            return retryInterval;
        }

        // 指数退避: delay = retryInterval * (backoffMultiplier ^ retryCount)
        long delay = (long) (retryInterval * Math.pow(backoffMultiplier, retryCount));
        return Math.min(delay, maxBackoffInterval);
    }

    /**
     * 是否还可以重试
     *
     * @param currentRetryTimes 当前已重试次数
     * @return true表示可以继续重试
     */
    public boolean canRetry(int currentRetryTimes) {
        return currentRetryTimes < maxRetryTimes;
    }
}
