# Database 模块故障排查指南

## 常见问题

### 1. Bean 冲突：SqlStatisticsCollector

#### 问题描述
启动应用时报错：
```
Parameter 0 of method sqlStatisticsInterceptor in com.basebackend.database.config.MyBatisPlusConfig 
required a single bean, but 2 were found:
- sqlStatisticsCollector: defined in file [...]
- mySqlStatisticsCollector: defined by method 'sqlStatisticsCollector' in class path resource [...]
```

#### 原因分析
`SqlStatisticsCollector` 被定义了两次：
1. 在类上使用 `@Component` 注解
2. 在配置类中使用 `@Bean` 方法

这导致 Spring 容器中存在两个相同类型的 Bean，依赖注入时无法确定使用哪一个。

#### 解决方案
移除 `SqlStatisticsCollector` 类上的 `@Component` 注解，只保留配置类中的 `@Bean` 定义。

**修改前：**
```java
@Slf4j
@Component  // ❌ 移除这个注解
public class SqlStatisticsCollector {
    // ...
}
```

**修改后：**
```java
@Slf4j
public class SqlStatisticsCollector {
    // ...
}
```

配置类中保留 Bean 定义：
```java
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.sql-statistics", name = "enabled", havingValue = "true")
public class SqlStatisticsConfig {
    
    @Bean
    public SqlStatisticsCollector sqlStatisticsCollector(
            @Lazy SqlStatisticsMapper sqlStatisticsMapper,
            DatabaseEnhancedProperties properties) {
        return new SqlStatisticsCollector(sqlStatisticsMapper, properties);
    }
}
```

#### 预防措施
- 避免同时使用 `@Component` 和 `@Bean` 定义同一个类
- 如果需要条件化创建 Bean，优先使用 `@Bean` + `@ConditionalOnProperty`
- 使用 `@Primary` 注解标记主要的 Bean（如果确实需要多个实例）

---

### 2. 循环依赖：SqlStatisticsInterceptor

#### 问题描述
启动应用时报错：
```
Error creating bean with name 'sqlStatisticsInterceptor': 
Unsatisfied dependency expressed through method 'sqlStatisticsInterceptor' parameter 0: 
Error creating bean with name 'sqlStatisticsCollector': 
Unsatisfied dependency expressed through method 'sqlStatisticsCollector' parameter 0: 
Error creating bean with name 'sqlStatisticsMapper': 
Unsatisfied dependency expressed through bean property 'sqlSessionFactory': 
Error creating bean with name 'sqlSessionFactory': 
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

#### 原因分析
存在循环依赖：
1. `SqlSessionFactory` 创建时需要注册 `SqlStatisticsInterceptor`
2. `SqlStatisticsInterceptor` 依赖 `SqlStatisticsCollector`
3. `SqlStatisticsCollector` 依赖 `SqlStatisticsMapper`
4. `SqlStatisticsMapper` 需要 `SqlSessionFactory`

形成了循环：`SqlSessionFactory` → `SqlStatisticsInterceptor` → `SqlStatisticsCollector` → `SqlStatisticsMapper` → `SqlSessionFactory`

#### 解决方案
使用 `@Lazy` 注解打破循环依赖，延迟加载 `SqlStatisticsMapper`。

**修改配置类：**
```java
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.sql-statistics", name = "enabled", havingValue = "true")
public class SqlStatisticsConfig {
    
    /**
     * Use @Lazy to break circular dependency with SqlSessionFactory
     */
    @Bean
    public SqlStatisticsInterceptor sqlStatisticsInterceptor(
            @Lazy SqlStatisticsCollector sqlStatisticsCollector,
            DatabaseEnhancedProperties properties) {
        return new SqlStatisticsInterceptor(sqlStatisticsCollector, properties);
    }
    
    /**
     * Use @Lazy to break circular dependency with SqlSessionFactory
     */
    @Bean
    public SqlStatisticsCollector sqlStatisticsCollector(
            @Lazy SqlStatisticsMapper sqlStatisticsMapper,
            DatabaseEnhancedProperties properties) {
        return new SqlStatisticsCollector(sqlStatisticsMapper, properties);
    }
}
```

#### 工作原理
- `@Lazy` 注解使 Spring 创建一个代理对象，而不是立即实例化真实的 Bean
- 当第一次调用代理对象的方法时，才会真正创建 Bean
- 这样就打破了循环依赖链

#### 预防措施
- 设计时避免循环依赖
- 对于 MyBatis 拦截器，优先使用 `@Lazy` 注入 Mapper
- 考虑使用事件驱动或异步处理来解耦依赖关系

---

### 3. 循环依赖：TenantDataSourceConfig

#### 问题描述
启动应用时报错：
```
The dependencies of some of the beans in the application context form a cycle:
sqlSessionFactory → tenantDataSourceConfig → tenantConfigServiceImpl → tenantConfigMapper → sqlSessionFactory
```

#### 原因分析
存在循环依赖：
1. `SqlSessionFactory` 创建时需要 `TenantDataSourceConfig`
2. `TenantDataSourceConfig` 依赖 `TenantConfigService`
3. `TenantConfigService` 实现类依赖 `TenantConfigMapper`
4. `TenantConfigMapper` 需要 `SqlSessionFactory`

形成了循环：`SqlSessionFactory` → `TenantDataSourceConfig` → `TenantConfigService` → `TenantConfigMapper` → `SqlSessionFactory`

#### 解决方案
在 `TenantDataSourceConfig` 的构造函数中使用 `@Lazy` 注解延迟加载依赖的服务。

**修改前：**
```java
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "database.enhanced.multi-tenancy", name = "enabled", havingValue = "true")
public class TenantDataSourceConfig {
    
    private final DatabaseEnhancedProperties properties;
    private final TenantConfigService tenantConfigService;
    private final TenantDataSourceService tenantDataSourceService;
    // ...
}
```

**修改后：**
```java
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.multi-tenancy", name = "enabled", havingValue = "true")
public class TenantDataSourceConfig {
    
    private final DatabaseEnhancedProperties properties;
    private final TenantConfigService tenantConfigService;
    private final TenantDataSourceService tenantDataSourceService;
    
    /**
     * Constructor with @Lazy to break circular dependency with SqlSessionFactory
     */
    public TenantDataSourceConfig(
            DatabaseEnhancedProperties properties,
            @Lazy TenantConfigService tenantConfigService,
            @Lazy TenantDataSourceService tenantDataSourceService) {
        this.properties = properties;
        this.tenantConfigService = tenantConfigService;
        this.tenantDataSourceService = tenantDataSourceService;
    }
    // ...
}
```

#### 工作原理
- 移除 `@RequiredArgsConstructor` 注解，手动编写构造函数
- 在构造函数参数上使用 `@Lazy` 注解
- Spring 会创建代理对象，延迟真实 Bean 的实例化
- 打破循环依赖链

#### 预防措施
- 对于需要 Mapper 的配置类，优先使用 `@Lazy` 注入
- 考虑使用 `ApplicationReadyEvent` 事件在应用启动后初始化
- 避免在配置类构造函数中直接使用需要 SqlSessionFactory 的 Bean

---

### 4. MyBatis 拦截器未生效

#### 问题描述
配置的 MyBatis 拦截器（如审计、加密、SQL 统计等）没有生效。

#### 可能原因
1. 配置未启用
2. Bean 未正确注册
3. 拦截器顺序不正确

#### 解决方案

**检查配置：**
```yaml
database:
  enhanced:
    audit:
      enabled: true
    security:
      encryption:
        enabled: true
      masking:
        enabled: true
    health:
      enabled: true
    sql-statistics:
      enabled: true
```

**检查 Bean 注册：**
确保拦截器 Bean 已正确注册到 Spring 容器：
```java
@Bean
@ConditionalOnProperty(prefix = "database.enhanced.audit", name = "enabled", havingValue = "true")
public AuditInterceptor auditInterceptor(...) {
    return new AuditInterceptor(...);
}
```

**检查拦截器顺序：**
某些拦截器有执行顺序要求：
1. TenantInterceptor（租户拦截器）- 必须在分页插件之前
2. EncryptionInterceptor（加密拦截器）- 在保存时执行
3. DecryptionInterceptor（解密拦截器）- 在查询结果处理时执行
4. PermissionMaskingInterceptor（权限脱敏拦截器）- 在解密之后执行

---

### 5. 慢查询日志未记录

#### 问题描述
配置了慢查询阈值，但没有看到慢查询日志。

#### 解决方案

**检查配置：**
```yaml
database:
  enhanced:
    health:
      enabled: true
      slow-query-threshold: 1000  # 毫秒
```

**检查日志级别：**
```yaml
logging:
  level:
    com.basebackend.database.health: DEBUG
```

**手动测试：**
```java
@Autowired
private SlowQueryLogger slowQueryLogger;

public void test() {
    slowQueryLogger.logSlowQuery("SELECT * FROM user", 2000L, null);
}
```

---

### 6. SQL 统计数据不准确

#### 问题描述
SQL 统计的执行次数、平均时间等数据不准确。

#### 可能原因
1. 缓存未及时刷新到数据库
2. 并发更新导致数据不一致
3. 统计收集器配置不正确

#### 解决方案

**手动刷新缓存：**
```java
@Autowired
private SqlStatisticsCollector collector;

public void flushStatistics() {
    collector.flushCache();
}
```

**配置定时刷新：**
```yaml
database:
  enhanced:
    sql-statistics:
      enabled: true
      flush-interval: 300000  # 5分钟刷新一次
```

**检查并发控制：**
确保 `SqlStatisticsCollector` 中的更新操作使用了同步：
```java
private void updateStatistics(SqlStatistics statistics, SqlExecutionInfo executionInfo) {
    synchronized (statistics) {
        // 更新统计信息
    }
}
```

---

### 7. 数据加密/解密失败

#### 问题描述
保存或查询数据时，加密或解密操作失败。

#### 解决方案

**检查配置：**
```yaml
database:
  enhanced:
    security:
      encryption:
        enabled: true
        algorithm: AES
        secret-key: your-secret-key-here  # 必须配置
```

**检查字段注解：**
```java
public class User {
    @Encrypted
    private String password;
    
    @Encrypted
    private String idCard;
}
```

**检查加密服务：**
```java
@Autowired
private EncryptionService encryptionService;

public void test() {
    String encrypted = encryptionService.encrypt("test");
    String decrypted = encryptionService.decrypt(encrypted);
    assert "test".equals(decrypted);
}
```

---

### 8. 多租户数据隔离失效

#### 问题描述
不同租户可以看到彼此的数据。

#### 解决方案

**检查租户拦截器配置：**
```yaml
database:
  enhanced:
    tenant:
      enabled: true
      tenant-id-column: tenant_id
```

**检查租户上下文：**
```java
// 设置租户ID
TenantContext.setTenantId("tenant001");

try {
    // 执行数据库操作
} finally {
    // 清理租户上下文
    TenantContext.clear();
}
```

**检查表配置：**
确保需要租户隔离的表都包含租户ID字段：
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    INDEX idx_tenant_id (tenant_id)
);
```

---

### 9. 审计日志未记录

#### 问题描述
数据库操作没有生成审计日志。

#### 解决方案

**检查配置：**
```yaml
database:
  enhanced:
    audit:
      enabled: true
      log-select: false  # 是否记录查询操作
      log-insert: true
      log-update: true
      log-delete: true
```

**检查审计表：**
确保审计日志表已创建：
```sql
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operation_type VARCHAR(20),
    table_name VARCHAR(100),
    record_id VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    operator VARCHAR(100),
    operate_time DATETIME,
    ip_address VARCHAR(50)
);
```

**检查用户上下文：**
```java
// 设置当前用户信息
UserContext.setUserId("user001");
UserContext.setUsername("张三");

try {
    // 执行数据库操作
} finally {
    UserContext.clear();
}
```

---

## 性能优化建议

### 1. SQL 统计缓存优化
```yaml
database:
  enhanced:
    sql-statistics:
      cache-size: 1000  # 缓存大小
      cache-expire-minutes: 10  # 缓存过期时间
```

### 2. 慢查询阈值调整
```yaml
database:
  enhanced:
    health:
      slow-query-threshold: 1000  # 根据实际情况调整
```

### 3. 审计日志异步处理
```yaml
database:
  enhanced:
    audit:
      async: true  # 启用异步处理
      thread-pool-size: 10
```

### 4. 批量操作优化
使用批量插入/更新减少数据库交互：
```java
// 批量插入
sqlStatisticsMapper.batchInsert(statisticsList);

// 批量更新
sqlStatisticsMapper.batchUpdate(statisticsList);
```

---

## 调试技巧

### 1. 启用详细日志
```yaml
logging:
  level:
    com.basebackend.database: DEBUG
    com.baomidou.mybatisplus: DEBUG
```

### 2. 查看 Bean 注册情况
```java
@Autowired
private ApplicationContext context;

public void checkBeans() {
    String[] beanNames = context.getBeanNamesForType(SqlStatisticsCollector.class);
    for (String name : beanNames) {
        System.out.println("Bean: " + name);
    }
}
```

### 3. 监控 SQL 执行
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 4. 使用 Actuator 监控
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,beans
```

访问：
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/metrics
- http://localhost:8080/actuator/beans

---

## 联系支持

如果以上方法都无法解决问题，请：
1. 收集完整的错误日志
2. 记录复现步骤
3. 提供配置文件
4. 提交 Issue 或联系技术支持
