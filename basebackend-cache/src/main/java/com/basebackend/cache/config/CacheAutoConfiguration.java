package com.basebackend.cache.config;

import com.basebackend.cache.exception.CacheConfigurationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

/**
 * 缓存自动配置类
 * 根据配置属性自动装配缓存相关的 Bean
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
@EnableScheduling
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "basebackend.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    RedisConfig.class,
    MultiLevelCacheConfig.class,
    CacheMetricsConfig.class
})
@org.springframework.context.annotation.ComponentScan(basePackages = "com.basebackend.cache")
public class CacheAutoConfiguration {

    private final CacheProperties cacheProperties;

    public CacheAutoConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @PostConstruct
    public void init() {
        validateConfiguration();
        
        log.info("=== Cache Module Configuration ===");
        log.info("Cache enabled: {}", cacheProperties.isEnabled());
        log.info("Multi-level cache enabled: {}", cacheProperties.getMultiLevel().isEnabled());
        log.info("Metrics enabled: {}", cacheProperties.getMetrics().isEnabled());
        log.info("Cache warming enabled: {}", cacheProperties.getWarming().isEnabled());
        log.info("Serialization type: {}", cacheProperties.getSerialization().getType());
        log.info("Fallback enabled: {}", cacheProperties.getResilience().isFallbackEnabled());
        log.info("Circuit breaker enabled: {}", cacheProperties.getResilience().getCircuitBreaker().isEnabled());
        log.info("===================================");
    }
    
    /**
     * 验证缓存配置的有效性
     * @throws CacheConfigurationException 如果配置无效
     */
    private void validateConfiguration() {
        // 验证序列化类型
        String serializationType = cacheProperties.getSerialization().getType();
        if (!StringUtils.hasText(serializationType)) {
            throw new CacheConfigurationException("Serialization type cannot be empty");
        }
        
        String normalizedType = serializationType.toLowerCase().trim();
        if (!normalizedType.equals("json") && !normalizedType.equals("protobuf") && !normalizedType.equals("kryo")) {
            throw new CacheConfigurationException(
                "Invalid serialization type: " + serializationType + 
                ". Supported types are: json, protobuf, kryo"
            );
        }
        
        // 验证多级缓存配置
        if (cacheProperties.getMultiLevel().isEnabled()) {
            if (cacheProperties.getMultiLevel().getLocalMaxSize() <= 0) {
                throw new CacheConfigurationException(
                    "Local cache max size must be positive, but got: " + 
                    cacheProperties.getMultiLevel().getLocalMaxSize()
                );
            }
            
            if (cacheProperties.getMultiLevel().getLocalTtl() == null || 
                cacheProperties.getMultiLevel().getLocalTtl().isNegative()) {
                throw new CacheConfigurationException(
                    "Local cache TTL must be positive"
                );
            }
        }
        
        // 验证指标配置
        if (cacheProperties.getMetrics().isEnabled()) {
            double threshold = cacheProperties.getMetrics().getLowHitRateThreshold();
            if (threshold < 0.0 || threshold > 1.0) {
                throw new CacheConfigurationException(
                    "Low hit rate threshold must be between 0.0 and 1.0, but got: " + threshold
                );
            }
        }
        
        // 验证容错配置
        if (cacheProperties.getResilience().getTimeout() == null || 
            cacheProperties.getResilience().getTimeout().isNegative()) {
            throw new CacheConfigurationException(
                "Resilience timeout must be positive"
            );
        }
        
        if (cacheProperties.getResilience().getCircuitBreaker().isEnabled()) {
            int threshold = cacheProperties.getResilience().getCircuitBreaker().getFailureThreshold();
            if (threshold <= 0) {
                throw new CacheConfigurationException(
                    "Circuit breaker failure threshold must be positive, but got: " + threshold
                );
            }
        }
        
        log.debug("Cache configuration validation passed");
    }
}
