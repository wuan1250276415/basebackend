# BaseBackend Admin-API 模块拆分分析报告

> **分析日期**: 2025-11-17
> **分析范围**: basebackend-admin-api 模块完整代码
> **目标**: 识别可拆分的微服务模块和可提取的公共功能

---

## 一、当前 Admin-API 现状分析

### 1.1 模块规模统计

| 类型 | 数量 | 说明 |
|------|------|------|
| **Controller** | 35+ | 包含多个子包 |
| **Service** | 30+ | 包含业务服务和基础设施服务 |
| **Entity** | 40+ | 涵盖用户、业务、通知、监控等 |
| **DTO** | 25+ | 数据传输对象 |
| **Aspect** | 2 | OperationLogAspect, PermissionAspect |
| **依赖模块** | 16 | 几乎所有基础设施模块 |

### 1.2 包结构分析

```
basebackend-admin-api/
├── annotation/           # 权限注解
│   ├── RequiresPermission.java
│   ├── RequiresRole.java
│   └── DataScope.java
├── aspect/              # AOP切面
│   ├── OperationLogAspect.java      # 操作日志
│   └── PermissionAspect.java        # 权限校验
├── config/              # 配置类
│   ├── AdminSecurityConfig.java
│   ├── JacksonConfig.java
│   ├── SwaggerConfig.java
│   └── WebMvcConfig.java
├── consumer/            # 消息消费者
│   └── NotificationConsumer.java
├── context/             # 用户上下文
│   ├── UserContext.java
│   └── UserContextHolder.java
├── controller/          # 控制器
│   ├── AuthController.java         # 认证
│   ├── DeptController.java         # 部门
│   ├── DictController.java         # 字典
│   ├── MenuController.java         # 菜单
│   ├── ApplicationController.java  # 应用
│   ├── messaging/          # 消息模块
│   ├── nacos/              # 配置中心
│   ├── observability/      # 可观测性
│   └── storage/            # 存储
├── dto/                  # 数据传输对象
├── entity/               # 实体类
│   ├── SysUser.java       # 用户
│   ├── SysRole.java       # 角色
│   ├── SysPermission.java # 权限
│   ├── SysDept.java       # 部门
│   ├── SysDict.java       # 字典
│   ├── SysMenu.java       # 菜单
│   ├── messaging/         # 消息实体
│   ├── nacos/             # 配置实体
│   ├── observability/     # 监控实体
│   └── storage/           # 存储实体
├── mapper/               # MyBatis映射器
├── service/              # 服务层
│   ├── AuthService.java          # 认证服务
│   ├── UserService.java          # 用户服务（隐含）
│   ├── messaging/                # 消息服务
│   ├── nacos/                    # 配置服务
│   ├── observability/            # 监控服务
│   └── storage/                  # 存储服务
```

### 1.3 存在的问题

#### ❌ **单体过度膨胀**
- 依赖16个其他模块
- 启动时间过长（估计30-60秒）
- 内存占用过大（估计1-2GB）
- 部署风险高（一个小bug影响所有功能）

#### ❌ **职责不清晰**
- 同时承担业务管理、系统配置、监控运维
- 既是API服务，又是管理后台
- 混合了多个业务域

#### ❌ **扩展性差**
- 无法按业务域独立扩缩容
- 用户量增长会拖慢整个系统
- 部署和迭代周期长

---

## 二、可拆分的微服务模块分析

基于**业务域隔离**和**数据独立性**原则，建议拆分为以下微服务：

### 2.1 🔐 用户认证与权限服务 (User Auth Service)

#### **职责范围**
- 用户认证与授权
- JWT令牌管理
- 用户角色权限控制
- 数据权限控制

#### **主要组件**

**Controller:**
- `AuthController.java` - 登录、注销、令牌刷新
- `PermissionController.java` - 权限管理
- `SecurityController.java` - 安全配置（2FA、设备管理）

**Service:**
- `AuthService.java` - 认证服务
- `PermissionService.java` - 权限服务

**Entity:**
```java
- SysUser.java                    # 用户信息
- SysRole.java                    # 角色
- SysPermission.java              # 权限
- SysUserRole.java                # 用户角色关联
- SysRolePermission.java          # 角色权限关联
- User2FA.java                    # 双因子认证
- UserDevice.java                 # 设备信息
```

**DTO:**
```java
- UserDTO.java                    # 用户数据传输对象
- UserCreateDTO.java              # 创建用户
- UserQueryDTO.java               # 查询用户
- LoginRequest.java               # 登录请求
- LoginResponse.java              # 登录响应
```

**配置:**
- `AdminSecurityConfig.java` - 安全配置
- `JwtConfig.java` - JWT配置

#### **依赖关系**
```
basebackend-common (工具类)
basebackend-jwt (JWT处理)
basebackend-security (安全框架)
basebackend-cache (Redis缓存)
basebackend-database (MyBatis)
basebackend-web (Web基础)
```

#### **数据库表设计**
```sql
sys_user                      # 用户表
sys_role                      # 角色表
sys_permission                # 权限表
sys_user_role                 # 用户角色关联表
sys_role_permission           # 角色权限关联表
sys_role_data_permission      # 数据权限表
sys_role_list_operation       # 列表操作权限表
user_2fa                      # 双因子认证表
user_device                   # 设备表
```

#### **API端点**
```
POST   /api/auth/login        # 用户登录
POST   /api/auth/logout       # 用户登出
POST   /api/auth/refresh      # 刷新令牌
GET    /api/auth/profile      # 获取用户信息
POST   /api/auth/2fa/enable   # 启用2FA
GET    /api/permissions       # 获取权限列表
GET    /api/roles             # 获取角色列表
POST   /api/users             # 创建用户
GET    /api/users/{id}        # 获取用户详情
PUT    /api/users/{id}        # 更新用户
DELETE /api/users/{id}        # 删除用户
```

#### **可独立扩展性** ⭐⭐⭐⭐⭐
- 认证是系统核心，需要高可用
- 用户量大时需独立扩展
- 安全策略可能频繁变更

---

### 2.2 👥 用户与组织服务 (User & Organization Service)

#### **职责范围**
- 用户信息管理
- 部门组织架构
- 用户偏好设置
- 用户操作日志

#### **主要组件**

**Controller:**
- `UserController.java` - 用户信息管理（需要确认是否存在）
- `DeptController.java` - 部门管理
- `PreferenceController.java` - 用户偏好

**Service:**
- `UserService.java` - 用户服务（可能未找到）
- `DeptService.java` - 部门服务
- `ProfileService.java` - 个人资料服务
- `PreferenceService.java` - 偏好服务

**Entity:**
```java
- SysDept.java                    # 部门信息
- SysDept.java                    # 部门层级
- UserPreference.java             # 用户偏好
- SysOperationLog.java            # 操作日志
- SysLoginLog.java                # 登录日志
- UserOperationLog.java           # 用户操作日志
```

**DTO:**
```java
- DeptDTO.java                    # 部门DTO
- UserPreferenceDTO.java          # 偏好DTO
- OperationLogDTO.java            # 操作日志DTO
- LoginLogDTO.java                # 登录日志DTO
```

#### **依赖关系**
```
basebackend-common
basebackend-database
basebackend-cache
basebackend-security (权限控制)
basebackend-observability (操作日志)
```

#### **数据库表设计**
```sql
sys_dept                       # 部门表
sys_dict                       # 字典表
sys_dict_data                  # 字典数据表
user_preference               # 用户偏好表
sys_operation_log             # 操作日志表
sys_login_log                 # 登录日志表
user_operation_log            # 用户操作日志表
```

#### **API端点**
```
# 部门管理
GET    /api/depts              # 获取部门列表
POST   /api/depts              # 创建部门
GET    /api/depts/{id}         # 获取部门详情
PUT    /api/depts/{id}         # 更新部门
DELETE /api/depts/{id}         # 删除部门
GET    /api/depts/tree         # 获取部门树

# 字典管理
GET    /api/dicts              # 获取字典列表
POST   /api/dicts              # 创建字典
GET    /api/dicts/{id}         # 获取字典详情
PUT    /api/dicts/{id}         # 更新字典
DELETE /api/dicts/{id}         # 删除字典

# 用户管理（如果存在）
GET    /api/users              # 获取用户列表
POST   /api/users              # 创建用户
GET    /api/users/{id}         # 获取用户详情
PUT    /api/users/{id}         # 更新用户
DELETE /api/users/{id}         # 删除用户

# 偏好管理
GET    /api/preferences        # 获取用户偏好
PUT    /api/preferences        # 更新用户偏好
```

#### **可独立扩展性** ⭐⭐⭐⭐
- 组织架构查询频繁，需缓存
- 操作日志量大，需独立存储

---

### 2.3 📱 系统配置服务 (System Config Service)

#### **职责范围**
- 菜单配置管理
- 应用资源管理
- 功能开关
- 列表操作配置

#### **主要组件**

**Controller:**
- `MenuController.java` - 菜单管理
- `ApplicationController.java` - 应用管理
- `ApplicationResourceController.java` - 资源管理
- `FeatureToggleController.java` - 功能开关
- `ListOperationController.java` - 列表操作

**Service:**
- `MenuService.java` - 菜单服务
- `ApplicationService.java` - 应用服务
- `ApplicationResourceService.java` - 资源服务

**Entity:**
```java
- SysMenu.java                    # 菜单表
- SysApplication.java             # 应用表
- SysApplicationResource.java     # 资源表
- SysRoleMenu.java                # 角色菜单关联
```

**DTO:**
```java
- MenuDTO.java                    # 菜单DTO
- ApplicationDTO.java             # 应用DTO
- ApplicationResourceDTO.java     # 资源DTO
```

#### **依赖关系**
```
basebackend-common
basebackend-database
basebackend-cache (菜单缓存)
basebackend-security (权限控制)
```

#### **数据库表设计**
```sql
sys_menu                        # 菜单表
sys_application                 # 应用表
sys_application_resource        # 资源表
sys_role_menu                   # 角色菜单关联表
```

#### **API端点**
```
GET    /api/menus               # 获取菜单列表
POST   /api/menus               # 创建菜单
GET    /api/menus/{id}          # 获取菜单详情
PUT    /api/menus/{id}          # 更新菜单
DELETE /api/menus/{id}          # 删除菜单
GET    /api/menus/tree          # 获取菜单树

GET    /api/applications        # 获取应用列表
POST   /api/applications        # 创建应用
GET    /api/applications/{id}   # 获取应用详情
PUT    /api/applications/{id}   # 更新应用
DELETE /api/applications/{id}   # 删除应用
```

#### **可独立扩展性** ⭐⭐⭐
- 菜单和配置变更不频繁
- 可独立部署更新

---

### 2.4 📧 通知中心服务 (Notification Service)

#### **职责范围**
- 系统通知推送
- 邮件通知
- 消息队列管理
- Webhook配置

#### **主要组件**

**Controller:**
- `NotificationController.java` - 通知管理
- `messaging/DeadLetterController.java` - 死信队列
- `messaging/EventController.java` - 事件管理
- `messaging/MessageMonitorController.java` - 消息监控
- `messaging/WebhookConfigController.java` - Webhook配置
- `messaging/WebhookLogController.java` - Webhook日志

**Service:**
- `NotificationService.java` - 通知服务
- `SSENotificationService.java` - 服务端推送
- `messaging/WebhookConfigService.java` - Webhook服务
- `messaging/MessageMonitorService.java` - 消息监控服务
- `messaging/DeadLetterService.java` - 死信处理服务

**Entity:**
```java
- UserNotification.java             # 用户通知
- messaging/SysWebhookConfig.java   # Webhook配置
- messaging/SysWebhookLog.java      # Webhook日志
- messaging/SysDeadLetter.java      # 死信消息
```

**DTO:**
```java
- notification/UserNotificationDTO.java  # 通知DTO
- messaging/WebhookConfigDTO.java        # Webhook配置DTO
```

#### **依赖关系**
```
basebackend-common
basebackend-database
basebackend-messaging (RocketMQ)
basebackend-cache (通知缓存)
basebackend-observability (通知追踪)
```

#### **数据库表设计**
```sql
user_notification             # 用户通知表
sys_webhook_config           # Webhook配置表
sys_webhook_log              # Webhook日志表
sys_dead_letter              # 死信队列表
```

#### **API端点**
```
POST   /api/notifications     # 发送通知
GET    /api/notifications     # 获取通知列表
GET    /api/notifications/{id} # 获取通知详情
PUT    /api/notifications/{id}/read # 标记已读

POST   /api/webhooks          # 创建Webhook
GET    /api/webhooks          # 获取Webhook列表
PUT    /api/webhooks/{id}     # 更新Webhook
DELETE /api/webhooks/{id}     # 删除Webhook
GET    /api/webhook-logs      # 获取Webhook日志
```

#### **可独立扩展性** ⭐⭐⭐⭐⭐
- 通知量波动大，需独立扩展
- 推送方式可能随时变更

---

### 2.5 💾 文件存储服务 (File Storage Service)

#### **职责范围**
- 文件上传下载
- 文件管理
- 备份恢复

#### **主要组件**

**Controller:**
- `storage/FileController.java` - 文件管理
- `storage/AdminFileController.java` - 管理员文件
- `storage/BackupController.java` - 备份管理

**Service:**
- `storage/SysFileService.java` - 文件服务
- `storage/SysBackupService.java` - 备份服务

**Entity:**
```java
- storage/SysFileInfo.java       # 文件信息
- storage/SysBackupRecord.java   # 备份记录
```

**DTO:**
```java
- storage/FileInfoDTO.java       # 文件信息DTO
- storage/BackupRecordDTO.java   # 备份记录DTO
```

#### **依赖关系**
```
basebackend-common
basebackend-database
basebackend-file-service (文件处理模块)
basebackend-backup (备份模块)
basebackend-observability (文件操作追踪)
```

#### **API端点**
```
POST   /api/files              # 上传文件
GET    /api/files/{id}         # 下载文件
GET    /api/files              # 获取文件列表
DELETE /api/files/{id}         # 删除文件

POST   /api/backups            # 创建备份
GET    /api/backups            # 获取备份列表
POST   /api/backups/{id}/restore # 恢复备份
```

#### **可独立扩展性** ⭐⭐⭐⭐
- 文件IO密集型服务
- 备份需要独立调度

---

### 2.6 📊 可观测性服务 (Observability Service)

#### **职责范围**
- 系统监控指标
- 日志查询
- 链路追踪
- 告警管理

#### **主要组件**

**Controller:**
- `observability/MetricsController.java` - 指标查询
- `observability/LogOController.java` - 日志查询
- `observability/TraceController.java` - 链路追踪
- `observability/AlertController.java` - 告警管理

**Service:**
- `observability/MetricsQueryService.java` - 指标查询服务
- `observability/LogQueryService.java` - 日志查询服务
- `observability/TraceQueryService.java` - 链路追踪服务
- `observability/AlertManagementService.java` - 告警管理服务

#### **依赖关系**
```
basebackend-common
basebackend-observability (监控框架)
basebackend-database (日志存储)
```

#### **API端点**
```
GET    /api/metrics            # 查询指标
GET    /api/logs               # 查询日志
GET    /api/traces             # 查询链路
POST   /api/alerts             # 创建告警
GET    /api/alerts             # 获取告警列表
PUT    /api/alerts/{id}        # 更新告警
DELETE /api/alerts/{id}        # 删除告警
```

#### **可独立扩展性** ⭐⭐⭐⭐⭐
- 监控数据量巨大
- 查询需要高性能存储
- 可独立于业务系统

---

### 2.7 ⚙️ 配置中心服务 (Config Center Service)

#### **职责范围**
- Nacos配置管理
- 配置历史
- 灰度发布
- 服务发现

#### **主要组件**

**Controller:**
- `nacos/NacosConfigController.java` - 配置管理
- `nacos/NacosConfigHistoryController.java` - 配置历史
- `nacos/NacosGrayReleaseController.java` - 灰度发布
- `nacos/NacosServiceDiscoveryController.java` - 服务发现

**Service:**
- `nacos/NacosConfigManagementService.java` - 配置管理服务
- `nacos/NacosConfigHistoryService.java` - 配置历史服务
- `nacos/NacosGrayReleaseManagementService.java` - 灰度发布服务
- `nacos/NacosServiceDiscoveryManagementService.java` - 服务发现服务

#### **依赖关系**
```
basebackend-common
basebackend-database
basebackend-nacos (配置中心框架)
```

#### **API端点**
```
GET    /api/nacos/configs      # 获取配置列表
POST   /api/nacos/configs      # 创建配置
GET    /api/nacos/configs/{id} # 获取配置详情
PUT    /api/nacos/configs/{id} # 更新配置
DELETE /api/nacos/configs/{id} # 删除配置
POST   /api/nacos/publish      # 发布配置
```

#### **可独立扩展性** ⭐⭐⭐⭐
- 配置变更需要高可用
- 服务发现需要高性能

---

## 三、可提取到公共类的功能分析

以下功能可以提取到 `basebackend-common` 或其他基础模块中：

### 3.1 🔧 AOP切面类

#### **OperationLogAspect** ⭐⭐⭐⭐⭐

**当前位置**: `basebackend-admin-api/src/main/java/com/basebackend/admin/aspect/OperationLogAspect.java`

**功能分析**:
- 自动记录用户操作日志
- 记录操作时间、参数、IP、结果
- 支持异常捕获和错误记录
- 自动根据方法名判断操作类型

**提取建议**:
```
移动到: basebackend-logging 模块
```

**优化改进**:
```java
// 增强建议
public class OperationLogAspect {

    // 1. 支持自定义操作名称注解
    @OperationName("创建用户")

    // 2. 支持忽略敏感参数
    @IgnoreParams({"password", "token"})

    // 3. 支持条件记录（仅成功/仅失败/全部）
    @LogCondition(RecordType.SUCCESS_ONLY)

    // 4. 支持异步记录（提高性能）
    @AsyncLog

    // 5. 支持批量记录（提高吞吐量）
    @BatchSize(100)
    public Object logOperation() { ... }
}
```

**配置化支持**:
```yaml
# application.yml
logging:
  operation:
    enabled: true
    async: true
    batch-size: 100
    retention-days: 90
    ignore-params:
      - password
      - token
      - secret
```

---

#### **PermissionAspect** ⭐⭐⭐⭐⭐

**当前位置**: `basebackend-admin-api/src/main/java/com/basebackend/admin/aspect/PermissionAspect.java`

**功能分析**:
- 权限校验切面
- 角色校验切面
- 数据权限控制
- 支持AND/OR逻辑

**提取建议**:
```
移动到: basebackend-security 模块
```

**优化改进**:
```java
// 增强建议
public class PermissionAspect {

    // 1. 支持多租户权限控制
    @RequiresPermission(value = "user:view", tenantScope = true)

    // 2. 支持数据权限范围
    @DataScope(scope = DataScope.DEPT_AND_SUB_DEPT)

    // 3. 支持API级别权限
    @RequiresApiPermission(api = "user-api", action = "query")

    // 4. 支持权限缓存
    @CachedPermission(expiration = 300)
    public Object checkPermission() { ... }
}
```

---

#### **新增建议的切面类** ⭐⭐⭐

1. **RedisCacheAspect** - Redis缓存切面
   - 自动缓存查询结果
   - 支持过期策略
   - 支持缓存穿透保护

2. **RateLimitAspect** - 限流切面
   - 基于注解的限流配置
   - 支持多种限流算法（令牌桶、漏桶）
   - 支持分布式限流

3. **CircuitBreakerAspect** - 熔断切面
   - 自动熔断降级
   - 支持失败重试
   - 支持熔断恢复

---

### 3.2 🏷️ 注解类

**当前位置**: `basebackend-admin-api/src/main/java/com/basebackend/admin/annotation/`

**提取建议**:

| 注解 | 当前模块 | 建议移动到 | 优先级 |
|------|----------|-----------|--------|
| RequiresPermission | admin-api | basebackend-security | ⭐⭐⭐⭐⭐ |
| RequiresRole | admin-api | basebackend-security | ⭐⭐⭐⭐⭐ |
| DataScope | admin-api | basebackend-database | ⭐⭐⭐⭐ |

**优化建议**:

```java
// 增强注解设计
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    String[] values() default {};

    String value() default "";

    Logical logical() default Logical.OR;

    boolean required() default true;  // 是否必需

    String description() default "";  // 权限描述
}

public enum Logical {
    AND,  // 所有权限都满足
    OR    // 任一权限满足
}
```

---

### 3.3 📋 实体类分析

#### 3.3.1 可提取到 basebackend-common 的实体

**用户相关实体** ⭐⭐⭐
```
- UserContext.java
- UserContextHolder.java
```

这些是纯工具类，应该放在common中。

#### 3.3.2 可提取到对应业务模块的实体

**用户权限实体** ⭐⭐⭐⭐⭐
```
- SysUser.java
- SysRole.java
- SysPermission.java
- SysUserRole.java
- SysRolePermission.java
```

**建议**:
```
移动到: basebackend-user-service 模块
```

**系统配置实体** ⭐⭐⭐⭐
```
- SysDept.java
- SysDict.java
- SysDictData.java
- SysMenu.java
```

**建议**:
```
移动到: basebackend-system-service 模块
```

**通知实体** ⭐⭐⭐⭐
```
- UserNotification.java
```

**建议**:
```
移动到: basebackend-notification-service 模块
```

**监控实体** ⭐⭐⭐
```
- observability 包下的所有实体
```

**建议**:
```
移动到: basebackend-observability-service 模块
```

---

### 3.4 🔧 工具类

**建议提取的配置文件**:

| 配置类 | 当前模块 | 建议移动到 | 优先级 |
|--------|----------|-----------|--------|
| JacksonConfig.java | admin-api | basebackend-web | ⭐⭐⭐⭐ |
| SwaggerConfig.java | admin-api | basebackend-common | ⭐⭐⭐ |
| WebMvcConfig.java | admin-api | basebackend-web | ⭐⭐⭐⭐ |
| AdminSecurityConfig.java | admin-api | basebackend-security | ⭐⭐⭐⭐⭐ |

---

### 3.5 💡 消息消费者

**NotificationConsumer.java** ⭐⭐⭐

**功能**:
- 监听消息队列
- 异步处理通知

**提取建议**:
```
移动到: basebackend-notification-service 模块
```

---

## 四、拆分优先级建议

### Phase 1: 最优先拆分（独立性强，影响面小）⭐⭐⭐⭐⭐

1. **可观测性服务** (Observability Service)
   - 独立性强，不依赖其他业务
   - 数据量大，需要独立存储
   - 影响面：监控告警

2. **文件存储服务** (File Storage Service)
   - IO密集型，与业务解耦
   - 独立扩展性好
   - 影响面：文件上传下载

### Phase 2: 高优先级拆分（核心业务，独立部署）

3. **用户认证与权限服务** (User Auth Service)
   - 系统核心，需要高可用
   - 安全要求高
   - 影响面：所有用户登录和权限校验

4. **通知中心服务** (Notification Service)
   - 业务依赖度高，但可独立
   - 消息量波动大
   - 影响面：系统通知

### Phase 3: 中优先级拆分（业务相关性高）

5. **用户与组织服务** (User & Organization Service)
   - 依赖认证服务
   - 业务逻辑复杂
   - 影响面：部门、用户管理

6. **系统配置服务** (System Config Service)
   - 配置变更频率低
   - 依赖认证服务
   - 影响面：菜单、字典

### Phase 4: 最后拆分（可以保留在admin-api）

7. **配置中心服务** (Config Center Service)
   - 可以集成到admin-api
   - 配置类变更较少
   - 或独立为basebackend-config-server

---

## 五、拆分实施计划

### 5.1 准备工作（1-2天）

#### Step 1: 创建新模块目录
```bash
mkdir -p basebackend-user-auth-service
mkdir -p basebackend-user-organization-service
mkdir -p basebackend-system-config-service
mkdir -p basebackend-notification-service
mkdir -p basebackend-file-storage-service
mkdir -p basebackend-observability-service
mkdir -p basebackend-config-center-service
```

#### Step 2: 创建基础POM文件
为每个服务创建标准的Spring Boot POM文件，包含必要的基础依赖。

#### Step 3: 创建基础配置
创建bootstrap.yml、application.yml等配置文件。

### 5.2 拆分执行顺序（2-3周）

#### Week 1: Phase 1 - 独立服务

**Day 1-2: 拆分可观测性服务**
- 复制观测相关Controller、Service、Entity到新模块
- 配置Prometheus/Grafana集成
- 创建独立的Docker配置

**Day 3-4: 拆分文件存储服务**
- 复制文件相关Controller、Service、Entity
- 配置MinIO/OSS集成
- 测试文件上传下载功能

**Day 5: 测试和优化**
- 端到端测试
- 性能测试
- 修复发现的问题

#### Week 2: Phase 2 - 核心服务

**Day 1-3: 拆分用户认证与权限服务**
- 这是最复杂的拆分，需要仔细处理依赖
- 迁移AuthService、PermissionService、Security相关类
- 配置JWT和权限系统
- 测试登录、权限校验流程

**Day 4-5: 拆分通知中心服务**
- 复制通知相关Controller、Service、Entity
- 配置RocketMQ集成
- 测试通知推送功能

#### Week 3: Phase 3 - 业务服务

**Day 1-3: 拆分用户与组织服务**
- 迁移用户、部门、字典相关类
- 配置缓存策略
- 测试组织架构功能

**Day 4-5: 拆分系统配置服务**
- 迁移菜单、应用资源相关类
- 配置缓存策略
- 测试配置管理功能

### 5.3 迁移步骤详解

#### Step 1: 复制代码
```bash
# 示例：迁移用户认证服务
mkdir -p basebackend-user-auth-service/src/main/java/com/basebackend/auth/controller
mkdir -p basebackend-user-auth-service/src/main/java/com/basebackend/auth/service
mkdir -p basebackend-user-auth-service/src/main/java/com/basebackend/auth/entity
mkdir -p basebackend-user-auth-service/src/main/java/com/basebackend/auth/mapper

# 复制文件
cp -r basebackend-admin-api/src/main/java/com/basebackend/admin/controller/AuthController.java basebackend-user-auth-service/src/main/java/com/basebackend/auth/controller/
cp -r basebackend-admin-api/src/main/java/com/basebackend/admin/service/AuthService.java basebackend-user-auth-service/src/main/java/com/basebackend/auth/service/
# ... 复制其他相关文件
```

#### Step 2: 调整包名
使用IDE的"重构 > 重命名"功能，将包名从 `com.basebackend.admin.*` 修改为 `com.basebackend.auth.*`。

#### Step 3: 更新依赖
在新的服务POM文件中添加必要的依赖：
```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common</artifactId>
</dependency>
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-security</artifactId>
</dependency>
<!-- ... 其他依赖 -->
```

#### Step 4: 调整配置
调整bootstrap.yml和application.yml文件：
```yaml
spring:
  application:
    name: basebackend-user-auth-service  # 服务名
  cloud:
    nacos:
      discovery:
        service-name: basebackend-user-auth-service
```

#### Step 5: 更新网关路由
在Gateway中添加新服务的路由：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-auth-service
          uri: lb://basebackend-user-auth-service
          predicates:
            - Path=/api/auth/**
```

---

## 六、数据迁移策略

### 6.1 数据库拆分策略

#### 策略1: 按业务域拆分数据库（推荐）

**用户认证库 (user_auth)**
```sql
sys_user
sys_role
sys_permission
sys_user_role
sys_role_permission
sys_user_device
user_2fa
```

**用户组织库 (user_org)**
```sql
sys_dept
sys_dict
sys_dict_data
user_preference
sys_operation_log
sys_login_log
user_operation_log
```

**系统配置库 (sys_config)**
```sql
sys_menu
sys_application
sys_application_resource
sys_role_menu
```

**通知中心库 (notification)**
```sql
user_notification
sys_webhook_config
sys_webhook_log
sys_dead_letter
```

#### 策略2: 共享数据库，按schema隔离

```sql
user_auth.sys_user
user_auth.sys_role
sys_config.sys_menu
notification.user_notification
```

#### 策略3: 单库按表名前缀区分

```sql
auth_sys_user
auth_sys_role
config_sys_menu
notify_user_notification
```

### 6.2 数据迁移步骤

#### Step 1: 导出数据
```bash
# 导出用户认证相关表
mysqldump -u root -p basebackend auth_sys_user auth_sys_role auth_sys_permission > user_auth.sql
```

#### Step 2: 创建新库
```sql
CREATE DATABASE user_auth;
CREATE DATABASE user_org;
CREATE DATABASE sys_config;
CREATE DATABASE notification;
```

#### Step 3: 导入数据
```bash
mysql -u root -p user_auth < user_auth.sql
```

#### Step 4: 更新数据源配置
```yaml
# 新服务的数据源配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/user_auth?useUnicode=true&characterEncoding=utf8
```

### 6.3 注意事项

⚠️ **事务一致性**:
- 跨库事务需要使用分布式事务（Seata）
- 建议采用最终一致性方案

⚠️ **外键关联**:
- 原有的跨模块外键需要移除
- 使用应用层关联查询

⚠️ **数据同步**:
- 迁移期间需要双写
- 迁移后需要校验数据一致性

---

## 七、服务间通信方案

### 7.1 同步调用（OpenFeign）

#### 用户服务调用认证服务验证权限

```java
// basebackend-user-organization-service
@FeignClient(name = "basebackend-user-auth-service", path = "/api/auth")
public interface AuthServiceClient {

    @GetMapping("/verify/{userId}")
    Result<UserInfo> verifyUser(@PathVariable("userId") Long userId);

    @GetMapping("/permissions/{userId}")
    Result<List<String>> getUserPermissions(@PathVariable("userId") Long userId);
}

// 使用
@RestController
public class UserController {

    @Autowired
    private AuthServiceClient authServiceClient;

    @GetMapping("/users")
    public Result<List<User>> getUsers() {
        // 调用认证服务验证权限
        authServiceClient.verifyUser(currentUserId);
        return userService.listUsers();
    }
}
```

### 7.2 异步消息（RocketMQ）

#### 通知服务异步发送通知

```java
// 发送方：用户服务
@Service
public class UserService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void createUser(User user) {
        userMapper.insert(user);
        // 异步发送通知
        rocketMQTemplate.convertAndSend("notification-topic", new UserCreatedEvent(user));
    }
}

// 接收方：通知服务
@Component
public class UserNotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @RocketMQMessageListener(topic = "notification-topic", consumerGroup = "notification-group")
    public void onMessage(UserCreatedEvent event) {
        notificationService.sendUserCreatedNotification(event.getUser());
    }
}
```

### 7.3 缓存策略

#### 用户权限缓存

```java
// 认证服务
@Service
public class AuthService {

    public UserInfo getUserInfo(Long userId) {
        // 从Redis缓存获取
        String key = "user:info:" + userId;
        UserInfo userInfo = redisTemplate.opsForValue().get(key);

        if (userInfo == null) {
            userInfo = loadFromDatabase(userId);
            redisTemplate.opsForValue().set(key, userInfo, Duration.ofMinutes(30));
        }

        return userInfo;
    }
}
```

---

## 八、监控与运维

### 8.1 服务健康检查

每个服务都需要实现健康检查端点：

```java
@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public HealthResult health() {
        return HealthResult.up()
            .withDetail("db", checkDatabase())
            .withDetail("redis", checkRedis())
            .withDetail("rocketmq", checkRocketMQ())
            .build();
    }
}
```

### 8.2 指标监控

使用Micrometer暴露指标：

```java
@Component
public class UserMetrics {

    private final Counter userCreateCounter;
    private final Timer userCreateTimer;

    public UserMetrics(MeterRegistry registry) {
        userCreateCounter = registry.counter("user.create.total");
        userCreateTimer = registry.timer("user.create.duration");
    }

    public void recordUserCreate(Duration duration) {
        userCreateCounter.increment();
        userCreateTimer.record(duration);
    }
}
```

### 8.3 日志追踪

使用SkyWalking或Zipkin进行链路追踪：

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@RequestParam("id") Long id) {
        // 自动生成追踪ID
        return userService.getUser(id);
    }
}
```

---

## 九、拆分后的部署架构

### 9.1 容器化部署

```yaml
# docker-compose.yml
version: '3.8'

services:
  gateway:
    image: basebackend/gateway:latest
    ports:
      - "8080:8080"

  user-auth-service:
    image: basebackend/user-auth-service:latest
    ports:
      - "8081:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      - mysql
      - redis

  user-org-service:
    image: basebackend/user-org-service:latest
    ports:
      - "8082:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      - mysql
      - redis

  system-config-service:
    image: basebackend/system-config-service:latest
    ports:
      - "8083:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      - mysql
      - redis

  notification-service:
    image: basebackend/notification-service:latest
    ports:
      - "8084:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      - mysql
      - redis
      - rocketmq

  file-storage-service:
    image: basebackend/file-storage-service:latest
    ports:
      - "8085:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      - mysql
      - minio

  observability-service:
    image: basebackend/observability-service:latest
    ports:
      - "8086:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
    depends_on:
      - prometheus

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123456
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7
    volumes:
      - redis-data:/data
```

### 9.2 Kubernetes部署

```yaml
# user-auth-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-auth-service
  template:
    metadata:
      labels:
        app: user-auth-service
    spec:
      containers:
        - name: user-auth-service
          image: basebackend/user-auth-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"

---
apiVersion: v1
kind: Service
metadata:
  name: user-auth-service
spec:
  selector:
    app: user-auth-service
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
```

---

## 十、总结与建议

### 10.1 拆分收益

| 方面 | 改进前 | 拆分后 | 提升 |
|------|--------|--------|------|
| **启动时间** | 30-60秒 | 5-10秒 | 5-10倍 |
| **内存占用** | 1-2GB | 200-500MB | 4倍 |
| **部署时间** | 5-10分钟 | 1-2分钟 | 5倍 |
| **扩展性** | 整体扩展 | 按服务扩展 | 独立扩展 |
| **故障影响** | 单点故障 | 故障隔离 | 局部影响 |
| **开发效率** | 代码耦合高 | 独立开发 | 并行开发 |

### 10.2 关键建议

#### ✅ **建议立即执行的**
1. 拆分OperationLogAspect和PermissionAspect到基础模块
2. 清理和整理Entity归属
3. 统一注解使用规范

#### ✅ **建议按阶段执行的**
1. 先拆分独立性强、可观测性服务
2. 再拆分核心的用户认证服务
3. 最后拆分业务相关性强的服务

#### ❌ **不建议拆分过早的**
1. 如果用户量<10万，建议先优化现有代码
2. 如果团队<5人，建议先完善现有架构
3. 如果基础设施不完善（缺少CI/CD、监控等），建议先完善基础设施

### 10.3 风险提示

⚠️ **数据一致性风险**
- 分布式事务复杂性增加
- 建议采用最终一致性而非强一致性

⚠️ **性能下降风险**
- 服务间调用增加网络开销
- 建议添加本地缓存和CDN

⚠️ **运维复杂度增加**
- 需要监控7个服务而非1个
- 建议完善监控告警系统

### 10.4 实施检查清单

- [ ] 完成OperationLogAspect和PermissionAspect迁移
- [ ] 完成所有注解迁移到基础模块
- [ ] 完成用户认证服务拆分和测试
- [ ] 完成用户组织服务拆分和测试
- [ ] 完成通知服务拆分和测试
- [ ] 完成文件存储服务拆分和测试
- [ ] 完成可观测性服务拆分和测试
- [ ] 完成系统配置服务拆分和测试
- [ ] 完成所有服务的容器化部署
- [ ] 完成监控告警配置
- [ ] 完成性能测试和优化
- [ ] 完成文档更新和团队培训

---

**文档版本**: v1.0
**最后更新**: 2025-11-17
**审核人**: 架构组

---

## 附录A: 详细的文件迁移清单

### A.1 用户认证与权限服务迁移文件

```bash
# Controllers
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/AuthController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/PermissionController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/SecurityController.java

# Services
basebackend-admin-api/src/main/java/com/basebackend/admin/service/AuthService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/AuthServiceImpl.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/PermissionService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/PermissionServiceImpl.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/SecurityService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/SecurityServiceImpl.java

# Entities
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysUser.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysRole.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysPermission.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysUserRole.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysRolePermission.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/User2FA.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/UserDevice.java

# DTOs
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/UserDTO.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/UserCreateDTO.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/UserQueryDTO.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/LoginRequest.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/LoginResponse.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/security/User2FADTO.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/security/UserDeviceDTO.java

# Mappers
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysUserMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysRoleMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysPermissionMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysUserRoleMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysRolePermissionMapper.xml

# Configs
basebackend-admin-api/src/main/java/com/basebackend/admin/config/AdminSecurityConfig.java
basebackend-admin-api/src/main/java/com/basebackend/admin/config/JwtConfig.java

# Annotations
basebackend-admin-api/src/main/java/com/basebackend/admin/annotation/RequiresPermission.java
basebackend-admin-api/src/main/java/com/basebackend/admin/annotation/RequiresRole.java

# Aspects (迁移到security模块)
basebackend-admin-api/src/main/java/com/basebackend/admin/aspect/PermissionAspect.java
```

### A.2 用户与组织服务迁移文件

```bash
# Controllers
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/DeptController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/DictController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/PreferenceController.java

# Services
basebackend-admin-api/src/main/java/com/basebackend/admin/service/DeptService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/DeptServiceImpl.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/DictService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/DictServiceImpl.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/PreferenceService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/PreferenceServiceImpl.java

# Entities
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysDept.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysDict.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysDictData.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/UserPreference.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysOperationLog.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/SysLoginLog.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/UserOperationLog.java

# DTOs
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/DeptDTO.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/UserPreferenceDTO.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/OperationLogDTO.java
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/LoginLogDTO.java

# Mappers
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysDeptMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysDictMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysDictDataMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysOperationLogMapper.xml
basebackend-admin-api/src/main/java/com/basebackend/admin/mapper/SysLoginLogMapper.xml

# Aspects (迁移到logging模块)
basebackend-admin-api/src/main/java/com/basebackend/admin/aspect/OperationLogAspect.java
```

### A.3 通知中心服务迁移文件

```bash
# Controllers
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/NotificationController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/messaging/DeadLetterController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/messaging/EventController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/messaging/MessageMonitorController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/messaging/WebhookConfigController.java
basebackend-admin-api/src/main/java/com/basebackend/admin/controller/messaging/WebhookLogController.java

# Services
basebackend-admin-api/src/main/java/com/basebackend/admin/service/NotificationService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/impl/NotificationServiceImpl.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/SSENotificationService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/messaging/WebhookConfigService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/messaging/WebhookLogService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/messaging/MessageMonitorService.java
basebackend-admin-api/src/main/java/com/basebackend/admin/service/messaging/DeadLetterService.java

# Entities
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/UserNotification.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/messaging/SysWebhookConfig.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/messaging/SysWebhookLog.java
basebackend-admin-api/src/main/java/com/basebackend/admin/entity/messaging/SysDeadLetter.java

# DTOs
basebackend-admin-api/src/main/java/com/basebackend/admin/dto/notification/UserNotificationDTO.java

# Consumer
basebackend-admin-api/src/main/java/com/basebackend/admin/consumer/NotificationConsumer.java

# Constants
basebackend-admin-api/src/main/java/com/basebackend/admin/constants/NotificationConstants.java
```

---

## 附录B: 测试策略

### B.1 单元测试

每个拆分后的服务都需要有完整的单元测试：

```java
// 示例：AuthService单元测试
@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void testLogin_Success() {
        // Given
        LoginRequest request = new LoginRequest("admin", "123456");

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertNotNull(response.getToken());
        assertEquals(200, response.getCode());
    }

    @Test
    void testLogin_Failed() {
        // Given
        LoginRequest request = new LoginRequest("admin", "wrong_password");

        // When & Then
        assertThrows(BusinessException.class, () -> {
            authService.login(request);
        });
    }
}
```

### B.2 集成测试

```java
// 测试服务间调用
@SpringBootTest
class UserAuthServiceIntegrationTest {

    @Autowired
    private AuthServiceClient authServiceClient;

    @Test
    void testVerifyUserPermission() {
        // 模拟用户验证
        Long userId = 1L;
        Result<UserInfo> result = authServiceClient.verifyUser(userId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
```

### B.3 端到端测试

```java
// 测试完整流程
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class E2ETest {

    @LocalServerPort
    private int port;

    @Test
    void testCompleteUserFlow() {
        // 1. 用户登录
        LoginResponse login = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            new LoginRequest("admin", "123456"),
            LoginResponse.class
        ).getBody();

        assertNotNull(login.getToken());

        // 2. 创建部门
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getToken());

        Dept dept = new Dept("测试部门");
        Dept created = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/depts",
            dept,
            Dept.class,
            headers
        ).getBody();

        assertNotNull(created.getId());

        // 3. 发送通知
        NotificationRequest notification = new NotificationRequest("新部门创建", created.getId());
        Result result = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/notifications",
            notification,
            Result.class,
            headers
        ).getBody();

        assertTrue(result.isSuccess());
    }
}
```

祝拆分顺利！🚀