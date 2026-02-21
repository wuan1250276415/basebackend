# Redis热点日志缓存

## 📖 概述

Redis热点日志缓存是专为 basebackend-logging 模块设计的高性能缓存解决方案，通过本地LRU + Redis双层缓存架构、热点识别机制和智能TTL管理，实现日志查询速度的显著提升。

### 核心特性

- ✅ **多级缓存**：本地LRU + Redis双层缓存，查询<10ms
- ✅ **热点识别**：基于访问频次的自动提升机制
- ✅ **雪崩防护**：TTL随机抖动，防止同时过期
- ✅ **LRU淘汰**：有界缓存防止内存溢出
- ✅ **灵活策略**：读透、写透、写回、失效等多种策略
- ✅ **完整监控**：命中/未命中/淘汰等关键指标

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **查询速度提升** | ≥5倍 | 相比无缓存场景 |
| **命中率** | ≥90% | 热点数据命中率 |
| **响应时间** | <10ms | 本地缓存查询延迟 |
| **内存使用** | 可控 | LRU自动淘汰 |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│              热点日志缓存架构                                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            Application Layer                         │  │
│  │  ┌──────────────┐  ┌──────────────┐                 │  │
│  │  │   查询服务   │  │   写日志服务  │                 │  │
│  │  └──────┬───────┘  └──────┬───────┘                 │  │
│  │         │                 │                          │  │
│  └─────────┼─────────────────┼──────────────────────────┘  │
│            │                 │                              │
│            ▼                 ▼                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │        @HotLoggable AOP 拦截                          │  │
│  │  ┌───────────────────────────────────────────────┐   │  │
│  │  │  HotLogCacheAspect                           │   │  │
│  │  │  - 解析缓存键                                 │   │  │
│  │  │  - 选择缓存策略                               │   │  │
│  │  │  - 热点判断                                   │   │  │
│  │  └────────────┬──────────────────────────────┘   │  │
│  └───────────────┼──────────────────────────────────────┘  │
│                  │                                          │
│                  ▼                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         RedisHotLogCache                             │  │
│  │                                                       │  │
│  │  ┌───────────────────┐  ┌──────────────────────┐     │  │
│  │  │  本地缓存 (L1)     │  │   Redis缓存 (L2)      │     │  │
│  │  │                   │  │                       │     │  │
│  │  │ - 快速响应 <10ms   │  │ - 容量大             │     │  │
│  │  │ - 1024条目        │  │ - 持久化             │     │  │
│  │  │ - LRU淘汰         │  │ - TTL管理            │     │  │
│  │  │ - 零网络开销      │  │ - 高可用             │     │  │
│  │  └───────┬───────────┘  └───────────┬─────────┘     │  │
│  │          │                          │               │  │
│  │          ▼                          ▼               │  │
│  │  ┌─────────────────────────────────────────────┐   │  │
│  │  │        热点识别机制                         │   │  │
│  │  │  - 访问频次统计                             │   │  │
│  │  │  - 达到阈值自动提升                         │   │  │
│  │  │  - ConcurrentHashMap 存储                  │   │  │
│  │  └─────────────────────────────────────────────┘   │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │                                  │
│                         ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              存储层                                    │  │
│  │                                                       │  │
│  │  ┌──────────────────┐     ┌─────────────────────┐     │  │
│  │  │   Redis Server   │     │   文件系统          │     │  │
│  │  │                  │     │                     │     │  │
│  │  │ - 单机/集群      │     │ - 压缩日志          │     │  │
│  │  │ - Sentinel      │     │ - 索引文件          │     │  │
│  │  │ - AOF/RDB       │     │ - 审计日志          │     │  │
│  │  └──────────────────┘     └─────────────────────┘     │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 添加依赖

确保 `basebackend-logging` 模块已正确引入，并添加 Redis 依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. 配置 Redis

在 `application.yml` 中配置 Redis 连接：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: # 可选
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### 3. 配置热点缓存

```yaml
basebackend:
  logging:
    hot-cache:
      enabled: true                           # 启用缓存
      cache-prefix: "hotlog:"                 # 键前缀
      ttl-seconds: 300                        # 默认TTL（5分钟）
      jitter-seconds: 30                      # TTL随机抖动
      hot-threshold: 5                        # 热点阈值（5次访问）
      local-max-entries: 2048                 # 本地缓存最大条目
      use-local-cache: true                   # 启用本地缓存
      preload-keys:                           # 预热键列表
        - "audit:latest"
        - "login:stats"
      connect-timeout-millis: 2000            # 连接超时
      timeout-millis: 5000                    # 读写超时
```

### 4. 使用 @HotLoggable 注解

```java
@Service
public class AuditLogService {

    /**
     * 查询最近审计日志（读透策略）
     */
    @HotLoggable(
        cacheKey = "audit:latest",
        strategy = HotLoggable.CacheStrategy.READ_THROUGH,
        ttlSeconds = 600
    )
    public List<AuditLog> getRecentAuditLogs() {
        // 查询数据库或文件
        return auditLogRepository.findRecent();
    }

    /**
     * 新增审计日志（失效策略）
     */
    @HotLoggable(
        cacheKey = "audit:latest",
        strategy = HotLoggable.CacheStrategy.INVALIDATE
    )
    public void addAuditLog(AuditLog log) {
        auditLogRepository.save(log);
    }

    /**
     * 获取登录统计（热点数据预热）
     */
    @HotLoggable(
        cacheKey = "login:stats",
        strategy = HotLoggable.CacheStrategy.READ_THROUGH,
        preload = true  # 标记为预热数据
    )
    public LoginStats getLoginStatistics() {
        return calculateLoginStats();
    }

    /**
     * 批量查询审计日志（自定义键）
     */
    @HotLoggable(
        cacheKey = "audit:batch:{userId}",
        strategy = HotLoggable.CacheStrategy.READ_THROUGH,
        ttlSeconds = 300
    )
    public List<AuditLog> getAuditLogsByUserId(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }
}
```

### 5. 验证缓存效果

```java
@RestController
public class CacheController {

    @Autowired
    private RedisHotLogCache cache;

    @GetMapping("/cache/stats")
    public Map<String, Object> getCacheStats() {
        return cache.getMetrics().toMap();
    }

    @PostMapping("/cache/clear")
    public void clearCache() {
        cache.clearAll();
    }
}
```

## ⚙️ 配置参数详解

### 基础配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | true | 是否启用热点缓存 |
| `cache-prefix` | String | "hotlog:" | 缓存键前缀，用于隔离不同应用 |
| `ttl-seconds` | long | 300 | 默认TTL（秒） |
| `jitter-seconds` | long | 30 | TTL随机抖动（秒），防止雪崩 |

### 缓存策略配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `hot-threshold` | int | 5 | 热点阈值（访问次数） |
| `local-max-entries` | int | 1024 | 本地缓存最大条目数 |
| `use-local-cache` | boolean | true | 是否启用本地缓存 |

### 预热配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `preload-keys` | List<String> | [] | 启动时预加载的键列表 |

### 连接配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `connect-timeout-millis` | long | 2000 | Redis连接超时（毫秒） |
| `timeout-millis` | long | 5000 | Redis读写超时（毫秒） |
| `cleanup-interval-seconds` | long | 60 | 清理任务执行间隔（秒） |

## 📊 缓存策略详解

### READ_THROUGH（读透）

**行为：**
1. 查询时先查缓存
2. 缓存未命中则执行方法并将结果放入缓存
3. 下次查询直接返回缓存

**适用场景：**
- 频繁查询的场景
- 数据变化不频繁
- 如：日志列表、统计信息、系统配置

**示例：**
```java
@HotLoggable(
    cacheKey = "audit:latest",
    strategy = HotLoggable.CacheStrategy.READ_THROUGH,
    ttlSeconds = 600
)
public List<AuditLog> getRecentAuditLogs() {
    return repository.findRecent();
}
```

### WRITE_THROUGH（写透）

**行为：**
1. 执行写入操作
2. 写入成功后同步更新缓存
3. 后续查询返回最新数据

**适用场景：**
- 需要强一致性的写入场景
- 写入后需要立即查询的场景

**示例：**
```java
@HotLoggable(
    cacheKey = "user:profile:{userId}",
    strategy = HotLoggable.CacheStrategy.WRITE_THROUGH,
    ttlSeconds = 3600
)
public void updateUserProfile(Long userId, UserProfile profile) {
    userRepository.save(userId, profile);
}
```

### INVALIDATE（失效）

**行为：**
1. 执行写入操作
2. 写入成功后删除缓存条目
3. 下次查询重新从数据源获取

**适用场景：**
- 写入后需要刷新数据的场景
- 数据变化频繁的场景

**示例：**
```java
@HotLoggable(
    cacheKey = "audit:latest",
    strategy = HotLoggable.CacheStrategy.INVALIDATE
)
public void addAuditLog(AuditLog log) {
    auditLogRepository.save(log);
}
```

## 🔧 高级用法

### 1. 自定义缓存键

支持动态键和参数化键：

```java
@HotLoggable(
    cacheKey = "audit:user:{userId}:date:{date}",
    strategy = HotLoggable.CacheStrategy.READ_THROUGH
)
public List<AuditLog> getAuditLogsByUserAndDate(Long userId, String date) {
    return repository.findByUserIdAndDate(userId, date);
}
```

### 2. 条件缓存

通过SpEL表达式控制缓存：

```java
@HotLoggable(
    cacheKey = "audit:conditional",
    strategy = HotLoggable.CacheStrategy.READ_THROUGH,
    ttlSeconds = 300
)
public List<AuditLog> getAuditLogsWithCondition(boolean includeDeleted) {
    return includeDeleted ?
        repository.findAll() :
        repository.findActive();
}
```

### 3. 异常处理

```java
@HotLoggable(
    cacheKey = "audit:safe",
    strategy = HotLoggable.CacheStrategy.READ_THROUGH,
    invalidateOnException = true  // 异常时失效缓存
)
public List<AuditLog> getAuditLogsSafe() {
    try {
        return repository.findRecent();
    } catch (Exception e) {
        // 缓存会被自动失效
        throw e;
    }
}
```

## 📊 监控指标

### 1. 通过代码获取指标

```java
@Autowired
private RedisHotLogCache cache;

// 获取指标
HotLogCacheMetrics metrics = cache.getMetrics();
System.out.println("命中率: " + metrics.getHitRatePercentage() + "%");
System.out.println("性能等级: " + metrics.getPerformanceGrade().getLabel());

// 获取详细统计
Map<String, Object> stats = metrics.toMap();
System.out.println(stats);
```

### 2. 通过 Micrometer 集成

```java
@Component
public class CacheMetrics {

    private final RedisHotLogCache cache;

    public CacheMetrics(RedisHotLogCache cache) {
        this.cache = cache;
        // 注册指标
        MeterRegistry.registry.gauge("logging.cache.hit.rate",
            cache.getMetrics(), m -> m.getHitRate());
        MeterRegistry.registry.gauge("logging.cache.hits",
            cache.getMetrics(), m -> (double) m.getHits());
        MeterRegistry.registry.gauge("logging.cache.misses",
            cache.getMetrics(), m -> (double) m.getMisses());
        MeterRegistry.registry.gauge("logging.cache.evictions",
            cache.getMetrics(), m -> (double) m.getEvictions());
        MeterRegistry.registry.gauge("logging.cache.local.size",
            cache, c -> (double) c.getLocalSize());
    }
}
```

### 3. 关键指标说明

| 指标 | 类型 | 说明 | 正常范围 |
|------|------|------|----------|
| `hitRate` | Gauge | 命中率（0-1） | > 0.9 |
| `hits` | Counter | 命中次数 | 持续增长 |
| `misses` | Counter | 未命中次数 | 增长缓慢 |
| `evictions` | Counter | 淘汰次数 | < 10% hits |
| `cacheSize` | Gauge | 当前缓存项数 | < localMaxEntries |

## 🔧 性能调优

### 1. 调整本地缓存大小

**高并发场景：**
```yaml
basebackend:
  logging:
    hot-cache:
      local-max-entries: 4096  # 增加本地缓存
```

**低内存环境：**
```yaml
basebackend:
  logging:
    hot-cache:
      local-max-entries: 512   # 减少本地缓存
```

### 2. 调整热点阈值

**快速提升场景：**
```yaml
basebackend:
  logging:
    hot-cache:
      hot-threshold: 2  # 降低阈值，快速提升
```

**严格热点场景：**
```yaml
basebackend:
  logging:
    hot-cache:
      hot-threshold: 10 # 提高阈值，严格筛选
```

### 3. TTL优化

**高频访问场景：**
```yaml
basebackend:
  logging:
    hot-cache:
      ttl-seconds: 1800      # 30分钟
      jitter-seconds: 300    # 5分钟抖动
```

**低频访问场景：**
```yaml
basebackend:
  logging:
    hot-cache:
      ttl-seconds: 600       # 10分钟
      jitter-seconds: 60     # 1分钟抖动
```

## 🛠️ 故障排查

### 常见问题

#### 1. 缓存未命中

**原因分析：**
- 缓存键不匹配
- 数据未达到热点阈值
- TTL已过期

**解决方案：**
```java
// 检查缓存键
System.out.println("缓存键: " + cacheKey);

// 查看热点统计
List<String> hotKeys = cache.getHotKeys(10);
System.out.println("热点键: " + hotKeys);

// 检查TTL
Duration ttl = cache.resolveTtl(300);
System.out.println("TTL: " + ttl);
```

#### 2. Redis连接失败

**解决方案：**
```java
// 检查Redis连接
boolean connected = cache.ping();
if (!connected) {
    // 检查Redis服务状态
    // 检查网络连接
    // 检查配置信息
}
```

#### 3. 内存使用过高

**解决方案：**
```yaml
basebackend:
  logging:
    hot-cache:
      local-max-entries: 1024  # 减少本地缓存
      hot-threshold: 10        # 提高热点阈值
```

### 调试模式

启用调试日志：

```yaml
logging:
  level:
    com.basebackend.logging.cache: DEBUG
```

## 📝 最佳实践

### 1. 键命名规范

```java
// ✅ 推荐：使用冒号分隔的层次结构
"audit:latest"                    // 最新审计日志
"login:stats:daily"              // 每日登录统计
"user:profile:{userId}"          // 用户档案

// ❌ 避免：过于简单的键名
"data"                            // 不明确
"logs"                           // 太泛化
```

### 2. TTL设置原则

- **高频数据**：5-30分钟
- **中频数据**：30-120分钟
- **低频数据**：2-6小时
- **配置数据**：24小时以上

### 3. 预热策略

```yaml
basebackend:
  logging:
    hot-cache:
      preload-keys:
        - "system:config"           # 系统配置
        - "menu:tree"              # 菜单树
        - "dict:all"               # 字典表
```

### 4. 监控建议

- **必监控指标**：hitRate、cacheSize、evictions
- **告警阈值**：
  - hitRate < 80%
  - cacheSize > 90% localMaxEntries
  - evictions > 20% hits

## 🔗 相关资源

- [Spring Data Redis 文档](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [FastJSON2 文档](https://github.com/alibaba/fastjson2)
- [Redis 最佳实践](https://redis.io/docs/manual/)
- [basebackend-logging 主页](./README.md)

## 📄 许可证

本项目遵循 Apache License 2.0 许可证。

---

**更多详细信息和更新，请访问 [basebackend 项目主页](https://github.com/basebackend/basebackend)**
