# 数据源故障转移快速开始指南

## 5分钟快速上手

### 1. 启用故障转移 (1分钟)

在 `application.yml` 中添加配置：

```yaml
database:
  enhanced:
    # 启用故障转移
    failover:
      enabled: true
      max-retry: 3
      retry-interval: 5000
      master-degradation: false
    
    # 启用健康监控（必需）
    health:
      enabled: true
      check-interval: 30
```

### 2. 自动工作 (0分钟)

启动应用后，故障转移机制会自动工作：

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    
    // 正常使用，故障转移自动处理
    public User getUser(Long id) {
        return userMapper.selectById(id);
    }
}
```

### 3. 查看状态 (1分钟)

通过健康检查端点查看状态：

```bash
curl http://localhost:8080/actuator/health
```

### 4. 监控日志 (1分钟)

关注以下关键日志：

```
# 主库故障
ERROR - Master database connection failed, attempting reconnection...

# 从库故障
ERROR - Slave database [slave1] connection failed
WARN - Removed slave [slave1] from available pool

# 从库恢复
INFO - Slave database [slave1] has recovered
INFO - Added slave [slave1] back to available pool
```

### 5. 高级使用 (2分钟)

如需手动控制，注入相关组件：

```java
@Service
public class DataSourceManagementService {
    
    @Autowired
    private DataSourceFailoverHandler failoverHandler;
    
    @Autowired
    private DataSourceRecoveryManager recoveryManager;
    
    // 检查主库是否降级
    public boolean isMasterDegraded() {
        return failoverHandler.isMasterDegraded();
    }
    
    // 获取可用从库
    public Set<String> getAvailableSlaves() {
        return recoveryManager.getAvailableSlaveKeys();
    }
}
```

## 常见场景

### 场景1: 主库临时故障

**现象**: 主库网络抖动导致连接失败

**处理**: 
1. 自动尝试重连（最多3次）
2. 重连成功后恢复正常
3. 无需人工干预

**日志**:
```
ERROR - Master database connection failed, attempting reconnection...
INFO - Reconnection attempt 1/3 for master database
INFO - Master database reconnection successful
```

### 场景2: 从库故障

**现象**: 从库宕机

**处理**:
1. 自动从可用列表移除
2. 读请求路由到其他从库或主库
3. 定时检测恢复状态

**日志**:
```
ERROR - Slave database [slave1] connection failed
WARN - Removed slave [slave1] from available pool
```

### 场景3: 从库恢复

**现象**: 从库重启后恢复正常

**处理**:
1. 定时健康检查检测到恢复
2. 自动加回可用列表
3. 开始接收读请求

**日志**:
```
INFO - Slave database [slave1] has recovered
INFO - Added slave [slave1] back to available pool
```

## 配置建议

### 开发环境

```yaml
database:
  enhanced:
    failover:
      enabled: true
      max-retry: 2          # 快速失败
      retry-interval: 3000  # 短间隔
      master-degradation: false
    health:
      check-interval: 60    # 较长间隔
```

### 生产环境

```yaml
database:
  enhanced:
    failover:
      enabled: true
      max-retry: 3          # 多次重试
      retry-interval: 5000  # 适中间隔
      master-degradation: true  # 启用降级
    health:
      check-interval: 30    # 频繁检查
```

## 故障排查

### 问题: 故障转移未生效

**解决步骤**:

1. 检查配置是否启用
```yaml
database:
  enhanced:
    failover:
      enabled: true  # 确认为 true
    health:
      enabled: true  # 确认为 true
```

2. 检查日志级别
```yaml
logging:
  level:
    com.basebackend.database.failover: DEBUG
    com.basebackend.database.health: DEBUG
```

3. 查看健康检查端点
```bash
curl http://localhost:8080/actuator/health
```

### 问题: 从库未自动恢复

**解决步骤**:

1. 确认从库已注册
```java
recoveryManager.registerDataSource("slave1", slave1DataSource, true);
```

2. 检查健康检查间隔
```yaml
database:
  enhanced:
    health:
      check-interval: 30  # 确认配置合理
```

3. 查看恢复检测日志
```
DEBUG - Starting scheduled recovery check
DEBUG - Checking recovery for 1 failed slave(s)
```

## 下一步

- 📖 阅读 [完整使用指南](FAILOVER_USAGE.md)
- 📊 查看 [实现总结](FAILOVER_IMPLEMENTATION_SUMMARY.md)
- 🔍 了解 [健康监控](HEALTH_MONITORING_USAGE.md)
- 📚 浏览 [数据库增强功能](DATABASE_ENHANCEMENT_README.md)

## 需要帮助？

- 查看日志文件
- 检查配置文件
- 测试健康检查端点
- 查阅相关文档

---

**提示**: 故障转移机制设计为"零配置"工作，只需启用即可自动处理大部分故障场景。
