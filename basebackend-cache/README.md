# BaseBackend Cache Module

企业级分布式缓存解决方案，提供注解驱动、多级缓存、监控指标、缓存预热、高级分布式锁等特性。

## 📋 目录

- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [核心功能](#核心功能)
- [配置说明](#配置说明)
- [使用文档](#使用文档)
- [最佳实践](#最佳实践)
- [性能优化](#性能优化)
- [故障排查](#故障排查)

## ✨ 功能特性

### 核心功能

- ✅ **注解驱动缓存**: 使用 `@Cacheable`、`@CacheEvict`、`@CachePut` 简化缓存操作
- ✅ **多级缓存**: 本地缓存（Caffeine）+ 分布式缓存（Redis）
- ✅ **智能键生成**: 支持多种键生成策略（简单、哈希、JSON、版本、租户等）
- ✅ **缓存指标**: 集成 Micrometer，提供命中率、延迟等监控指标
- ✅ **缓存预热**: 应用启动时自动加载热点数据
- ✅ **高级分布式锁**: 支持可重入锁、公平锁、联锁、红锁、读写锁
- ✅ **缓存模式**: Cache-Aside、Write-Through、Write-Behind
- ✅ **分布式数据结构**: Map、Queue、Set、List、SortedSet
- ✅ **容错降级**: 熔断器、超时控制、自动恢复
- ✅ **灵活序列化**: 支持 JSON、Protobuf、Kryo

### 企业级特性

- 🔒 **线程安全**: 所有操作都是线程安全的
- 📊 **可观测性**: 完整的日志、指标和追踪
- 🛡️ **高可用**: 熔断器、降级、自动恢复
- ⚡ **高性能**: 多级缓存、批量操作、Pipeline
- 🔧 **易扩展**: 支持自定义序列化器、缓存策略
- 📝 **完整文档**: 详细的使用指南和示例

## 🚀 快速开始

### 1. 添加依赖

在项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-cache</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 配置 Redis

在 `application.yml` 中配置 Redis 连接：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password: 

basebackend:
  cache:
    enabled: true
```

### 3. 使用注解

在 Service 方法上添加缓存注解：

```java
@Service
public class UserService {
    
    @Cacheable(key = "user:#{#userId}")
    public User getUserById(Long userId) {
        // 查询数据库
        return userRepository.findById(userId);
    }
    
    @CacheEvict(key = "user:#{#user.id}")
    public void updateUser(User user) {
        // 更新数据库
        userRepository.save(user);
    }
}
```

### 4. 编程式使用

注入 `CacheService` 或 `RedisService`：

```java
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final CacheService cacheService;
    
    public Product getProduct(Long productId) {
        String key = "product:" + productId;
        return cacheService.getOrLoad(
            key,
            () -> productRepository.findById(productId),
            Duration.ofHours(1)
        );
    }
}
```

## 🎯 核心功能

### 1. 注解驱动缓存

使用注解简化缓存操作，支持 SpEL 表达式：

```java
// 缓存方法返回值
@Cacheable(key = "order:#{#orderId}", ttl = 3600)
public Order getOrder(Long orderId) { }

// 清除缓存
@CacheEvict(key = "order:#{#orderId}")
public void deleteOrder(Long orderId) { }

// 更新缓存
@CachePut(key = "order:#{#order.id}")
public Order updateOrder(Order order) { }

// 条件缓存
@Cacheable(key = "user:#{#userId}", condition = "#userId > 0")
public User getUser(Long userId) { }
```

详细文档：[注解使用指南](USAGE_EXAMPLES.md#注解驱动缓存)

### 2. 多级缓存

结合本地缓存和 Redis，提升性能并降低网络开销：

```yaml
basebackend:
  cache:
    multi-level:
      enabled: true
      local-max-size: 1000
      local-ttl: 5m
      eviction-policy: LRU
```

查询顺序：本地缓存 → Redis → 数据源

详细文档：[多级缓存配置](USAGE_EXAMPLES.md#多级缓存)

### 3. 缓存预热

应用启动时自动加载热点数据：

```java
@Configuration
public class CacheWarmingConfig {
    
    @Autowired
    private CacheWarmingManager warmingManager;
    
    @PostConstruct
    public void registerWarmingTasks() {
        CacheWarmingTask task = CacheWarmingTask.builder()
            .name("warm-hot-products")
            .priority(1)
            .dataLoader(() -> loadHotProducts())
            .ttl(Duration.ofHours(2))
            .async(true)
            .build();
        
        warmingManager.registerWarmingTask(task);
    }
}
```

详细文档：[CACHE_WARMING_USAGE.md](CACHE_WARMING_USAGE.md)

### 4. 分布式锁

支持多种锁类型，防止并发问题：

```java
// 注解方式
@DistributedLock(key = "order:#{#orderId}", waitTime = 5, leaseTime = 30)
public void processOrder(Long orderId) { }

// 编程方式
lockService.executeWithLock("payment:" + orderId, () -> {
    // 业务逻辑
    return result;
}, 5, 30);

// 读写锁
@DistributedLock(key = "data:read", lockType = LockType.READ)
public Data readData() { }

@DistributedLock(key = "data:write", lockType = LockType.WRITE)
public void writeData(Data data) { }
```

详细文档：[DISTRIBUTED_LOCK_USAGE.md](DISTRIBUTED_LOCK_USAGE.md)

### 5. 缓存键生成器

提供强大的缓存键生成工具，支持多种场景：

```java
@Autowired
private CacheKeyGenerator keyGenerator;

// 简单键
String key = keyGenerator.generateSimpleKey("cache", "user", "123");

// 版本控制键
String versionedKey = keyGenerator.generateVersionedKey("cache", "api", "data", "1.0");

// 租户键
String tenantKey = keyGenerator.generateTenantKey("tenant001", "cache", "user", "123");

// 分页键
String pageKey = keyGenerator.generatePageKey("cache", "userList", 1, 20);

// JSON 键（复杂对象）
String jsonKey = keyGenerator.generateJsonKey("cache", "query", queryObject);

// 批量键
List<String> keys = keyGenerator.generateBatchKeys("cache", "user", userIds);
```

详细文档：[CACHE_KEY_GENERATOR_USAGE.md](CACHE_KEY_GENERATOR_USAGE.md)

### 6. 缓存模式

提供常见缓存模式的模板实现：

```java
// Cache-Aside 模式
cacheAsideTemplate.get(key, () -> loadFromDB(), Duration.ofHours(1));

// Write-Through 模式
writeThroughTemplate.set(key, value, v -> saveToDB(v));

// Write-Behind 模式
writeBehindTemplate.set(key, value);
```

详细文档：[缓存模式使用](USAGE_EXAMPLES.md#缓存模式)

### 7. 分布式数据结构

使用分布式数据结构在多个节点间共享数据：

```java
// 分布式 Map
distributedMapService.put("myMap", "key", "value");

// 分布式 Queue
distributedQueueService.offer("myQueue", element);

// 分布式 Set
distributedSetService.add("mySet", element);
```

详细文档：[分布式数据结构](USAGE_EXAMPLES.md#分布式数据结构)

### 8. 容错降级

自动处理 Redis 故障，确保系统可用性：

```yaml
basebackend:
  cache:
    resilience:
      fallback-enabled: true
      timeout: 3s
      circuit-breaker:
        enabled: true
        failure-threshold: 5
```

详细文档：[FAULT_TOLERANCE_USAGE.md](FAULT_TOLERANCE_USAGE.md)

## ⚙️ 配置说明

### 基础配置

```yaml
basebackend:
  cache:
    # 启用缓存模块
    enabled: true
    
    # 多级缓存
    multi-level:
      enabled: false
      local-max-size: 1000
      local-ttl: 5m
    
    # 指标收集
    metrics:
      enabled: true
      low-hit-rate-threshold: 0.5
    
    # 序列化方式
    serialization:
      type: json  # json, protobuf, kryo
```

### 完整配置

查看完整配置模板：[application-cache.yml](src/main/resources/application-cache.yml)

### 配置优先级

1. 应用配置文件（application.yml）
2. 环境变量
3. 系统属性
4. 默认值

## 📚 使用文档

### 核心文档

- [使用示例](USAGE_EXAMPLES.md) - 常见使用场景和代码示例
- [最佳实践](BEST_PRACTICES.md) - 推荐的使用方式和注意事项
- [配置示例](CONFIG_EXAMPLES.md) - 不同场景的配置示例
- [配置参考](src/main/resources/application-cache.yml) - 完整的配置选项

### 专题文档

- [缓存预热使用指南](CACHE_WARMING_USAGE.md) - 应用启动时预加载热点数据
- [分布式锁使用指南](DISTRIBUTED_LOCK_USAGE.md) - 多种锁类型和使用方式
- [容错降级使用指南](FAULT_TOLERANCE_USAGE.md) - 熔断器、超时控制、自动恢复
- [异常处理指南](EXCEPTION_HANDLING_GUIDE.md) - 异常层次结构和处理策略
- [集成测试说明](INTEGRATION_TESTS_README.md) - 使用 Testcontainers 进行集成测试

## 💡 最佳实践

### 1. 缓存键设计

```java
// ✅ 好的实践：使用命名空间和清晰的层次结构
"user:profile:123"
"product:detail:456"
"order:list:user:789"

// ❌ 避免：模糊的键名
"u123"
"data"
```

### 2. TTL 设置

```java
// 根据数据特性设置合理的 TTL
@Cacheable(key = "config:#{#key}", ttl = 86400)  // 配置数据：24小时
public Config getConfig(String key) { }

@Cacheable(key = "user:#{#id}", ttl = 3600)  // 用户数据：1小时
public User getUser(Long id) { }

@Cacheable(key = "stock:#{#id}", ttl = 60)  // 库存数据：1分钟
public Stock getStock(Long id) { }
```

### 3. 缓存粒度

```java
// ✅ 好的实践：细粒度缓存
@Cacheable(key = "user:#{#userId}")
public User getUser(Long userId) { }

// ❌ 避免：粗粒度缓存
@Cacheable(key = "all:users")
public List<User> getAllUsers() { }  // 数据量大，更新频繁
```

### 4. 异常处理

```java
try {
    cacheService.set(key, value);
} catch (CacheSerializationException e) {
    log.error("Serialization failed", e);
    // 处理序列化失败
} catch (CacheConnectionException e) {
    log.error("Redis unavailable", e);
    // 处理连接失败
}
```

### 5. 监控指标

```java
// 定期检查缓存指标
CacheStatistics stats = metricsService.getStatistics("myCache");
log.info("Hit rate: {}, Miss rate: {}", 
    stats.getHitRate(), 
    stats.getMissRate());
```

更多最佳实践：[BEST_PRACTICES.md](BEST_PRACTICES.md)

## ⚡ 性能优化

### 1. 启用多级缓存

减少网络往返，提升响应速度：

```yaml
basebackend:
  cache:
    multi-level:
      enabled: true
      local-max-size: 1000
```

### 2. 使用批量操作

减少网络开销：

```java
// 批量获取
Map<String, User> users = cacheService.multiGet(userIds, User.class);

// 批量设置
cacheService.multiSet(dataMap, Duration.ofHours(1));
```

### 3. 选择合适的序列化器

```yaml
basebackend:
  cache:
    serialization:
      type: kryo  # 性能: kryo > protobuf > json
```

### 4. 使用 Pipeline

```java
redisService.executePipelined(operations -> {
    operations.set("key1", "value1");
    operations.set("key2", "value2");
    operations.set("key3", "value3");
});
```

### 5. 缓存预热

避免冷启动，提升用户体验：

```yaml
basebackend:
  cache:
    warming:
      enabled: true
      async: true
```

## 🔧 故障排查

### 常见问题

#### 1. 缓存未生效

**症状**：方法每次都执行，缓存没有命中

**排查步骤**：
1. 检查 `basebackend.cache.enabled: true`
2. 确认 Redis 连接正常
3. 检查缓存键是否正确生成
4. 查看日志中的缓存操作记录

```bash
# 查看缓存日志
grep "cache" application.log
```

#### 2. Redis 连接失败

**症状**：`CacheConnectionException` 异常

**解决方案**：
1. 检查 Redis 服务是否运行
2. 验证连接配置（host、port、password）
3. 检查网络连接
4. 启用降级机制

```yaml
basebackend:
  cache:
    resilience:
      fallback-enabled: true
```

#### 3. 序列化失败

**症状**：`CacheSerializationException` 异常

**解决方案**：
1. 确保对象实现 `Serializable`
2. 检查是否有循环引用
3. 尝试更换序列化器
4. 查看详细错误日志

#### 4. 锁获取失败

**症状**：`CacheLockException` 异常

**解决方案**：
1. 增加等待时间 `waitTime`
2. 检查是否有死锁
3. 确认锁键唯一性
4. 查看锁持有情况

#### 5. 命中率低

**症状**：缓存命中率低于预期

**排查步骤**：
1. 检查 TTL 是否过短
2. 确认缓存键生成逻辑
3. 查看缓存淘汰策略
4. 分析访问模式

```java
// 查看缓存统计
CacheStatistics stats = metricsService.getStatistics("myCache");
log.info("Hit rate: {}, Total: {}", stats.getHitRate(), stats.getTotalCount());
```

### 日志级别

调整日志级别以获取更多信息：

```yaml
logging:
  level:
    com.basebackend.cache: DEBUG
```

### 监控指标

通过 Actuator 查看缓存指标：

```bash
# 查看缓存指标
curl http://localhost:8080/actuator/metrics/cache.hits
curl http://localhost:8080/actuator/metrics/cache.misses
```

## 🧪 测试

### 单元测试

```java
@SpringBootTest
class CacheServiceTest {
    
    @Autowired
    private CacheService cacheService;
    
    @Test
    void testCacheOperations() {
        cacheService.set("test:key", "value", Duration.ofMinutes(5));
        String value = cacheService.get("test:key", String.class);
        assertEquals("value", value);
    }
}
```

### 集成测试

使用 Testcontainers 进行集成测试：

```java
@SpringBootTest
@Testcontainers
class CacheIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
    
    @Test
    void testRealRedisOperations() {
        // 测试真实的 Redis 操作
    }
}
```

详细文档：[INTEGRATION_TESTS_README.md](INTEGRATION_TESTS_README.md)

## 📊 性能指标

### 基准测试结果

| 操作 | 本地缓存 | Redis | 多级缓存 |
|------|---------|-------|---------|
| 读取 | < 1ms | 2-5ms | < 1ms (命中本地) |
| 写入 | < 1ms | 3-8ms | 3-8ms |
| 批量读取(100) | < 5ms | 20-50ms | < 5ms (命中本地) |
| 批量写入(100) | < 5ms | 30-80ms | 30-80ms |

### 缓存命中率

- 启用多级缓存：85-95%
- 仅使用 Redis：70-85%
- 启用预热：90-98%

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

## 📄 许可证

本项目采用 MIT 许可证。

## 📞 支持

如有问题或需要帮助，请：

1. 查看[使用文档](#使用文档)
2. 查看[故障排查](#故障排查)
3. 提交 Issue
4. 联系技术支持

---

**版本**: 1.0.0  
**最后更新**: 2025-11-20


## 📚 完整文档列表

### 核心功能文档
- [使用示例](USAGE_EXAMPLES.md) - 各种功能的使用示例
- [配置示例](CONFIG_EXAMPLES.md) - 详细的配置说明
- [最佳实践](BEST_PRACTICES.md) - 使用建议和最佳实践

### 专项功能文档
- [缓存键生成器使用指南](CACHE_KEY_GENERATOR_USAGE.md) - 多种键生成策略详解
- [缓存预热使用指南](CACHE_WARMING_USAGE.md) - 缓存预热配置和使用
- [分布式锁使用指南](DISTRIBUTED_LOCK_USAGE.md) - 分布式锁的各种用法
- [容错降级使用指南](FAULT_TOLERANCE_USAGE.md) - 容错和降级策略
- [异常处理指南](EXCEPTION_HANDLING_GUIDE.md) - 异常处理最佳实践

### 测试文档
- [集成测试说明](INTEGRATION_TESTS_README.md) - 集成测试指南

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。
