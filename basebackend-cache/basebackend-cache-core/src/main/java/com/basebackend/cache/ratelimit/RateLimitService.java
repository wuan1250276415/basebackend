package com.basebackend.cache.ratelimit;

import org.redisson.api.RateType;

import java.util.concurrent.TimeUnit;

/**
 * 分布式限流服务接口
 * 提供基于 Redisson RRateLimiter 的编程式限流能力
 */
public interface RateLimitService {

    /**
     * 尝试获取一个令牌
     *
     * @param key      限流键
     * @param rate     每个时间窗口允许的请求数
     * @param interval 时间窗口大小
     * @param unit     时间窗口单位
     * @param mode     限流模式 (OVERALL / PER_CLIENT)
     * @return true 如果获取成功
     */
    boolean tryAcquire(String key, long rate, long interval, TimeUnit unit, RateType mode);

    /**
     * 尝试获取多个令牌
     *
     * @param key     限流键
     * @param permits 请求的令牌数
     * @param rate    每个时间窗口允许的请求数
     * @param interval 时间窗口大小
     * @param unit    时间窗口单位
     * @param mode    限流模式 (OVERALL / PER_CLIENT)
     * @return true 如果获取成功
     */
    boolean tryAcquire(String key, long permits, long rate, long interval, TimeUnit unit, RateType mode);

    /**
     * 查询指定限流器的可用令牌数
     *
     * @param key 限流键
     * @return 可用令牌数
     */
    long availablePermits(String key);

    /**
     * 删除限流器（释放 Redis 资源）
     *
     * @param key 限流键
     */
    void deleteRateLimiter(String key);
}
