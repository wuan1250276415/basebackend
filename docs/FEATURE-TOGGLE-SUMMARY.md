# Feature Toggle (特性开关) 集成总结

## ✅ 集成完成

BaseBackend项目已成功集成Unleash和Flagsmith两大特性开关平台，提供统一抽象接口，支持灵活切换。

## 📦 核心组件

### 1️⃣ basebackend-feature-toggle模块

**新建模块**，包含：

#### 核心接口
- `FeatureToggleService` - 统一特性开关服务接口
- `FeatureContext` - 特性开关上下文（用户信息、环境等）
- `Variant` - 变体模型（用于AB测试）

#### 实现类
- `UnleashFeatureToggleService` - Unleash实现
- `FlagsmithFeatureToggleService` - Flagsmith实现
- `CompositeFeatureToggleService` - 组合服务（同时支持两者）

#### 配置类
- `FeatureToggleProperties` - 配置属性
- `FeatureToggleAutoConfiguration` - 自动配置类

#### 注解
- `@EnableFeatureToggle` - 启用特性开关
- `@FeatureToggle` - 方法级开关
- `@GradualRollout` - 灰度发布
- `@ABTest` - AB测试

#### AOP切面
- `FeatureToggleAspect` - 实现注解驱动的特性控制

### 2️⃣ Maven配置

**父POM (pom.xml)**:
```xml
<unleash.version>9.2.2</unleash.version>
<flagsmith.version>7.2.0</flagsmith.version>
```

### 3️⃣ 配置文件

**application-feature-toggle.yml**:
```yaml
feature-toggle:
  enabled: false  # 默认禁用
  provider: UNLEASH  # 或 FLAGSMITH 或 BOTH
  primary-provider: UNLEASH  # 当provider=BOTH时的主提供商

  unleash:
    url: http://localhost:4242/api
    api-token: ${UNLEASH_API_TOKEN:}
    app-name: basebackend
    environment: ${SPRING_PROFILES_ACTIVE:development}

  flagsmith:
    url: https://edge.api.flagsmith.com/api/v1/
    api-key: ${FLAGSMITH_API_KEY:}
```

### 4️⃣ Docker Compose部署

**docker-compose-feature-toggle.yml**:
- Unleash服务 + PostgreSQL
- Flagsmith服务 + PostgreSQL
- 启动脚本: `./scripts/start-feature-toggle.sh`

**访问地址**:
- Unleash: http://localhost:4242 (admin/unleash4all)
- Flagsmith: http://localhost:8000

### 5️⃣ 业务服务集成

已集成到 **basebackend-admin-api**:
```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-feature-toggle</artifactId>
</dependency>
```

其他服务（Gateway、Demo API、File Service）按照相同方式集成。

## 🚀 快速开始

### 1. 启动Feature Toggle服务

```bash
# 启动Unleash和Flagsmith
./scripts/start-feature-toggle.sh

# 等待服务就绪后访问：
# Unleash: http://localhost:4242
# Flagsmith: http://localhost:8000
```

### 2. 配置API Token

**Unleash**:
1. 登录 http://localhost:4242 (admin/unleash4all)
2. 进入 Project Settings → API Access
3. 创建Client API Token
4. 复制Token到配置文件

**Flagsmith**:
1. 访问 http://localhost:8000
2. 首次访问创建管理员账户
3. 创建项目和环境
4. 进入 Environment Settings → API Keys
5. 复制Environment Key到配置文件

### 3. 启用特性开关

在 `application.yml` 或 `application-dev.yml` 中:

```yaml
feature-toggle:
  enabled: true
  provider: UNLEASH  # 或 FLAGSMITH 或 BOTH
  unleash:
    api-token: your-unleash-client-api-token-here
  flagsmith:
    api-key: your-flagsmith-environment-key-here
```

### 4. 代码中使用

#### 方式1: 直接调用服务

```java
@Service
public class UserService {

    @Autowired
    private FeatureToggleService featureToggleService;

    public void someMethod() {
        // 简单检查
        if (featureToggleService.isEnabled("new-user-registration")) {
            // 新功能代码
        }

        // 带用户上下文
        FeatureContext context = FeatureContext.forUser(userId, username, email);
        if (featureToggleService.isEnabled("premium-features", context)) {
            // VIP功能
        }

        // AB测试
        Variant variant = featureToggleService.getVariant("checkout-flow", context);
        if ("variant-a".equals(variant.getName())) {
            // 使用A版本
        } else {
            // 使用B版本
        }
    }
}
```

#### 方式2: 使用注解

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 简单开关
    @FeatureToggle(value = "new-api-endpoint", throwException = true)
    @GetMapping("/new-feature")
    public ResponseEntity<?> newFeature() {
        return ResponseEntity.ok("New feature enabled!");
    }

    // 灰度发布（10%用户）
    @GradualRollout(value = "gradual-feature", percentage = 10)
    @GetMapping("/gradual")
    public ResponseEntity<?> gradualFeature() {
        return ResponseEntity.ok("Gradual rollout feature!");
    }

    // AB测试
    @ABTest(value = "checkout-experiment", track = true)
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout() {
        return ResponseEntity.ok("Checkout completed!");
    }
}
```

## 🎯 核心功能

### 1. 基础特性开关
- 简单的开/关控制
- 支持默认值（服务不可用时）
- 支持用户上下文

### 2. 灰度发布
- 百分比渐进式发布
- 用户粘性（同一用户始终得到相同结果）
- 支持基于用户属性的分组

### 3. 用户分组
```java
FeatureContext context = FeatureContext.builder()
    .userId(userId)
    .addProperty("role", "VIP")
    .addProperty("region", "CN")
    .build();

// Unleash/Flagsmith会根据配置的策略判断
boolean enabled = featureToggleService.isEnabled("vip-feature", context);
```

### 4. AB测试
```java
Variant variant = featureToggleService.getVariant("experiment-name", context);
String variantName = variant.getName(); // "control", "variant-a", "variant-b"
String payload = variant.getPayload();  // 可选的配置数据
```

## 📊 监控与管理

### 查看所有特性状态

```java
Map<String, Boolean> states = featureToggleService.getAllFeatureStates();
states.forEach((name, enabled) -> {
    log.info("Feature {}: {}", name, enabled ? "ENABLED" : "DISABLED");
});
```

### 刷新配置

```java
featureToggleService.refresh();
```

### 检查服务可用性

```java
if (featureToggleService.isAvailable()) {
    log.info("Provider: {}", featureToggleService.getProviderName());
}
```

## 🔧 多环境配置

### 开发环境 (dev)
```yaml
feature-toggle:
  enabled: true
  provider: UNLEASH
  unleash:
    url: http://localhost:4242/api
    environment: development
```

### 测试环境 (test)
```yaml
feature-toggle:
  enabled: true
  provider: BOTH  # 同时测试两个平台
  primary-provider: UNLEASH
```

### 生产环境 (prod)
```yaml
feature-toggle:
  enabled: true
  provider: UNLEASH
  unleash:
    url: https://unleash.your-company.com/api
    api-token: ${UNLEASH_API_TOKEN}  # 从环境变量读取
    environment: production
```

## 🐳 Docker部署

```bash
# 启动服务
./scripts/start-feature-toggle.sh

# 查看日志
docker-compose -f docker-compose-feature-toggle.yml logs -f

# 停止服务
docker-compose -f docker-compose-feature-toggle.yml down

# 停止并删除数据
docker-compose -f docker-compose-feature-toggle.yml down -v
```

## ☸️ Kubernetes部署

### Unleash部署

```bash
# 1. 创建命名空间
kubectl create namespace feature-toggle

# 2. 部署PostgreSQL
kubectl apply -f k8s/base/unleash/postgres.yaml

# 3. 部署Unleash
kubectl apply -f k8s/base/unleash/deployment.yaml

# 4. 创建Service和Ingress
kubectl apply -f k8s/base/unleash/service.yaml
kubectl apply -f k8s/base/unleash/ingress.yaml
```

### Flagsmith部署

```bash
# 类似Unleash的部署流程
kubectl apply -f k8s/base/flagsmith/
```

## 🔒 安全最佳实践

### 1. API Token管理

**不要在代码中硬编码Token**:
```yaml
# ❌ 错误
unleash:
  api-token: your-token-12345

# ✅ 正确
unleash:
  api-token: ${UNLEASH_API_TOKEN}
```

使用环境变量或Secret管理：
```bash
export UNLEASH_API_TOKEN="your-secret-token"
export FLAGSMITH_API_KEY="your-secret-key"
```

Kubernetes Secret:
```bash
kubectl create secret generic feature-toggle-secrets \
  --from-literal=unleash-token=your-token \
  --from-literal=flagsmith-key=your-key \
  -n basebackend
```

### 2. 权限控制

- 仅授权服务账号只读权限
- 生产环境使用独立的API Token
- 定期轮换Token

### 3. 降级策略

始终提供默认值：
```java
boolean enabled = featureToggleService.isEnabled("feature", context, false);
```

## 📋 文件清单

### Java代码 (basebackend-feature-toggle/)
- `FeatureToggleService.java` - 统一接口
- `UnleashFeatureToggleService.java` - Unleash实现
- `FlagsmithFeatureToggleService.java` - Flagsmith实现
- `CompositeFeatureToggleService.java` - 组合服务
- `FeatureContext.java` - 上下文模型
- `Variant.java` - 变体模型
- `FeatureToggleProperties.java` - 配置属性
- `FeatureToggleAutoConfiguration.java` - 自动配置
- `@FeatureToggle.java` - 特性开关注解
- `@GradualRollout.java` - 灰度发布注解
- `@ABTest.java` - AB测试注解
- `@EnableFeatureToggle.java` - 启用注解
- `FeatureToggleAspect.java` - AOP切面
- `FeatureNotEnabledException.java` - 异常类

### 配置文件
- `basebackend-feature-toggle/pom.xml` - 模块POM
- `pom.xml` - 父POM（已更新）
- `application-feature-toggle.yml` - 配置模板
- `basebackend-admin-api/pom.xml` - 已集成
- `basebackend-admin-api/application.yml` - 已启用profile

### Docker配置
- `docker-compose-feature-toggle.yml` - Docker Compose配置
- `scripts/start-feature-toggle.sh` - 启动脚本

### Kubernetes配置
- `k8s/base/unleash/` - Unleash K8s配置
- `k8s/base/flagsmith/` - Flagsmith K8s配置

### 文档
- `docs/FEATURE-TOGGLE-SUMMARY.md` - 本文档

## 🔗 相关资源

- [Unleash官方文档](https://docs.getunleash.io/)
- [Flagsmith官方文档](https://docs.flagsmith.com/)
- [Feature Toggle最佳实践](https://martinfowler.com/articles/feature-toggles.html)

## ⚠️ 注意事项

1. **不要过度使用特性开关**
   - 及时清理已发布的特性开关
   - 避免代码中积累大量废弃的开关

2. **测试所有分支**
   - 测试特性启用和禁用两种情况
   - 测试降级场景

3. **监控特性使用情况**
   - 定期查看特性开关的使用率
   - 分析AB测试数据

4. **文档化特性开关**
   - 记录每个特性开关的用途
   - 标注计划的清理时间

## 🎉 下一步

1. **其他服务集成**
   - Gateway: 路由控制、限流策略
   - Demo API: 功能演示
   - File Service: 存储策略切换

2. **创建更多示例**
   - 实际业务场景的特性开关
   - 灰度发布流程
   - AB测试实验

3. **监控集成**
   - 集成到Prometheus/Grafana
   - 特性开关使用率仪表板

4. **编写详细文档**
   - 完整使用指南
   - 最佳实践文档
   - K8s部署指南

---

**集成完成！** 🎉

如需详细的使用文档和最佳实践，请参考后续创建的完整文档。
