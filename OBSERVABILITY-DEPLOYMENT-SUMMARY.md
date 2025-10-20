## 可观测性系统部署总结

### ✅ 已完成的工作

#### 1. 基础设施配置文件
- ✅ `docker-compose.yml` - Docker Compose 编排文件
  - Loki (端口 3100) - 日志聚合
  - Prometheus (端口 9090) - 指标存储
  - Tempo (端口 3200, 9411) - 追踪存储
  - Grafana (端口 3000) - 可视化界面

- ✅ `prometheus.yml` - Prometheus 配置
  - 配置了 basebackend-admin-api 采集目标
  - 15秒采集间隔

- ✅ `tempo.yml` - Tempo 配置
  - Zipkin 协议接收器
  - 本地存储配置
  - 48小时数据保留

#### 2. 启动脚本
- ✅ `start.sh` - 一键启动脚本（自动检测 sudo）
- ✅ `stop.sh` - 一键停止脚本

#### 3. 应用配置
- ✅ `application-observability.yml` - 可观测性专用配置
  - Prometheus/Loki/Tempo 地址配置
  - 告警配置（邮件/钉钉/企业微信）
  - 结构化日志配置
  - Actuator 端点配置
  - 追踪采样配置

#### 4. 部署文档
- ✅ `README.md` - 快速部署指南

### 📁 文件位置

```
/home/wuan/IdeaProjects/basebackend/
├── docker/observability/
│   ├── docker-compose.yml       ✅ 已创建
│   ├── prometheus.yml           ✅ 已创建
│   ├── tempo.yml                ✅ 已创建
│   ├── start.sh                 ✅ 已创建（可执行）
│   ├── stop.sh                  ✅ 已创建（可执行）
│   └── README.md                ✅ 已创建
└── basebackend-admin-api/src/main/resources/
    └── application-observability.yml  ✅ 已创建
```

### 🚀 下一步操作（需要您手动执行）

由于 Docker 需要 sudo 权限，请按以下步骤操作：

#### 步骤 1: 启动可观测性基础设施

```bash
cd /home/wuan/IdeaProjects/basebackend/docker/observability

# 启动服务
sudo ./start.sh

# 或手动启动
sudo docker compose up -d
```

#### 步骤 2: 验证服务状态

等待约 30 秒后验证：

```bash
# 查看容器状态
sudo docker compose ps

# 应该看到 4 个服务都是 Up 状态：
# - basebackend-loki
# - basebackend-prometheus
# - basebackend-tempo
# - basebackend-grafana

# 验证服务可访问
curl http://localhost:3100/ready  # Loki
curl http://localhost:9090/-/ready  # Prometheus
curl http://localhost:3200/ready  # Tempo
curl http://localhost:3000/api/health  # Grafana
```

#### 步骤 3: 修改应用配置并启动

```bash
cd /home/wuan/IdeaProjects/basebackend

# 1. 修改 application.yml，添加 observability profile
# spring:
#   profiles:
#     active: dev,observability

# 2. 确保使用结构化日志配置
# logging:
#   config: classpath:logback-structured.xml

# 3. 重新编译
mvn clean package -DskipTests

# 4. 启动应用
cd basebackend-admin-api
java -jar target/basebackend-admin-api-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=dev,observability
```

#### 步骤 4: 验证数据采集

```bash
# 1. 检查 Actuator 端点
curl http://localhost:8082/actuator/prometheus | head -20

# 2. 在 Prometheus 中验证
# 访问 http://localhost:9090/targets
# 应该看到 basebackend-admin-api 状态为 UP

# 3. 发起几个 API 请求生成数据
curl http://localhost:8082/actuator/health

# 4. 查询 Prometheus 指标
# 访问 http://localhost:9090
# 输入查询: api_calls_total

# 5. 查询 Loki 日志
# 访问 http://localhost:3000
# 添加数据源: Loki, URL: http://loki:3100
# 在 Explore 查询: {application="basebackend-admin-api"}
```

#### 步骤 5: 访问前端监控页面

```bash
# 启动前端
cd /home/wuan/IdeaProjects/basebackend/basebackend-admin-web
npm run dev

# 访问监控页面
# http://localhost:3001/monitor/observability/overview
```

### 🎯 访问地址汇总

| 服务 | 地址 | 用途 |
|------|------|------|
| **后端 API** | http://localhost:8082 | 应用后端 |
| **前端页面** | http://localhost:3001 | 监控页面 |
| **Prometheus** | http://localhost:9090 | 指标查询 |
| **Loki** | http://localhost:3100 | 日志查询 |
| **Tempo** | http://localhost:3200 | 追踪查询 |
| **Grafana** | http://localhost:3000 | 可视化（admin/admin） |
| **Actuator** | http://localhost:8082/actuator | 应用监控端点 |
| **Swagger** | http://localhost:8082/doc.html | API 文档 |

### 📊 监控页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 可观测性概览 | `/monitor/observability/overview` | 系统指标、日志统计、追踪统计、告警统计 |
| 日志查询 | `/monitor/observability/logs` | 搜索和过滤日志 |
| 追踪查询 | `/monitor/observability/traces` | 查看分布式追踪 |
| 告警管理 | `/monitor/observability/alerts` | 创建和管理告警规则 |

### ⚠️ 注意事项

1. **Docker 权限**: 需要 sudo 权限或将用户添加到 docker 组
2. **端口占用**: 确保 3000, 3100, 3200, 8082, 9090, 9411 端口未被占用
3. **内存要求**: 建议至少 4GB 可用内存
4. **磁盘空间**: 数据目录需要足够空间存储日志、指标和追踪数据

### 🛠️ 常用管理命令

```bash
# 查看服务日志
sudo docker compose logs -f [service_name]

# 重启服务
sudo docker compose restart [service_name]

# 停止服务
sudo docker compose down

# 完全清理（包括数据）
sudo docker compose down -v

# 查看资源使用
sudo docker stats
```

### 📝 配置调优

生产环境建议调整以下配置：

```yaml
# application-observability.yml

# 降低追踪采样率（10%）
management:
  tracing:
    sampling:
      probability: 0.1

# 启用告警
observability:
  alert:
    email:
      enabled: true
      from: alert@example.com
      to: admin@example.com
```

### ✅ 验证清单

- [ ] Docker 服务已启动（4个容器 Running）
- [ ] Prometheus 可访问 http://localhost:9090
- [ ] Grafana 可访问 http://localhost:3000
- [ ] 应用 Actuator 端点可访问
- [ ] Prometheus 采集到应用指标
- [ ] Loki 收到应用日志
- [ ] Tempo 收到追踪数据
- [ ] 前端监控页面可访问

---

**部署准备完成！请按照上述步骤手动启动 Docker 服务。** 🚀
