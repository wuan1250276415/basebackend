package com.basebackend.cache.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存指标配置类
 * 配置 Micrometer 指标收集和导出
 */
@Slf4j
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "basebackend.cache.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheMetricsConfig {

    private final CacheProperties cacheProperties;

    public CacheMetricsConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 配置 MeterRegistry 自定义器
     * 为缓存指标添加通用标签
     */
    @Bean
    @ConditionalOnProperty(prefix = "basebackend.cache.metrics", name = "export-to-micrometer", havingValue = "true", matchIfMissing = true)
    public MeterRegistryCustomizer<MeterRegistry> cacheMetricsCustomizer() {
        log.info("Configuring cache metrics with Micrometer");
        log.info("Low hit rate threshold: {}", cacheProperties.getMetrics().getLowHitRateThreshold());
        
        return registry -> {
            // 添加缓存模块的通用标签
            registry.config().commonTags(
                Tags.of(
                    Tag.of("module", "cache"),
                    Tag.of("application", "basebackend")
                )
            );
            
            log.info("Cache metrics registered with common tags");
        };
    }
}
