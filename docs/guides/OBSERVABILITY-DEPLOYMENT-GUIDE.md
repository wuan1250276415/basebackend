# 可观测性系统实施指南

## 概述

本系统实现了完整的可观测性解决方案，包括：
- **结构化日志** - 基于 Logback + Loki 的集中式日志管理
- **指标监控** - 基于 Micrometer + Prometheus 的指标采集
- **分布式追踪** - 基于 Brave + Tempo 的链路追踪
- **智能告警** - 多渠道告警通知（邮件、钉钉、企业微信）

---

## 一、架构设计

### 1.1 技术栈

| 组件 | 技术选型 | 用途 |
|------|---------|------|
| 日志采集 | Logback + Logstash Encoder | 结构化日志输出 |
| 日志存储 | Loki | 日志聚合存储 |
| 指标采集 | Micrometer + Spring Actuator | 应用指标收集 |
| 指标存储 | Prometheus | 时序数据库 |
| 追踪采集 | Brave (OpenTelemetry 兼容) | 分布式追踪 |
| 追踪存储 | Tempo | 追踪数据存储 |
| 告警引擎 | 自研 | 规则评估和通知 |
| 前端展示 | React + Ant Design | 监控仪表板 |

### 1.2 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     应用层 (admin-api)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ Logging  │  │ Metrics  │  │ Tracing  │  │ Alerting │   │
│  │  Module  │  │  Module  │  │  Module  │  │  Module  │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
└───────┼─────────────┼─────────────┼─────────────┼──────────┘
        │             │             │             │
        ▼             ▼             ▼             ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│    Loki    │ │ Prometheus │ │   Tempo    │ │   SMTP/    │
│  (日志)    │ │  (指标)    │ │  (追踪)    │ │   钉钉/    │
│            │ │            │ │            │ │   企业微信  │
└─────┬──────┘ └─────┬──────┘ └─────┬──────┘ └────────────┘
      │              │              │
      └──────────────┴──────────────┘
                     │
              ┌──────▼──────┐
              │  Query API  │
              │  (REST API) │
              └──────┬──────┘
                     │
              ┌──────▼──────┐
              │  Admin Web  │
              │  (前端页面) │
              └─────────────┘
```

---

## 二、部署配置

### 2.1 Docker Compose 部署（推荐）

创建 `docker-compose-observability.yml`:

```yaml
version: '3.8'

services:
  # Loki - 日志聚合
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml
    volumes:
      - ./loki-data:/loki
    networks:
      - observability

  # Prometheus - 指标存储
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - observability

  # Tempo - 追踪存储
  tempo:
    image: grafana/tempo:latest
    ports:
      - "3200:3200"   # Tempo HTTP
      - "9411:9411"   # Zipkin
    volumes:
      - ./tempo.yml:/etc/tempo.yaml
      - ./tempo-data:/tmp/tempo
    command: ["-config.file=/etc/tempo.yaml"]
    networks:
      - observability

  # Grafana - 可视化（可选）
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - ./grafana-data:/var/lib/grafana
    networks:
      - observability

networks:
  observability:
    driver: bridge
```

### 2.2 Prometheus 配置

创建 `prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          application: 'basebackend-admin-api'
```

### 2.3 Tempo 配置

创建 `tempo.yml`:

```yaml
server:
  http_listen_port: 3200

distributor:
  receivers:
    zipkin:

storage:
  trace:
    backend: local
    local:
      path: /tmp/tempo/traces

compactor:
  compaction:
    block_retention: 48h
```

### 2.4 启动服务

```bash
# 启动可观测性基础设施
docker-compose -f docker-compose-observability.yml up -d

# 验证服务状态
docker-compose -f docker-compose-observability.yml ps
```

---

## 三、应用配置

### 3.1 application.yml 配置

在 `basebackend-admin-api/src/main/resources/application.yml` 中添加：

```yaml
spring:
  application:
    name: basebackend-admin-api

  # 邮件配置（用于告警）
  mail:
    host: smtp.example.com
    port: 587
    username: alert@example.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

# 日志配置
logging:
  config: classpath:logback-structured.xml
  structured:
    enabled: true
    loki-enabled: true
    loki-url: http://localhost:3100/loki/api/v1/push

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0  # 100% 采样（生产环境建议降低到 0.1）

# 可观测性配置
observability:
  # Prometheus 地址
  prometheus:
    url: http://localhost:9090

  # Loki 地址
  loki:
    url: http://localhost:3100

  # Tempo 配置
  tempo:
    enabled: true
    endpoint: http://localhost:9411/api/v2/spans
    query-url: http://localhost:3200

  # 告警配置
  alert:
    # 邮件告警
    email:
      enabled: true
      from: alert@example.com
      to: admin@example.com

    # 钉钉告警
    dingtalk:
      enabled: false
      webhook: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
      secret: YOUR_SECRET

    # 企业微信告警
    wechat:
      enabled: false
      webhook: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY
```

### 3.2 Logback 配置

日志配置文件已创建：`basebackend-logging/src/main/resources/logback-structured.xml`

关键配置点：
- JSON 格式输出
- MDC 上下文传播 (TraceId, RequestId, UserId)
- Loki 集成
- 异步日志

---

## 四、功能说明

### 4.1 结构化日志

**特性：**
- 自动注入 TraceId、RequestId、UserId 到日志上下文
- JSON 格式输出，便于机器解析
- 自动记录 API 请求/响应
- 慢 API 检测（>1s 自动告警）
- 敏感数据脱敏

**使用示例：**

```java
import com.basebackend.logging.context.LogContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserService {
    public void processUser(Long userId) {
        // 手动设置用户ID到日志上下文
        LogContext.setUserId(userId.toString());

        log.info("Processing user");
        // 输出: {"timestamp":"2025-10-20T14:30:00","level":"INFO","message":"Processing user","traceId":"abc123","userId":"123"}
    }
}
```

### 4.2 指标监控

**自动采集的指标：**

| 类别 | 指标名称 | 说明 |
|------|---------|------|
| API | `api.calls.total` | API 调用总数 |
| API | `api.response.time` | API 响应时间 |
| API | `api.errors.total` | API 错误数 |
| API | `api.active.requests` | 活跃请求数 |
| JVM | `jvm_memory_used_bytes` | JVM 内存使用 |
| JVM | `jvm_gc_pause_seconds` | GC 暂停时间 |
| System | `system_cpu_usage` | CPU 使用率 |
| System | `disk_free_bytes` | 磁盘剩余空间 |

**自定义指标示例：**

```java
import com.basebackend.observability.metrics.CustomMetrics;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CustomMetrics customMetrics;

    public void createOrder() {
        customMetrics.recordBusinessOperation("order", "create");
        // 业务逻辑...
    }
}
```

### 4.3 分布式追踪

**特性：**
- 自动为每个请求生成 TraceId
- 跨服务追踪（通过 HTTP Header 传播）
- 响应头自动添加 `X-Trace-Id` 和 `X-Span-Id`
- 支持查询完整调用链

**TraceId 传播流程：**

```
客户端请求
   │
   ├──> TracingFilter (生成/读取 TraceId)
   │
   ├──> LogContextFilter (注入到 MDC)
   │
   ├──> 业务逻辑 (TraceId 自动传播到日志)
   │
   └──> 响应 (TraceId 写入响应头)
```

### 4.4 智能告警

**支持的告警类型：**

1. **阈值告警** - 指标超过阈值时触发
   - CPU 使用率 > 80%
   - 内存使用率 > 85%
   - API 错误率 > 5%
   - 响应时间 > 1000ms

2. **日志告警** - 错误日志数量超标
   - ERROR 日志数 > 10/分钟
   - FATAL 日志出现

3. **自定义告警** - 根据业务规则触发

**告警级别：**
- INFO - 信息提示
- WARNING - 警告
- ERROR - 错误
- CRITICAL - 严重（会 @ 所有人）

**通知渠道：**
- Email - 发送邮件通知
- DingTalk - 钉钉机器人
- WeChat - 企业微信机器人

**告警抑制：**
- 相同规则 5 分钟内只发送一次
- 防止告警风暴

---

## 五、前端页面使用

### 5.1 可观测性概览

**访问路径：** `/monitor/observability/overview`

**功能：**
- 系统指标实时展示（CPU、内存、API 调用数、错误率）
- 日志统计（按级别分组）
- 追踪统计（总数、平均响应时间、慢追踪）
- 告警统计（总数、按级别、通知成功率）
- 自动刷新（每30秒）

### 5.2 日志查询

**访问路径：** `/monitor/observability/logs`

**功能：**
- 关键词搜索
- 按日志级别过滤
- 按 TraceId 查询
- 按应用名称过滤
- 时间范围选择
- 实时查询 Loki

### 5.3 追踪查询

**访问路径：** `/monitor/observability/traces`

**功能：**
- 按服务名称搜索
- 按响应时间过滤（慢追踪检测）
- 查看完整调用链
- Span 详情查看

### 5.4 告警管理

**访问路径：** `/monitor/observability/alerts`

**功能：**
- 创建/编辑/删除告警规则
- 查看最近告警事件
- 测试告警规则
- 启用/禁用规则

---

## 六、API 接口文档

### 6.1 指标查询 API

```http
### 查询指标数据
POST /api/observability/metrics/query
Content-Type: application/json

{
  "metricName": "api.calls.total",
  "tags": "{\"method\":\"GET\"}",
  "startTime": "2025-10-20T10:00:00",
  "endTime": "2025-10-20T11:00:00",
  "aggregation": "sum",
  "step": 60
}

### 获取系统概览
GET /api/observability/metrics/overview

### 获取可用指标列表
GET /api/observability/metrics/available
```

### 6.2 日志查询 API

```http
### 查询日志
POST /api/observability/logs/query
Content-Type: application/json

{
  "keyword": "error",
  "level": "ERROR",
  "traceId": "abc123",
  "application": "basebackend-admin-api",
  "startTime": "2025-10-20T10:00:00",
  "endTime": "2025-10-20T11:00:00",
  "limit": 100
}

### 获取日志统计
GET /api/observability/logs/stats?startTime=2025-10-20T10:00:00&endTime=2025-10-20T11:00:00

### 根据 TraceId 查询日志
GET /api/observability/logs/trace/{traceId}
```

### 6.3 追踪查询 API

```http
### 根据 TraceId 查询
GET /api/observability/traces/{traceId}

### 搜索追踪
POST /api/observability/traces/search
Content-Type: application/json

{
  "serviceName": "basebackend-admin-api",
  "minDuration": 100,
  "maxDuration": 5000,
  "limit": 20
}

### 获取服务列表
GET /api/observability/traces/services

### 获取追踪统计
GET /api/observability/traces/stats?serviceName=basebackend-admin-api&hours=1
```

### 6.4 告警管理 API

```http
### 注册告警规则
POST /api/observability/alerts/rules
Content-Type: application/json

{
  "ruleName": "CPU使用率过高",
  "ruleType": "THRESHOLD",
  "metricName": "system_cpu_usage",
  "thresholdValue": 80,
  "comparisonOperator": ">",
  "severity": "WARNING",
  "enabled": true,
  "notifyChannels": "email,dingtalk"
}

### 获取所有规则
GET /api/observability/alerts/rules

### 获取最近告警事件
GET /api/observability/alerts/events

### 测试规则
POST /api/observability/alerts/rules/test

### 删除规则
DELETE /api/observability/alerts/rules/{ruleId}
```

---

## 七、故障排查

### 7.1 日志不显示

**问题：** Loki 中查询不到日志

**排查步骤：**
1. 检查 Loki 服务是否运行
   ```bash
   curl http://localhost:3100/ready
   ```

2. 检查应用配置
   ```yaml
   logging:
     structured:
       loki-enabled: true
       loki-url: http://localhost:3100/loki/api/v1/push
   ```

3. 查看应用日志是否有错误
   ```bash
   tail -f logs/application.log | grep -i loki
   ```

### 7.2 指标无数据

**问题：** Prometheus 无法采集指标

**排查步骤：**
1. 检查 Actuator 端点是否暴露
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. 检查 Prometheus 配置中的 targets
   ```yaml
   - targets: ['host.docker.internal:8080']  # 确保地址正确
   ```

3. 查看 Prometheus Targets 页面
   ```
   http://localhost:9090/targets
   ```

### 7.3 告警不发送

**问题：** 告警规则触发但未收到通知

**排查步骤：**
1. 检查告警引擎日志
   ```bash
   grep -i "alert" logs/application.log
   ```

2. 检查通知器配置
   ```yaml
   observability:
     alert:
       email:
         enabled: true
         from: alert@example.com
         to: admin@example.com
   ```

3. 测试告警规则
   ```bash
   curl -X POST http://localhost:8080/api/observability/alerts/rules/test \
     -H "Content-Type: application/json" \
     -d '{"ruleName":"测试","ruleType":"THRESHOLD",...}'
   ```

---

## 八、性能优化建议

### 8.1 日志优化

1. **降低采样率**（生产环境）
   ```yaml
   logging:
     structured:
       sampling-rate: 0.1  # 只记录 10% 的日志
   ```

2. **使用异步日志**
   ```xml
   <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
       <appender-ref ref="CONSOLE" />
       <queueSize>512</queueSize>
   </appender>
   ```

3. **定期清理旧日志**
   ```yaml
   logging:
     file:
       max-history: 7  # 保留 7 天
   ```

### 8.2 指标优化

1. **降低采集频率**
   ```yaml
   management:
     metrics:
       export:
         prometheus:
           step: 1m  # 改为1分钟
   ```

2. **禁用不需要的指标**
   ```yaml
   management:
     metrics:
       enable:
         jvm: true
         system: true
         http: true
         jdbc: false  # 禁用 JDBC 指标
   ```

### 8.3 追踪优化

1. **降低采样率**（生产环境）
   ```yaml
   management:
     tracing:
       sampling:
         probability: 0.01  # 1% 采样
   ```

2. **排除静态资源**
   ```java
   @Bean
   public FilterRegistrationBean<TracingFilter> tracingFilterRegistration(TracingFilter filter) {
       FilterRegistrationBean<TracingFilter> registration = new FilterRegistrationBean<>(filter);
       registration.addUrlPatterns("/api/*");  // 只追踪 API 请求
       registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
       return registration;
   }
   ```

---

## 九、最佳实践

### 9.1 日志最佳实践

✅ **推荐做法：**
```java
// 使用结构化日志
log.info("User login successful - userId: {}, ip: {}", userId, ip);

// 使用 MDC 传递上下文
LogContext.setUserId(userId.toString());
log.info("Processing user data");
```

❌ **不推荐：**
```java
// 避免字符串拼接
log.info("User " + userId + " login from " + ip);

// 避免在循环中大量打印日志
for (User user : users) {
    log.debug("Processing user: " + user);  // 可能产生大量日志
}
```

### 9.2 指标最佳实践

✅ **推荐做法：**
```java
// 使用封装的 CustomMetrics
customMetrics.recordApiCall(method, uri, "success");
customMetrics.recordBusinessOperation("order", "create");

// 使用有意义的标签
Counter.builder("order.created")
    .tag("status", "success")
    .tag("channel", "web")
    .register(registry);
```

❌ **不推荐：**
```java
// 避免高基数标签（会产生大量时间序列）
Counter.builder("api.calls")
    .tag("userId", userId)  // ❌ 用户ID会导致无数个序列
    .register(registry);
```

### 9.3 告警最佳实践

✅ **推荐做法：**
- 设置合理的阈值（通过历史数据分析）
- 使用多级告警（WARNING、ERROR、CRITICAL）
- 配置告警抑制，避免告警风暴
- 定期审查和优化告警规则

❌ **不推荐：**
- 阈值过于敏感导致频繁告警
- 所有告警都设置为 CRITICAL
- 没有配置告警抑制

---

## 十、总结

本可观测性系统提供了完整的监控解决方案，涵盖：

✅ **已实现功能：**
- ✅ 结构化日志（Logback + Loki）
- ✅ 指标监控（Micrometer + Prometheus）
- ✅ 分布式追踪（Brave + Tempo）
- ✅ 智能告警（多渠道通知）
- ✅ 前端仪表板（React + Ant Design）
- ✅ REST API（完整的查询接口）

**关键特性：**
- 🚀 自动化采集 - 零侵入式监控
- 📊 实时展示 - 30秒自动刷新
- 🔍 全链路追踪 - 从日志到追踪无缝关联
- 🚨 智能告警 - 多级别、多渠道、自动抑制
- 📱 友好界面 - 直观易用的监控仪表板

**下一步建议：**
1. 根据实际业务场景调整告警阈值
2. 配置生产环境的采样率
3. 设置数据保留策略
4. 定期审查和优化监控规则
5. 培训团队成员使用监控系统

---

## 附录

### A. 配置文件清单

| 文件 | 路径 | 说明 |
|------|------|------|
| application.yml | admin-api/src/main/resources/ | 主配置文件 |
| logback-structured.xml | logging/src/main/resources/ | 日志配置 |
| prometheus.yml | docker/ | Prometheus 配置 |
| tempo.yml | docker/ | Tempo 配置 |
| docker-compose-observability.yml | docker/ | Docker 编排 |

### B. 端口清单

| 服务 | 端口 | 说明 |
|------|------|------|
| Admin API | 8080 | 后端服务 |
| Admin Web | 3001 | 前端页面 |
| Loki | 3100 | 日志服务 |
| Prometheus | 9090 | 指标服务 |
| Tempo | 3200 | 追踪服务 |
| Zipkin | 9411 | Zipkin 协议 |
| Grafana | 3000 | 可视化（可选） |

### C. 相关链接

- [Prometheus 文档](https://prometheus.io/docs/)
- [Loki 文档](https://grafana.com/docs/loki/)
- [Tempo 文档](https://grafana.com/docs/tempo/)
- [Micrometer 文档](https://micrometer.io/docs)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**文档版本：** 1.0
**最后更新：** 2025-10-20
**维护团队：** BaseBackend Team
