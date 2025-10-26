# CI/CD 文件结构说明

本文档说明CI/CD相关文件的组织结构和用途。

## 📁 目录结构

\`\`\`
basebackend/
├── .github/
│   └── workflows/              # GitHub Actions工作流
│       ├── ci.yml             # 持续集成（编译、测试、代码检查）
│       ├── sonarcloud.yml     # SonarCloud代码质量扫描
│       └── build-and-push.yml # Docker镜像构建和推送
│
├── basebackend-gateway/
│   └── Dockerfile             # Gateway服务Docker镜像
│
├── basebackend-admin-api/
│   └── Dockerfile             # Admin API服务Docker镜像
│
├── basebackend-demo-api/
│   └── Dockerfile             # Demo API服务Docker镜像
│
├── basebackend-file-service/
│   └── Dockerfile             # File Service服务Docker镜像
│
├── k8s/                       # Kubernetes配置（在gitops分支）
│   ├── base/                  # 基础配置（所有环境共享）
│   │   ├── gateway/
│   │   ├── admin-api/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   ├── configmap.yaml
│   │   │   ├── hpa.yaml
│   │   │   └── kustomization.yaml
│   │   ├── demo-api/
│   │   └── file-service/
│   │
│   ├── overlays/              # 环境特定配置
│   │   ├── dev/               # 开发环境
│   │   │   └── admin-api/
│   │   │       ├── kustomization.yaml
│   │   │       └── deployment-patch.yaml
│   │   ├── test/              # 测试环境
│   │   ├── staging/           # 预发布环境
│   │   └── prod/              # 生产环境
│   │       └── admin-api/
│   │           ├── kustomization.yaml
│   │           └── deployment-patch.yaml
│   │
│   └── argocd/                # Argo CD配置
│       ├── project.yaml       # Argo CD项目定义
│       ├── application-admin-api-dev.yaml
│       └── application-admin-api-prod.yaml
│
├── scripts/
│   └── cicd/                  # CI/CD辅助脚本
│       ├── build-images.sh    # 本地构建Docker镜像
│       └── deploy-argocd.sh   # 部署Argo CD到K8s集群
│
├── docs/                      # CI/CD文档
│   ├── CICD-QUICKSTART.md     # 快速开始指南
│   ├── CI-CD-GUIDE.md         # 完整CI/CD指南
│   ├── K8S-SETUP.md           # Kubernetes集群搭建
│   ├── GITHUB-SECRETS-SETUP.md # GitHub Secrets配置
│   └── CICD-IMPLEMENTATION-SUMMARY.md # 实施总结
│
├── .dockerignore              # Docker构建忽略文件
├── .trivy.yaml                # Trivy镜像扫描配置
├── .trivyignore               # Trivy忽略规则
├── sonar-project.properties   # SonarCloud项目配置
└── pom.xml                    # 添加了JaCoCo和SonarQube插件
\`\`\`

## 📄 文件说明

### GitHub Actions Workflows

#### `.github/workflows/ci.yml`
**用途**: 持续集成流水线

**触发条件**:
- Push到main/develop分支
- Pull Request
- 手动触发

**功能**:
- Maven编译和单元测试
- JaCoCo代码覆盖率生成
- OWASP依赖安全检查
- 构建所有服务
- 发布测试报告

#### `.github/workflows/sonarcloud.yml`
**用途**: 代码质量分析

**触发条件**:
- Push到main/develop分支
- Pull Request

**功能**:
- 代码质量扫描
- 代码异味检测
- 安全漏洞分析
- 质量门禁检查
- PR自动评论

#### `.github/workflows/build-and-push.yml`
**用途**: 构建和发布Docker镜像

**触发条件**:
- Push到main分支
- 创建版本标签 (v*.*.*)
- 手动触发

**功能**:
- 构建Docker镜像
- Trivy安全扫描
- 推送到Docker Hub
- 生成SBOM
- 自动更新GitOps配置

### Dockerfile

每个微服务都有独立的Dockerfile，特点：

- **多阶段构建**: Builder阶段 + Runtime阶段
- **基础镜像**: maven:3.9-temurin-17 (构建) + temurin:17-jre-alpine (运行)
- **优化**: 分层缓存、最小化镜像体积
- **安全**: 非root用户运行
- **健康检查**: 内置健康检查配置

### Kubernetes配置

#### `k8s/base/`
基础配置，所有环境共享：

- **deployment.yaml**: Pod副本、容器配置、资源限制
- **service.yaml**: 服务暴露配置
- **configmap.yaml**: 应用配置文件
- **hpa.yaml**: 水平自动扩缩容配置
- **kustomization.yaml**: Kustomize基础配置

#### `k8s/overlays/<env>/`
环境特定配置：

- **kustomization.yaml**: 环境配置覆盖
- **deployment-patch.yaml**: 部署配置补丁
- **Secrets**: 环境变量和敏感信息

环境差异：

| 配置项 | Dev | Test | Staging | Prod |
|--------|-----|------|---------|------|
| 副本数 | 1 | 2 | 2 | 3 |
| CPU请求 | 100m | 250m | 500m | 500m |
| 内存请求 | 256Mi | 512Mi | 1Gi | 1Gi |
| 同步策略 | 自动 | 自动 | 手动 | 手动 |

#### `k8s/argocd/`
Argo CD应用定义：

- **project.yaml**: 项目级配置、RBAC、同步窗口
- **application-*.yaml**: 具体应用配置
  - 源仓库路径
  - 目标集群和命名空间
  - 同步策略
  - 健康检查规则

### 配置文件

#### `sonar-project.properties`
SonarCloud项目配置：

- 项目Key和Organization
- 源代码路径
- 测试路径
- 排除规则
- 多模块配置
- 覆盖率报告路径

#### `.trivy.yaml`
Trivy镜像扫描配置：

- 扫描类型（漏洞、配置、密钥）
- 严重性级别（CRITICAL, HIGH, MEDIUM）
- 输出格式
- 超时设置

#### `.dockerignore`
Docker构建忽略文件：

- 源代码控制文件(.git, .gitignore)
- IDE配置(.idea, .vscode)
- 构建输出(target/)
- 文档(docs/, *.md)
- CI/CD配置

### 脚本

#### `scripts/cicd/build-images.sh`
本地Docker镜像构建脚本

**功能**:
- 支持构建单个或多个服务
- 自动Maven编译
- Docker镜像构建
- 可选推送到仓库
- 版本标签管理

**用法**:
\`\`\`bash
./scripts/cicd/build-images.sh [选项] [服务...]

# 示例
./scripts/cicd/build-images.sh admin-api
./scripts/cicd/build-images.sh -u myuser -p -v v1.0.0 admin-api
\`\`\`

#### `scripts/cicd/deploy-argocd.sh`
Argo CD部署脚本

**功能**:
- 创建argocd命名空间
- 安装Argo CD
- 配置服务访问
- 获取初始密码
- 应用AppProject

**用法**:
\`\`\`bash
./scripts/cicd/deploy-argocd.sh
\`\`\`

### 文档

#### `docs/CICD-QUICKSTART.md`
快速开始指南（10分钟配置）

内容：
- 3步快速配置
- 完整流程演示
- 核心概念速览
- 检查清单
- 快速问题排查

#### `docs/CI-CD-GUIDE.md`
完整CI/CD指南（90+页）

内容：
- 架构概览
- 详细配置步骤
- GitHub Actions说明
- Docker镜像构建
- GitOps部署
- Argo CD配置
- 安全扫描
- 常见问题

#### `docs/K8S-SETUP.md`
Kubernetes集群搭建指南

内容：
- 方案选择对比
- 本地开发环境（kind/minikube）
- 云环境部署（EKS/ACK/k3s）
- 集群初始化
- 必需组件安装
- 安全加固

#### `docs/GITHUB-SECRETS-SETUP.md`
GitHub Secrets配置指南

内容：
- 必需Secrets说明
- 详细获取步骤
- 安全最佳实践
- 轮换策略
- 配置验证
- 常见问题

## 🔄 工作流程

### 开发流程

1. **开发者提交代码**
   \`\`\`bash
   git add .
   git commit -m "feat: new feature"
   git push origin feature/new-feature
   \`\`\`

2. **自动触发CI检查**
   - PR触发: ci.yml, sonarcloud.yml
   - 运行测试和质量检查
   - 在PR中显示结果

3. **合并到main分支**
   \`\`\`bash
   # 通过PR合并
   git checkout main
   git pull origin main
   \`\`\`

4. **自动构建和部署**
   - 触发: build-and-push.yml
   - 构建Docker镜像
   - 推送到Docker Hub
   - 更新gitops分支
   - Argo CD自动同步（Dev环境）

### 发布流程

1. **创建版本标签**
   \`\`\`bash
   git tag -a v1.2.3 -m "Release v1.2.3"
   git push origin v1.2.3
   \`\`\`

2. **自动构建镜像**
   - 构建带版本标签的镜像
   - 推送到Docker Hub

3. **手动部署到生产**
   - 在Argo CD UI中手动同步
   - 或使用CLI: \`argocd app sync admin-api-prod\`

## 📊 配置管理

### 镜像版本管理

镜像标签策略：

- \`latest\` - 最新的main分支构建
- \`v1.2.3\` - 语义化版本
- \`main-abc1234\` - 分支-commit短SHA
- \`pr-123\` - PR编号

### Secret管理

Secret存储位置：

1. **GitHub Secrets**: CI/CD凭证
   - DOCKER_USERNAME
   - DOCKER_PASSWORD
   - SONAR_TOKEN
   - GITOPS_TOKEN

2. **Kubernetes Secrets**: 应用凭证
   - 数据库密码
   - Redis密码
   - JWT密钥
   - 第三方API密钥

3. **生产环境**: 建议使用
   - Sealed Secrets
   - External Secrets Operator
   - HashiCorp Vault

## 🔍 监控和日志

### CI/CD监控

- **GitHub Actions**: https://github.com/<user>/<repo>/actions
- **SonarCloud**: https://sonarcloud.io/
- **Docker Hub**: https://hub.docker.com/

### Kubernetes监控

- **Argo CD UI**: 应用健康状态
- **Kubectl**: \`kubectl get pods -n <namespace>\`
- **Prometheus + Grafana**: 指标监控（可选）

## 🐛 故障排查

### 构建失败

\`\`\`bash
# 查看GitHub Actions日志
gh run list
gh run view <run-id> --log
\`\`\`

### 部署失败

\`\`\`bash
# 查看Argo CD应用状态
argocd app get <app-name>

# 查看Pod日志
kubectl logs -f <pod-name> -n <namespace>

# 查看Pod事件
kubectl describe pod <pod-name> -n <namespace>
\`\`\`

## 📚 相关资源

- [GitHub Actions文档](https://docs.github.com/en/actions)
- [Docker文档](https://docs.docker.com/)
- [Kubernetes文档](https://kubernetes.io/docs/)
- [Argo CD文档](https://argo-cd.readthedocs.io/)
- [Kustomize文档](https://kubectl.docs.kubernetes.io/references/kustomize/)

---

**维护**: 此文档应随着CI/CD配置变更及时更新
**版本**: 1.0.0
**最后更新**: 2025-10-23
