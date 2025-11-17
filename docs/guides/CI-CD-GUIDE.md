# CI/CD 完整指南

本文档提供Base Backend项目的完整CI/CD配置和使用指南。

## 📋 目录

- [架构概览](#架构概览)
- [快速开始](#快速开始)
- [GitHub Actions配置](#github-actions配置)
- [Docker镜像构建](#docker镜像构建)
- [GitOps部署](#gitops部署)
- [Argo CD配置](#argo-cd配置)
- [安全扫描](#安全扫描)
- [常见问题](#常见问题)

## 🏗 架构概览

### CI/CD流程

```
代码提交 → GitHub Actions CI
    ↓
 编译测试 → SonarCloud质量扫描
    ↓
构建镜像 → Trivy安全扫描
    ↓
推送镜像 → Docker Hub
    ↓
更新GitOps → gitops分支
    ↓
Argo CD同步 → Kubernetes集群
```

### 技术栈

- **CI平台**: GitHub Actions
- **代码质量**: SonarCloud
- **镜像扫描**: Trivy + OWASP Dependency Check
- **镜像仓库**: Docker Hub
- **GitOps**: Argo CD
- **K8s配置**: Kustomize

## 🚀 快速开始

### 1. 前置条件

#### 必需工具
```bash
# Docker
docker --version  # >= 20.10

# kubectl
kubectl version --client  # >= 1.24

# Maven
mvn --version  # >= 3.8

# Git
git --version  # >= 2.30
```

#### 账号准备
- **GitHub账号**: 用于Actions和代码托管
- **Docker Hub账号**: 用于镜像存储
- **SonarCloud账号**: 用于代码质量分析
- **Kubernetes集群**: 用于应用部署

### 2. 配置GitHub Secrets

在GitHub仓库设置中配置以下Secrets：

```
Settings → Secrets and variables → Actions → New repository secret
```

必需的Secrets：

| Secret名称 | 说明 | 示例 |
|-----------|------|------|
| `DOCKER_USERNAME` | Docker Hub用户名 | `myusername` |
| `DOCKER_PASSWORD` | Docker Hub密码/Token | `dckr_pat_xxx` |
| `SONAR_TOKEN` | SonarCloud Token | `sqp_xxx` |

可选的Secrets（用于GitOps自动更新）：

| Secret名称 | 说明 |
|-----------|------|
| `GITOPS_TOKEN` | GitHub Personal Access Token (有repo权限) |

### 3. 配置SonarCloud

1. 访问 [SonarCloud](https://sonarcloud.io/)
2. 使用GitHub账号登录
3. 创建组织（如果还没有）
4. 导入basebackend仓库
5. 获取项目Key和Organization Key
6. 更新 `sonar-project.properties`:

```properties
sonar.projectKey=你的用户名_basebackend
sonar.organization=你的组织名
```

7. 在SonarCloud中生成Token，添加到GitHub Secrets

### 4. 初始化GitOps分支

```bash
# 创建gitops分支
git checkout -b gitops

# 将k8s配置推送到gitops分支
git add k8s/
git commit -m "feat: initialize GitOps configuration"
git push origin gitops

# 切回main分支
git checkout main
```

### 5. 构建和推送第一个镜像

```bash
# 方式1: 使用脚本（推荐）
./scripts/cicd/build-images.sh -u YOUR_DOCKER_USERNAME -v v1.0.0 -p admin-api

# 方式2: 手动触发GitHub Actions
# 在GitHub仓库页面: Actions → Build and Push Docker Images → Run workflow
```

### 6. 部署Argo CD

```bash
# 使用脚本自动部署
./scripts/cicd/deploy-argocd.sh

# 或手动部署
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 等待就绪
kubectl wait --for=condition=Ready pods --all -n argocd --timeout=300s

# 获取初始密码
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

### 7. 配置Argo CD应用

```bash
# 创建项目
kubectl apply -f k8s/argocd/project.yaml

# 部署dev环境
kubectl apply -f k8s/argocd/application-admin-api-dev.yaml

# 部署prod环境（可选）
kubectl apply -f k8s/argocd/application-admin-api-prod.yaml
```

## 🔧 GitHub Actions配置

### Workflows说明

#### 1. CI Pipeline (`ci.yml`)

触发条件：
- Push到main/develop分支
- Pull Request到main/develop
- 手动触发

功能：
- Maven编译和测试
- JaCoCo代码覆盖率
- OWASP依赖安全检查
- 构建所有服务

#### 2. SonarCloud Analysis (`sonarcloud.yml`)

触发条件：
- Push到main/develop分支
- Pull Request

功能：
- 代码质量分析
- 代码异味检测
- 安全漏洞扫描
- 质量门禁检查

#### 3. Build and Push (`build-and-push.yml`)

触发条件：
- Push到main分支
- 创建版本标签 (v*.*.*)
- 手动触发

功能：
- 构建Docker镜像
- Trivy镜像扫描
- 推送到Docker Hub
- 生成SBOM
- 自动更新GitOps配置

### 手动触发构建

```bash
# 方式1: GitHub Web UI
Actions → 选择Workflow → Run workflow

# 方式2: GitHub CLI
gh workflow run build-and-push.yml -f services="admin-api,gateway"
```

### 查看构建日志

```bash
# 使用GitHub CLI
gh run list
gh run view <run-id>
gh run view <run-id> --log
```

## 🐳 Docker镜像构建

### 本地构建

```bash
# 构建所有服务
./scripts/cicd/build-images.sh

# 构建指定服务
./scripts/cicd/build-images.sh admin-api gateway

# 构建并推送
./scripts/cicd/build-images.sh -u myusername -p admin-api

# 指定版本
./scripts/cicd/build-images.sh -v v1.2.3 admin-api

# 不使用缓存
./scripts/cicd/build-images.sh --no-cache admin-api
```

### Dockerfile说明

每个服务的Dockerfile采用多阶段构建：

1. **Builder阶段**:
   - 基于 `maven:3.9.5-eclipse-temurin-17`
   - 分层下载依赖（利用Docker缓存）
   - 编译Java代码
   - 提取JAR分层

2. **Runtime阶段**:
   - 基于 `eclipse-temurin:17-jre-alpine`
   - 非root用户运行
   - 健康检查配置
   - 优化的JVM参数

### 镜像标签策略

- `latest`: 最新的main分支构建
- `v1.2.3`: 语义化版本标签
- `main-abc1234`: 分支名-commit短SHA
- `pr-123`: PR编号

## 🔄 GitOps部署

### 目录结构

```
k8s/
├── base/                    # 基础配置
│   └── admin-api/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── configmap.yaml
│       ├── hpa.yaml
│       └── kustomization.yaml
├── overlays/               # 环境覆盖
│   ├── dev/
│   ├── test/
│   ├── staging/
│   └── prod/
└── argocd/                 # Argo CD配置
    ├── project.yaml
    ├── application-admin-api-dev.yaml
    └── application-admin-api-prod.yaml
```

### 环境配置

#### Dev环境特点
- 1个副本
- 较小的资源限制
- 自动同步
- 开启调试端口

#### Prod环境特点
- 3个副本
- 较大的资源限制
- 手动同步
- 严格的健康检查
- 同步窗口限制

### 更新部署

#### 自动更新（推荐）
```bash
# 1. 提交代码到main分支
git push origin main

# 2. GitHub Actions自动构建镜像

# 3. GitHub Actions自动更新gitops分支的镜像版本

# 4. Argo CD检测到变更，自动同步（dev环境）
```

#### 手动更新
```bash
# 切换到gitops分支
git checkout gitops

# 更新镜像版本
cd k8s/overlays/prod/admin-api
vim kustomization.yaml
# 修改 newTag: v1.2.3

# 提交变更
git add kustomization.yaml
git commit -m "chore: update admin-api to v1.2.3"
git push origin gitops

# 在Argo CD UI中手动同步（prod环境）
```

### 验证部署

```bash
# 查看Pod状态
kubectl get pods -n basebackend-dev

# 查看服务
kubectl get svc -n basebackend-dev

# 查看HPA
kubectl get hpa -n basebackend-dev

# 查看日志
kubectl logs -f deployment/dev-admin-api -n basebackend-dev
```

## 🛡 Argo CD配置

### 访问Argo CD

```bash
# 端口转发（本地访问）
kubectl port-forward svc/argocd-server -n argocd 8080:443

# 浏览器访问
# https://localhost:8080

# 获取密码
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d
```

### 使用Argo CD CLI

```bash
# 安装CLI
brew install argocd  # macOS
# 或下载二进制: https://argo-cd.readthedocs.io/en/stable/cli_installation/

# 登录
argocd login localhost:8080

# 列出应用
argocd app list

# 查看应用状态
argocd app get admin-api-dev

# 同步应用
argocd app sync admin-api-dev

# 查看同步历史
argocd app history admin-api-dev

# 回滚
argocd app rollback admin-api-dev <revision-id>
```

### 配置同步策略

#### 自动同步（Dev/Test）
```yaml
syncPolicy:
  automated:
    prune: true      # 自动删除
    selfHeal: true   # 自动修复
```

#### 手动同步（Staging/Prod）
```yaml
syncPolicy:
  automated:
    prune: false     # 不自动删除
    selfHeal: false  # 不自动修复
```

### 健康检查

Argo CD会自动检查：
- Pod状态
- Service端点
- Deployment副本数
- 自定义健康检查

### 通知配置

配置Slack/钉钉/企业微信通知：

```yaml
# 在Application中添加
metadata:
  annotations:
    notifications.argoproj.io/subscribe.on-sync-succeeded.slack: channel-name
    notifications.argoproj.io/subscribe.on-health-degraded.slack: channel-name
```

## 🔒 安全扫描

### SonarCloud质量扫描

查看报告：
```
https://sonarcloud.io/dashboard?id=wuan1250276415_basebackend
```

质量指标：
- 代码覆盖率 >= 80%
- 重复率 < 3%
- 可维护性评级 >= A
- 可靠性评级 >= A
- 安全性评级 >= A

### Trivy镜像扫描

本地扫描：
```bash
# 扫描镜像
trivy image basebackend-admin-api:latest

# 只显示高危和严重漏洞
trivy image --severity HIGH,CRITICAL basebackend-admin-api:latest

# 生成报告
trivy image --format json --output trivy-report.json basebackend-admin-api:latest
```

### OWASP依赖检查

本地运行：
```bash
# 执行依赖检查
mvn org.owasp:dependency-check-maven:aggregate

# 查看报告
open target/dependency-check-report/dependency-check-report.html
```

### 漏洞处理流程

1. **识别**: CI/CD自动扫描发现漏洞
2. **评估**: 安全团队评估影响和风险
3. **修复**:
   - 更新依赖版本
   - 应用安全补丁
   - 重新构建镜像
4. **验证**: 重新扫描确认修复
5. **部署**: 通过GitOps部署新版本

## 🐛 常见问题

### CI/CD相关

**Q: GitHub Actions构建失败**

```bash
# 检查日志
gh run view <run-id> --log

# 常见原因
1. Maven依赖下载失败 → 检查网络或使用国内镜像
2. 测试失败 → 本地运行测试: mvn test
3. Docker构建失败 → 检查Dockerfile语法
```

**Q: SonarCloud扫描失败**

```bash
# 检查配置
1. 确认SONAR_TOKEN正确
2. 检查sonar-project.properties配置
3. 确认组织名和项目Key匹配
```

**Q: 镜像推送失败**

```bash
# 检查Docker Hub凭证
1. 确认DOCKER_USERNAME和DOCKER_PASSWORD正确
2. 检查Docker Hub账号是否激活
3. 确认镜像名称格式正确
```

### Argo CD相关

**Q: 应用无法同步**

```bash
# 检查步骤
1. 查看应用状态
argocd app get <app-name>

2. 查看详细错误
kubectl describe application <app-name> -n argocd

3. 常见原因
- GitOps仓库路径错误
- Kubernetes RBAC权限不足
- 资源定义错误
```

**Q: 镜像拉取失败**

```bash
# 创建Docker Hub Secret
kubectl create secret docker-registry regcred \
  --docker-server=docker.io \
  --docker-username=<username> \
  --docker-password=<password> \
  -n basebackend-dev

# 在Deployment中引用
spec:
  imagePullSecrets:
    - name: regcred
```

**Q: Pod无法启动**

```bash
# 检查Pod日志
kubectl logs <pod-name> -n <namespace>

# 检查Pod事件
kubectl describe pod <pod-name> -n <namespace>

# 常见原因
1. 镜像不存在
2. 资源限制不足
3. 配置错误
4. 健康检查失败
```

### Kubernetes相关

**Q: 服务无法访问**

```bash
# 检查服务
kubectl get svc -n <namespace>

# 检查端点
kubectl get endpoints -n <namespace>

# 测试连通性
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://admin-api.basebackend-dev:8082/actuator/health
```

**Q: HPA不工作**

```bash
# 检查Metrics Server
kubectl get deployment metrics-server -n kube-system

# 如未安装
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 检查HPA状态
kubectl get hpa -n <namespace>
kubectl describe hpa <hpa-name> -n <namespace>
```

## 📚 参考资料

- [GitHub Actions文档](https://docs.github.com/en/actions)
- [SonarCloud文档](https://docs.sonarcloud.io/)
- [Trivy文档](https://aquasecurity.github.io/trivy/)
- [Argo CD文档](https://argo-cd.readthedocs.io/)
- [Kustomize文档](https://kubectl.docs.kubernetes.io/references/kustomize/)

## 🤝 贡献

如有问题或建议，请提交Issue或Pull Request。
