# 监控模块使用示例

## 目录

1. [快速开始](#快速开始)
2. [JVM监控](#jvm监控)
3. [SQL性能分析](#sql性能分析)
4. [日志分析](#日志分析)
5. [服务拓扑](#服务拓扑)
6. [线程分析](#线程分析)
7. [Arthas调试](#arthas调试)
8. [告警配置](#告警配置)
9. [统计分析](#统计分析)
10. [系统维护](#系统维护)

---

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-observability</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置文件

```yaml
spring:
  profiles:
    include: observability

observability:
  enabled: true
  profiling:
    enabled: true
    jvm-metrics:
      collect-interval: 10000
    sql:
      slow-threshold: 1000
  tracing:
    enabled: true
  alerting:
    enabled: true
```

### 3. 启动应用

模块会自动启动JVM监控、SQL监控等功能。

---

## JVM监控

### 查看实时JVM指标

```bash
curl http://localhost:8080/api/observability/profiling/jvm/metrics

# 响应
{
  "code": 200,
  "data": {
    "metrics": {
      "heapUsed": 536870912,
      "heapMax": 1073741824,
      "threadCount": 120,
      "cpuUsage": 35.6
    },
    "heapUsagePercent": 50.0
  }
}
```

### 查看历史趋势

```bash
# 查看最近24小时的JVM趋势
curl http://localhost:8080/api/observability/profiling/jvm/history?hours=24

# 查看性能趋势图
curl http://localhost:8080/api/observability/statistics/performance-trend?hours=24
```

### Java代码示例

```java
@RestController
public class MyController {
    
    @Autowired
    private JvmMetricsCollector jvmCollector;
    
    @GetMapping("/custom-metrics")
    public void collectCustomMetrics() {
        // JVM监控会自动每10秒采集
        // 也可以手动触发采集
        jvmCollector.collectJvmMetrics();
    }
}
```

---

## SQL性能分析

### 查看慢SQL

```bash
# 查看最近1小时的慢SQL
curl http://localhost:8080/api/observability/profiling/sql/slow?hours=1

# 查看Top 10慢SQL
curl http://localhost:8080/api/observability/profiling/sql/top?topN=10

# 按方法聚合统计
curl http://localhost:8080/api/observability/profiling/sql/aggregate?hours=24
```

### 慢SQL自动监控

SQL监控通过AOP自动拦截，无需额外配置：

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 所有方法都会被自动监控
    User selectById(Long id);  // 如果执行>1秒，会自动记录
}
```

### 配置慢SQL阈值

```yaml
observability:
  profiling:
    sql:
      slow-threshold: 1000  # 1秒，可根据需要调整
```

---

## 日志分析

### 全文搜索

```bash
curl -X POST http://localhost:8080/api/observability/logs/search \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "NullPointerException",
    "services": ["user-service"],
    "levels": ["ERROR"],
    "startTime": "2025-10-24T00:00:00",
    "endTime": "2025-10-24T23:59:59",
    "from": 0,
    "size": 20
  }'
```

### 查看日志上下文

```bash
# 获取指定日志的前后10条
curl http://localhost:8080/api/observability/logs/context/log-123?before=10&after=10
```

### 异常聚合

```bash
# 查看Top异常
curl http://localhost:8080/api/observability/logs/exceptions/top?limit=10&hours=24
```

### WebSocket实时日志流

```javascript
// 前端订阅实时日志
const ws = new WebSocket('ws://localhost:8080/ws/logs/user-service');

ws.onmessage = function(event) {
    const log = JSON.parse(event.data);
    console.log('New log:', log);
    
    // 在界面上实时显示
    displayLog(log);
};
```

### Java代码示例

```java
@Service
public class MyService {
    
    @Autowired
    private ExceptionAggregationService exceptionService;
    
    public void handleException(Exception e) {
        // 记录异常到聚合系统
        exceptionService.recordException(
            e.getClass().getName(),
            e.getMessage(),
            getStackTrace(e),
            "my-service",
            "log-123"
        );
    }
}
```

---

## 服务拓扑

### 获取服务依赖图

```bash
# 获取最近1小时的服务拓扑
curl http://localhost:8080/api/observability/topology

# 指定时间范围
curl "http://localhost:8080/api/observability/topology?startTime=2025-10-24T00:00:00&endTime=2025-10-24T23:59:59"
```

### 响应示例

```json
{
  "nodes": [
    {
      "name": "user-service",
      "callCount": 10000,
      "errorRate": 0.1,
      "avgDuration": 150.5,
      "p95Duration": 300.0,
      "healthScore": 95
    }
  ],
  "edges": [
    {
      "source": "api-gateway",
      "target": "user-service",
      "callCount": 10000,
      "qps": 166.67
    }
  ]
}
```

---

## 线程分析

### 获取所有线程

```bash
curl http://localhost:8080/api/observability/threads
```

### 查看Top CPU线程

```bash
curl http://localhost:8080/api/observability/threads/top-cpu?limit=10
```

### 死锁检测

```bash
curl http://localhost:8080/api/observability/threads/deadlocks

# 响应（如果检测到死锁）
{
  "code": 500,
  "message": "检测到 1 个死锁",
  "data": [
    {
      "threads": [...],
      "description": "检测到 2 个线程互相死锁",
      "severity": "CRITICAL"
    }
  ]
}
```

### 线程堆栈快照

```bash
# 获取完整的线程转储
curl http://localhost:8080/api/observability/threads/dump > thread-dump.json
```

---

## Arthas调试

### 启动Arthas

```bash
curl -X POST http://localhost:8080/api/observability/arthas/start?port=3658

# 响应
{
  "status": "success",
  "pid": "12345",
  "telnetPort": 3658,
  "httpPort": 8563,
  "url": "http://localhost:8563"
}
```

### 常用命令

```bash
# 查看线程
curl "http://localhost:8080/api/observability/arthas/thread?threadId=1"

# Dashboard
curl http://localhost:8080/api/observability/arthas/dashboard

# 反编译类
curl "http://localhost:8080/api/observability/arthas/jad?className=com.basebackend.User"

# 监控方法
curl -X POST http://localhost:8080/api/observability/arthas/watch \
  -d "className=com.basebackend.UserService" \
  -d "methodName=getUser" \
  -d "express={params, returnObj}"

# 追踪方法调用
curl -X POST http://localhost:8080/api/observability/arthas/trace \
  -d "className=com.basebackend.UserService" \
  -d "methodName=getUser"
```

### 停止Arthas

```bash
curl -X POST http://localhost:8080/api/observability/arthas/stop
```

---

## 告警配置

### 初始化默认规则

```bash
curl -X POST http://localhost:8080/api/observability/alerts/rules/init-defaults
```

### 查看所有规则

```bash
curl http://localhost:8080/api/observability/alerts/rules
```

### 添加自定义规则

```bash
curl -X POST http://localhost:8080/api/observability/alerts/rules \
  -H "Content-Type: application/json" \
  -d '{
    "id": "custom-memory-alert",
    "name": "自定义内存告警",
    "metric": "heap.usage.percent",
    "operator": ">",
    "threshold": 85,
    "severity": "MEDIUM",
    "enabled": true
  }'
```

### Java代码示例

```java
@Service
public class MyMonitorService {
    
    @Autowired
    private AlertRuleService alertRuleService;
    
    @Autowired
    private AlertNotificationService notificationService;
    
    public void checkCustomMetric() {
        double metricValue = getCustomMetricValue();
        
        // 评估指标
        List<AlertEvent> events = alertRuleService.evaluateMetric(
            "custom.metric", 
            metricValue, 
            Map.of("source", "my-service")
        );
        
        // 发送告警
        notificationService.sendAlerts(events);
    }
}
```

---

## 统计分析

### 系统健康总览

```bash
curl http://localhost:8080/api/observability/statistics/health-overview

# 响应
{
  "jvm": {
    "heapUsagePercent": 65.5,
    "status": "HEALTHY"
  },
  "sql": {
    "slowSqlCount": 15
  },
  "healthScore": 85,
  "healthStatus": "HEALTHY"
}
```

### 性能趋势

```bash
# 获取24小时性能趋势
curl http://localhost:8080/api/observability/statistics/performance-trend?hours=24
```

### 资源排行

```bash
# 获取资源使用排行
curl http://localhost:8080/api/observability/statistics/resource-ranking
```

### GC分析

```bash
# GC统计
curl http://localhost:8080/api/observability/gc/statistics

# GC趋势和建议
curl http://localhost:8080/api/observability/gc/trend
```

---

## 系统维护

### 数据清理

```bash
# 手动清理7天前的数据
curl -X POST "http://localhost:8080/api/observability/maintenance/cleanup?retentionDays=7"

# 响应
{
  "success": true,
  "message": "清理完成",
  "jvmMetricsDeleted": 1000,
  "slowSqlDeleted": 500
}
```

### 存储统计

```bash
curl http://localhost:8080/api/observability/maintenance/storage

# 响应
{
  "jvmMetricsCount": 10000,
  "slowSqlCount": 5000,
  "estimatedSizeMB": 50.5
}
```

### 健康检查

```bash
# Spring Boot Actuator健康检查
curl http://localhost:8080/actuator/health

# 可观测性模块健康检查
curl http://localhost:8080/actuator/health/observability
```

---

## 最佳实践

### 1. 告警配置

```yaml
# 根据业务调整阈值
observability:
  alerting:
    rules:
      heap-usage:
        threshold: 85  # 调整到合适的值
      cpu-usage:
        threshold: 75
```

### 2. 数据保留策略

```yaml
# 定时清理配置
observability:
  cleanup:
    jvm-metrics-retention-days: 7
    slow-sql-retention-days: 30
```

### 3. 性能优化

```java
// 批量操作
@Service
public class BatchMonitorService {
    
    @Scheduled(fixedRate = 60000)
    public void batchProcess() {
        // 每分钟批量处理一次，而不是每次都处理
    }
}
```

### 4. 监控指标导出

```yaml
# 集成Prometheus
management:
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 常见问题

### Q1: JVM指标不更新？

**A**: 检查是否启用了监控：
```yaml
observability:
  profiling:
    enabled: true
```

### Q2: 慢SQL没有记录？

**A**: 
1. 检查阈值配置是否太高
2. 确认Mapper方法是否被AOP拦截

### Q3: Arthas启动失败？

**A**: 
1. 检查端口是否被占用
2. 确认有足够的权限
3. 查看日志获取详细错误信息

### Q4: 告警太频繁？

**A**: 
1. 调整阈值
2. 冷却机制默认10分钟，可以延长
3. 关闭不需要的规则

---

## 更多资源

- [API参考文档](OBSERVABILITY-API-REFERENCE.md)
- [实施指南](OBSERVABILITY-REFACTOR-GUIDE.md)
- [完整报告](OBSERVABILITY-PHASE3-COMPLETE.md)
