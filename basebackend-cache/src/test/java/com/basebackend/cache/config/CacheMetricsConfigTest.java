package com.basebackend.cache.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheMetricsConfig 缓存指标配置测试
 * 测试 Micrometer 指标配置的创建
 */
@SpringBootTest(classes = {
    CacheMetricsConfig.class,
    CacheProperties.class,
    CacheMetricsConfigTest.TestConfig.class
})
@TestPropertySource(properties = {
    "basebackend.cache.enabled=true",
    "basebackend.cache.metrics.enabled=true",
    "basebackend.cache.metrics.export-to-micrometer=true",
    "basebackend.cache.metrics.low-hit-rate-threshold=0.7"
})
class CacheMetricsConfigTest {

    @Configuration
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private MeterRegistryCustomizer<MeterRegistry> cacheMetricsCustomizer;

    @Test
    void testCacheMetricsConfigLoaded() {
        assertNotNull(applicationContext);
        assertTrue(applicationContext.containsBean("cacheMetricsConfig"));
    }

    @Test
    void testMeterRegistryAvailable() {
        assertNotNull(meterRegistry);
    }

    @Test
    void testCacheMetricsCustomizerCreated() {
        assertNotNull(cacheMetricsCustomizer);
    }

    @Test
    void testMetricsPropertiesLoaded() {
        CacheProperties.Metrics metrics = cacheProperties.getMetrics();
        assertTrue(metrics.isEnabled());
        assertTrue(metrics.isExportToMicrometer());
        assertTrue(metrics.getLowHitRateThreshold() > 0);
    }

    @Test
    void testMeterRegistryCustomization() {
        assertNotNull(cacheMetricsCustomizer);
        assertNotNull(meterRegistry);
        
        // Apply the customizer
        cacheMetricsCustomizer.customize(meterRegistry);
        
        // Verify that the registry is properly configured
        assertNotNull(meterRegistry.config());
    }
}
