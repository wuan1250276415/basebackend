# Design Document

## Overview

本设计文档描述了 basebackend-cache 模块的增强方案。该模块将从基础的 Redis 操作工具升级为功能完善的企业级分布式缓存解决方案，提供注解驱动、多级缓存、监控指标、缓存预热、高级分布式锁等特性。

设计遵循以下原则：
- **易用性**: 提供注解和模板简化开发
- **可观测性**: 集成 Micrometer 提供完整的监控指标
- **高性能**: 支持多级缓存减少网络开销
- **高可用**: 提供容错和降级机制
- **可扩展性**: 支持自定义序列化和缓存策略

## Architecture

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│  (使用 @Cacheable, @CacheEvict 等注解或直接调用服务)          │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Cache Abstraction Layer                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Cache Manager│  │Cache Template│  │Cache Advisor │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Multi-Level Cache Layer                    │
│  ┌──────────────┐         ┌──────────────┐                  │
│  │ Local Cache  │────────▶│ Redis Cache  │                  │
│  │  (Caffeine)  │         │  (Redisson)  │                  │
│  └──────────────┘         └──────────────┘                  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Observability Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Metrics    │  │    Logging   │  │   Tracing    │      │
│  │ (Micrometer) │  │   (SLF4J)    │  │   (Brave)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 模块结构

```
basebackend-cache/
├── config/                    # 配置类
│   ├── CacheAutoConfiguration.java
│   ├── CacheProperties.java
│   ├── MultiLevelCacheConfig.java
│   └── CacheMetricsConfig.java
├── manager/                   # 缓存管理器
│   ├── MultiLevelCacheManager.java
│   ├── CacheWarmingManager.java
│   └── CacheEvictionManager.java
├── service/                   # 服务层
│   ├── RedisService.java (已存在，需增强)
│   ├── CacheService.java
│   └── CacheMetricsService.java
├── template/                  # 缓存模板
│   ├── CacheAsideTemplate.java
│   ├── WriteThroughTemplate.java
│   └── WriteBehindTemplate.java
├── aspect/                    # AOP 切面
│   ├── CacheAspect.java
│   └── CacheMetricsAspect.java
├── annotation/                # 注解
│   ├── Cacheable.java
│   ├── CacheEvict.java
│   ├── CachePut.java
│   └── DistributedLock.java
├── lock/                      # 分布式锁
│   ├── DistributedLockService.java
│   ├── FairLockService.java
│   └── MultiLockService.java
├── structure/                 # 分布式数据结构
│   ├── DistributedMapService.java
│   ├── DistributedQueueService.java
│   └── DistributedSetService.java
├── serializer/                # 序列化器
│   ├── CacheSerializer.java
│   ├── JsonCacheSerializer.java
│   ├── ProtobufCacheSerializer.java
│   └── KryoCacheSerializer.java
├── warming/                   # 缓存预热
│   ├── CacheWarmingTask.java
│   └── CacheWarmingExecutor.java
├── metrics/                   # 指标收集
│   ├── CacheMetrics.java
│   ├── CacheStatistics.java
│   └── CacheMetricsCollector.java
├── exception/                 # 异常处理
│   ├── CacheException.java
│   ├── CacheSerializationException.java
│   └── CacheLockException.java
└── util/                      # 工具类
    ├── RedissonLockUtil.java (已存在，需增强)
    ├── CacheKeyGenerator.java
    └── BloomFilterUtil.java
```

## Components and Interfaces

### 1. Cache Manager

**MultiLevelCacheManager**
```java
public class MultiLevelCacheManager {
    // 获取缓存，先查本地再查 Redis
    <T> T get(String key, Class<T> type);
    
    // 设置缓存，同时更新本地和 Redis
    void set(String key, Object value, Duration ttl);
    
    // 删除缓存，同时清除本地和 Redis
    void evict(String key);
    
    // 清空所有缓存
    void clear();
    
    // 获取缓存统计信息
    CacheStatistics getStatistics();
}
```

**CacheWarmingManager**
```java
public class CacheWarmingManager {
    // 注册预热任务
    void registerWarmingTask(CacheWarmingTask task);
    
    // 执行所有预热任务
    void executeWarmingTasks();
    
    // 获取预热进度
    WarmingProgress getProgress();
}
```

### 2. Cache Service

**CacheService**
```java
public interface CacheService {
    // Cache-Aside 模式
    <T> T getOrLoad(String key, Supplier<T> loader, Duration ttl);
    
    // 批量获取
    <T> Map<String, T> multiGet(Set<String> keys, Class<T> type);
    
    // 批量设置
    void multiSet(Map<String, Object> entries, Duration ttl);
    
    // 模式匹配删除
    long deleteByPattern(String pattern);
}
```

**CacheMetricsService**
```java
public interface CacheMetricsService {
    // 记录缓存命中
    void recordHit(String cacheName);
    
    // 记录缓存未命中
    void recordMiss(String cacheName);
    
    // 记录操作延迟
    void recordLatency(String operation, long milliseconds);
    
    // 获取缓存统计
    CacheStatistics getStatistics(String cacheName);
}
```

### 3. Cache Template

**CacheAsideTemplate**
```java
public class CacheAsideTemplate {
    // 查询缓存，未命中时从数据源加载
    <T> T get(String key, Supplier<T> dataLoader, Duration ttl);
    
    // 更新数据源并删除缓存
    <T> T update(String key, Function<T, T> updater);
}
```

**WriteThroughTemplate**
```java
public class WriteThroughTemplate {
    // 同步更新缓存和数据源
    <T> void set(String key, T value, Consumer<T> dataPersister);
}
```

**WriteBehindTemplate**
```java
public class WriteBehindTemplate {
    // 更新缓存，异步批量更新数据源
    <T> void set(String key, T value);
    
    // 刷新待写入的数据
    void flush();
}
```

### 4. Distributed Lock Service

**DistributedLockService**
```java
public interface DistributedLockService {
    // 尝试获取锁
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);
    
    // 释放锁
    void unlock(String lockKey);
    
    // 执行带锁的操作
    <T> T executeWithLock(String lockKey, Supplier<T> action, long waitTime, long leaseTime);
    
    // 获取公平锁
    RLock getFairLock(String lockKey);
    
    // 获取联锁
    RLock getMultiLock(String... lockKeys);
    
    // 获取红锁
    RLock getRedLock(String lockKey);
}
```

### 5. Distributed Data Structures

**DistributedMapService**
```java
public interface DistributedMapService {
    <K, V> RMap<K, V> getMap(String name);
    <K, V> void put(String mapName, K key, V value);
    <K, V> V get(String mapName, K key);
    <K> void remove(String mapName, K key);
}
```

**DistributedQueueService**
```java
public interface DistributedQueueService {
    <T> RQueue<T> getQueue(String name);
    <T> void offer(String queueName, T element);
    <T> T poll(String queueName);
    long size(String queueName);
}
```

### 6. Cache Serializer

**CacheSerializer Interface**
```java
public interface CacheSerializer {
    byte[] serialize(Object obj) throws CacheSerializationException;
    <T> T deserialize(byte[] data, Class<T> type) throws CacheSerializationException;
    String getType();
}
```

## Data Models

### CacheProperties
```java
@ConfigurationProperties(prefix = "basebackend.cache")
public class CacheProperties {
    private boolean enabled = true;
    private MultiLevel multiLevel = new MultiLevel();
    private Metrics metrics = new Metrics();
    private Warming warming = new Warming();
    private Serialization serialization = new Serialization();
    private Resilience resilience = new Resilience();
    
    public static class MultiLevel {
        private boolean enabled = false;
        private int localMaxSize = 1000;
        private Duration localTtl = Duration.ofMinutes(5);
        private String evictionPolicy = "LRU";
    }
    
    public static class Metrics {
        private boolean enabled = true;
        private double lowHitRateThreshold = 0.5;
    }
    
    public static class Warming {
        private boolean enabled = false;
        private List<WarmingTask> tasks = new ArrayList<>();
    }
    
    public static class Serialization {
        private String type = "json"; // json, protobuf, kryo
    }
    
    public static class Resilience {
        private boolean fallbackEnabled = true;
        private Duration timeout = Duration.ofSeconds(3);
        private int circuitBreakerThreshold = 5;
    }
}
```

### CacheStatistics
```java
@Data
public class CacheStatistics {
    private String cacheName;
    private long hitCount;
    private long missCount;
    private long totalCount;
    private double hitRate;
    private long evictionCount;
    private long size;
    private long averageLoadTime;
    private Instant lastAccessTime;
}
```

### CacheWarmingTask
```java
@Data
public class CacheWarmingTask {
    private String name;
    private int priority;
    private Supplier<Map<String, Object>> dataLoader;
    private Duration ttl;
    private boolean async;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

在编写具体属性之前，让我分析需求中的可测试性：

**需求 1 - 注解驱动缓存**
- 1.1-1.3: 可测试为属性 - 验证注解触发正确的缓存操作
- 1.4-1.5: 可测试为属性 - 验证表达式解析和条件判断

**需求 2 - 缓存监控**
- 2.1-2.2: 可测试为属性 - 验证指标记录和统计
- 2.3-2.5: 可测试为示例 - 特定场景的日志和错误处理

**需求 3 - 多级缓存**
- 3.1-3.3: 可测试为属性 - 验证缓存层级查询和同步
- 3.4-3.5: 可测试为属性 - 验证淘汰策略和失效同步

**需求 4 - 缓存预热**
- 4.1-4.4: 可测试为示例 - 系统启动时的特定行为
- 4.5: 可测试为属性 - 验证数据加载

**需求 5 - 高级分布式锁**
- 5.1-5.5: 可测试为属性 - 验证各种锁的行为

**需求 6 - 缓存模式**
- 6.1-6.5: 可测试为属性 - 验证缓存模式的正确性

**需求 7 - 分布式数据结构**
- 7.1-7.5: 可测试为属性 - 验证数据结构操作

**需求 8 - 生命周期管理**
- 8.1-8.5: 可测试为属性 - 验证缓存管理操作

**需求 9 - 容错能力**
- 9.1-9.5: 可测试为属性 - 验证故障场景下的行为

**需求 10 - 序列化**
- 10.1-10.3: 可测试为属性 - 验证序列化往返
- 10.4-10.5: 可测试为属性 - 验证错误处理

**冗余分析**：
- Property 1 和 Property 2 可以合并为"注解触发正确的缓存操作"
- Property 10-12 关于序列化的可以合并为"序列化往返一致性"
- Property 15-17 关于分布式锁可以合并为"锁的互斥性和公平性"

### Property 1: 注解驱动的缓存操作正确性
*For any* 带有缓存注解的方法和任意输入参数，当方法被调用时，缓存操作应该按照注解配置正确执行（@Cacheable 缓存结果、@CacheEvict 清除缓存、@CachePut 更新缓存）
**Validates: Requirements 1.1, 1.2, 1.3**

### Property 2: SpEL 表达式解析一致性
*For any* 缓存注解中的 SpEL 表达式和方法参数，表达式解析生成的缓存键应该对于相同的参数值保持一致
**Validates: Requirements 1.4**

### Property 3: 缓存指标记录完整性
*For any* 缓存操作（get、set、evict），操作执行后应该正确更新对应的指标计数器（命中数、未命中数、操作数）
**Validates: Requirements 2.1, 2.2**

### Property 4: 多级缓存查询顺序
*For any* 缓存键，当启用多级缓存时，查询应该先检查本地缓存，本地未命中时再查询 Redis，且 Redis 命中时应同步到本地缓存
**Validates: Requirements 3.1, 3.2**

### Property 5: 多级缓存更新一致性
*For any* 缓存键和值，更新操作应该同时更新本地缓存和 Redis，确保两级缓存的数据一致性
**Validates: Requirements 3.3**

### Property 6: 本地缓存 LRU 淘汰正确性
*For any* 本地缓存，当缓存条目数超过配置的最大容量时，应该按照 LRU 策略淘汰最久未使用的条目
**Validates: Requirements 3.4**

### Property 7: Cache-Aside 模式正确性
*For any* 缓存键和数据加载函数，使用 Cache-Aside 模式时，如果缓存未命中应该调用加载函数并将结果缓存，如果缓存命中应该直接返回缓存值而不调用加载函数
**Validates: Requirements 6.1**

### Property 8: 缓存穿透防护有效性
*For any* 不存在的键，当启用布隆过滤器或空值缓存时，重复查询不应该每次都穿透到数据源
**Validates: Requirements 6.4**

### Property 9: 分布式锁互斥性
*For any* 锁键，在任意时刻最多只有一个线程能够持有该锁，其他线程必须等待锁释放
**Validates: Requirements 5.1, 5.2, 5.3**

### Property 10: 锁自动释放正确性
*For any* 分布式锁，当锁的租约时间到期时，即使持有者未主动释放，锁也应该自动释放并允许其他线程获取
**Validates: Requirements 5.4**

### Property 11: 读写锁并发正确性
*For any* 读写锁，应该允许多个线程同时持有读锁，但写锁与任何其他锁（读锁或写锁）互斥
**Validates: Requirements 5.5**

### Property 12: 分布式 Map 操作原子性
*For any* 分布式 Map 和键值对，put 和 get 操作应该是原子的，get 操作应该返回最近一次 put 的值或 null
**Validates: Requirements 7.1**

### Property 13: 分布式 Queue FIFO 顺序
*For any* 分布式队列和元素序列，按顺序 offer 的元素应该按相同顺序被 poll 出来
**Validates: Requirements 7.2**

### Property 14: 缓存淘汰策略有效性
*For any* 缓存，当缓存大小达到最大容量时，新的缓存条目应该能够成功添加，且总条目数不超过最大容量
**Validates: Requirements 8.2**

### Property 15: 模式匹配删除完整性
*For any* 缓存键模式，批量删除操作应该删除所有匹配该模式的键，且不删除不匹配的键
**Validates: Requirements 8.3**

### Property 16: Redis 故障降级正确性
*For any* 缓存操作，当 Redis 不可用时，如果启用降级，操作应该降级到本地缓存或直接访问数据源，而不是抛出异常
**Validates: Requirements 9.1, 9.3**

### Property 17: 序列化往返一致性
*For any* 可序列化对象和序列化器类型（JSON、Protobuf、Kryo），序列化后再反序列化应该得到与原对象等价的对象
**Validates: Requirements 10.1, 10.2, 10.3**

### Property 18: 序列化错误处理安全性
*For any* 损坏的序列化数据，反序列化操作应该捕获异常、记录错误日志、返回 null，而不是抛出未捕获的异常
**Validates: Requirements 10.5**

## Error Handling

### 异常层次结构
```
CacheException (基础异常)
├── CacheSerializationException (序列化异常)
├── CacheLockException (锁异常)
├── CacheConnectionException (连接异常)
└── CacheConfigurationException (配置异常)
```

### 错误处理策略

1. **Redis 连接失败**
   - 记录错误日志
   - 如果启用降级，切换到本地缓存
   - 如果未启用降级，返回 null 或抛出异常

2. **序列化/反序列化失败**
   - 记录详细错误信息和数据摘要
   - 删除损坏的缓存数据
   - 返回 null

3. **锁获取超时**
   - 记录警告日志
   - 返回 false 或抛出 CacheLockException

4. **缓存操作超时**
   - 记录警告日志
   - 触发熔断器（如果配置）
   - 降级处理

5. **配置错误**
   - 启动时验证配置
   - 抛出 CacheConfigurationException 阻止启动

## Testing Strategy

### Unit Testing

使用 JUnit 5 和 Mockito 进行单元测试：

1. **配置类测试**
   - 测试配置属性的加载和验证
   - 测试 Bean 的创建和依赖注入

2. **服务类测试**
   - Mock Redis 和 Redisson 客户端
   - 测试各种缓存操作的逻辑
   - 测试异常处理和降级逻辑

3. **序列化器测试**
   - 测试各种数据类型的序列化和反序列化
   - 测试边界情况（null、空对象、大对象）

4. **工具类测试**
   - 测试键生成器的正确性
   - 测试布隆过滤器的准确性

### Property-Based Testing

使用 **jqwik** 作为 Java 的 property-based testing 框架。每个属性测试应该运行至少 100 次迭代。

1. **注解驱动测试**
   - 生成随机方法参数
   - 验证缓存注解的行为
   - **Feature: cache-enhancement, Property 1: 注解驱动的缓存操作正确性**

2. **多级缓存测试**
   - 生成随机的缓存键和值
   - 验证查询顺序和同步逻辑
   - **Feature: cache-enhancement, Property 4: 多级缓存查询顺序**
   - **Feature: cache-enhancement, Property 5: 多级缓存更新一致性**

3. **分布式锁测试**
   - 生成随机的锁键和并发场景
   - 验证锁的互斥性和公平性
   - **Feature: cache-enhancement, Property 9: 分布式锁互斥性**
   - **Feature: cache-enhancement, Property 11: 读写锁并发正确性**

4. **序列化测试**
   - 生成随机的对象实例
   - 验证序列化往返一致性
   - **Feature: cache-enhancement, Property 17: 序列化往返一致性**

5. **缓存模式测试**
   - 生成随机的缓存操作序列
   - 验证 Cache-Aside、Write-Through 等模式的正确性
   - **Feature: cache-enhancement, Property 7: Cache-Aside 模式正确性**

6. **数据结构测试**
   - 生成随机的操作序列
   - 验证分布式 Map、Queue、Set 的行为
   - **Feature: cache-enhancement, Property 12: 分布式 Map 操作原子性**
   - **Feature: cache-enhancement, Property 13: 分布式 Queue FIFO 顺序**

### Integration Testing

1. **Redis 集成测试**
   - 使用 Testcontainers 启动 Redis
   - 测试真实的 Redis 操作

2. **多级缓存集成测试**
   - 测试本地缓存和 Redis 的协同工作
   - 测试缓存失效和同步

3. **并发测试**
   - 使用多线程测试分布式锁
   - 测试高并发场景下的缓存一致性

4. **性能测试**
   - 测试缓存操作的延迟
   - 测试多级缓存的性能提升

### Test Configuration

```yaml
# application-test.yml
basebackend:
  cache:
    enabled: true
    multi-level:
      enabled: true
      local-max-size: 100
      local-ttl: 1m
    metrics:
      enabled: true
    serialization:
      type: json
    resilience:
      fallback-enabled: true
      timeout: 1s
```

## Dependencies

需要在 pom.xml 中添加以下依赖：

```xml
<!-- Caffeine for local cache -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- Micrometer for metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>

<!-- Spring Boot AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Protobuf (optional) -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <optional>true</optional>
</dependency>

<!-- Kryo (optional) -->
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
    <optional>true</optional>
</dependency>

<!-- Bloom Filter -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
</dependency>

<!-- jqwik for property-based testing -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers for integration testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

## Performance Considerations

1. **本地缓存大小**: 默认 1000 条，可根据内存情况调整
2. **Redis 连接池**: 使用 Redisson 的连接池配置
3. **序列化性能**: JSON < Protobuf < Kryo（性能递增，可读性递减）
4. **批量操作**: 使用 pipeline 或 multiGet/multiSet 减少网络往返
5. **异步操作**: Write-Behind 模式使用异步批量写入

## Security Considerations

1. **缓存键命名**: 使用命名空间避免键冲突
2. **敏感数据**: 不缓存敏感信息，或使用加密序列化器
3. **访问控制**: 通过 Redis ACL 限制访问权限
4. **注入攻击**: 验证缓存键，防止注入攻击
