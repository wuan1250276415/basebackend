# Promtail 日志收集配置

## 概述

Promtail是Grafana Loki的日志收集代理，负责收集应用日志并推送到Loki进行存储和查询。

## 架构

```
应用日志文件 -> Promtail -> Loki -> Grafana
```

## 配置文件说明

### promtail-config.yml

主配置文件，包含以下部分：

1. **Server配置**：HTTP和gRPC监听端口
2. **Positions**：记录已读取日志的位置
3. **Clients**：Loki服务器地址
4. **Scrape Configs**：日志收集规则

### 收集的日志类型

1. **服务日志**：
   - Gateway服务
   - Auth API服务
   - User API服务
   - System API服务
   - Notification服务
   - Observability服务

2. **特殊日志**：
   - 错误日志（error.log）
   - 访问日志（access.log）
   - 操作日志（operation.log）

### Pipeline处理

每个日志流都配置了pipeline处理：

1. **Multiline**：合并多行日志（如异常堆栈）
2. **Regex**：解析日志格式，提取字段
3. **Labels**：添加标签用于查询和过滤
4. **Timestamp**：解析时间戳

## 快速启动

### 1. 启动Loki和Promtail

```bash
cd config/promtail
docker-compose -f docker-compose.promtail.yml up -d
```

### 2. 验证服务状态

```bash
# 检查Promtail状态
curl http://localhost:9080/ready

# 检查Loki状态
curl http://localhost:3100/ready

# 查看Promtail targets
curl http://localhost:9080/targets
```

### 3. 在Grafana中配置Loki数据源

1. 访问Grafana：http://localhost:3000
2. 添加数据源：Configuration -> Data Sources -> Add data source
3. 选择Loki
4. URL：http://loki:3100
5. 保存并测试

## 日志查询示例

### LogQL查询语法

```logql
# 查询特定服务的日志
{service="basebackend-gateway"}

# 查询错误级别日志
{level="ERROR"}

# 查询特定时间范围的日志
{service="basebackend-user-api"} |= "login" [5m]

# 正则表达式过滤
{service="basebackend-auth-api"} |~ "token.*expired"

# 聚合查询 - 统计错误数
sum(rate({level="ERROR"}[5m])) by (service)

# 查询特定用户的操作日志
{log_type="operation", username="admin"}

# 查询HTTP 5xx错误
{log_type="access", status=~"5.."}
```

## 日志格式要求

### 应用日志格式

```
2024-01-15 10:30:45.123 INFO [http-nio-8080-exec-1] c.b.gateway.filter.AuthFilter : User authenticated: admin
```

格式：`timestamp level [thread] logger : message`

### 访问日志格式

```
192.168.1.100 - - [15/Jan/2024:10:30:45 +0800] "GET /api/users HTTP/1.1" 200 1234 "-" "Mozilla/5.0"
```

### 操作日志格式（JSON）

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "username": "admin",
  "operation": "用户登录",
  "method": "POST",
  "params": "{\"username\":\"admin\"}",
  "ip": "192.168.1.100",
  "status": "success"
}
```

## 性能优化

### 1. 限流配置

```yaml
limits_config:
  readline_rate_enabled: true
  readline_rate: 10000        # 每秒读取行数
  readline_burst: 20000       # 突发读取行数
```

### 2. 批量发送

Promtail会自动批量发送日志到Loki，减少网络开销。

### 3. 位置记录

Promtail会记录已读取的日志位置，避免重复读取。

## 故障排查

### 1. Promtail无法连接Loki

```bash
# 检查Loki是否运行
docker ps | grep loki

# 检查网络连接
docker exec promtail ping loki

# 查看Promtail日志
docker logs promtail
```

### 2. 日志未被收集

```bash
# 检查日志文件路径是否正确
docker exec promtail ls -la /app/logs

# 检查Promtail targets
curl http://localhost:9080/targets

# 查看positions文件
docker exec promtail cat /tmp/positions.yaml
```

### 3. 日志解析失败

检查日志格式是否匹配regex表达式，可以使用在线正则测试工具验证。

## 监控指标

Promtail暴露Prometheus指标：

```bash
curl http://localhost:9080/metrics
```

关键指标：
- `promtail_read_bytes_total`：读取的字节数
- `promtail_sent_bytes_total`：发送的字节数
- `promtail_dropped_entries_total`：丢弃的日志条数
- `promtail_targets_active_total`：活跃的目标数

## 日志保留策略

Loki配置了30天的日志保留期：

```yaml
limits_config:
  retention_period: 720h  # 30天
```

可根据需要调整保留时间。

## 安全建议

1. **生产环境**：启用Loki的认证功能
2. **网络隔离**：使用内部网络，不暴露Loki端口
3. **日志脱敏**：避免记录敏感信息（密码、token等）
4. **访问控制**：限制Grafana访问权限

## 扩展配置

### 添加新服务日志收集

在`promtail-config.yml`中添加新的job：

```yaml
- job_name: new-service
  static_configs:
    - targets:
        - localhost
      labels:
        job: new-service
        service: basebackend-new-service
        environment: ${ENVIRONMENT:-dev}
        __path__: /app/logs/new-service/*.log
  pipeline_stages:
    - multiline:
        firstline: '^\d{4}-\d{2}-\d{2}'
        max_wait_time: 3s
    - regex:
        expression: '^(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(?P<level>\w+).*'
    - labels:
        level:
    - timestamp:
        source: timestamp
        format: '2006-01-02 15:04:05.000'
```

## 参考资料

- [Promtail官方文档](https://grafana.com/docs/loki/latest/clients/promtail/)
- [LogQL查询语言](https://grafana.com/docs/loki/latest/logql/)
- [Loki配置参考](https://grafana.com/docs/loki/latest/configuration/)
