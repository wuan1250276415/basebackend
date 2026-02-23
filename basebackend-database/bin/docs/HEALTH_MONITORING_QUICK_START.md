# 数据源健康监控快速入门

## 5分钟快速开始

### 1. 启用健康监控

在 `application.yml` 中添加配置：

```yaml
database:
  enhanced:
    health:
      enabled: true
      check-interval: 30
      pool-usage-threshold: 80

# 启用 Actuator 健康端点
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
```

### 2. 访问健康状态

启动应用后，访问健康检查端点：

```bash
curl http://localhost:8080/actuator/health
```

### 3. 在代码中使用

```java
@Service
public class MyService {
    
    @Autowired
    private HealthMonitoringService healthMonitoringService;
    
    public void checkDatabaseHealth() {
        // 获取健康状态
        DataSourceHealth health = 
            healthMonitoringService.getHealthStatus("primary");
        
        if (health.getStatus() != DataSourceHealth.HealthStatus.UP) {
            // 处理数据库不健康的情况
            log.error("Database is unhealthy: {}", health.getErrorMessage());
        }
        
        // 检查连接池
        double usageRate = healthMonitoringService.getConnectionPoolUsageRate();
        if (usageRate > 80) {
            log.warn("Connection pool usage is high: {}%", usageRate);
        }
    }
}
```

## 常用操作

### 查看连接池统计

```java
Map<String, Object> stats = healthMonitoringService.getConnectionPoolStats();
System.out.println("Active connections: " + stats.get("activeConnections"));
System.out.println("Usage rate: " + stats.get("usageRate") + "%");
```

### 手动触发健康检查

```java
DataSourceHealth health = healthMonitoringService.triggerHealthCheck("primary");
```

### 获取详细统计

```java
Map<String, Object> detailedStats = 
    healthMonitoringService.getDetailedConnectionPoolStats();
```

## 健康状态说明

- **UP**: 数据源正常 ✅
- **DEGRADED**: 数据源可用但性能下降（如连接池使用率过高）⚠️
- **DOWN**: 数据源不可用 ❌

## 告警阈值

默认配置：
- 连接池使用率告警：80%
- 慢查询阈值：1000ms
- 健康检查间隔：30秒

可根据实际情况调整配置。

## 监控指标

| 指标 | 说明 |
|------|------|
| connected | 是否连接成功 |
| responseTime | 响应时间（毫秒）|
| activeConnections | 活跃连接数 |
| idleConnections | 空闲连接数 |
| maxConnections | 最大连接数 |
| poolUsageRate | 连接池使用率（%）|

## 故障排查

### 问题：健康检查显示 DOWN

1. 检查数据库服务是否启动
2. 验证数据库连接配置
3. 查看错误日志：`grep "ALERT" application.log`

### 问题：连接池使用率过高

1. 检查是否有连接泄漏
2. 优化慢查询
3. 增加连接池大小

## 更多信息

详细文档请参考：
- [完整使用指南](HEALTH_MONITORING_USAGE.md)
- [实现总结](HEALTH_MONITORING_IMPLEMENTATION_SUMMARY.md)
