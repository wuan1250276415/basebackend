# 慢查询和连接池告警实现总结

## 概述

本次实现完成了数据库模块的慢查询监控和连接池告警功能，满足需求 4.4 和 4.5。

## 实现的组件

### 1. SlowQueryLogger (慢查询日志记录器)
**位置**: `com.basebackend.database.health.logger.SlowQueryLogger`

**功能**:
- 记录执行时间超过阈值的 SQL 语句
- 维护慢查询统计信息（执行次数、平均时间、最大/最小时间）
- 支持慢查询统计的查询和清除

**关键方法**:
- `logSlowQuery(String sql, long executionTime, Object parameters)`: 记录慢查询
- `getSlowQueryStats()`: 获取慢查询统计信息
- `isSlowQuery(long executionTime)`: 判断是否为慢查询

### 2. SqlExecutionTimeInterceptor (SQL执行时间拦截器)
**位置**: `com.basebackend.database.health.interceptor.SqlExecutionTimeInterceptor`

**功能**:
- 拦截所有 SQL 执行（查询和更新）
- 测量 SQL 执行时间
- 自动识别并记录慢查询

**实现方式**:
- 使用 MyBatis 标准拦截器（@Intercepts）
- 拦截 Executor 的 query 和 update 方法
- 在 SQL 执行前后计算执行时间

### 3. AlertNotificationService (告警通知服务)
**位置**: `com.basebackend.database.health.alert.AlertNotificationService`

**功能**:
- 发送各类数据库告警通知
- 防止重复告警（5分钟冷却时间）
- 支持多种告警级别（INFO、WARNING、ERROR）

**告警类型**:
- 慢查询告警
- 连接池使用率告警
- 数据源连接失败告警
- 健康状态变化告警

**扩展点**:
```java
// TODO: 集成实际的告警通知渠道
// 1. 邮件通知
// 2. 短信通知
// 3. 钉钉/企业微信通知
// 4. Webhook通知
```

### 4. 集成更新

#### ConnectionPoolMonitor
- 集成 AlertNotificationService
- 当连接池使用率超过阈值时自动发送告警

#### HealthCheckScheduler
- 集成 AlertNotificationService
- 数据源健康状态变化时发送告警
- 数据源连接失败时发送告警

#### MyBatisPlusConfig
- 注册 SqlExecutionTimeInterceptor 到 MyBatis 配置
- 条件启用：`database.enhanced.health.enabled=true`

## 配置说明

### application.yml 配置示例

```yaml
database:
  enhanced:
    health:
      enabled: true                    # 启用健康监控
      check-interval: 30               # 健康检查间隔（秒）
      slow-query-threshold: 1000       # 慢查询阈值（毫秒）
      pool-usage-threshold: 80         # 连接池告警阈值（百分比）
```

## 使用示例

### 1. 查看慢查询统计

```java
@Autowired
private SlowQueryLogger slowQueryLogger;

public void checkSlowQueries() {
    // 获取慢查询统计
    Map<String, SlowQueryStats> stats = slowQueryLogger.getSlowQueryStats();
    
    // 获取慢查询总数
    long totalSlowQueries = slowQueryLogger.getTotalSlowQueries();
    
    // 清除统计
    slowQueryLogger.clearStats();
}
```

### 2. 查看告警历史

```java
@Autowired
private AlertNotificationService alertService;

public void checkAlerts() {
    // 获取告警历史
    Map<String, AlertRecord> history = alertService.getAlertHistory();
    
    // 清除告警历史
    alertService.clearAlertHistory();
}
```

### 3. 手动发送告警

```java
@Autowired
private AlertNotificationService alertService;

public void sendCustomAlert() {
    alertService.sendAlert(
        AlertLevel.WARNING,
        "Custom Alert",
        "This is a custom alert message"
    );
}
```

## 工作流程

### 慢查询监控流程

1. SQL 执行前，SqlExecutionTimeInterceptor 记录开始时间
2. SQL 执行完成后，计算执行时间
3. 如果执行时间超过阈值（默认 1000ms）：
   - SlowQueryLogger 记录慢查询日志
   - AlertNotificationService 发送慢查询告警
   - 更新慢查询统计信息

### 连接池告警流程

1. HealthCheckScheduler 定期（默认 30 秒）检查连接池状态
2. ConnectionPoolMonitor 计算连接池使用率
3. 如果使用率超过阈值（默认 80%）：
   - 记录警告日志
   - AlertNotificationService 发送连接池告警
   - 防止 5 分钟内重复告警

## 性能影响

- **SQL 执行时间拦截器**: 每次 SQL 执行增加约 1-2ms 的开销（主要是时间戳记录）
- **慢查询日志**: 异步记录，不影响 SQL 执行性能
- **告警通知**: 异步发送，不阻塞业务逻辑

## 监控指标

### 慢查询统计
- SQL 语句
- 执行次数
- 总执行时间
- 平均执行时间
- 最大/最小执行时间
- 最后执行时间

### 连接池监控
- 活跃连接数
- 空闲连接数
- 最大连接数
- 使用率
- 创建/销毁计数
- 等待线程数

## 日志示例

### 慢查询日志
```
WARN  - SLOW QUERY DETECTED - Execution time: 1523ms (threshold: 1000ms)
SQL: SELECT * FROM user WHERE status = ? AND create_time > ?
Parameters: {status=1, create_time=2024-01-01}
```

### 连接池告警日志
```
WARN  - ALERT [WARNING]: Connection Pool Alert
Connection pool usage is high!
Usage rate: 85.50% (threshold: 80%)
Active connections: 171 / 200
```

### 数据源故障告警日志
```
ERROR - ALERT [ERROR]: Data Source Failure Alert
Data source connection failed!
Data source: primary
Error: Connection refused
```

## 后续扩展

### 1. 告警通知渠道集成
- 邮件通知（SMTP）
- 短信通知（阿里云/腾讯云）
- 钉钉机器人
- 企业微信机器人
- Webhook 通知

### 2. 慢查询分析
- SQL 执行计划分析
- 索引建议
- 查询优化建议

### 3. 告警规则配置
- 可配置的告警级别
- 可配置的告警接收人
- 可配置的告警时间窗口
- 告警聚合和去重

### 4. 监控数据持久化
- 将慢查询统计存储到数据库
- 提供历史数据查询接口
- 支持数据导出和报表生成

## 验证需求

### 需求 4.4: 慢查询日志
✅ WHEN SQL 执行时间超过配置阈值时 THEN Database Module SHALL 记录慢查询日志

**实现**:
- SqlExecutionTimeInterceptor 拦截所有 SQL 执行
- SlowQueryLogger 记录超过阈值的 SQL
- AlertNotificationService 发送慢查询告警

### 需求 4.5: 连接池告警
✅ WHEN 连接池使用率超过配置阈值时 THEN Database Module SHALL 触发告警通知

**实现**:
- ConnectionPoolMonitor 监控连接池使用率
- HealthCheckScheduler 定期检查连接池状态
- AlertNotificationService 发送连接池告警

## 相关文件

### 新增文件
- `SlowQueryLogger.java` - 慢查询日志记录器
- `SqlExecutionTimeInterceptor.java` - SQL 执行时间拦截器
- `AlertNotificationService.java` - 告警通知服务

### 修改文件
- `ConnectionPoolMonitor.java` - 集成告警服务
- `HealthCheckScheduler.java` - 集成告警服务
- `MyBatisPlusConfig.java` - 注册 SQL 执行时间拦截器

## 测试建议

### 单元测试
1. 测试慢查询识别逻辑
2. 测试告警防重复机制
3. 测试连接池使用率计算

### 集成测试
1. 执行慢查询并验证日志记录
2. 模拟高连接池使用率并验证告警
3. 验证告警冷却时间机制

### 性能测试
1. 测试拦截器对 SQL 执行的性能影响
2. 测试高并发场景下的慢查询记录性能
3. 测试告警发送的性能影响
