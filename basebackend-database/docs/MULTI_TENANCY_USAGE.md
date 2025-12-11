# 多租户数据源路由使用指南

## 概述

多租户数据源路由功能支持三种租户隔离模式：

1. **SHARED_DB（共享数据库）**：所有租户共享同一个数据库，通过 `tenant_id` 字段进行数据隔离
2. **SEPARATE_DB（独立数据库）**：每个租户使用独立的数据库，通过动态数据源路由
3. **SEPARATE_SCHEMA（独立 Schema）**：每个租户使用独立的 Schema，共享数据库实例

## 配置

### 1. 启用多租户功能

在 `application.yml` 中配置：

```yaml
database:
  enhanced:
    multi-tenancy:
      enabled: true
      # 隔离模式: SHARED_DB, SEPARATE_DB, SEPARATE_SCHEMA
      isolation-mode: SHARED_DB
      # 租户字段名
      tenant-column: tenant_id
      # 排除的表（不添加租户过滤）
      excluded-tables:
        - sys_tenant_config
        - sys_dict
```

### 2. 配置租户信息

在数据库中配置租户信息（`sys_tenant_config` 表）：

```sql
-- SHARED_DB 模式示例
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, status)
VALUES ('tenant-001', '租户A', 'SHARED_DB', 'ACTIVE');

-- SEPARATE_DB 模式示例
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, data_source_key, status)
VALUES ('tenant-002', '租户B', 'SEPARATE_DB', 'tenant_db_002', 'ACTIVE');

-- SEPARATE_SCHEMA 模式示例
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, schema_name, status)
VALUES ('tenant-003', '租户C', 'SEPARATE_SCHEMA', 'tenant_schema_003', 'ACTIVE');
```

## 使用方式

### 1. 设置租户上下文

在处理请求时，首先设置租户上下文：

```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头或 Token 中获取租户 ID
        String tenantId = request.getHeader("X-Tenant-Id");
        
        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 清除租户上下文，避免内存泄漏
        TenantContext.clear();
    }
}
```

### 2. 自动数据源路由

设置租户上下文后，所有数据库操作会自动路由到对应的数据源：

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public List<User> listUsers() {
        // 自动使用当前租户的数据源
        // SHARED_DB: 自动添加 tenant_id 过滤条件
        // SEPARATE_DB: 路由到租户专属数据源
        // SEPARATE_SCHEMA: 切换到租户专属 Schema
        return userMapper.selectList(null);
    }
}
```

### 3. 手动切换租户上下文

使用工具类在不同租户上下文中执行操作：

```java
@Service
public class CrossTenantService {
    
    @Autowired
    private UserMapper userMapper;
    
    public void processMultipleTenants() {
        // 在租户 A 的上下文中执行
        List<User> usersA = TenantDataSourceUtil.executeWithTenant("tenant-001", () -> {
            return userMapper.selectList(null);
        });
        
        // 在租户 B 的上下文中执行
        List<User> usersB = TenantDataSourceUtil.executeWithTenant("tenant-002", () -> {
            return userMapper.selectList(null);
        });
        
        // 在默认数据源中执行（不使用租户隔离）
        TenantDataSourceUtil.executeWithDefaultDataSource(() -> {
            // 访问系统表或公共数据
            // ...
        });
    }
}
```

### 4. 动态注册租户数据源

对于 SEPARATE_DB 模式，可以动态注册新租户的数据源：

```java
@Service
public class TenantManagementService {
    
    @Autowired
    private TenantDataSourceService tenantDataSourceService;
    
    @Autowired
    private TenantConfigService tenantConfigService;
    
    public void createNewTenant(String tenantId, String dbName) {
        // 1. 创建租户配置
        TenantConfig config = new TenantConfig();
        config.setTenantId(tenantId);
        config.setTenantName("新租户");
        config.setIsolationMode("SEPARATE_DB");
        config.setDataSourceKey(dbName);
        config.setStatus("ACTIVE");
        tenantConfigService.save(config);
        
        // 2. 创建数据源
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/" + dbName);
        dataSource.setUsername("root");
        dataSource.setPassword("password");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.init();
        
        // 3. 注册数据源
        tenantDataSourceService.registerTenantDataSource(tenantId, dataSource);
    }
    
    public void removeTenant(String tenantId) {
        // 1. 注销数据源
        tenantDataSourceService.unregisterTenantDataSource(tenantId);
        
        // 2. 删除租户配置
        tenantConfigService.delete(tenantId);
    }
}
```

### 5. 验证数据源连接

```java
@Service
public class TenantHealthService {
    
    @Autowired
    private TenantDataSourceService tenantDataSourceService;
    
    public boolean checkTenantHealth(String tenantId) {
        return tenantDataSourceService.validateTenantDataSource(tenantId);
    }
    
    public void refreshTenantDataSource(String tenantId) {
        tenantDataSourceService.refreshTenantDataSource(tenantId);
    }
}
```

## 三种隔离模式对比

| 特性 | SHARED_DB | SEPARATE_DB | SEPARATE_SCHEMA |
|------|-----------|-------------|-----------------|
| 数据隔离 | 字段级 | 数据库级 | Schema 级 |
| 资源消耗 | 低 | 高 | 中 |
| 扩展性 | 一般 | 好 | 好 |
| 管理复杂度 | 低 | 高 | 中 |
| 性能 | 好 | 最好 | 好 |
| 适用场景 | 小型租户 | 大型租户 | 中型租户 |

## 注意事项

### 1. 租户上下文管理

- **必须清除**：在请求结束时必须调用 `TenantContext.clear()`，避免内存泄漏
- **线程安全**：`TenantContext` 使用 `ThreadLocal`，在异步操作中需要手动传递租户 ID

### 2. SEPARATE_DB 模式

- 需要预先创建租户数据库
- 需要配置数据源连接信息
- 注意连接池资源消耗

### 3. SEPARATE_SCHEMA 模式

- 需要预先创建租户 Schema
- MySQL 中 Schema 等同于 Database
- 切换 Schema 会有轻微性能开销

### 4. 排除表配置

某些表不需要租户隔离（如系统配置表），需要在配置中排除：

```yaml
database:
  enhanced:
    multi-tenancy:
      excluded-tables:
        - sys_tenant_config
        - sys_dict
        - sys_config
```

## 最佳实践

### 1. 租户 ID 来源

推荐从以下位置获取租户 ID：
- JWT Token 中的租户声明
- HTTP 请求头（如 `X-Tenant-Id`）
- 子域名（如 `tenant001.example.com`）

### 2. 异步操作

在异步操作中需要手动传递租户上下文：

```java
@Service
public class AsyncService {
    
    @Async
    public void asyncOperation() {
        String tenantId = TenantContext.getTenantId();
        
        CompletableFuture.runAsync(() -> {
            try {
                TenantContext.setTenantId(tenantId);
                // 执行异步操作
            } finally {
                TenantContext.clear();
            }
        });
    }
}
```

### 3. 数据源初始化

应用启动时会自动初始化所有激活租户的数据源。如果初始化失败，不会影响应用启动，但该租户将无法使用。

### 4. 监控和告警

建议监控以下指标：
- 租户数据源连接状态
- 租户数据源连接池使用率
- 租户切换失败次数

## 故障排查

### 1. 租户上下文未设置

**错误**：`TenantContextException: Tenant context is not set`

**原因**：未设置租户上下文就执行数据库操作

**解决**：确保在拦截器或过滤器中设置租户上下文

### 2. 租户配置不存在

**错误**：`TenantContextException: Tenant config not found`

**原因**：租户配置未在数据库中创建

**解决**：在 `sys_tenant_config` 表中添加租户配置

### 3. 数据源连接失败

**错误**：`TenantContextException: Failed to validate data source`

**原因**：租户数据源配置错误或数据库不可用

**解决**：
1. 检查租户数据源配置
2. 验证数据库连接信息
3. 确保数据库已创建

### 4. Schema 切换失败

**错误**：`TenantContextException: Failed to switch schema`

**原因**：Schema 不存在或权限不足

**解决**：
1. 确保 Schema 已创建
2. 检查数据库用户权限

## 示例代码

完整的示例代码请参考：
- `TenantDataSourceRouter.java` - 数据源路由器
- `TenantDataSourceService.java` - 数据源管理服务
- `TenantDataSourceUtil.java` - 工具类
- `TenantContext.java` - 租户上下文
