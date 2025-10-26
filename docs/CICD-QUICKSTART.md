# CI/CD 快速开始

本文档提供Base Backend项目CI/CD的快速配置指南，10分钟内即可完成基本配置。

## 🚀 快速配置（3步）

### Step 1: 配置GitHub Secrets（2分钟）

访问 GitHub仓库 → Settings → Secrets and variables → Actions

添加以下Secrets：

```
DOCKER_USERNAME=你的DockerHub用户名
DOCKER_PASSWORD=你的DockerHub密码或Token
SONAR_TOKEN=你的SonarCloud Token
```

#### 如何获取这些凭证？

**Docker Hub**:
1. 访问 https://hub.docker.com/
2. 注册/登录账号
3. Settings → Security → New Access Token
4. 复制生成的Token

**SonarCloud**:
1. 访问 https://sonarcloud.io/
2. 使用GitHub账号登录
3. My Account → Security → Generate Token
4. 复制Token
5. 导入basebackend项目

### Step 2: 初始化GitOps分支（3分钟）

```bash
# 克隆仓库
git clone https://github.com/wuan1250276415/basebackend.git
cd basebackend

# 创建gitops分支
git checkout -b gitops

# 推送分支
git push origin gitops

# 切回main分支
git checkout main
```

### Step 3: 触发首次构建（5分钟）

```bash
# 方式1: 推送代码触发自动构建
git commit --allow-empty -m "trigger: first CI/CD build"
git push origin main

# 方式2: 在GitHub UI手动触发
# Actions → Build and Push Docker Images → Run workflow
```

构建完成后，Docker Hub会有新镜像，GitOps分支会自动更新。

## 📋 完整流程演示

### 场景：部署admin-api到开发环境

#### 1. 本地构建镜像（可选）

```bash
# 使用脚本构建
./scripts/cicd/build-images.sh -u YOUR_USERNAME admin-api

# 手动构建
mvn clean package -pl basebackend-admin-api -am -DskipTests
docker build -t YOUR_USERNAME/basebackend-admin-api:test \
  -f basebackend-admin-api/Dockerfile .
```

#### 2. 推送代码触发CI/CD

```bash
# 提交代码
git add .
git commit -m "feat: update admin-api"
git push origin main

# GitHub Actions自动执行：
# ✓ 编译测试
# ✓ SonarCloud扫描
# ✓ 构建Docker镜像
# ✓ Trivy安全扫描
# ✓ 推送到Docker Hub
# ✓ 更新GitOps配置
```

#### 3. 部署到Kubernetes

```bash
# 方式A: 使用kind本地测试（推荐新手）
# 创建kind集群
kind create cluster --name basebackend

# 部署Argo CD
./scripts/cicd/deploy-argocd.sh

# 应用配置
kubectl apply -f k8s/argocd/project.yaml
kubectl apply -f k8s/argocd/application-admin-api-dev.yaml

# 等待部署完成
kubectl get pods -n basebackend-dev -w

# 方式B: 使用云K8s集群
# 参考 docs/K8S-SETUP.md 配置云环境
```

#### 4. 验证部署

```bash
# 查看Pod状态
kubectl get pods -n basebackend-dev

# 查看服务
kubectl get svc -n basebackend-dev

# 访问服务（端口转发）
kubectl port-forward svc/dev-admin-api -n basebackend-dev 8082:8082

# 测试接口
curl http://localhost:8082/actuator/health
```

## 🎯 核心概念速览

### CI/CD流程图

```
┌─────────────┐
│ 代码提交     │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│ GitHub Actions CI           │
│ • 编译测试                   │
│ • SonarCloud质量扫描         │
│ • OWASP依赖检查              │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ Docker镜像构建               │
│ • 多阶段构建                 │
│ • Trivy安全扫描              │
│ • 推送到Docker Hub           │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ GitOps自动更新               │
│ • 更新gitops分支             │
│ • 修改镜像版本               │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ Argo CD自动同步              │
│ • 检测配置变更               │
│ • 应用到K8s集群              │
│ • 健康检查                   │
└─────────────────────────────┘
```

### 关键文件说明

```
basebackend/
├── .github/workflows/          # GitHub Actions工作流
│   ├── ci.yml                 # 持续集成
│   ├── sonarcloud.yml         # 代码质量
│   └── build-and-push.yml     # 镜像构建
├── basebackend-*/Dockerfile   # 各服务的Dockerfile
├── k8s/                       # Kubernetes配置（在gitops分支）
│   ├── base/                  # 基础配置
│   ├── overlays/              # 环境覆盖
│   └── argocd/                # Argo CD配置
├── scripts/cicd/              # CI/CD脚本
│   ├── build-images.sh        # 构建镜像
│   └── deploy-argocd.sh       # 部署Argo CD
└── docs/                      # 文档
    ├── CI-CD-GUIDE.md         # 完整指南
    └── K8S-SETUP.md           # K8s搭建
```

## 🔍 检查清单

部署前检查：

- [ ] GitHub Secrets已配置（DOCKER_USERNAME, DOCKER_PASSWORD, SONAR_TOKEN）
- [ ] SonarCloud项目已创建并关联
- [ ] Docker Hub账号可用
- [ ] gitops分支已创建
- [ ] Kubernetes集群已就绪
- [ ] kubectl可以连接集群
- [ ] Argo CD已部署

首次部署检查：

- [ ] CI Pipeline执行成功
- [ ] SonarCloud扫描通过
- [ ] Docker镜像已推送
- [ ] GitOps配置已更新
- [ ] Argo CD应用已创建
- [ ] Pod运行正常
- [ ] 服务可以访问

## 🐛 快速问题排查

### 构建失败

```bash
# 查看GitHub Actions日志
# GitHub → Actions → 选择失败的workflow → 查看日志

# 常见问题：
# 1. Maven依赖下载失败 → 检查网络
# 2. 测试失败 → 本地运行: mvn test
# 3. Docker构建失败 → 检查Dockerfile
```

### 部署失败

```bash
# 查看Argo CD应用状态
kubectl get applications -n argocd
kubectl describe application admin-api-dev -n argocd

# 查看Pod状态
kubectl get pods -n basebackend-dev
kubectl describe pod <pod-name> -n basebackend-dev
kubectl logs <pod-name> -n basebackend-dev

# 常见问题：
# 1. 镜像拉取失败 → 检查镜像名称和凭证
# 2. 配置错误 → 检查ConfigMap和Secret
# 3. 资源不足 → 增加节点或调整资源请求
```

### Argo CD无法访问

```bash
# 检查Argo CD是否运行
kubectl get pods -n argocd

# 端口转发
kubectl port-forward svc/argocd-server -n argocd 8080:443

# 浏览器访问
# https://localhost:8080

# 获取密码
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d
```

## 📚 深入学习

完成快速开始后，建议阅读以下文档深入了解：

1. **[CI/CD完整指南](./CI-CD-GUIDE.md)** - 详细的CI/CD配置和最佳实践
2. **[K8s集群搭建](./K8S-SETUP.md)** - Kubernetes集群搭建详细步骤
3. **[GitHub Actions文档](https://docs.github.com/en/actions)** - 官方文档
4. **[Argo CD文档](https://argo-cd.readthedocs.io/)** - GitOps最佳实践

## 🎓 下一步建议

### 基础功能（必做）

- [x] 配置CI/CD基础流程
- [ ] 为其他服务（gateway, demo-api, file-service）配置部署
- [ ] 配置测试环境和生产环境
- [ ] 设置通知（Slack/钉钉/企业微信）

### 进阶功能（推荐）

- [ ] 配置蓝绿部署/金丝雀发布
- [ ] 集成性能测试
- [ ] 配置自动回滚
- [ ] 设置质量门禁
- [ ] 配置Secret管理（Sealed Secrets/External Secrets）

### 生产准备（生产环境必做）

- [ ] 配置生产环境同步窗口
- [ ] 设置资源配额和限制
- [ ] 配置备份策略
- [ ] 设置监控告警
- [ ] 编写灾难恢复计划

## 🆘 获取帮助

遇到问题？

1. 查看 [常见问题](./CI-CD-GUIDE.md#常见问题)
2. 搜索 [GitHub Issues](https://github.com/wuan1250276415/basebackend/issues)
3. 创建新的Issue描述问题

## 🤝 贡献

欢迎提交Issue和Pull Request改进CI/CD流程！
