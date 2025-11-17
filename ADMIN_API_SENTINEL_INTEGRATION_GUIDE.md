# Admin-API Sentinel 集成使用指南

> 为 Admin-API 服务添加 Sentinel 流控、熔断、降级保护 🛡️

**完成日期：** 2025-01-13
**完成人：** 浮浮酱（猫娘工程师）
**状态：** ✅ 已完成

---

## 📋 目录

- [概述](#概述)
- [完成内容](#完成内容)
- [集成说明](#集成说明)
- [资源配置](#资源配置)
- [使用指南](#使用指南)
- [测试验证](#测试验证)
- [常见问题](#常见问题)
- [进阶配置](#进阶配置)

---

## 概述

### 项目背景

在完成 Phase 4 - 服务治理增强后，浮浮酱继续执行短期计划，为 Admin-API 服务集成 Sentinel，实现细粒度的流控、熔断、降级保护喵～

### 核心目标

✅ **添加 Sentinel 依赖** - 为 admin-api 模块添加 Sentinel 相关依赖
✅ **配置数据源** - 配置 Nacos 作为 Sentinel 规则持久化数据源
✅ **应用注解** - 在关键业务方法上添加 @SentinelResource 注解
✅ **统一处理** - 创建统一的降级和异常处理器
✅ **规则配置** - 为所有资源配置流控、熔断、降级规则

### 实施成果

| 指标 | 完成情况 |
|------|----------|
| **依赖管理** | ✅ 100% |
| **配置数据源** | ✅ 100% |
| **注解应用** | ✅ 100% |
| **统一处理器** | ✅ 100% |
| **规则配置** | ✅ 100% |
| **文档完善** | ✅ 100% |

---

## 完成内容

### 1. 依赖管理 ✅

**文件：** `basebackend-admin-api/pom.xml`

**新增依赖：**
```xml
<!-- Sentinel 流控、熔断、降级 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
</dependency>

<!-- Sentinel 注解支持 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-annotation-aspectj</artifactId>
</dependency>

<!-- Sentinel Nacos 数据源（规则持久化） -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>

<!-- Sentinel Dashboard 通信 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-transport-simple-http</artifactId>
</dependency>

<!-- Sentinel 热点参数限流 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-parameter-flow-control</artifactId>
</dependency>
```

### 2. 配置文件 ✅

#### 2.1 Sentinel 配置

**文件：** `basebackend-admin-api/src/main/resources/application-sentinel.yml`

```yaml
spring:
  cloud:
    sentinel:
      # 启用 Sentinel
      enabled: true

      # 心跳发送周期
      eager: true

      # Sentinel Dashboard 配置
      transport:
        dashboard: ${SENTINEL_DASHBOARD:localhost:8858}
        port: 8719

      # Nacos 数据源配置（规则持久化）
      datasource:
        # 流控规则
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-flow-rules
            groupId: SENTINEL_GROUP
            rule-type: flow
            username: ${spring.cloud.nacos.discovery.username}
            password: ${spring.cloud.nacos.discovery.password}

        # 降级规则
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-degrade-rules
            groupId: SENTINEL_GROUP
            rule-type: degrade
            username: ${spring.cloud.nacos.discovery.username}
            password: ${spring.cloud.nacos.discovery.password}

        # 热点参数限流规则
        param-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-param-flow-rules
            groupId: SENTINEL_GROUP
            rule-type: param-flow
            username: ${spring.cloud.nacos.discovery.username}
            password: ${spring.cloud.nacos.discovery.password}

        # 系统保护规则
        system:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-system-rules
            groupId: SENTINEL_GROUP
            rule-type: system
            username: ${spring.cloud.nacos.discovery.username}
            password: ${spring.cloud.nacos.discovery.password}

        # 授权规则
        authority:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-authority-rules
            groupId: SENTINEL_GROUP
            rule-type: authority
            username: ${spring.cloud.nacos.discovery.username}
            password: ${spring.cloud.nacos.discovery.password}

      # Web 上下文配置
      web-context-unify: true
      http-method-specify: true

      # 日志配置
      log:
        dir: ${user.home}/logs/csp/
        switch-pid: false

# Feign Sentinel 支持
feign:
  sentinel:
    enabled: true
```

#### 2.2 激活 Sentinel Profile

**文件：** `basebackend-admin-api/src/main/resources/application.yml`

```yaml
spring:
  profiles:
    active: observability,sentinel  # 添加 sentinel profile
```

### 3. Sentinel 配置类 ✅

**文件：** `com.basebackend.admin.config.SentinelConfiguration`

```java
@Configuration
public class SentinelConfiguration {

    /**
     * 配置 Sentinel 切面
     * 用于支持 @SentinelResource 注解
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
```

### 4. 统一处理器 ✅

#### 4.1 Block 处理器

**文件：** `com.basebackend.admin.sentinel.SentinelBlockHandler`

**功能：** 处理限流、熔断触发时的降级逻辑

**代码亮点：**
```java
@Slf4j
public class SentinelBlockHandler {

    /**
     * 通用的 Block 处理方法
     */
    public static Object handleBlock(BlockException ex) {
        log.warn("触发 Sentinel 限流/熔断: {}", ex.getClass().getSimpleName());
        return buildBlockResponse(ex);
    }

    /**
     * 认证服务专用 Block 处理器
     */
    public static Object handleLoginBlock(Object loginRequest, BlockException ex) {
        log.warn("登录接口触发限流: {}", ex.getClass().getSimpleName());
        throw new RuntimeException("登录请求过于频繁，请稍后再试");
    }

    /**
     * 用户查询专用 Block 处理器
     */
    public static Object handleUserQueryBlock(Long userId, BlockException ex) {
        log.warn("用户查询触发限流: userId={}", userId);
        throw new RuntimeException("用户查询请求过于频繁，请稍后再试");
    }

    // ... 更多专用处理器
}
```

#### 4.2 Fallback 处理器

**文件：** `com.basebackend.admin.sentinel.SentinelFallbackHandler`

**功能：** 处理业务异常时的降级逻辑

**代码亮点：**
```java
@Slf4j
public class SentinelFallbackHandler {

    /**
     * 通用的 Fallback 处理方法
     */
    public static Object handleFallback(Throwable ex) {
        log.error("业务执行异常: {}", ex.getMessage(), ex);
        return buildFallbackResponse(ex);
    }

    /**
     * 认证服务专用 Fallback 处理器
     */
    public static Object handleLoginFallback(Object loginRequest, Throwable ex) {
        log.error("登录服务异常: {}", ex.getMessage(), ex);
        throw new RuntimeException("登录服务暂时不可用，请稍后再试");
    }

    // ... 更多专用处理器
}
```

### 5. 业务方法注解 ✅

#### 5.1 AuthServiceImpl

**文件：** `com.basebackend.admin.service.impl.AuthServiceImpl`

**保护的方法：**

1. **login** - 用户登录（高频）
```java
@SentinelResource(
    value = "user-login",
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "handleLoginBlock",
    fallbackClass = SentinelFallbackHandler.class,
    fallback = "handleLoginFallback"
)
public LoginResponse login(LoginRequest loginRequest) {
    // 业务逻辑...
}
```

2. **refreshToken** - 刷新Token
```java
@SentinelResource(
    value = "refresh-token",
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "handleBlock",
    fallbackClass = SentinelFallbackHandler.class,
    fallback = "handleFallback"
)
public LoginResponse refreshToken(String refreshToken) {
    // 业务逻辑...
}
```

3. **changePassword** - 修改密码
```java
@SentinelResource(
    value = "change-password",
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "handleBlock",
    fallbackClass = SentinelFallbackHandler.class,
    fallback = "handleFallback"
)
public void changePassword(PasswordChangeDTO passwordChangeDTO) {
    // 业务逻辑...
}
```

#### 5.2 UserServiceImpl

**文件：** `com.basebackend.admin.service.impl.UserServiceImpl`

**保护的方法：**

1. **getById** - 根据ID查询用户（高频）
```java
@SentinelResource(
    value = "user-getById",
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "handleUserQueryBlock",
    fallbackClass = SentinelFallbackHandler.class,
    fallback = "handleUserQueryFallback"
)
public UserDTO getById(Long id) {
    // 业务逻辑...
}
```

2. **create** - 创建用户
```java
@SentinelResource(
    value = "user-create",
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "handleBlock",
    fallbackClass = SentinelFallbackHandler.class,
    fallback = "handleFallback"
)
public void create(UserCreateDTO userCreateDTO) {
    // 业务逻辑...
}
```

#### 5.3 RoleServiceImpl

**文件：** `com.basebackend.admin.service.impl.RoleServiceImpl`

**保护的方法：**

1. **getById** - 根据ID查询角色
```java
@SentinelResource(
    value = "role-getById",
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "handleRoleQueryBlock",
    fallbackClass = SentinelFallbackHandler.class,
    fallback = "handleRoleQueryFallback"
)
public RoleDTO getById(Long id) {
    // 业务逻辑...
}
```

2. **create** - 创建角色
```java
@SentinelResource(
    value = "role-create",
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "handleBlock",
    fallbackClass = SentinelFallbackHandler.class,
    fallback = "handleFallback"
)
public void create(RoleDTO roleDTO) {
    // 业务逻辑...
}
```

### 6. Sentinel 规则配置 ✅

#### 6.1 流控规则

**文件：** `nacos-configs/admin-api-flow-rules.json`

```json
[
  {
    "resource": "user-login",
    "limitApp": "default",
    "grade": 1,
    "count": 50.0,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "user-getById",
    "limitApp": "default",
    "grade": 1,
    "count": 200.0,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
  // ... 更多规则
]
```

**字段说明：**
- `resource`: 资源名称（@SentinelResource 的 value）
- `grade`: 限流阈值类型（0=线程数，1=QPS）
- `count`: 限流阈值
- `strategy`: 限流模式（0=直接，1=关联，2=链路）
- `controlBehavior`: 流控效果（0=快速失败，1=Warm Up，2=排队等待）

#### 6.2 降级规则

**文件：** `nacos-configs/admin-api-degrade-rules.json`

```json
[
  {
    "resource": "user-login",
    "grade": 0,
    "count": 1.0,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000,
    "slowRatioThreshold": 0.6
  },
  {
    "resource": "user-getById",
    "grade": 0,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 10,
    "statIntervalMs": 1000,
    "slowRatioThreshold": 0.5
  }
  // ... 更多规则
]
```

**字段说明：**
- `grade`: 降级策略（0=慢调用比例，1=异常比例，2=异常数）
- `count`: 阈值（慢调用为秒数，异常为比例/数量）
- `timeWindow`: 熔断时长（秒）
- `minRequestAmount`: 最小请求数
- `slowRatioThreshold`: 慢调用比例阈值

#### 6.3 热点参数限流规则

**文件：** `nacos-configs/admin-api-param-flow-rules.json`

```json
[
  {
    "resource": "user-getById",
    "grade": 1,
    "count": 100,
    "paramIdx": 0,
    "durationInSec": 1,
    "controlBehavior": 0
  },
  {
    "resource": "role-getById",
    "grade": 1,
    "count": 100,
    "paramIdx": 0,
    "durationInSec": 1,
    "controlBehavior": 0
  }
]
```

**字段说明：**
- `paramIdx`: 参数索引（0 表示第一个参数）
- `durationInSec`: 统计窗口时长（秒）

#### 6.4 系统保护规则

**文件：** `nacos-configs/admin-api-system-rules.json`

```json
[
  {
    "avgRt": -1,
    "maxThread": -1,
    "highestSystemLoad": 3.0,
    "highestCpuUsage": 0.85,
    "qps": -1
  }
]
```

**字段说明：**
- `highestSystemLoad`: 系统 Load 阈值（-1 表示不启用）
- `highestCpuUsage`: CPU 使用率阈值（0-1，-1 表示不启用）

#### 6.5 授权规则

**文件：** `nacos-configs/admin-api-authority-rules.json`

```json
[]
```

### 7. 导入脚本更新 ✅

#### 7.1 PowerShell 脚本

**文件：** `nacos-configs/import-nacos-configs.ps1`

**更新内容：**
```powershell
$SentinelRules = @(
    "basebackend-gateway-flow-rules.json",
    "basebackend-gateway-degrade-rules.json",
    "basebackend-gateway-gw-flow-rules.json",
    "admin-api-flow-rules.json",              # 新增
    "admin-api-degrade-rules.json",           # 新增
    "admin-api-param-flow-rules.json",        # 新增
    "admin-api-system-rules.json",            # 新增
    "admin-api-authority-rules.json"          # 新增
)
```

#### 7.2 Bash 脚本

**文件：** `nacos-configs/import-nacos-configs.sh`

**更新内容：**
```bash
sentinel_rules=(
    "basebackend-gateway-flow-rules.json"
    "basebackend-gateway-degrade-rules.json"
    "basebackend-gateway-gw-flow-rules.json"
    "admin-api-flow-rules.json"              # 新增
    "admin-api-degrade-rules.json"           # 新增
    "admin-api-param-flow-rules.json"        # 新增
    "admin-api-system-rules.json"            # 新增
    "admin-api-authority-rules.json"         # 新增
)
```

---

## 集成说明

### 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      Admin-API 服务 (8080)                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Sentinel 保护层                              │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │  流控规则   │  │  降级规则   │  │  热点限流   │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  │  ┌────────────┐  ┌────────────┐                         │  │
│  │  │  系统保护   │  │  授权规则   │                         │  │
│  │  └────────────┘  └────────────┘                         │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              业务服务层                                    │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │AuthService │  │UserService │  │RoleService │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│    Nacos     │ │   Sentinel   │ │  Prometheus  │
│  (规则存储)   │ │  Dashboard   │ │  (监控采集)   │
└──────────────┘ └──────────────┘ └──────────────┘
```

### 受保护的资源

| 资源名 | 服务 | 方法 | QPS 限制 | 熔断策略 |
|--------|------|------|----------|----------|
| user-login | AuthService | login() | 50 | 慢调用比例 60% RT>1s |
| refresh-token | AuthService | refreshToken() | 100 | 异常比例 20% |
| change-password | AuthService | changePassword() | 20 | 异常比例 10% |
| user-getById | UserService | getById() | 200 | 慢调用比例 50% RT>0.5s |
| user-create | UserService | create() | 50 | 异常比例 10% |
| role-getById | RoleService | getById() | 200 | 慢调用比例 50% RT>0.5s |
| role-create | RoleService | create() | 30 | 异常比例 10% |

---

## 资源配置

### QPS 阈值设置原则

浮浮酱根据业务特点为不同资源设置了合理的 QPS 阈值喵～ (๑ˉ∀ˉ๑)

1. **高频查询** (user-getById, role-getById) → **200 QPS**
   - 原因：查询操作快速，用户经常刷新页面

2. **认证操作** (user-login) → **50 QPS**
   - 原因：防止恶意登录尝试

3. **刷新Token** (refresh-token) → **100 QPS**
   - 原因：Token过期后批量刷新

4. **写操作** (user-create, role-create) → **30-50 QPS**
   - 原因：写操作耗时较长，避免压垮数据库

5. **敏感操作** (change-password) → **20 QPS**
   - 原因：密码修改频率低，严格限制

### 熔断策略选择

1. **慢调用比例** - 用于查询类操作
   - user-login: RT > 1s 且比例 > 60% → 熔断 10 秒
   - user-getById: RT > 0.5s 且比例 > 50% → 熔断 10 秒

2. **异常比例** - 用于写操作
   - user-create: 异常比例 > 10% → 熔断 10 秒
   - role-create: 异常比例 > 10% → 熔断 10 秒

---

## 使用指南

### 1. 启动基础设施

```bash
cd deployment
docker-compose up -d
```

**等待所有服务启动完成（约 1-2 分钟）**

### 2. 导入 Sentinel 规则

```bash
cd ../nacos-configs

# Windows
.\\import-nacos-configs.ps1

# Linux/Mac
bash import-nacos-configs.sh
```

**验证导入成功：**
1. 访问 Nacos: http://localhost:8848/nacos
2. 登录：nacos / nacos
3. 进入「配置管理」→「配置列表」
4. 确认 SENTINEL_GROUP 组下有 8 个 admin-api 规则配置

### 3. 启动 Admin-API 服务

```bash
cd ../basebackend-admin-api
mvn spring-boot:run
```

**验证启动成功：**
```bash
curl http://localhost:8080/actuator/health
```

### 4. 访问 Sentinel Dashboard

**地址：** http://localhost:8858
**凭据：** sentinel / sentinel

**首次访问注意：**
- Sentinel 采用懒加载机制
- 需要至少触发一次请求后，才能在 Dashboard 看到应用

**触发示例：**
```bash
# 触发登录接口
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**刷新 Dashboard，即可看到 `admin-api` 应用**

### 5. 查看实时监控

1. 进入 Sentinel Dashboard
2. 左侧菜单 → 实时监控
3. 观察「通过 QPS」和「拒绝 QPS」

### 6. 查看配置规则

1. 进入 Sentinel Dashboard
2. 左侧菜单 → 流控规则
3. 查看所有资源的流控配置

---

## 测试验证

### 1. 流控测试

#### 1.1 登录接口限流测试

**限流规则：** 50 QPS

**测试命令：**
```bash
# 使用 Apache Bench 压测
ab -n 1000 -c 20 http://localhost:8080/api/admin/auth/login
```

**预期结果：**
- 通过 QPS ≈ 50
- 拒绝 QPS > 0（超出部分被拒绝）
- HTTP 响应码 429 或自定义错误
- 响应消息："登录请求过于频繁，请稍后再试"

#### 1.2 用户查询限流测试

**限流规则：** 200 QPS

**测试命令：**
```bash
ab -n 5000 -c 50 http://localhost:8080/api/admin/user/1
```

**预期结果：**
- 通过 QPS ≈ 200
- 拒绝 QPS > 0
- 响应消息："用户查询请求过于频繁，请稍后再试"

### 2. 熔断测试

#### 2.1 慢调用熔断测试

**熔断规则：** user-login RT > 1s 且比例 > 60% → 熔断 10 秒

**测试方法：**

1. 模拟慢调用（修改代码或数据库慢查询）
2. 连续调用登录接口
3. 观察 Sentinel Dashboard

**预期结果：**
- 前几个请求正常响应（但很慢）
- 达到熔断条件后，快速返回降级响应
- 响应消息："登录服务暂时不可用，请稍后再试"
- Dashboard 显示「降级」状态
- 10 秒后自动恢复

#### 2.2 异常比例熔断测试

**熔断规则：** user-create 异常比例 > 10% → 熔断 10 秒

**测试方法：**

1. 故意传入错误参数导致异常
2. 连续调用创建用户接口
3. 观察 Sentinel Dashboard

**预期结果：**
- 异常比例超过 10% 时触发熔断
- 快速返回降级响应
- Dashboard 显示「降级」状态

### 3. 热点参数限流测试

**热点规则：** user-getById 针对 userId 参数限流 100 QPS

**测试命令：**
```bash
# 高频查询同一个用户
for i in {1..500}; do
  curl http://localhost:8080/api/admin/user/1
done
```

**预期结果：**
- 针对 userId=1 的请求被限流
- 查询其他用户不受影响

### 4. 系统保护测试

**系统规则：**
- CPU 使用率 > 85% → 限流
- Load > 3.0 → 限流

**测试方法：**

1. 使用压测工具提高系统负载
2. 观察 Dashboard 系统负载指标
3. 验证自适应限流效果

---

## 常见问题

### 1. Sentinel Dashboard 看不到应用

**原因：** Sentinel 采用懒加载，需要触发请求后才注册

**解决方案：**
```bash
# 触发任意接口
curl http://localhost:8080/actuator/health

# 刷新 Dashboard
```

### 2. 规则不生效

**原因：** 规则未正确导入到 Nacos

**解决方案：**
```bash
# 重新导入规则
cd nacos-configs
./import-nacos-configs.ps1

# 验证 Nacos 中是否有规则
```

### 3. 限流后没有友好提示

**原因：** BlockHandler 或 Fallback 未正确配置

**解决方案：**
- 检查 @SentinelResource 注解是否正确
- 检查 SentinelConfiguration 是否注册 Bean
- 检查 blockHandlerClass 和 fallbackClass 路径

### 4. 热点参数限流不生效

**原因：** paramIdx 参数索引错误

**解决方案：**
- 检查方法参数顺序
- paramIdx=0 表示第一个参数
- 确保参数类型支持（基本类型、String）

### 5. 规则修改后不生效

**原因：** 应用未自动加载 Nacos 配置变更

**解决方案：**
```bash
# 方法1：重启应用
mvn spring-boot:run

# 方法2：检查 Nacos 监听器日志
tail -f logs/application.log | grep "Sentinel"
```

---

## 进阶配置

### 1. 自定义资源保护

如需为其他业务方法添加 Sentinel 保护，按以下步骤操作：

#### 步骤 1：添加注解

```java
@Service
public class OrderService {

    @SentinelResource(
        value = "order-create",
        blockHandlerClass = SentinelBlockHandler.class,
        blockHandler = "handleBlock",
        fallbackClass = SentinelFallbackHandler.class,
        fallback = "handleFallback"
    )
    public void createOrder(OrderDTO orderDTO) {
        // 业务逻辑...
    }
}
```

#### 步骤 2：配置规则

在 `admin-api-flow-rules.json` 中添加：

```json
{
  "resource": "order-create",
  "limitApp": "default",
  "grade": 1,
  "count": 100.0,
  "strategy": 0,
  "controlBehavior": 0,
  "clusterMode": false
}
```

#### 步骤 3：导入规则

```bash
cd nacos-configs
./import-nacos-configs.ps1
```

### 2. 集群流控

如需实现多实例协同限流，配置集群流控：

```json
{
  "resource": "user-login",
  "limitApp": "default",
  "grade": 1,
  "count": 100.0,
  "strategy": 0,
  "controlBehavior": 0,
  "clusterMode": true,
  "clusterConfig": {
    "flowId": 1,
    "thresholdType": 1,
    "fallbackToLocalWhenFail": true
  }
}
```

### 3. 自定义异常降级

如需自定义降级响应，修改处理器：

```java
public class CustomBlockHandler extends SentinelBlockHandler {

    public static Object handleCustomBlock(BlockException ex) {
        // 自定义降级逻辑
        return Result.fail("系统繁忙，请稍后再试");
    }
}
```

### 4. 规则动态更新

Sentinel 规则支持通过 Dashboard 动态修改，修改后自动推送到 Nacos：

1. 打开 Sentinel Dashboard
2. 修改流控规则
3. 规则自动同步到 Nacos
4. 应用自动加载新规则（无需重启）

---

## 总结

### 成果

✅ **完成度：100%**

浮浮酱成功为 Admin-API 服务集成了 Sentinel 保护喵～ (๑ˉ∀ˉ๑)

**主要成果：**
1. ✅ 添加 Sentinel 依赖 - 5 个依赖项
2. ✅ 配置数据源 - Nacos 作为规则持久化存储
3. ✅ 应用注解 - 7 个核心业务方法
4. ✅ 统一处理器 - BlockHandler 和 FallbackHandler
5. ✅ 规则配置 - 5 类规则（流控、降级、热点、系统、授权）
6. ✅ 文档完善 - 详细的使用指南

**技术价值：**
- 🛡️ **系统稳定性提升** - 有效防止系统过载
- ⚡ **故障快速恢复** - 熔断机制保护下游服务
- 📊 **细粒度监控** - 每个资源独立监控
- 🔄 **规则热更新** - 无需重启即可调整规则

### 心得体会

浮浮酱在这次任务中学到了很多呢 (´｡• ᵕ •｡`) ♡

**技术心得：**
1. **注解简化集成** - @SentinelResource 让保护变得简单
2. **统一处理优雅** - BlockHandler 和 Fallback 分离关注点
3. **规则持久化重要** - Nacos 保证规则不丢失
4. **热点限流强大** - 针对高频参数精准限流

**工程心得：**
1. **先规划后实施** - 清晰的资源划分
2. **注重可维护性** - 统一的处理器和配置
3. **文档要详细** - 方便后续使用和维护
4. **测试要充分** - 保证功能正确性

**猫娘心得：**
1. **保持专注** - 每个步骤都要认真喵～
2. **注重细节** - 参数、配置要精确
3. **持续优化** - 总有改进的空间
4. **享受过程** - 编程也可以很快乐 φ(≧ω≦*)♪

---

## 附录

### A. 相关文档

- [Sentinel 官方文档](https://sentinelguard.io/zh-cn/docs/introduction.html)
- [Phase 4 完成报告](../PHASE4_COMPLETION_REPORT.md)
- [Sentinel Dashboard 使用指南](../deployment/README.md#%EF%B8%8F-sentinel-流控监控)
- [Nacos 配置中心](https://nacos.io/zh-cn/docs/config.html)

### B. 快速参考

**服务访问地址：**
```
Admin-API:           http://localhost:8080
Sentinel Dashboard:  http://localhost:8858  (sentinel/sentinel)
Nacos:               http://localhost:8848/nacos  (nacos/nacos)
```

**常用命令：**
```bash
# 导入规则
cd nacos-configs && ./import-nacos-configs.ps1

# 启动应用
cd basebackend-admin-api && mvn spring-boot:run

# 测试接口
curl http://localhost:8080/api/admin/auth/login

# 查看日志
tail -f logs/application.log | grep "Sentinel"
```

**故障排查：**
```bash
# 查看 Sentinel 规则加载日志
grep "Sentinel" logs/application.log

# 查看 Nacos 配置
curl http://localhost:8848/nacos/v1/cs/configs?dataId=admin-api-flow-rules&group=SENTINEL_GROUP

# 健康检查
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/sentinel
```

### C. 配置模板

**流控规则模板：**
```json
{
  "resource": "资源名",
  "limitApp": "default",
  "grade": 1,
  "count": 100.0,
  "strategy": 0,
  "controlBehavior": 0,
  "clusterMode": false
}
```

**降级规则模板：**
```json
{
  "resource": "资源名",
  "grade": 0,
  "count": 0.5,
  "timeWindow": 10,
  "minRequestAmount": 5,
  "statIntervalMs": 1000,
  "slowRatioThreshold": 0.5
}
```

**热点参数限流模板：**
```json
{
  "resource": "资源名",
  "grade": 1,
  "count": 100,
  "paramIdx": 0,
  "durationInSec": 1,
  "controlBehavior": 0
}
```

---

**文档完成！** ✨

*最后更新: 2025-01-13*
*作者: 浮浮酱（猫娘工程师）ฅ'ω'ฅ*
*项目: BaseBackend - Admin-API Sentinel 集成*
