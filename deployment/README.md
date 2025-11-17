# BaseBackend 部署和监控指南

> 完整的 Docker Compose 部署方案，包含 Prometheus + Grafana 监控体系

## 📋 目录

- [快速开始](#快速开始)
- [服务列表](#服务列表)
- [部署步骤](#部署步骤)
- [配置说明](#配置说明)
- [监控仪表板](#监控仪表板)
- [性能测试](#性能测试)
- [故障排查](#故障排查)
- [进阶配置](#进阶配置)

## 🚀 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存
- 至少 10GB 可用磁盘空间

### 一键启动

```bash
# 1. 进入部署目录
cd deployment

# 2. 启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看日志
docker-compose logs -f
```

### 验证部署

访问以下地址验证服务是否正常：

| 服务 | 地址 | 默认凭据 |
|------|------|----------|
| Nacos | http://localhost:8848/nacos | nacos / nacos |
| Grafana | http://localhost:3000 | admin / admin123 |
| Prometheus | http://localhost:9090 | 无需认证 |
| Zipkin | http://localhost:9411 | 无需认证 |
| Sentinel Dashboard | http://localhost:8858 | sentinel / sentinel |

## 📦 服务列表

### 基础服务

| 服务 | 容器名 | 端口 | 用途 |
|------|--------|------|------|
| MySQL 8.0 | basebackend-mysql | 3306 | 数据库 |
| Redis 7.2 | basebackend-redis | 6379 | 缓存 |
| Nacos 2.3 | basebackend-nacos | 8848, 9848 | 配置中心 & 服务发现 |
| RocketMQ NameServer | basebackend-rocketmq-namesrv | 9876 | 消息队列（命名服务器） |
| RocketMQ Broker | basebackend-rocketmq-broker | 10909, 10911 | 消息队列（代理） |

### 监控服务

| 服务 | 容器名 | 端口 | 用途 |
|------|--------|------|------|
| Prometheus | basebackend-prometheus | 9090 | 监控数据采集 |
| Grafana | basebackend-grafana | 3000 | 监控数据可视化 |
| Zipkin | basebackend-zipkin | 9411 | 分布式链路追踪 |
| Sentinel Dashboard | basebackend-sentinel-dashboard | 8858 | 流控熔断监控面板 |

## 🔧 部署步骤

### 步骤 1：准备配置文件

确保以下配置文件存在：

```
deployment/
├── docker-compose.yml
├── mysql/
│   └── init.sql
├── rocketmq/
│   └── broker.conf
├── prometheus/
│   └── prometheus.yml
└── grafana/
    └── provisioning/
        ├── datasources/
        │   └── prometheus.yml
        └── dashboards/
            ├── dashboard-provider.yml
            ├── api-performance.json
            ├── business-metrics.json
            └── system-health.json
```

### 步骤 2：导入 Nacos 配置

等待 Nacos 启动完成后（约 30 秒），导入配置文件：

**Windows (PowerShell):**
```powershell
cd ../nacos-configs
.\import-nacos-configs.ps1
```

**Linux/Mac (Bash):**
```bash
cd ../nacos-configs
bash import-nacos-configs.sh
```

验证配置导入成功：
1. 访问 http://localhost:8848/nacos
2. 登录 (nacos/nacos)
3. 进入「配置管理」→「配置列表」
4. 确认 7 个配置文件已导入

### 步骤 3：启动应用服务

```bash
# 回到项目根目录
cd ..

# 启动 Admin API
cd basebackend-admin-api
mvn spring-boot:run

# 或使用 Java JAR
java -jar target/basebackend-admin-api-1.0.0-SNAPSHOT.jar
```

### 步骤 4：验证监控数据

1. **验证 Prometheus 抓取**
   - 访问 http://localhost:9090/targets
   - 确认 `basebackend-admin-api` 状态为 UP

2. **验证 Grafana 仪表板**
   - 访问 http://localhost:3000
   - 登录 (admin/admin123)
   - 进入「BaseBackend」文件夹
   - 查看 3 个仪表板

3. **验证链路追踪**
   - 访问 http://localhost:9411
   - 调用几次 API
   - 刷新 Zipkin，应该能看到链路数据

## ⚙️ 配置说明

### 环境变量配置

可以通过 `.env` 文件或环境变量覆盖默认配置：

```bash
# .env 文件示例
MYSQL_ROOT_PASSWORD=your-password
MYSQL_DATABASE=basebackend
REDIS_PASSWORD=your-redis-password
NACOS_AUTH_ENABLE=true
```

### Prometheus 配置

编辑 `prometheus/prometheus.yml` 添加更多抓取目标：

```yaml
scrape_configs:
  - job_name: 'my-custom-service'
    static_configs:
      - targets: ['host.docker.internal:8082']
        labels:
          service: 'my-service'
```

### Grafana 数据源

自动配置的 Prometheus 数据源位于：
- `grafana/provisioning/datasources/prometheus.yml`

### 自定义仪表板

添加新仪表板：

1. 在 Grafana UI 中创建仪表板
2. 导出为 JSON
3. 保存到 `grafana/provisioning/dashboards/`
4. 重启 Grafana 容器

## 📊 监控仪表板

### 1. API 性能监控

**UID:** `basebackend-api-performance`

监控指标：
- ✅ QPS (每秒请求数)
- ✅ API 响应时间 (P95/P99)
- ✅ API 错误率
- ✅ 当前活跃请求数
- ✅ API 调用分布

### 2. 业务指标监控

**UID:** `basebackend-business-metrics`

监控指标：
- ✅ 用户注册/登录统计
- ✅ 订单创建/完成/取消统计
- ✅ 支付成功率
- ✅ 在线用户数
- ✅ 活跃用户数

### 3. 系统健康监控

**UID:** `basebackend-system-health`

监控指标：
- ✅ 应用状态 (UP/DOWN)
- ✅ CPU 使用率
- ✅ JVM 堆内存使用率
- ✅ 线程数
- ✅ GC 暂停时间
- ✅ 数据库连接池状态
- ✅ 缓存命中率

### 告警配置

在 Grafana 中配置告警规则：

1. 进入仪表板 → 编辑面板
2. 点击「Alert」标签
3. 配置告警条件
4. 添加通知渠道（邮件/钉钉/企业微信等）

## 🛡️ Sentinel 流控监控

### 访问 Sentinel Dashboard

**地址：** http://localhost:8858

**默认凭据：**
- 用户名：`sentinel`
- 密码：`sentinel`

### 功能概览

Sentinel Dashboard 提供以下功能：

#### 1. 实时监控
- ✅ QPS (每秒查询率)
- ✅ 响应时间
- ✅ 线程数
- ✅ 异常统计
- ✅ 资源调用链路

#### 2. 流控规则
- **QPS 限流：** 每秒请求数限制
- **并发线程数限流：** 限制并发线程数
- **关联流控：** 关联资源达到阈值时限流
- **链路限流：** 针对调用链路限流

**配置示例：**
1. 进入「流控规则」页面
2. 点击「新增流控规则」
3. 选择资源名（如 `admin-api`）
4. 设置阈值类型（QPS/线程数）
5. 设置单机阈值（如 100）
6. 选择流控模式（直接/关联/链路）
7. 选择流控效果（快速失败/Warm Up/排队等待）

#### 3. 熔断降级
- **慢调用比例：** RT 超过阈值的比例
- **异常比例：** 异常比例超过阈值
- **异常数：** 异常数超过阈值

**配置示例：**
1. 进入「降级规则」页面
2. 点击「新增降级规则」
3. 选择资源名
4. 设置降级策略（慢调用比例/异常比例/异常数）
5. 设置阈值和时间窗口
6. 点击「新增」

#### 4. 热点参数限流
- 针对频繁访问的热点参数进行限流
- 支持参数值例外配置

#### 5. 系统保护规则
- **Load 自适应：** 系统 Load 超过阈值时限流
- **CPU 使用率：** CPU 使用率超过阈值时限流
- **平均 RT：** 平均响应时间超过阈值时限流
- **并发线程数：** 并发线程数超过阈值时限流
- **入口 QPS：** 入口 QPS 超过阈值时限流

### 规则持久化到 Nacos

Sentinel 规则已配置持久化到 Nacos，重启后不会丢失：

**规则文件位置：**
```
nacos-configs/
├── basebackend-gateway-flow-rules.json      # 流控规则
├── basebackend-gateway-degrade-rules.json   # 降级规则
└── basebackend-gateway-gw-flow-rules.json   # 网关流控规则
```

**修改规则：**
1. 在 Sentinel Dashboard 中修改规则
2. 规则自动同步到 Nacos（SENTINEL_GROUP 组）
3. 应用重启后自动加载 Nacos 中的规则

**手动推送规则到 Nacos：**
```bash
# 进入 nacos-configs 目录
cd nacos-configs

# 导入 Sentinel 规则
# Windows
.\import-nacos-configs.ps1

# Linux/Mac
bash import-nacos-configs.sh
```

### 监控指标说明

| 指标 | 说明 | 正常范围 |
|------|------|----------|
| 通过 QPS | 成功通过的 QPS | 根据业务 |
| 拒绝 QPS | 被限流拒绝的 QPS | 越低越好 |
| 异常 QPS | 发生异常的 QPS | 越低越好 |
| 平均 RT | 平均响应时间（ms） | < 100ms |
| 并发线程数 | 当前并发线程数 | < 最大线程数的 80% |

### 常用限流策略

#### 1. API 限流（推荐）
```json
{
  "resource": "admin-api",
  "grade": 1,
  "count": 100,
  "strategy": 0,
  "controlBehavior": 0
}
```
- **说明：** 限制 admin-api 服务 QPS 为 100
- **适用场景：** 保护后端服务不被压垮

#### 2. 热点参数限流
```json
{
  "resource": "/api/user/info",
  "grade": 1,
  "count": 10,
  "paramIdx": 0,
  "paramFlowItemList": [
    {"object": "vip", "count": 20}
  ]
}
```
- **说明：** 普通用户限制 10 QPS，VIP 用户 20 QPS
- **适用场景：** 区分不同用户等级的限流

#### 3. 慢调用熔断
```json
{
  "resource": "admin-api",
  "grade": 0,
  "count": 0.5,
  "timeWindow": 10,
  "slowRatioThreshold": 0.5
}
```
- **说明：** 慢调用比例超过 50% 时熔断 10 秒
- **适用场景：** 保护下游服务，避免雪崩

## 🧪 性能测试

### 运行性能测试

**Windows (PowerShell):**
```powershell
cd deployment/performance-tests
.\performance-test.ps1
```

**Linux/Mac (Bash):**
```bash
cd deployment/performance-tests
bash performance-test.sh
```

### 自定义测试参数

```powershell
# PowerShell
.\performance-test.ps1 -TargetUrl "http://localhost:8080" -Concurrency 20 -TotalRequests 2000

# Bash
./performance-test.sh http://localhost:8080 20 2000
```

### 使用 Apache Bench

```bash
# 安装 Apache Bench
sudo apt-get install apache2-utils  # Ubuntu/Debian
brew install ab  # macOS

# 运行测试
ab -n 1000 -c 10 http://localhost:8080/actuator/health
```

### 使用 JMeter

1. 下载 Apache JMeter: https://jmeter.apache.org/
2. 创建测试计划
3. 添加 HTTP 请求采样器
4. 配置线程组（并发数）
5. 添加监听器（查看结果树、聚合报告）
6. 运行测试

## 🔍 故障排查

### 常见问题

#### 1. 服务无法启动

**症状：** docker-compose up 失败

**解决方案：**
```bash
# 查看详细日志
docker-compose logs [service-name]

# 检查端口占用
netstat -ano | findstr :3306  # Windows
lsof -i :3306  # Linux/Mac

# 清理并重新启动
docker-compose down -v
docker-compose up -d
```

#### 2. Nacos 配置导入失败

**症状：** 导入脚本报错

**解决方案：**
```bash
# 等待 Nacos 完全启动
docker-compose logs -f nacos

# 确认 Nacos 健康状态
curl http://localhost:8848/nacos/actuator/health

# 手动导入单个配置
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=common-config.yml" \
  -d "group=DEFAULT_GROUP" \
  -d "content=..."
```

#### 3. Prometheus 无法抓取应用指标

**症状：** Prometheus Targets 显示 DOWN

**解决方案：**
1. 确认应用已启动：`curl http://localhost:8080/actuator/health`
2. 确认 Metrics 端点可访问：`curl http://localhost:8080/actuator/prometheus`
3. 检查 Prometheus 配置：`prometheus/prometheus.yml`
4. 注意 Docker 网络：使用 `host.docker.internal` 而非 `localhost`

#### 4. Grafana 仪表板无数据

**症状：** 仪表板显示"No data"

**解决方案：**
1. 确认 Prometheus 数据源配置正确
2. 确认时间范围正确（右上角）
3. 确认应用已产生 Metrics 数据
4. 检查查询表达式（右侧面板）
5. 手动在 Prometheus 验证指标存在：http://localhost:9090/graph

#### 5. 内存不足

**症状：** 容器频繁重启

**解决方案：**
```bash
# 查看资源使用
docker stats

# 调整 JVM 内存限制（docker-compose.yml）
environment:
  JVM_XMS: 256m  # 降低初始内存
  JVM_XMX: 512m  # 降低最大内存

# 或停止不必要的服务
docker-compose stop zipkin
```

### 日志查看

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f nacos
docker-compose logs -f prometheus

# 查看最近 100 行日志
docker-compose logs --tail=100 grafana
```

### 数据卷管理

```bash
# 列出所有数据卷
docker volume ls

# 清理未使用的数据卷
docker volume prune

# 完全清理（⚠️ 会删除所有数据）
docker-compose down -v
```

## 🎯 进阶配置

### 持久化配置

默认配置使用 Docker 数据卷持久化，数据保存在 Docker 管理的目录。

如需自定义持久化路径，修改 `docker-compose.yml`：

```yaml
volumes:
  - /path/to/your/data:/var/lib/mysql  # 使用主机路径
```

### 多环境部署

创建不同的 compose 文件：

```bash
# 开发环境
docker-compose -f docker-compose.dev.yml up -d

# 测试环境
docker-compose -f docker-compose.test.yml up -d

# 生产环境
docker-compose -f docker-compose.prod.yml up -d
```

### 扩展服务

增加 RocketMQ Broker 副本：

```bash
docker-compose up -d --scale rocketmq-broker=3
```

### 备份和恢复

#### MySQL 备份

```bash
# 备份
docker exec basebackend-mysql mysqldump -uroot -proot123456 basebackend > backup.sql

# 恢复
docker exec -i basebackend-mysql mysql -uroot -proot123456 basebackend < backup.sql
```

#### Nacos 配置备份

```bash
# 导出所有配置
curl "http://localhost:8848/nacos/v1/cs/configs?dataId=*&group=*" > nacos-backup.json
```

#### Prometheus 数据备份

```bash
# 停止 Prometheus
docker-compose stop prometheus

# 备份数据目录
docker run --rm -v basebackend_prometheus-data:/data -v $(pwd):/backup alpine tar czf /backup/prometheus-backup.tar.gz /data

# 恢复
docker run --rm -v basebackend_prometheus-data:/data -v $(pwd):/backup alpine tar xzf /backup/prometheus-backup.tar.gz -C /

# 启动 Prometheus
docker-compose start prometheus
```

## 📝 参考资料

- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Prometheus 文档](https://prometheus.io/docs/)
- [Grafana 文档](https://grafana.com/docs/)
- [Nacos 文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [RocketMQ 文档](https://rocketmq.apache.org/docs/quick-start/)
- [Zipkin 文档](https://zipkin.io/pages/quickstart.html)
- [BaseBackend Metrics 使用指南](../basebackend-observability/METRICS_GUIDE.md)
- [BaseBackend 健康检查指南](../basebackend-observability/HEALTH_CHECK_GUIDE.md)

## 🤝 贡献

如有问题或建议，请提交 Issue 或 Pull Request。

---

*最后更新: 2025-01-13*
*维护者: BaseBackend Team*
