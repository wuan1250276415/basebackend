[根目录](../../CLAUDE.md) > **basebackend-system-api**

# basebackend-system-api

## 模块职责

系统管理微服务，从 admin-api 拆分而来。负责部门、字典、应用、权限、菜单、日志、监控等系统域功能。

## 入口与启动

- 独立微服务，含Dockerfile
- Nacos服务注册

## 数据模型

Mapper XML: SysDeptMapper, SysDictDataMapper, SysApplicationMapper, SysApplicationResourceMapper, SysRoleMapper, SysRoleResourceMapper, SysPermissionMapper, SysDictMapper

## 测试与质量

12+个测试，覆盖 Controller 和 Service 两层:
- Controller: ApplicationControllerTest, DeptControllerTest, DictControllerTest, LogControllerTest, MonitorControllerTest, PermissionControllerTest
- Service: ApplicationServiceTest, DictServiceTest, LogServiceTest, MonitorServiceTest
- 基础: BaseServiceTest, BaseControllerTest, BaseWebMvcTest, TestSecurityConfig
- 测试数据: `src/test/resources/schema.sql`, `data.sql`

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
