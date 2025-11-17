# Base Backend 部署指南

本目录包含 Base Backend 项目的完整部署文档，涵盖开发、测试和生产环境。

## 📚 文档导航

### 快速开始
- [Docker 快速部署](docker/quick-start.md) - 5分钟启动开发环境
- [开发环境搭建](../development/getting-started.md) - 完整的开发指南

### Docker 部署
- [快速开始](docker/quick-start.md) - Docker 快速部署
- [生产环境部署](docker/production.md) - 生产环境最佳实践 (待完善)
- [故障排查](docker/troubleshooting.md) - 常见问题解决 (待完善)

### Kubernetes 部署
- [部署指南](kubernetes/deployment.md) - K8s 部署步骤 (待完善)
- [Helm Charts](kubernetes/helm-charts.md) - Helm 部署 (待完善)
- [扩缩容配置](kubernetes/scaling.md) - 自动扩缩容 (待完善)

### 配置管理
- [Nacos 配置](configuration/nacos.md) - Nacos 配置中心 (待完善)
- [环境变量](configuration/environment.md) - 环境变量说明 (待完善)
- [密钥管理](configuration/secrets.md) - 密钥管理最佳实践 (待完善)

## 🚀 快速部署

### 开发环境 (Docker)

```bash
# 1. 克隆项目
git clone https://github.com/your-org/basebackend.git
cd basebackend

# 2. 启动基础设施
cd docker/compose
./start-all.sh

# 3. 导入配置
cd ../../config/nacos-configs
./import-nacos-configs.sh  # Linux/Mac
# 或
.\import-nacos-configs.ps1  # Windows

# 4. 启动应用
cd ../..
mvn clean install -DskipTests
cd basebackend-gateway
mvn spring-boot:run
```

### 测试环境 (Docker)

```bash
# 使用测试环境配置
cd docker/compose
./start-all.sh env/.env.test
```

### 生产环境 (Kubernetes)

```bash
# 使用 Helm 部署
helm install basebackend ./k8s/helm-charts \
  --namespace basebackend \
  --create-namespace \
  --values values-prod.yaml
```

## 📋 部署架构

### 开发环境架构

```
┌─────────────────────────────────────────┐
│          Developer Machine              │
│                                         │
│  ┌──────────┐  ┌──────────┐           │
│  │ Gateway  │  │Admin API │           │
│  │  :8080   │  │  :8081   │           │
│  └────┬─────┘  └────┬─────┘           │
│       │             │                  │
│       └─────────────┘                  │
│              │                         │
└──────────────┼─────────────────────────┘
               │
        ┌──────▼──────┐
        │   Docker    │
        │             │
        │  ┌────────┐ │
        │  │ MySQL  │ │
        │  │ Redis  │ │
        │  │ Nacos  │ │
        │  │RocketMQ│ │
        │  └────────┘ │
        └─────────────┘
```

### 生产环境架构

```
┌─────────────────────────────────────────────┐
│              Load Balancer                  │
│            (Nginx/ALB/SLB)                  │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────▼──────────┐
        │    Kubernetes       │
        │                     │
        │  ┌──────────────┐  │
        │  │   Gateway    │  │
        │  │  (3 replicas)│  │
        │  └──────┬───────┘  │
        │         │           │
        │  ┌──────▼───────┐  │
        │  │  User API    │  │
        │  │ System API   │  │
        │  │  Auth API    │  │
        │  │ (Auto-scale) │  │
        │  └──────┬───────┘  │
        │         │           │
        └─────────┼───────────┘
                  │
        ┌─────────▼─────────┐
        │  External Services│
        │                   │
        │  ┌─────────────┐ │
        │  │MySQL Cluster│ │
        │  │Redis Cluster│ │
        │  │Nacos Cluster│ │
        │  │RocketMQ     │ │
        │  └─────────────┘ │
        └───────────────────┘
```

## 🔧 环境要求

### 开发环境

| 组件 | 最小配置 | 推荐配置 |
|-----|---------|---------|
| CPU | 4核 | 8核 |
| 内存 | 8GB | 16GB |
| 磁盘 | 50GB | 100GB SSD |
| Docker | 24.0+ | 最新版 |
| JDK | 17 | 17 |
| Maven | 3.8+ | 3.9+ |

### 测试环境

| 组件 | 配置 |
|-----|------|
| CPU | 8核 |
| 内存 | 16GB |
| 磁盘 | 200GB SSD |
| 网络 | 100Mbps |

### 生产环境

| 组件 | 配置 |
|-----|------|
| CPU | 16核+ |
| 内存 | 32GB+ |
| 磁盘 | 500GB SSD |
| 网络 | 1Gbps |
| 高可用 | 多可用区 |

## 📦 服务依赖

### 基础设施

| 服务 | 版本 | 用途 |
|-----|------|------|
| MySQL | 8.0.33+ | 关系型数据库 |
| Redis | 7.2+ | 缓存和会话 |
| Nacos | 2.2.3+ | 配置中心和服务发现 |

### 中间件

| 服务 | 版本 | 用途 |
|-----|------|------|
| RocketMQ | 5.2.0+ | 消息队列 |
| Seata | 1.7.1+ | 分布式事务 |
| Sentinel | 1.8.6+ | 流量控制 |

### 监控组件

| 服务 | 版本 | 用途 |
|-----|------|------|
| Prometheus | 2.45+ | 指标收集 |
| Grafana | 10.0+ | 可视化 |
| Loki | 2.9+ | 日志聚合 |

## 🔐 安全配置

### 开发环境

- 使用默认密码（仅限开发）
- 禁用 SSL/TLS
- 开放所有端口

### 生产环境

- ✅ 修改所有默认密码
- ✅ 启用 SSL/TLS
- ✅ 配置防火墙规则
- ✅ 使用密钥管理服务
- ✅ 启用审计日志
- ✅ 定期安全扫描

## 📊 监控和运维

### 监控指标

- **应用指标**: CPU、内存、线程、GC
- **业务指标**: QPS、响应时间、错误率
- **基础设施**: 数据库连接、缓存命中率、消息队列积压

### 日志管理

- **应用日志**: 存储在 `logs/` 目录
- **容器日志**: 使用 `docker logs` 查看
- **集中日志**: 使用 Loki 聚合

### 告警配置

- **CPU 使用率** > 80%
- **内存使用率** > 85%
- **错误率** > 1%
- **响应时间** > 1000ms
- **数据库连接池** > 80%

## 🔄 CI/CD 流程

### 开发流程

```
开发 → 提交 → 代码审查 → 合并 → 自动构建 → 自动测试 → 部署到测试环境
```

### 发布流程

```
测试通过 → 创建发布分支 → 构建镜像 → 推送到镜像仓库 → 部署到生产环境 → 验证 → 完成
```

### 回滚流程

```
发现问题 → 停止流量 → 回滚到上一版本 → 验证 → 恢复流量 → 分析问题
```

## 📝 部署检查清单

### 部署前

- [ ] 代码已合并到发布分支
- [ ] 所有测试通过
- [ ] 数据库迁移脚本已准备
- [ ] 配置文件已更新
- [ ] 依赖服务已就绪
- [ ] 备份已完成

### 部署中

- [ ] 停止旧版本服务
- [ ] 执行数据库迁移
- [ ] 部署新版本
- [ ] 验证健康检查
- [ ] 验证关键功能
- [ ] 监控指标正常

### 部署后

- [ ] 验证所有功能
- [ ] 检查日志无异常
- [ ] 监控指标正常
- [ ] 通知相关人员
- [ ] 更新文档
- [ ] 归档部署记录

## 🆘 故障处理

### 紧急联系方式

- **架构组**: architecture@basebackend.com
- **运维组**: devops@basebackend.com
- **值班电话**: 138-0013-8000

### 故障等级

| 等级 | 描述 | 响应时间 |
|-----|------|---------|
| P0 | 服务完全不可用 | 立即 |
| P1 | 核心功能不可用 | 15分钟 |
| P2 | 部分功能异常 | 1小时 |
| P3 | 性能下降 | 4小时 |

### 常见故障

- [服务无法启动](docker/troubleshooting.md#服务无法启动)
- [数据库连接失败](docker/troubleshooting.md#数据库连接失败)
- [内存溢出](docker/troubleshooting.md#内存溢出)
- [网络超时](docker/troubleshooting.md#网络超时)

## 📚 相关文档

- [项目 README](../../README.md)
- [架构设计](../architecture/)
- [开发指南](../development/)
- [API 文档](http://localhost:8080/doc.html)
- [故障排查](../troubleshooting/)

## 🔄 更新日志

- 2025-11-17: 创建部署文档结构
- 2025-11-17: 添加 Docker 快速部署指南
- 2025-11-17: 添加开发环境搭建指南

---

**文档维护**: Architecture Team  
**最后更新**: 2025-11-17
