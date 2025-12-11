# 缓存模块最佳实践指南

本文档提供使用 basebackend-cache 模块的最佳实践和注意事项。

## 目录

- [缓存设计原则](#缓存设计原则)
- [缓存键设计](#缓存键设计)
- [TTL 设置策略](#ttl-设置策略)
- [缓存粒度控制](#缓存粒度控制)
- [并发控制](#并发控制)
- [性能优化](#性能优化)
- [监控和运维](#监控和运维)
- [常见陷阱](#常见陷阱)

## 缓存设计原则

### 1. 只缓存必要的数据

**✅ 好的实践**：
```java
// 缓存热点数据
@Cacheable(key = "hot:product:#{#productId}")
public Product getHotProduct(Long productId) {
    return productRepository.findById(productId).orElse(null);
}
```

**❌ 避免**：
```java
// 不要缓存所有数据
@Cacheable(key = "all:products")
public List<Product> getAllProducts() {
    return productRepository.findAll(); // 数据量大，更新频繁
}
```

### 2. 缓存不可变或变化缓慢的数据

**适合缓存**：
- 配置数据
- 字典数据
- 用户基本信息
- 产品详情
- 分类信息

**不适合缓存**：
- 实时库存
- 订单状态（频繁变化）
- 支付状态
- 实时统计数据

### 3. 设置合理的过期时间

```java
// 根据数据特性设置 TTL
@Cacheable(key = "config:#{#key}", ttl = 86400)  // 配置：24小时
public String getConfig(String key) { }

@Cacheable(key = "user:#{#id}", ttl = 3600)  // 用户：1小时
public User getUser(Long id) { }

@Cacheable(key = "stock:#{#id}", ttl = 60)  // 库存：1分钟
public Integer getStock(Long id) { }
```

### 4. 考虑缓存一致性

**强一致性场景**：
```java
// 更新时立即清除缓存
@CacheEvict(key = "user:#{#user.id}")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

**最终一致性场景**：
```java
// 使用较短的 TTL，允许短暂的不一致
@Cacheable(key = "product:#{#id}", ttl = 300)
public Product getProduct(Long id) {
    return productRepository.findById(id).orElse(null);
}
```

## 缓存键设计

### 1. 使用清晰的命名空间

**✅ 好的实践**：
```java
// 使用层次化的键名
"user:profile:123"
"product:detail:456"
"order:list:user:789:status:pending"
"cache:v1:user:123"  // 包含版本号
```

**❌ 避免**：
```java
// 模糊的键名
"u123"
"data"
"temp"
```

### 2. 键名规范

```java
public class CacheKeyConstants {
    // 使用常量定义键前缀
    public static final String USER_PREFIX = "user:";
    public static final String PRODUCT_PREFIX = "product:";
    public static final String ORDER_PREFIX = "order:";
    
    // 使用方法生成键
    public static String userKey(Long userId) {
        return USER_PREFIX + userId;
    }
    
    public static String productKey(Long productId) {
        return PRODUCT_PREFIX + productId;
    }
    
    public static String orderListKey(Long userId, String status) {
        return ORDER_PREFIX + "list:user:" + userId + ":status:" + status;
    }
}
```

### 3. 避免键冲突

```java
// ✅ 使用应用名或模块名作为前缀
"basebackend:user:123"
"basebackend:product:456"

// ✅ 使用环境标识
"prod:user:123"
"dev:user:123"

// ✅ 使用版本号
"v1:user:123"
"v2:user:123"
```

### 4. 键长度控制

```java
// ✅ 简洁但清晰
"usr:123"
"prd:456"

// ❌ 过长的键
"application:module:submodule:entity:user:profile:detail:123"
```

## TTL 设置策略

### 1. 根据数据特性设置

| 数据类型 | 推荐 TTL | 说明 |
|---------|---------|------|
| 系统配置 | 24小时 - 7天 | 很少变化 |
| 字典数据 | 12小时 - 24小时 | 偶尔变化 |
| 用户信息 | 1小时 - 4小时 | 定期变化 |
| 产品详情 | 30分钟 - 2小时 | 经常变化 |
| 库存数据 | 30秒 - 5分钟 | 频繁变化 |
| 实时数据 | 10秒 - 1分钟 | 实时性要求高 |

### 2. 使用随机 TTL 防止雪崩

```java
@Service
public class CacheService {
    
    private final Random random = new Random();
    
    /**
     * 在基础 TTL 上增加随机偏移
     */
    public void setWithRandomTTL(String key, Object value, long baseTTL) {
        // 增加 ±20% 的随机偏移
        long offset = (long) (baseTTL * 0.2 * random.nextDouble());
        long actualTTL = baseTTL + offset - (long) (baseTTL * 0.1);
        
        cacheService.set(key, value, Duration.ofSeconds(actualTTL));
    }
}
```

### 3. 分层 TTL 策略

```java
@Service
public class MultiLevelTTLService {
    
    /**
     * 本地缓存使用较短的 TTL
     * Redis 使用较长的 TTL
     */
    public void cacheWithMultiLevelTTL(String key, Object value) {
        // 本地缓存：5分钟
        localCache.set(key, value, Duration.ofMinutes(5));
        
        // Redis：1小时
        redisService.set(key, value, Duration.ofHours(1));
    }
}
```

## 缓存粒度控制

### 1. 细粒度 vs 粗粒度

**✅ 细粒度缓存**（推荐）：
```java
// 缓存单个用户
@Cacheable(key = "user:#{#userId}")
public User getUser(Long userId) { }

// 缓存单个产品
@Cacheable(key = "product:#{#productId}")
public Product getProduct(Long productId) { }
```

**❌ 粗粒度缓存**（谨慎使用）：
```java
// 缓存所有用户（数据量大，更新频繁）
@Cacheable(key = "all:users")
public List<User> getAllUsers() { }
```

### 2. 列表数据的缓存策略

**方案 1：缓存整个列表**（适用于小列表）：
```java
@Cacheable(key = "categories:all", ttl = 3600)
public List<Category> getAllCategories() {
    return categoryRepository.findAll();
}
```

**方案 2：缓存列表 ID，单独缓存详情**（适用于大列表）：
```java
// 缓存 ID 列表
@Cacheable(key = "product:ids:category:#{#categoryId}", ttl = 600)
public List<Long> getProductIds(Long categoryId) {
    return productRepository.findIdsByCategoryId(categoryId);
}

// 单独缓存每个产品
@Cacheable(key = "product:#{#productId}", ttl = 3600)
public Product getProduct(Long productId) {
    return productRepository.findById(productId).orElse(null);
}

// 组合使用
public List<Product> getProductsByCategory(Long categoryId) {
    List<Long> ids = getProductIds(categoryId);
    return ids.stream()
        .map(this::getProduct)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
}
```

### 3. 分页数据的缓存

```java
@Service
public class PageCacheService {
    
    /**
     * 缓存分页数据
     */
    @Cacheable(key = "products:page:#{#page}:#{#size}:#{#sort}", ttl = 300)
    public Page<Product> getProducts(int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return productRepository.findAll(pageable);
    }
}
```

## 并发控制

### 1. 使用分布式锁防止缓存击穿

```java
@Service
@RequiredArgsConstructor
public class SafeCacheService {
    
    private final DistributedLockService lockService;
    private final CacheService cacheService;
    
    /**
     * 使用分布式锁保护热点数据
     */
    public Product getHotProduct(Long productId) {
        String key = "product:" + productId;
        
        // 先尝试从缓存获取
        Product product = cacheService.get(key, Product.class);
        if (product != null) {
            return product;
        }
        
        // 使用分布式锁
        String lockKey = "lock:product:" + productId;
        return lockService.executeWithLock(lockKey, () -> {
            // 双重检查
            Product cached = cacheService.get(key, Product.class);
            if (cached != null) {
                return cached;
            }
            
            // 从数据库加载
            Product loaded = loadFromDatabase(productId);
            if (loaded != null) {
                cacheService.set(key, loaded, Duration.ofHours(1));
            }
            return loaded;
        }, 5, 10);
    }
}
```

### 2. 使用布隆过滤器防止缓存穿透

```java
@Service
@RequiredArgsConstructor
public class BloomFilterCacheService {
    
    private final BloomFilterUtil bloomFilterUtil;
    private final CacheService cacheService;
    
    /**
     * 使用布隆过滤器快速判断数据是否存在
     */
    public Product getProduct(Long productId) {
        // 先检查布隆过滤器
        if (!bloomFilterUtil.mightContain("products", productId.toString())) {
            return null; // 肯定不存在
        }
        
        // 可能存在，继续查询
        String key = "product:" + productId;
        return cacheService.getOrLoad(
            key,
            () -> loadFromDatabase(productId),
            Duration.ofHours(1)
        );
    }
    
    /**
     * 添加数据时更新布隆过滤器
     */
    public Product addProduct(Product product) {
        Product saved = saveToDatabase(product);
        bloomFilterUtil.put("products", saved.getId().toString());
        return saved;
    }
}
```

### 3. 控制并发更新

```java
@Service
public class ConcurrentUpdateService {
    
    /**
     * 使用乐观锁控制并发更新
     */
    @CachePut(key = "product:#{#product.id}")
    public Product updateProduct(Product product) {
        try {
            return productRepository.save(product);
        } catch (OptimisticLockException e) {
            // 版本冲突，重试
            throw new ConcurrentUpdateException("Product updated by another user");
        }
    }
}
```

## 性能优化

### 1. 启用多级缓存

```yaml
basebackend:
  cache:
    multi-level:
      enabled: true
      local-max-size: 1000
      local-ttl: 5m
```

**优势**：
- 减少网络往返
- 降低 Redis 负载
- 提升响应速度

### 2. 使用批量操作

```java
@Service
@RequiredArgsConstructor
public class BatchOperationService {
    
    private final CacheService cacheService;
    
    /**
     * 批量获取，减少网络往返
     */
    public Map<Long, User> getUsers(Set<Long> userIds) {
        Set<String> keys = userIds.stream()
            .map(id -> "user:" + id)
            .collect(Collectors.toSet());
        
        return cacheService.multiGet(keys, User.class);
    }
    
    /**
     * 批量设置
     */
    public void cacheUsers(List<User> users) {
        Map<String, Object> dataMap = users.stream()
            .collect(Collectors.toMap(
                u -> "user:" + u.getId(),
                u -> u
            ));
        
        cacheService.multiSet(dataMap, Duration.ofHours(1));
    }
}
```

### 3. 选择合适的序列化器

```yaml
basebackend:
  cache:
    serialization:
      # 性能排序：kryo > protobuf > json
      # 可读性排序：json > protobuf > kryo
      type: kryo  # 生产环境推荐
```

### 4. 使用 Pipeline

```java
@Service
@RequiredArgsConstructor
public class PipelineService {
    
    private final RedisService redisService;
    
    /**
     * 使用 Pipeline 批量操作
     */
    public void batchUpdate(Map<String, String> data) {
        redisService.executePipelined(operations -> {
            data.forEach((key, value) -> {
                operations.set(key, value);
                operations.expire(key, 3600L);
            });
        });
    }
}
```

### 5. 预热热点数据

```java
@Configuration
public class CacheWarmingConfig {
    
    @Autowired
    private CacheWarmingManager warmingManager;
    
    @PostConstruct
    public void warmCache() {
        // 预热热门产品
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

## 监控和运维

### 1. 监控关键指标

```java
@Service
@RequiredArgsConstructor
public class CacheMonitorService {
    
    private final CacheMetricsService metricsService;
    
    /**
     * 定期检查缓存健康状态
     */
    @Scheduled(fixedRate = 60000)
    public void checkCacheHealth() {
        CacheStatistics stats = metricsService.getStatistics("myCache");
        
        // 检查命中率
        if (stats.getHitRate() < 0.5) {
            log.warn("Low cache hit rate: {}", stats.getHitRate());
            alertService.sendAlert("Low cache hit rate");
        }
        
        // 检查延迟
        if (stats.getAverageLoadTime() > 1000) {
            log.warn("High cache latency: {}ms", stats.getAverageLoadTime());
            alertService.sendAlert("High cache latency");
        }
        
        // 检查缓存大小
        if (stats.getSize() > 10000) {
            log.warn("Cache size too large: {}", stats.getSize());
        }
    }
}
```

### 2. 日志记录

```java
@Aspect
@Component
@Slf4j
public class CacheLoggingAspect {
    
    /**
     * 记录缓存操作日志
     */
    @Around("@annotation(cacheable)")
    public Object logCacheOperation(ProceedingJoinPoint pjp, Cacheable cacheable) 
            throws Throwable {
        String key = cacheable.key();
        long start = System.currentTimeMillis();
        
        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            
            log.debug("Cache operation: key={}, duration={}ms", key, duration);
            return result;
        } catch (Exception e) {
            log.error("Cache operation failed: key={}", key, e);
            throw e;
        }
    }
}
```

### 3. 定期清理

```java
@Service
public class CacheMaintenanceService {
    
    /**
     * 定期清理过期数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void cleanupExpiredCache() {
        log.info("Starting cache cleanup");
        
        // 清理过期的临时数据
        cacheService.deleteByPattern("temp:*");
        
        // 清理旧版本数据
        cacheService.deleteByPattern("v1:*");
        
        log.info("Cache cleanup completed");
    }
}
```

## 常见陷阱

### 1. 缓存雪崩

**问题**：大量缓存同时过期，导致数据库压力激增

**解决方案**：
```java
// 使用随机 TTL
public void cacheWithRandomTTL(String key, Object value, long baseTTL) {
    long randomOffset = (long) (baseTTL * 0.2 * random.nextDouble());
    long actualTTL = baseTTL + randomOffset;
    cacheService.set(key, value, Duration.ofSeconds(actualTTL));
}
```

### 2. 缓存穿透

**问题**：查询不存在的数据，每次都穿透到数据库

**解决方案**：
```java
// 使用布隆过滤器
if (!bloomFilter.mightContain(key)) {
    return null;
}

// 或缓存空值
if (value == null) {
    cacheService.set(key, NULL_VALUE, Duration.ofMinutes(5));
}
```

### 3. 缓存击穿

**问题**：热点数据过期，大量请求同时访问数据库

**解决方案**：
```java
// 使用分布式锁
lockService.executeWithLock(lockKey, () -> {
    // 双重检查
    Object cached = cacheService.get(key);
    if (cached != null) {
        return cached;
    }
    // 加载数据
    return loadFromDatabase();
}, 5, 10);
```

### 4. 缓存一致性

**问题**：缓存和数据库数据不一致

**解决方案**：
```java
// 更新时先更新数据库，再删除缓存
@Transactional
@CacheEvict(key = "user:#{#user.id}")
public User updateUser(User user) {
    return userRepository.save(user);
}

// 或使用较短的 TTL
@Cacheable(key = "user:#{#id}", ttl = 300)
public User getUser(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

### 5. 大对象缓存

**问题**：缓存大对象导致内存占用高、序列化慢

**解决方案**：
```java
// 只缓存必要的字段
public class UserCacheDTO {
    private Long id;
    private String name;
    private String email;
    // 不包含大字段如头像、详细信息等
}

// 或分拆缓存
@Cacheable(key = "user:basic:#{#id}")
public UserBasicInfo getUserBasic(Long id) { }

@Cacheable(key = "user:detail:#{#id}")
public UserDetailInfo getUserDetail(Long id) { }
```

### 6. 缓存键冲突

**问题**：不同业务使用相同的缓存键

**解决方案**：
```java
// 使用命名空间
public class CacheKeys {
    private static final String NAMESPACE = "basebackend:";
    
    public static String userKey(Long userId) {
        return NAMESPACE + "user:" + userId;
    }
    
    public static String productKey(Long productId) {
        return NAMESPACE + "product:" + productId;
    }
}
```

### 7. 忘记设置 TTL

**问题**：缓存永不过期，占用大量内存

**解决方案**：
```java
// 始终设置 TTL
@Cacheable(key = "user:#{#id}", ttl = 3600)
public User getUser(Long id) { }

// 或在配置中设置默认 TTL
basebackend:
  cache:
    template:
      cache-aside:
        default-ttl: 1h
```

## 总结

遵循这些最佳实践可以帮助你：

1. ✅ 设计合理的缓存策略
2. ✅ 避免常见的缓存问题
3. ✅ 提升系统性能和可用性
4. ✅ 简化运维和监控
5. ✅ 确保数据一致性

更多信息请参考：
- [README.md](../README.md) - 模块概述
- [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md) - 使用示例
- [FAULT_TOLERANCE_USAGE.md](FAULT_TOLERANCE_USAGE.md) - 容错降级
