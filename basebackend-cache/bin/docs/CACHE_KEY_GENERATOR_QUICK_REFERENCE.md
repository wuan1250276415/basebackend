# CacheKeyGenerator 快速参考

## 常用方法速查表

| 方法 | 用途 | 示例 |
|------|------|------|
| `generateSimpleKey()` | 简单键 | `cache:user:123` |
| `generateVersionedKey()` | 版本控制键 | `cache:api:v1.0:data` |
| `generateTenantKey()` | 租户键 | `tenant:t001:cache:user:123` |
| `generateNamespacedKey()` | 命名空间键 | `prod:cache:config:key` |
| `generatePageKey()` | 分页键 | `cache:list:page:1_20` |
| `generateJsonKey()` | JSON 键 | `cache:query:{hash}` |
| `generateHashKey()` | 哈希键 | `prefix:5d41402abc4b2a76...` |
| `generateBatchKeys()` | 批量键 | `[cache:user:1, cache:user:2]` |
| `generatePatternKey()` | 模式键 | `cache:user:*` |

## 场景选择指南

### 基础场景
```java
// 单个对象缓存
generateSimpleKey("cache", "user", userId)

// 列表缓存
generateListKey("cache", "users", "active")

// 集合缓存
generateCollectionKey("cache", "tags", "popular")
```

### 多租户场景
```java
// 租户数据
generateTenantKey(tenantId, "cache", "data", key)

// 清除租户缓存
deleteByPattern("tenant:" + tenantId + ":*")
```

### 版本控制场景
```java
// 带版本的缓存
generateVersionedKey("cache", "api", key, "1.0")

// 升级版本（旧缓存自动失效）
generateVersionedKey("cache", "api", key, "2.0")
```

### 分页场景
```java
// 简单分页
generatePageKey("cache", "list", pageNum, pageSize)

// 带查询条件
generatePageKey("cache", "list", pageNum, pageSize, queryParams)
```

### 复杂查询场景
```java
// 复杂对象作为键
generateJsonKey("cache", "query", queryObject)

// Map 参数
String mapKey = generateMapKey(params);
generateSimpleKey("cache", "search", mapKey)
```

### 批量操作场景
```java
// 批量生成键
List<String> keys = generateBatchKeys("cache", "user", userIds);

// 批量获取
Map<String, Object> results = redisService.multiGet(keys);
```

## 键命名规范

### 推荐格式
```
{prefix}:{cacheName}:{type}:{identifier}
```

### 示例
```
cache:user:123                    # 用户缓存
cache:order:list:recent           # 订单列表
cache:product:page:1_20           # 产品分页
tenant:t001:cache:config:key      # 租户配置
prod:cache:api:v1.0:data          # 生产环境 API 数据
```

## 性能建议

### ✅ 推荐做法
- 使用简短有意义的前缀
- 对长键使用哈希压缩
- 批量操作使用 `generateBatchKeys()`
- 相同参数保证生成相同的键

### ❌ 避免做法
- 键中包含空格或特殊字符
- 键过长（超过 200 字符不哈希）
- 重复生成相同的键
- 使用不确定的键生成逻辑

## 常见问题

### Q: 如何确保相同参数生成相同的键？
A: 使用 `generateMapKey()` 会自动排序参数，确保一致性。

### Q: 键太长怎么办？
A: 使用 `generateHashKey()` 自动压缩，或使用 `generateJsonKey()` 处理复杂对象。

### Q: 如何清除某个租户的所有缓存？
A: 使用模式键：`redisService.deleteByPattern("tenant:" + tenantId + ":*")`

### Q: 版本升级后如何处理旧缓存？
A: 使用 `generateVersionedKey()`，版本号变化后旧缓存自动失效。

### Q: 如何处理分页查询的缓存？
A: 使用 `generatePageKey()` 包含页码、页大小和查询条件。

## 代码模板

### 基础缓存模板
```java
public User getUser(Long userId) {
    String key = keyGenerator.generateSimpleKey("cache", "user", userId.toString());
    User user = redisService.get(key);
    if (user != null) return user;
    
    user = loadFromDB(userId);
    redisService.set(key, user, 3600, TimeUnit.SECONDS);
    return user;
}
```

### 分页缓存模板
```java
public PageResult getPage(int pageNum, int pageSize, QueryParams params) {
    Map<String, Object> paramMap = convertToMap(params);
    String key = keyGenerator.generatePageKey("cache", "list", pageNum, pageSize, paramMap);
    
    PageResult result = redisService.get(key);
    if (result != null) return result;
    
    result = queryFromDB(pageNum, pageSize, params);
    redisService.set(key, result, 600, TimeUnit.SECONDS);
    return result;
}
```

### 批量查询模板
```java
public Map<Long, User> getUsers(List<Long> userIds) {
    List<String> keys = keyGenerator.generateBatchKeys("cache", "user", userIds);
    Map<String, Object> cached = redisService.multiGet(new HashSet<>(keys));
    
    // 找出未命中的 ID
    Set<Long> missedIds = findMissedIds(userIds, keys, cached);
    
    if (!missedIds.isEmpty()) {
        List<User> missed = queryFromDB(missedIds);
        Map<String, Object> toCache = new HashMap<>();
        for (User user : missed) {
            String key = keyGenerator.generateSimpleKey("cache", "user", user.getId().toString());
            toCache.put(key, user);
        }
        redisService.multiSet(toCache, Duration.ofHours(1));
        cached.putAll(toCache);
    }
    
    return convertToUserMap(userIds, keys, cached);
}
```

## 更多信息

- 详细使用指南：[CACHE_KEY_GENERATOR_USAGE.md](CACHE_KEY_GENERATOR_USAGE.md)
- 完整示例代码：[CacheKeyGeneratorExample.java](../src/main/java/com/basebackend/cache/example/CacheKeyGeneratorExample.java)
- 增强总结：[CACHE_KEY_GENERATOR_ENHANCEMENT_SUMMARY.md](CACHE_KEY_GENERATOR_ENHANCEMENT_SUMMARY.md)
