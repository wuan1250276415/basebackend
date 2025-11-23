package com.basebackend.logging.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 热点日志缓存配置属性
 *
 * 通过 Spring Boot ConfigurationProperties 注解自动绑定配置。
 * 支持通过 application.yml 或 application.properties 文件进行配置。
 *
 * @author basebackend team
 * @since 2025-11-22
 */
@ConfigurationProperties(prefix = "basebackend.logging.hot-cache")
public class HotLogCacheProperties {

    /**
     * 是否启用热点日志缓存
     */
    private boolean enabled = true;

    /**
     * 缓存键前缀，用于隔离不同应用的缓存空间
     */
    private String cachePrefix = "hotlog:";

    /**
     * 逻辑最大条目数（Redis层面的限制）
     * 实际Redis不强制执行，用于LRU淘汰的参考值
     */
    private int maxEntries = 5000;

    /**
     * 本地内存缓存最大条目数
     * 用于保护Redis，降低延迟（目标<10ms）
     */
    private int localMaxEntries = 1024;

    /**
     * 是否启用本地缓存
     * 禁用后仅使用Redis缓存
     */
    private boolean useLocalCache = true;

    /**
     * 默认TTL（秒）
     * 超过此时间未访问的缓存条目将被删除
     */
    private long ttlSeconds = 300;

    /**
     * TTL随机抖动（秒）
     * 用于防止缓存雪崩，为每个缓存项随机添加0-jitterSeconds的过期时间
     */
    private long jitterSeconds = 30;

    /**
     * 热点阈值
     * 访问次数达到此值后将数据提升为热点数据并缓存
     */
    private int hotThreshold = 5;

    /**
     * 预热键列表
     * 启动时自动加载到本地缓存的键列表
     */
    private List<String> preloadKeys = new ArrayList<>();

    /**
     * 是否验证序列化
     * 启用后会在序列化/反序列化时进行额外检查
     */
    private boolean verifySerialization = true;

    /**
     * Redis连接超时时间（毫秒）
     */
    private long connectTimeoutMillis = 2000;

    /**
     * Redis读写超时时间（毫秒）
     */
    private long timeoutMillis = 5000;

    /**
     * 缓存清理任务执行间隔（秒）
     */
    private long cleanupIntervalSeconds = 60;

    // ==================== Getter/Setter ====================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCachePrefix() {
        return cachePrefix;
    }

    public void setCachePrefix(String cachePrefix) {
        this.cachePrefix = cachePrefix;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public int getLocalMaxEntries() {
        return localMaxEntries;
    }

    public void setLocalMaxEntries(int localMaxEntries) {
        this.localMaxEntries = localMaxEntries;
    }

    public boolean isUseLocalCache() {
        return useLocalCache;
    }

    public void setUseLocalCache(boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public long getJitterSeconds() {
        return jitterSeconds;
    }

    public void setJitterSeconds(long jitterSeconds) {
        this.jitterSeconds = jitterSeconds;
    }

    public int getHotThreshold() {
        return hotThreshold;
    }

    public void setHotThreshold(int hotThreshold) {
        this.hotThreshold = hotThreshold;
    }

    public List<String> getPreloadKeys() {
        return preloadKeys;
    }

    public void setPreloadKeys(List<String> preloadKeys) {
        this.preloadKeys = preloadKeys;
    }

    public boolean isVerifySerialization() {
        return verifySerialization;
    }

    public void setVerifySerialization(boolean verifySerialization) {
        this.verifySerialization = verifySerialization;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(long connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public long getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }

    public void setCleanupIntervalSeconds(long cleanupIntervalSeconds) {
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
    }

    // ==================== 工具方法 ====================

    /**
     * 获取带随机抖动的TTL持续时间
     * 用于防止缓存雪崩
     *
     * @return 随机抖动后的TTL
     */
    public Duration ttlDurationWithJitter() {
        long jitter = jitterSeconds <= 0 ? 0 : (long) (Math.random() * jitterSeconds);
        long ttl = Math.max(1, ttlSeconds + jitter);
        return Duration.ofSeconds(ttl);
    }

    /**
     * 验证配置的有效性
     * 在启动时调用，确保配置参数合理
     */
    public void validate() {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be > 0");
        }
        if (hotThreshold < 0) {
            throw new IllegalArgumentException("hotThreshold must be >= 0");
        }
        if (localMaxEntries <= 0) {
            throw new IllegalArgumentException("localMaxEntries must be > 0");
        }
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be > 0");
        }
        if (connectTimeoutMillis <= 0) {
            throw new IllegalArgumentException("connectTimeoutMillis must be > 0");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be > 0");
        }
        if (cleanupIntervalSeconds <= 0) {
            throw new IllegalArgumentException("cleanupIntervalSeconds must be > 0");
        }
    }
}
