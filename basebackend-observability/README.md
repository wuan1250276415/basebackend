# Observability Module - 可观测性模块

## 简介

可观测性模块提供完整的调试与监控工具平台，包括分布式追踪、日志分析、性能分析和实时调试功能。

## 功能特性

### ✅ 已实现功能

#### 1. 分布式追踪
- ✅ 调用链可视化（树形结构）
- ✅ 性能瓶颈自动检测（4种规则）
- ✅ 关键路径计算
- ✅ 慢请求记录
- 📋 异常链路高亮（待实现）
- 📋 服务依赖拓扑（待实现）

#### 2. 性能分析
- ✅ JVM实时监控（堆/线程/GC/CPU）
- ✅ SQL性能监控（AOP拦截）
- ✅ 慢SQL自动记录和聚合
- ✅ 自动告警（内存/CPU/线程）
- 📋 堆转储分析（待实现）
- 📋 火焰图生成（待实现）

#### 3. 数据持久化
- ✅ 13个核心数据表
- ✅ MyBatis Plus 集成
- ✅ Flyway 数据库迁移

#### 4. REST API
- ✅ 追踪查询API（3个端点）
- ✅ 性能分析API（4个端点）
- ✅ Swagger 文档

### 📋 规划中功能

- [ ] Elasticsearch 日志全文检索
- [ ] 实时日志流（WebSocket）
- [ ] Arthas 调试工具集成
- [ ] 前端可视化界面
- [ ] 告警规则引擎

## 技术栈

- **追踪**: OpenTelemetry 1.32.0 + Jaeger
- **日志**: Loki + Elasticsearch 8.11
- **性能**: JMX + Async-profiler 2.9
- **调试**: Arthas 3.7.1
- **持久化**: MyBatis Plus + MySQL
- **框架**: Spring Boot 3.1.5

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-observability</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 启用配置

```yaml
spring:
  profiles:
    include: observability

observability:
  enabled: true
  profiling:
    enabled: true
  tracing:
    enabled: true
```

### 3. 启动应用

模块会自动：
- ✅ 执行数据库迁移（创建13个表）
- ✅ 启动JVM指标采集（每10秒）
- ✅ 注册SQL性能监控（AOP）
- ✅ 暴露REST API

### 4. 查看监控数据

```bash
# JVM指标
curl http://localhost:8080/api/observability/profiling/jvm/metrics

# 慢SQL
curl http://localhost:8080/api/observability/profiling/sql/slow?hours=1

# 调用链
curl http://localhost:8080/api/observability/traces/{traceId}/graph
```

## API 文档

### 追踪API

| 端点 | 方法 | 描述 |
|------|------|------|
| /traces/{traceId}/graph | GET | 获取调用链可视化图 |
| /traces/{traceId}/bottlenecks | GET | 检测性能瓶颈 |
| /traces/{traceId}/spans | GET | 获取Span列表 |
| /traces/errors | GET | 获取错误追踪 |

### 性能分析API

| 端点 | 方法 | 描述 |
|------|------|------|
| /profiling/jvm/metrics | GET | 获取最新JVM指标 |
| /profiling/jvm/history | GET | 获取JVM历史数据 |
| /profiling/sql/slow | GET | 获取慢SQL列表 |
| /profiling/sql/top | GET | Top N慢SQL |
| /profiling/sql/aggregate | GET | 慢SQL聚合统计 |

## 性能瓶颈检测

### 检测规则

#### 1. 慢Span检测
单个操作耗时 > 总时长30%

```json
{
  "type": "SLOW_SPAN",
  "description": "GET /users 耗时 800ms，占比 53.3%",
  "suggestion": "优化该操作的执行效率"
}
```

#### 2. N+1查询检测
相同SQL查询 > 10次

```json
{
  "type": "N_PLUS_ONE_QUERY",
  "description": "检测到N+1查询问题：相同查询执行了15次",
  "queryCount": 15,
  "suggestion": "使用JOIN或批量查询替代循环查询"
}
```

#### 3. 串行调用检测
多次串行调用同一服务

```json
{
  "type": "SERIAL_CALLS",
  "description": "串行调用 order-service 服务 8 次",
  "suggestion": "考虑使用批量接口或并行调用"
}
```

#### 4. 外部服务超时
HTTP调用 > 3秒

```json
{
  "type": "EXTERNAL_SERVICE_TIMEOUT",
  "description": "外部服务调用超时：POST /api/payment 耗时 3500ms",
  "suggestion": "检查网络连接或使用降级策略"
}
```

## JVM监控

### 监控指标

- **堆内存**: used/max/committed
- **非堆内存**: metaspace等
- **线程**: 总数/守护/峰值
- **GC**: 次数/耗时
- **CPU**: 使用率
- **负载**: 系统负载

### 自动告警

- 堆内存使用 > 90%
- 线程数 > 1000
- CPU使用率 > 80%

### 使用示例

```bash
# 查看实时指标
curl http://localhost:8080/api/observability/profiling/jvm/metrics

# 返回
{
  "metrics": {
    "instanceId": "app-server-12345",
    "heapUsed": 536870912,
    "heapMax": 1073741824,
    "threadCount": 120,
    "gcCount": 15,
    "cpuUsage": 35.6
  },
  "heapUsagePercent": 50.0
}

# 查看历史数据
curl http://localhost:8080/api/observability/profiling/jvm/history?hours=1
```

## SQL性能监控

### 工作原理

AOP拦截所有 `*.mapper.*` 方法：
- 记录执行时间
- 关联TraceId
- 慢SQL自动保存（>1秒）
- 严重慢SQL告警（>5秒）

### 使用示例

```bash
# 查看慢SQL
curl http://localhost:8080/api/observability/profiling/sql/slow?hours=1

# 返回
[
  {
    "methodName": "SysUserMapper.selectById",
    "duration": 1250,
    "timestamp": "2025-10-24T15:30:00",
    "traceId": "abc123"
  }
]

# Top 10 慢SQL
curl http://localhost:8080/api/observability/profiling/sql/top?topN=10
```

## 数据库表

### 核心表结构

```sql
-- 追踪Span
trace_span_ext (追踪数据)
slow_trace_record (慢请求)
service_dependency (服务依赖)
trace_service_stats (统计汇总)

-- 日志分析
log_statistics (日志统计)
exception_aggregation (异常聚合)

-- 性能分析
jvm_metrics (JVM指标)
slow_sql_record (慢SQL)
profiling_session (性能剖析)

-- 调试工具
breakpoint_config (断点配置)
hot_deploy_history (热部署)

-- 告警系统
alert_rule_config (规则配置)
alert_history (告警历史)
```

## 配置参考

### 完整配置

```yaml
observability:
  enabled: true
  
  tracing:
    enabled: true
    exporter: jaeger
    jaeger:
      endpoint: http://localhost:14250
    sampling-rate: 0.1
    
  profiling:
    enabled: true
    jvm-metrics:
      collect-interval: 10000
    sql:
      slow-threshold: 1000
      
  alerting:
    enabled: true
    rules:
      heap-usage:
        threshold: 90
      thread-count:
        threshold: 1000
      cpu-usage:
        threshold: 80
```

## 扩展开发

### 添加新的瓶颈检测规则

```java
@Service
public class CustomBottleneckDetector {
    
    public void detectCacheIssue(TraceGraph graph, List<Bottleneck> bottlenecks) {
        // 自定义检测逻辑
    }
}
```

### 添加自定义告警

```java
@Component
public class CustomAlertRule {
    
    @Scheduled(fixedRate = 60000)
    public void checkCustomMetric() {
        // 自定义指标检查
    }
}
```

## 故障排查

### 问题1：JVM指标未采集
检查配置：
```yaml
observability:
  profiling:
    enabled: true  # 必须为true
```

### 问题2：SQL监控不工作
检查：
1. 是否有Mapper方法调用
2. AOP是否生效
3. 数据库表是否创建

### 问题3：数据库表未创建
手动执行迁移：
```bash
mvn flyway:migrate
```

## 性能影响

| 功能 | 性能开销 | 说明 |
|-----|---------|------|
| JVM采集 | <1% | 每10秒采集一次 |
| SQL监控 | <5% | AOP拦截 |
| 追踪 | <3% | 10%采样率 |
| 总计 | <10% | 可接受范围 |

## 许可证

Copyright © 2025 BaseBackend

## 更多文档

- [重构指南](../OBSERVABILITY-REFACTOR-GUIDE.md)
- [方案总结](../OBSERVABILITY-REFACTOR-SUMMARY.md)
- [实施完成报告](../OBSERVABILITY-IMPLEMENTATION-COMPLETE.md)
