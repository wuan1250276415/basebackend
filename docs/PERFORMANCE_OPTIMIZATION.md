# 性能优化建议

> **文档版本**: v1.0  
> **创建日期**: 2025-11-18  
> **适用范围**: BaseBackend所有微服务

---

## 1. 数据库优化

### 1.1 索引优化

**当前状态**: 基础索引已创建  
**优化建议**:

```sql
-- 用户表索引优化
CREATE INDEX idx_user_username ON sys_user(username);
CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_user_phone ON sys_user(phone);
CREATE INDEX idx_user_dept_id ON sys_user(dept_id);
CREATE INDEX idx_user_status ON sys_user(status);
CREATE INDEX idx_user_create_time ON sys_user(create_time);

-- 角色表索引
CREATE INDEX idx_role_code ON sys_role(role_code);
CREATE INDEX idx_role_status ON sys_role(status);

-- 权限表索引
CREATE INDEX idx_permission_code ON sys_permission(perms);
CREATE INDEX idx_permission_type ON sys_permission(type);

-- 通知表索引
CREATE INDEX idx_notification_user_id ON user_notification(user_id);
CREATE INDEX idx_notification_is_read ON user_notification(is_read);
CREATE INDEX idx_notification_create_time ON user_notification(create_time);
CREATE INDEX idx_notification_user_read ON user_notification(user_id, is_read);

-- 告警规则表索引
CREATE INDEX idx_alert_rule_metric ON alert_rule(metric_name);
CREATE INDEX idx_alert_rule_enabled ON alert_rule(enabled);
CREATE INDEX idx_alert_event_rule_id ON alert_event(rule_id);
CREATE INDEX idx_alert_event_status ON alert_event(status);
```

### 1.2 查询优化

**慢查询识别**:
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow-query.log';
```

**分页查询优化**:
```java
// 使用游标分页代替offset
// 不推荐
SELECT * FROM sys_user LIMIT 10000, 10;

// 推荐
SELECT * FROM sys_user WHERE id > last_id ORDER BY id LIMIT 10;
```

### 1.3 连接池优化

**HikariCP配置**:
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
```

---

## 2. 缓存优化

### 2.1 Redis缓存策略

**多级缓存**:
```java
// L1: 本地缓存（Caffeine）
// L2: Redis缓存
// L3: 数据库

@Cacheable(value = "user", key = "#id", unless = "#result == null")
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

**缓存预热**:
```java
@PostConstruct
public void warmUpCache() {
    // 预加载热点数据
    List<Dict> dicts = dictService.getAllDicts();
    dicts.forEach(dict -> 
        redisTemplate.opsForValue().set("dict:" + dict.getType(), dict, 1, TimeUnit.HOURS)
    );
}
```

### 2.2 缓存过期策略

**推荐配置**:
```yaml
# 用户信息: 30分钟
user:*: 1800

# 角色权限: 1小时
role:*: 3600
permission:*: 3600

# 字典数据: 24小时
dict:*: 86400

# 部门菜单: 12小时
dept:*: 43200
menu:*: 43200

# 通知: 5分钟
notification:*: 300
```

### 2.3 缓存穿透防护

**布隆过滤器**:
```java
@Bean
public BloomFilter<String> bloomFilter() {
    return BloomFilter.create(
        Funnels.stringFunnel(Charset.defaultCharset()),
        100000,
        0.01
    );
}
```

---

## 3. 服务间调用优化

### 3.1 Feign优化

**连接池配置**:
```yaml
feign:
  httpclient:
    enabled: true
    max-connections: 200
    max-connections-per-route: 50
    connection-timeout: 2000
    connection-timer-repeat: 3000
  compression:
    request:
      enabled: true
      mime-types: text/xml,application/xml,application/json
      min-request-size: 2048
    response:
      enabled: true
```

**超时配置**:
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

### 3.2 异步调用

**使用CompletableFuture**:
```java
@Async
public CompletableFuture<User> getUserAsync(Long id) {
    User user = userService.getUserById(id);
    return CompletableFuture.completedFuture(user);
}

// 并行调用
CompletableFuture<User> userFuture = getUserAsync(userId);
CompletableFuture<List<Role>> rolesFuture = getRolesAsync(userId);

CompletableFuture.allOf(userFuture, rolesFuture).join();
```

---

## 4. JVM优化

### 4.1 堆内存配置

**推荐配置**:
```bash
# 开发环境
-Xms512m -Xmx1g

# 测试环境
-Xms1g -Xmx2g

# 生产环境
-Xms2g -Xmx4g

# 通用参数
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/heapdump.hprof
```

### 4.2 GC优化

**G1 GC配置**:
```bash
-XX:+UseG1GC
-XX:G1HeapRegionSize=16m
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
-XX:ConcGCThreads=4
-XX:ParallelGCThreads=8
```

---

## 5. 线程池优化

### 5.1 异步线程池

**配置**:
```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 5.2 Tomcat线程池

**配置**:
```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 10000
    accept-count: 100
    connection-timeout: 20000
```

---

## 6. 网络优化

### 6.1 HTTP/2启用

```yaml
server:
  http2:
    enabled: true
```

### 6.2 Gzip压缩

```yaml
server:
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml
    min-response-size: 1024
```

---

## 7. 监控和调优

### 7.1 关键指标

**应用指标**:
- QPS (每秒请求数)
- 响应时间 (P50, P95, P99)
- 错误率
- 并发连接数

**JVM指标**:
- 堆内存使用率
- GC频率和时间
- 线程数
- CPU使用率

**数据库指标**:
- 连接池使用率
- 慢查询数量
- 锁等待时间
- 缓存命中率

### 7.2 性能测试

**JMeter测试脚本**:
```xml
<!-- 并发用户: 100 -->
<!-- 持续时间: 300秒 -->
<!-- 目标QPS: 1000 -->
```

**压测命令**:
```bash
# 使用ab工具
ab -n 10000 -c 100 http://localhost:8080/api/user/users

# 使用wrk工具
wrk -t12 -c400 -d30s http://localhost:8080/api/user/users
```

---

## 8. 代码优化

### 8.1 避免N+1查询

**不推荐**:
```java
List<User> users = userMapper.selectList(null);
for (User user : users) {
    List<Role> roles = roleMapper.selectByUserId(user.getId());
    user.setRoles(roles);
}
```

**推荐**:
```java
List<User> users = userMapper.selectList(null);
List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
List<UserRole> userRoles = userRoleMapper.selectByUserIds(userIds);
// 批量关联
```

### 8.2 批量操作

**批量插入**:
```java
@Transactional
public void batchInsert(List<User> users) {
    // 使用MyBatis-Plus批量插入
    userService.saveBatch(users, 1000);
}
```

### 8.3 懒加载

```java
@OneToMany(fetch = FetchType.LAZY)
private List<Role> roles;
```

---

## 9. 配置优化建议

### 9.1 Nacos配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        heart-beat-interval: 5000
        heart-beat-timeout: 15000
        ip-delete-timeout: 30000
```

### 9.2 RocketMQ配置

```yaml
rocketmq:
  producer:
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
    max-message-size: 4194304
  consumer:
    pull-batch-size: 32
```

---

## 10. 优化检查清单

### 启动优化
- [ ] 减少不必要的Bean扫描
- [ ] 延迟初始化非关键Bean
- [ ] 优化配置加载

### 运行时优化
- [ ] 启用缓存
- [ ] 优化数据库查询
- [ ] 使用连接池
- [ ] 启用Gzip压缩
- [ ] 配置合理的超时时间

### 资源优化
- [ ] 合理配置JVM参数
- [ ] 优化线程池大小
- [ ] 控制连接数
- [ ] 监控资源使用

### 代码优化
- [ ] 避免N+1查询
- [ ] 使用批量操作
- [ ] 异步处理耗时操作
- [ ] 合理使用事务

---

## 11. 性能目标

### 响应时间
- P50 < 100ms
- P95 < 500ms
- P99 < 1000ms

### 吞吐量
- QPS > 1000 (单实例)
- 并发用户 > 500

### 资源使用
- CPU < 70%
- 内存 < 80%
- 数据库连接 < 80%

---

## 12. 持续优化

### 监控
- 使用Prometheus + Grafana监控
- 设置告警规则
- 定期查看性能报告

### 测试
- 定期进行压力测试
- 模拟真实场景
- 记录性能基线

### 优化
- 根据监控数据优化
- 持续改进代码质量
- 定期review配置

---

**文档维护**: 架构团队  
**最后更新**: 2025-11-18
