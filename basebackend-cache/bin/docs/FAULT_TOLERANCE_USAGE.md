# 容错和降级机制使用指南

## 概述

basebackend-cache 模块提供了完善的容错和降级机制，确保在 Redis 故障时系统仍能正常运行。

## 核心特性

### 1. 超时控制

所有 Redis 操作都支持超时配置，防止操作长时间阻塞：

```yaml
basebackend:
  cache:
    resilience:
      # Redis 操作超时时间
      timeout: 3s
```

当操作超时时，系统会：
- 取消正在执行的操作
- 记录超时日志
- 根据降级配置返回默认值或抛出异常

### 2. 熔断器（Circuit Breaker）

熔断器模式防止系统在 Redis 持续故障时不断重试，减少资源浪费：

```yaml
basebackend:
  cache:
    resilience:
      circuit-breaker:
        # 是否启用熔断器
        enabled: true
        # 连续失败次数阈值
        failure-threshold: 5
        # 熔断器打开时长
        open-duration: 30s
        # 半开状态允许的请求数
        half-open-requests: 3
```

#### 熔断器状态

1. **CLOSED（关闭）**: 正常状态，所有请求正常执行
2. **OPEN（打开）**: 达到失败阈值后打开，所有请求直接降级
3. **HALF_OPEN（半开）**: 打开一段时间后进入半开状态，允许部分请求尝试恢复

#### 状态转换

```
CLOSED --[连续失败 >= threshold]--> OPEN
OPEN --[等待 open-duration]--> HALF_OPEN
HALF_OPEN --[成功 >= half-open-requests]--> CLOSED
HALF_OPEN --[任何失败]--> OPEN
```

### 3. 降级策略

当 Redis 操作失败时，系统可以自动降级：

```yaml
basebackend:
  cache:
    resilience:
      # 是否启用降级
      fallback-enabled: true
```

启用降级后：
- 读操作返回 null 或空集合
- 写操作静默失败（不抛出异常）
- 系统继续运行，但缓存功能暂时不可用

禁用降级后：
- 所有失败操作抛出 `CacheConnectionException`
- 调用方需要处理异常

### 4. 自动恢复检测

系统会定期检测 Redis 连接状态，自动恢复：

```yaml
basebackend:
  cache:
    resilience:
      auto-recovery:
        # 是否启用自动恢复检测
        enabled: true
        # 检测间隔
        check-interval: 10s
```

自动恢复机制：
- 定期执行 Redis ping 操作
- 检测 Redis 是否恢复可用
- 记录恢复状态日志

## 使用示例

### 基本使用

```java
@Autowired
private RedisService redisService;

public void example() {
    // 所有操作都自动包含容错逻辑
    redisService.set("key", "value");
    Object value = redisService.get("key");
    
    // 检查 Redis 是否可用
    if (redisService.isRedisAvailable()) {
        // Redis 可用（熔断器关闭）
    } else {
        // Redis 不可用（熔断器打开）
    }
    
    // 获取熔断器状态
    String state = redisService.getCircuitBreakerState(); // CLOSED, OPEN, HALF_OPEN
    
    // 获取连续失败次数
    int failures = redisService.getConsecutiveFailures();
}
```

### 手动控制熔断器

```java
// 手动重置熔断器（用于测试或手动恢复）
redisService.resetCircuitBreaker();

// 手动打开熔断器（用于测试或主动降级）
redisService.openCircuitBreaker();
```

## 监控和日志

### 日志级别

- **ERROR**: Redis 连接失败、操作超时、熔断器打开
- **WARN**: 使用降级值、连续失败警告、熔断器状态变化
- **INFO**: 熔断器恢复、Redis 连接恢复
- **DEBUG**: 失败计数重置、半开状态成功计数

### 关键日志示例

```
# 连接失败
ERROR - Redis connection failed for operation: get, error: Connection refused

# 熔断器打开
ERROR - Circuit breaker opening after 5 consecutive failures

# 熔断器状态转换
INFO - Circuit breaker transitioning from OPEN to HALF_OPEN
INFO - Circuit breaker transitioning from HALF_OPEN to CLOSED after 3 successful requests

# 使用降级
WARN - Circuit breaker is OPEN for operation: get, using fallback
WARN - Using fallback for operation: get, reason: Redis connection failure
```

## 配置建议

### 开发环境

```yaml
basebackend:
  cache:
    resilience:
      fallback-enabled: true
      timeout: 5s
      circuit-breaker:
        enabled: false  # 开发环境可以禁用，方便调试
```

### 生产环境

```yaml
basebackend:
  cache:
    resilience:
      fallback-enabled: true
      timeout: 3s
      circuit-breaker:
        enabled: true
        failure-threshold: 5
        open-duration: 30s
        half-open-requests: 3
      auto-recovery:
        enabled: true
        check-interval: 10s
```

### 高可用环境

```yaml
basebackend:
  cache:
    resilience:
      fallback-enabled: true
      timeout: 2s
      circuit-breaker:
        enabled: true
        failure-threshold: 3  # 更快触发熔断
        open-duration: 60s    # 更长的恢复时间
        half-open-requests: 5 # 更多的恢复验证
      auto-recovery:
        enabled: true
        check-interval: 5s    # 更频繁的检测
```

## 最佳实践

1. **始终启用降级**: 在生产环境中，应该始终启用降级，避免 Redis 故障导致整个系统不可用

2. **合理设置超时**: 超时时间应该根据网络延迟和业务需求设置，通常 2-5 秒比较合适

3. **调整失败阈值**: 失败阈值应该根据系统的容错能力设置，通常 3-10 次比较合适

4. **监控熔断器状态**: 应该监控熔断器的打开和关闭事件，及时发现 Redis 故障

5. **配合多级缓存**: 结合本地缓存使用，可以进一步提高系统的容错能力

## 故障场景处理

### 场景 1: Redis 完全不可用

1. 前 5 次请求失败，记录错误日志
2. 第 5 次失败后，熔断器打开
3. 后续请求直接降级，不再访问 Redis
4. 30 秒后进入半开状态
5. 3 次成功后熔断器关闭，恢复正常

### 场景 2: Redis 间歇性故障

1. 部分请求失败，失败计数增加
2. 成功请求会重置失败计数
3. 如果连续失败达到阈值，触发熔断
4. 否则系统继续正常运行

### 场景 3: Redis 操作超时

1. 操作超过配置的超时时间
2. 取消操作，记录超时日志
3. 失败计数增加
4. 根据降级配置返回默认值或抛出异常

## 性能影响

- **超时控制**: 使用线程池执行操作，有轻微的性能开销（< 1ms）
- **熔断器**: 状态检查非常快，几乎没有性能影响
- **降级**: 直接返回默认值，性能影响可忽略
- **自动恢复**: 定期执行，不影响正常请求

## 注意事项

1. 超时时间不应该设置得太短，否则可能导致正常操作也被取消
2. 失败阈值不应该设置得太低，否则可能导致熔断器过于敏感
3. 熔断器打开时长应该足够长，给 Redis 足够的恢复时间
4. 自动恢复检测间隔不应该太短，避免频繁的 ping 操作
5. 在使用降级时，应用程序应该能够处理缓存未命中的情况

## 相关文档

- [缓存预热使用指南](CACHE_WARMING_USAGE.md)
- [分布式锁使用指南](DISTRIBUTED_LOCK_USAGE.md)
- [缓存配置参考](../src/main/resources/application-cache.yml)
