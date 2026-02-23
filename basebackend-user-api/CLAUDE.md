[根目录](../../CLAUDE.md) > **basebackend-user-api**

# basebackend-user-api

## 模块职责

用户微服务，从 admin-api 拆分而来。负责用户CRUD、角色分配、认证授权等用户域功能。

## 入口与启动

- 独立微服务，含Dockerfile
- Nacos服务注册

## 数据模型

Mapper XML: SysUserMapper, SysRoleMapper, SysRoleMenuMapper, SysRoleResourceMapper, SysRoleListOperationMapper

## 测试与质量

1个测试: RoleServiceImplTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
