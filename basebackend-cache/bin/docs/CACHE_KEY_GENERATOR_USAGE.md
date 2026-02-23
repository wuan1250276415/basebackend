# CacheKeyGenerator 使用指南

## 概述

`CacheKeyGenerator` 是一个强大的缓存键生成工具类，提供了多种键生成策略，支持复杂场景下的缓存键管理。

## 核心功能

### 1. 基本键生成

#### 默认键生成
```java
@Autowired
private CacheKeyGenerator keyGenerator;

// 自动生成键：prefix:cacheName:methodName:arg1_arg2
String key = keyGenerator.generateKey("cache", "UserService", "", 
    target, method, args);
```

#### 简单键生成
```java
// 生成简单键：cache:user:123
String key = keyGenerator.generateSimpleKey("cache", "user", "123");
```

#### 模式匹配键
```java
// 生成模式键：cache:UserService:*
String pattern = keyGenerator.generatePatternKey("cache", "UserService");
```

### 2. 哈希键生成

#### MD5 哈希（默认）
```java
// 对于过长的键，自动使用 MD5 哈希
String longKey = "very_long_cache_key_that_exceeds_200_characters...";
String hashedKey = keyGenerator.generateHashKey(longKey);
// 结果：very_long_cache_key_that_exceeds_200_cha:5d41402abc4b2a76b9719d911017c592
```

#### 自定义长度限制
```java
// 指定最大长度为 100
String hashedKey = keyGenerator.generateHashKey(longKey, 100);
```

#### SHA-256 哈希
```java
// 使用更安全的 SHA-256 算法
String secureKey = keyGenerator.generateSHA256Key("sensitive_data_key");
```

### 3. JSON 键生成

适用于复杂对象作为缓存键的场景：

```java
// 将对象序列化为 JSON 作为键的一部分
UserQuery query = new UserQuery();
query.setName("张三");
query.setAge(25);

String jsonKey = keyGenerator.generateJsonKey("cache", "userQuery", query);
// 自动处理过长的 JSON，使用哈希
```

### 4. 版本控制键

用于缓存版本管理，方便批量失效旧版本：

```java
// 生成带版本的键：cache:user:v1.0:123
String versionedKey = keyGenerator.generateVersionedKey(
    "cache", "user", "123", "1.0");

// 升级版本后，旧缓存自动失效
String newVersionKey = keyGenerator.generateVersionedKey(
    "cache", "user", "123", "2.0");
```

### 5. 命名空间键

用于多租户或多环境场景：

```java
// 生成带命名空间的键：prod:cache:user:123
String namespacedKey = keyGenerator.generateNamespacedKey(
    "prod", "cache", "user", "123");

// 测试环境
String testKey = keyGenerator.generateNamespacedKey(
    "test", "cache", "user", "123");
```

### 6. 租户键

专门用于多租户场景：

```java
// 生成租户键：tenant:tenant001:cache:user:123
String tenantKey = keyGenerator.generateTenantKey(
    "tenant001", "cache", "user", "123");
```

### 7. 分页键

#### 基本分页
```java
// 生成分页键：cache:userList:page:1_20
String pageKey = keyGenerator.generatePageKey("cache", "userList", 1, 20);
```

#### 带查询条件的分页
```java
Map<String, Object> queryParams = new HashMap<>();
queryParams.put("status", "active");
queryParams.put("role", "admin");

// 生成带查询条件的分页键：cache:userList:page:1_20:role=admin&status=active
String pageKey = keyGenerator.generatePageKey(
    "cache", "userList", 1, 20, queryParams);
```

### 8. 集合键

```java
// 生成集合键：cache:users:collection:activeUsers
String collectionKey = keyGenerator.generateCollectionKey(
    "cache", "users", "activeUsers");
```

### 9. 列表键

```java
// 生成列表键：cache:orders:list:recentOrders
String listKey = keyGenerator.generateListKey(
    "cache", "orders", "recentOrders");
```

### 10. Map 键生成

将 Map 参数转换为键字符串：

```java
Map<String, Object> params = new HashMap<>();
params.put("city", "北京");
params.put("age", 25);
params.put("gender", "male");

// 生成 Map 键：age=25&city=北京&gender=male（按键排序）
String mapKey = keyGenerator.generateMapKey(params);
```

### 11. 时间相关键

#### 时间戳键
```java
// 生成带时间戳的键：cache:report:daily:1700000000000
String timestampKey = keyGenerator.generateTimestampKey(
    "cache", "report", "daily", System.currentTimeMillis());
```

#### TTL 键
```java
// 将 TTL 编码到键中：cache:session:ttl3600:sessionId123
String ttlKey = keyGenerator.generateTTLKey(
    "cache", "session", "sessionId123", 3600);
```

### 12. 批量操作

```java
List<Long> userIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);

// 生成批量键列表
List<String> batchKeys = keyGenerator.generateBatchKeys(
    "cache", "user", userIds);
// 结果：[cache:user:1, cache:user:2, cache:user:3, cache:user:4, cache:user:5]
```

### 13. 范围查询键

```java
// 生成范围查询模式键：cache:orders:range:2024-01-01_2024-12-31:*
String rangePattern = keyGenerator.generateRangePatternKey(
    "cache", "orders", "2024-01-01", "2024-12-31");
```

## 键管理工具

### 键解析

```java
String key = "cache:user:v1.0:123";

// 解析键的各个部分
List<String> parts = keyGenerator.parseKey(key);
// 结果：[cache, user, v1.0, 123]

// 提取前缀
String prefix = keyGenerator.extractPrefix(key);
// 结果：cache

// 提取缓存名称
String cacheName = keyGenerator.extractCacheName(key);
// 结果：user
```

### 键验证

```java
String key = "cache:user:123";

// 验证键是否有效
boolean isValid = keyGenerator.isValidKey(key);

// 清理键（移除非法字符）
String sanitized = keyGenerator.sanitizeKey("cache: user :123");
// 结果：cache:_user_:123
```

## 实际应用场景

### 场景 1：用户信息缓存

```java
@Service
public class UserService {
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    @Autowired
    private RedisService redisService;
    
    public User getUserById(Long userId) {
        // 生成简单键
        String key = keyGenerator.generateSimpleKey("cache", "user", userId.toString());
        
        User user = redisService.get(key);
        if (user != null) {
            return user;
        }
        
        // 从数据库查询
        user = userRepository.findById(userId);
        redisService.set(key, user, 3600, TimeUnit.SECONDS);
        
        return user;
    }
}
```

### 场景 2：分页查询缓存

```java
@Service
public class OrderService {
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    @Autowired
    private RedisService redisService;
    
    public PageResult<Order> getOrders(int pageNum, int pageSize, OrderQuery query) {
        // 将查询条件转换为 Map
        Map<String, Object> params = new HashMap<>();
        params.put("status", query.getStatus());
        params.put("userId", query.getUserId());
        
        // 生成分页键
        String key = keyGenerator.generatePageKey(
            "cache", "orders", pageNum, pageSize, params);
        
        PageResult<Order> result = redisService.get(key);
        if (result != null) {
            return result;
        }
        
        // 从数据库查询
        result = orderRepository.findByPage(pageNum, pageSize, query);
        redisService.set(key, result, 600, TimeUnit.SECONDS);
        
        return result;
    }
}
```

### 场景 3：多租户缓存

```java
@Service
public class TenantDataService {
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    @Autowired
    private RedisService redisService;
    
    public TenantConfig getTenantConfig(String tenantId, String configKey) {
        // 生成租户键
        String key = keyGenerator.generateTenantKey(
            tenantId, "cache", "config", configKey);
        
        TenantConfig config = redisService.get(key);
        if (config != null) {
            return config;
        }
        
        // 从数据库查询
        config = configRepository.findByTenantAndKey(tenantId, configKey);
        redisService.set(key, config, 7200, TimeUnit.SECONDS);
        
        return config;
    }
    
    public void clearTenantCache(String tenantId) {
        // 清除租户的所有缓存
        String pattern = keyGenerator.generateTenantKey(
            tenantId, "cache", "*", "*");
        redisService.deleteByPattern(pattern);
    }
}
```

### 场景 4：版本控制缓存

```java
@Service
public class ApiService {
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    @Autowired
    private RedisService redisService;
    
    private static final String API_VERSION = "2.0";
    
    public ApiResponse getApiData(String apiKey) {
        // 生成带版本的键
        String key = keyGenerator.generateVersionedKey(
            "cache", "api", apiKey, API_VERSION);
        
        ApiResponse response = redisService.get(key);
        if (response != null) {
            return response;
        }
        
        // 调用 API
        response = apiClient.call(apiKey);
        redisService.set(key, response, 1800, TimeUnit.SECONDS);
        
        return response;
    }
    
    public void upgradeApiVersion(String newVersion) {
        // 升级版本后，旧版本缓存自动失效
        // 无需手动清理，因为键中包含版本号
        log.info("API version upgraded to: {}", newVersion);
    }
}
```

### 场景 5：复杂查询对象缓存

```java
@Service
public class ReportService {
    
    @Autowired
    private CacheKeyGenerator keyGenerator;
    
    @Autowired
    private RedisService redisService;
    
    public Report generateReport(ReportQuery query) {
        // 使用 JSON 键生成器处理复杂查询对象
        String key = keyGenerator.generateJsonKey("cache", "report", query);
        
        Report report = redisService.get(key);
        if (report != null) {
            return report;
        }
        
        // 生成报表
        report = reportGenerator.generate(query);
        redisService.set(key, report, 3600, TimeUnit.SECONDS);
        
        return report;
    }
}
```

## 最佳实践

### 1. 键命名规范

- 使用有意义的前缀和缓存名称
- 保持键的层次结构清晰
- 避免使用特殊字符和空格

```java
// 好的示例
String key = keyGenerator.generateSimpleKey("app", "user", "123");
// 结果：app:user:123

// 不好的示例
String key = "user 123"; // 包含空格
```

### 2. 键长度控制

- 对于可能过长的键，使用哈希
- 设置合理的最大长度限制

```java
// 自动处理过长的键
String key = keyGenerator.generateHashKey(veryLongKey);
```

### 3. 版本管理

- 使用版本号管理缓存生命周期
- 升级版本时无需手动清理旧缓存

```java
String key = keyGenerator.generateVersionedKey("cache", "data", "key", "1.0");
```

### 4. 多租户隔离

- 使用租户键确保数据隔离
- 便于按租户清理缓存

```java
String key = keyGenerator.generateTenantKey(tenantId, "cache", "data", "key");
```

### 5. 批量操作优化

- 使用批量键生成减少重复代码
- 提高批量操作效率

```java
List<String> keys = keyGenerator.generateBatchKeys("cache", "user", userIds);
List<User> users = redisService.multiGet(keys);
```

## 注意事项

1. **键长度限制**：Redis 键建议不超过 1024 字节，过长的键会自动哈希
2. **字符限制**：避免在键中使用空格、换行等特殊字符
3. **性能考虑**：JSON 键生成会有序列化开销，仅在必要时使用
4. **命名冲突**：确保不同业务使用不同的前缀或命名空间
5. **版本管理**：定期清理旧版本的缓存，避免内存浪费

## 总结

`CacheKeyGenerator` 提供了丰富的键生成策略，涵盖了大多数缓存场景。合理使用这些工具方法，可以：

- 提高代码可维护性
- 减少键命名冲突
- 简化缓存管理
- 支持复杂业务场景

根据实际需求选择合适的键生成策略，可以让缓存管理更加高效和规范。
