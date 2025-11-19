# 追踪查询服务使用指南

## 概述

TraceQueryService集成了Zipkin/Tempo API，提供分布式追踪查询功能。

## 配置

### application.yml

```yaml
observability:
  trace:
    endpoint: http://192.168.66.126:3200  # Tempo endpoint
    format: tempo  # zipkin or tempo
    enabled: true
```

或使用Zipkin：

```yaml
observability:
  trace:
    endpoint: http://192.168.66.126:9411  # Zipkin endpoint
    format: zipkin
    enabled: true
```

### 环境变量

```bash
# Tempo
export TRACE_ENDPOINT=http://192.168.66.126:3200
export TRACE_FORMAT=tempo

# Zipkin
export TRACE_ENDPOINT=http://192.168.66.126:9411
export TRACE_FORMAT=zipkin
```

## API接口

### 1. 根据TraceID查询

**请求：**
```bash
curl -X GET http://localhost:8087/api/traces/{traceId} \
  -H "Authorization: Bearer <token>"
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "traceId": "abc123",
    "serviceName": "basebackend-user-api",
    "operationName": "GET /api/users",
    "startTime": 1700000000000,
    "duration": 150,
    "spanCount": 5,
    "spans": [
      {
        "spanId": "span1",
        "traceId": "abc123",
        "parentSpanId": null,
        "operationName": "GET /api/users",
        "startTime": 1700000000000,
        "duration": 150,
        "kind": "SERVER",
        "serviceName": "basebackend-user-api",
        "tags": {
          "http.method": "GET",
          "http.url": "/api/users",
          "http.status_code": "200"
        }
      }
    ]
  }
}
```

### 2. 搜索追踪

**请求：**
```bash
curl -X POST http://localhost:8087/api/traces/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "serviceName": "basebackend-user-api",
    "operationName": "GET /api/users",
    "minDuration": 100,
    "maxDuration": 5000,
    "limit": 50
  }'
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "traces": [
      {
        "traceId": "abc123",
        "serviceName": "basebackend-user-api",
        "operationName": "GET /api/users",
        "startTime": 1700000000000,
        "duration": 150,
        "spanCount": 5
      }
    ],
    "total": 1,
    "limit": 50
  }
}
```

### 3. 获取服务列表

**请求：**
```bash
curl -X GET http://localhost:8087/api/traces/services \
  -H "Authorization: Bearer <token>"
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    "basebackend-user-api",
    "basebackend-system-api",
    "basebackend-auth-api",
    "basebackend-gateway"
  ]
}
```

### 4. 获取追踪统计

**请求：**
```bash
curl -X GET "http://localhost:8087/api/traces/stats?serviceName=basebackend-user-api&hours=1" \
  -H "Authorization: Bearer <token>"
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "serviceName": "basebackend-user-api",
    "hours": 1,
    "totalTraces": 1250,
    "errorTraces": 15,
    "errorRate": 0.012,
    "avgDuration": 125,
    "maxDuration": 3500,
    "minDuration": 10,
    "startTime": 1700000000000,
    "endTime": 1700003600000
  }
}
```

## Zipkin API集成

### 支持的Zipkin API

1. **GET /api/v2/trace/{traceId}** - 获取单个trace
2. **GET /api/v2/traces** - 搜索traces
3. **GET /api/v2/services** - 获取服务列表
4. **GET /api/v2/spans** - 获取span名称列表

### 查询参数映射

| TraceQueryRequest | Zipkin API参数 | 说明 |
|-------------------|----------------|------|
| serviceName | serviceName | 服务名称 |
| operationName | spanName | 操作名称 |
| minDuration | minDuration | 最小持续时间（微秒） |
| maxDuration | maxDuration | 最大持续时间（微秒） |
| endTime | endTs | 结束时间戳 |
| startTime/endTime | lookback | 查询时间范围 |
| limit | limit | 结果数量限制 |

## Tempo集成

### 方式1：使用Tempo原生API（推荐）

```yaml
observability:
  trace:
    endpoint: http://tempo:3200
    format: tempo
```

Tempo API endpoint：
- 查询trace：`GET /api/traces/{traceId}`
- 返回OpenTelemetry格式数据

### 方式2：使用Zipkin兼容API

```yaml
observability:
  trace:
    endpoint: http://tempo:3200  # Tempo的Zipkin兼容端点
    format: zipkin
```

需要在Tempo配置中启用Zipkin接收器：

```yaml
# tempo.yaml
distributor:
  receivers:
    zipkin:
      endpoint: 0.0.0.0:9411
```

## 数据格式对比

### Tempo/OpenTelemetry格式

```json
{
  "trace": {
    "resourceSpans": [{
      "resource": {
        "attributes": [{
          "key": "service.name",
          "value": {"stringValue": "basebackend-user-api"}
        }]
      },
      "scopeSpans": [{
        "spans": [{
          "traceId": "aR2CoYFXDZdc18dVHqWoQQ==",  // Base64编码
          "spanId": "wXxv1CaQvT4=",
          "parentSpanId": "LeaS+fh01A8=",
          "name": "http post /api/user/auth/login",
          "kind": "SPAN_KIND_SERVER",
          "startTimeUnixNano": "1763541665970193000",
          "endTimeUnixNano": "1763541666252545000",
          "attributes": [...],
          "events": [...]
        }]
      }]
    }]
  }
}
```

### Zipkin格式

```json
[{
  "traceId": "691d82a181570d97",
  "id": "c17c6fd42690bd3e",
  "parentId": "2de692f9f874d40f",
  "name": "http post /api/user/auth/login",
  "kind": "SERVER",
  "timestamp": 1763541665970193,
  "duration": 282352,
  "localEndpoint": {
    "serviceName": "basebackend-user-api"
  },
  "tags": {...},
  "annotations": [...]
}]
```

### 统一输出格式

无论输入是Tempo还是Zipkin格式，都会转换为统一格式：

```json
{
  "traceId": "691d82a181570d97",
  "serviceName": "basebackend-user-api",
  "operationName": "http post /api/user/auth/login",
  "startTime": 1763541665970,
  "duration": 282,
  "spanCount": 5,
  "status": "success",
  "spans": [{
    "spanId": "c17c6fd42690bd3e",
    "traceId": "691d82a181570d97",
    "parentSpanId": "2de692f9f874d40f",
    "operationName": "http post /api/user/auth/login",
    "serviceName": "basebackend-user-api",
    "startTime": 1763541665970,
    "duration": 282,
    "kind": "SERVER",
    "tags": {...},
    "logs": [...]
  }]
}
```

## 数据格式

### Span结构

```json
{
  "spanId": "unique-span-id",
  "traceId": "unique-trace-id",
  "parentSpanId": "parent-span-id",
  "operationName": "GET /api/users",
  "startTime": 1700000000000,
  "duration": 150,
  "kind": "SERVER",
  "serviceName": "basebackend-user-api",
  "tags": {
    "http.method": "GET",
    "http.url": "/api/users",
    "http.status_code": "200",
    "error": "false"
  },
  "logs": [
    {
      "timestamp": 1700000000100,
      "value": "Processing request"
    }
  ]
}
```

### Span Kind类型

- `CLIENT` - 客户端span
- `SERVER` - 服务端span
- `PRODUCER` - 消息生产者
- `CONSUMER` - 消息消费者

## 错误处理

### 连接失败

如果无法连接到Zipkin/Tempo：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "traceId": "abc123",
    "serviceName": "unknown",
    "operationName": "unknown",
    "spans": []
  }
}
```

服务会返回空数据而不是抛出异常，确保系统稳定性。

### 日志输出

```
ERROR c.b.o.s.i.TraceQueryServiceImpl - Error getting trace by id: abc123
java.net.ConnectException: Connection refused
```

## 性能优化

### 1. 限制查询结果

```java
request.setLimit(100);  // 默认100条
```

### 2. 缩小时间范围

```java
request.setStartTime(System.currentTimeMillis() - 3600000);  // 最近1小时
request.setEndTime(System.currentTimeMillis());
```

### 3. 使用持续时间过滤

```java
request.setMinDuration(100L);   // 只查询>100ms的trace
request.setMaxDuration(5000L);  // 只查询<5s的trace
```

## 监控和调试

### 启用DEBUG日志

```yaml
logging:
  level:
    com.basebackend.observability.service.impl.TraceQueryServiceImpl: DEBUG
```

### 查看请求日志

```
DEBUG c.b.o.s.i.TraceQueryServiceImpl - Querying traces: http://192.168.66.126:9411/api/v2/traces?serviceName=basebackend-user-api&limit=100
```

## 常见问题

### Q1: 为什么查询不到数据？

**检查清单：**
1. Zipkin/Tempo是否运行
2. endpoint配置是否正确
3. 服务是否已发送trace数据
4. 时间范围是否合理

### Q2: 如何查看原始Zipkin数据？

直接访问Zipkin UI：
```
http://192.168.66.126:9411/zipkin/
```

### Q3: 支持哪些追踪系统？

- Zipkin
- Tempo（Zipkin兼容模式）
- Jaeger（通过Zipkin兼容API）

### Q4: 如何添加自定义标签过滤？

目前Zipkin API不直接支持标签过滤，需要：
1. 获取所有traces
2. 在应用层过滤

或使用Jaeger的原生API。

## 示例场景

### 场景1：查找慢请求

```bash
curl -X POST http://localhost:8087/api/traces/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "serviceName": "basebackend-user-api",
    "minDuration": 1000,
    "limit": 20
  }'
```

### 场景2：查找错误请求

1. 先搜索traces
2. 检查span的tags中是否有`error: true`

### 场景3：分析服务依赖

1. 获取trace详情
2. 分析spans的parent-child关系
3. 构建服务调用链

## 扩展开发

### 添加新的查询方法

```java
@Override
public List<String> getOperations(String serviceName) {
    String url = traceEndpoint + "/api/v2/spans?serviceName=" + serviceName;
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    // 解析并返回
}
```

### 自定义数据转换

```java
private Map<String, Object> convertZipkinSpan(Map<String, Object> zipkinSpan) {
    // 自定义转换逻辑
}
```

## 参考资料

- [Zipkin API文档](https://zipkin.io/zipkin-api/)
- [Tempo文档](https://grafana.com/docs/tempo/latest/)
- [OpenTelemetry规范](https://opentelemetry.io/docs/specs/otel/)

## 更新日志

- 2024-11-19: 实现Zipkin/Tempo API集成
- 2024-11-19: 添加trace查询、搜索、统计功能
