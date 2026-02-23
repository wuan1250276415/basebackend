# CI/CD 部署指南

## 概述

BaseBackend 项目采用基于 GitHub Actions 的 CI/CD 流水线，支持自动化构建、测试、部署到测试和生产环境。

### 流水线架构

```
┌────────────┐
│   触发条件   │
│ (Push/PR)  │
└─────┬──────┘
      │
      ▼
┌────────────────────────────────────────────┐
│              Code Quality Check            │
│  • 代码编译                                   │
│  • 单元测试                                   │
│  • 测试覆盖率检查 (JaCoCo)                    │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│          Dependency Security Scan          │
│  • OWASP 依赖检查                             │
│  • 安全漏洞扫描                               │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│               Build Services               │
│  • Maven 构建                               │
│  • 打包 JAR 文件                             │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│            Integration Tests               │
│  • 数据库集成测试                             │
│  • 服务集成测试                               │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
┌────────────────────────────────────────────┐
│            Docker Build & Push             │
│  • 构建 Docker 镜像                          │
│  • 推送至 GitHub Container Registry         │
└─────────────────┬──────────────────────────┘
                  │
                  ▼
          ┌───────┴────────┐
          ▼                ▼
  ┌────────────────┐  ┌─────────────────┐
  │  Deploy        │  │  Deploy         │
  │  Staging       │  │  Production     │
  │  (自动)        │  │  (手动审批)     │
  └────────────────┘  └─────────────────┘
```

## 流水线配置

### 1. GitHub Actions 工作流

**文件位置**: `.github/workflows/ci.yml`

**主要阶段**:

#### 阶段 1: 代码质量检查 (code-quality)
- **触发**: 所有 PR 和推送
- **内容**:
  - 代码编译验证
  - 运行单元测试
  - 生成测试覆盖率报告 (目标: 80% 行覆盖率, 75% 分支覆盖率)
  - 上传测试结果和覆盖率报告

#### 阶段 2: 安全扫描 (dependency-check)
- **触发**: 所有 PR 和推送
- **内容**:
  - OWASP Dependency Check 扫描
  - 检查 CVSS 评分 > 7 的漏洞
  - 生成安全报告
  - PR 自动评论通知

#### 阶段 3: 构建验证 (build)
- **触发**: 通过代码质量和安全检查后
- **矩阵构建**: 并行构建多个服务
  - `basebackend-scheduler`
  - `basebackend-gateway`
  - `basebackend-admin-api`
  - `basebackend-demo-api`
  - `basebackend-file-service`
- **输出**: JAR 文件 artifacts

#### 阶段 4: 集成测试 (integration-test)
- **触发**: 构建成功后（仅对 develop/main 分支）
- **服务依赖**:
  - PostgreSQL 15
  - Redis 7
- **内容**:
  - 端到端测试
  - 数据库集成测试
  - API 集成测试

#### 阶段 5: Docker 构建和推送 (docker-build)
- **触发**: develop/main 分支
- **内容**:
  - 构建多平台镜像 (linux/amd64, linux/arm64)
  - 推送至 GitHub Container Registry
  - 自动标签 (branch, sha, latest)
  - 缓存优化

#### 阶段 6: 部署到测试环境 (deploy-staging)
- **触发**: develop 分支推送
- **环境**: staging
- **内容**:
  - 拉取最新镜像
  - 部署至测试服务器
  - 健康检查
  - 冒烟测试

#### 阶段 7: 部署到生产环境 (deploy-production)
- **触发**: main 分支推送
- **环境**: production (需要审批)
- **内容**:
  - 备份生产数据
  - 部署至生产服务器
  - 健康检查
  - 冒烟测试
  - 性能指标验证

### 2. 环境变量配置

#### GitHub Secrets 配置

在 GitHub 仓库设置中配置以下 Secrets:

```bash
# 测试环境
STAGING_HOST=staging.example.com
STAGING_USER=deployer
STAGING_SSH_KEY=<SSH Private Key>

# 生产环境
PRODUCTION_HOST=production.example.com
PRODUCTION_USER=deployer
PRODUCTION_SSH_KEY=<SSH Private Key>

# 数据库密码
DB_PASSWORD=<production_db_password>
REDIS_PASSWORD=<production_redis_password>
GRAFANA_PASSWORD=<grafana_admin_password>

# 通知
SLACK_WEBHOOK_URL=<slack_webhook_url>
```

#### 本地环境变量

创建 `.env.production` 文件:

```bash
# 数据库
DB_PASSWORD=your_secure_db_password
REDIS_PASSWORD=your_secure_redis_password
GRAFANA_PASSWORD=your_secure_grafana_password

# 应用配置
SERVER_TOMCAT_THREADS_MAX=200
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

### 3. Docker Compose 配置

#### 测试环境: `docker-compose.staging.yml`

**服务列表**:
- `nginx` (端口 80/443)
- `scheduler` (端口 8081)
- `gateway` (端口 8082)
- `admin-api` (端口 8083)
- `postgres` (端口 5432)
- `redis` (端口 6379)
- `prometheus` (端口 9090)
- `grafana` (端口 3000)

#### 生产环境: `docker-compose.production.yml`

**服务列表**:
- `nginx` (高可用负载均衡)
- `scheduler` + `scheduler-secondary` (双实例)
- `gateway`
- `admin-api`
- `postgres-primary` + `postgres-replica` (主从复制)
- `redis` (单实例，需要哨兵模式)
- `nacos` (服务注册发现)
- `prometheus` (保留720小时数据)
- `grafana`
- `alertmanager` (报警通知)

**高可用特性**:
- 多实例部署
- 主从数据库复制
- 健康检查
- 自动重启
- 资源限制

## 部署流程

### 方式一: 自动化部署 (推荐)

#### 开发环境 → 测试环境

1. **推送代码至 develop 分支**

   ```bash
   git checkout develop
   git merge feature/your-feature
   git push origin develop
   ```

2. **触发 CI/CD 流水线**
   - GitHub Actions 自动运行
   - 执行代码质量检查
   - 构建和测试
   - 部署到测试环境

3. **验证部署结果**
   - 访问 `http://staging.example.com`
   - 检查 Grafana 监控: `http://staging.example.com:3000`
   - 查看 Prometheus 指标: `http://staging.example.com:9090`

#### 测试环境 → 生产环境

1. **合并到 main 分支**

   ```bash
   git checkout main
   git merge develop
   git push origin main
   ```

2. **手动审批**
   - GitHub Actions 创建生产部署审批请求
   - 需要仓库管理员点击 "Approve and Deploy"

3. **自动部署到生产**
   - 审批通过后自动执行生产部署
   - 包含数据备份、健康检查、冒烟测试

### 方式二: 手动部署

#### 测试环境部署

```bash
# 1. 拉取最新镜像
docker-compose -f docker-compose.staging.yml pull

# 2. 部署服务
docker-compose -f docker-compose.staging.yml up -d

# 3. 检查服务状态
docker-compose -f docker-compose.staging.yml ps

# 4. 查看日志
docker-compose -f docker-compose.staging.yml logs -f scheduler
```

#### 生产环境部署

```bash
# 1. 备份数据
./scripts/backup.sh

# 2. 拉取最新镜像
docker-compose -f docker-compose.production.yml pull

# 3. 停止服务 (可选)
docker-compose -f docker-compose.production.yml stop

# 4. 部署服务
docker-compose -f docker-compose.production.yml up -d

# 5. 健康检查
curl http://localhost:8080/actuator/health

# 6. 查看服务状态
docker-compose -f docker-compose.production.yml ps
```

## 回滚流程

### 自动回滚

如果生产环境健康检查失败，CI/CD 流水线会自动回滚到上一个稳定版本。

### 手动回滚

```bash
# 1. 使用备份恢复
./scripts/restore.sh basebackend_backup_YYYYMMDD_HHMMSS.tar.gz all

# 2. 或者使用 Docker 镜像回滚
docker-compose -f docker-compose.production.yml pull scheduler:previous-tag
docker-compose -f docker-compose.production.yml up -d scheduler
```

## 监控与告警

### Prometheus 监控指标

- **应用指标**: JVM 内存、线程池、HTTP 请求
- **业务指标**: 工作流执行数量、任务处理时长
- **数据库指标**: 连接数、查询性能
- **系统指标**: CPU、内存、磁盘使用率

### 告警规则

位于 `monitoring/prometheus/rules/`:

```yaml
groups:
  - name: basebackend-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"

      - alert: DatabaseDown
        expr: up{job="postgresql"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database is down"
```

### Grafana 仪表板

- **服务概览**: CPU、内存、请求量
- **工作流性能**: 执行时长、成功率
- **数据库监控**: 连接池、慢查询
- **告警中心**: 实时告警状态

## 故障排除

### 常见问题

#### 1. 构建失败

```bash
# 检查 Maven 构建日志
mvn clean package -pl basebackend-scheduler -am -X

# 检查测试报告
find . -name "*.xml" -path "*/target/surefire-reports/*" -exec cat {} \;
```

#### 2. 部署失败

```bash
# 检查 Docker 容器状态
docker-compose -f docker-compose.production.yml ps

# 查看容器日志
docker logs basebackend-scheduler-prod-1

# 检查网络连接
docker network ls
docker network inspect basebackend-network
```

#### 3. 健康检查失败

```bash
# 手动调用健康检查
curl -v http://localhost:8080/actuator/health

# 检查数据库连接
docker exec basebackend-postgres-prod-primary pg_isready -U basebackend

# 检查 Redis 连接
docker exec basebackend-redis-prod redis-cli ping
```

### 调试工具

```bash
# 进入容器调试
docker exec -it basebackend-scheduler-prod-1 /bin/bash

# 查看资源使用
docker stats

# 查看网络详情
docker network inspect basebackend-network

# 查看磁盘使用
docker system df -v
```

## 最佳实践

### 1. 版本管理

- 使用语义化版本号 (Semantic Versioning)
- 主分支保护，禁止直接推送
- 所有更改通过 PR 审核

### 2. 测试策略

- 单元测试覆盖率 > 80%
- 集成测试覆盖核心业务流程
- 性能测试验证关键指标

### 3. 安全实践

- 定期更新依赖包
- 扫描安全漏洞
- 使用强密码和密钥
- 启用 RBAC 权限控制

### 4. 监控告警

- 设置合理的告警阈值
- 配置多种通知渠道
- 定期验证告警有效性

### 5. 备份策略

- 每日自动备份
- 异地存储备份文件
- 定期测试恢复流程

## 性能优化

### 1. CI/CD 流水线优化

- 并行执行构建任务
- 使用 Docker 层缓存
- 分离构建和部署阶段

### 2. 应用性能优化

- 调整 JVM 参数
- 优化数据库连接池
- 启用响应缓存

### 3. 监控性能优化

- 调整指标采集频率
- 优化查询性能
- 使用聚合指标

## 维护计划

### 日常维护

- 每日检查服务状态
- 监控告警处理
- 查看错误日志

### 每周维护

- 分析性能趋势
- 更新依赖包
- 备份验证

### 每月维护

- 容量规划
- 安全审计
- 灾难恢复演练

## 联系信息

- **开发团队**: dev@example.com
- **运维团队**: ops@example.com
- **紧急联系人**: +86-138-xxxx-xxxx

---

**文档版本**: v1.0
**最后更新**: 2024-11-24
**维护人**: BaseBackend DevOps Team
