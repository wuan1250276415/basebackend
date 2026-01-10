package com.basebackend.logging.cache;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 热点日志缓存自动配置
 *
 * 当RedisTemplate类存在且配置了basebackend.logging.hot-cache.enabled=true时生效。
 * 自动装配所有必需的Bean，包括：
 * - FastJson2RedisSerializer：序列化器
 * - hotLogRedisTemplate：专门的RedisTemplate
 * - LocalLruCache：本地缓存
 * - HotLogCacheMetrics：监控指标
 * - RedisHotLogCache：Redis缓存
 * - HotLogCacheAspect：AOP切面
 * - 预热任务：启动时加载预热键
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
@EnableConfigurationProperties(HotLogCacheProperties.class)
@ConditionalOnProperty(prefix = "basebackend.logging.hot-cache", name = "enabled", havingValue = "true", matchIfMissing = true // 默认启用
)
public class HotLogCacheConfiguration {

    /**
     * 配置FastJson2序列化器
     *
     * @return 序列化器实例
     */
    @Bean
    public RedisSerializer<Object> fastJson2RedisSerializer() {
        return new FastJson2RedisSerializer();
    }

    /**
     * 配置专门的RedisTemplate用于热点日志缓存
     * 使用独立的RedisTemplate避免与其他缓存冲突
     *
     * @param connectionFactory        Redis连接工厂
     * @param fastJson2RedisSerializer 序列化器
     * @return RedisTemplate实例
     */
    @Bean(name = "hotLogRedisTemplate")
    @Primary
    public RedisTemplate<String, Object> hotLogRedisTemplate(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<Object> fastJson2RedisSerializer) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置默认序列化器
        template.setDefaultSerializer(fastJson2RedisSerializer);

        // 设置键的序列化器（字符串）
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        // 设置值的序列化器（JSON）
        template.setValueSerializer(fastJson2RedisSerializer);
        template.setHashValueSerializer(fastJson2RedisSerializer);

        // 启用事务支持
        template.setEnableTransactionSupport(true);

        // 初始化
        template.afterPropertiesSet();

        return template;
    }

    /**
     * 配置本地LRU缓存
     *
     * @param properties 配置属性
     * @return LocalLruCache实例
     */
    @Bean
    public LocalLruCache<String, Object> hotLogLocalCache(HotLogCacheProperties properties) {
        return new LocalLruCache<>(properties.getLocalMaxEntries());
    }

    /**
     * 配置缓存监控指标
     *
     * @return HotLogCacheMetrics实例
     */
    @Bean
    public HotLogCacheMetrics hotLogCacheMetrics() {
        return new HotLogCacheMetrics();
    }

    /**
     * 配置Redis热点日志缓存
     *
     * @param hotLogRedisTemplate Redis模板
     * @param properties          配置属性
     * @param hotLogLocalCache    本地缓存
     * @param metrics             监控指标
     * @return RedisHotLogCache实例
     */
    @Bean
    public RedisHotLogCache redisHotLogCache(
            RedisTemplate<String, Object> hotLogRedisTemplate,
            HotLogCacheProperties properties,
            LocalLruCache<String, Object> hotLogLocalCache,
            HotLogCacheMetrics metrics) {

        // 验证配置
        properties.validate();

        return new RedisHotLogCache(
                hotLogRedisTemplate,
                properties,
                hotLogLocalCache,
                metrics);
    }

    /**
     * 配置缓存AOP切面
     *
     * @param cache      Redis缓存
     * @param properties 配置属性
     * @return HotLogCacheAspect实例
     */
    @Bean(name = "loggingHotLogCacheAspect")
    public HotLogCacheAspect hotLogCacheAspect(
            RedisHotLogCache cache,
            HotLogCacheProperties properties) {
        return new HotLogCacheAspect(cache, properties);
    }

    /**
     * 配置缓存预热任务
     * 启动时自动加载预热键到本地缓存
     *
     * @param cache      Redis缓存
     * @param properties 配置属性
     * @return ApplicationRunner实例
     */
    @Bean
    public ApplicationRunner hotLogCachePreheater(
            RedisHotLogCache cache,
            HotLogCacheProperties properties) {

        return args -> {
            if (properties.getPreloadKeys() != null && !properties.getPreloadKeys().isEmpty()) {
                cache.preload(properties.getPreloadKeys());
            }
        };
    }
}
