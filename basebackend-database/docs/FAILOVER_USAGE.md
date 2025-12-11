# 数据源故障转移使用指南

## 概述

数据源故障转移机制提供了自动检测和处理数据库连接故障的能力，包括：

- **主库故障转移**：自动重连和降级策略
- **从库故障处理**：自动移除和恢复检测
- **健康监控集成**：定时检查和告警通知

## 功能特性

### 1. 主库故障转移 (Requirement 6.1, 6.2)

当主库连接失败时：
1. 自动尝试重连（可配置重试次数和间隔）
2. 重连失败后根据配置决定是否降级到只读模式
3. 触发告警通知

### 2. 从库故障处理 (Requirement 6.3)

当从库连接失败时：
1. 自动从可用从库列表中移除
2. 记录失败次数
3. 触发告警通知

### 3. 从库恢复检测 (Requirement 6.4)

定时检查失败的从库：
1. 检测从库是否已恢复
2. 自动将恢复的从库加回可用列表
3. 重置失败计数

### 4. 读请求路由 (Requirement 6.5)

当所有从库不可用时：
1. 自动将读请求路由到主库
2. 记录告警日志

## 配置说明

### application.yml 配置

```yaml
database:
  enhanced:
    # 故障转移配置
    failover:
      # 是否启用故障转移
      enabled: true
      # 重连尝试次数
      max-retry: 3
      # 重连间隔（毫秒）
      retry-interval: 5000
      # 主库降级（主库不可用时是否降级到只读）
      master-degradation: false
    
    # 健康监控配置（与故障转移集成）
    health:
      enabled: true
      # 检查间隔（秒）
      check-interval: 30
```

### 配置参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `failover.enabled` | boolean | true | 是否启用故障转移 |
| `failover.max-retry` | int | 3 | 主库重连最大尝试次数 |
| `failover.retry-interval` | long | 5000 | 重连间隔（毫秒） |
| `failover.master-degradation` | boolean | false | 主库持续不可用时是否降级到只读模式 |
| `health.check-interval` | int | 30 | 健康检查间隔（秒） |

## 使用示例

### 1. 基本使用

故障转移机制会自动工作，无需手动干预：

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 正常使用，故障转移会自动处理
    public User getUser(Long id) {
        return userMapper.selectById(id);
    }
}
```

### 2. 手动触发故障检测

```java
@Service
public class DataSourceManagementService {
    
    @Autowired
    private DataSourceFailoverHandler failoverHandler;
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 手动触发主库故障转移
     */
    public boolean triggerMasterFailover() {
        return failoverHandler.handleMasterFailure(dataSource);
    }
    
    /**
     * 检查主库是否已降级
     */
    public boolean isMasterDegraded() {
        return failoverHandler.isMasterDegraded();
    }
    
    /**
     * 获取失败的从库列表
     */
    public Set<String> getFailedSlaves() {
        return failoverHandler.getFailedSlaves();
    }
}
```

### 3. 注册数据源用于恢复检测

```java
@Configuration
public class DataSourceConfiguration {
    
    @Autowired
    private DataSourceRecoveryManager recoveryManager;
    
    @Bean
    public void registerDataSources() {
        // 注册主库
        recoveryManager.registerDataSource("master", masterDataSource(), false);
        
        // 注册从库
        recoveryManager.registerDataSource("slave1", slave1DataSource(), true);
        recoveryManager.registerDataSource("slave2", slave2DataSource(), true);
    }
}
```

### 4. 获取可用从库

```java
@Service
public class ReadRoutingService {
    
    @Autowired
    private DataSourceRecoveryManager recoveryManager;
    
    /**
     * 获取可用的从库列表
     */
    public Set<String> getAvailableSlaves() {
        return recoveryManager.getAvailableSlaveKeys();
    }
    
    /**
     * 选择一个可用的从库
     */
    public String selectAvailableSlave() {
        Set<String> availableSlaves = getAvailableSlaves();
        
        if (availableSlaves.isEmpty()) {
            // 所有从库不可用，使用主库
            return "master";
        }
        
        // 简单轮询选择
        return availableSlaves.iterator().next();
    }
}
```

## 工作流程

### 主库故障转移流程

```
1. 健康检查检测到主库连接失败
   ↓
2. 触发 DataSourceFailoverHandler.handleMasterFailure()
   ↓
3. 尝试重连（最多 max-retry 次）
   ↓
4. 重连成功？
   ├─ 是 → 重置失败计数，恢复正常
   └─ 否 → 检查是否启用降级
              ├─ 是 → 降级到只读模式
              └─ 否 → 记录错误日志
```

### 从库故障处理流程

```
1. 检测到从库连接失败
   ↓
2. 触发 DataSourceFailoverHandler.handleSlaveFailure()
   ↓
3. 增加失败计数
   ↓
4. 从可用列表中移除
   ↓
5. 触发告警通知
```

### 从库恢复检测流程

```
1. 定时任务触发恢复检测
   ↓
2. 遍历所有失败的从库
   ↓
3. 对每个从库执行健康检查
   ↓
4. 从库恢复？
   ├─ 是 → 加回可用列表，重置失败计数
   └─ 否 → 继续等待下次检测
```

## 监控和告警

### 日志监控

故障转移会产生以下关键日志：

```
# 主库连接失败
ERROR - Master database connection failed, attempting reconnection...

# 重连成功
INFO - Master database reconnection successful

# 重连失败，触发降级
WARN - Degrading master database to read-only mode
ERROR - ALERT: Master database is degraded to read-only mode!

# 从库故障
ERROR - Slave database [slave1] connection failed
WARN - Removed slave [slave1] from available pool
ERROR - ALERT: Slave database [slave1] is down!

# 从库恢复
INFO - Slave database [slave1] has recovered
INFO - Added slave [slave1] back to available pool

# 所有从库不可用
WARN - All slave databases are unavailable, reads will be routed to master
```

### 健康检查端点

通过 Spring Boot Actuator 查看健康状态：

```bash
# 查看整体健康状态
curl http://localhost:8080/actuator/health

# 查看数据源健康详情
curl http://localhost:8080/actuator/health/dataSource
```

响应示例：

```json
{
  "status": "UP",
  "details": {
    "dataSources": {
      "primary": {
        "name": "primary",
        "connected": true,
        "responseTime": 15,
        "activeConnections": 5,
        "idleConnections": 15,
        "maxConnections": 20,
        "poolUsageRate": 25.0,
        "status": "UP"
      }
    }
  }
}
```

## 最佳实践

### 1. 合理配置重试参数

```yaml
database:
  enhanced:
    failover:
      # 生产环境建议配置
      max-retry: 3          # 重试3次
      retry-interval: 5000  # 每次间隔5秒
```

### 2. 启用主库降级（可选）

```yaml
database:
  enhanced:
    failover:
      # 如果有备用主库或只读副本，可以启用降级
      master-degradation: true
```

### 3. 配置合适的健康检查间隔

```yaml
database:
  enhanced:
    health:
      # 根据业务需求调整，建议30-60秒
      check-interval: 30
```

### 4. 集成告警系统

实现自定义告警处理：

```java
@Component
public class CustomAlertHandler {
    
    @EventListener
    public void handleDataSourceFailure(DataSourceFailureEvent event) {
        // 发送邮件、短信、钉钉等告警
        sendAlert("数据源故障", event.getMessage());
    }
}
```

## 故障排查

### 问题1：故障转移未生效

**检查项：**
1. 确认 `failover.enabled=true`
2. 确认 `health.enabled=true`
3. 检查日志是否有错误信息

### 问题2：从库未自动恢复

**检查项：**
1. 确认从库已注册到 RecoveryManager
2. 检查健康检查间隔配置
3. 查看恢复检测日志

### 问题3：主库降级未触发

**检查项：**
1. 确认 `master-degradation=true`
2. 确认重连已达到最大次数
3. 检查主库是否真的不可用

## 性能影响

- **健康检查开销**：每次检查约 10-50ms（取决于网络延迟）
- **重连开销**：每次重连尝试约 5-10秒（包含等待时间）
- **恢复检测开销**：与失败从库数量成正比，每个约 10-50ms

## 注意事项

1. **不要频繁触发手动故障转移**：可能导致系统不稳定
2. **合理设置重试次数**：过多会延长故障恢复时间
3. **监控告警日志**：及时发现和处理数据库问题
4. **定期测试故障转移**：确保机制正常工作
5. **主库降级需谨慎**：确保应用能处理只读模式

## 相关文档

- [健康监控使用指南](HEALTH_MONITORING_USAGE.md)
- [动态数据源使用指南](NESTED_DATASOURCE_SWITCHING.md)
- [数据库增强功能总览](DATABASE_ENHANCEMENT_README.md)
