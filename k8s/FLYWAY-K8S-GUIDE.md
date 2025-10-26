# Kubernetes Flyway迁移部署指南

## 概述

本项目使用InitContainer模式在Kubernetes中执行Flyway数据库迁移，确保在应用启动前完成数据库schema更新。

## 架构设计

```
┌─────────────────────────────────────┐
│   Kubernetes Pod                     │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  InitContainer: flyway-migration│ │
│  │  - 执行数据库迁移               │ │
│  │  - 从ConfigMap加载SQL脚本       │ │
│  │  - 从Secret获取数据库凭证       │ │
│  └────────────────────────────────┘ │
│              ↓                       │
│  ┌────────────────────────────────┐ │
│  │  Main Container: admin-api      │ │
│  │  - 迁移成功后才启动             │ │
│  │  - Spring Boot应用              │ │
│  └────────────────────────────────┘ │
└─────────────────────────────────────┘
```

## 快速开始

### 1. 准备数据库Secret

复制示例文件并修改配置：

```bash
cd k8s/base/admin-api
cp secret.yaml.example secret.yaml
vim secret.yaml  # 修改数据库连接信息和密码
```

**重要**: `secret.yaml` 已在 `.gitignore` 中，不会提交到代码仓库。

创建Secret：

```bash
kubectl create namespace basebackend
kubectl apply -f secret.yaml -n basebackend
```

### 2. 创建迁移脚本ConfigMap

使用提供的脚本自动创建：

```bash
# 预览（不实际创建）
./k8s/scripts/create-flyway-migration-configmap.sh --dry-run

# 创建到basebackend命名空间
./k8s/scripts/create-flyway-migration-configmap.sh

# 创建到其他命名空间
./k8s/scripts/create-flyway-migration-configmap.sh -n production
```

或手动创建：

```bash
kubectl create configmap flyway-migration-scripts \
  --from-file=basebackend-admin-api/src/main/resources/db/migration \
  --namespace=basebackend
```

### 3. 部署应用

使用Kustomize部署：

```bash
# 部署到开发环境
kubectl apply -k k8s/overlays/dev

# 部署到生产环境
kubectl apply -k k8s/overlays/prod
```

### 4. 验证部署

```bash
# 查看Pod状态（应显示InitContainer已完成）
kubectl get pods -n basebackend

# 查看InitContainer日志（Flyway迁移日志）
kubectl logs -n basebackend <pod-name> -c flyway-migration

# 查看应用日志
kubectl logs -n basebackend <pod-name> -c admin-api

# 查看迁移历史（通过临时Pod）
kubectl run flyway-info --rm -i --tty \
  --image=flyway/flyway:9.22.3 \
  --env="FLYWAY_URL=jdbc:mysql://mysql-service:3306/basebackend_admin" \
  --env="FLYWAY_USER=admin" \
  --env="FLYWAY_PASSWORD=your-password" \
  -- info
```

## ConfigMap管理

### 更新迁移脚本

当添加新的迁移脚本后，需要更新ConfigMap：

```bash
# 方法1: 使用脚本（推荐）
./k8s/scripts/create-flyway-migration-configmap.sh -n basebackend

# 方法2: 手动更新
kubectl create configmap flyway-migration-scripts \
  --from-file=basebackend-admin-api/src/main/resources/db/migration \
  --namespace=basebackend \
  --dry-run=client -o yaml | kubectl apply -f -
```

### 触发Pod重启

更新ConfigMap后，需要重启Pod以应用新的迁移：

```bash
kubectl rollout restart deployment/admin-api -n basebackend

# 查看滚动更新状态
kubectl rollout status deployment/admin-api -n basebackend
```

## 多环境配置

### 开发环境 (dev)

- **位置**: `k8s/overlays/dev/`
- **特点**:
  - 单副本部署
  - 较小的资源限制
  - 自动同步（如使用Argo CD）

```bash
kubectl apply -k k8s/overlays/dev
```

### 生产环境 (prod)

- **位置**: `k8s/overlays/prod/`
- **特点**:
  - 3个副本（高可用）
  - 更严格的资源限制
  - 手动同步（需审批）
  - Pod反亲和性（不同节点）

```bash
kubectl apply -k k8s/overlays/prod
```

## 安全性最佳实践

### 1. Secret管理

**不要将Secret提交到Git！**

推荐使用以下方式管理Secret：

- **Sealed Secrets**: 加密Secret后提交到Git
- **External Secrets Operator**: 从外部密钥管理系统（如AWS Secrets Manager）同步
- **HashiCorp Vault**: 与Vault集成管理敏感信息

示例：使用kubeseal创建Sealed Secret

```bash
# 安装kubeseal客户端
wget https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/kubeseal-linux-amd64
sudo install -m 755 kubeseal-linux-amd64 /usr/local/bin/kubeseal

# 创建Sealed Secret
kubectl create secret generic admin-api-secrets \
  --from-literal=database.url="jdbc:mysql://..." \
  --from-literal=database.username="admin" \
  --from-literal=database.password="your-password" \
  --dry-run=client -o yaml | \
  kubeseal -o yaml > sealed-secret.yaml

# 提交sealed-secret.yaml到Git（安全）
git add k8s/base/admin-api/sealed-secret.yaml
git commit -m "chore: add sealed secret for admin-api"
```

### 2. Flyway配置

在 `flyway-configmap.yaml` 中已启用安全配置：

- `FLYWAY_CLEAN_DISABLED: "true"` - 禁止clean操作（防止误删数据）
- `FLYWAY_VALIDATE_ON_MIGRATE: "true"` - 迁移前验证
- `FLYWAY_OUT_OF_ORDER: "false"` - 禁止乱序迁移

### 3. RBAC权限

创建专用ServiceAccount（可选，增强安全）：

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-api-sa
  namespace: basebackend
---
# 根据需要配置RBAC权限
```

## 故障排查

### InitContainer失败

**现象**: Pod状态为 `Init:Error` 或 `Init:CrashLoopBackOff`

**排查步骤**:

1. 查看InitContainer日志：
```bash
kubectl logs <pod-name> -c flyway-migration -n basebackend
```

2. 常见错误及解决方案：

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| `Connection refused` | 数据库服务不可达 | 检查数据库Service名称和端口 |
| `Access denied` | 数据库凭证错误 | 检查Secret中的用户名密码 |
| `Validate failed` | 迁移脚本校验失败 | 检查脚本完整性，运行validate.sh |
| `Checksum mismatch` | 已应用的脚本被修改 | 不要修改已应用的脚本，创建新版本 |

### ConfigMap未更新

**现象**: 新的迁移脚本未执行

**解决**:

1. 确认ConfigMap已更新：
```bash
kubectl get configmap flyway-migration-scripts -n basebackend -o yaml
```

2. 重启Deployment：
```bash
kubectl rollout restart deployment/admin-api -n basebackend
```

### 迁移执行缓慢

**现象**: InitContainer长时间运行

**排查**:

```bash
# 实时查看日志
kubectl logs -f <pod-name> -c flyway-migration -n basebackend

# 检查数据库性能
# 连接到数据库Pod查看慢查询日志
```

### 多副本并发问题

**现象**: 多个Pod同时执行迁移导致冲突

**说明**: Flyway使用数据库锁机制，多个实例会自动排队执行。第一个Pod执行迁移，其他Pod等待后发现已完成则直接启动。

**验证**:

```bash
# 查看所有Pod的InitContainer日志
kubectl logs -l app=admin-api -c flyway-migration -n basebackend --prefix=true
```

## CI/CD集成

### 自动化部署流程

1. **GitHub Actions构建镜像** → 推送到Docker Hub
2. **更新GitOps仓库** → 修改镜像tag
3. **Argo CD检测变更** → 自动/手动同步
4. **InitContainer执行迁移** → 应用启动

### 迁移脚本更新流程

```bash
# 1. 开发者添加新迁移脚本
git add basebackend-admin-api/src/main/resources/db/migration/V1.7__add_new_feature.sql
git commit -m "feat: add new database migration V1.7"
git push

# 2. CI/CD pipeline自动验证
# - GitHub Actions运行flyway-test.yml
# - 验证脚本命名、语法、迁移测试

# 3. 合并到main分支后，手动更新K8s ConfigMap
./k8s/scripts/create-flyway-migration-configmap.sh -n production

# 4. 通过Argo CD或kubectl触发部署
kubectl rollout restart deployment/admin-api -n production
```

## 回滚策略

### 应用回滚

```bash
# 回滚到上一个版本
kubectl rollout undo deployment/admin-api -n basebackend

# 回滚到特定版本
kubectl rollout history deployment/admin-api -n basebackend
kubectl rollout undo deployment/admin-api --to-revision=3 -n basebackend
```

### 数据库回滚

Flyway不支持自动回滚。需要手动处理：

**方案1: 数据库备份恢复**
```bash
# 在生产环境，迁移前应有自动备份
# 联系DBA恢复到迁移前的备份点
```

**方案2: 创建撤销脚本**
```sql
-- V1.8__rollback_v1.7.sql
-- 撤销V1.7的更改
DROP TABLE IF EXISTS new_feature_table;
ALTER TABLE users DROP COLUMN new_column;
```

## 监控与告警

### 推荐监控指标

- **InitContainer成功率**: 监控迁移失败次数
- **迁移执行时间**: 检测性能问题
- **数据库连接**: 迁移期间的数据库健康状态

### Prometheus示例

```yaml
# ServiceMonitor配置
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: admin-api
spec:
  selector:
    matchLabels:
      app: admin-api
  endpoints:
  - port: http
    path: /actuator/prometheus
```

## 最佳实践总结

✅ **DO**:
- 总是使用 `CREATE TABLE IF NOT EXISTS`
- 迁移脚本提交前运行 `validate.sh`
- 在测试环境验证迁移后再部署生产
- 生产迁移前备份数据库
- 使用Sealed Secrets或External Secrets管理敏感信息
- 监控InitContainer状态和迁移时间

❌ **DON'T**:
- 不要修改已应用的迁移脚本（会导致checksum错误）
- 不要在迁移脚本中使用 `DROP TABLE`（除非确定）
- 不要在迁移脚本中使用 `USE database`（Flyway会自动连接）
- 不要将Secret提交到Git仓库
- 不要在生产环境启用 `flyway.clean-disabled=false`

## 相关文档

- [Flyway使用指南](../FLYWAY-GUIDE.md) - 详细的Flyway使用文档
- [Flyway官方文档](https://documentation.red-gate.com/fd)
- [Kubernetes InitContainers](https://kubernetes.io/docs/concepts/workloads/pods/init-containers/)
