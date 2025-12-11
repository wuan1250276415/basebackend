package com.basebackend.file.config;

import com.basebackend.file.limit.RateLimiter;
import com.basebackend.file.limit.SimpleRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流器配置
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 创建简单限流器 Bean
     *
     * <strong>注意</strong>：当前为内存版实现，适用于单机部署
     * 生产环境建议使用 Redis 分布式限流器
     *
     * @return 限流器实例
     */
    @Bean
    public RateLimiter rateLimiter() {
        return new SimpleRateLimiter();
    }
}
