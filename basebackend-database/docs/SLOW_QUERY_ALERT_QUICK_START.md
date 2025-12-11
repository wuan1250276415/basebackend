# 慢查询和告警功能快速开始指南

## 快速启用

### 1. 配置文件设置

在 `application.yml` 中添加以下配置：

```yaml
database:
  enhanced:
    health:
      enabled: true                    # 启用健康监控（默认：true）
      check-interval: 30               # 健康检查间隔（秒，默认：30）
      slow-query-threshold: 1000       # 慢查询阈值（毫秒，默认：1000）
      pool-usage-threshold: 80         # 连接池告警阈值（百分比，默认：80）
```

### 2. 自动启用

功能会自动启用，无需额外配置。当满足以下条件时：
- `database.enhanced.health.enabled=true`（默认启用）
- 应用启动时自动注册所有监控组件

## 功能说明

### 慢查询监控

**自动监控**：
- 所有 SQL 查询和更新操作都会被自动监控
- 执行时间超过阈值的 SQL 会被记录为慢查询
- 自动发送告警通知

**日志示例**：
```
2024-11-20 10:30:15.123 WARN  - SLOW QUERY DETECTED - Execution time: 1523ms (threshold: 1000ms)
SQL: SELECT * FROM user WHERE status = ? AND create_time > ?
Parameters: {status=1, create_time=2024-01-01}

2024-11-20 10:30:15.125 WARN  - ALERT [WARNING]: Slow Query Alert
Slow query detected!
Execution time: 1523ms (threshold: 1000ms)
SQL: SELECT * FROM user WHERE status = ? AND create_time > ?
```

### 连接池告警

**自动监控**：
- 每 30 秒（可配置）自动检查连接池状态
- 使用率超过阈值时自动发送告警
- 5 分钟内不会重复发送相同告警

**日志示例**：
```
2024-11-20 10:30:45.456 WARN  - Connection pool usage rate is high: 85.5% (threshold: 80%)

2024-11-20 10:30:45.458 WARN  - ALERT [WARNING]: Connection Pool Alert
Connection pool usage is high!
Usage rate: 85.50% (threshold: 80%)
Active connections: 171 / 200
```

## 编程接口

### 查询慢查询统计

```java
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    @Autowired
    private SlowQueryLogger slowQueryLogger;
    
    @GetMapping("/slow-queries")
    public Map<String, Object> getSlowQueries() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取慢查询统计
        Map<String, SlowQueryStats> stats = slowQueryLogger.getSlowQueryStats();
        result.put("stats", stats);
        
        // 获取慢查询总数
        long total = slowQueryLogger.getTotalSlowQueries();
        result.put("total", total);
        
        // 获取阈值
        long threshold = slowQueryLogger.getSlowQueryThreshold();
        result.put("threshold", threshold);
        
        return result;
    }
    
    @PostMapping("/slow-queries/clear")
    public void clearSlowQueries() {
        slowQueryLogger.clearStats();
    }
}
```

### 查询连接池状态

```java
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    @Autowired
    private ConnectionPoolMonitor poolMonitor;
    
    @GetMapping("/connection-pool")
    public Map<String, Object> getConnectionPoolStats() {
        // 获取连接池统计
        return poolMonitor.monitorConnectionPool();
    }
    
    @GetMapping("/connection-pool/detailed")
    public Map<String, Object> getDetailedStats() {
        // 获取详细统计
        return poolMonitor.getDetailedStats();
    }
    
    @GetMapping("/connection-pool/health")
    public boolean isPoolHealthy() {
        // 检查连接池是否健康
        return poolMonitor.isPoolHealthy();
    }
}
```

### 查询告警历史

```java
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    @Autowired
    private AlertNotificationService alertService;
    
    @GetMapping("/alerts")
    public Map<String, AlertRecord> getAlerts() {
        return alertService.getAlertHistory();
    }
    
    @PostMapping("/alerts/clear")
    public void clearAlerts() {
        alertService.clearAlertHistory();
    }
    
    @PostMapping("/alerts/cleanup")
    public void cleanupExpiredAlerts() {
        alertService.cleanupExpiredAlerts();
    }
}
```

### 手动发送告警

```java
@Service
public class CustomMonitoringService {
    
    @Autowired
    private AlertNotificationService alertService;
    
    public void checkCustomCondition() {
        // 检查自定义条件
        if (someCondition) {
            alertService.sendAlert(
                AlertLevel.WARNING,
                "Custom Alert",
                "Custom condition detected: " + details
            );
        }
    }
}
```

## 配置调优

### 慢查询阈值调整

根据业务需求调整慢查询阈值：

```yaml
database:
  enhanced:
    health:
      # 对于高性能要求的系统，可以设置更低的阈值
      slow-query-threshold: 500    # 500ms
      
      # 对于复杂查询较多的系统，可以设置更高的阈值
      # slow-query-threshold: 2000  # 2000ms
```

### 连接池阈值调整

根据系统负载调整连接池告警阈值：

```yaml
database:
  enhanced:
    health:
      # 对于高并发系统，可以设置更高的阈值
      pool-usage-threshold: 90     # 90%
      
      # 对于需要更早预警的系统，可以设置更低的阈值
      # pool-usage-threshold: 70   # 70%
```

### 检查间隔调整

根据监控需求调整检查间隔：

```yaml
database:
  enhanced:
    health:
      # 更频繁的检查（更及时的告警，但增加系统开销）
      check-interval: 15           # 15秒
      
      # 较少的检查（降低系统开销，但告警可能延迟）
      # check-interval: 60         # 60秒
```

## 性能影响

### SQL 执行时间拦截器
- **开销**: 每次 SQL 执行增加约 1-2ms
- **影响**: 可忽略不计
- **建议**: 生产环境可以启用

### 慢查询日志
- **开销**: 异步记录，不阻塞 SQL 执行
- **影响**: 几乎无影响
- **建议**: 生产环境推荐启用

### 连接池监控
- **开销**: 每 30 秒执行一次检查
- **影响**: 可忽略不计
- **建议**: 生产环境推荐启用

## 告警通知扩展

当前实现仅记录日志，可以扩展以下通知渠道：

### 1. 邮件通知

```java
@Service
public class EmailAlertService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private AlertNotificationService alertService;
    
    @EventListener
    public void onAlert(AlertEvent event) {
        // 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("admin@example.com");
        message.setSubject(event.getTitle());
        message.setText(event.getMessage());
        mailSender.send(message);
    }
}
```

### 2. 钉钉通知

```java
@Service
public class DingTalkAlertService {
    
    @Value("${dingtalk.webhook.url}")
    private String webhookUrl;
    
    public void sendAlert(String title, String message) {
        // 构造钉钉消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("msgtype", "text");
        msg.put("text", Map.of("content", title + "\n" + message));
        
        // 发送到钉钉
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(webhookUrl, msg, String.class);
    }
}
```

### 3. 企业微信通知

```java
@Service
public class WeChatAlertService {
    
    @Value("${wechat.webhook.url}")
    private String webhookUrl;
    
    public void sendAlert(String title, String message) {
        // 构造企业微信消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("msgtype", "text");
        msg.put("text", Map.of("content", title + "\n" + message));
        
        // 发送到企业微信
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(webhookUrl, msg, String.class);
    }
}
```

## 故障排查

### 慢查询未记录

**检查项**：
1. 确认 `database.enhanced.health.enabled=true`
2. 确认 SQL 执行时间确实超过阈值
3. 检查日志级别是否为 WARN 或更低
4. 确认 SqlExecutionTimeInterceptor 已注册

**验证方法**：
```java
@Autowired
private SqlSessionFactory sqlSessionFactory;

public void checkInterceptors() {
    List<Interceptor> interceptors = 
        sqlSessionFactory.getConfiguration().getInterceptors();
    
    boolean hasTimeInterceptor = interceptors.stream()
        .anyMatch(i -> i instanceof SqlExecutionTimeInterceptor);
    
    System.out.println("SqlExecutionTimeInterceptor registered: " + hasTimeInterceptor);
}
```

### 告警未发送

**检查项**：
1. 确认告警条件已触发
2. 检查是否在 5 分钟冷却期内
3. 确认日志级别配置正确
4. 检查 AlertNotificationService 是否正常初始化

**验证方法**：
```java
@Autowired
private AlertNotificationService alertService;

public void testAlert() {
    // 手动发送测试告警
    alertService.sendAlert(
        AlertLevel.INFO,
        "Test Alert",
        "This is a test alert"
    );
}
```

### 连接池监控不工作

**检查项**：
1. 确认使用的是 Druid 数据源
2. 确认定时任务已启用（@EnableScheduling）
3. 检查 check-interval 配置是否正确
4. 确认 ConnectionPoolMonitor 已初始化

**验证方法**：
```java
@Autowired
private ConnectionPoolMonitor poolMonitor;

public void testPoolMonitor() {
    // 手动触发监控
    Map<String, Object> stats = poolMonitor.monitorConnectionPool();
    System.out.println("Pool stats: " + stats);
}
```

## 最佳实践

### 1. 合理设置阈值
- 根据业务特点设置慢查询阈值
- 定期review慢查询日志，优化SQL
- 根据系统负载调整连接池阈值

### 2. 定期清理统计数据
- 定期清理慢查询统计，避免内存占用过大
- 定期清理告警历史，避免内存泄漏

### 3. 集成监控系统
- 将慢查询统计导出到监控系统
- 将告警集成到现有告警平台
- 建立告警响应流程

### 4. 性能优化
- 对于高并发系统，考虑使用采样统计
- 对于慢查询较多的系统，考虑异步批量处理
- 定期分析慢查询，优化数据库性能

## 相关文档

- [健康监控实现总结](HEALTH_MONITORING_IMPLEMENTATION_SUMMARY.md)
- [健康监控使用指南](HEALTH_MONITORING_USAGE.md)
- [慢查询和告警实现总结](SLOW_QUERY_AND_ALERT_IMPLEMENTATION.md)
- [数据库增强功能总览](DATABASE_ENHANCEMENT_README.md)
