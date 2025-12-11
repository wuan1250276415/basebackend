# 缓存模块使用示例

本文档提供 basebackend-cache 模块的常见使用场景和代码示例。

## 目录

- [注解驱动缓存](#注解驱动缓存)
- [编程式缓存](#编程式缓存)
- [多级缓存](#多级缓存)
- [缓存模式](#缓存模式)
- [分布式数据结构](#分布式数据结构)
- [批量操作](#批量操作)
- [高级用法](#高级用法)

## 注解驱动缓存

### 基本用法

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 缓存用户信息
     * 首次调用时查询数据库并缓存，后续调用直接返回缓存
     */
    @Cacheable(key = "user:#{#userId}")
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }
    
    /**
     * 更新用户时清除缓存
     */
    @CacheEvict(key = "user:#{#user.id}")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * 更新用户并刷新缓存
     */
    @CachePut(key = "user:#{#user.id}")
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
```

### SpEL 表达式

```java
@Service
public class OrderService {
    
    /**
     * 使用方法参数生成缓存键
     */
    @Cacheable(key = "order:#{#orderId}:user:#{#userId}")
    public Order getUserOrder(Long userId, Long orderId) {
        return orderRepository.findByUserIdAndOrderId(userId, orderId);
    }
    
    /**
     * 使用对象属性生成缓存键
     */
    @Cacheable(key = "order:list:user:#{#request.userId}:status:#{#request.status}")
    public List<Order> getOrders(OrderQueryRequest request) {
        return orderRepository.findByUserIdAndStatus(
            request.getUserId(), 
            request.getStatus()
        );
    }
    
    /**
     * 使用方法返回值生成缓存键
     */
    @CachePut(key = "order:#{#result.id}")
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        // 设置订单属性
        return orderRepository.save(order);
    }
}
```

### 条件缓存

```java
@Service
public class ProductService {
    
    /**
     * 只缓存有效的产品
     */
    @Cacheable(
        key = "product:#{#productId}",
        condition = "#productId != null && #productId > 0"
    )
    public Product getProduct(Long productId) {
        return productRepository.findById(productId).orElse(null);
    }
    
    /**
     * 只缓存非空结果
     */
    @Cacheable(
        key = "product:search:#{#keyword}",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<Product> searchProducts(String keyword) {
        return productRepository.searchByKeyword(keyword);
    }
    
    /**
     * 根据用户角色决定是否缓存
     */
    @Cacheable(
        key = "product:#{#productId}",
        condition = "@securityService.isVipUser()"
    )
    public Product getProductForVip(Long productId) {
        return productRepository.findById(productId).orElse(null);
    }
}
```

### 自定义 TTL

```java
@Service
public class ConfigService {
    
    /**
     * 配置数据缓存 24 小时
     */
    @Cacheable(key = "config:#{#key}", ttl = 86400)
    public String getConfig(String key) {
        return configRepository.findByKey(key);
    }
    
    /**
     * 实时数据缓存 1 分钟
     */
    @Cacheable(key = "stock:#{#productId}", ttl = 60)
    public Integer getStock(Long productId) {
        return inventoryService.getStock(productId);
    }
    
    /**
     * 用户会话缓存 30 分钟
     */
    @Cacheable(key = "session:#{#sessionId}", ttl = 1800)
    public UserSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }
}
```

### 批量清除缓存

```java
@Service
public class CacheManagementService {
    
    /**
     * 清除单个缓存
     */
    @CacheEvict(key = "user:#{#userId}")
    public void evictUser(Long userId) {
        // 缓存会被自动清除
    }
    
    /**
     * 清除多个缓存
     */
    @CacheEvict(key = "user:*", allEntries = true)
    public void evictAllUsers() {
        // 清除所有用户缓存
    }
    
    /**
     * 清除指定前缀的缓存
     */
    public void evictByPattern(String pattern) {
        cacheService.deleteByPattern(pattern);
    }
}
```

## 编程式缓存

### 使用 CacheService

```java
@Service
@RequiredArgsConstructor
public class ArticleService {
    
    private final CacheService cacheService;
    private final ArticleRepository articleRepository;
    
    /**
     * 基本的 get/set 操作
     */
    public Article getArticle(Long articleId) {
        String key = "article:" + articleId;
        
        // 尝试从缓存获取
        Article article = cacheService.get(key, Article.class);
        if (article != null) {
            return article;
        }
        
        // 缓存未命中，从数据库加载
        article = articleRepository.findById(articleId).orElse(null);
        if (article != null) {
            // 缓存 1 小时
            cacheService.set(key, article, Duration.ofHours(1));
        }
        
        return article;
    }
    
    /**
     * 使用 getOrLoad 简化代码
     */
    public Article getArticleSimple(Long articleId) {
        String key = "article:" + articleId;
        return cacheService.getOrLoad(
            key,
            () -> articleRepository.findById(articleId).orElse(null),
            Duration.ofHours(1)
        );
    }
    
    /**
     * 更新缓存
     */
    public Article updateArticle(Article article) {
        Article saved = articleRepository.save(article);
        String key = "article:" + article.getId();
        cacheService.set(key, saved, Duration.ofHours(1));
        return saved;
    }
    
    /**
     * 删除缓存
     */
    public void deleteArticle(Long articleId) {
        articleRepository.deleteById(articleId);
        String key = "article:" + articleId;
        cacheService.evict(key);
    }
}
```

### 使用 RedisService

```java
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final RedisService redisService;
    
    /**
     * 字符串操作
     */
    public void saveSession(String sessionId, String data) {
        String key = "session:" + sessionId;
        redisService.set(key, data, 1800L); // 30 分钟
    }
    
    public String getSession(String sessionId) {
        String key = "session:" + sessionId;
        return redisService.get(key);
    }
    
    /**
     * 对象操作
     */
    public void saveUserSession(String sessionId, UserSession session) {
        String key = "session:" + sessionId;
        redisService.setObject(key, session, 1800L);
    }
    
    public UserSession getUserSession(String sessionId) {
        String key = "session:" + sessionId;
        return redisService.getObject(key, UserSession.class);
    }
    
    /**
     * Hash 操作
     */
    public void saveUserProfile(Long userId, Map<String, Object> profile) {
        String key = "user:profile:" + userId;
        redisService.hSetAll(key, profile);
        redisService.expire(key, 3600L);
    }
    
    public Map<Object, Object> getUserProfile(Long userId) {
        String key = "user:profile:" + userId;
        return redisService.hGetAll(key);
    }
    
    /**
     * List 操作
     */
    public void addToRecentViews(Long userId, Long productId) {
        String key = "user:recent:" + userId;
        redisService.lPush(key, productId.toString());
        redisService.lTrim(key, 0, 9); // 只保留最近 10 个
        redisService.expire(key, 86400L); // 1 天
    }
    
    public List<Object> getRecentViews(Long userId) {
        String key = "user:recent:" + userId;
        return redisService.lRange(key, 0, -1);
    }
    
    /**
     * Set 操作
     */
    public void addToFavorites(Long userId, Long productId) {
        String key = "user:favorites:" + userId;
        redisService.sAdd(key, productId.toString());
    }
    
    public boolean isFavorite(Long userId, Long productId) {
        String key = "user:favorites:" + userId;
        return redisService.sIsMember(key, productId.toString());
    }
    
    /**
     * Sorted Set 操作
     */
    public void updateScore(String leaderboard, String userId, double score) {
        String key = "leaderboard:" + leaderboard;
        redisService.zAdd(key, userId, score);
    }
    
    public Set<Object> getTopPlayers(String leaderboard, int count) {
        String key = "leaderboard:" + leaderboard;
        return redisService.zReverseRange(key, 0, count - 1);
    }
}
```

## 多级缓存

### 配置多级缓存

```yaml
basebackend:
  cache:
    multi-level:
      enabled: true
      local-max-size: 1000
      local-ttl: 5m
      eviction-policy: LRU
```

### 使用多级缓存

```java
@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final MultiLevelCacheManager cacheManager;
    private final CategoryRepository categoryRepository;
    
    /**
     * 多级缓存自动处理查询顺序
     * 1. 先查本地缓存
     * 2. 本地未命中查 Redis
     * 3. Redis 命中后同步到本地
     * 4. 都未命中则从数据库加载
     */
    public Category getCategory(Long categoryId) {
        String key = "category:" + categoryId;
        
        // 尝试从多级缓存获取
        Category category = cacheManager.get(key, Category.class);
        if (category != null) {
            return category;
        }
        
        // 从数据库加载
        category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null) {
            // 同时更新本地缓存和 Redis
            cacheManager.set(key, category, Duration.ofMinutes(30));
        }
        
        return category;
    }
    
    /**
     * 更新时清除所有级别的缓存
     */
    public Category updateCategory(Category category) {
        Category saved = categoryRepository.save(category);
        String key = "category:" + category.getId();
        cacheManager.evict(key); // 同时清除本地和 Redis
        return saved;
    }
    
    /**
     * 查看缓存统计
     */
    public void printCacheStats() {
        CacheStatistics stats = cacheManager.getStatistics();
        System.out.println("Total hits: " + stats.getHitCount());
        System.out.println("Total misses: " + stats.getMissCount());
        System.out.println("Hit rate: " + stats.getHitRate());
        System.out.println("Local cache size: " + stats.getLocalCacheSize());
        System.out.println("Redis cache size: " + stats.getRedisCacheSize());
    }
}
```

## 缓存模式

### Cache-Aside 模式

```java
@Service
@RequiredArgsConstructor
public class ProductCacheService {
    
    private final CacheAsideTemplate cacheAsideTemplate;
    private final ProductRepository productRepository;
    
    /**
     * 读取：先查缓存，未命中则从数据库加载
     */
    public Product getProduct(Long productId) {
        String key = "product:" + productId;
        return cacheAsideTemplate.get(
            key,
            () -> productRepository.findById(productId).orElse(null),
            Duration.ofHours(1)
        );
    }
    
    /**
     * 更新：先更新数据库，然后删除缓存
     */
    public Product updateProduct(Product product) {
        String key = "product:" + product.getId();
        return cacheAsideTemplate.update(key, p -> {
            return productRepository.save(product);
        });
    }
}
```

### Write-Through 模式

```java
@Service
@RequiredArgsConstructor
public class InventoryService {
    
    private final WriteThroughTemplate writeThroughTemplate;
    private final InventoryRepository inventoryRepository;
    
    /**
     * 同步更新缓存和数据库
     */
    public void updateStock(Long productId, Integer quantity) {
        String key = "stock:" + productId;
        writeThroughTemplate.set(
            key,
            quantity,
            q -> inventoryRepository.updateStock(productId, q)
        );
    }
}
```

### Write-Behind 模式

```java
@Service
@RequiredArgsConstructor
public class ViewCountService {
    
    private final WriteBehindTemplate writeBehindTemplate;
    private final ArticleRepository articleRepository;
    
    /**
     * 异步批量更新数据库
     * 适用于高频写入场景，如浏览计数
     */
    public void incrementViewCount(Long articleId) {
        String key = "article:views:" + articleId;
        Long currentCount = writeBehindTemplate.increment(key);
        
        // 数据会被异步批量写入数据库
        writeBehindTemplate.set(key, currentCount);
    }
    
    /**
     * 手动触发刷新
     */
    public void flushViewCounts() {
        writeBehindTemplate.flush();
    }
}
```

## 分布式数据结构

### 分布式 Map

```java
@Service
@RequiredArgsConstructor
public class ConfigurationService {
    
    private final DistributedMapService mapService;
    
    /**
     * 使用分布式 Map 存储配置
     */
    public void saveConfig(String key, String value) {
        mapService.put("system:config", key, value);
    }
    
    public String getConfig(String key) {
        return mapService.get("system:config", key);
    }
    
    public Map<String, String> getAllConfigs() {
        RMap<String, String> map = mapService.getMap("system:config");
        return new HashMap<>(map);
    }
    
    public void removeConfig(String key) {
        mapService.remove("system:config", key);
    }
}
```

### 分布式 Queue

```java
@Service
@RequiredArgsConstructor
public class TaskQueueService {
    
    private final DistributedQueueService queueService;
    
    /**
     * 添加任务到队列
     */
    public void submitTask(Task task) {
        queueService.offer("task:queue", task);
    }
    
    /**
     * 从队列获取任务
     */
    public Task pollTask() {
        return queueService.poll("task:queue");
    }
    
    /**
     * 查看队列大小
     */
    public long getQueueSize() {
        return queueService.size("task:queue");
    }
    
    /**
     * 批量处理任务
     */
    public void processTasks(int batchSize) {
        for (int i = 0; i < batchSize; i++) {
            Task task = queueService.poll("task:queue");
            if (task == null) {
                break;
            }
            processTask(task);
        }
    }
    
    private void processTask(Task task) {
        // 处理任务逻辑
    }
}
```

### 分布式 Set

```java
@Service
@RequiredArgsConstructor
public class OnlineUserService {
    
    private final DistributedSetService setService;
    
    /**
     * 用户上线
     */
    public void userOnline(Long userId) {
        setService.add("online:users", userId.toString());
    }
    
    /**
     * 用户下线
     */
    public void userOffline(Long userId) {
        setService.remove("online:users", userId.toString());
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return setService.contains("online:users", userId.toString());
    }
    
    /**
     * 获取在线用户数
     */
    public long getOnlineUserCount() {
        return setService.size("online:users");
    }
    
    /**
     * 获取所有在线用户
     */
    public Set<String> getAllOnlineUsers() {
        RSet<String> set = setService.getSet("online:users");
        return new HashSet<>(set);
    }
}
```

## 批量操作

### 批量读取

```java
@Service
@RequiredArgsConstructor
public class BatchCacheService {
    
    private final CacheService cacheService;
    private final UserRepository userRepository;
    
    /**
     * 批量获取用户
     */
    public Map<Long, User> getUsers(Set<Long> userIds) {
        // 构建缓存键
        Set<String> keys = userIds.stream()
            .map(id -> "user:" + id)
            .collect(Collectors.toSet());
        
        // 批量从缓存获取
        Map<String, User> cached = cacheService.multiGet(keys, User.class);
        
        // 找出未命中的 ID
        Set<Long> missedIds = userIds.stream()
            .filter(id -> !cached.containsKey("user:" + id))
            .collect(Collectors.toSet());
        
        // 从数据库加载未命中的数据
        if (!missedIds.isEmpty()) {
            List<User> users = userRepository.findAllById(missedIds);
            Map<String, Object> toCache = users.stream()
                .collect(Collectors.toMap(
                    u -> "user:" + u.getId(),
                    u -> u
                ));
            
            // 批量写入缓存
            cacheService.multiSet(toCache, Duration.ofHours(1));
            
            // 合并结果
            users.forEach(u -> cached.put("user:" + u.getId(), u));
        }
        
        // 转换键格式
        return cached.entrySet().stream()
            .collect(Collectors.toMap(
                e -> Long.parseLong(e.getKey().substring(5)),
                Map.Entry::getValue
            ));
    }
}
```

### 批量写入

```java
@Service
@RequiredArgsConstructor
public class BulkUpdateService {
    
    private final CacheService cacheService;
    
    /**
     * 批量更新产品缓存
     */
    public void updateProducts(List<Product> products) {
        Map<String, Object> dataMap = products.stream()
            .collect(Collectors.toMap(
                p -> "product:" + p.getId(),
                p -> p
            ));
        
        cacheService.multiSet(dataMap, Duration.ofHours(2));
    }
    
    /**
     * 批量删除缓存
     */
    public void deleteProducts(Set<Long> productIds) {
        Set<String> keys = productIds.stream()
            .map(id -> "product:" + id)
            .collect(Collectors.toSet());
        
        keys.forEach(cacheService::evict);
    }
}
```

### Pipeline 操作

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
    
    /**
     * Pipeline 批量读取
     */
    public List<String> batchGet(List<String> keys) {
        return redisService.executePipelined(operations -> {
            keys.forEach(operations::get);
        });
    }
}
```

## 高级用法

### 缓存穿透防护

```java
@Service
@RequiredArgsConstructor
public class AntiPenetrationService {
    
    private final CacheAsideTemplate cacheAsideTemplate;
    private final BloomFilterUtil bloomFilterUtil;
    private final ProductRepository productRepository;
    
    /**
     * 使用布隆过滤器防止缓存穿透
     */
    public Product getProduct(Long productId) {
        // 先检查布隆过滤器
        if (!bloomFilterUtil.mightContain("products", productId.toString())) {
            // 肯定不存在，直接返回
            return null;
        }
        
        // 可能存在，查询缓存和数据库
        String key = "product:" + productId;
        return cacheAsideTemplate.get(
            key,
            () -> productRepository.findById(productId).orElse(null),
            Duration.ofHours(1)
        );
    }
    
    /**
     * 添加产品时更新布隆过滤器
     */
    public Product addProduct(Product product) {
        Product saved = productRepository.save(product);
        bloomFilterUtil.put("products", saved.getId().toString());
        return saved;
    }
}
```

### 缓存击穿防护

```java
@Service
@RequiredArgsConstructor
public class AntiBreakdownService {
    
    private final CacheService cacheService;
    private final DistributedLockService lockService;
    private final ProductRepository productRepository;
    
    /**
     * 使用分布式锁防止缓存击穿
     */
    public Product getHotProduct(Long productId) {
        String key = "product:" + productId;
        
        // 先尝试从缓存获取
        Product product = cacheService.get(key, Product.class);
        if (product != null) {
            return product;
        }
        
        // 缓存未命中，使用分布式锁
        String lockKey = "lock:product:" + productId;
        return lockService.executeWithLock(lockKey, () -> {
            // 双重检查
            Product cached = cacheService.get(key, Product.class);
            if (cached != null) {
                return cached;
            }
            
            // 从数据库加载
            Product loaded = productRepository.findById(productId).orElse(null);
            if (loaded != null) {
                cacheService.set(key, loaded, Duration.ofHours(2));
            }
            return loaded;
        }, 5, 10);
    }
}
```

### 缓存雪崩防护

```java
@Service
@RequiredArgsConstructor
public class AntiAvalancheService {
    
    private final CacheService cacheService;
    private final Random random = new Random();
    
    /**
     * 使用随机 TTL 防止缓存雪崩
     */
    public void cacheWithRandomTTL(String key, Object value, long baseTTL) {
        // 在基础 TTL 上增加随机时间（±20%）
        long randomOffset = (long) (baseTTL * 0.2 * random.nextDouble());
        long actualTTL = baseTTL + randomOffset - (long) (baseTTL * 0.1);
        
        cacheService.set(key, value, Duration.ofSeconds(actualTTL));
    }
    
    /**
     * 批量缓存时使用不同的 TTL
     */
    public void batchCacheWithRandomTTL(Map<String, Object> data, long baseTTL) {
        data.forEach((key, value) -> {
            cacheWithRandomTTL(key, value, baseTTL);
        });
    }
}
```

### 自定义缓存键生成

```java
@Service
public class CustomKeyService {
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    /**
     * 使用自定义键生成器
     */
    public String generateKey(Object... params) {
        return keyGenerator.generate("myCache", "myMethod", params);
    }
    
    /**
     * 生成带命名空间的键
     */
    public String generateNamespacedKey(String namespace, Object... params) {
        return keyGenerator.generateWithNamespace(namespace, params);
    }
}
```

### 监控和指标

```java
@Service
@RequiredArgsConstructor
public class CacheMonitoringService {
    
    private final CacheMetricsService metricsService;
    
    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getCacheStats(String cacheName) {
        return metricsService.getStatistics(cacheName);
    }
    
    /**
     * 检查缓存健康状态
     */
    public boolean isCacheHealthy(String cacheName) {
        CacheStatistics stats = metricsService.getStatistics(cacheName);
        
        // 检查命中率
        if (stats.getHitRate() < 0.5) {
            log.warn("Low hit rate for cache: {}, rate: {}", 
                cacheName, stats.getHitRate());
            return false;
        }
        
        // 检查平均延迟
        if (stats.getAverageLoadTime() > 1000) {
            log.warn("High latency for cache: {}, latency: {}ms", 
                cacheName, stats.getAverageLoadTime());
            return false;
        }
        
        return true;
    }
    
    /**
     * 定期报告缓存指标
     */
    @Scheduled(fixedRate = 60000) // 每分钟
    public void reportMetrics() {
        List<String> cacheNames = Arrays.asList("user", "product", "order");
        
        for (String cacheName : cacheNames) {
            CacheStatistics stats = metricsService.getStatistics(cacheName);
            log.info("Cache [{}] - Hits: {}, Misses: {}, Hit Rate: {:.2f}%, Size: {}",
                cacheName,
                stats.getHitCount(),
                stats.getMissCount(),
                stats.getHitRate() * 100,
                stats.getSize());
        }
    }
}
```

## 总结

本文档涵盖了 basebackend-cache 模块的主要使用场景和代码示例。更多详细信息请参考：

- [README.md](../README.md) - 模块概述和快速开始
- [BEST_PRACTICES.md](BEST_PRACTICES.md) - 最佳实践指南
- [CACHE_WARMING_USAGE.md](CACHE_WARMING_USAGE.md) - 缓存预热详细指南
- [DISTRIBUTED_LOCK_USAGE.md](DISTRIBUTED_LOCK_USAGE.md) - 分布式锁详细指南
- [FAULT_TOLERANCE_USAGE.md](FAULT_TOLERANCE_USAGE.md) - 容错降级详细指南
