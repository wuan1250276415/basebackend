# Monitor Service - 数据存储说明

## 概述

监控服务（basebackend-monitor-service）**不需要独立的数据库**，所有数据存储在 Redis 中，服务器信息通过 JMX（Java Management API）实时获取。

## 数据存储方式

### 1. Redis 存储

监控服务使用 Redis 存储以下数据：

#### 在线用户信息

- **Key 格式**: `online_users:{userId}`
- **数据类型**: Hash
- **存储内容**:
  ```json
  {
    "userId": 1,
    "username": "admin",
    "nickname": "管理员",
    "deptName": "技术部",
    "loginIp": "127.0.0.1",
    "loginLocation": "本地",
    "browser": "Chrome",
    "os": "Windows 10",
    "loginTime": 1732435200000,
    "lastAccessTime": 1732438800000,
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
  ```
- **过期时间**: 根据 JWT token 过期时间设置（默认 24 小时）

#### 登录令牌

- **Key 格式**: `login_tokens:{username}`
- **数据类型**: String
- **存储内容**: JWT token 字符串
- **过期时间**: 与 JWT token 过期时间一致

### 2. JMX 实时监控

服务器信息通过 Java Management API 实时获取，不需要持久化存储：

- **内存使用情况**: MemoryMXBean
- **CPU 负载**: OperatingSystemMXBean
- **JVM 运行时信息**: RuntimeMXBean
- **线程信息**: ThreadMXBean

### 3. 缓存信息统计

缓存信息通过 Redis INFO 命令实时获取，当前实现返回模拟数据：

```java
// TODO: 实现真实的缓存信息统计
// 可以通过以下方式获取真实数据：
// 1. RedisTemplate.execute((RedisCallback<Properties>) connection -> connection.info())
// 2. Redisson.getNodesGroup().pingAll()
// 3. Spring Cache Statistics
```

## Redis 配置要求

### 共享 Redis 实例

监控服务使用与其他服务相同的 Redis 实例：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:1.117.67.222}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis_ycecQi}
      database: ${REDIS_DATABASE:0}
```

### Key 命名空间

为避免 key 冲突，监控服务使用以下命名空间：

- `online_users:*` - 在线用户信息
- `login_tokens:*` - 登录令牌

## 数据初始化

监控服务**不需要任何数据库初始化脚本**，启动后即可使用。在线用户数据由用户登录时自动创建。

## 数据清理

### 自动清理

- Redis 的 TTL 机制会自动清理过期的在线用户数据
- 用户登出时会主动删除对应的 Redis keys

### 手动清理

管理员可以通过监控服务提供的 API 手动清理：

```bash
# 强制用户下线（删除指定用户的在线数据）
DELETE /api/monitor/online/{token}

# 清空指定缓存
DELETE /api/monitor/cache/{cacheName}

# 清空所有缓存（危险操作）
DELETE /api/monitor/cache
```

## 监控指标

### 实时指标

- 当前在线用户数
- 内存使用率
- CPU 负载
- JVM 运行时间
- 缓存命中率（模拟）

### 历史数据

监控服务不存储历史数据。如需历史监控数据，建议：

1. 集成 Prometheus + Grafana
2. 使用 Spring Boot Actuator + Micrometer
3. 接入日志分析系统（ELK）

## 扩展性

### 未来可能的改进

1. **持久化历史监控数据**
   - 使用 InfluxDB 或 Prometheus 存储时序数据
   - 生成历史趋势图表

2. **实时告警**
   - 集成告警服务（如 AlertManager）
   - 内存/CPU 阈值告警

3. **分布式追踪**
   - 集成 SkyWalking 或 Zipkin
   - 链路追踪和性能分析

## 总结

监控服务是一个**无状态的、轻量级的监控服务**，依赖于：

- ✅ Redis（存储在线用户信息）
- ✅ JMX（获取 JVM 和系统信息）
- ❌ 不需要独立数据库
- ❌ 不需要数据库初始化脚本

这种设计保证了监控服务的简洁性和高可用性。
