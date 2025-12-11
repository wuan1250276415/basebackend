# 数据源健康监控使用指南

## 概述

数据源健康监控模块提供了全面的数据库连接健康检查和连接池监控功能，帮助运维人员实时了解数据库状态，及时发现和处理问题。

## 功能特性

- **自动健康检查**：定时检查数据源连接状态
- **连接池监控**：实时监控连接池使用情况
- **告警机制**：连接失败或使用率过高时自动告警
- **Spring Boot Actuator 集成**：通过 `/actuator/health` 端点访问健康状态
- **详细统计信息**：提供连接池的详细运行统计

## 配置

### 基础配置

在 `application.yml` 中配置健康监控：

```yaml
database:
  enhanced:
    health:
      # 是否启用健康监控
      enabled: true
      # 检查间隔（秒）
      check-interval: 30
      # 慢查询阈值（毫秒）
      slow-query-threshold: 1000
      # 连接池告警阈值（百分比）
      pool-usage-threshold: 80
```

### Spring Boot Actuator 配置

启用健康检查端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

## 使用方式

### 1. 通过 Actuator 端点访问

访问健康检查端点：

```bash
curl http://localhost:8080/actuator/health
```

响应示例：

```json
{
  "status": "UP",
  "components": {
    "dataSourceHealthIndicator": {
      "status": "UP",
      "details": {
        "dataSources": {
          "primary": {
            "name": "primary",
            "connected": true,
            "responseTime": 5,
            "activeConnections": 2,
            "idleConnections": 8,
            "maxConnections": 10,
            "poolUsageRate": 20.0,
            "lastCheckTime": "2025-11-20T10:30:00",
            "status": "UP"
          }
        }
      }
    }
  }
}
```

### 2. 通过服务接口访问

注入 `HealthMonitoringService` 使用：

```java
@Service
public class MyService {
    
    @Autowired
    private HealthMonitoringService healthMonitoringService;
    
    public void checkHealth() {
        // 获取所有数据源健康状态
        Map<String, DataSourceHealth> allStatus = 
            healthMonitoringService.getAllHealthStatus();
        
        // 获取指定数据源健康状态
        DataSourceHealth primaryHealth = 
            healthMonitoringService.getHealthStatus("primary");
        
        // 手动触发健康检查
        DataSourceHealth health = 
            healthMonitoringService.triggerHealthCheck("primary");
        
        // 获取连接池统计
        Map<String, Object> poolStats = 
            healthMonitoringService.getConnectionPoolStats();
        
        // 检查连接池是否健康
        boolean isHealthy = 
            healthMonitoringService.isConnectionPoolHealthy();
        
        // 获取连接池使用率
        double usageRate = 
            healthMonitoringService.getConnectionPoolUsageRate();
    }
}
```

### 3. 直接使用组件

#### DataSourceHealthIndicator

```java
@Autowired
private DataSourceHealthIndicator healthIndicator;

public void checkDataSource() {
    // 检查整体健康状态
    Health health = healthIndicator.health();
    
    // 检查特定数据源
    DataSourceHealth dsHealth = 
        healthIndicator.checkDataSource(dataSource, "myDataSource");
}
```

#### ConnectionPoolMonitor

```java
@Autowired
private ConnectionPoolMonitor poolMonitor;

public void monitorPool() {
    // 监控连接池
    Map<String, Object> stats = poolMonitor.monitorConnectionPool();
    
    // 获取使用率
    double usageRate = poolMonitor.getPoolUsageRate();
    
    // 检查是否健康
    boolean isHealthy = poolMonitor.isPoolHealthy();
    
    // 获取详细统计
    Map<String, Object> detailedStats = poolMonitor.getDetailedStats();
}
```

#### HealthCheckScheduler

```java
@Autowired
private HealthCheckScheduler scheduler;

public void useScheduler() {
    // 获取最新健康状态
    Map<String, DataSourceHealth> healthStatus = 
        scheduler.getLatestHealthStatus();
    
    // 获取最新连接池统计
    Map<String, Object> poolStats = 
        scheduler.getLatestPoolStats();
    
    // 手动触发健康检查
    DataSourceHealth health = 
        scheduler.triggerManualHealthCheck("primary");
    
    // 清除历史记录
    scheduler.clearHealthHistory();
}
```

## 健康状态说明

### HealthStatus 枚举

- **UP**：数据源正常运行
- **DOWN**：数据源不可用
- **DEGRADED**：数据源可用但性能下降（如连接池使用率过高）

### DataSourceHealth 模型

```java
public class DataSourceHealth {
    private String name;              // 数据源名称
    private boolean connected;        // 是否连接
    private long responseTime;        // 响应时间（毫秒）
    private int activeConnections;    // 活跃连接数
    private int idleConnections;      // 空闲连接数
    private int maxConnections;       // 最大连接数
    private double poolUsageRate;     // 连接池使用率（0-100）
    private LocalDateTime lastCheckTime; // 最后检查时间
    private String errorMessage;      // 错误信息
    private String type;              // 数据源类型
    private HealthStatus status;      // 健康状态
}
```

## 告警机制

### 连接失败告警

当数据源连接失败时，系统会：
1. 记录错误日志
2. 设置健康状态为 DOWN
3. 触发告警（日志输出，可扩展为邮件、短信等）

```
ERROR: ALERT: Data source primary is DOWN. Response time: 0ms. Error: Connection refused
```

### 连接池使用率告警

当连接池使用率超过配置阈值时，系统会：
1. 记录警告日志
2. 设置健康状态为 DEGRADED
3. 触发告警

```
WARN: ALERT: Connection pool usage rate is high: 85.00% (active: 85, max: 100, threshold: 80%)
```

## 监控指标

### 基础指标

- `activeConnections`: 活跃连接数
- `idleConnections`: 空闲连接数
- `maxConnections`: 最大连接数
- `usageRate`: 使用率（百分比）

### 操作指标

- `createCount`: 创建连接次数
- `destroyCount`: 销毁连接次数
- `connectCount`: 获取连接次数
- `closeCount`: 关闭连接次数

### 等待指标

- `waitThreadCount`: 等待线程数
- `notEmptyWaitCount`: 非空等待次数
- `notEmptyWaitMillis`: 非空等待时间

### 错误指标

- `connectErrorCount`: 连接错误次数
- `errorCount`: 总错误次数

## 最佳实践

### 1. 合理设置检查间隔

```yaml
database:
  enhanced:
    health:
      # 生产环境建议 30-60 秒
      check-interval: 30
```

### 2. 根据业务设置告警阈值

```yaml
database:
  enhanced:
    health:
      # 根据实际负载调整
      pool-usage-threshold: 80
```

### 3. 集成监控系统

将健康检查结果集成到 Prometheus、Grafana 等监控系统：

```java
@Component
public class HealthMetricsExporter {
    
    @Autowired
    private HealthMonitoringService healthMonitoringService;
    
    @Scheduled(fixedRate = 60000)
    public void exportMetrics() {
        Map<String, DataSourceHealth> healthStatus = 
            healthMonitoringService.getAllHealthStatus();
        
        // 导出到 Prometheus
        for (Map.Entry<String, DataSourceHealth> entry : healthStatus.entrySet()) {
            DataSourceHealth health = entry.getValue();
            // 记录指标
            meterRegistry.gauge("datasource.pool.usage", 
                Tags.of("datasource", entry.getKey()), 
                health.getPoolUsageRate());
        }
    }
}
```

### 4. 实现自定义告警

扩展告警机制，支持多种通知方式：

```java
@Component
public class CustomAlertService {
    
    @EventListener
    public void onHealthAlert(HealthAlertEvent event) {
        // 发送邮件
        emailService.sendAlert(event);
        
        // 发送短信
        smsService.sendAlert(event);
        
        // 发送钉钉通知
        dingTalkService.sendAlert(event);
    }
}
```

### 5. 定期审查健康日志

定期检查健康检查日志，分析趋势：

```bash
# 查看健康检查日志
grep "Health check" application.log

# 查看告警日志
grep "ALERT" application.log
```

## 故障排查

### 问题：健康检查一直显示 DOWN

**可能原因**：
1. 数据库服务未启动
2. 网络连接问题
3. 数据库配置错误
4. 连接池配置不当

**解决方法**：
1. 检查数据库服务状态
2. 验证网络连接
3. 检查数据源配置
4. 查看详细错误日志

### 问题：连接池使用率持续过高

**可能原因**：
1. 并发请求过多
2. 连接泄漏
3. 慢查询导致连接占用时间长
4. 连接池配置过小

**解决方法**：
1. 增加连接池大小
2. 检查是否有连接未正确关闭
3. 优化慢查询
4. 使用连接池监控分析详细统计

### 问题：健康检查响应时间过长

**可能原因**：
1. 数据库负载过高
2. 网络延迟
3. 验证查询复杂

**解决方法**：
1. 优化数据库性能
2. 检查网络状况
3. 简化验证查询（默认使用 `SELECT 1`）

## 扩展开发

### 添加自定义数据源监控

```java
@Component
public class CustomDataSourceMonitor {
    
    @Autowired
    private DataSourceHealthIndicator healthIndicator;
    
    @Autowired
    @Qualifier("customDataSource")
    private DataSource customDataSource;
    
    @Scheduled(fixedRate = 30000)
    public void monitorCustomDataSource() {
        DataSourceHealth health = 
            healthIndicator.checkDataSource(customDataSource, "custom");
        
        // 处理健康状态
        if (health.getStatus() != DataSourceHealth.HealthStatus.UP) {
            // 自定义处理逻辑
        }
    }
}
```

### 实现多数据源监控

```java
@Component
public class MultiDataSourceMonitor {
    
    @Autowired
    private DataSourceHealthIndicator healthIndicator;
    
    @Autowired
    private Map<String, DataSource> dataSources;
    
    public Map<String, DataSourceHealth> checkAllDataSources() {
        Map<String, DataSourceHealth> healthMap = new HashMap<>();
        
        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            DataSourceHealth health = 
                healthIndicator.checkDataSource(entry.getValue(), entry.getKey());
            healthMap.put(entry.getKey(), health);
        }
        
        return healthMap;
    }
}
```

## 参考资料

- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Druid 监控配置](https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE_StatFilter)
- [数据库连接池最佳实践](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
