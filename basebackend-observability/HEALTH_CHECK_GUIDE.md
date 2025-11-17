# 健康检查使用指南

## 📊 概述

本项目提供了完整的健康检查体系，包括：

1. **系统健康检查** - 数据库、Redis、RocketMQ、磁盘空间
2. **应用程序健康检查** - 内存、线程、运行时间
3. **Kubernetes 探针** - Liveness Probe 和 Readiness Probe
4. **自定义健康检查** - 可扩展的健康检查机制

## 🎯 快速开始

### 1. 访问健康检查端点

所有健康检查端点都在 `/actuator` 路径下：

```bash
# 查看所有健康检查
curl http://localhost:8080/actuator/health

# 查看详细健康信息
curl http://localhost:8080/actuator/health | jq .
```

**响应示例：**

```json
{
  "status": "UP",
  "components": {
    "application": {
      "status": "UP",
      "details": {
        "memory": {
          "used": "256.50 MB",
          "max": "2.00 GB",
          "usagePercent": "12.50%"
        },
        "threads": {
          "count": 45,
          "peak": 52,
          "daemon": 38,
          "deadlocked": 0
        },
        "uptime": "2h 15m 30s"
      }
    },
    "database": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "version": "8.0.33",
        "responseTime": "5ms",
        "pool": {
          "active": 2,
          "idle": 8,
          "total": 10,
          "maxPoolSize": 20,
          "poolUtilization": "10.00%"
        }
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": "500.00 GB",
        "free": "250.00 GB",
        "usable": "240.00 GB",
        "usedPercent": "50.00%"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "ping": "PONG",
        "responseTime": "2ms",
        "version": "7.0.5",
        "readWriteTest": "passed"
      }
    },
    "rocketMQ": {
      "status": "UP",
      "details": {
        "producerRunning": true,
        "status": "connected"
      }
    }
  }
}
```

### 2. 健康检查状态说明

健康检查有以下几种状态：

- **UP** - 组件正常运行 ✅
- **DOWN** - 组件故障，需要立即处理 ❌
- **OUT_OF_SERVICE** - 组件暂时停止服务 ⚠️
- **UNKNOWN** - 组件状态未知 ❓

### 3. Kubernetes 健康探针

#### Liveness Probe（存活探针）

检查应用是否存活，如果失败 Kubernetes 会重启容器。

```yaml
# Kubernetes Deployment 配置
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

**端点：**
```bash
curl http://localhost:8080/actuator/health/liveness
```

**响应：**
```json
{
  "status": "UP"
}
```

#### Readiness Probe（就绪探针）

检查应用是否准备好接收流量，如果失败 Kubernetes 会停止向该 Pod 发送流量。

```yaml
# Kubernetes Deployment 配置
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

**端点：**
```bash
curl http://localhost:8080/actuator/health/readiness
```

**响应：**
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "application": {
      "status": "UP"
    }
  }
}
```

## 📋 健康检查详解

### 1. 数据库健康检查（DatabaseHealthIndicator）

**检查项：**
- ✅ 数据库连接有效性
- ✅ 连接池状态（活跃连接、空闲连接、总连接数）
- ✅ 连接池利用率
- ✅ 等待连接的线程数
- ✅ 数据库响应时间

**故障触发条件：**
- ❌ 数据库连接无效
- ❌ 连接池利用率超过 90%
- ❌ 有线程等待数据库连接
- ❌ 响应时间超过 5 秒

**响应示例：**
```json
{
  "status": "UP",
  "details": {
    "database": "MySQL",
    "version": "8.0.33",
    "responseTime": "5ms",
    "catalog": "basebackend",
    "pool": {
      "active": 2,
      "idle": 8,
      "total": 10,
      "threadsAwaitingConnection": 0,
      "maxPoolSize": 20,
      "minIdle": 5,
      "poolUtilization": "10.00%"
    }
  }
}
```

### 2. Redis 健康检查（RedisHealthIndicator）

**检查项：**
- ✅ Redis PING 命令响应
- ✅ Redis 连接响应时间
- ✅ Redis 读写测试
- ✅ Redis 版本信息

**故障触发条件：**
- ❌ PING 命令失败
- ❌ 响应时间超过 1 秒
- ❌ 读写测试失败

**响应示例：**
```json
{
  "status": "UP",
  "details": {
    "ping": "PONG",
    "responseTime": "2ms",
    "version": "7.0.5",
    "readWriteTest": "passed"
  }
}
```

### 3. RocketMQ 健康检查（RocketMQHealthIndicator）

**检查项：**
- ✅ Producer 是否启动
- ✅ RocketMQ 连接状态

**故障触发条件：**
- ❌ Producer 未运行
- ❌ 连接失败

**响应示例：**
```json
{
  "status": "UP",
  "details": {
    "producerRunning": true,
    "status": "connected"
  }
}
```

### 4. 磁盘空间健康检查（DiskSpaceHealthIndicator）

**检查项：**
- ✅ 磁盘总空间
- ✅ 磁盘可用空间
- ✅ 磁盘使用率

**故障触发条件：**
- ❌ 可用空间低于 10GB
- ❌ 磁盘使用率超过 90%

**响应示例：**
```json
{
  "status": "UP",
  "details": {
    "total": "500.00 GB",
    "free": "250.00 GB",
    "usable": "240.00 GB",
    "used": "250.00 GB",
    "usedPercent": "50.00%",
    "threshold": "10.00 GB",
    "path": "/"
  }
}
```

### 5. 应用程序健康检查（ApplicationHealthIndicator）

**检查项：**
- ✅ JVM 堆内存使用情况
- ✅ 线程数量（总数、峰值、守护线程）
- ✅ 死锁检测
- ✅ 系统负载
- ✅ 应用运行时间

**故障触发条件：**
- ❌ 内存使用率超过 90%
- ❌ 线程数超过 1000
- ❌ 检测到死锁线程

**响应示例：**
```json
{
  "status": "UP",
  "details": {
    "memory": {
      "used": "256.50 MB",
      "max": "2.00 GB",
      "usagePercent": "12.50%"
    },
    "threads": {
      "count": 45,
      "peak": 52,
      "daemon": 38,
      "deadlocked": 0
    },
    "systemLoadAverage": "1.25",
    "uptime": "2h 15m 30s"
  }
}
```

## 🔧 配置说明

### 健康检查配置

在 `observability-config.yml` 中配置：

```yaml
management:
  health:
    # 数据库健康检查
    db:
      enabled: true
    # Redis 健康检查
    redis:
      enabled: true
    # 磁盘空间健康检查
    diskspace:
      enabled: true
      threshold: 10GB
    # 显示详细信息
    show-details: always  # always, when-authorized, never
    show-components: always
    # 健康检查组
    group:
      # 存活探针
      liveness:
        include: ping
        show-details: never
      # 就绪探针
      readiness:
        include: db,redis,diskSpace,application
        show-details: when-authorized
```

### 自定义阈值

#### 磁盘空间阈值

修改 `DiskSpaceHealthIndicator.java`：

```java
// 磁盘空间阈值（字节）
private static final long THRESHOLD_BYTES = 1024L * 1024 * 1024 * 10; // 10GB

// 磁盘空间使用率阈值
private static final double THRESHOLD_PERCENT = 0.9; // 90%
```

#### 内存使用率阈值

修改 `ApplicationHealthIndicator.java`：

```java
// 内存使用率阈值
private static final double MEMORY_THRESHOLD = 0.9; // 90%
```

#### 线程数阈值

修改 `ApplicationHealthIndicator.java`：

```java
// 线程数阈值
private static final int THREAD_COUNT_THRESHOLD = 1000;
```

## 🎨 创建自定义健康检查

### 1. 实现 HealthIndicator 接口

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // 执行健康检查逻辑
            boolean isHealthy = checkSomething();

            if (isHealthy) {
                return Health.up()
                        .withDetail("customCheck", "passed")
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build();
            } else {
                return Health.down()
                        .withDetail("customCheck", "failed")
                        .withDetail("reason", "Something went wrong")
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkSomething() {
        // 自定义检查逻辑
        return true;
    }
}
```

### 2. 条件化健康检查

使用 `@ConditionalOnProperty` 控制健康检查是否启用：

```java
@Component
@ConditionalOnProperty(
    prefix = "custom.health",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class ConditionalHealthIndicator implements HealthIndicator {
    // ...
}
```

配置文件：

```yaml
custom:
  health:
    enabled: true
```

### 3. 响应式健康检查

对于异步/响应式应用，实现 `ReactiveHealthIndicator`：

```java
@Component
public class ReactiveCustomHealthIndicator implements ReactiveHealthIndicator {

    @Override
    public Mono<Health> health() {
        return checkHealthAsync()
                .map(healthy -> healthy ?
                        Health.up().build() :
                        Health.down().build())
                .onErrorResume(ex ->
                        Mono.just(Health.down()
                                .withException(ex)
                                .build()));
    }

    private Mono<Boolean> checkHealthAsync() {
        // 异步健康检查逻辑
        return Mono.just(true);
    }
}
```

## 📊 监控和告警

### 1. Prometheus 集成

健康检查状态会自动导出到 Prometheus：

```promql
# 应用健康状态（1=UP, 0=DOWN）
health_status{application="basebackend-admin-api"}

# 各组件健康状态
health_component_status{component="database"} 1
health_component_status{component="redis"} 1
health_component_status{component="diskSpace"} 1
```

### 2. Grafana 告警

在 Grafana 中配置告警规则：

```yaml
# 数据库健康检查失败告警
alert: DatabaseHealthCheckFailed
expr: health_component_status{component="database"} == 0
for: 1m
labels:
  severity: critical
annotations:
  summary: "Database health check failed"
  description: "Database is DOWN for {{ $labels.application }}"

# 磁盘空间不足告警
alert: DiskSpaceInsufficient
expr: health_component_status{component="diskSpace"} == 0
for: 5m
labels:
  severity: warning
annotations:
  summary: "Disk space insufficient"
  description: "Disk space is running low for {{ $labels.application }}"
```

## 🆘 常见问题

### Q1: 健康检查端点返回 404？

**A:** 检查以下几点：
1. 确认 `/actuator/health` 端点已暴露
2. 检查 `observability-config.yml` 中的配置
3. 确认应用已启用 Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### Q2: 健康检查显示 DOWN，但系统实际正常？

**A:** 可能是阈值设置过于严格：
1. 检查各健康检查器的阈值配置
2. 查看 `details` 中的具体错误信息
3. 根据实际情况调整阈值

### Q3: 如何禁用某个健康检查？

**A:** 在配置文件中禁用：

```yaml
management:
  health:
    redis:
      enabled: false
```

### Q4: Kubernetes 探针失败导致频繁重启？

**A:**
1. 增加 `initialDelaySeconds` 给应用更多启动时间
2. 增加 `failureThreshold` 允许更多失败次数
3. 检查 `readinessProbe` 是否包含了不必要的检查项

```yaml
readinessProbe:
  initialDelaySeconds: 30  # 增加到 30 秒
  failureThreshold: 5      # 允许 5 次失败
```

## 📚 最佳实践

### 1. 合理设置阈值

✅ **好的做法：**
- 根据历史数据和实际负载设置阈值
- 预留足够的缓冲空间（如磁盘空间保留 20%）
- 定期审查和调整阈值

❌ **不好的做法：**
- 阈值设置过于严格，导致误报
- 阈值设置过于宽松，错过真实故障

### 2. 区分 Liveness 和 Readiness

**Liveness Probe 应该简单快速：**
```yaml
liveness:
  include: ping  # 只检查应用是否存活
```

**Readiness Probe 应该全面：**
```yaml
readiness:
  include: db,redis,diskSpace,application  # 检查依赖服务
```

### 3. 健康检查超时设置

确保健康检查不会因为超时而失败：

```java
// 设置合理的超时时间
Connection connection = dataSource.getConnection();
if (!connection.isValid(5)) {  // 5 秒超时
    // ...
}
```

### 4. 日志记录

在健康检查中适当记录日志：

```java
// ✅ 只在失败时记录 ERROR
if (error) {
    log.error("Health check failed", e);
}

// ✅ 成功时记录 DEBUG
log.debug("Health check passed");

// ❌ 避免每次都记录 INFO（会产生大量日志）
log.info("Health check executed");  // 不推荐
```

## 🔗 参考资料

- [Spring Boot Actuator Health](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)
- [Kubernetes Probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/)
- [Micrometer Health Indicators](https://micrometer.io/docs/concepts#_health_indicators)

---

*最后更新: 2025-01-13*
*维护者: BaseBackend Team*
