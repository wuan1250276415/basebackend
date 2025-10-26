# GitHub Secrets 配置指南

完整的GitHub Secrets配置说明，确保CI/CD流程正常运行。

## 📋 必需的Secrets

| Secret名称 | 用途 | 如何获取 |
|-----------|------|----------|
| DOCKER_USERNAME | Docker Hub登录用户名 | Docker Hub账号用户名 |
| DOCKER_PASSWORD | Docker Hub登录密码 | Docker Hub Access Token（推荐）或密码 |
| SONAR_TOKEN | SonarCloud认证Token | SonarCloud个人设置生成 |

## 🔐 详细配置步骤

### 1. 配置Docker Hub凭证

#### 获取Docker Hub凭证

**方式A: 使用Access Token（推荐）**

1. 访问 https://hub.docker.com/
2. 登录账号
3. 点击右上角头像 → Account Settings
4. Security → New Access Token
5. 填写描述（如：basebackend-ci）
6. 权限选择 "Read, Write, Delete"
7. 点击 Generate
8. **立即复制Token**（仅显示一次）

**方式B: 使用密码**

直接使用Docker Hub登录密码（不推荐，安全性较低）

#### 添加到GitHub

1. 打开GitHub仓库
2. Settings → Secrets and variables → Actions
3. 点击 "New repository secret"
4. 添加两个Secrets：

```
Name: DOCKER_USERNAME
Secret: 你的Docker Hub用户名

Name: DOCKER_PASSWORD
Secret: 你的Access Token或密码
```

#### 验证

```bash
# 在GitHub Actions中测试登录
echo "${{ secrets.DOCKER_PASSWORD }}" | \
  docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
```

### 2. 配置SonarCloud Token

#### 获取SonarCloud Token

1. 访问 https://sonarcloud.io/
2. 使用GitHub账号登录
3. 点击右上角头像 → My Account
4. Security标签页
5. Generate Tokens
   - Name: basebackend-ci
   - Type: User Token
   - Expires in: No expiration（或选择期限）
6. 点击 Generate
7. **立即复制Token**（以sqp_开头）

#### 配置SonarCloud项目

在生成Token之前，需要先导入项目：

1. SonarCloud主页 → "+" → Analyze new project
2. 选择 basebackend 仓库
3. 选择免费方案（Free plan）
4. Set Up
5. 记录显示的：
   - Organization Key（组织名）
   - Project Key（项目Key）

#### 更新项目配置

编辑 `sonar-project.properties`:

```properties
sonar.projectKey=你的Organization_basebackend
sonar.organization=你的Organization名称
```

#### 添加到GitHub

```
Name: SONAR_TOKEN
Secret: 你的SonarCloud Token (sqp_xxx...)
```

#### 验证

在GitHub Actions中会自动使用，查看SonarCloud workflow的运行日志。

### 3. 配置GitOps Token（可选）

用于GitHub Actions自动更新gitops分支的镜像版本。

#### 获取GitHub Personal Access Token

1. GitHub → Settings（个人设置，非仓库设置）
2. Developer settings → Personal access tokens → Tokens (classic)
3. Generate new token → Generate new token (classic)
4. 设置：
   - Note: basebackend-gitops
   - Expiration: 90 days（或更长）
   - 权限勾选：
     - ✅ repo（所有子选项）
5. Generate token
6. **立即复制Token**

#### 添加到GitHub

```
Name: GITOPS_TOKEN
Secret: 你的GitHub Personal Access Token
```

#### 在Workflow中使用

```yaml
- name: Checkout gitops branch
  uses: actions/checkout@v4
  with:
    ref: gitops
    token: ${{ secrets.GITOPS_TOKEN }}

- name: Commit changes
  run: |
    git config user.name "github-actions[bot]"
    git config user.email "github-actions[bot]@users.noreply.github.com"
    git add .
    git commit -m "Update image version"
    git push
```

## 🛡 安全最佳实践

### 1. 使用Access Token而非密码

❌ 不推荐：
```
DOCKER_PASSWORD=my_actual_password
```

✅ 推荐：
```
DOCKER_PASSWORD=dckr_pat_xxxxxxxxxxxxx
```

### 2. 设置Token过期时间

- Docker Hub Token: 建议每6个月轮换
- SonarCloud Token: 建议每3个月轮换
- GitHub Token: 建议每90天轮换

### 3. 使用最小权限原则

每个Token只授予必需的最小权限：

- Docker Hub: Read, Write（不需要Delete）
- SonarCloud: 只需要项目分析权限
- GitHub: 只需要repo权限

### 4. 定期审计Secrets

```bash
# 检查哪些workflow使用了哪些secrets
grep -r "secrets\." .github/workflows/
```

### 5. 不要在日志中打印Secrets

❌ 危险：
```yaml
- name: Debug
  run: echo "Token is ${{ secrets.SONAR_TOKEN }}"
```

✅ 安全：
```yaml
- name: Debug
  run: |
    if [ -n "${{ secrets.SONAR_TOKEN }}" ]; then
      echo "Token is set"
    else
      echo "Token is missing"
    fi
```

## 🔄 轮换Secrets

### 什么时候需要轮换？

- 定期轮换（建议每90天）
- Token泄露或可能泄露时
- 团队成员离职时
- 权限变更时

### 轮换步骤

1. **生成新Token**
   - 在相应平台生成新Token
   - 不要立即删除旧Token

2. **更新GitHub Secrets**
   - Settings → Secrets → 编辑对应Secret
   - 粘贴新Token
   - Save

3. **验证新Token**
   - 手动触发一次workflow
   - 确认所有job都成功

4. **删除旧Token**
   - 确认新Token工作正常后
   - 在原平台删除旧Token

## ✅ 配置验证

### 检查清单

```bash
# 1. 检查Secrets是否已配置
gh secret list

# 2. 触发测试workflow
gh workflow run ci.yml

# 3. 查看运行状态
gh run list --workflow=ci.yml

# 4. 检查具体job
gh run view <run-id> --log
```

### 验证脚本

在仓库根目录创建 `.github/workflows/test-secrets.yml`:

```yaml
name: Test Secrets Configuration

on:
  workflow_dispatch:

jobs:
  test-secrets:
    name: Verify Secrets
    runs-on: ubuntu-latest
    steps:
      - name: Check Docker credentials
        run: |
          if [ -n "${{ secrets.DOCKER_USERNAME }}" ]; then
            echo "✅ DOCKER_USERNAME is set"
          else
            echo "❌ DOCKER_USERNAME is missing"
            exit 1
          fi

          if [ -n "${{ secrets.DOCKER_PASSWORD }}" ]; then
            echo "✅ DOCKER_PASSWORD is set"
          else
            echo "❌ DOCKER_PASSWORD is missing"
            exit 1
          fi

      - name: Check SonarCloud token
        run: |
          if [ -n "${{ secrets.SONAR_TOKEN }}" ]; then
            echo "✅ SONAR_TOKEN is set"
          else
            echo "❌ SONAR_TOKEN is missing"
            exit 1
          fi

      - name: Test Docker login
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | \
            docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
          echo "✅ Docker login successful"
          docker logout

      - name: Summary
        run: |
          echo "🎉 All secrets are correctly configured!"
```

手动运行这个workflow来验证配置。

## 🐛 常见问题

### Q: Docker login失败

```
Error: Cannot perform an interactive login from a non TTY device
```

**解决方案**:
```yaml
# 使用 --password-stdin
echo "${{ secrets.DOCKER_PASSWORD }}" | \
  docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
```

### Q: SonarCloud认证失败

```
Error: Not authorized. Please check the properties sonar.login
```

**检查步骤**:
1. Token是否正确（以sqp_开头）
2. Token是否过期
3. Organization和Project Key是否匹配

### Q: Git push失败（更新gitops分支）

```
Error: Permission denied
```

**解决方案**:
1. 确认GITOPS_TOKEN已配置
2. 确认Token有repo权限
3. 在checkout时使用Token:
```yaml
- uses: actions/checkout@v4
  with:
    token: ${{ secrets.GITOPS_TOKEN }}
```

### Q: Secret更新不生效

**解决方案**:
```bash
# 1. 确认Secret已更新
gh secret list

# 2. 触发新的workflow运行
# （更新Secret不会自动重新运行现有的workflow）
gh workflow run ci.yml

# 3. 检查新运行的日志
gh run list
```

## 📚 参考资料

- [GitHub Secrets文档](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Docker Hub Access Tokens](https://docs.docker.com/docker-hub/access-tokens/)
- [SonarCloud Token文档](https://docs.sonarcloud.io/advanced-setup/user-accounts/)

## 🆘 需要帮助？

如果配置过程中遇到问题：

1. 检查本文档的常见问题部分
2. 查看GitHub Actions运行日志
3. 在仓库提交Issue，附上：
   - 错误信息
   - Workflow运行日志
   - 已尝试的解决方案
