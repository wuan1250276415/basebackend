# Promtail集成指南

## 与现有系统集成

### 1. 与Prometheus集成

Promtail暴露Prometheus指标，可以被Prometheus抓取：

**prometheus.yml配置：**

```yaml
scrape_configs:
  - job_name: 'promtail'
    static_configs:
      - targets: ['promtail:9080']
        labels:
          service: 'promtail'
```

**关键指标：**

- `promtail_read_bytes_total` - 读取的字节数
- `promtail_sent_bytes_total` - 发送的字节数
- `promtail_dropped_entries_total` - 丢弃的日志条数
- `promtail_targets_active_total` - 活跃的目标数
- `promtail_targets_failed_total` - 失败的目标数

### 2. 与Grafana集成

#### 添加Loki数据源

1. 登录Grafana (http://localhost:3000)
2. Configuration -> Data Sources -> Add data source
3. 选择Loki
4. 配置：
   - Name: Loki
   - URL: http://loki:3100
   - Access: Server (default)
5. Save & Test

#### 导入仪表板

```bash
# 使用提供的仪表板
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @grafana-loki-dashboard.json
```

或手动导入：
1. Dashboards -> Import
2. 上传 `grafana-loki-dashboard.json`
3. 选择Loki数据源
4. Import

### 3. 与应用日志集成

#### Spring Boot应用配置

**logback-spring.xml：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATH" value="${LOG_PATH:-./logs}"/>
    <property name="SERVICE_NAME" value="${spring.application.name}"/>
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %c{40} : %m%n</pattern>
        </encoder>
    </appender>
    
    <!-- 文件输出 - 所有日志 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${SERVICE_NAME}/application.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %c{40} : %m%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${SERVICE_NAME}/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- 文件输出 - 错误日志 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${SERVICE_NAME}/error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %c{40} : %m%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${SERVICE_NAME}/error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- 操作日志 - JSON格式 -->
    <appender name="OPERATION_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${SERVICE_NAME}/operation.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>username</includeMdcKeyName>
            <includeMdcKeyName>operation</includeMdcKeyName>
            <includeMdcKeyName>ip</includeMdcKeyName>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${SERVICE_NAME}/operation.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>
    
    <logger name="OPERATION_LOG" level="INFO" additivity="false">
        <appender-ref ref="OPERATION_FILE"/>
    </logger>
</configuration>
```

**application.yml：**

```yaml
logging:
  file:
    path: ./logs
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %c{40} : %m%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%t] %c{40} : %m%n"
  level:
    root: INFO
    com.basebackend: DEBUG
```

### 4. 与告警系统集成

#### Loki告警规则

创建 `loki-rules.yml`：

```yaml
groups:
  - name: basebackend-alerts
    interval: 1m
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate({level="ERROR"}[5m])) by (service) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "服务 {{ $labels.service }} 错误率过高"
          description: "错误率: {{ $value }} 错误/秒"
      
      - alert: ServiceDown
        expr: |
          absent(rate({service=~".+"}[5m]))
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.service }} 无日志输出"
          description: "可能服务已停止"
      
      - alert: SlowQuery
        expr: |
          count_over_time({job=~".+"} |~ "slow query|执行时间.*[5-9]\\d{3,}ms"[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "检测到慢查询"
          description: "5分钟内慢查询次数: {{ $value }}"
```

#### 配置Alertmanager

**alertmanager.yml：**

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'critical'
    - match:
        severity: warning
      receiver: 'warning'

receivers:
  - name: 'default'
    webhook_configs:
      - url: 'http://basebackend-notification-service:8086/api/alerts/webhook'
  
  - name: 'critical'
    webhook_configs:
      - url: 'http://basebackend-notification-service:8086/api/alerts/webhook'
    email_configs:
      - to: 'ops@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.example.com:587'
        auth_username: 'alertmanager@example.com'
        auth_password: 'password'
  
  - name: 'warning'
    webhook_configs:
      - url: 'http://basebackend-notification-service:8086/api/alerts/webhook'
```

### 5. Docker Compose完整配置

**docker-compose.observability.yml：**

```yaml
version: '3.8'

services:
  # Prometheus
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ../prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ../prometheus/alerts:/etc/prometheus/alerts
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    networks:
      - basebackend-network
    restart: unless-stopped

  # Loki
  loki:
    image: grafana/loki:2.9.3
    container_name: loki
    ports:
      - "3100:3100"
    volumes:
      - ../promtail/loki-config.yml:/etc/loki/local-config.yaml
      - loki-data:/loki
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - basebackend-network
    restart: unless-stopped

  # Promtail
  promtail:
    image: grafana/promtail:2.9.3
    container_name: promtail
    volumes:
      - ../promtail/promtail-config.yml:/etc/promtail/config.yml
      - ../../logs:/app/logs:ro
    command: -config.file=/etc/promtail/config.yml
    environment:
      - ENVIRONMENT=${ENVIRONMENT:-dev}
    networks:
      - basebackend-network
    restart: unless-stopped
    depends_on:
      - loki

  # Grafana
  grafana:
    image: grafana/grafana:10.2.0
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
      - ../grafana/provisioning:/etc/grafana/provisioning
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    networks:
      - basebackend-network
    restart: unless-stopped
    depends_on:
      - prometheus
      - loki

  # Alertmanager
  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ../prometheus/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager-data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    networks:
      - basebackend-network
    restart: unless-stopped

volumes:
  prometheus-data:
  loki-data:
  grafana-data:
  alertmanager-data:

networks:
  basebackend-network:
    external: true
```

### 6. 启动完整监控栈

```bash
# 启动所有监控服务
docker-compose -f docker-compose.observability.yml up -d

# 验证服务
curl http://localhost:9090/-/healthy  # Prometheus
curl http://localhost:3100/ready      # Loki
curl http://localhost:9080/ready      # Promtail
curl http://localhost:3000/api/health # Grafana
curl http://localhost:9093/-/healthy  # Alertmanager
```

## 最佳实践

### 1. 日志结构化

使用结构化日志格式（JSON），便于解析和查询：

```java
import org.slf4j.MDC;

// 添加上下文信息
MDC.put("userId", userId);
MDC.put("traceId", traceId);
MDC.put("operation", "login");

logger.info("User login successful");

// 清理上下文
MDC.clear();
```

### 2. 日志级别管理

- **ERROR**: 错误和异常
- **WARN**: 警告信息
- **INFO**: 重要业务操作
- **DEBUG**: 调试信息（生产环境关闭）

### 3. 敏感信息脱敏

```java
// 不要记录敏感信息
logger.info("User login: {}", username);  // ✓
logger.info("Password: {}", password);    // ✗

// 使用脱敏工具
logger.info("Card: {}", maskCardNumber(cardNumber));
```

### 4. 性能优化

- 使用异步日志appender
- 合理设置日志级别
- 定期清理旧日志
- 使用日志采样（高频日志）

### 5. 监控告警

- 设置错误率告警
- 监控日志量异常
- 关注慢查询日志
- 追踪关键业务操作

## 故障排查

### 问题1：日志未被收集

**检查清单：**
1. 日志文件路径是否正确
2. Promtail是否有读取权限
3. 日志格式是否匹配regex
4. Promtail是否连接到Loki

**解决方法：**
```bash
# 检查Promtail targets
curl http://localhost:9080/targets

# 查看Promtail日志
docker logs promtail

# 测试日志文件权限
docker exec promtail ls -la /app/logs
```

### 问题2：Loki查询慢

**优化方法：**
1. 使用标签过滤，减少扫描范围
2. 缩短查询时间范围
3. 增加Loki内存配置
4. 启用查询缓存

### 问题3：日志丢失

**可能原因：**
1. Promtail限流
2. Loki存储满
3. 网络问题

**解决方法：**
```bash
# 检查Promtail指标
curl http://localhost:9080/metrics | grep dropped

# 检查Loki存储
docker exec loki df -h /loki

# 增加限流配置
# 在promtail-config.yml中调整readline_rate
```

## 参考资料

- [Loki官方文档](https://grafana.com/docs/loki/latest/)
- [Promtail配置](https://grafana.com/docs/loki/latest/clients/promtail/configuration/)
- [LogQL查询语言](https://grafana.com/docs/loki/latest/logql/)
- [Grafana仪表板](https://grafana.com/grafana/dashboards/)
