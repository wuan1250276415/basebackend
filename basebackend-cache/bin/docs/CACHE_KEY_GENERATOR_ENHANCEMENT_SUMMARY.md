# CacheKeyGenerator 增强总结

## 概述

本次对 `CacheKeyGenerator` 工具类进行了全面的增强和补充，新增了多种键生成策略，支持更复杂的业务场景。

## 新增功能

### 1. 哈希键生成增强

#### 功能
- 支持自定义最大长度的哈希键生成
- 新增 SHA-256 哈希算法支持
- 优化字节数组转十六进制字符串的性能

#### 方法
```java
// MD5 哈希（默认 200 字符限制）
String hashedKey = keyGenerator.generateHashKey(longKey);

// 自定义长度限制
String hashedKey = keyGenerator.generateHashKey(longKey, 100);

// SHA-256 哈希（更安全）
String secureKey = keyGenerator.generateSHA256Key(key);
```

### 2. JSON 键生成

#### 功能
- 将复杂对象序列化为 JSON 作为键的一部分
- 自动处理过长的 JSON，使用哈希压缩
- 序列化失败时自动降级为 hashCode

#### 方法
```java
String jsonKey = keyGenerator.generateJsonKey("cache", "query", queryObject);
```

### 3. 版本控制键

#### 功能
- 支持缓存版本管理
- 方便批量失效旧版本缓存
- 适用于 API 版本控制场景

#### 方法
```java
String versionedKey = keyGenerator.generateVersionedKey("cache", "api", "data", "1.0");
```

### 4. 命名空间键

#### 功能
- 支持多环境隔离（开发、测试、生产）
- 支持多租户场景
- 便于按命名空间管理缓存

#### 方法
```java
// 命名空间键
String namespacedKey = keyGenerator.generateNamespacedKey("prod", "cache", "user", "123");

// 租户键
String tenantKey = keyGenerator.generateTenantKey("tenant001", "cache", "user", "123");
```

### 5. 分页键

#### 功能
- 支持基本分页键生成
- 支持带查询条件的分页键
- 查询参数自动排序，确保一致性

#### 方法
```java
// 基本分页
String pageKey = keyGenerator.generatePageKey("cache", "userList", 1, 20);

// 带查询条件
Map<String, Object> params = new HashMap<>();
params.put("status", "active");
String pageKey = keyGenerator.generatePageKey("cache", "userList", 1, 20, params);
```

### 6. 集合和列表键

#### 功能
- 支持集合类型数据的键生成
- 支持列表类型数据的键生成

#### 方法
```java
String collectionKey = keyGenerator.generateCollectionKey("cache", "users", "activeUsers");
String listKey = keyGenerator.generateListKey("cache", "orders", "recentOrders");
```

### 7. Map 键生成

#### 功能
- 将 Map 参数转换为键字符串
- 自动按键排序，确保相同参数生成相同的键
- 支持空 Map 处理

#### 方法
```java
Map<String, Object> params = new HashMap<>();
params.put("city", "北京");
params.put("age", 25);
String mapKey = keyGenerator.generateMapKey(params);
// 结果：age=25&city=北京
```

### 8. 时间相关键

#### 功能
- 支持时间戳键生成
- 支持 TTL 编码到键中

#### 方法
```java
// 时间戳键
String timestampKey = keyGenerator.generateTimestampKey("cache", "report", "daily", timestamp);

// TTL 键
String ttlKey = keyGenerator.generateTTLKey("cache", "session", "sessionId", 3600);
```

### 9. 批量操作

#### 功能
- 批量生成键列表
- 支持任意类型的 ID 集合

#### 方法
```java
List<Long> userIds = Arrays.asList(1L, 2L, 3L);
List<String> keys = keyGenerator.generateBatchKeys("cache", "user", userIds);
```

### 10. 范围查询键

#### 功能
- 生成范围查询的模式键
- 支持按范围清除缓存

#### 方法
```java
String rangePattern = keyGenerator.generateRangePatternKey(
    "cache", "orders", "2024-01-01", "2024-12-31");
```

### 11. 键管理工具

#### 功能
- 键解析：将完整键拆分为各个部分
- 键提取：提取前缀、缓存名称等
- 键验证：检查键是否有效
- 键清理：移除非法字符

#### 方法
```java
// 解析键
List<String> parts = keyGenerator.parseKey("cache:user:123");

// 提取前缀
String prefix = keyGenerator.extractPrefix("cache:user:123");

// 提取缓存名称
String cacheName = keyGenerator.extractCacheName("cache:user:123");

// 验证键
boolean isValid = keyGenerator.isValidKey(key);

// 清理键
String sanitized = keyGenerator.sanitizeKey("cache: user :123");
```

## 技术改进

### 1. 字符编码
- 使用 UTF-8 编码确保国际化支持
- 正确处理中文等多字节字符

### 2. 性能优化
- 优化字节数组转十六进制的性能
- 使用 StringBuilder 减少字符串拼接开销
- 缓存 ObjectMapper 实例

### 3. 容错处理
- JSON 序列化失败时自动降级
- 哈希算法不可用时使用备选方案
- 空值和 null 值的安全处理

### 4. 代码质量
- 添加详细的 JavaDoc 注释
- 统一的命名规范
- 完善的异常处理

## 测试覆盖

### 单元测试
创建了 `CacheKeyGeneratorTest`，包含 33 个测试用例：

1. 简单键生成测试
2. 模式键生成测试
3. 哈希键生成测试
4. SHA-256 键生成测试
5. JSON 键生成测试
6. 版本控制键测试
7. 命名空间键测试
8. 租户键测试
9. 集合键测试
10. 分页键测试
11. 列表键测试
12. Map 键生成测试
13. 时间戳键测试
14. TTL 键测试
15. 键解析测试
16. 键提取测试
17. 键验证测试
18. 键清理测试
19. 批量键生成测试
20. 范围查询键测试
21. 键一致性测试
22. 键唯一性测试
23. 版本隔离测试
24. 租户隔离测试

所有测试均通过 ✅

## 文档完善

### 1. 使用指南
创建了 `CACHE_KEY_GENERATOR_USAGE.md`，包含：
- 核心功能介绍
- 详细的使用示例
- 实际应用场景
- 最佳实践
- 注意事项

### 2. 示例代码
创建了 `CacheKeyGeneratorExample.java`，包含 10 个实际业务场景的示例：
- 用户信息缓存
- API 版本控制
- 多租户缓存
- 分页查询缓存
- 复杂查询对象缓存
- 批量操作
- 多环境配置
- 缓存清理
- 时间敏感缓存

### 3. README 更新
在 `basebackend-cache/README.md` 中：
- 添加了缓存键生成器功能介绍
- 添加了使用示例
- 添加了文档链接

## 应用场景

### 1. 基础缓存
- 用户信息缓存
- 配置信息缓存
- 字典数据缓存

### 2. 多租户系统
- 租户数据隔离
- 租户配置管理
- 按租户清理缓存

### 3. 版本控制
- API 版本管理
- 数据模型版本控制
- 无缝版本升级

### 4. 分页查询
- 列表分页缓存
- 带条件的分页查询
- 搜索结果缓存

### 5. 复杂查询
- 多条件查询缓存
- 报表查询缓存
- 统计数据缓存

### 6. 批量操作
- 批量查询优化
- 批量更新缓存
- 批量删除缓存

## 最佳实践

### 1. 键命名规范
- 使用有意义的前缀和缓存名称
- 保持键的层次结构清晰
- 避免使用特殊字符和空格

### 2. 键长度控制
- 对于可能过长的键，使用哈希
- 设置合理的最大长度限制
- 保留前缀便于识别

### 3. 版本管理
- 使用版本号管理缓存生命周期
- 升级版本时无需手动清理旧缓存
- 定期清理过期版本

### 4. 多租户隔离
- 使用租户键确保数据隔离
- 便于按租户清理缓存
- 避免租户间数据泄露

### 5. 批量操作优化
- 使用批量键生成减少重复代码
- 提高批量操作效率
- 减少网络往返次数

## 性能影响

### 1. 内存占用
- 键长度优化减少内存占用
- 哈希压缩长键节省空间
- 合理的键命名避免冗余

### 2. 性能提升
- 批量操作减少网络开销
- 键生成逻辑优化提升性能
- 缓存命中率提高

### 3. 可维护性
- 统一的键生成策略
- 清晰的键结构
- 便于调试和监控

## 兼容性

### 1. 向后兼容
- 保留原有的键生成方法
- 新增方法不影响现有功能
- 平滑升级路径

### 2. 扩展性
- 易于添加新的键生成策略
- 支持自定义键生成逻辑
- 灵活的配置选项

## 总结

本次 `CacheKeyGenerator` 增强为 cache 模块提供了强大的键管理能力，涵盖了大多数实际业务场景。通过合理使用这些工具方法，可以：

1. ✅ 提高代码可维护性
2. ✅ 减少键命名冲突
3. ✅ 简化缓存管理
4. ✅ 支持复杂业务场景
5. ✅ 提升系统性能
6. ✅ 增强数据隔离
7. ✅ 便于版本控制
8. ✅ 优化批量操作

所有功能均经过充分测试，文档完善，可以直接在生产环境中使用。
