package com.basebackend.logging.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

/**
 * Redis热点日志缓存
 *
 * 核心特性：
 * 1. 多级缓存：本地LRU + Redis双层缓存
 * 2. 热点识别：基于访问频次的自动提升机制
 * 3. 雪崩防护：TTL随机抖动，防止同时过期
 * 4. 性能监控：完整的命中/未命中/淘汰统计
 * 5. 灵活策略：支持读透、写透、写回、失效等策略
 *
 * 性能指标：
 * - 查询速度提升：目标5倍以上
 * - 命中率：目标>90%
 * - 响应时间：<10ms（本地缓存）
 *
 * @author basebackend team
 * @since 2025-11-22
 */
public class RedisHotLogCache {

    /**
     * Redis模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 配置属性
     */
    private final HotLogCacheProperties properties;

    /**
     * 本地缓存
     */
    private final LocalLruCache<String, Object> local;

    /**
     * 监控指标
     */
    private final HotLogCacheMetrics metrics;

    /**
     * 访问频次统计（用于热点识别）
     */
    private final Map<String, LongAdder> frequency = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param redisTemplate Redis模板
     * @param properties   配置属性
     * @param local        本地缓存
     * @param metrics      监控指标
     */
    public RedisHotLogCache(RedisTemplate<String, Object> redisTemplate,
                           HotLogCacheProperties properties,
                           LocalLruCache<String, Object> local,
                           HotLogCacheMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.local = local;
        this.metrics = metrics;
    }

    /**
     * 构建完整缓存键
     *
     * @param rawKey 原始键
     * @return 带前缀的完整键
     */
    public String buildKey(String rawKey) {
        return properties.getCachePrefix() + rawKey;
    }

    /**
     * 获取缓存值（读透策略）
     *
     * @param rawKey 原始键
     * @param type   类型
     * @param <T>    泛型
     * @return 缓存值（可能为空）
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String rawKey, Class<T> type) {
        String key = buildKey(rawKey);

        // 优先查本地缓存（最快）
        if (properties.isUseLocalCache()) {
            Object cached = local.get(key);
            if (cached != null) {
                metrics.hit();
                return Optional.of((T) cached);
            }
        }

        // 查Redis缓存
        Object remote = redisTemplate.opsForValue().get(key);
        if (remote != null) {
            metrics.hit();
            // 同步到本地缓存
            if (properties.isUseLocalCache()) {
                local.put(key, remote);
            }
            return Optional.of((T) remote);
        }

        metrics.miss();
        return Optional.empty();
    }

    /**
     * 放入缓存
     *
     * @param rawKey 原始键
     * @param value  值
     * @param ttl    过期时间
     */
    public void put(String rawKey, Object value, Duration ttl) {
        String key = buildKey(rawKey);

        // 写入Redis
        redisTemplate.opsForValue().set(key, value, ttl);

        // 同步到本地缓存
        if (properties.isUseLocalCache()) {
            local.put(key, value);
            metrics.updateSize(1);
        }
    }

    /**
     * 删除缓存
     *
     * @param rawKey 原始键
     */
    public void evict(String rawKey) {
        String key = buildKey(rawKey);

        // 删除Redis
        redisTemplate.delete(key);

        // 删除本地缓存
        if (properties.isUseLocalCache()) {
            local.remove(key);
            metrics.evict();
            metrics.updateSize(-1);
        }

        // 删除频次统计
        frequency.remove(key);
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        // 清空Redis
        redisTemplate.delete(redisTemplate.keys(properties.getCachePrefix() + "*"));

        // 清空本地缓存
        if (properties.isUseLocalCache()) {
            local.clear();
        }

        // 清空频次统计
        frequency.clear();

        // 重置指标
        metrics.reset();
    }

    /**
     * 预热缓存
     * 将指定的键从Redis加载到本地缓存
     *
     * @param keys 键集合
     */
    public void preload(Iterable<String> keys) {
        if (!properties.isUseLocalCache()) {
            return;
        }

        for (String key : keys) {
            String fullKey = buildKey(key);
            Object value = redisTemplate.opsForValue().get(fullKey);
            if (value != null) {
                local.put(fullKey, value);
                metrics.preload();
            }
        }
    }

    /**
     * 判断是否应该提升为热点数据
     *
     * @param rawKey          原始键
     * @param thresholdOverride 阈值覆盖（-1表示使用全局配置）
     * @return true=应该提升，false=暂不提升
     */
    public boolean shouldPromote(String rawKey, int thresholdOverride) {
        int threshold = thresholdOverride > 0 ? thresholdOverride : properties.getHotThreshold();
        if (threshold <= 0) {
            return true;  // 阈值<=0表示所有数据都提升
        }

        String key = buildKey(rawKey);
        LongAdder adder = frequency.computeIfAbsent(key, k -> new LongAdder());
        adder.increment();

        return adder.sum() >= threshold;
    }

    /**
     * 解析TTL
     *
     * @param overrideSeconds 覆盖秒数（-1表示使用全局配置）
     * @return 过期时间
     */
    public Duration resolveTtl(long overrideSeconds) {
        if (overrideSeconds > 0) {
            return Duration.ofSeconds(overrideSeconds);
        }

        // 添加随机抖动防止雪崩
        long jitter = properties.getJitterSeconds() <= 0
                ? 0
                : ThreadLocalRandom.current().nextLong(properties.getJitterSeconds() + 1);
        long ttl = Math.max(1, properties.getTtlSeconds() + jitter);
        return Duration.ofSeconds(ttl);
    }

    /**
     * 生成默认缓存键
     *
     * @param target 目标对象
     * @param method 方法名
     * @param args   参数
     * @return 默认键
     */
    public String defaultKey(Object target, String method, Object[] args) {
        StringBuilder base = new StringBuilder();
        base.append(target.getClass().getName());
        base.append("#");
        base.append(method);

        if (args != null && args.length > 0) {
            // 使用第一个参数的MD5作为键的一部分
            String firstArg = Objects.toString(args[0], "");
            String hash = DigestUtils.md5DigestAsHex(firstArg.getBytes(StandardCharsets.UTF_8));
            base.append("|");
            base.append(hash);
        }

        return base.toString();
    }

    /**
     * 批量获取
     *
     * @param keys 键集合
     * @return 键值对Map
     */
    public Map<String, Object> batchGet(Iterable<String> keys) {
        Map<String, Object> result = new ConcurrentHashMap<>();
        for (String key : keys) {
            get(key, Object.class).ifPresent(value -> result.put(key, value));
        }
        return result;
    }

    /**
     * 批量删除
     *
     * @param keys 键集合
     */
    public void batchEvict(Iterable<String> keys) {
        for (String key : keys) {
            evict(key);
        }
    }

    /**
     * 获取指标
     *
     * @return 监控指标
     */
    public HotLogCacheMetrics getMetrics() {
        return metrics;
    }

    /**
     * 获取本地缓存大小
     *
     * @return 本地缓存条目数
     */
    public int getLocalSize() {
        return local != null ? local.size() : 0;
    }

    /**
     * 获取Redis缓存大小
     *
     * @return Redis缓存条目数（近似）
     */
    public long getRedisSize() {
        try {
            return redisTemplate.keys(properties.getCachePrefix() + "*").size();
        } catch (Exception e) {
            return -1;  // 获取失败
        }
    }

    /**
     * 获取热点键列表
     *
     * @param limit 限制数量
     * @return 热点键列表
     */
    public java.util.List<String> getHotKeys(int limit) {
        return frequency.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().sum(), e1.getValue().sum()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 检查Redis连接
     *
     * @return true=连接正常，false=连接异常
     */
    public boolean ping() {
        try {
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return "PONG".equals(result);
        } catch (Exception e) {
            return false;
        }
    }
}
