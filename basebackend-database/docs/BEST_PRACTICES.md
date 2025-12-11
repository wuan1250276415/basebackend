# 最佳实践文档

本文档提供 basebackend-database 模块的最佳实践指南，帮助您在生产环境中正确、高效地使用各项功能。

## 目录

- [审计系统最佳实践](#审计系统最佳实践)
- [多租户最佳实践](#多租户最佳实践)
- [数据安全最佳实践](#数据安全最佳实践)
- [健康监控最佳实践](#健康监控最佳实践)
- [动态数据源最佳实践](#动态数据源最佳实践)
- [故障转移最佳实践](#故障转移最佳实践)
- [数据库迁移最佳实践](#数据库迁移最佳实践)
- [SQL 性能优化最佳实践](#sql-性能优化最佳实践)
- [性能优化建议](#性能优化建议)
- [安全建议](#安全建议)

## 审计系统最佳实践

### 1. 使用异步处理

**推荐做法：**
```yaml
database:
  enhanced:
    audit:
      enabled: true
      async: true  # 启用异步处理
      thread-pool:
        core-size: 2
        max-size: 5
        queue-capacity: 1000
```

**原因：**
- 异步处理不会阻塞业务操作
- 对业务性能影响 < 5%
- 提高系统吞吐量

### 2. 合理设置保留期限

**推荐做法：**
```yaml
database:
  enhanced:
    audit:
      retention-days: 90  # 主表保留 3 个月
      archive:
        enabled: true
        archive-retention-days: 365  # 归档表保留 1 年
```

**原因：**
- 避免审计日志表过大影响性能
- 满足合规要求的同时控制存储成本
- 归档表可以使用更便宜的存储

### 3. 排除不需要审计的表

**推荐做法：**
```yaml
database:
  enhanced:
    audit:
      excluded-tables:
        - sys_audit_log
        - sys_audit_log_archive
        - sys_sql_statistics
        - sys_log
        - sys_cache
```

**原因：**
- 减少不必要的审计日志
- 避免循环审计（审计日志表本身不需要审计）
- 提高性能

### 4. 定期归档和清理

**推荐做法：**
```yaml
database:
  enhanced:
    audit:
      archive:
        enabled: true
        cleanup-cron: "0 0 2 * * ?"  # 每天凌晨 2 点执行
        auto-cleanup-enabled: true
```

**原因：**
- 自动化维护，减少人工干预
- 在业务低峰期执行，减少影响
- 保持数据库性能

### 5. 监控审计日志队列

**推荐做法：**
```java
@Component
public class AuditQueueMonitor {
    @Scheduled(fixedRate = 60000)
    public void monitorQueue() {
        int queueSize = auditLogService.getQueueSize();
        if (queueSize > 800) {
            // 告警：队列积压
            alertService.sendAlert("审计日志队列积压: " + queueSize);
        }
    }
}
```

**原因：**
- 及时发现审计日志处理瓶颈
- 避免队列溢出导致审计日志丢失

## 多租户最佳实践

### 1. 选择合适的隔离模式

**场景分析：**

| 场景 | 推荐模式 | 原因 |
|------|---------|------|
| 租户数量多（>1000），数据量小 | SHARED_DB | 成本低，管理简单 |
| 租户数量少（<100），数据量大 | SEPARATE_DB | 性能好，隔离性强 |
| 中等规模，需要灵活性 | SEPARATE_SCHEMA | 平衡性能和成本 |

### 2. 始终使用 try-finally 清理租户上下文

**推荐做法：**
```java
@Service
public class OrderService {
    public void processOrder(String tenantId, Long orderId) {
        TenantContext.setTenantId(tenantId);
        try {
            // 业务逻辑
            Order order = orderMapper.selectById(orderId);
            // ...
        } finally {
            TenantContext.clear();  // 必须清理
        }
    }
}
```

**原因：**
- 避免租户上下文泄露
- 防止线程池复用导致的租户数据混乱
- 确保线程安全

### 3. 在网关层设置租户上下文

**推荐做法：**
```java
@Component
public class TenantFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String tenantId = req.getHeader("X-Tenant-Id");
        
        if (tenantId == null) {
            throw new TenantContextException("Missing tenant ID");
        }
        
        TenantContext.setTenantId(tenantId);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
```

**原因：**
- 统一的租户上下文管理
- 避免在每个服务中重复设置
- 提高安全性

### 4. 为租户配置独立的连接池

**推荐做法（SEPARATE_DB 模式）：**
```yaml
spring:
  datasource:
    dynamic:
      datasource:
        tenant_001:
          url: jdbc:mysql://localhost:3306/tenant_001_db
          hikari:
            maximum-pool-size: 10
            minimum-idle: 2
        tenant_002:
          url: jdbc:mysql://localhost:3306/tenant_002_db
          hikari:
            maximum-pool-size: 20  # VIP 租户更大的连接池
            minimum-idle: 5
```

**原因：**
- 为不同租户提供差异化服务
- 避免某个租户占用过多资源
- 提高系统稳定性

## 数据安全最佳实践

### 1. 密钥管理

**推荐做法：**
```yaml
database:
  enhanced:
    security:
      encryption:
        enabled: true
        secret-key: ${ENCRYPTION_KEY}  # 从环境变量获取
```

**部署时设置环境变量：**
```bash
# 使用密钥管理服务
export ENCRYPTION_KEY=$(vault kv get -field=key secret/database/encryption)

# 或使用 Kubernetes Secret
kubectl create secret generic db-encryption-key \
  --from-literal=key=your-32-character-key
```

**原因：**
- 避免密钥硬编码
- 支持密钥轮换
- 符合安全合规要求

### 2. 选择性加密

**推荐做法：**
```java
@Data
@TableName("sys_user")
public class User {
    private Long id;
    private String name;
    
    // 只加密真正敏感的字段
    @Sensitive(type = SensitiveType.PHONE, encrypt = true)
    private String phone;
    
    @Sensitive(type = SensitiveType.ID_CARD, encrypt = true)
    private String idCard;
    
    // 不敏感的字段不加密
    private String email;
    private String address;
}
```

**原因：**
- 减少加密开销
- 提高查询性能
- 降低存储成本

### 3. 分层脱敏

**推荐做法：**
```java
@Service
public class UserService {
    public User getUserForAdmin(Long id) {
        // 管理员可以看到完整数据
        PermissionContext.setPermission("VIEW_SENSITIVE_DATA");
        try {
            return userMapper.selectById(id);
        } finally {
            PermissionContext.clear();
        }
    }
    
    public User getUserForNormalUser(Long id) {
        // 普通用户看到脱敏数据
        return userMapper.selectById(id);
    }
}
```

**原因：**
- 基于角色的数据访问控制
- 满足不同用户的数据需求
- 提高数据安全性

### 4. 日志脱敏

**推荐做法：**
```xml
<!-- logback-spring.xml -->
<configuration>
    <conversionRule conversionWord="mask" 
                    converterClass="com.basebackend.database.security.masking.LogMaskingConverter"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} - %mask(%msg)%n</pattern>
        </encoder>
    </appender>
</configuration>
```

**原因：**
- 防止敏感数据通过日志泄露
- 满足合规要求
- 保护用户隐私

## 健康监控最佳实践

### 1. 设置合理的阈值

**推荐做法：**
```yaml
database:
  enhanced:
    health:
      # 根据业务特点调整
      slow-query-threshold: 1000  # 1 秒
      pool-usage-threshold: 80    # 80%
```

**不同场景的阈值建议：**

| 场景 | 慢查询阈值 | 连接池阈值 |
|------|-----------|-----------|
| 高性能 API | 500ms | 70% |
| 普通业务 | 1000ms | 80% |
| 报表查询 | 5000ms | 90% |

### 2. 集成告警系统

**推荐做法：**
```java
@Component
public class DatabaseAlertHandler {
    @Autowired
    private AlertService alertService;
    
    @EventListener
    public void handleSlowQuery(SlowQueryEvent event) {
        if (event.getExecutionTime() > 5000) {
            // 超过 5 秒的查询发送紧急告警
            alertService.sendUrgentAlert(
                "严重慢查询",
                "SQL: " + event.getSql() + 
                ", 耗时: " + event.getExecutionTime() + "ms"
            );
        }
    }
    
    @EventListener
    public void handlePoolAlert(PoolUsageEvent event) {
        if (event.getUsageRate() > 0.9) {
            alertService.sendAlert(
                "连接池使用率过高",
                "数据源: " + event.getDataSourceName() + 
                ", 使用率: " + event.getUsageRate() * 100 + "%"
            );
        }
    }
}
```

**原因：**
- 及时发现性能问题
- 快速响应故障
- 提高系统可用性

### 3. 定期分析慢查询

**推荐做法：**
```java
@Scheduled(cron = "0 0 1 * * ?")  // 每天凌晨 1 点
public void analyzeSlowQueries() {
    List<SlowQuery> slowQueries = slowQueryLogger.getSlowQueries(
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now()
    );
    
    // 生成慢查询报告
    SlowQueryReport report = generateReport(slowQueries);
    
    // 发送给开发团队
    emailService.sendReport(report);
}
```

**原因：**
- 持续优化数据库性能
- 发现潜在的性能瓶颈
- 指导索引优化

## 动态数据源最佳实践

### 1. 明确数据源切换范围

**推荐做法：**
```java
@Service
public class OrderService {
    // 方法级别切换
    @DS("order_db")
    public Order getOrder(Long id) {
        return orderMapper.selectById(id);
    }
    
    // 避免在类级别切换（除非所有方法都使用同一数据源）
    // @DS("order_db")  // 不推荐
    // public class OrderService { ... }
}
```

**原因：**
- 更精确的数据源控制
- 避免意外的数据源切换
- 提高代码可读性

### 2. 处理嵌套调用

**推荐做法：**
```java
@Service
public class OrderService {
    @Autowired
    private UserService userService;
    
    @DS("order_db")
    public Order createOrder(OrderDTO dto) {
        // 当前使用 order_db
        Order order = new Order();
        order.setUserId(dto.getUserId());
        
        // 嵌套调用会切换到 user_db，完成后自动恢复
        User user = userService.getUser(dto.getUserId());
        order.setUserName(user.getName());
        
        // 这里又回到 order_db
        orderMapper.insert(order);
        return order;
    }
}

@Service
public class UserService {
    @DS("user_db")
    public User getUser(Long id) {
        return userMapper.selectById(id);
    }
}
```

**原因：**
- 支持复杂的业务场景
- 自动管理数据源切换栈
- 避免数据源混乱

### 3. 读写分离

**推荐做法：**
```java
@Service
public class ProductService {
    // 写操作使用主库
    @DS("master")
    public void updateProduct(Product product) {
        productMapper.updateById(product);
    }
    
    // 读操作使用从库
    @DS("slave")
    public Product getProduct(Long id) {
        return productMapper.selectById(id);
    }
    
    // 列表查询使用从库
    @DS("slave")
    public List<Product> listProducts() {
        return productMapper.selectList(null);
    }
}
```

**原因：**
- 分散数据库压力
- 提高查询性能
- 保护主库资源

## 故障转移最佳实践

### 1. 配置合理的重试策略

**推荐做法：**
```yaml
database:
  enhanced:
    failover:
      enabled: true
      max-retry: 3
      retry-interval: 5000  # 5 秒
      master-degradation: false  # 生产环境慎用
```

**原因：**
- 给数据库足够的恢复时间
- 避免过于频繁的重试加重数据库负担
- 快速失败，避免长时间等待

### 2. 监控故障转移事件

**推荐做法：**
```java
@Component
public class FailoverEventListener {
    @EventListener
    public void handleMasterFailover(MasterFailoverEvent event) {
        log.error("主库故障转移: {}", event.getDataSourceName());
        
        // 发送告警
        alertService.sendUrgentAlert(
            "数据库主库故障",
            "数据源: " + event.getDataSourceName()
        );
        
        // 记录到监控系统
        metricsService.recordFailover(event);
    }
}
```

**原因：**
- 及时发现数据库故障
- 快速响应和处理
- 积累故障数据用于分析

### 3. 定期演练故障转移

**推荐做法：**
```bash
# 定期进行故障演练
# 1. 模拟主库故障
docker stop mysql-master

# 2. 观察应用行为
# 3. 验证告警是否触发
# 4. 验证从库是否正常工作

# 5. 恢复主库
docker start mysql-master

# 6. 验证主库是否自动恢复
```

**原因：**
- 验证故障转移机制是否正常
- 提高团队应急响应能力
- 发现潜在问题

## 数据库迁移最佳实践

### 1. 遵循命名规范

**推荐做法：**
```
db/migration/
├── V1.0.0__init_schema.sql
├── V1.0.1__add_user_table.sql
├── V1.0.2__add_order_table.sql
├── V1.1.0__add_user_email_column.sql
└── V1.1.1__add_order_index.sql
```

**命名规则：**
- `V{version}__{description}.sql`
- 版本号使用语义化版本
- 描述使用下划线分隔
- 使用英文描述

### 2. 编写可回滚的迁移脚本

**推荐做法：**
```sql
-- V1.1.0__add_user_email_column.sql

-- 添加列
ALTER TABLE sys_user ADD COLUMN email VARCHAR(100);

-- 如果需要回滚，创建对应的回滚脚本
-- U1.1.0__rollback_add_user_email_column.sql
-- ALTER TABLE sys_user DROP COLUMN email;
```

**原因：**
- 支持快速回滚
- 降低迁移风险
- 提高部署信心

### 3. 在迁移前备份数据

**推荐做法：**
```yaml
database:
  enhanced:
    migration:
      backup:
        enabled: true
        backup-dir: /data/backup/database
```

**原因：**
- 防止数据丢失
- 支持快速恢复
- 满足合规要求

### 4. 分阶段执行大型迁移

**推荐做法：**
```sql
-- 不推荐：一次性迁移大量数据
-- UPDATE sys_user SET status = 'ACTIVE' WHERE status IS NULL;

-- 推荐：分批迁移
-- V1.2.0__migrate_user_status_batch.sql
UPDATE sys_user 
SET status = 'ACTIVE' 
WHERE status IS NULL 
LIMIT 1000;

-- 使用定时任务多次执行，直到完成
```

**原因：**
- 避免长时间锁表
- 减少对业务的影响
- 降低迁移风险

## SQL 性能优化最佳实践

### 1. 定期分析 SQL 统计

**推荐做法：**
```java
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点
public void analyzeSqlStatistics() {
    // 获取执行次数最多的 SQL
    List<SqlStatistics> topExecuted = sqlStatisticsService.getTopExecuted(10);
    
    // 获取平均耗时最长的 SQL
    List<SqlStatistics> slowest = sqlStatisticsService.getSlowest(10);
    
    // 生成优化建议
    List<OptimizationSuggestion> suggestions = 
        sqlPerformanceAnalyzer.analyze(topExecuted, slowest);
    
    // 发送报告
    emailService.sendOptimizationReport(suggestions);
}
```

**原因：**
- 持续优化数据库性能
- 发现性能瓶颈
- 指导索引设计

### 2. 避免在生产环境启用执行计划分析

**推荐做法：**
```yaml
# 生产环境
database:
  enhanced:
    sql-statistics:
      enabled: true
      explain-enabled: false  # 禁用

# 测试环境
database:
  enhanced:
    sql-statistics:
      enabled: true
      explain-enabled: true  # 启用
```

**原因：**
- 执行计划分析会影响性能
- 增加数据库负担
- 生产环境应该关注结果而非过程

### 3. 定期清理统计数据

**推荐做法：**
```yaml
database:
  enhanced:
    sql-statistics:
      retention-days: 30  # 只保留 30 天
```

**原因：**
- 避免统计表过大
- 保持查询性能
- 节省存储空间

## 性能优化建议

### 1. 连接池配置

**推荐做法：**
```yaml
spring:
  datasource:
    hikari:
      # 最小空闲连接数
      minimum-idle: 5
      # 最大连接数（根据业务压力调整）
      maximum-pool-size: 20
      # 连接超时时间
      connection-timeout: 30000
      # 空闲连接存活时间
      idle-timeout: 600000
      # 连接最大存活时间
      max-lifetime: 1800000
```

**调优建议：**
- 根据并发量调整 `maximum-pool-size`
- 保持 `minimum-idle` 在 5-10 之间
- 定期监控连接池使用情况

### 2. 批量操作

**推荐做法：**
```java
// 不推荐：逐条插入
for (User user : users) {
    userMapper.insert(user);
}

// 推荐：批量插入
userMapper.insertBatch(users);
```

**原因：**
- 减少数据库交互次数
- 提高插入性能
- 降低网络开销

### 3. 使用缓存

**推荐做法：**
```java
@Service
public class DictService {
    @Cacheable(value = "dict", key = "#type")
    public List<Dict> getDictByType(String type) {
        return dictMapper.selectByType(type);
    }
}
```

**原因：**
- 减少数据库查询
- 提高响应速度
- 降低数据库压力

## 安全建议

### 1. 最小权限原则

**推荐做法：**
```sql
-- 应用数据库用户只授予必要的权限
GRANT SELECT, INSERT, UPDATE, DELETE ON basebackend.* TO 'app_user'@'%';

-- 不要授予 DROP, ALTER 等危险权限
-- REVOKE DROP, ALTER ON basebackend.* FROM 'app_user'@'%';
```

### 2. 定期审计

**推荐做法：**
```java
@Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨 3 点
public void auditSecurityEvents() {
    // 检查异常的数据访问
    List<AuditLog> suspiciousLogs = auditLogService.findSuspicious();
    
    // 检查跨租户访问尝试
    List<TenantViolation> violations = tenantService.findViolations();
    
    // 生成安全报告
    SecurityReport report = generateSecurityReport(suspiciousLogs, violations);
    
    // 发送给安全团队
    emailService.sendSecurityReport(report);
}
```

### 3. 加密传输

**推荐做法：**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?useSSL=true&requireSSL=true
```

**原因：**
- 防止数据在传输过程中被窃取
- 满足安全合规要求
- 保护敏感数据

## 总结

遵循这些最佳实践可以帮助您：

1. **提高性能**：通过合理的配置和优化策略
2. **增强安全性**：通过加密、脱敏和权限控制
3. **提高可用性**：通过健康监控和故障转移
4. **降低风险**：通过审计、备份和测试
5. **简化运维**：通过自动化和监控

记住：最佳实践不是一成不变的，应该根据实际业务场景和需求进行调整。
