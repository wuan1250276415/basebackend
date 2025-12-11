# 缓存配置示例

本文档提供不同场景下的缓存配置示例。

## 目录

- [开发环境配置](#开发环境配置)
- [测试环境配置](#测试环境配置)
- [生产环境配置](#生产环境配置)
- [高可用配置](#高可用配置)
- [高性能配置](#高性能配置)
- [特定场景配置](#特定场景配置)

## 开发环境配置

适用于本地开发，注重调试便利性。

```yaml
# application-dev.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password: 
      timeout: 5s

basebackend:
  cache:
    # 启用缓存
    enabled: true
    
    # 禁用多级缓存，方便调试
    multi-level:
      enabled: false
    
    # 启用指标收集
    metrics:
      enabled: true
      low-hit-rate-threshold: 0.3  # 开发环境降低阈值
      export-to-micrometer: true
    
    # 禁用缓存预热
    warming:
      enabled: false
    
    # 使用 JSON 序列化，便于查看
    serialization:
      type: json
      json:
        pretty-print: true  # 格式化输出
    
    # 启用降级，但禁用熔断器
    resilience:
      fallback-enabled: true
      timeout: 10s  # 较长的超时时间
      circuit-breaker:
        enabled: false  # 开发环境禁用熔断器
    
    # 缓存键配置
    key:
      prefix: "dev"
      include-app-name: true
    
    # 分布式锁配置
    lock:
      default-wait-time: 30s  # 较长的等待时间
      default-lease-time: 60s

# 日志配置
logging:
  level:
    com.basebackend.cache: DEBUG  # 详细日志
```

## 测试环境配置

适用于集成测试和 QA 测试。

```yaml
# application-test.yml
spring:
  data:
    redis:
      host: redis-test.example.com
      port: 6379
      database: 1
      password: ${REDIS_PASSWORD}
      timeout: 3s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2

basebackend:
  cache:
    enabled: true
    
    # 启用多级缓存
    multi-level:
      enabled: true
      local-max-size: 500
      local-ttl: 3m
      eviction-policy: LRU
    
    # 启用指标收集
    metrics:
      enabled: true
      low-hit-rate-threshold: 0.5
      export-to-micrometer: true
    
    # 启用缓存预热
    warming:
      enabled: true
      timeout: 3m
      async: true
    
    # 使用 JSON 序列化
    serialization:
      type: json
    
    # 启用容错机制
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
    
    # 缓存键配置
    key:
      prefix: "test"
      include-app-name: true
    
    # 缓存模板配置
    template:
      cache-aside:
        default-ttl: 30m
        bloom-filter-enabled: true
    
    # 分布式锁配置
    lock:
      default-wait-time: 10s
      default-lease-time: 30s

logging:
  level:
    com.basebackend.cache: INFO
```

## 生产环境配置

适用于生产环境，注重性能和可靠性。

```yaml
# application-prod.yml
spring:
  data:
    redis:
      host: redis-prod.example.com
      port: 6379
      database: 0
      password: ${REDIS_PASSWORD}
      timeout: 2s
      lettuce:
        pool:
          max-active: 64
          max-idle: 32
          min-idle: 8
          max-wait: 1s

basebackend:
  cache:
    enabled: true
    
    # 启用多级缓存
    multi-level:
      enabled: true
      local-max-size: 2000
      local-ttl: 5m
      eviction-policy: LRU
    
    # 启用指标收集
    metrics:
      enabled: true
      low-hit-rate-threshold: 0.6
      export-to-micrometer: true
    
    # 启用缓存预热
    warming:
      enabled: true
      timeout: 5m
      async: true
    
    # 使用 Kryo 序列化（高性能）
    serialization:
      type: kryo
      kryo:
        enabled: true
        register-required: false
    
    # 启用完整的容错机制
    resilience:
      fallback-enabled: true
      timeout: 2s
      circuit-breaker:
        enabled: true
        failure-threshold: 3
        open-duration: 60s
        half-open-requests: 5
      auto-recovery:
        enabled: true
        check-interval: 5s
    
    # 缓存键配置
    key:
      prefix: "prod"
      include-app-name: true
    
    # 缓存模板配置
    template:
      cache-aside:
        default-ttl: 1h
        bloom-filter-enabled: true
      write-behind:
        enabled: true
        batch-size: 100
        batch-interval: 5s
    
    # 分布式锁配置
    lock:
      default-wait-time: 5s
      default-lease-time: 30s
      fair-lock-enabled: false

# Redisson 配置
redisson:
  single-server-config:
    address: redis://${spring.data.redis.host}:${spring.data.redis.port}
    database: ${spring.data.redis.database}
    password: ${spring.data.redis.password}
    connect-timeout: 2000
    timeout: 2000
    connection-pool-size: 64
    connection-minimum-idle-size: 16
    retry-attempts: 3
    retry-interval: 1000

logging:
  level:
    com.basebackend.cache: WARN
```

## 高可用配置

适用于对可用性要求极高的场景。

```yaml
# application-ha.yml
spring:
  data:
    redis:
      # Redis 哨兵模式
      sentinel:
        master: mymaster
        nodes:
          - sentinel1.example.com:26379
          - sentinel2.example.com:26379
          - sentinel3.example.com:26379
      password: ${REDIS_PASSWORD}
      timeout: 2s
      lettuce:
        pool:
          max-active: 128
          max-idle: 64
          min-idle: 16

basebackend:
  cache:
    enabled: true
    
    # 启用多级缓存作为备份
    multi-level:
      enabled: true
      local-max-size: 5000
      local-ttl: 10m
      eviction-policy: LRU
    
    metrics:
      enabled: true
      low-hit-rate-threshold: 0.7
      export-to-micrometer: true
    
    warming:
      enabled: true
      timeout: 10m
      async: true
    
    serialization:
      type: kryo
    
    # 强化容错机制
    resilience:
      fallback-enabled: true
      timeout: 1s  # 更短的超时
      circuit-breaker:
        enabled: true
        failure-threshold: 2  # 更快触发熔断
        open-duration: 120s  # 更长的恢复时间
        half-open-requests: 10
      auto-recovery:
        enabled: true
        check-interval: 3s  # 更频繁的检测
    
    key:
      prefix: "ha"
      include-app-name: true
    
    template:
      cache-aside:
        default-ttl: 2h
        bloom-filter-enabled: true
      write-behind:
        enabled: true
        batch-size: 200
        batch-interval: 3s
    
    lock:
      default-wait-time: 3s
      default-lease-time: 20s
      red-lock-enabled: true  # 启用红锁

# Redisson 哨兵配置
redisson:
  sentinel-servers-config:
    master-name: mymaster
    sentinel-addresses:
      - redis://sentinel1.example.com:26379
      - redis://sentinel2.example.com:26379
      - redis://sentinel3.example.com:26379
    password: ${REDIS_PASSWORD}
    database: 0
    connect-timeout: 2000
    timeout: 2000
    master-connection-pool-size: 64
    slave-connection-pool-size: 64
    retry-attempts: 5
    retry-interval: 1000

logging:
  level:
    com.basebackend.cache: INFO
```

## 高性能配置

适用于对性能要求极高的场景。

```yaml
# application-perf.yml
spring:
  data:
    redis:
      # Redis 集群模式
      cluster:
        nodes:
          - redis1.example.com:6379
          - redis2.example.com:6379
          - redis3.example.com:6379
          - redis4.example.com:6379
          - redis5.example.com:6379
          - redis6.example.com:6379
        max-redirects: 3
      password: ${REDIS_PASSWORD}
      timeout: 1s
      lettuce:
        pool:
          max-active: 256
          max-idle: 128
          min-idle: 32

basebackend:
  cache:
    enabled: true
    
    # 大容量本地缓存
    multi-level:
      enabled: true
      local-max-size: 10000
      local-ttl: 15m
      eviction-policy: LRU
    
    metrics:
      enabled: true
      low-hit-rate-threshold: 0.8
      export-to-micrometer: true
    
    warming:
      enabled: true
      timeout: 15m
      async: true
    
    # 使用最快的序列化器
    serialization:
      type: kryo
      kryo:
        enabled: true
        register-required: true  # 预注册类提升性能
    
    resilience:
      fallback-enabled: true
      timeout: 500ms  # 极短的超时
      circuit-breaker:
        enabled: true
        failure-threshold: 3
        open-duration: 30s
        half-open-requests: 5
    
    key:
      prefix: "perf"
      separator: ":"
      include-app-name: false  # 减少键长度
    
    template:
      cache-aside:
        default-ttl: 4h
        bloom-filter-enabled: true
      write-behind:
        enabled: true
        batch-size: 500  # 大批量
        batch-interval: 2s  # 短间隔
    
    lock:
      default-wait-time: 1s
      default-lease-time: 10s

# Redisson 集群配置
redisson:
  cluster-servers-config:
    node-addresses:
      - redis://redis1.example.com:6379
      - redis://redis2.example.com:6379
      - redis://redis3.example.com:6379
      - redis://redis4.example.com:6379
      - redis://redis5.example.com:6379
      - redis://redis6.example.com:6379
    password: ${REDIS_PASSWORD}
    connect-timeout: 1000
    timeout: 1000
    master-connection-pool-size: 128
    slave-connection-pool-size: 128
    retry-attempts: 3
    retry-interval: 500

logging:
  level:
    com.basebackend.cache: ERROR  # 最小日志开销
```

## 特定场景配置

### 电商场景

```yaml
# application-ecommerce.yml
basebackend:
  cache:
    enabled: true
    
    multi-level:
      enabled: true
      local-max-size: 3000
      local-ttl: 5m
    
    warming:
      enabled: true
      # 预热热门商品、分类等
    
    serialization:
      type: json  # 便于调试商品数据
    
    template:
      cache-aside:
        default-ttl: 30m  # 商品信息
        bloom-filter-enabled: true  # 防止恶意查询
      write-behind:
        enabled: true  # 浏览记录、点击统计等
        batch-size: 200
        batch-interval: 5s
    
    lock:
      default-wait-time: 5s
      default-lease-time: 30s
```

### 社交媒体场景

```yaml
# application-social.yml
basebackend:
  cache:
    enabled: true
    
    multi-level:
      enabled: true
      local-max-size: 5000
      local-ttl: 3m  # 较短的本地缓存
    
    serialization:
      type: kryo  # 高性能序列化
    
    template:
      cache-aside:
        default-ttl: 10m  # 动态内容更新快
      write-behind:
        enabled: true  # 点赞、评论计数等
        batch-size: 500
        batch-interval: 2s
    
    lock:
      default-wait-time: 3s
      default-lease-time: 15s
```

### 金融场景

```yaml
# application-finance.yml
basebackend:
  cache:
    enabled: true
    
    multi-level:
      enabled: false  # 金融数据要求强一致性
    
    serialization:
      type: json  # 便于审计
    
    resilience:
      fallback-enabled: false  # 不允许降级
      timeout: 1s
      circuit-breaker:
        enabled: true
        failure-threshold: 2
    
    template:
      cache-aside:
        default-ttl: 5m  # 较短的 TTL
        bloom-filter-enabled: false
      write-through:
        enabled: true  # 使用 Write-Through 保证一致性
    
    lock:
      default-wait-time: 10s
      default-lease-time: 60s
      fair-lock-enabled: true  # 使用公平锁
```

### 内容管理场景

```yaml
# application-cms.yml
basebackend:
  cache:
    enabled: true
    
    multi-level:
      enabled: true
      local-max-size: 2000
      local-ttl: 30m  # 内容变化慢
    
    warming:
      enabled: true
      # 预热首页、热门文章等
    
    serialization:
      type: json
    
    template:
      cache-aside:
        default-ttl: 2h  # 较长的 TTL
        bloom-filter-enabled: false
    
    lock:
      default-wait-time: 5s
      default-lease-time: 30s
```

## 配置优先级

配置加载顺序（后面的会覆盖前面的）：

1. `application.yml` - 默认配置
2. `application-{profile}.yml` - 环境特定配置
3. 环境变量
4. 系统属性
5. 命令行参数

## 环境变量示例

```bash
# Redis 连接
export SPRING_DATA_REDIS_HOST=redis.example.com
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DATA_REDIS_PASSWORD=your_password

# 缓存配置
export BASEBACKEND_CACHE_ENABLED=true
export BASEBACKEND_CACHE_MULTI_LEVEL_ENABLED=true
export BASEBACKEND_CACHE_SERIALIZATION_TYPE=kryo

# 容错配置
export BASEBACKEND_CACHE_RESILIENCE_TIMEOUT=3s
export BASEBACKEND_CACHE_RESILIENCE_CIRCUIT_BREAKER_ENABLED=true
```

## 配置验证

启动时会自动验证配置，如果配置无效会抛出 `CacheConfigurationException`：

```
CacheConfigurationException: Invalid serialization type: invalid_type. 
Supported types: json, protobuf, kryo
```

## 总结

选择合适的配置对于系统性能和可靠性至关重要：

- **开发环境**：注重调试便利性
- **测试环境**：模拟生产环境
- **生产环境**：平衡性能和可靠性
- **高可用环境**：最大化可用性
- **高性能环境**：最大化性能

根据实际业务场景调整配置参数，并通过监控指标持续优化。
