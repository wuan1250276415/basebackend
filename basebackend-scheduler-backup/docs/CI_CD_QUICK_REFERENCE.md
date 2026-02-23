# CI/CD 快速参考手册

## 🚀 快速开始

### 1. 基本部署流程

```bash
# 开发 → 测试环境（自动）
git checkout develop
git push origin feature/xxx

# 测试 → 生产环境（需要审批）
git checkout main
git merge develop
git push origin main
# → GitHub 上手动审批
```

### 2. GitHub Secrets 配置清单

**在仓库 Settings → Secrets and variables → Actions 中配置:**

| Secret 名称 | 说明 | 示例值 |
|-------------|------|--------|
| `STAGING_HOST` | 测试环境服务器地址 | `staging.example.com` |
| `STAGING_USER` | 测试环境部署用户 | `deployer` |
| `STAGING_SSH_KEY` | 测试环境SSH私钥 | `-----BEGIN PRIVATE KEY-----` |
| `PRODUCTION_HOST` | 生产环境服务器地址 | `production.example.com` |
| `PRODUCTION_USER` | 生产环境部署用户 | `deployer` |
| `PRODUCTION_SSH_KEY` | 生产环境SSH私钥 | `-----BEGIN PRIVATE KEY-----` |
| `DB_PASSWORD` | 生产数据库密码 | `your_strong_password` |
| `REDIS_PASSWORD` | Redis密码 | `your_redis_password` |
| `GRAFANA_PASSWORD` | Grafana管理员密码 | `your_grafana_password` |
| `SLACK_WEBHOOK_URL` | Slack通知地址 | `https://hooks.slack.com/...` |

### 3. 环境变量配置

```bash
# .env.production
DB_PASSWORD=your_secure_db_password
REDIS_PASSWORD=your_secure_redis_password
GRAFANA_PASSWORD=your_secure_grafana_password
```

## 📋 流水线阶段详解

### 阶段 1: 代码质量检查
- ✅ 编译验证
- ✅ 单元测试
- ✅ 覆盖率检查 (80% 行, 75% 分支)
- ✅ 测试报告生成

**失败处理**:
```bash
# 本地修复
mvn test -pl basebackend-scheduler
mvn jacoco:report
```

### 阶段 2: 安全扫描
- ✅ OWASP 依赖漏洞扫描
- ✅ CVSS 评分检查 (阈值: 7.0)
- ✅ 自动PR评论通知

**失败处理**:
```bash
# 更新漏洞依赖
mvn dependency:tree -pl basebackend-scheduler
# 或忽略特定CVE (仅测试环境)
mvn org.owasp:dependency-check-maven:aggregate -DfailBuildOnCVSS=10
```

### 阶段 3: 构建验证
- ✅ 多服务并行构建
- ✅ JAR 文件生成
- ✅ 制品上传

**失败处理**:
```bash
# 清理重新构建
mvn clean
mvn compile -pl basebackend-scheduler -am
```

### 阶段 4: 集成测试
- ✅ 数据库集成测试
- ✅ 服务间集成测试
- ✅ 端到端测试

**失败处理**:
```bash
# 运行集成测试
mvn verify -pl basebackend-scheduler -Pintegration
```

### 阶段 5: Docker 构建
- ✅ 多平台镜像构建
- ✅ 推送到GitHub Container Registry
- ✅ 自动标签

**失败处理**:
```bash
# 本地构建测试
docker build -t ghcr.io/your-org/basebackend-scheduler:latest -f basebackend-scheduler/Dockerfile .
docker push ghcr.io/your-org/basebackend-scheduler:latest
```

### 阶段 6: 测试环境部署
- ✅ 自动部署到测试环境
- ✅ 健康检查
- ✅ 冒烟测试

**访问地址**:
- 应用: `http://staging.example.com`
- Grafana: `http://staging.example.com:3000`
- Prometheus: `http://staging.example.com:9090`

### 阶段 7: 生产环境部署
- ✅ 数据备份
- ✅ 审批机制
- ✅ 部署验证
- ✅ 健康检查

**访问地址**:
- 应用: `https://production.example.com`
- Grafana: `https://production.example.com:3000`
- Prometheus: `https://production.example.com:9090`

## 🛠️ 常用命令

### 本地测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl basebackend-scheduler

# 运行测试并生成覆盖率报告
mvn test -Pcoverage

# 运行集成测试
mvn verify -Pintegration

# 运行特定测试类
mvn test -Dtest=ProcessDefinitionServiceImplTest

# 运行特定测试方法
mvn test -Dtest=ProcessDefinitionServiceImplTest#testFindAllActive
```

### 构建和打包

```bash
# 编译
mvn clean compile

# 打包 (跳过测试)
mvn clean package -DskipTests

# 构建特定模块
mvn clean package -pl basebackend-scheduler -am

# 构建并推送Docker镜像
mvn clean package docker:push -pl basebackend-scheduler -am
```

### 代码质量

```bash
# 代码格式检查
mvn fmt:check

# 代码格式修复
mvn fmt:format

# 安全扫描
mvn org.owasp:dependency-check-maven:aggregate

# SonarQube分析 (需要配置)
mvn sonar:sonar
```

### 部署操作

```bash
# 部署测试环境
docker-compose -f docker-compose.staging.yml up -d

# 部署生产环境
docker-compose -f docker-compose.production.yml up -d

# 查看部署状态
docker-compose -f docker-compose.production.yml ps

# 查看日志
docker-compose -f docker-compose.production.yml logs -f scheduler
```

## 🔧 故障排除

### 构建失败

**症状**: `mvn clean package` 失败

```bash
# 1. 检查依赖冲突
mvn dependency:tree -pl basebackend-scheduler

# 2. 清理本地仓库
rm -rf ~/.m2/repository

# 3. 重新下载依赖
mvn clean install -U

# 4. 检查编译错误
mvn compile -X
```

### 测试失败

**症状**: 单元测试或集成测试报错

```bash
# 1. 运行单个测试类调试
mvn test -Dtest=SendEmailDelegateTest -X

# 2. 跳过测试继续构建 (紧急情况)
mvn clean package -DskipTests

# 3. 查看测试报告
open target/surefire-reports/*.html

# 4. 查看集成测试报告
open target/failsafe-reports/*.html
```

### 部署失败

**症状**: 容器启动失败或健康检查失败

```bash
# 1. 检查容器日志
docker logs basebackend-scheduler-prod-1

# 2. 检查容器状态
docker ps -a

# 3. 检查网络连接
docker network ls
docker network inspect basebackend-network

# 4. 进入容器调试
docker exec -it basebackend-scheduler-prod-1 /bin/bash

# 5. 检查资源使用
docker stats

# 6. 重启服务
docker-compose -f docker-compose.production.yml restart scheduler
```

### 数据库连接失败

**症状**: 应用无法连接数据库

```bash
# 1. 检查数据库状态
docker exec basebackend-postgres-prod-primary pg_isready -U basebackend

# 2. 测试数据库连接
docker exec basebackend-postgres-prod-primary psql -U basebackend -d basebackend_prod -c "SELECT 1;"

# 3. 检查连接池配置
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# 4. 检查数据库日志
docker logs basebackend-postgres-prod-primary
```

### Redis 连接失败

**症状**: 应用无法连接Redis

```bash
# 1. 检查Redis状态
docker exec basebackend-redis-prod redis-cli ping

# 2. 测试认证
docker exec basebackend-redis-prod redis-cli -a <password> ping

# 3. 检查内存使用
docker exec basebackend-redis-prod redis-cli info memory

# 4. 清理Redis缓存 (谨慎操作)
docker exec basebackend-redis-prod redis-cli FLUSHALL
```

## 🔄 回滚流程

### 自动回滚

CI/CD 流水线在以下情况会自动回滚:
- 健康检查失败
- 冒烟测试失败
- 应用启动失败

### 手动回滚

```bash
# 1. 使用备份恢复
./scripts/restore.sh basebackend_backup_20241124_120000.tar.gz all

# 2. 或回滚到特定Docker镜像版本
docker-compose -f docker-compose.production.yml pull scheduler:1.0.0
docker-compose -f docker-compose.production.yml up -d scheduler

# 3. 验证回滚结果
curl http://localhost:8080/actuator/health
```

### 紧急回滚脚本

```bash
#!/bin/bash
# emergency-rollback.sh

echo "开始紧急回滚..."

# 1. 停止所有服务
docker-compose -f docker-compose.production.yml down

# 2. 恢复数据库
LATEST_BACKUP=$(ls -t /backup/basebackend_backup_*.tar.gz | head -n1)
./scripts/restore.sh $LATEST_BACKUP database

# 3. 启动服务
docker-compose -f docker-compose.production.yml up -d

# 4. 验证
sleep 30
curl -f http://localhost:8080/actuator/health

echo "回滚完成"
```

## 📊 监控指标

### 关键指标

| 指标类别 | 指标名称 | 告警阈值 | 说明 |
|----------|----------|----------|------|
| 可用性 | 服务健康状态 | DOWN | 应用健康检查 |
| 性能 | HTTP响应时间 | P95 > 2s | 95%请求响应时间 |
| 错误率 | HTTP 5xx错误率 | > 1% | 5xx错误占比 |
| 资源 | JVM堆内存使用率 | > 80% | 堆内存使用情况 |
| 数据库 | 连接池活跃连接数 | > 80% | 数据库连接使用情况 |

### 常用查询

```promql
# 应用请求率
rate(http_requests_total[5m])

# 平均响应时间
rate(http_request_duration_seconds_sum[5m]) / rate(http_requests_total[5m])

# 错误率
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])

# JVM堆内存使用
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# 数据库连接数
hikaricp_connections_active
```

## 📞 联系信息

| 角色 | 联系邮箱 | 电话 | 备注 |
|------|----------|------|------|
| 开发负责人 | dev-lead@example.com | +86-138-xxxx-xxxx | 技术问题 |
| 运维负责人 | ops-lead@example.com | +86-138-xxxx-xxxx | 部署问题 |
| DBA | dba@example.com | +86-138-xxxx-xxxx | 数据库问题 |
| 紧急联系人 | oncall@example.com | +86-138-xxxx-xxxx | 紧急情况 |

## 📚 相关文档

- [CI/CD部署指南](CI_CD_DEPLOYMENT_GUIDE.md) - 完整部署文档
- [监控仪表板使用说明](./monitoring/README.md) - 监控指标说明
- [数据库备份与恢复](./scripts/README.md) - 数据备份操作
- [性能优化指南](./PERFORMANCE_OPTIMIZATION.md) - 性能调优

---

**最后更新**: 2024-11-24
**版本**: v1.0
