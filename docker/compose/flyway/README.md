# Flyway Baseline

当前生效的 Flyway 主链目录是 `sql/`。
这组 SQL 旨在为 `docker/compose/services` 当前会起的
`basebackend-user-api`、`basebackend-system-api`、`basebackend-notification-service`
和 `basebackend-observability-service` 提供一套最小可行的 MySQL baseline。

## 目标范围

- 覆盖登录、刷新 token、当前用户信息、部门名称查询
- 覆盖字典、应用、应用资源、权限、登录日志、操作日志主链
- 覆盖通知中心 `user_notification`
- 覆盖可观测性 `alert_rule / alert_event`
- 覆盖事务消息 `sys_message_log`
- 为遗留 `sys_menu/sys_role_menu` 提供最小兼容层
- 默认管理员账号对齐当前联调约定：`admin/password`

## 文件说明

- `V1.0__basebackend_user_system_baseline_schema.sql`
  创建 `user-api/system-api` 主链依赖的核心表
- `V1.1__basebackend_user_system_baseline_seed.sql`
  写入最小 seed，包括部门、管理员、角色、权限、应用、资源、字典和兼容菜单
- `V1.2__create_user_notification_table.sql`
  补齐通知服务依赖的通知表
- `V1.3__init_alert_tables.sql`
  补齐可观测性依赖的告警表与默认规则
- `V1.4__create_sys_message_log_table.sql`
  补齐消息事务日志表

`core/` 和 `observability/` 目录下的旧草案文件已经不再接线，
当前只保留 `sql/` 作为实际执行目录。

## 关键假设

- 数据库版本为 MySQL 8.x
- 这份 baseline 优先保证当前代码主链能启动，不追求历史表结构的完全还原
- 关联表尽量采用唯一键和索引，不额外加外键，减少迁移顺序和数据修复成本
- `sys_application_resource` 是当前资源/动态路由主表，`sys_menu` 仅保留兼容用途
- `sys_permission` 中的 `*:*` 用于让 `admin` 角色覆盖
  `@RequiresPermission` 场景

## 建议接入方式

当前 `docker-compose` 的 Flyway runner 已经直接挂载本目录：

```yaml
volumes:
  - ./flyway/sql:/flyway/sql/local:ro
  - ../../basebackend-database/database-migration/src/main/resources/db/migration/mysql:/flyway/sql/shared:ro
```

然后执行：

```bash
docker-compose -f base/docker-compose.base.yml \
  -f docker-compose-flyway.yml \
  --env-file env/.env.dev up --abort-on-container-exit flyway
```
