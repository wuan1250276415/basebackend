# 循环依赖临时解决方案

## 问题描述

在启动 user-api 服务时，遇到了与 database 模块增强功能相关的循环依赖问题。

## 临时解决方案

在 `application.yml` 中禁用了以下功能：

```yaml
database:
  enhanced:
    # Disable multi-tenancy to avoid circular dependency
    multi-tenancy:
      enabled: false
    # Disable SQL statistics to avoid circular dependency
    sql-statistics:
      enabled: false
```

## 原因分析

### 1. 多租户功能循环依赖

```
SqlSessionFactory → TenantDataSourceRouter → TenantConfigService → TenantConfigMapper → SqlSessionFactory
```

### 2. SQL 统计功能循环依赖

```
SqlSessionFactory → SqlStatisticsInterceptor → SqlStatisticsCollector → SqlStatisticsMapper → SqlSessionFactory
```

## 长期解决方案

### 方案 1：重构租户数据源路由器

将 `TenantDataSourceRouter` 的初始化延迟到应用启动后：

```java
@Bean
public TenantDataSourceRouter tenantDataSourceRouter(
        DataSource defaultDataSource,
        @Lazy TenantConfigService tenantConfigService) {
    TenantDataSourceRouter router = new TenantDataSourceRouter(tenantConfigService, properties);
    router.setDefaultTargetDataSource(defaultDataSource);
    return router;
}
```

### 方案 2：使用 ObjectProvider

使用 Spring 的 `ObjectProvider` 延迟获取依赖：

```java
@Bean
public TenantDataSourceRouter tenantDataSourceRouter(
        DataSource defaultDataSource,
        ObjectProvider<TenantConfigService> tenantConfigServiceProvider) {
    TenantDataSourceRouter router = new TenantDataSourceRouter(
        tenantConfigServiceProvider.getObject(), properties);
    router.setDefaultTargetDataSource(defaultDataSource);
    return router;
}
```

### 方案 3：分离配置和运行时

将配置加载和运行时路由分离：

1. 配置加载使用静态配置文件或环境变量
2. 运行时动态更新使用事件监听

## 启用功能的步骤

当需要启用这些功能时：

### 1. 确保循环依赖已解决

检查 database 模块的相关配置类是否已正确使用 `@Lazy` 注解。

### 2. 更新配置

在 `application.yml` 中启用功能：

```yaml
database:
  enhanced:
    multi-tenancy:
      enabled: true
      tenant-id-column: tenant_id
    sql-statistics:
      enabled: true
      flush-interval: 300000
```

### 3. 测试启动

```bash
mvn spring-boot:run -pl basebackend-user-api
```

### 4. 验证功能

- 检查日志中是否有相关功能的初始化信息
- 测试多租户数据隔离
- 查看 SQL 统计数据

## 影响评估

### 禁用多租户功能

**影响：**
- 无法实现租户数据隔离
- 所有租户共享同一个数据源

**适用场景：**
- 单租户应用
- 开发和测试环境
- 不需要数据隔离的场景

### 禁用 SQL 统计功能

**影响：**
- 无法收集 SQL 执行统计
- 无法分析慢查询
- 无法进行性能优化

**适用场景：**
- 开发环境
- 性能要求不高的场景
- 已有其他监控方案

## 替代方案

### 多租户功能替代

1. **应用层隔离**：在业务代码中手动添加租户 ID 过滤
2. **数据库视图**：为每个租户创建视图
3. **独立数据库**：每个租户使用独立的数据库

### SQL 统计功能替代

1. **数据库慢查询日志**：使用 MySQL 的慢查询日志
2. **APM 工具**：使用 SkyWalking、Pinpoint 等
3. **数据库监控**：使用 Prometheus + Grafana

## 注意事项

1. **生产环境**：不建议在生产环境禁用这些功能
2. **性能影响**：启用这些功能会有一定的性能开销
3. **配置管理**：确保不同环境使用不同的配置
4. **监控告警**：启用功能后需要配置相应的监控和告警

## 相关文档

- [CIRCULAR_DEPENDENCY_FIX_SUMMARY.md](../basebackend-database/CIRCULAR_DEPENDENCY_FIX_SUMMARY.md)
- [TROUBLESHOOTING.md](../basebackend-database/TROUBLESHOOTING.md)
- [MULTI_TENANCY_USAGE.md](../basebackend-database/MULTI_TENANCY_USAGE.md)
- [SQL_STATISTICS_USAGE.md](../basebackend-database/SQL_STATISTICS_USAGE.md)
