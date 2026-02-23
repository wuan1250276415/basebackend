package com.basebackend.common.ratelimit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("basebackend.common.ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * 最大追踪key数量，超出时清理最旧的条目
     */
    private int maxKeys = 10000;

    /**
     * 自动清理间隔（分钟）
     */
    private int cleanupIntervalMinutes = 5;
}
