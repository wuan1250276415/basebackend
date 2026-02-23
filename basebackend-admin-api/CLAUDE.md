[根目录](../../CLAUDE.md) > **basebackend-admin-api**

# basebackend-admin-api

## 模块职责

管理后台API服务（单体模式），提供完整的RBAC权限管理、系统管理、消息集成、Nacos配置管理、可观测性、文件存储等功能。正在向 `user-api` + `system-api` 微服务架构拆分。

## 入口与启动

- **启动类**: `com.basebackend.admin.AdminApiApplication`
- **端口**: 8082
- **Profile**: dev / test / prod, 额外可选 seata / messaging / nacos / observability / minio
- **扫描包**: `com.basebackend.admin`, `com.basebackend.common`, `com.basebackend.jwt`, `com.basebackend.database`, `com.basebackend.cache`, `com.basebackend.logging`, `com.basebackend.observability`, `com.basebackend.messaging`, `com.basebackend.nacos`, `com.basebackend.file`

## 对外接口

### 核心控制器

| 控制器 | 路径前缀 | 职责 |
|--------|---------|------|
| AuthController | /api/auth | 登录/登出/刷新Token |
| ApplicationController | /api/application | 应用管理 |
| ApplicationResourceController | /api/application-resource | 应用资源管理 |
| DeptController | /api/dept | 部门管理(树形) |
| DictController | /api/dict | 字典管理 |
| MenuController | /api/menu | 菜单管理(树形) |
| PermissionController | /api/permission | 权限管理 |
| MonitorController | /api/monitor | 系统监控(服务器/在线用户/缓存) |
| LogController | /api/log | 操作日志/登录日志 |
| NotificationController | /api/notification | 通知中心 |
| PreferenceController | /api/preference | 用户偏好设置 |
| SecurityController | /api/security | 2FA/设备管理/安全设置 |
| FeatureToggleController | /api/feature-toggle | 功能开关管理 |
| OpenApiController | /api/open | 开放API |
| ListOperationController | /api/list-operation | 列表操作管理 |

### 消息集成

| 控制器 | 职责 |
|--------|------|
| EventController | 事件发布 |
| MessageMonitorController | 消息监控 |
| WebhookConfigController | Webhook配置 |
| WebhookLogController | Webhook日志 |
| DeadLetterController | 死信队列管理 |

### Nacos管理

| 控制器 | 职责 |
|--------|------|
| NacosConfigController | Nacos配置CRUD |
| NacosConfigHistoryController | 配置历史/回滚 |
| NacosGrayReleaseController | 灰度发布 |
| NacosServiceDiscoveryController | 服务发现 |

### 可观测性

| 控制器 | 职责 |
|--------|------|
| AlertController | 告警管理 |
| LogOController | 日志查询 |
| MetricsController | 指标查询 |
| TraceController | 链路追踪查询 |

### 文件存储

| 控制器 | 职责 |
|--------|------|
| FileController | 文件上传/下载/预览 |
| AdminFileController | 管理端文件管理 |
| BackupController | 备份管理 |

## 关键依赖与配置

- MyBatis Plus: `mapper-locations: classpath*:mapper/**/*.xml`, ASSIGN_ID主键, 逻辑删除
- Flyway: `db/migration/` 下 V1.0 ~ V1.11 迁移脚本
- RocketMQ: `192.168.66.126:9876`, producer-group: `basebackend-producer-group`
- Seata: AT模式, tx-service-group: `basebackend-seata-group`, 注册到Nacos
- Knife4j: 启用, 中文界面
- 邮件: Outlook SMTP

## 数据模型

### 核心实体

| 实体 | 表 | 说明 |
|------|-----|------|
| SysUser | sys_user | 用户 |
| SysRole | sys_role | 角色 |
| SysMenu | sys_menu | 菜单 |
| SysPermission | sys_permission | 权限 |
| SysDept | sys_dept | 部门 |
| SysDict / SysDictData | sys_dict / sys_dict_data | 字典 |
| SysApplication | sys_application | 应用 |
| SysApplicationResource | sys_application_resource | 应用资源 |
| SysLoginLog | sys_login_log | 登录日志 |
| SysOperationLog | sys_operation_log | 操作日志 |

### 关联实体

SysUserRole, SysRoleMenu, SysRolePermission, SysRoleResource, SysRoleDataPermission, SysRoleListOperation, SysListOperation

### 扩展实体

UserNotification, UserPreference, UserDevice, User2FA, UserOperationLog

### 消息实体

SysDeadLetter, SysWebhookConfig, SysWebhookLog

### Nacos实体

SysNacosConfig, SysNacosConfigHistory, SysNacosGrayConfig, SysNacosPublishTask, SysNacosService

### 存储实体

SysFileInfo, SysBackupRecord

## 测试与质量

当前该模块无单元测试。测试已迁移至拆分后的 `basebackend-system-api` 和 `basebackend-user-api`。

## 相关文件清单

- 入口: `src/main/java/com/basebackend/admin/AdminApiApplication.java`
- 配置: `src/main/resources/application.yml`, `application-*.yml`, `bootstrap.yml`
- 迁移: `src/main/resources/db/migration/V1.0__init_database.sql` ~ `V1.11__seata_integration.sql`
- Mapper XML: `src/main/resources/mapper/*.xml`
- Dockerfile: `Dockerfile`
- 邮件模板: `src/main/resources/templates/email/`

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
