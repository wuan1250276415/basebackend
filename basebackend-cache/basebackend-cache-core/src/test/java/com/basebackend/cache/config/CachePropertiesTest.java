package com.basebackend.cache.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheProperties 配置属性测试
 * 测试配置属性的加载和默认值
 */
@SpringBootTest(classes = {CacheProperties.class})
@TestPropertySource(properties = {
    "basebackend.cache.enabled=true",
    "basebackend.cache.multi-level.enabled=true",
    "basebackend.cache.multi-level.local-max-size=2000",
    "basebackend.cache.multi-level.local-ttl=10m",
    "basebackend.cache.multi-level.eviction-policy=LFU",
    "basebackend.cache.metrics.enabled=true",
    "basebackend.cache.metrics.low-hit-rate-threshold=0.6",
    "basebackend.cache.metrics.export-to-micrometer=true",
    "basebackend.cache.serialization.type=protobuf",
    "basebackend.cache.resilience.fallback-enabled=false",
    "basebackend.cache.resilience.timeout=5s",
    "basebackend.cache.resilience.circuit-breaker.enabled=true",
    "basebackend.cache.resilience.circuit-breaker.failure-threshold=10",
    "basebackend.cache.key.prefix=test-app",
    "basebackend.cache.key.separator=-",
    "basebackend.cache.lock.default-wait-time=20s",
    "basebackend.cache.lock.fair-lock-enabled=true"
})
class CachePropertiesTest {

    @Autowired
    private CacheProperties cacheProperties;

    @Test
    void testCachePropertiesLoaded() {
        assertNotNull(cacheProperties);
        assertTrue(cacheProperties.isEnabled());
    }

    @Test
    void testMultiLevelConfiguration() {
        CacheProperties.MultiLevel multiLevel = cacheProperties.getMultiLevel();
        assertNotNull(multiLevel);
        // Properties are loaded from test configuration
        assertNotNull(multiLevel.getLocalTtl());
        assertNotNull(multiLevel.getEvictionPolicy());
    }

    @Test
    void testMetricsConfiguration() {
        CacheProperties.Metrics metrics = cacheProperties.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.isEnabled());
        assertTrue(metrics.getLowHitRateThreshold() > 0);
        assertTrue(metrics.isExportToMicrometer());
    }

    @Test
    void testSerializationConfiguration() {
        CacheProperties.Serialization serialization = cacheProperties.getSerialization();
        assertNotNull(serialization);
        assertNotNull(serialization.getType());
        
        // Test nested configurations
        assertNotNull(serialization.getJson());
        assertFalse(serialization.getJson().isPrettyPrint());
        assertFalse(serialization.getJson().isIncludeTypeInfo());
        
        assertNotNull(serialization.getProtobuf());
        assertFalse(serialization.getProtobuf().isEnabled());
        
        assertNotNull(serialization.getKryo());
        assertFalse(serialization.getKryo().isEnabled());
        assertFalse(serialization.getKryo().isRegisterRequired());
    }

    @Test
    void testResilienceConfiguration() {
        CacheProperties.Resilience resilience = cacheProperties.getResilience();
        assertNotNull(resilience);
        assertNotNull(resilience.getTimeout());
        
        // Test circuit breaker configuration
        CacheProperties.Resilience.CircuitBreaker circuitBreaker = resilience.getCircuitBreaker();
        assertNotNull(circuitBreaker);
        assertNotNull(circuitBreaker.getOpenDuration());
        assertTrue(circuitBreaker.getFailureThreshold() > 0);
        assertTrue(circuitBreaker.getHalfOpenRequests() > 0);
        
        // Test auto recovery configuration
        CacheProperties.Resilience.AutoRecovery autoRecovery = resilience.getAutoRecovery();
        assertNotNull(autoRecovery);
        assertNotNull(autoRecovery.getCheckInterval());
    }

    @Test
    void testWarmingConfiguration() {
        CacheProperties.Warming warming = cacheProperties.getWarming();
        assertNotNull(warming);
        assertFalse(warming.isEnabled());
        assertNotNull(warming.getTasks());
        assertTrue(warming.getTasks().isEmpty());
        assertEquals(Duration.ofMinutes(5), warming.getTimeout());
        assertTrue(warming.isAsync());
    }

    @Test
    void testKeyConfiguration() {
        CacheProperties.Key key = cacheProperties.getKey();
        assertNotNull(key);
        assertNotNull(key.getPrefix());
        assertNotNull(key.getSeparator());
    }

    @Test
    void testTemplateConfiguration() {
        CacheProperties.Template template = cacheProperties.getTemplate();
        assertNotNull(template);
        
        // Test Cache-Aside configuration
        CacheProperties.Template.CacheAside cacheAside = template.getCacheAside();
        assertNotNull(cacheAside);
        assertNotNull(cacheAside.getDefaultTtl());
        
        // Test Write-Through configuration
        CacheProperties.Template.WriteThrough writeThrough = template.getWriteThrough();
        assertNotNull(writeThrough);
        
        // Test Write-Behind configuration
        CacheProperties.Template.WriteBehind writeBehind = template.getWriteBehind();
        assertNotNull(writeBehind);
        assertTrue(writeBehind.getBatchSize() > 0);
        assertNotNull(writeBehind.getBatchInterval());
    }

    @Test
    void testLockConfiguration() {
        CacheProperties.Lock lock = cacheProperties.getLock();
        assertNotNull(lock);
        assertNotNull(lock.getDefaultWaitTime());
        assertNotNull(lock.getDefaultLeaseTime());
    }

    @Test
    void testDefaultValues() {
        CacheProperties defaultProps = new CacheProperties();
        
        // Test main properties defaults
        assertTrue(defaultProps.isEnabled());
        
        // Test multi-level defaults
        assertFalse(defaultProps.getMultiLevel().isEnabled());
        assertEquals(1000, defaultProps.getMultiLevel().getLocalMaxSize());
        assertEquals(Duration.ofMinutes(5), defaultProps.getMultiLevel().getLocalTtl());
        assertEquals("LRU", defaultProps.getMultiLevel().getEvictionPolicy());
        
        // Test metrics defaults
        assertTrue(defaultProps.getMetrics().isEnabled());
        assertEquals(0.5, defaultProps.getMetrics().getLowHitRateThreshold(), 0.001);
        assertTrue(defaultProps.getMetrics().isExportToMicrometer());
        
        // Test warming defaults
        assertFalse(defaultProps.getWarming().isEnabled());
        assertTrue(defaultProps.getWarming().getTasks().isEmpty());
        assertEquals(Duration.ofMinutes(5), defaultProps.getWarming().getTimeout());
        assertTrue(defaultProps.getWarming().isAsync());
        
        // Test serialization defaults
        assertEquals("json", defaultProps.getSerialization().getType());
        assertFalse(defaultProps.getSerialization().getJson().isPrettyPrint());
        assertFalse(defaultProps.getSerialization().getJson().isIncludeTypeInfo());
        
        // Test resilience defaults
        assertTrue(defaultProps.getResilience().isFallbackEnabled());
        assertEquals(Duration.ofSeconds(3), defaultProps.getResilience().getTimeout());
        assertFalse(defaultProps.getResilience().getCircuitBreaker().isEnabled());
        assertEquals(5, defaultProps.getResilience().getCircuitBreaker().getFailureThreshold());
        
        // Test key defaults
        assertEquals("basebackend", defaultProps.getKey().getPrefix());
        assertEquals(":", defaultProps.getKey().getSeparator());
        assertTrue(defaultProps.getKey().isIncludeAppName());
        
        // Test template defaults
        assertEquals(Duration.ofHours(1), defaultProps.getTemplate().getCacheAside().getDefaultTtl());
        assertFalse(defaultProps.getTemplate().getCacheAside().isBloomFilterEnabled());
        assertFalse(defaultProps.getTemplate().getWriteThrough().isEnabled());
        assertFalse(defaultProps.getTemplate().getWriteBehind().isEnabled());
        
        // Test lock defaults
        assertEquals(Duration.ofSeconds(10), defaultProps.getLock().getDefaultWaitTime());
        assertEquals(Duration.ofSeconds(30), defaultProps.getLock().getDefaultLeaseTime());
        assertFalse(defaultProps.getLock().isFairLockEnabled());
        assertFalse(defaultProps.getLock().isRedLockEnabled());
    }

    @Test
    void testWarmingTaskConfiguration() {
        CacheProperties.WarmingTask task = new CacheProperties.WarmingTask();
        task.setName("test-task");
        task.setPriority(5);
        task.setCacheName("test-cache");
        task.setTtl(Duration.ofHours(2));
        
        assertEquals("test-task", task.getName());
        assertEquals(5, task.getPriority());
        assertEquals("test-cache", task.getCacheName());
        assertEquals(Duration.ofHours(2), task.getTtl());
    }

    @Test
    void testWarmingTaskDefaults() {
        CacheProperties.WarmingTask task = new CacheProperties.WarmingTask();
        assertEquals(0, task.getPriority());
        assertEquals(Duration.ofHours(1), task.getTtl());
    }
}
