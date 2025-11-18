# BaseBackend Observability Service

可观测性服务 - 提供系统监控、追踪、日志查询和告警管理功能。

## 功能特性

### 1. 指标监控
- 查询Prometheus指标数据
- 获取系统概览信息
- 支持自定义指标查询
- 集成Micrometer和Prometheus

### 2. 分布式追踪
- 集成Jaeger/Zipkin追踪系统
- 查询追踪详情
- 搜索追踪记录
- 追踪统计分析

### 3. 日志查询
- 日志搜索和过滤
- 实时日志流
- 日志统计分析
- 支持多服务日志查询

### 4. 告警管理
- 告警规则管理（CRUD）
- 告警事件记录
- 告警统计分析
- 支持多种通知渠道

### 5. Grafana集成
- 预置监控仪表板
- 自动配置数据源
- 支持自定义仪表板

## 技术栈

- Spring Boot 3.1.5
- Micrometer + Prometheus
- Jaeger/Zipkin
- MyBatis-Plus
- Redis
- Grafana

## 快速开始

### 1. 数据库初始化

```bash
# 执行SQL脚本创建表
mysql -u root -p basebackend < src/main/resources/sql/schema.sql
```

### 2. 配置环境变量

```bash
# Nacos配置
export NACOS_SERVER_ADDR=127.0.0.1:8848

# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=basebackend
export DB_USERNAME=root
export DB_PASSWORD=root

# Redis配置
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Zipkin配置
export ZIPKIN_URL=http://localhost:9411
```

### 3. 启动服务

```bash
# 开发环境
mvn spring-boot:run

# 或使用jar包
java -jar target/basebackend-observability-service-1.0.0-SNAPSHOT.jar
```

### 4. 访问服务

- API文档: http://localhost:8087/doc.html
- Actuator: http://localhost:8087/actuator
- Prometheus指标: http://localhost:8087/actuator/prometheus

## 启动可观测性栈

使用Docker Compose启动完整的可观测性栈：

```bash
cd docker/compose/observability
docker-compose -f docker-compose.observability.yml up -d
```

这将启动：
- Prometheus (http://localhost:9090)
- Grafana (http://localhost:3000) - admin/admin
- Jaeger (http://localhost:16686)
- Loki (http://localhost:3100)

## API接口

### 指标监控

```bash
# 查询指标数据
POST /api/metrics/query
{
  "metricName": "jvm.memory.used",
  "startTime": 1700000000000,
  "endTime": 1700003600000
}

# 获取系统概览
GET /api/metrics/overview

# 获取可用指标列表
GET /api/metrics/available
```

### 分布式追踪

```bash
# 查询追踪详情
GET /api/traces/{traceId}

# 搜索追踪
POST /api/traces/search
{
  "serviceName": "basebackend-user-api",
  "startTime": 1700000000000,
  "endTime": 1700003600000
}

# 获取服务列表
GET /api/traces/services

# 获取追踪统计
GET /api/traces/stats?serviceName=basebackend-user-api&hours=1
```

### 日志查询

```bash
# 搜索日志
POST /api/logs/search
{
  "serviceName": "basebackend-user-api",
  "level": "ERROR",
  "keyword": "exception",
  "startTime": 1700000000000,
  "endTime": 1700003600000
}

# 实时日志流
GET /api/logs/tail?serviceName=basebackend-user-api&lines=100

# 获取日志统计
GET /api/logs/stats?serviceName=basebackend-user-api&hours=1
```

### 告警管理

```bash
# 注册告警规则
POST /api/alerts/rules
{
  "ruleName": "High CPU Usage",
  "metricName": "system.cpu.usage",
  "threshold": 0.8,
  "operator": "gt",
  "duration": 300,
  "severity": "warning",
  "enabled": true
}

# 获取所有告警规则
GET /api/alerts/rules

# 删除告警规则
DELETE /api/alerts/rules/{ruleId}

# 获取最近的告警事件
GET /api/alerts/events

# 获取告警统计
GET /api/alerts/stats
```

## Grafana仪表板

### 访问Grafana

1. 打开浏览器访问: http://localhost:3000
2. 使用默认账号登录: admin/admin
3. 导航到 Dashboards -> BaseBackend

### 预置仪表板

- **System Overview**: 系统概览，包含JVM内存、CPU、HTTP请求等
- 更多仪表板可以在Grafana中自定义创建

### 数据源配置

Grafana已自动配置以下数据源：
- Prometheus: http://prometheus:9090
- Loki: http://loki:3100
- Jaeger: http://jaeger:16686

## 告警规则配置

### Prometheus告警规则

告警规则定义在 `docker/compose/observability/prometheus/alert_rules.yml`

示例规则：
- ServiceDown: 服务宕机
- HighCPUUsage: CPU使用率过高
- HighMemoryUsage: 内存使用率过高
- HighErrorRate: 错误率过高
- SlowResponseTime: 响应时间过慢

### 自定义告警规则

通过API注册自定义告警规则：

```bash
curl -X POST http://localhost:8087/api/alerts/rules \
  -H "Content-Type: application/json" \
  -d '{
    "ruleName": "Custom Alert",
    "metricName": "custom.metric",
    "threshold": 100,
    "operator": "gt",
    "duration": 60,
    "severity": "warning",
    "enabled": true,
    "notificationChannels": "email"
  }'
```

## 集成到其他服务

### 1. 添加依赖

在其他微服务的pom.xml中添加：

```xml
<!-- Micrometer Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Zipkin Reporter -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### 2. 配置追踪

在application.yml中添加：

```yaml
management:
  tracing:
    sampling:
      probability: 1.0

spring:
  zipkin:
    base-url: http://localhost:9411
    enabled: true
```

### 3. 暴露Prometheus端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

## 故障排查

### 1. Prometheus无法抓取指标

检查服务是否暴露了Prometheus端点：
```bash
curl http://localhost:8087/actuator/prometheus
```

### 2. Jaeger无法接收追踪数据

检查Zipkin URL配置：
```bash
echo $ZIPKIN_URL
# 应该输出: http://localhost:9411
```

### 3. Grafana无法连接数据源

检查Docker网络连接：
```bash
docker network inspect basebackend-network
```

## 性能优化

### 1. 采样率调整

生产环境建议降低追踪采样率：
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10%采样率
```

### 2. 指标保留时间

调整Prometheus数据保留时间：
```yaml
# prometheus.yml
global:
  scrape_interval: 15s
storage:
  tsdb:
    retention.time: 15d  # 保留15天
```

### 3. 日志聚合

使用Loki进行日志聚合，减少磁盘占用。

## 监控最佳实践

1. **设置合理的告警阈值**: 避免告警疲劳
2. **使用标签**: 为指标添加有意义的标签
3. **定期审查仪表板**: 确保监控覆盖关键指标
4. **追踪关键路径**: 重点追踪核心业务流程
5. **日志结构化**: 使用结构化日志便于查询

## 相关文档

- [Prometheus文档](https://prometheus.io/docs/)
- [Grafana文档](https://grafana.com/docs/)
- [Jaeger文档](https://www.jaegertracing.io/docs/)
- [Micrometer文档](https://micrometer.io/docs)

## 许可证

MIT License
