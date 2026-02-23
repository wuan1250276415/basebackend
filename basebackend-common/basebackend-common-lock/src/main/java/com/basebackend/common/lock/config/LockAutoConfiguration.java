package com.basebackend.common.lock.config;

import com.basebackend.common.lock.aspect.DistributedLockAspect;
import com.basebackend.common.lock.provider.DistributedLockProvider;
import com.basebackend.common.lock.provider.impl.InMemoryDistributedLockProvider;
import com.basebackend.common.lock.provider.impl.RedisDistributedLockProvider;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式锁自动配置
 * <p>
 * 根据配置和 Redis 可用性自动选择锁实现：
 * <ul>
 *   <li>type=redis 且 RedissonClient 存在 → RedisDistributedLockProvider</li>
 *   <li>type=memory 或 RedissonClient 不存在 → InMemoryDistributedLockProvider</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(LockProperties.class)
public class LockAutoConfiguration {

    /**
     * Redis 锁实现（当 Redisson 可用且配置为 redis 类型时生效）
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "basebackend.lock", name = "type", havingValue = "redis", matchIfMissing = true)
    static class RedisLockConfiguration {

        @Bean
        @ConditionalOnMissingBean(DistributedLockProvider.class)
        public DistributedLockProvider redisDistributedLockProvider(RedissonClient redissonClient) {
            log.info("初始化 Redis 分布式锁提供者");
            return new RedisDistributedLockProvider(redissonClient);
        }
    }

    /**
     * 内存锁降级实现（当 Redis 不可用或配置为 memory 类型时生效）
     */
    @Bean
    @ConditionalOnMissingBean(DistributedLockProvider.class)
    public DistributedLockProvider inMemoryDistributedLockProvider() {
        log.info("初始化内存分布式锁提供者（单机降级模式）");
        return new InMemoryDistributedLockProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockAspect distributedLockAspect(DistributedLockProvider lockProvider,
                                                       LockProperties lockProperties) {
        return new DistributedLockAspect(lockProvider, lockProperties);
    }
}
