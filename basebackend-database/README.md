# Database Enhancement Module

## 概述

basebackend-database 模块是一个企业级数据库增强模块，在 MyBatis Plus 的基础上提供了丰富的企业级功能，包括数据审计、多租户支持、数据安全、健康监控、动态数据源、故障转移、数据库迁移和 SQL 性能分析等能力。

## 核心特性

### 1. 数据审计系统
- ✅ 自动记录所有数据变更（INSERT/UPDATE/DELETE）
- ✅ 记录变更前后的数据差异
- ✅ 支持异步审计日志处理
- ✅ 自动归档和清理过期日志
- ✅ 灵活的审计日志查询接口

### 2. 多租户架构
- ✅ 支持共享数据库模式（SHARED_DB）
- ✅ 支持独立数据库模式（SEPARATE_DB）
- ✅ 支持独立 Schema 模式（SEPARATE_SCHEMA）
- ✅ 自动租户数据隔离
- ✅ 动态租户数据源切换

### 3. 数据安全
- ✅ 字段级 AES 加密
- ✅ 多种数据脱敏策略（手机号、身份证、银行卡等）
- ✅ 基于权限的数据可见性控制
- ✅ 日志自动脱敏

### 4. 健康监控
- ✅ 实时数据源健康检查
- ✅ 连接池监控和告警
- ✅ 慢查询日志记录
- ✅ 数据库性能指标收集

### 5. 动态数据源
- ✅ 运行时数据源切换
- ✅ 基于注解的数据源路由
- ✅ 支持嵌套数据源切换
- ✅ 读写分离支持

### 6. 高可用支持
- ✅ 自动故障检测
- ✅ 主库自动重连
- ✅ 从库故障转移
- ✅ 主库降级策略

### 7. 数据库迁移
- ✅ 集成 Flyway 版本管理
- ✅ 迁移失败自动回滚
- ✅ 数据迁移自动备份
- ✅ 生产环境确认机制

### 8. SQL 性能分析
- ✅ SQL 执行统计收集
- ✅ 性能指标分析
- ✅ SQL 执行计划分析
- ✅ 自动清理过期统计数据

## 快速开始

### 1. 添加依赖

在项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-database</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 配置文件

在 `application.yml` 中引入增强配置：

```yaml
spring:
  profiles:
    include: database-enhanced
```

或者直接在 `application.yml` 中配置：

```yaml
database:
  enhanced:
    # 启用审计系统
    audit:
      enabled: true
      async: true
      retention-days: 90
    
    # 启用健康监控
    health:
      enabled: true
      slow-query-threshold: 1000
```

### 3. 初始化数据库表

执行以下 SQL 脚本创建必要的表结构：

```sql
-- 审计日志表
CREATE TABLE sys_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operation_type VARCHAR(20) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    primary_key VARCHAR(100),
    before_data TEXT,
    after_data TEXT,
    changed_fields VARCHAR(500),
    operator_id BIGINT,
    operator_name VARCHAR(100),
    operator_ip VARCHAR(50),
    operate_time DATETIME NOT NULL,
    tenant_id VARCHAR(50),
    INDEX idx_table_name (table_name),
    INDEX idx_operate_time (operate_time),
    INDEX idx_tenant_id (tenant_id)
);

-- SQL 统计表
CREATE TABLE sys_sql_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sql_md5 VARCHAR(32) NOT NULL UNIQUE,
    sql_template TEXT NOT NULL,
    execute_count BIGINT DEFAULT 0,
    total_time BIGINT DEFAULT 0,
    avg_time BIGINT DEFAULT 0,
    max_time BIGINT DEFAULT 0,
    min_time BIGINT DEFAULT 0,
    fail_count BIGINT DEFAULT 0,
    last_execute_time DATETIME,
    INDEX idx_execute_count (execute_count),
    INDEX idx_avg_time (avg_time)
);
```

### 4. 使用示例

#### 数据审计

审计功能会自动记录所有数据变更，无需额外代码：

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    public void updateUser(User user) {
        // 自动记录审计日志
        userMapper.updateById(user);
    }
}
```

查询审计日志：

```java
@Autowired
private AuditLogService auditLogService;

// 查询某个表的审计日志
Page<AuditLog> logs = auditLogService.query(
    AuditLogQuery.builder()
        .tableName("sys_user")
        .startTime(LocalDateTime.now().minusDays(7))
        .endTime(LocalDateTime.now())
        .build()
);
```

#### 多租户

启用多租户后，所有查询和插入操作会自动添加租户过滤：

```java
// 设置租户上下文
TenantContext.setTenantId("tenant_001");

try {
    // 自动添加 WHERE tenant_id = 'tenant_001'
    List<User> users = userMapper.selectList(null);
    
    // 自动填充 tenant_id 字段
    User user = new User();
    user.setName("张三");
    userMapper.insert(user);
} finally {
    TenantContext.clear();
}
```

#### 数据加密

使用 `@Sensitive` 注解标记敏感字段：

```java
@Data
@TableName("sys_user")
public class User {
    private Long id;
    private String name;
    
    @Sensitive(type = SensitiveType.PHONE, encrypt = true)
    private String phone;
    
    @Sensitive(type = SensitiveType.ID_CARD, encrypt = true)
    private String idCard;
}
```

#### 动态数据源

使用 `@DS` 注解切换数据源：

```java
@Service
public class OrderService {
    
    @DS("order_db")
    public Order getOrder(Long id) {
        // 使用 order_db 数据源
        return orderMapper.selectById(id);
    }
    
    @DS("slave")
    public List<Order> listOrders() {
        // 使用从库查询
        return orderMapper.selectList(null);
    }
}
```

#### 健康检查

通过 Spring Boot Actuator 查看数据源健康状态：

```bash
curl http://localhost:8080/actuator/health/dataSource
```

响应示例：

```json
{
  "status": "UP",
  "details": {
    "master": {
      "connected": true,
      "responseTime": 5,
      "activeConnections": 2,
      "idleConnections": 8,
      "poolUsageRate": 0.2
    },
    "slave": {
      "connected": true,
      "responseTime": 3,
      "activeConnections": 1,
      "idleConnections": 9,
      "poolUsageRate": 0.1
    }
  }
}
```

## 详细文档

### 功能文档
- [数据审计使用指南](AUDIT_ARCHIVE_IMPLEMENTATION.md)
- [多租户使用指南](MULTI_TENANCY_USAGE.md)
- [数据加密使用指南](ENCRYPTION_USAGE.md)
- [数据脱敏使用指南](DATA_MASKING_USAGE.md)
- [权限控制使用指南](PERMISSION_CONTROL_USAGE.md)
- [健康监控使用指南](HEALTH_MONITORING_USAGE.md)
- [慢查询告警使用指南](SLOW_QUERY_AND_ALERT_IMPLEMENTATION.md)
- [动态数据源使用指南](NESTED_DATASOURCE_SWITCHING.md)
- [故障转移使用指南](FAILOVER_USAGE.md)
- [Flyway 迁移指南](FLYWAY_MIGRATION_GUIDE.md)
- [SQL 统计使用指南](SQL_STATISTICS_USAGE.md)

### 快速开始文档
- [健康监控快速开始](HEALTH_MONITORING_QUICK_START.md)
- [慢查询告警快速开始](SLOW_QUERY_ALERT_QUICK_START.md)
- [嵌套数据源快速开始](NESTED_DATASOURCE_QUICK_START.md)
- [故障转移快速开始](FAILOVER_QUICK_START.md)
- [Flyway 快速开始](FLYWAY_QUICK_START.md)
- [SQL 统计快速开始](SQL_STATISTICS_QUICK_START.md)

### 实现总结文档
- [健康监控实现总结](HEALTH_MONITORING_IMPLEMENTATION_SUMMARY.md)
- [嵌套数据源实现总结](NESTED_DATASOURCE_IMPLEMENTATION_SUMMARY.md)
- [故障转移实现总结](FAILOVER_IMPLEMENTATION_SUMMARY.md)
- [Flyway 实现总结](FLYWAY_IMPLEMENTATION_SUMMARY.md)
- [SQL 统计实现总结](SQL_STATISTICS_IMPLEMENTATION_SUMMARY.md)

## 配置说明

详细的配置说明请参考 [配置说明文档](CONFIG_GUIDE.md)

## 最佳实践

详细的最佳实践请参考 [最佳实践文档](BEST_PRACTICES.md)

## 性能考虑

### 审计系统
- 使用异步处理，对业务性能影响 < 5%
- 建议定期归档和清理审计日志
- 高并发场景建议使用消息队列解耦

### 加密解密
- 单字段加密耗时 < 1ms
- 建议对频繁访问的加密字段使用缓存
- 考虑使用硬件加密加速

### SQL 统计
- 高并发场景建议使用采样统计
- 定期清理过期统计数据
- 避免在生产环境启用执行计划分析

## 故障排查

### 常见问题

#### 1. 审计日志未记录

检查配置：
```yaml
database:
  enhanced:
    audit:
      enabled: true
```

检查表是否在排除列表中：
```yaml
database:
  enhanced:
    audit:
      excluded-tables:
        - sys_audit_log
```

#### 2. 租户过滤不生效

确保设置了租户上下文：
```java
TenantContext.setTenantId("tenant_001");
```

检查表是否在排除列表中。

#### 3. 数据源切换失败

检查数据源是否已配置：
```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/db1
        slave:
          url: jdbc:mysql://localhost:3306/db2
```

确保 `@DS` 注解的值与配置的数据源名称一致。

## 版本要求

- Java 17+
- Spring Boot 3.0+
- MyBatis Plus 3.5+
- MySQL 5.7+ / MySQL 8.0+

## 许可证

本项目采用 Apache License 2.0 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题，请联系开发团队。
