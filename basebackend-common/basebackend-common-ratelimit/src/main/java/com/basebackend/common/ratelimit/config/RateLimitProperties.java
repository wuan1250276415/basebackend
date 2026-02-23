package com.basebackend.common.ratelimit.config;

import com.basebackend.common.ratelimit.RateLimitAlgorithm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * 存储类型：memory / redis
     * <p>默认 memory，当 Redis 可用且配置为 redis 时自动切换</p>
     */
    private String type = "memory";

    /**
     * 默认限流算法
     */
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.SLIDING_WINDOW;

    /**
     * 最大追踪key数量，超出时清理最旧的条目（仅内存模式生效）
     */
    private int maxKeys = 10000;

    /**
     * 自动清理间隔（分钟）（仅内存模式生效）
     */
    private int cleanupIntervalMinutes = 5;
}
