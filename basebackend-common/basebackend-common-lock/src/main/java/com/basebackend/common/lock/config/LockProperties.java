package com.basebackend.common.lock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁配置属性
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "basebackend.lock")
public class LockProperties {

    /**
     * 锁实现类型: redis / memory
     * <p>
     * 默认 redis，当 Redis 不可用时自动降级为 memory
     * </p>
     */
    private String type = "redis";

    /**
     * 全局 key 前缀
     */
    private String keyPrefix = "";
}
