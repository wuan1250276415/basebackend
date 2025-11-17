# 监控模块 API 参考文档

## 目录

1. [追踪API](#追踪api)
2. [性能分析API](#性能分析api)
3. [日志分析API](#日志分析api)
4. [服务拓扑API](#服务拓扑api)
5. [线程分析API](#线程分析api)
6. [WebSocket](#websocket)

---

## 追踪API

### 1. 获取调用链可视化图

**端点**: `GET /api/observability/traces/{traceId}/graph`

**描述**: 获取指定TraceId的调用链树形结构，包含关键路径和瓶颈标注。

**路径参数**:
- `traceId` - 追踪ID

**响应示例**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "rootSpan": {
      "spanId": "abc123",
      "serviceName": "user-service",
      "operationName": "GET /users",
      "duration": 1500,
      "startTime": 1729756800000,
      "isBottleneck": true,
      "children": [...]
    },
    "criticalPath": ["abc123", "def456", "ghi789"],
    "totalDuration": 1500,
    "spanCount": 15,
    "serviceCount": 3
  }
}
```

### 2. 检测性能瓶颈

**端点**: `GET /api/observability/traces/{traceId}/bottlenecks`

**描述**: 使用4种智能算法检测调用链中的性能瓶颈。

**路径参数**:
- `traceId` - 追踪ID

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "type": "N_PLUS_ONE_QUERY",
      "severity": "HIGH",
      "description": "检测到N+1查询问题：相同查询执行了15次",
      "queryCount": 15,
      "totalDuration": 750,
      "sqlQuery": "SELECT * FROM users WHERE id = ?",
      "suggestion": "使用JOIN或批量查询替代循环查询"
    },
    {
      "type": "SLOW_SPAN",
      "severity": "MEDIUM",
      "description": "database.query 耗时 800ms，占比 53.3%",
      "location": "user-service.getUserById",
      "suggestion": "优化该操作的执行效率"
    }
  ]
}
```

**瓶颈类型**:
- `SLOW_SPAN` - 单个Span耗时过长
- `N_PLUS_ONE_QUERY` - N+1查询问题
- `SERIAL_CALLS` - 串行调用
- `EXTERNAL_SERVICE_TIMEOUT` - 外部服务超时

### 3. 获取Span列表

**端点**: `GET /api/observability/traces/{traceId}/spans`

**描述**: 获取指定TraceId的所有Span列表。

### 4. 获取错误追踪

**端点**: `GET /api/observability/traces/errors`

**描述**: 获取指定时间范围内的错误追踪列表。

**查询参数**:
- `startTime` - 开始时间（毫秒时间戳）
- `endTime` - 结束时间（毫秒时间戳）

---

## 性能分析API

### 1. 获取JVM实时指标

**端点**: `GET /api/observability/profiling/jvm/metrics`

**描述**: 获取最新的JVM性能指标，包含堆内存、线程、GC、CPU等。

**查询参数**:
- `instanceId` (可选) - 实例ID，默认当前实例

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "metrics": {
      "instanceId": "app-server-12345",
      "timestamp": "2025-10-24T15:30:00",
      "heapUsed": 536870912,
      "heapMax": 1073741824,
      "heapCommitted": 805306368,
      "nonHeapUsed": 104857600,
      "threadCount": 120,
      "daemonThreadCount": 15,
      "peakThreadCount": 135,
      "gcCount": 25,
      "gcTime": 150,
      "cpuUsage": 35.6,
      "loadAverage": 2.5
    },
    "heapUsagePercent": 50.0
  }
}
```

### 2. 获取JVM历史数据

**端点**: `GET /api/observability/profiling/jvm/history`

**描述**: 获取指定时间范围的JVM指标历史数据。

**查询参数**:
- `instanceId` (可选) - 实例ID
- `hours` (默认1) - 查询最近N小时

### 3. 获取慢SQL列表

**端点**: `GET /api/observability/profiling/sql/slow`

**描述**: 获取慢SQL列表（执行时间>1秒）。

**查询参数**:
- `hours` (默认1) - 查询最近N小时
- `limit` (默认100) - 返回数量限制

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "methodName": "SysUserMapper.selectById",
      "duration": 1250,
      "parameters": "[1]",
      "traceId": "abc123",
      "serviceName": "user-service",
      "timestamp": "2025-10-24T15:30:00"
    }
  ]
}
```

### 4. 获取Top N慢SQL

**端点**: `GET /api/observability/profiling/sql/top`

**描述**: 获取执行时间最长的Top N慢SQL。

**查询参数**:
- `topN` (默认10) - 返回数量

### 5. 慢SQL聚合统计

**端点**: `GET /api/observability/profiling/sql/aggregate`

**描述**: 按方法名聚合统计慢SQL。

**查询参数**:
- `hours` (默认24) - 统计最近N小时

---

## 日志分析API

### 1. 搜索日志

**端点**: `POST /api/observability/logs/search`

**描述**: 使用Elasticsearch进行全文日志搜索，支持多维度过滤。

**请求体**:
```json
{
  "keyword": "NullPointerException",
  "services": ["user-service", "order-service"],
  "levels": ["ERROR", "WARN"],
  "startTime": "2025-10-24T00:00:00",
  "endTime": "2025-10-24T23:59:59",
  "traceId": "abc123",
  "exceptionClass": "java.lang.NullPointerException",
  "from": 0,
  "size": 50,
  "sortField": "timestamp",
  "sortOrder": "desc"
}
```

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "logs": [
      {
        "id": "log-123",
        "timestamp": "2025-10-24T15:30:00",
        "level": "ERROR",
        "service": "user-service",
        "message": "NullPointerException at line 42",
        "logger": "com.basebackend.UserService",
        "thread": "http-nio-8080-exec-1",
        "traceId": "abc123",
        "spanId": "def456",
        "exceptionClass": "java.lang.NullPointerException",
        "stackTrace": "..."
      }
    ],
    "total": 150,
    "took": 45
  }
}
```

### 2. 获取日志上下文

**端点**: `GET /api/observability/logs/context/{logId}`

**描述**: 获取指定日志的上下文（前N后N条日志）。

**路径参数**:
- `logId` - 日志ID

**查询参数**:
- `before` (默认10) - 前N条
- `after` (默认10) - 后N条

### 3. 获取Top异常

**端点**: `GET /api/observability/logs/exceptions/top`

**描述**: 获取出现次数最多的Top N异常。

**查询参数**:
- `limit` (默认10) - 返回数量
- `hours` (默认24) - 统计最近N小时

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "exceptionClass": "java.lang.NullPointerException",
      "exceptionMessage": "Cannot invoke method on null object",
      "stackTraceHash": "a1b2c3d4...",
      "occurrenceCount": 156,
      "firstSeen": "2025-10-24T10:00:00",
      "lastSeen": "2025-10-24T15:30:00",
      "sampleLogId": "log-123",
      "serviceName": "user-service",
      "status": "NEW",
      "severity": "HIGH"
    }
  ]
}
```

### 4. 更新异常状态

**端点**: `PUT /api/observability/logs/exceptions/{id}/status`

**描述**: 更新异常的处理状态。

**路径参数**:
- `id` - 异常聚合ID

**查询参数**:
- `status` - 状态（NEW, IN_PROGRESS, RESOLVED, IGNORED）

### 5. 记录异常

**端点**: `POST /api/observability/logs/exceptions/record`

**描述**: 手动记录异常到聚合系统。

**查询参数**:
- `exceptionClass` - 异常类名
- `exceptionMessage` - 异常消息
- `stackTrace` - 堆栈跟踪
- `serviceName` - 服务名
- `logId` (可选) - 关联日志ID

---

## 服务拓扑API

### 1. 获取服务拓扑图

**端点**: `GET /api/observability/topology`

**描述**: 生成服务依赖关系拓扑图，包含节点和边的性能指标。

**查询参数**:
- `startTime` (可选) - 开始时间（ISO 8601格式）
- `endTime` (可选) - 结束时间（ISO 8601格式）
- 默认：最近1小时

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "nodes": [
      {
        "name": "user-service",
        "callCount": 10000,
        "errorCount": 10,
        "avgDuration": 150.5,
        "p95Duration": 300.0,
        "errorRate": 0.1,
        "healthScore": 95,
        "type": "INTERNAL"
      },
      {
        "name": "order-service",
        "callCount": 5000,
        "errorCount": 50,
        "avgDuration": 200.0,
        "p95Duration": 450.0,
        "errorRate": 1.0,
        "healthScore": 80,
        "type": "INTERNAL"
      }
    ],
    "edges": [
      {
        "source": "api-gateway",
        "target": "user-service",
        "callCount": 10000,
        "errorCount": 10,
        "avgDuration": 150.5,
        "errorRate": 0.1,
        "qps": 166.67
      }
    ],
    "statistics": {
      "totalServices": 5,
      "totalDependencies": 8,
      "totalCalls": 50000,
      "totalErrors": 150,
      "avgErrorRate": 0.3,
      "unhealthyServices": 0
    }
  }
}
```

**健康评分算法**:
```
基础分: 100

扣分规则:
- 错误率 >10%: -40
- 错误率 >5%:  -25
- 错误率 >1%:  -10

- 平均响应时间 >3s:  -30
- 平均响应时间 >1s:  -15
- 平均响应时间 >500ms: -5

- P95响应时间 >5s:  -20
- P95响应时间 >2s:  -10

最终分数: max(0, score)
```

---

## 线程分析API

### 1. 获取所有线程

**端点**: `GET /api/observability/threads`

**描述**: 获取当前JVM的所有线程详细信息。

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "threadId": 1,
      "threadName": "main",
      "state": "RUNNABLE",
      "cpuTime": 1500000000,
      "userTime": 1200000000,
      "blockedCount": 5,
      "blockedTime": 100,
      "waitedCount": 10,
      "waitedTime": 500,
      "lockName": null,
      "lockOwnerId": -1,
      "stackTrace": [
        "java.lang.Thread.run(Thread.java:750)",
        "..."
      ],
      "daemon": false,
      "priority": 5
    }
  ]
}
```

### 2. 获取Top CPU线程

**端点**: `GET /api/observability/threads/top-cpu`

**描述**: 获取CPU使用率最高的线程。

**查询参数**:
- `limit` (默认10) - 返回数量

### 3. 获取Top阻塞线程

**端点**: `GET /api/observability/threads/top-blocked`

**描述**: 获取阻塞时间最长的线程。

**查询参数**:
- `limit` (默认10) - 返回数量

### 4. 检测死锁

**端点**: `GET /api/observability/threads/deadlocks`

**描述**: 检测JVM中的死锁线程。

**响应示例**:
```json
{
  "code": 500,
  "message": "检测到 1 个死锁",
  "data": [
    {
      "threads": [
        {
          "threadId": 42,
          "threadName": "Thread-1",
          "state": "BLOCKED",
          "lockName": "java.lang.Object@12345",
          "lockOwnerId": 43
        },
        {
          "threadId": 43,
          "threadName": "Thread-2",
          "state": "BLOCKED",
          "lockName": "java.lang.Object@67890",
          "lockOwnerId": 42
        }
      ],
      "description": "检测到 2 个线程互相死锁:\n  - 线程 'Thread-1' (ID: 42) 等待锁: java.lang.Object@12345, 锁被线程 43 持有\n  - 线程 'Thread-2' (ID: 43) 等待锁: java.lang.Object@67890, 锁被线程 42 持有",
      "detectedAt": 1729756800000,
      "severity": "CRITICAL"
    }
  ]
}
```

### 5. 获取线程统计

**端点**: `GET /api/observability/threads/statistics`

**描述**: 获取线程统计信息。

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "totalThreads": 120,
    "peakThreads": 135,
    "daemonThreads": 15,
    "totalStartedThreads": 200,
    "stateDistribution": {
      "RUNNABLE": 50,
      "WAITING": 40,
      "TIMED_WAITING": 25,
      "BLOCKED": 5
    }
  }
}
```

### 6. 获取线程堆栈快照

**端点**: `GET /api/observability/threads/dump`

**描述**: 获取完整的线程堆栈转储（Thread Dump）。

**响应**: 包含所有线程、死锁和统计信息的完整快照。

---

## WebSocket

### 实时日志流

**端点**: `ws://localhost:8080/ws/logs/{serviceName}`

**描述**: 订阅指定服务的实时日志流。

**连接示例** (JavaScript):
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/logs/user-service');

ws.onopen = function() {
    console.log('Connected to log stream');
};

ws.onmessage = function(event) {
    const log = JSON.parse(event.data);
    console.log('New log:', log);
    // log 格式同 LogEntry
};

ws.onerror = function(error) {
    console.error('WebSocket error:', error);
};

ws.onclose = function() {
    console.log('Disconnected from log stream');
};
```

**推送消息格式**:
```json
{
  "id": "log-123",
  "timestamp": "2025-10-24T15:30:00",
  "level": "INFO",
  "service": "user-service",
  "message": "User logged in successfully",
  "logger": "com.basebackend.UserService",
  "thread": "http-nio-8080-exec-1",
  "traceId": "abc123"
}
```

---

## 通用响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "成功",
  "data": { /* 响应数据 */ }
}
```

### 错误响应
```json
{
  "code": 500,
  "message": "错误信息",
  "data": null
}
```

### 状态码
- `200` - 成功
- `400` - 请求参数错误
- `404` - 资源不存在
- `500` - 服务器内部错误

---

## Swagger文档

完整的API文档可通过Swagger UI访问：

**URL**: `http://localhost:8080/swagger-ui.html`

---

## 快速测试

使用curl快速测试API：

```bash
# JVM监控
curl http://localhost:8080/api/observability/profiling/jvm/metrics

# 慢SQL
curl http://localhost:8080/api/observability/profiling/sql/slow?hours=1

# 服务拓扑
curl http://localhost:8080/api/observability/topology

# 线程分析
curl http://localhost:8080/api/observability/threads/statistics

# 检测死锁
curl http://localhost:8080/api/observability/threads/deadlocks

# 日志搜索
curl -X POST http://localhost:8080/api/observability/logs/search \
  -H "Content-Type: application/json" \
  -d '{"keyword":"error","from":0,"size":20}'
```

---

## 更多文档

- [实施指南](OBSERVABILITY-REFACTOR-GUIDE.md)
- [第一阶段报告](OBSERVABILITY-IMPLEMENTATION-COMPLETE.md)
- [第二阶段报告](OBSERVABILITY-PHASE2-COMPLETE.md)
- [模块README](basebackend-observability/README.md)
