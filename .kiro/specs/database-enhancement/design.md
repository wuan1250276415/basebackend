# Design Document

## Overview

本设计文档描述了 basebackend-database 模块的增强方案。该模块将在现有 MyBatis Plus、读写分离、分布式事务的基础上，新增企业级数据库管理能力，包括：

- **数据审计系统**：完整记录数据变更历史，支持审计追溯
- **多租户架构**：支持共享数据库、独立数据库等多种隔离模式
- **数据安全**：提供字段级加密和多种脱敏策略
- **健康监控**：实时监控数据源状态、连接池、慢查询等指标
- **动态数据源**：支持运行时数据源切换和路由
- **高可用支持**：自动故障检测和转移
- **数据库迁移**：集成 Flyway 实现版本化管理
- **性能分析**：SQL 执行统计和性能优化建议

设计遵循以下原则：
1. **非侵入性**：通过拦截器、AOP 等方式实现，最小化对业务代码的影响
2. **可配置性**：所有功能均可通过配置开关控制
3. **高性能**：异步处理审计日志，使用缓存优化性能
4. **可扩展性**：提供 SPI 机制支持自定义扩展

## Architecture

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Application Layer                       │
│                    (Service/Controller)                      │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                   Database Module (Enhanced)                 │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Audit System │  │Multi-Tenancy │  │Data Security │     │
│  │  - Logging   │  │  - Isolation │  │  - Encrypt   │     │
│  │  - History   │  │  - Context   │  │  - Masking   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │Health Monitor│  │Dynamic DS    │  │ Migration    │     │
│  │  - Check     │  │  - Routing   │  │  - Flyway    │     │
│  │  - Metrics   │  │  - Switch    │  │  - Version   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                              │
│  ┌──────────────────────────────────────────────────┐      │
│  │         MyBatis Plus Interceptor Chain           │      │
│  │  - Tenant Filter                                 │      │
│  │  - Audit Interceptor                             │      │
│  │  - Encryption Interceptor                        │      │
│  │  - SQL Statistics                                │      │
│  └──────────────────────────────────────────────────┘      │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│              DataSource Management Layer                     │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐           │
│  │  Master DS │  │  Slave DS  │  │  Tenant DS │           │
│  └────────────┘  └────────────┘  └────────────┘           │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                    Database Servers                          │
│         (MySQL Master/Slave, Tenant Databases)              │
└─────────────────────────────────────────────────────────────┘
```

### 模块划分

#### 1. Audit Module (审计模块)
- `AuditInterceptor`: MyBatis 拦截器，捕获 SQL 执行
- `AuditLogService`: 审计日志服务，异步写入审计记录
- `AuditLogRepository`: 审计日志存储
- `AuditLogQuery`: 审计日志查询接口

#### 2. Multi-Tenancy Module (多租户模块)
- `TenantContext`: 租户上下文，存储当前租户 ID
- `TenantInterceptor`: 自动添加租户过滤条件
- `TenantDataSourceRouter`: 租户数据源路由器
- `TenantProperties`: 多租户配置

#### 3. Data Security Module (数据安全模块)
- `EncryptionService`: 加密服务接口
- `AESEncryptionService`: AES 加密实现
- `EncryptionInterceptor`: 字段加密拦截器
- `DataMaskingService`: 数据脱敏服务
- `@Sensitive`: 敏感字段注解

#### 4. Health Monitor Module (健康监控模块)
- `DataSourceHealthIndicator`: 数据源健康检查
- `ConnectionPoolMonitor`: 连接池监控
- `SlowQueryLogger`: 慢查询日志
- `DatabaseMetricsCollector`: 数据库指标收集器

#### 5. Dynamic DataSource Module (动态数据源模块)
- `DynamicDataSource`: 动态数据源实现
- `DataSourceRouter`: 数据源路由器
- `@DS`: 数据源切换注解
- `DataSourceContextHolder`: 数据源上下文

#### 6. Failover Module (故障转移模块)
- `DataSourceFailoverHandler`: 故障转移处理器
- `HealthCheckScheduler`: 定时健康检查
- `DataSourceRecoveryManager`: 数据源恢复管理

#### 7. Migration Module (迁移模块)
- `FlywayConfiguration`: Flyway 配置
- `MigrationService`: 迁移服务
- `MigrationHistoryRepository`: 迁移历史存储

#### 8. SQL Statistics Module (SQL 统计模块)
- `SqlStatisticsInterceptor`: SQL 统计拦截器
- `SqlStatisticsCollector`: SQL 统计收集器
- `SqlPerformanceAnalyzer`: SQL 性能分析器

## Components and Interfaces

### 1. Audit System

#### AuditInterceptor
```java
@Component
public class AuditInterceptor implements InnerInterceptor {
    
    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) {
        // 记录更新前的数据
        captureBeforeData(parameter);
    }
    
    @Override
    public void afterUpdate(Executor executor, MappedStatement ms, Object parameter, Object result) {
        // 记录更新后的数据和差异
        captureAfterData(parameter, result);
        // 异步写入审计日志
        auditLogService.logAsync(buildAuditLog());
    }
}
```

#### AuditLogService
```java
public interface AuditLogService {
    /**
     * 异步记录审计日志
     */
    void logAsync(AuditLog auditLog);
    
    /**
     * 查询审计日志
     */
    Page<AuditLog> query(AuditLogQuery query);
    
    /**
     * 归档过期日志
     */
    void archiveExpiredLogs(int retentionDays);
}
```

### 2. Multi-Tenancy System

#### TenantContext
```java
public class TenantContext {
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    
    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static String getTenantId() {
        return TENANT_ID.get();
    }
    
    public static void clear() {
        TENANT_ID.remove();
    }
}
```

#### TenantInterceptor
```java
@Component
public class TenantInterceptor implements InnerInterceptor {
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, 
                           RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // 自动添加 tenant_id 过滤条件
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new TenantContextException("Tenant context is not set");
        }
        // 修改 SQL 添加 WHERE tenant_id = ?
        modifySqlWithTenantFilter(boundSql, tenantId);
    }
}
```

### 3. Data Security System

#### EncryptionService
```java
public interface EncryptionService {
    /**
     * 加密数据
     */
    String encrypt(String plainText);
    
    /**
     * 解密数据
     */
    String decrypt(String cipherText);
}
```

#### DataMaskingService
```java
public interface DataMaskingService {
    /**
     * 脱敏手机号
     */
    String maskPhone(String phone);
    
    /**
     * 脱敏身份证号
     */
    String maskIdCard(String idCard);
    
    /**
     * 脱敏银行卡号
     */
    String maskBankCard(String bankCard);
    
    /**
     * 自定义脱敏规则
     */
    String mask(String data, MaskingRule rule);
}
```

#### @Sensitive Annotation
```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    /**
     * 敏感类型
     */
    SensitiveType type() default SensitiveType.CUSTOM;
    
    /**
     * 是否加密存储
     */
    boolean encrypt() default false;
    
    /**
     * 是否脱敏显示
     */
    boolean mask() default true;
}
```

### 4. Health Monitor System

#### DataSourceHealthIndicator
```java
@Component
public class DataSourceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        Map<String, DataSourceHealth> dataSourceHealthMap = new HashMap<>();
        
        for (DataSource ds : getAllDataSources()) {
            DataSourceHealth health = checkDataSource(ds);
            dataSourceHealthMap.put(ds.getName(), health);
        }
        
        return Health.up()
            .withDetail("dataSources", dataSourceHealthMap)
            .build();
    }
    
    private DataSourceHealth checkDataSource(DataSource ds) {
        // 检查连接、响应时间、连接池状态等
        return DataSourceHealth.builder()
            .connected(isConnected(ds))
            .responseTime(getResponseTime(ds))
            .activeConnections(getActiveConnections(ds))
            .idleConnections(getIdleConnections(ds))
            .build();
    }
}
```

### 5. Dynamic DataSource System

#### DynamicDataSource
```java
public class DynamicDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceKey();
    }
    
    /**
     * 添加数据源
     */
    public void addDataSource(String key, DataSource dataSource) {
        Map<Object, Object> targetDataSources = getResolvedDataSources();
        targetDataSources.put(key, dataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }
    
    /**
     * 移除数据源
     */
    public void removeDataSource(String key) {
        Map<Object, Object> targetDataSources = getResolvedDataSources();
        targetDataSources.remove(key);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }
}
```

#### @DS Annotation
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DS {
    /**
     * 数据源名称
     */
    String value();
}
```

## Data Models

### AuditLog (审计日志)
```java
@Data
@TableName("sys_audit_log")
public class AuditLog extends BaseEntity {
    /** 操作类型 (INSERT/UPDATE/DELETE) */
    private String operationType;
    
    /** 表名 */
    private String tableName;
    
    /** 主键值 */
    private String primaryKey;
    
    /** 变更前数据 (JSON) */
    private String beforeData;
    
    /** 变更后数据 (JSON) */
    private String afterData;
    
    /** 变更字段 */
    private String changedFields;
    
    /** 操作人 ID */
    private Long operatorId;
    
    /** 操作人姓名 */
    private String operatorName;
    
    /** 操作 IP */
    private String operatorIp;
    
    /** 操作时间 */
    private LocalDateTime operateTime;
    
    /** 租户 ID */
    private String tenantId;
}
```

### TenantConfig (租户配置)
```java
@Data
@TableName("sys_tenant_config")
public class TenantConfig extends BaseEntity {
    /** 租户 ID */
    private String tenantId;
    
    /** 租户名称 */
    private String tenantName;
    
    /** 隔离模式 (SHARED_DB/SEPARATE_DB/SEPARATE_SCHEMA) */
    private String isolationMode;
    
    /** 数据源键（独立数据库模式使用） */
    private String dataSourceKey;
    
    /** Schema 名称（独立 Schema 模式使用） */
    private String schemaName;
    
    /** 状态 (ACTIVE/INACTIVE) */
    private String status;
}
```

### DataSourceHealth (数据源健康状态)
```java
@Data
public class DataSourceHealth {
    /** 数据源名称 */
    private String name;
    
    /** 是否连接 */
    private boolean connected;
    
    /** 响应时间 (ms) */
    private long responseTime;
    
    /** 活跃连接数 */
    private int activeConnections;
    
    /** 空闲连接数 */
    private int idleConnections;
    
    /** 最大连接数 */
    private int maxConnections;
    
    /** 连接池使用率 */
    private double poolUsageRate;
    
    /** 最后检查时间 */
    private LocalDateTime lastCheckTime;
}
```

### SqlStatistics (SQL 统计)
```java
@Data
@TableName("sys_sql_statistics")
public class SqlStatistics {
    /** SQL 语句 (MD5) */
    private String sqlMd5;
    
    /** SQL 模板 */
    private String sqlTemplate;
    
    /** 执行次数 */
    private Long executeCount;
    
    /** 总执行时间 (ms) */
    private Long totalTime;
    
    /** 平均执行时间 (ms) */
    private Long avgTime;
    
    /** 最大执行时间 (ms) */
    private Long maxTime;
    
    /** 最小执行时间 (ms) */
    private Long minTime;
    
    /** 失败次数 */
    private Long failCount;
    
    /** 最后执行时间 */
    private LocalDateTime lastExecuteTime;
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Audit System Properties

**Property 1: Insert operations are fully audited**
*For any* entity insertion, the audit log should contain the complete inserted data and operation metadata (operator, timestamp, IP address).
**Validates: Requirements 1.1**

**Property 2: Update operations record data differences**
*For any* entity update, the audit log should contain both the before and after states, allowing reconstruction of what changed.
**Validates: Requirements 1.2**

**Property 3: Delete operations preserve deleted data**
*For any* entity deletion, the audit log should contain the complete data of the deleted entity.
**Validates: Requirements 1.3**

**Property 4: Audit log queries return complete records**
*For any* audit log query, all returned records should contain operation type, operation time, operator, table name, and change content.
**Validates: Requirements 1.4**

**Property 5: Expired audit logs are cleaned**
*For any* configured retention period, audit logs older than the retention period should be archived or removed, while newer logs are preserved.
**Validates: Requirements 1.5**

### Multi-Tenancy Properties

**Property 6: Queries are tenant-filtered**
*For any* database query with a valid tenant context, the returned data should only contain records belonging to the current tenant.
**Validates: Requirements 2.2**

**Property 7: Inserts are tenant-tagged**
*For any* entity insertion with a valid tenant context, the inserted record should automatically have the tenant ID field populated with the current tenant ID.
**Validates: Requirements 2.3**

**Property 8: Tenant data source switching**
*For any* tenant using separate database mode, switching the tenant context should result in queries being executed against the corresponding tenant's data source.
**Validates: Requirements 2.5**

### Data Security Properties

**Property 9: Sensitive fields are encrypted at rest**
*For any* entity with fields marked as sensitive, saving the entity should result in those fields being encrypted in the database.
**Validates: Requirements 3.1**

**Property 10: Encryption round-trip preserves data**
*For any* entity with encrypted sensitive fields, saving then retrieving the entity should return the original unencrypted values.
**Validates: Requirements 3.2**

**Property 11: Logs mask sensitive data**
*For any* log output containing sensitive data, the sensitive information should be masked according to the configured masking rules.
**Validates: Requirements 3.3**

**Property 12: Masking rules support common types**
*For any* sensitive data of type phone, ID card, or bank card, applying the corresponding masking rule should produce a properly masked result.
**Validates: Requirements 3.4**

**Property 13: Permissions control data visibility**
*For any* query of sensitive data, users with view permissions should receive unmasked data, while users without permissions should receive masked data.
**Validates: Requirements 3.5**

### Health Monitoring Properties

**Property 14: Connection failures are logged and alerted**
*For any* data source connection failure, the system should record an error log and trigger an alert notification.
**Validates: Requirements 4.2**

**Property 15: Health checks return complete status**
*For any* health check query, the response should include health status and performance metrics for all configured data sources.
**Validates: Requirements 4.3**

**Property 16: Slow queries are logged**
*For any* SQL execution, if the execution time exceeds the configured threshold, it should be recorded in the slow query log.
**Validates: Requirements 4.4**

**Property 17: Connection pool alerts trigger**
*For any* connection pool, when the usage rate exceeds the configured threshold, an alert notification should be triggered.
**Validates: Requirements 4.5**

### Dynamic DataSource Properties

**Property 18: Annotated methods use specified data source**
*For any* method annotated with @DS, during method execution, database operations should use the specified data source.
**Validates: Requirements 5.2**

**Property 19: Data source context is restored**
*For any* method that switches data source, after method completion, the data source context should be restored to the default data source.
**Validates: Requirements 5.3**

**Property 20: Nested data source switching**
*For any* nested method calls with different @DS annotations, each method should use its specified data source correctly.
**Validates: Requirements 5.5**

### Failover Properties

**Property 21: Failed master triggers reconnection**
*For any* master database connection failure, the system should automatically attempt to reconnect.
**Validates: Requirements 6.1**

**Property 22: Persistent master failure triggers degradation**
*For any* master database that remains unavailable beyond the configured threshold, the system should degrade to read-only mode if configured to do so.
**Validates: Requirements 6.2**

**Property 23: Failed slaves are removed from pool**
*For any* slave database connection failure, the failed node should be automatically removed from the available slave list.
**Validates: Requirements 6.3**

**Property 24: Recovered slaves rejoin pool**
*For any* slave database that recovers from failure, the node should be automatically added back to the available slave list.
**Validates: Requirements 6.4**

**Property 25: All slaves down routes to master**
*For any* read request when all slave databases are unavailable, the request should be routed to the master database.
**Validates: Requirements 6.5**

### Migration Properties

**Property 26: Failed migrations rollback**
*For any* migration script that fails during execution, all changes made by that migration should be rolled back and an error should be recorded.
**Validates: Requirements 7.2**

**Property 27: Migration history is complete**
*For any* migration history query, the response should include all successfully executed migrations.
**Validates: Requirements 7.3**

**Property 28: Data migrations create backups**
*For any* migration script that modifies data, a backup of the affected data should be created before execution.
**Validates: Requirements 7.4**

**Property 29: Production migrations require confirmation**
*For any* migration execution in production environment, an additional confirmation step should be required.
**Validates: Requirements 7.5**

### SQL Statistics Properties

**Property 30: SQL execution is tracked**
*For any* SQL execution, the system should record execution time, affected rows, and other statistical information.
**Validates: Requirements 8.1**

**Property 31: Statistics queries return sorted data**
*For any* SQL statistics query, the returned data should be sorted by the specified dimension (execution count, average time, etc.).
**Validates: Requirements 8.2**

**Property 32: Failed SQL is logged**
*For any* SQL execution failure, the system should record the failure reason and the complete SQL statement.
**Validates: Requirements 8.3**

**Property 33: SQL analysis provides execution plans**
*For any* SQL execution when analysis is enabled, the system should provide execution plan analysis.
**Validates: Requirements 8.4**

**Property 34: Expired statistics are cleaned**
*For any* configured retention period, statistics data older than the retention period should be automatically cleaned.
**Validates: Requirements 8.5**

## Error Handling

### 异常类型定义

```java
// 租户上下文异常
public class TenantContextException extends RuntimeException {
    public TenantContextException(String message) {
        super(message);
    }
}

// 数据源异常
public class DataSourceException extends RuntimeException {
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 加密解密异常
public class EncryptionException extends RuntimeException {
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}

// 审计异常
public class AuditException extends RuntimeException {
    public AuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 错误处理策略

1. **租户上下文缺失**
   - 抛出 `TenantContextException`
   - 拒绝数据操作
   - 记录警告日志

2. **数据源连接失败**
   - 记录错误日志
   - 触发告警
   - 尝试故障转移
   - 如果所有数据源都失败，抛出 `DataSourceException`

3. **加密解密失败**
   - 记录错误日志
   - 抛出 `EncryptionException`
   - 不影响其他字段的处理

4. **审计日志写入失败**
   - 记录错误日志
   - 不影响业务操作（异步处理）
   - 可配置是否在审计失败时阻止业务操作

5. **迁移脚本执行失败**
   - 自动回滚已执行的变更
   - 记录详细错误信息
   - 阻止应用启动（可配置）

## Testing Strategy

### 单元测试

使用 JUnit 5 和 Mockito 进行单元测试，覆盖以下场景：

1. **拦截器测试**
   - 测试 SQL 拦截和修改逻辑
   - 测试租户过滤条件注入
   - 测试加密解密拦截

2. **服务层测试**
   - 测试审计日志服务
   - 测试数据源路由逻辑
   - 测试健康检查逻辑

3. **工具类测试**
   - 测试加密解密工具
   - 测试数据脱敏工具
   - 测试 SQL 统计收集器

### 属性测试

使用 jqwik 进行基于属性的测试，验证系统的正确性属性：

**配置要求**：
- 每个属性测试至少运行 100 次迭代
- 使用 `@Property` 注解标记属性测试
- 每个测试必须明确引用设计文档中的属性编号

**测试标注格式**：
```java
/**
 * Feature: database-enhancement, Property 1: Insert operations are fully audited
 * Validates: Requirements 1.1
 */
@Property
void insertOperationsAreFullyAudited(@ForAll("entities") Entity entity) {
    // 测试实现
}
```

**属性测试覆盖**：

1. **审计系统属性测试**
   - Property 1: 插入操作完整审计
   - Property 2: 更新操作记录差异
   - Property 3: 删除操作保留数据
   - Property 4: 审计日志查询完整性
   - Property 5: 过期日志清理

2. **多租户属性测试**
   - Property 6: 查询租户过滤
   - Property 7: 插入租户标记
   - Property 8: 租户数据源切换

3. **数据安全属性测试**
   - Property 9: 敏感字段加密存储
   - Property 10: 加密往返一致性（round-trip）
   - Property 11: 日志脱敏
   - Property 12: 脱敏规则支持
   - Property 13: 权限控制数据可见性

4. **健康监控属性测试**
   - Property 14: 连接失败日志告警
   - Property 15: 健康检查完整性
   - Property 16: 慢查询日志
   - Property 17: 连接池告警

5. **动态数据源属性测试**
   - Property 18: 注解指定数据源
   - Property 19: 数据源上下文恢复
   - Property 20: 嵌套数据源切换

6. **故障转移属性测试**
   - Property 21: 主库重连
   - Property 22: 主库降级
   - Property 23: 从库移除
   - Property 24: 从库恢复
   - Property 25: 从库全挂路由主库

7. **迁移属性测试**
   - Property 26: 迁移失败回滚
   - Property 27: 迁移历史完整性
   - Property 28: 数据迁移备份
   - Property 29: 生产环境确认

8. **SQL 统计属性测试**
   - Property 30: SQL 执行追踪
   - Property 31: 统计查询排序
   - Property 32: 失败 SQL 日志
   - Property 33: SQL 分析执行计划
   - Property 34: 过期统计清理

### 集成测试

使用 Testcontainers 启动真实的 MySQL 容器进行集成测试：

1. **读写分离测试**
   - 验证主从切换
   - 验证负载均衡
   - 验证故障转移

2. **多租户集成测试**
   - 验证租户数据隔离
   - 验证租户数据源切换
   - 验证跨租户查询被阻止

3. **审计系统集成测试**
   - 验证审计日志完整性
   - 验证审计日志查询
   - 验证审计日志归档

4. **迁移集成测试**
   - 验证 Flyway 迁移执行
   - 验证迁移回滚
   - 验证迁移历史记录

### 性能测试

使用 JMH 进行性能基准测试：

1. **拦截器性能**
   - 测试拦截器对 SQL 执行的性能影响
   - 目标：拦截器开销 < 5%

2. **加密解密性能**
   - 测试字段加密解密的性能
   - 目标：单字段加密 < 1ms

3. **审计日志性能**
   - 测试异步审计日志写入的吞吐量
   - 目标：支持 10000+ TPS

## Configuration

### application-database-enhanced.yml

```yaml
# 数据库增强配置
database:
  enhanced:
    # 审计系统配置
    audit:
      enabled: true
      # 异步处理
      async: true
      # 线程池配置
      thread-pool:
        core-size: 2
        max-size: 5
        queue-capacity: 1000
      # 日志保留天数
      retention-days: 90
      # 排除的表（不记录审计日志）
      excluded-tables:
        - sys_audit_log
        - sys_sql_statistics
    
    # 多租户配置
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
    
    # 数据安全配置
    security:
      # 加密配置
      encryption:
        enabled: true
        # 加密算法: AES, SM4
        algorithm: AES
        # 密钥（生产环境应从密钥管理服务获取）
        secret-key: ${ENCRYPTION_KEY:your-secret-key-here}
      
      # 脱敏配置
      masking:
        enabled: true
        # 脱敏规则
        rules:
          phone: "***-****-####"  # 保留后4位
          id-card: "######********####"  # 保留前6位和后4位
          bank-card: "#### **** **** ####"  # 保留前4位和后4位
    
    # 健康监控配置
    health:
      enabled: true
      # 检查间隔（秒）
      check-interval: 30
      # 慢查询阈值（毫秒）
      slow-query-threshold: 1000
      # 连接池告警阈值（百分比）
      pool-usage-threshold: 80
    
    # 动态数据源配置
    dynamic-datasource:
      enabled: true
      # 默认数据源
      primary: master
      # 严格模式（数据源不存在时抛异常）
      strict: true
    
    # 故障转移配置
    failover:
      enabled: true
      # 重连尝试次数
      max-retry: 3
      # 重连间隔（毫秒）
      retry-interval: 5000
      # 主库降级（主库不可用时是否降级到只读）
      master-degradation: false
    
    # SQL 统计配置
    sql-statistics:
      enabled: true
      # 统计数据保留天数
      retention-days: 30
      # 是否启用执行计划分析
      explain-enabled: false

# Flyway 配置
spring:
  flyway:
    enabled: true
    # 迁移脚本位置
    locations: classpath:db/migration
    # 基线版本
    baseline-version: 1.0.0
    # 基线迁移
    baseline-on-migrate: true
    # 验证迁移
    validate-on-migrate: true
    # 生产环境需要确认
    out-of-order: false
```

## Implementation Notes

### 性能优化建议

1. **审计日志异步处理**
   - 使用独立线程池处理审计日志
   - 批量写入数据库
   - 考虑使用消息队列解耦

2. **加密字段缓存**
   - 对频繁访问的加密字段使用缓存
   - 使用 Caffeine 本地缓存
   - 设置合理的过期时间

3. **SQL 统计采样**
   - 高并发场景下使用采样统计
   - 避免每次 SQL 都记录统计
   - 使用滑动窗口算法

4. **健康检查优化**
   - 使用连接池的内置健康检查
   - 避免频繁的数据库查询
   - 使用缓存减少检查频率

### 安全注意事项

1. **加密密钥管理**
   - 不要在配置文件中硬编码密钥
   - 使用密钥管理服务（如 Vault）
   - 定期轮换密钥

2. **审计日志保护**
   - 审计日志表应有严格的访问控制
   - 考虑审计日志的加密存储
   - 定期备份审计日志

3. **租户隔离**
   - 确保租户 ID 不能被篡改
   - 在网关层验证租户身份
   - 记录跨租户访问尝试

### 兼容性考虑

1. **数据库版本**
   - 支持 MySQL 5.7+
   - 支持 MySQL 8.0+
   - 考虑其他数据库的适配（PostgreSQL, Oracle）

2. **Spring Boot 版本**
   - 基于 Spring Boot 3.x
   - 兼容 Spring Boot 2.7+

3. **MyBatis Plus 版本**
   - 基于 MyBatis Plus 3.5+
   - 确保拦截器链的兼容性
