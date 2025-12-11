package com.basebackend.featuretoggle.config;

import com.basebackend.featuretoggle.cache.FeatureToggleCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 特性开关缓存自动配置
 * <p>
 * 自动配置Caffeine缓存管理器，用于缓存特性开关状态。
 * 可通过配置属性自定义缓存参数。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnClass({Caffeine.class, CacheManager.class})
@EnableConfigurationProperties(FeatureToggleProperties.class)
public class CacheAutoConfiguration {

    /**
     * 配置Caffeine缓存管理器
     *
     * @param properties 特性开关配置属性
     * @return 缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "basebackend.feature-toggle.cache.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager(FeatureToggleProperties properties) {
        log.info("Initializing Caffeine cache manager for feature toggle");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置缓存规范 - 使用默认值，避免getCache()方法问题
        com.github.benmanes.caffeine.cache.Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(10000L)
                .expireAfterWrite(300L, TimeUnit.SECONDS)
                .expireAfterAccess(600L, TimeUnit.SECONDS)
                .recordStats();

        // 配置缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
                FeatureToggleCache.FEATURE_STATE_CACHE,
                FeatureToggleCache.VARIANT_CACHE,
                FeatureToggleCache.GROUP_ASSIGNMENT_CACHE,
                FeatureToggleCache.ROLLOUT_CACHE
        ));

        cacheManager.setCaffeine(caffeineBuilder);

        log.info("Caffeine cache manager configured with default settings");

        return cacheManager;
    }

    /**
     * 配置特性开关缓存管理Bean
     *
     * @param cacheManager 缓存管理器
     * @return 特性开关缓存实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "basebackend.feature-toggle.enabled", havingValue = "true", matchIfMissing = true)
    public FeatureToggleCache featureToggleCache(CacheManager cacheManager) {
        log.info("Creating FeatureToggleCache bean");
        return new FeatureToggleCache(cacheManager);
    }
}
