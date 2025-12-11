# 多租户数据源路由实现总结

## 实现概述

本次实现完成了多租户数据源路由功能，支持三种租户隔离模式：
- **SHARED_DB**：共享数据库，通过 tenant_id 字段隔离
- **SEPARATE_DB**：独立数据库，每个租户使用独立的数据源
- **SEPARATE_SCHEMA**：独立 Schema，每个租户使用独立的 Schema

## 实现的组件

### 1. 核心路由器

**TenantDataSourceRouter.java**
- 继承 `AbstractRoutingDataSource`，实现动态数据源路由
- 根据租户配置自动路由到对应的数据源
- 支持 SEPARATE_SCHEMA 模式的 Schema 切换
- 提供数据源的动态添加和移除功能

主要方法：
- `determineCurrentLookupKey()`: 确定当前使用的数据源键
- `getConnection()`: 获取连接并处理 Schema 切换
- `addTenantDataSource()`: 动态添加租户数据源
- `removeTenantDataSource()`: 移除租户数据源

### 2. 数据源管理服务

**TenantDataSourceService.java** (接口)
- 定义租户数据源管理的标准接口

**TenantDataSourceServiceImpl.java** (实现)
- 实现租户数据源的注册、注销、验证等功能
- 支持应用启动时自动初始化所有激活租户的数据源
- 提供数据源刷新功能
- 自动创建和配置 DruidDataSource

主要方法：
- `registerTenantDataSource()`: 注册租户数据源
- `unregisterTenantDataSource()`: 注销租户数据源
- `initializeTenantDataSources()`: 初始化所有租户数据源
- `refreshTenantDataSource()`: 刷新租户数据源
- `validateTenantDataSource()`: 验证数据源连接

### 3. 配置类

**TenantDataSourceConfig.java**
- 配置租户数据源路由器
- 监听应用启动事件，自动初始化租户数据源
- 仅在多租户功能启用时生效

### 4. 工具类

**TenantDataSourceUtil.java**
- 提供便捷的租户上下文切换方法
- 自动管理租户上下文的设置和清理
- 支持在不同租户上下文中执行操作

主要方法：
- `executeWithTenant()`: 在指定租户上下文中执行操作
- `executeWithDefaultDataSource()`: 在默认数据源中执行操作

## 技术特点

### 1. 非侵入性设计
- 通过 Spring 的 `AbstractRoutingDataSource` 实现透明路由
- 业务代码无需感知数据源切换逻辑
- 自动集成到 MyBatis Plus 拦截器链

### 2. 动态数据源管理
- 支持运行时动态添加和移除租户数据源
- 无需重启应用即可添加新租户
- 自动验证数据源连接有效性

### 3. 多种隔离模式
- **SHARED_DB**: 适合小型租户，资源消耗低
- **SEPARATE_DB**: 适合大型租户，数据完全隔离
- **SEPARATE_SCHEMA**: 适合中型租户，平衡隔离性和资源消耗

### 4. 自动 Schema 切换
- SEPARATE_SCHEMA 模式下自动切换数据库 Schema
- 使用 MySQL 的 `USE` 语句实现
- 对业务代码完全透明

### 5. 完善的错误处理
- 租户配置不存在时抛出明确异常
- 租户未激活时拒绝访问
- 数据源连接失败时提供详细错误信息

## 配置示例

### 启用多租户功能

```yaml
database:
  enhanced:
    multi-tenancy:
      enabled: true
      isolation-mode: SHARED_DB
      tenant-column: tenant_id
      excluded-tables:
        - sys_tenant_config
        - sys_dict
```

### 租户配置示例

```sql
-- SHARED_DB 模式
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, status)
VALUES ('tenant-001', '租户A', 'SHARED_DB', 'ACTIVE');

-- SEPARATE_DB 模式
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, data_source_key, status)
VALUES ('tenant-002', '租户B', 'SEPARATE_DB', 'tenant_db_002', 'ACTIVE');

-- SEPARATE_SCHEMA 模式
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, schema_name, status)
VALUES ('tenant-003', '租户C', 'SEPARATE_SCHEMA', 'tenant_schema_003', 'ACTIVE');
```

## 使用示例

### 1. 设置租户上下文

```java
// 在拦截器中设置
TenantContext.setTenantId("tenant-001");

// 执行数据库操作（自动路由到对应数据源）
List<User> users = userMapper.selectList(null);

// 清除租户上下文
TenantContext.clear();
```

### 2. 使用工具类切换租户

```java
// 在指定租户上下文中执行
List<User> users = TenantDataSourceUtil.executeWithTenant("tenant-001", () -> {
    return userMapper.selectList(null);
});

// 在默认数据源中执行
TenantDataSourceUtil.executeWithDefaultDataSource(() -> {
    // 访问系统表
});
```

### 3. 动态注册租户数据源

```java
// 创建数据源
DruidDataSource dataSource = new DruidDataSource();
dataSource.setUrl("jdbc:mysql://localhost:3306/tenant_db");
dataSource.setUsername("root");
dataSource.setPassword("password");
dataSource.init();

// 注册数据源
tenantDataSourceService.registerTenantDataSource("tenant-001", dataSource);
```

## 与现有功能的集成

### 1. 与租户拦截器集成
- `TenantInterceptor` 自动添加 tenant_id 过滤条件（SHARED_DB 模式）
- 数据源路由在拦截器之前执行，确保使用正确的数据源

### 2. 与审计系统集成
- 审计日志会记录租户 ID
- 支持跨租户的审计日志查询

### 3. 与健康监控集成
- 可以监控每个租户数据源的健康状态
- 支持租户数据源的连接池监控

## 性能考虑

### 1. 数据源缓存
- 使用 `ConcurrentHashMap` 缓存租户数据源
- 避免重复创建数据源对象

### 2. Schema 切换开销
- SEPARATE_SCHEMA 模式下，每次获取连接都需要执行 `USE` 语句
- 开销很小（通常 < 1ms），但在高并发场景下需要注意

### 3. 连接池配置
- 每个租户数据源都有独立的连接池
- 需要根据租户数量合理配置连接池参数
- 建议监控总连接数，避免资源耗尽

## 安全考虑

### 1. 租户隔离
- SHARED_DB 模式依赖拦截器添加 tenant_id 过滤
- SEPARATE_DB 和 SEPARATE_SCHEMA 提供更强的隔离性

### 2. 租户验证
- 路由器会验证租户配置是否存在
- 检查租户状态是否为 ACTIVE
- 拒绝访问未激活的租户

### 3. SQL 注入防护
- Schema 名称使用反引号包裹
- 防止 SQL 注入攻击

## 后续优化建议

### 1. 数据源配置外部化
- 当前数据源配置硬编码在代码中
- 建议从配置中心或数据库读取完整的数据源配置

### 2. 连接池监控
- 添加每个租户数据源的连接池监控
- 实现连接池使用率告警

### 3. 数据源预热
- 应用启动时预热租户数据源连接池
- 避免首次访问时的延迟

### 4. 故障转移
- 为 SEPARATE_DB 模式添加主从切换支持
- 实现租户数据源的健康检查和自动恢复

### 5. 性能优化
- 考虑使用连接池复用（对于 SEPARATE_SCHEMA 模式）
- 优化 Schema 切换逻辑

## 测试建议

### 1. 单元测试
- 测试数据源路由逻辑
- 测试租户上下文切换
- 测试异常场景处理

### 2. 集成测试
- 使用 Testcontainers 启动多个 MySQL 实例
- 测试三种隔离模式的数据隔离性
- 测试动态添加和移除租户数据源

### 3. 性能测试
- 测试数据源切换的性能开销
- 测试高并发场景下的表现
- 测试连接池资源消耗

## 文档

详细使用文档请参考：
- `MULTI_TENANCY_USAGE.md` - 多租户使用指南
- `DATABASE_ENHANCEMENT_README.md` - 数据库增强功能总览

## 总结

本次实现完成了完整的多租户数据源路由功能，支持三种隔离模式，提供了灵活的数据源管理能力。实现遵循了非侵入性、可配置性、高性能的设计原则，与现有的审计、拦截器等功能无缝集成。

主要成果：
- ✅ 实现了租户数据源路由器
- ✅ 支持独立数据库模式（SEPARATE_DB）
- ✅ 支持独立 Schema 模式（SEPARATE_SCHEMA）
- ✅ 实现了租户数据源动态切换
- ✅ 提供了完善的数据源管理服务
- ✅ 编写了详细的使用文档

符合需求 2.5：WHEN 使用独立数据库模式 WHEN 切换租户时 THEN Database Module SHALL 动态切换到对应租户的数据源
