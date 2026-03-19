# BaseBackend 可观测性部署指南

## 📋 快速开始

本指南将帮助您快速部署 BaseBackend 可观测性系统。

---

## 前置条件

- ✅ Docker (已安装)
- ✅ Docker Compose (已安装)
- ✅ Java 17+
- ✅ Maven 3.6+

---

## 步骤 1: 启动可观测性基础设施

### 方式一：使用启动脚本（推荐）

```bash
cd /home/wuan/IdeaProjects/basebackend/docker/observability

# 如果当前用户在 docker 组
./start.sh

# 如果需要 sudo 权限
sudo ./start.sh
```

### 方式二：手动启动

```bash
cd /home/wuan/IdeaProjects/basebackend/docker/observability

# 启动所有服务
docker compose up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 验证服务

等待约 30 秒后，验证各服务是否正常：

```bash
# Loki
curl http://localhost:3100/ready

# Prometheus
curl http://localhost:9090/-/ready

# Tempo
curl http://localhost:3200/ready

# Grafana
curl http://localhost:3000/api/health
```

**Web 界面访问：**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (用户名/密码: admin/admin)
- Loki: http://localhost:3100

---

## 步骤 2: 配置应用

### 2.1 启用可观测性配置

在 `basebackend-user-api/src/main/resources/application.yml` 中添加：

```yaml
spring:
  profiles:
    active: dev,observability  # 添加 observability profile
```

已创建配置文件：`application-observability.yml`

### 2.2 配置 Logback

确保使用结构化日志配置：

在 `application.yml` 或 `application-observability.yml` 中添加：

```yaml
logging:
  config: classpath:logback-structured.xml
```

---

## 步骤 3: 编译并启动应用

### 3.1 编译项目

```bash
cd /home/wuan/IdeaProjects/basebackend

# 完整编译
mvn clean package -DskipTests

# 或仅编译 user-api
mvn clean package -pl basebackend-user-api -am -DskipTests
```

### 3.2 启动应用

```bash
cd basebackend-user-api

# 方式一：使用 Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev,observability

# 方式二：使用 jar
java -jar target/basebackend-user-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev,observability
```

### 3.3 验证应用启动

```bash
# 检查应用健康状态
curl http://localhost:8081/actuator/health

# 检查 Prometheus 指标端点
curl http://localhost:8081/actuator/prometheus

# 检查是否有日志输出到 Loki（启动几秒后）
curl "http://localhost:3100/loki/api/v1/label" | jq .
```

---

## 步骤 4: 验证数据采集

### 4.1 验证 Prometheus 采集

1. 访问 Prometheus: http://localhost:9090
2. 进入 Status > Targets
3. 确认 `basebackend-user-api` 目标状态为 UP
4. 在查询框输入：`api_calls_total` 并执行

### 4.2 验证 Loki 日志

```bash
# 查询最近的日志
curl -G "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={application="basebackend-user-api"}' \
  --data-urlencode "start=$(date -d '5 minutes ago' +%s)000000000" \
  --data-urlencode "end=$(date +%s)000000000" \
  --data-urlencode 'limit=10' | jq .
```

或在 Grafana 中：
1. 访问 http://localhost:3000
2. 添加 Loki 数据源: http://loki:3100
3. 在 Explore 中查询：`{application="basebackend-user-api"}`

### 4.3 验证 Tempo 追踪

发起几个 API 请求：

```bash
# 发起测试请求（假设有登录接口）
curl -X POST http://localhost:8081/api/user/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

然后在 Grafana 中添加 Tempo 数据源: http://tempo:3200

---

## 步骤 5: 启动前端并访问监控页面

### 5.1 启动前端

```bash
cd /home/wuan/IdeaProjects/basebackend/basebackend-admin-web

# 开发模式
npm run dev

# 或使用已构建的版本
npm run preview
```

### 5.2 访问监控页面

1. **登录系统**
   - 访问: http://localhost:3001
   - 使用管理员账号登录

2. **可观测性概览**
   - 路径: `/monitor/observability/overview`
   - 查看系统指标、日志统计、追踪统计、告警统计

3. **日志查询**
   - 路径: `/monitor/observability/logs`
   - 搜索和过滤日志

4. **追踪查询**
   - 路径: `/monitor/observability/traces`
   - 查看分布式追踪

5. **告警管理**
   - 路径: `/monitor/observability/alerts`
   - 创建和管理告警规则

---

## 步骤 6: 配置告警（可选）

### 6.1 配置邮件告警

在 `application-observability.yml` 中配置：

```yaml
spring:
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

observability:
  alert:
    email:
      enabled: true
      from: alert@example.com
      to: admin@example.com
```

### 6.2 配置钉钉告警

```yaml
observability:
  alert:
    dingtalk:
      enabled: true
      webhook: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
      secret: YOUR_SECRET
```

### 6.3 创建告警规则

通过前端页面或 API 创建告警规则：

```bash
curl -X POST http://localhost:8082/api/observability/alerts/rules \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "ruleName": "CPU使用率过高",
    "ruleType": "THRESHOLD",
    "metricName": "system_cpu_usage",
    "thresholdValue": 80,
    "comparisonOperator": ">",
    "severity": "WARNING",
    "enabled": true,
    "notifyChannels": "email",
    "description": "CPU使用率超过80%时告警"
  }'
```

---

## 常用命令

### Docker 管理

```bash
cd /home/wuan/IdeaProjects/basebackend/docker/observability

# 启动服务
./start.sh

# 停止服务
./stop.sh

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f [service_name]

# 重启单个服务
docker compose restart prometheus

# 完全清理（包括数据）
docker compose down -v
```

### 应用管理

```bash
# 重新编译
mvn clean package -DskipTests

# 启动应用
cd basebackend-user-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev,observability

# 查看应用日志
tail -f logs/application.log
```

### 数据查询

```bash
# Prometheus - 查询指标
curl "http://localhost:9090/api/v1/query?query=api_calls_total"

# Loki - 查询日志
curl -G "http://localhost:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={application="basebackend-user-api"}' \
  --data-urlencode 'limit=10'

# Tempo - 查询追踪（需要 TraceId）
curl "http://localhost:3200/api/traces/YOUR_TRACE_ID"
```

---

## 故障排查

### 服务无法启动

```bash
# 检查端口占用
netstat -tuln | grep -E '3100|9090|3200|9411|3000'

# 查看 Docker 日志
docker compose logs [service_name]

# 检查磁盘空间
df -h
```

### Prometheus 无法采集指标

1. 检查应用是否启动
2. 访问 http://localhost:8082/actuator/prometheus
3. 检查 Prometheus targets: http://localhost:9090/targets
4. 确认 `prometheus.yml` 配置正确

### Loki 没有日志

1. 检查应用配置中 `loki-enabled: true`
2. 确认 Loki 地址正确: `http://localhost:3100`
3. 查看应用日志是否有 Loki 错误
4. 测试 Loki 连接: `curl http://localhost:3100/ready`

### Tempo 没有追踪数据

1. 确认应用配置中 `tempo.enabled: true`
2. 检查 Zipkin 端点: http://localhost:9411
3. 确认采样率不为 0: `management.tracing.sampling.probability: 1.0`

---

## 性能优化

### 生产环境配置建议

```yaml
# 降低追踪采样率
management:
  tracing:
    sampling:
      probability: 0.1  # 10% 采样

# 日志保留策略
logging:
  file:
    max-history: 7  # 保留7天

# Prometheus 数据保留
# 在 prometheus.yml 的 command 中添加：
# - '--storage.tsdb.retention.time=15d'
```

---

## 文件清单

已创建的文件：

```
docker/observability/
├── docker-compose.yml          # Docker Compose 配置
├── prometheus.yml              # Prometheus 配置
├── tempo.yml                   # Tempo 配置
├── start.sh                    # 启动脚本
└── stop.sh                     # 停止脚本

basebackend-user-api/src/main/resources/
└── application-observability.yml  # 可观测性配置
```

---

## 下一步

1. ✅ 基础设施已部署
2. ✅ 配置文件已创建
3. ⏳ 需要启动 Docker 服务（需要 Docker 权限）
4. ⏳ 需要启动应用
5. ⏳ 访问前端监控页面

---

## 快速启动命令汇总

```bash
# 1. 启动可观测性基础设施
cd /home/wuan/IdeaProjects/basebackend/docker/observability
sudo ./start.sh

# 2. 编译应用
cd /home/wuan/IdeaProjects/basebackend
mvn clean package -DskipTests

# 3. 启动应用
cd basebackend-user-api
java -jar target/basebackend-user-api-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev,observability

# 4. 启动前端
cd ../basebackend-admin-web
npm run dev

# 5. 访问监控页面
# http://localhost:3001/monitor/observability/overview
```

---

**部署完成！** 🎉

如有问题，请查看各服务日志或参考 OBSERVABILITY-DEPLOYMENT-GUIDE.md 完整文档。
