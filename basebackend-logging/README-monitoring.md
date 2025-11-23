# 监控仪表板系统

## 📖 概述

监控仪表板系统是专为 basebackend-logging 模块设计的全方位监控解决方案，集成 Prometheus + Grafana + AlertManager，提供实时监控、可视化分析和智能告警功能。

### 核心特性

- ✅ **实时监控**：毫秒级指标采集，秒级数据展示
- ✅ **可视化分析**：20+ 专业图表，直观展示系统状态
- ✅ **智能告警**：多渠道通知，支持告警抑制和升级
- ✅ **多维度分析**：性能、错误、系统、业务四大维度
- ✅ **自定义指标**：灵活扩展业务特定指标
- ✅ **一键部署**：Docker Compose 快速启动
- ✅ **生产就绪**：高可用、自动恢复、持久化存储

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **采集频率** | 10s | 指标采集间隔 |
| **数据保留** | 15天 | Prometheus 数据保留期 |
| **告警延迟** | <30s | 从异常到告警的时间 |
| **Dashboard 刷新** | 10s | 面板自动刷新间隔 |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    监控系统架构                                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              应用层 (Application Layer)               │  │
│  │  ┌──────────────┐  ┌──────────────┐                 │  │
│  │  │ @Timer      │  │  Counter    │                 │  │
│  │  │   延迟统计    │  │   事件计数   │                 │  │
│  │  └──────┬───────┘  └──────┬───────┘                 │  │
│  │         │                 │                          │  │
│  │  ┌──────┴────────────────┴──────────────┐         │  │
│  │  │   CustomMetricsCollector            │         │  │
│  │  │   - 指标采集器                      │         │  │
│  │  │   - 业务指标统计                    │         │  │
│  │  │   - 性能监控                        │         │  │
│  │  └──────────────────┬───────────────────┘         │  │
│  └─────────────────────┼──────────────────────────────┘  │
│                        │                                  │
│  ┌─────────────────────┼──────────────────────────────┐  │
│  │                     │                              │  │
│  │  ┌──────────────────┴──────────────────────────┐ │  │
│  │  │         Micrometer Registry                  │ │  │
│  │  │   - 指标注册    - 指标存储                  │ │  │
│  │  └──────────────────┬──────────────────────────┘ │  │
│  │                     │                              │  │
│  └─────────────────────┼──────────────────────────────┘  │
│                        │                                  │
│                        ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              采集层 (Scrape Layer)                    │  │
│  │                                                       │  │
│  │  ┌───────────────────┐  ┌────────────────────┐        │  │
│  │  │   Prometheus      │  │  Actuator Endpoint │        │  │
│  │  │  - 抓取指标       │  │  /actuator/prom    │        │  │
│  │  │  - 数据存储       │  │  /actuator/metrics │        │  │
│  │  │  - 告警规则       │  │  /logging-metrics  │        │  │
│  │  └───────┬───────────┘  └────────────┬───────┘        │  │
│  │          │                          │                │  │
│  │          └──────────────┬───────────┘                │  │
│  │                         │                            │  │
│  │  ┌──────────────────────┴────────────────────────┐ │  │
│  │  │           Metrics Storage                      │ │  │
│  │  │  - TSDB 存储       - 数据压缩                  │ │  │
│  │  │  - 数据保留       - 查询优化                   │ │  │
│  │  └──────────────────────┬────────────────────────┘ │  │
│  │                         │                            │  │
│  └─────────────────────────┼────────────────────────────┘  │
│                            │                                │
│  ┌─────────────────────────┼────────────────────────────┐  │
│  │                         │                            │  │
│  │  ┌──────────────────────┴────────────────────────┐ │  │
│  │  │         AlertManager                         │ │  │
│  │  │  - 告警路由       - 告警抑制                  │ │  │
│  │  │  - 通知发送       - 升级策略                  │ │  │
│  │  └──────────────────────┬────────────────────────┘ │  │
│  │                         │                            │  │
│  │  ┌──────────────────────┴────────────────────────┐ │  │
│  │  │            Notification Channels               │ │  │
│  │  │  - Email         - Slack                      │ │  │
│  │  │  - Webhook       - PagerDuty                  │ │  │
│  │  │  - DingTalk      - WeChat                     │ │  │
│  │  └───────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────────┘  │
│                            │                                │
│  ┌─────────────────────────┼────────────────────────────┐  │
│  │                         │                            │  │
│  │  ┌──────────────────────┴────────────────────────┐ │  │
│  │  │             Grafana Dashboard                  │ │  │
│  │  │                                                │ │  │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐        │ │  │
│  │  │  │  概览面板  │ │  性能面板  │ │  错误面板  │        │ │  │
│  │  │  │  Overview │ │ Performance│ │  Error   │        │ │  │
│  │  │  └────┬─────┘ └────┬─────┘ └────┬─────┘        │ │  │
│  │  │       │            │            │               │ │  │
│  │  │  ┌────┴────────────┴────────────┴───┐        │ │  │
│  │  │  │        实时日志流监控                │        │ │  │
│  │  │  │    Live Log Stream Monitoring      │        │ │  │
│  │  │  └─────────────────────────────────┘        │ │  │
│  │  └─────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 启动监控系统

```bash
# 克隆项目
git clone https://github.com/basebackend/basebackend-logging.git
cd basebackend-logging

# 启动监控系统
docker-compose -f docker-compose.monitoring.yml up -d

# 查看启动状态
docker-compose -f docker-compose.monitoring.yml ps
```

### 2. 访问服务

- **Grafana**: http://localhost:3000 (admin/admin123)
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093
- **Node Exporter**: http://localhost:9100/metrics
- **cAdvisor**: http://localhost:8080

### 3. 配置应用监控

在 `application.yml` 中添加配置：

```yaml
basebackend:
  logging:
    monitoring:
      enabled: true
      prometheus:
        port: 9090
        path: /actuator/prometheus
      grafana:
        auto-import: true
      thresholds:
        error-rate: 5.0
        latency-p95: 500ms
        queue-depth: 1000
      toggles:
        performance-metrics: true
        business-metrics: true
        exporter-enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,logging-metrics
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 4. 导入仪表板

```bash
# 自动导入（如果启用）
# 或者手动导入
curl -X POST \\
  http://admin:admin123@localhost:3000/api/dashboards/db \\
  -H 'Content-Type: application/json' \\
  -d @monitoring/dashboard-logging.json
```

## ⚙️ 配置参数详解

### 基础配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | true | 是否启用监控 |
| `scrape-interval` | Duration | 10s | 数据采集间隔 |
| `retention` | String | 15d | 数据保留期 |

### Prometheus 配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `prometheus.port` | int | 9090 | Prometheus 端口 |
| `prometheus.path` | String | /actuator/prometheus | 指标路径 |
| `prometheus.namespace` | String | basebackend | 命名空间 |

### 告警阈值

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `thresholds.error-rate` | double | 5.0 | 错误率阈值(%) |
| `thresholds.latency-p95` | Duration | 500ms | P95延迟阈值 |
| `thresholds.queue-depth` | int | 1000 | 队列深度阈值 |
| `thresholds.disk-usage-percent` | double | 85.0 | 磁盘使用率阈值(%) |

### 功能开关

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `toggles.performance-metrics` | boolean | true | 性能指标开关 |
| `toggles.business-metrics` | boolean | true | 业务指标开关 |
| `toggles.exporter-enabled` | boolean | true | 导出器开关 |
| `toggles.health-check-enabled` | boolean | true | 健康检查开关 |

## 📊 监控指标

### 基础指标

#### 日志流量指标

```yaml
# 总日志数
logging_ingest_count

# 成功日志数
logging_ingest_count{type="success"}

# 错误日志数
logging_ingest_count{type="error"}

# 警告日志数
logging_ingest_count{type="warn"}
```

#### 性能指标

```yaml
# 延迟统计（直方图）
logging_latency_seconds_bucket
logging_latency_seconds_sum
logging_latency_seconds_count

# 吞吐量（字节）
logging_throughput_bytes

# 批量大小
logging_batch_size
```

#### 系统指标

```yaml
# 队列深度
logging_queue_depth

# 缓存命中率（%）
logging_cache_hit_ratio

# 压缩比（%）
logging_compression_ratio

# 活跃线程数
logging_active_threads

# 内存使用率（%）
logging_memory_usage
```

### 业务指标

```yaml
# 异步批量操作
logging_async_batch_count

# GZIP 压缩次数
logging_gzip_compression_count

# Redis 缓存操作
logging_redis_cache_operations

# 脱敏操作
logging_masking_operations

# 审计事件
logging_audit_events
```

## 📈 Grafana 仪表板

### 仪表板结构

#### 1. 概览面板

**关键指标卡片**：
- 总日志数（选定时间范围）
- 错误率（百分比，颜色编码）
- 平均延迟（毫秒）
- 队列深度（实时值）

#### 2. 性能监控

**时间序列图表**：
- 吞吐量趋势（字节/秒）
  - 显示当前吞吐量、平均值、最大值
  - 支持缩放和时间范围选择

- 延迟分布
  - P50、P95、P99 延迟对比
  - 延迟分位数随时间变化

#### 3. 系统资源

**资源使用图表**：
- 缓存命中率趋势
  - 目标线：80%（黄色）、90%（绿色）
  - 低于阈值触发告警

- 压缩比监控
  - GZIP 压缩效率
  - 目标范围：20%-90%

- 活跃线程数
  - 线程池状态
  - 异常峰值检测

#### 4. 错误分析

**错误统计面板**：
- 错误类型分布（横向条形图）
  - Top 10 错误类型
  - 按业务模块分组

- 错误日志 TOP 10（表格）
  - 错误次数最多的服务
  - 排序和过滤

#### 5. 业务指标

**业务统计卡片**：
- 异步批量操作次数
- GZIP 压缩次数
- Redis 操作次数
- 脱敏操作次数
- 审计事件总数

#### 6. 实时日志流

**日志查看面板**：
- 实时日志滚动
- 关键词搜索
- 日志级别过滤
- 时间范围选择

### 变量配置

#### 实例变量

```json
{
  "name": "instance",
  "type": "query",
  "query": "label_values(up, instance)",
  "multi": true,
  "includeAll": true
}
```

#### 环境变量

```json
{
  "name": "environment",
  "type": "query",
  "query": "label_values(logging_ingest_count, env)",
  "multi": true,
  "includeAll": true
}
```

### 面板配置示例

#### 错误率面板

```json
{
  "type": "stat",
  "title": "错误率",
  "targets": [
    {
      "expr": "sum(increase(logging_ingest_count{type=\"error\"}[5m])) / sum(increase(logging_ingest_count[5m])) * 100",
      "refId": "A",
      "legendFormat": "错误率"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "thresholds": {
        "steps": [
          {"color": "green", "value": 0},
          {"color": "yellow", "value": 5},
          {"color": "red", "value": 10}
        ]
      },
      "unit": "percent"
    }
  }
}
```

## 🚨 告警配置

### 告警规则

#### 1. 高错误率告警

```yaml
- alert: LoggingHighErrorRate
  expr: |
    (
      sum(increase(logging_ingest_count{type="error"}[5m])) by (job, instance) /
      sum(increase(logging_ingest_count[5m])) by (job, instance)
    ) > 0.05
  for: 2m
  labels:
    severity: critical
    team: platform
  annotations:
    summary: "日志系统错误率过高"
    description: "实例 {{ $labels.instance }} 错误率超过5%"
```

#### 2. 高延迟告警

```yaml
- alert: LoggingHighLatencyP95
  expr: |
    histogram_quantile(0.95, rate(logging_latency_seconds_bucket[5m])) > 0.5
  for: 3m
  labels:
    severity: warning
    team: platform
  annotations:
    summary: "P95延迟过高"
    description: "实例 {{ $labels.instance }} P95延迟超过500ms"
```

#### 3. 队列满载告警

```yaml
- alert: LoggingQueueDepthHigh
  expr: logging_queue_depth > 1000
  for: 2m
  labels:
    severity: critical
    team: platform
  annotations:
    summary: "队列深度过高"
    description: "实例 {{ $labels.instance }} 队列深度超过1000"
```

### 通知渠道

#### Email 配置

```yaml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alerts@basebackend.com'
  smtp_auth_username: 'alerts@basebackend.com'
  smtp_auth_password: 'password'

route:
  receiver: 'email-alerts'
  routes:
    - match:
        severity: critical
      receiver: 'critical-email-alerts'
```

#### Slack 配置

```yaml
receivers:
  - name: 'slack-alerts'
    slack_configs:
      - channel: '#alerts'
        api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        title: 'BaseBackend Alert'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
```

#### Webhook 配置

```yaml
receivers:
  - name: 'webhook-alerts'
    webhook_configs:
      - url: 'https://your-webhook-endpoint.com/alerts'
        send_resolved: true
```

### 告警抑制

```yaml
inhibit_rules:
  # 抑制低级别告警
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'cluster']

  # 抑制实例级重复告警
  - source_match:
      alertname: 'InstanceDown'
    target_match_re:
      alertname: '.*'
    equal: ['instance']
```

## 🔧 高级用法

### 1. 自定义指标

```java
@RestController
public class CustomMetricsController {

    @Autowired
    private CustomMetricsCollector metrics;

    @GetMapping("/test")
    public String test() {
        // 记录延迟
        long start = System.currentTimeMillis();
        // 业务逻辑
        long duration = System.currentTimeMillis() - start;
        metrics.recordLatency(Duration.ofMillis(duration));

        // 记录吞吐量
        metrics.recordThroughputBytes(1024);

        // 更新系统指标
        metrics.updateQueueDepth(100);
        metrics.updateCacheHitRatio(85);

        return "OK";
    }
}
```

### 2. 指标端点查询

```bash
# 查询所有日志指标
curl http://localhost:8080/actuator/logging-metrics

# 查询指定类型指标
curl http://localhost:8080/actuator/logging-metrics/performance

# 查询性能指标
curl http://localhost:8080/actuator/logging-metrics/latency

# 查询系统指标
curl http://localhost:8080/actuator/logging-metrics/system
```

### 3. Prometheus 查询示例

```promql
# 错误率
sum(increase(logging_ingest_count{type="error"}[5m])) /
sum(increase(logging_ingest_count[5m]))

# P95 延迟
histogram_quantile(0.95, rate(logging_latency_seconds_bucket[5m]))

# 平均吞吐量
rate(logging_throughput_bytes_sum[5m])

# 队列深度
logging_queue_depth

# Top 5 错误类型
topk(5, sum by (type) (increase(logging_ingest_count{type="error"}[1h])))
```

### 4. 仪表板导出导入

```java
@Autowired
private GrafanaDashboardExporter exporter;

@PostMapping("/import-dashboard")
public void importDashboard() {
    ImportResult result = exporter.importDashboard(
        "http://localhost:3000",
        "your-api-token",
        dashboardJson
    );

    if (result.isSuccess()) {
        log.info("仪表板导入成功: {}", result.getMessage());
    } else {
        log.error("仪表板导入失败: {}", result.getMessage());
    }
}
```

## 📊 性能调优

### 1. Prometheus 调优

```yaml
# prometheus.yml
global:
  scrape_interval: 15s  # 降低采集频率
  evaluation_interval: 30s

scrape_configs:
  - job_name: 'basebackend-logging'
    scrape_interval: 30s  # 对非关键指标使用更长间隔
    scrape_timeout: 10s
```

### 2. 存储优化

```yaml
# Docker Compose 调优
prometheus:
  command:
    - "--storage.tsdb.retention.time=30d"  # 延长保留期
    - "--storage.tsdb.retention.size=10GB"  # 设置大小限制
    - "--storage.tsdb.min-block-duration=2h"  # 块持续时间
    - "--storage.tsdb.max-block-duration=36h"
```

### 3. Grafana 调优

```ini
# grafana.ini
[dashboards]
default_home_dashboard_path = /etc/grafana/dashboards/logging-dashboard.json

[metrics]
enabled = false  # 禁用内部指标

[analytics]
reporting_enabled = false  # 禁用报告
check_for_updates = false  # 禁用更新检查
```

### 4. 告警优化

```yaml
# 优化告警规则
- alert: LoggingHighErrorRate
  expr: |
    sum(increase(logging_ingest_count{type="error"}[10m])) by (job, instance) /
    sum(increase(logging_ingest_count[10m])) by (job, instance)
  for: 5m  # 增加等待时间
  labels:
    severity: warning  # 初始为警告
```

## 🛠️ 故障排查

### 常见问题

#### 1. 指标未采集

**检查步骤**：
1. 确认应用端点可访问
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. 检查 Prometheus 目标状态
   - 访问 http://localhost:9090/targets
   - 确认目标状态为 UP

3. 查看 Prometheus 日志
   ```bash
   docker logs prometheus
   ```

**解决方案**：
- 启用 Actuator 端点
- 检查防火墙配置
- 验证指标路径

#### 2. 告警未触发

**检查步骤**：
1. 查看 Prometheus 告警状态
   - 访问 http://localhost:9090/alerts
   - 确认告警规则存在

2. 检查 AlertManager 配置
   - 访问 http://localhost:9093
   - 查看告警路由

3. 测试通知渠道
   ```bash
   curl -X POST http://localhost:9093/api/v1/alerts \\
        -H 'Content-Type: application/json' \\
        -d '[{"labels":{"alertname":"TestAlert","severity":"warning"}}]'
   ```

**解决方案**：
- 调整告警阈值
- 检查通知配置
- 验证告警路由

#### 3. 仪表板无数据

**检查步骤**：
1. 验证数据源配置
   - 检查 Prometheus URL
   - 确认数据源可访问

2. 查看 Grafana 日志
   ```bash
   docker logs grafana
   ```

3. 检查查询语句
   - 使用 Prometheus 查看指标
   - 验证查询语法

**解决方案**：
- 更新数据源配置
- 修正查询语句
- 重新导入仪表板

#### 4. 性能问题

**优化建议**：
- 降低采集频率
- 减少保留数据量
- 优化查询语句
- 使用记录规则

## 📝 最佳实践

### 1. 指标设计原则

```java
// ✅ 推荐：使用有意义的指标名称
logging_ingest_count{type="success"}
logging_latency_seconds_bucket
logging_queue_depth

// ❌ 避免：模糊的指标名称
custom_metric_1
data_counter
value_gauge
```

### 2. 告警配置规范

```yaml
# ✅ 推荐：明确的标签
labels:
  severity: critical
  team: platform
  component: logging

# ✅ 推荐：详细的描述
annotations:
  summary: "日志错误率过高"
  description: "实例 {{ $labels.instance }} 错误率超过5%"
  runbook_url: "https://wiki/basebackend/runbooks/logging-error-rate"
```

### 3. 监控覆盖

**必监控指标**：
- ✅ 基础：日志流量、错误率、延迟
- ✅ 性能：吞吐量、队列、线程
- ✅ 资源：CPU、内存、磁盘
- ✅ 业务：审计、脱敏、缓存

**告警阈值**：
- ✅ 错误率 > 5% → 警告
- ✅ P95 延迟 > 500ms → 警告
- ✅ 队列深度 > 1000 → 严重
- ✅ 内存使用 > 85% → 严重

### 4. 运维规范

- **每日检查**：仪表板数据更新
- **每周审查**：告警规则有效性
- **每月优化**：性能指标和存储
- **季度升级**：组件版本和安全补丁

## 🔗 相关资源

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [Micrometer 文档](https://micrometer.io/docs)
- [AlertManager 文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [basebackend 项目主页](https://github.com/basebackend/basebackend)

## 📄 许可证

本项目遵循 Apache License 2.0 许可证。

---

**更多详细信息和更新，请访问 [basebackend 项目主页](https://github.com/basebackend/basebackend)**
