[根目录](../../CLAUDE.md) > **basebackend-database**

# basebackend-database

## 模块职责

数据库基础设施库。动态多数据源切换、读写分离、分库分表(ShardingSphere)、审计日志、健康监控、SQL统计、数据脱敏。

## 对外接口

- 动态数据源切换注解与API
- 数据源上下文 `DataSourceContextHolder`
- 健康监控端点
- 权限控制集成

## 关键依赖

- MyBatis Plus 3.5.5
- Druid 1.2.20
- Flyway 9.22.3
- ShardingSphere 5.4.1

## 数据模型

迁移脚本: `V1.0.1` ~ `V3.0` (审计日志、归档、租户配置、迁移备份、SQL统计、统一备份)

## 测试与质量

5个测试类:
- DynamicDataSourceTest, DataSourceContextHolderTest, NestedDataSourceSwitchingTest
- NestedDataSourceIntegrationTest, HealthMonitoringIntegrationTest, PermissionControlIntegrationTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
