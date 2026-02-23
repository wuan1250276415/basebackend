package com.basebackend.common.idempotent.config;

import com.basebackend.common.idempotent.aspect.IdempotentAspect;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.idempotent.store.impl.InMemoryIdempotentStore;
import com.basebackend.common.idempotent.store.impl.RedisIdempotentStore;
import com.basebackend.common.idempotent.token.IdempotentTokenController;
import com.basebackend.common.idempotent.token.IdempotentTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 幂等性自动配置
 * <p>
 * 根据 Redis 可用性自动选择存储实现：
 * <ul>
 *   <li>StringRedisTemplate 存在 → RedisIdempotentStore + IdempotentTokenService</li>
 *   <li>StringRedisTemplate 不存在 → InMemoryIdempotentStore</li>
 * </ul>
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(IdempotentProperties.class)
@ConditionalOnProperty(prefix = "basebackend.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {

    /**
     * Redis 存储实现（当 StringRedisTemplate 可用时生效）
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnBean(StringRedisTemplate.class)
    static class RedisIdempotentConfiguration {

        @Bean
        @ConditionalOnMissingBean(IdempotentStore.class)
        public IdempotentStore redisIdempotentStore(StringRedisTemplate redisTemplate) {
            log.info("初始化 Redis 幂等存储");
            return new RedisIdempotentStore(redisTemplate);
        }

        @Bean
        @ConditionalOnMissingBean
        public IdempotentTokenService idempotentTokenService(StringRedisTemplate redisTemplate,
                                                              IdempotentProperties properties) {
            log.info("初始化幂等 Token 服务");
            return new IdempotentTokenService(redisTemplate, properties);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(IdempotentTokenService.class)
        public IdempotentTokenController idempotentTokenController(IdempotentTokenService tokenService) {
            return new IdempotentTokenController(tokenService);
        }
    }

    /**
     * 内存存储降级实现（当 Redis 不可用时生效）
     */
    @Bean
    @ConditionalOnMissingBean(IdempotentStore.class)
    public IdempotentStore inMemoryIdempotentStore() {
        log.info("初始化内存幂等存储（单机降级模式）");
        return new InMemoryIdempotentStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(IdempotentStore idempotentStore,
                                              IdempotentProperties properties,
                                              IdempotentTokenService idempotentTokenService) {
        return new IdempotentAspect(idempotentStore, properties, idempotentTokenService);
    }
}
