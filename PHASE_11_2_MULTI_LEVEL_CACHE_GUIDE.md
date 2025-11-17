# Phase 11.2: 多级缓存架构实施指南

## 📋 概述

本指南介绍如何实现多级缓存架构，包括 L1 本地缓存（Caffeine）和 L2 分布式缓存（Redis），提供完整的缓存解决方案，包括预热、穿透防护、雪崩防护等功能。

---

## 🏗️ 多级缓存架构

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      多级缓存架构                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐                                          │
│  │   应用服务    │                                          │
│  └──────┬───────┘                                          │
│         │                                                  │
│  ┌──────▼───────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ L1 Cache     │  │ L2 Cache    │  │  Database   │        │
│  │ (Caffeine)   │  │ (Redis)     │  │ (MySQL)     │        │
│  │              │  │             │  │             │        │
│  │ - 热点数据    │  │ - 业务数据  │  │ - 原始数据  │        │
│  │ - 1000条     │  │ - 所有数据  │  │ - 全量数据  │        │
│  │ - TTL 5-10分 │  │ - TTL 1-24小时│  │ - 后备方案 │        │
│  └──────┬───────┘  └──────┬──────┘  └──────┬──────┘        │
│         │                  │                  │              │
│  ┌──────▼───────┐  ┌──────▼──────┐  ┌──────▼──────┐        │
│  │   快速响应    │  │  分布式共享   │  │  最终数据源   │        │
│  │   < 1ms      │  │  < 10ms      │  │  < 50ms     │        │
│  └──────────────┘  └─────────────┘  └─────────────┘        │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              缓存策略与防护                             │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ • Cache Aside (推荐)                                 │  │
│  │ • 预热机制 (Warm-up)                                 │  │
│  │ • 穿透防护 (Bloom Filter)                             │  │
│  │ • 雪崩防护 (Random TTL + 分布式锁)                    │  │
│  │ • 击穿防护 (SingleFlight)                            │  │
│  │ • 数据一致性 (Canal)                                  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 缓存层级说明

| 层级 | 技术 | 容量 | TTL | 作用 | 响应时间 |
|------|------|------|-----|------|----------|
| **L1** | Caffeine | 1000 条 | 5-10 分钟 | 热点数据 | < 1ms |
| **L2** | Redis | 无限制 | 1-24 小时 | 业务数据 | < 10ms |
| **L3** | MySQL | 无限制 | - | 最终数据源 | < 50ms |

---

## 🔧 依赖配置

### Maven 依赖

在 `pom.xml` 中添加：

```xml
<!-- 多级缓存 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Canal 数据一致性 -->
<dependency>
    <groupId>com.alibaba.otter</groupId>
    <artifactId>canal.client</artifactId>
    <version>1.1.7</version>
</dependency>

<!-- 布隆过滤器 -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.3-jre</version>
</dependency>

<!-- Redisson (Redis 客户端增强) -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.24.3</version>
</dependency>
```

---

## ☕ L1 Cache: Caffeine 配置

### 1. Caffeine 配置类

```java
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    /**
     * 用户缓存配置
     */
    @Bean("userCache")
    public Cache<String, Object> userCache() {
        return Caffeine.newBuilder()
            // 初始容量
            .initialCapacity(100)
            // 最大容量
            .maximumSize(1000)
            // 写后过期时间
            .expireAfterWrite(Duration.ofMinutes(10))
            // 读后过期时间
            .expireAfterAccess(Duration.ofMinutes(5))
            // 访问后刷新时间
            .refreshAfterWrite(Duration.ofMinutes(3))
            // 开启统计
            .recordStats()
            // 监听器
            .removalListener((key, value, cause) ->
                log.info("用户缓存移除: key={}, cause={}", key, cause))
            .build();
    }

    /**
     * 菜单缓存配置
     */
    @Bean("menuCache")
    public Cache<String, Object> menuCache() {
        return Caffeine.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .recordStats()
            .build();
    }

    /**
     * 权限缓存配置
     */
    @Bean("permissionCache")
    public Cache<String, Object> permissionCache() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .expireAfterAccess(Duration.ofMinutes(5))
            .recordStats()
            .build();
    }

    /**
     * 字典缓存配置
     */
    @Bean("dictCache")
    public Cache<String, Object> dictCache() {
        return Caffeine.newBuilder()
            .initialCapacity(20)
            .maximumSize(200)
            .expireAfterWrite(Duration.ofMinutes(60))
            .expireAfterAccess(Duration.ofMinutes(30))
            .recordStats()
            .build();
    }

    /**
     * 用户配置缓存
     */
    @Bean("userProfileCache")
    public Cache<String, Object> userProfileCache() {
        return Caffeine.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(20))
            .expireAfterAccess(Duration.ofMinutes(10))
            .recordStats()
            .build();
    }

    /**
     * 热点数据缓存
     */
    @Bean("hotDataCache")
    public Cache<String, Object> hotDataCache() {
        return Caffeine.newBuilder()
            .initialCapacity(200)
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .expireAfterAccess(Duration.ofMinutes(2))
            .recordStats()
            .build();
    }
}
```

### 2. 缓存管理器配置

```java
@Configuration
public class CacheManagerConfig {

    @Primary
    @Bean("cacheManager")
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .expireAfterAccess(Duration.ofMinutes(5))
            .recordStats());
        return cacheManager;
    }
}
```

---

## 🔴 L2 Cache: Redis 配置

### 1. Redis 连接配置

```yaml
# application.yml
spring:
  cache:
    type: redis

  redis:
    host: ${REDIS_HOST:1.117.67.222}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:redis_ycecQi}
    database: ${REDIS_DATABASE:0}
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: -1ms
    # 连接超时
    connect-timeout: 3000ms
    # 读取超时
    read-timeout: 3000ms

  # 多级缓存配置
  cache:
    multi-level:
      enabled: true
      l1:
        type: caffeine
      l2:
        type: redis
        key-prefix: "basebackend:"
        ttl: 3600 # 默认 TTL (秒)
```

### 2. Redis 配置类

```java
@Configuration
@ConfigurationProperties(prefix = "spring.cache.multi-level")
public class MultiLevelCacheProperties {

    private boolean enabled = true;

    private L1Config l1 = new L1Config();

    private L2Config l2 = new L2Config();

    @Data
    public static class L1Config {
        private String type = "caffeine";
    }

    @Data
    public static class L2Config {
        private String type = "redis";
        private String keyPrefix = "basebackend:";
        private int ttl = 3600; // 秒
    }
}
```

### 3. Redis 序列化配置

```java
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 StringRedisSerializer 序列化 key
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 使用 GenericJackson2JsonRedisSerializer 序列化 value
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter
            .nonLockingRedisCacheWriter(connectionFactory);

        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return new RedisCacheManager(redisCacheWriter, config);
    }
}
```

---

## 🔄 多级缓存实现

### 1. 多级缓存接口

```java
/**
 * 多级缓存接口
 */
public interface MultiLevelCache {

    /**
     * 获取缓存
     *
     * @param key   缓存键
     * @param type  数据类型
     * @param loader 数据加载器
     * @return 缓存值
     */
    <T> T get(String key, Class<T> type, Supplier<T> loader);

    /**
     * 设置缓存
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param ttl      过期时间
     */
    void put(String key, Object value, Duration ttl);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void evict(String key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean contains(String key);
}
```

### 2. 多级缓存实现

```java
@Component
public class MultiLevelCacheImpl implements MultiLevelCache {

    private final Cache<String, Object> l1Cache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MultiLevelCacheProperties properties;

    public MultiLevelCacheImpl(
            @Qualifier("userCache") Cache<String, Object> userCache,
            RedisTemplate<String, Object> redisTemplate,
            MultiLevelCacheProperties properties) {
        this.l1Cache = userCache;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public <T> T get(String key, Class<T> type, Supplier<T> loader) {
        // 1. 先查 L1 缓存
        Object value = l1Cache.getIfPresent(key);
        if (value != null) {
            log.debug("缓存命中 (L1): {}", key);
            return type.cast(value);
        }

        // 2. 再查 L2 缓存 (Redis)
        String redisKey = buildRedisKey(key);
        value = redisTemplate.opsForValue().get(redisKey);
        if (value != null) {
            log.debug("缓存命中 (L2): {}", key);
            // 同步到 L1 缓存
            l1Cache.put(key, value);
            return type.cast(value);
        }

        // 3. L1/L2 都未命中，加载数据
        log.debug("缓存未命中，加载数据: {}", key);
        value = loader.get();

        if (value != null) {
            // 写入 L2 缓存
            redisTemplate.opsForValue().set(
                redisKey,
                value,
                Duration.ofSeconds(properties.getL2().getTtl())
            );

            // 写入 L1 缓存
            l1Cache.put(key, value);

            log.debug("数据已缓存: {}", key);
        }

        return type.cast(value);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        // 同时写入 L1 和 L2
        l1Cache.put(key, value);

        redisTemplate.opsForValue().set(
            buildRedisKey(key),
            value,
            ttl
        );

        log.debug("数据已写入缓存: {}", key);
    }

    @Override
    public void evict(String key) {
        // 同时删除 L1 和 L2
        l1Cache.invalidate(key);
        redisTemplate.delete(buildRedisKey(key));

        log.debug("缓存已删除: {}", key);
    }

    @Override
    public void clear() {
        l1Cache.invalidateAll();
        // 注意: Redis 清空要谨慎，可以考虑按前缀删除
        // redisTemplate.delete(keys);
        log.debug("L1 缓存已清空");
    }

    @Override
    public boolean contains(String key) {
        return l1Cache.getIfPresent(key) != null ||
               redisTemplate.hasKey(buildRedisKey(key));
    }

    private String buildRedisKey(String key) {
        return properties.getL2().getKeyPrefix() + key;
    }
}
```

### 3. 缓存注解

```java
/**
 * 多级缓存注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiLevelCached {

    /**
     * 缓存键
     */
    String key() default "";

    /**
     * 缓存键 spel 表达式
     */
    String keySpel() default "";

    /**
     * 数据类型
     */
    Class<?> type() default Object.class;

    /**
     * TTL (秒)
     */
    long ttl() default 3600;

    /**
     * 是否使用布隆过滤器
     */
    boolean useBloomFilter() default false;

    /**
     * 是否防穿透
     */
    boolean preventPenetration() default true;

    /**
     * 是否防雪崩
     */
    boolean preventAvalanche() default true;
}
```

---

## 🔥 缓存预热

### 1. 预热服务

```java
@Service
public class CacheWarmupService {

    private final MultiLevelCache multiLevelCache;
    private final UserService userService;
    private final MenuService menuService;
    private final DictService dictService;

    /**
     * 系统启动时预热缓存
     */
    @PostConstruct
    public void warmupCache() {
        log.info("开始缓存预热...");

        // 并行预热多个缓存
        CompletableFuture.allOf(
            CompletableFuture.runAsync(this::warmupUserCache),
            CompletableFuture.runAsync(this::warmupMenuCache),
            CompletableFuture.runAsync(this::warmupDictCache),
            CompletableFuture.runAsync(this::warmupPermissionCache)
        ).join();

        log.info("缓存预热完成");
    }

    /**
     * 预热用户缓存
     */
    private void warmupUserCache() {
        log.info("预热用户缓存...");
        List<User> users = userService.listAll();
        for (User user : users) {
            String key = "user:" + user.getId();
            multiLevelCache.put(key, user, Duration.ofHours(2));
        }
        log.info("用户缓存预热完成: {} 条", users.size());
    }

    /**
     * 预热菜单缓存
     */
    private void warmupMenuCache() {
        log.info("预热菜单缓存...");
        List<Menu> menus = menuService.getAllMenus();
        String key = "menus:tree";
        multiLevelCache.put(key, menus, Duration.ofHours(6));
        log.info("菜单缓存预热完成: {} 条", menus.size());
    }

    /**
     * 预热字典缓存
     */
    private void warmupDictCache() {
        log.info("预热字典缓存...");
        List<Dict> dicts = dictService.listAll();
        for (Dict dict : dicts) {
            String key = "dict:" + dict.getType();
            multiLevelCache.put(key, dict.getValues(), Duration.ofHours(12));
        }
        log.info("字典缓存预热完成: {} 条", dicts.size());
    }

    /**
     * 预热权限缓存
     */
    private void warmupPermissionCache() {
        log.info("预热权限缓存...");
        Map<String, Set<String>> permissions = permissionService.getAllPermissions();
        for (Map.Entry<String, Set<String>> entry : permissions.entrySet()) {
            String key = "permissions:user:" + entry.getKey();
            multiLevelCache.put(key, entry.getValue(), Duration.ofHours(2));
        }
        log.info("权限缓存预热完成: {} 个用户", permissions.size());
    }

    /**
     * 定时预热任务
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void scheduledWarmup() {
        log.info("执行定时缓存预热...");
        warmupCache();
    }
}
```

### 2. 定时刷新

```java
@Component
public class CacheRefreshTask {

    private final MultiLevelCache multiLevelCache;

    @Scheduled(fixedRate = 300000) // 5分钟刷新一次
    public void refreshHotData() {
        log.debug("刷新热点数据...");

        // 刷新热点数据
        List<String> hotKeys = getHotKeys();
        for (String key : hotKeys) {
            refreshCache(key);
        }

        log.debug("热点数据刷新完成: {} 条", hotKeys.size());
    }

    private void refreshCache(String key) {
        // 实现缓存刷新逻辑
        // ...
    }

    private List<String> getHotKeys() {
        // 从监控数据中获取热点数据
        // ...
        return new ArrayList<>();
    }
}
```

---

## 🛡️ 缓存穿透防护

### 1. 布隆过滤器

```java
@Component
public class BloomFilterHelper {

    private final LoadingCache<String, Boolean> bloomFilter;

    public BloomFilterHelper() {
        // 创建布隆过滤器 (预期元素数量: 10000, 误判率: 0.01)
        bloomFilter = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build(key -> {
                // 这里应该初始化时加载所有存在的 key
                return false;
            });
    }

    /**
     * 检查 key 是否可能存在
     */
    public boolean mightContain(String key) {
        return bloomFilter.getIfPresent(key) != null;
    }

    /**
     * 添加 key 到布隆过滤器
     */
    public void put(String key) {
        bloomFilter.put(key, true);
    }

    /**
     * 批量添加 keys
     */
    public void putAll(List<String> keys) {
        keys.forEach(this::put);
    }
}
```

### 2. 穿透防护实现

```java
@Component
public class CachePenetrationPreventer {

    private final MultiLevelCache multiLevelCache;
    private final BloomFilterHelper bloomFilter;

    /**
     * 获取缓存（防穿透）
     */
    public <T> T getWithPenetrationPrevent(String key, Class<T> type, Supplier<T> loader) {
        // 1. 布隆过滤器检查
        if (!bloomFilter.mightContain(key)) {
            log.debug("布隆过滤器判定 key 不存在: {}", key);
            return null;
        }

        // 2. 尝试从缓存获取
        T value = multiLevelCache.get(key, type, () -> null);

        // 3. 缓存未命中，加载数据
        if (value == null) {
            value = loader.get();

            if (value != null) {
                multiLevelCache.put(key, value, Duration.ofHours(1));
                bloomFilter.put(key);
            } else {
                // 缓存空值（短期）
                multiLevelCache.put(key, new NullValue(), Duration.ofMinutes(5));
            }
        }

        return value;
    }

    /**
     * 空值对象
     */
    private static class NullValue {
        // 占位对象
    }
}
```

---

## ❄️ 缓存雪崩防护

### 1. 随机过期时间

```java
@Component
public class CacheAvalanchePreventer {

    /**
     * 生成随机过期时间
     */
    public Duration randomTtl(Duration baseTtl) {
        long baseMillis = baseTtl.toMillis();
        // 随机偏移 ±10%
        double randomFactor = 0.9 + Math.random() * 0.2; // 0.9 ~ 1.1
        long randomMillis = (long) (baseMillis * randomFactor);
        return Duration.ofMillis(randomMillis);
    }

    /**
     * 缓存数据（带随机过期）
     */
    public <T> void putWithRandomTtl(MultiLevelCache cache, String key, T value, Duration baseTtl) {
        Duration ttl = randomTtl(baseTtl);
        cache.put(key, value, ttl);
        log.debug("缓存数据 (随机 TTL): key={}, ttl={}ms", key, ttl.toMillis());
    }
}
```

### 2. 分布式锁防护

```java
@Component
public class CacheLock {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOCK_PREFIX = "lock:";
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 获取分布式锁
     */
    public Boolean tryLock(String key, Duration expireTime) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();

        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, expireTime);

        if (Boolean.TRUE.equals(result)) {
            log.debug("获取锁成功: {}", key);
            return true;
        }

        return false;
    }

    /**
     * 释放分布式锁
     */
    public void releaseLock(String key, String lockValue) {
        String lockKey = LOCK_PREFIX + key;
        String currentValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
            log.debug("释放锁成功: {}", key);
        }
    }

    /**
     * 缓存雪崩防护包装
     */
    public <T> T getWithAvalanchePrevent(String key, Class<T> type, Supplier<T> loader) {
        // 1. 先查缓存
        T value = multiLevelCache.get(key, type, () -> null);

        if (value != null) {
            return value;
        }

        // 2. 获取锁
        String lockValue = UUID.randomUUID().toString();
        try {
            if (tryLock(key, LOCK_TIMEOUT)) {
                // 双重检查
                value = multiLevelCache.get(key, type, () -> null);
                if (value != null) {
                    return value;
                }

                // 加载数据
                value = loader.get();
                if (value != null) {
                    multiLevelCache.put(key, value, Duration.ofHours(1));
                }

                return value;
            } else {
                // 获取锁失败，等待后重试
                try {
                    Thread.sleep(100);
                    return getWithAvalanchePrevent(key, type, loader);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        } finally {
            releaseLock(key, lockValue);
        }
    }

    private MultiLevelCache multiLevelCache;
}
```

---

## 📊 缓存监控

### 1. 缓存统计

```java
@Component
public class CacheStats {

    private final Cache<String, Object> userCache;
    private final Cache<String, Object> menuCache;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getStatistics() {
        return CacheStatistics.builder()
            .l1Cache(collectL1Stats())
            .l2Cache(collectL2Stats())
            .build();
    }

    private L1CacheStatistics collectL1Stats() {
        CacheStats userStats = userCache.stats();
        CacheStats menuStats = menuCache.stats();

        return L1CacheStatistics.builder()
            .requestCount(userStats.requestCount() + menuStats.requestCount())
            .hitCount(userStats.hitCount() + menuStats.hitCount())
            .hitRate(userStats.hitRate())
            .missCount(userStats.missCount() + menuStats.missCount())
            .loadCount(userStats.loadCount() + menuStats.loadCount())
            .build();
    }

    private L2CacheStatistics collectL2Stats() {
        // Redis 统计信息
        RedisServerInfo info = redisTemplate.getConnectionFactory()
            .getConnection()
            .info("stats");

        return L2CacheStatistics.builder()
            .connectedClients(getInfoValue(info, "connected_clients"))
            .usedMemory(getInfoValue(info, "used_memory_human"))
            .hitRate(calculateL2HitRate())
            .build();
    }

    private double calculateL2HitRate() {
        // 实现 L2 命中率计算
        return 0.95; // 示例值
    }

    private String getInfoValue(RedisServerInfo info, String key) {
        // 解析 info 信息
        return "0"; // 示例值
    }

    @Data
    @Builder
    public static class CacheStatistics {
        private L1CacheStatistics l1Cache;
        private L2CacheStatistics l2Cache;
    }

    @Data
    @Builder
    public static class L1CacheStatistics {
        private long requestCount;
        private long hitCount;
        private double hitRate;
        private long missCount;
        private long loadCount;
    }

    @Data
    @Builder
    public static class L2CacheStatistics {
        private String connectedClients;
        private String usedMemory;
        private double hitRate;
    }
}
```

### 2. 缓存健康检查

```java
@RestController
@RequestMapping("/api/monitor/cache")
public class CacheHealthController {

    private final CacheStats cacheStats;

    @GetMapping("/health")
    public Result<CacheHealth> checkHealth() {
        CacheHealth health = new CacheHealth();

        // 检查 L1 缓存
        CacheStatistics stats = cacheStats.getStatistics();
        health.setL1HitRate(stats.getL1Cache().getHitRate());
        health.setL1Status(stats.getL1Cache().getHitRate() > 0.8 ? "UP" : "DEGRADED");

        // 检查 L2 缓存
        health.setL2HitRate(stats.getL2Cache().getHitRate());
        health.setL2Status(stats.getL2Cache().getHitRate() > 0.9 ? "UP" : "DEGRADED");

        // 综合状态
        if ("UP".equals(health.getL1Status()) && "UP".equals(health.getL2Status())) {
            health.setOverallStatus("UP");
            return Result.success(health);
        } else {
            health.setOverallStatus("DEGRADED");
            return Result.failed("缓存性能下降");
        }
    }

    @Data
    public static class CacheHealth {
        private String overallStatus;
        private String l1Status;
        private double l1HitRate;
        private String l2Status;
        private double l2HitRate;
    }
}
```

---

## 📝 使用示例

### 1. 业务代码

```java
@Service
public class UserService {

    private final MultiLevelCache cache;
    private final CachePenetrationPreventer penetrationPreventer;
    private final CacheAvalanchePreventer avalanchePreventer;

    @MultiLevelCached(
        key = "user:#{id}",
        type = UserDTO.class,
        ttl = 3600,
        useBloomFilter = true,
        preventPenetration = true,
        preventAvalanche = true
    )
    public UserDTO getUserById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 手动使用多级缓存
     */
    public UserDTO getUserByIdManual(Long id) {
        String key = "user:" + id;

        return penetrationPreventer.getWithPenetrationPrevent(
            key,
            UserDTO.class,
            () -> {
                User user = userMapper.selectById(id);
                return convertToDTO(user);
            }
        );
    }

    /**
     * 更新用户（清除缓存）
     */
    @CacheEvict(key = "user:#{user.id}")
    public void updateUser(User user) {
        userMapper.updateById(user);
    }
}
```

---

## ✅ 测试用例

### 1. 缓存命中率测试

```java
@SpringBootTest
public class MultiLevelCacheTest {

    @Autowired
    private MultiLevelCache cache;

    @Autowired
    private CacheStats cacheStats;

    @Test
    public void testCacheHitRate() {
        // 预热缓存
        for (int i = 0; i < 100; i++) {
            cache.put("key:" + i, "value:" + i, Duration.ofHours(1));
        }

        // 访问缓存
        for (int i = 0; i < 100; i++) {
            cache.get("key:" + i, String.class, () -> null);
        }

        // 检查命中率
        CacheStatistics stats = cacheStats.getStatistics();
        assertThat(stats.getL1Cache().getHitRate()).isGreaterThan(0.9);
    }
}
```

---

## 📚 参考资料

1. [Caffeine 缓存指南](https://github.com/ben-manes/caffeine)
2. [Redis 缓存最佳实践](https://redis.io/docs/manual/eviction/)
3. [多级缓存架构设计](https://tech.meituan.com/2018/01/19/distributed-cache.html)

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ 多级缓存架构即将完成！** ฅ'ω'ฅ
