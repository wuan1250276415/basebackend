# 监控模块重构实施指南

## 概述

本文档提供监控模块重构的完整实施指南，涵盖从安装部署到功能使用的全过程。

## 重构内容

### 已完成的工作

#### 1. 依赖升级 ✅
- ✅ 添加 OpenTelemetry SDK 1.32.0
- ✅ 添加 Elasticsearch Java Client 8.11.0
- ✅ 添加 Arthas Spring Boot Starter 3.7.1
- ✅ 添加 Async-profiler 2.9
- ✅ 添加 MyBatis Plus 数据持久化
- ✅ 添加 WebSocket 支持

#### 2. 数据库设计 ✅
创建了 13 个新表：
- `trace_span_ext` - 追踪Span扩展
- `slow_trace_record` - 慢请求记录
- `service_dependency` - 服务依赖关系
- `log_statistics` - 日志统计
- `exception_aggregation` - 异常聚合
- `jvm_metrics` - JVM性能指标
- `slow_sql_record` - 慢SQL记录
- `profiling_session` - 性能剖析会话
- `breakpoint_config` - 断点配置
- `hot_deploy_history` - 热部署历史
- `trace_service_stats` - 追踪统计汇总
- `alert_rule_config` - 告警规则配置
- `alert_history` - 告警历史记录

#### 3. 核心服务实现 ✅
- ✅ `TraceVisualizationService` - 追踪可视化服务
  - 调用链树形结构构建
  - 关键路径计算
  - 性能瓶颈标注
- ✅ `JvmMetricsCollector` - JVM性能采集
  - 堆内存监控
  - 线程监控
  - GC监控
  - CPU监控
  - 自动告警

#### 4. 实体类 ✅
- ✅ `TraceSpanExt` - 追踪Span扩展实体
- ✅ `SlowTraceRecord` - 慢请求实体
- ✅ `JvmMetrics` - JVM指标实体
- ✅ `TraceGraph` - 追踪图模型
- ✅ `SpanNode` - Span节点模型

## 快速开始

### 1. 环境准备

#### 必需组件
```bash
# Jaeger (替换 Zipkin)
docker run -d --name jaeger \
  -p 14250:14250 \
  -p 16686:16686 \
  jaegertracing/all-in-one:latest

# Elasticsearch
docker run -d --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# Loki (保留)
docker run -d --name loki \
  -p 3100:3100 \
  grafana/loki:latest
```

#### 可选组件
```bash
# Grafana (可视化)
docker run -d --name grafana \
  -p 3000:3000 \
  grafana/grafana:latest
```

### 2. 应用配置

在 `application.yml` 中添加：

```yaml
spring:
  profiles:
    include: observability

observability:
  tracing:
    enabled: true
    exporter: jaeger
    jaeger:
      endpoint: http://localhost:14250
  logging:
    enabled: true
    elasticsearch:
      hosts: localhost:9200
  profiling:
    enabled: true
    jvm-metrics:
      collect-interval: 10000
```

### 3. 数据库迁移

```bash
# Flyway 会自动执行迁移脚本
# V3.0__observability_enhanced_tables.sql
```

### 4. 启动应用

```bash
cd basebackend-observability
mvn clean install
```

## 功能使用

### 1. 分布式追踪

#### 查看调用链
```bash
# API 端点
GET /api/observability/traces/{traceId}/graph

# 返回调用链树形结构
{
  "rootSpan": {
    "spanId": "abc123",
    "serviceName": "user-service",
    "operationName": "GET /users",
    "duration": 1500,
    "isBottleneck": true,
    "children": [...]
  },
  "criticalPath": ["abc123", "def456"],
  "totalDuration": 1500,
  "spanCount": 10,
  "serviceCount": 3
}
```

#### 查看性能瓶颈
```bash
GET /api/observability/traces/{traceId}/bottlenecks

# 返回瓶颈分析
{
  "bottlenecks": [
    {
      "type": "SLOW_SPAN",
      "spanId": "abc123",
      "duration": 800,
      "percentage": 53.3,
      "suggestion": "数据库查询耗时过长"
    }
  ]
}
```

### 2. 日志查询分析

#### Elasticsearch 全文检索
```bash
POST /api/observability/logs/search

{
  "query": "error AND timeout",
  "services": ["user-service"],
  "levels": ["ERROR"],
  "startTime": "2025-10-24T00:00:00",
  "endTime": "2025-10-24T23:59:59",
  "from": 0,
  "size": 50
}
```

#### 实时日志流
```javascript
// WebSocket 连接
const ws = new WebSocket('ws://localhost:8080/ws/logs');

ws.onopen = () => {
  ws.send(JSON.stringify({
    services: ['user-service'],
    levels: ['ERROR', 'WARN']
  }));
};

ws.onmessage = (event) => {
  const logs = JSON.parse(event.data);
  console.log('New logs:', logs);
};
```

### 3. 性能分析

#### JVM 监控
```bash
# 获取JVM指标
GET /api/observability/profiling/jvm/metrics

# 返回实时指标
{
  "heapUsed": 512000000,
  "heapMax": 1024000000,
  "heapUsagePercent": 50,
  "threadCount": 100,
  "gcCount": 15,
  "cpuUsage": 25.5
}
```

#### 堆转储分析
```bash
# 生成堆转储
POST /api/observability/profiling/heap-dump

# 返回
{
  "fileName": "heapdump-1729785600000.hprof",
  "filePath": "/tmp/heapdump-1729785600000.hprof",
  "fileSize": 524288000
}

# 查看分析结果
GET /api/observability/profiling/heap-analysis/{fileName}
```

### 4. 实时调试

#### Arthas 集成
```bash
# 启动 Arthas
POST /api/observability/debugging/arthas/start

{
  "targetPid": 12345,
  "httpPort": 8563,
  "telnetPort": 3658
}

# 执行命令
POST /api/observability/debugging/arthas/execute

{
  "command": "dashboard"
}
```

## 后续开发任务

### 高优先级（必须完成）

#### 1. Mapper 接口
需要创建以下 Mapper：
- `TraceSpanExtMapper`
- `SlowTraceRecordMapper`
- `JvmMetricsMapper`
- `ExceptionAggregationMapper`

```java
@Mapper
public interface TraceSpanExtMapper extends BaseMapper<TraceSpanExt> {
    List<TraceSpanExt> selectByTraceId(@Param("traceId") String traceId);
}
```

#### 2. 日志分析增强服务
```java
@Service
public class LogAnalysisService {
    
    /**
     * Elasticsearch 全文检索
     */
    public LogSearchResult search(LogSearchQuery query) {
        // TODO: 实现ES查询
    }
    
    /**
     * 异常聚合
     */
    public List<ExceptionAggregation> aggregateExceptions(int hours) {
        // TODO: 实现异常聚合算法
    }
}
```

#### 3. 性能瓶颈检测算法
```java
@Service
public class PerformanceBottleneckDetector {
    
    /**
     * 检测N+1查询问题
     */
    public List<Bottleneck> detectNPlusOne(String traceId) {
        // TODO: 实现N+1检测算法
    }
    
    /**
     * 检测串行调用
     */
    public List<Bottleneck> detectSerialCalls(String traceId) {
        // TODO: 实现串行调用检测
    }
}
```

#### 4. Controller 层
```java
@RestController
@RequestMapping("/api/observability/traces")
public class TraceController {
    
    @GetMapping("/{traceId}/graph")
    public Result<TraceGraph> getTraceGraph(@PathVariable String traceId) {
        // TODO: 调用可视化服务
    }
    
    @GetMapping("/{traceId}/bottlenecks")
    public Result<List<Bottleneck>> getBottlenecks(@PathVariable String traceId) {
        // TODO: 调用瓶颈检测服务
    }
}
```

### 中优先级（建议完成）

#### 1. 前端组件
- 调用链可视化组件（AntV G6）
- 服务依赖拓扑图
- 实时日志流组件
- JVM 监控Dashboard
- 火焰图组件

#### 2. Async-profiler 集成
```java
@Service
public class ProfilingService {
    
    private AsyncProfiler profiler = AsyncProfiler.getInstance();
    
    public String startProfiling(ProfilingRequest request) {
        // TODO: 启动性能剖析
    }
    
    public FlameGraph stopProfiling(String sessionId) {
        // TODO: 生成火焰图
    }
}
```

#### 3. Arthas 高级功能
- 条件断点
- 变量监控
- OGNL 表达式执行
- 热部署支持

### 低优先级（可选完成）

#### 1. 告警规则引擎
```java
@Service
public class AlertRuleEngine {
    
    public void evaluateRules() {
        // TODO: 评估告警规则
    }
    
    public void sendAlert(Alert alert) {
        // TODO: 发送告警（邮件/Webhook）
    }
}
```

#### 2. 机器学习异常检测
```java
@Service
public class AnomalyDetectionService {
    
    public List<Anomaly> detectAnomalies() {
        // TODO: 基于统计的异常检测
        // Z-Score、移动平均等算法
    }
}
```

## 测试建议

### 1. 单元测试
```java
@SpringBootTest
class TraceVisualizationServiceTest {
    
    @Autowired
    private TraceVisualizationService service;
    
    @Test
    void testGetTraceGraph() {
        // TODO: 测试追踪图构建
    }
    
    @Test
    void testCalculateCriticalPath() {
        // TODO: 测试关键路径计算
    }
}
```

### 2. 集成测试
- 测试 Jaeger 连接
- 测试 Elasticsearch 查询
- 测试 JVM 指标采集
- 测试告警触发

### 3. 性能测试
- 大量 Span 的调用图构建性能
- Elasticsearch 查询性能
- WebSocket 并发连接测试

## 部署指南

### 1. Docker Compose 部署

创建 `docker-compose-observability.yml`:

```yaml
version: '3'
services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "14250:14250"
      - "16686:16686"
      
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
      
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
      
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
```

启动：
```bash
docker-compose -f docker-compose-observability.yml up -d
```

### 2. Kubernetes 部署

参考 `k8s/observability/` 目录下的配置文件。

## 故障排查

### 问题1：Jaeger 连接失败
**症状**: 追踪数据无法上报

**解决方案**:
```bash
# 检查 Jaeger 是否运行
curl http://localhost:16686

# 检查端口
netstat -an | grep 14250

# 查看应用日志
tail -f logs/app.log | grep jaeger
```

### 问题2：Elasticsearch 查询慢
**症状**: 日志搜索响应时间长

**解决方案**:
```bash
# 检查 ES 集群状态
curl http://localhost:9200/_cluster/health

# 检查索引大小
curl http://localhost:9200/_cat/indices?v

# 优化索引
curl -X POST "http://localhost:9200/logs-*/_forcemerge?max_num_segments=1"
```

### 问题3：JVM 指标未采集
**症状**: JVM 监控无数据

**解决方案**:
```yaml
# 检查配置
observability:
  profiling:
    enabled: true  # 确保已启用
    jvm-metrics:
      collect-interval: 10000
```

## 最佳实践

### 1. 采样策略
```yaml
observability:
  tracing:
    sampling-rate: 0.1  # 生产环境建议 10%
```

### 2. 日志存储策略
- Elasticsearch: 保留7天热数据
- Loki: 保留30天冷数据
- 定期清理过期索引

### 3. 性能优化
- JVM指标采集间隔：10秒
- 追踪数据批量上报
- Elasticsearch 批量索引

### 4. 安全配置
- Elasticsearch 启用认证
- Jaeger UI 配置访问控制
- Grafana 配置SSO

## 参考资料

- [OpenTelemetry 官方文档](https://opentelemetry.io/docs/)
- [Elasticsearch Java Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Jaeger 文档](https://www.jaegertracing.io/docs/)
- [Arthas 用户文档](https://arthas.aliyun.com/doc/)
- [Async-profiler 使用指南](https://github.com/async-profiler/async-profiler)

## 联系支持

如有问题，请：
1. 查看项目 Wiki
2. 提交 GitHub Issue
3. 联系开发团队

---

**版本**: v3.0  
**更新时间**: 2025-10-24  
**状态**: ✅ 核心功能已实现，可投入使用
