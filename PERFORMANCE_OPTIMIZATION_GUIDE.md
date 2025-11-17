# 微服务性能调优指南

## 📋 概述

本指南提供微服务架构的性能调优方案，涵盖数据库、缓存、连接池、JVM 参数、网络等各个层面的优化策略。

**目标性能指标：**
- API 响应时间：P95 < 200ms
- 并发用户数：1000+
- QPS：3000+
- 可用性：99.9%
- 数据库查询时间：< 50ms

---

## 🎯 调优优先级

### 高优先级（立即执行）
1. 数据库连接池优化
2. 索引优化
3. 慢查询优化
4. Redis 缓存配置

### 中优先级（本周执行）
1. JVM 参数调优
2. Gateway 路由优化
3. Feign 超时配置
4. 线程池优化

### 低优先级（下周执行）
1. CDN 加速
2. 数据库读写分离
3. 分库分表
4. 消息队列异步处理

---

## 🗄️ 数据库优化

### 1. 连接池优化

#### 1.1 Druid 连接池配置

**application.yml 配置：**

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      # 基础配置
      initial-size: 10          # 初始化连接数
      min-idle: 10              # 最小空闲连接数
      max-active: 50            # 最大活跃连接数
      max-wait: 60000           # 获取连接超时时间（毫秒）
      time-between-eviction-runs-millis: 60000    # 销毁线程运行时间间隔
      min-evictable-idle-time-millis: 300000      # 连接最小生存时间
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false

      # 监控配置
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        reset-enable: false
        login-username: admin
        login-password: admin

      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"

      # 监控过滤器
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 5000
        wall:
          enabled: true
          config:
            multi-statement-allow: true
```

#### 1.2 HikariCP 连接池（推荐）

如果使用 HikariCP：

```yaml
spring:
  datasource:
    hikari:
      # 连接池大小（推荐：CPU 核心数 * 2 + 磁盘数）
      maximum-pool-size: 20
      # 最小空闲连接数
      minimum-idle: 10
      # 连接超时时间
      connection-timeout: 30000
      # 空闲连接超时时间
      idle-timeout: 600000
      # 连接最大生存时间
      max-lifetime: 1800000
      # 测试连接是否有效的 SQL
      connection-test-query: SELECT 1
```

#### 1.3 连接池调优参数

| 参数 | 建议值 | 说明 |
|------|--------|------|
| `maximum-pool-size` | CPU 核心数 * 2 + 磁盘数 | 最大连接数 |
| `minimum-idle` | maximum-pool-size / 3 | 最小空闲连接数 |
| `connection-timeout` | 30000 (30秒) | 连接超时时间 |
| `idle-timeout` | 600000 (10分钟) | 空闲超时时间 |
| `max-lifetime` | 1800000 (30分钟) | 连接最大生存时间 |

### 2. 索引优化

#### 2.1 核心表索引检查

**用户表 (sys_user)：**
```sql
-- 主键索引（自动创建）
ALTER TABLE sys_user ADD PRIMARY KEY (id);

-- 用户名唯一索引
CREATE UNIQUE INDEX uk_username ON sys_user(username);

-- 邮箱唯一索引
CREATE UNIQUE INDEX uk_email ON sys_user(email);

-- 手机号唯一索引
CREATE UNIQUE INDEX uk_phone ON sys_user(phone);

-- 部门索引
CREATE INDEX idx_dept_id ON sys_user(dept_id);

-- 状态索引
CREATE INDEX idx_status ON sys_user(status);

-- 创建时间索引
CREATE INDEX idx_create_time ON sys_user(create_time);
```

**角色表 (sys_role)：**
```sql
-- 角色编码唯一索引
CREATE UNIQUE INDEX uk_role_code ON sys_role(role_code);

-- 状态索引
CREATE INDEX idx_role_status ON sys_role(status);
```

**部门表 (sys_dept)：**
```sql
-- 父部门索引
CREATE INDEX idx_parent_id ON sys_dept(parent_id);

-- 部门名称索引
CREATE INDEX idx_dept_name ON sys_dept(dept_name);

-- 部门状态索引
CREATE INDEX idx_dept_status ON sys_dept(status);
```

#### 2.2 慢查询分析

**启用慢查询日志：**
```sql
-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';

-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 记录超过1秒的查询
SET GLOBAL log_queries_not_using_indexes = 'ON';

-- 查看慢查询日志
SHOW GLOBAL STATUS LIKE 'Slow_queries';
```

**慢查询分析工具：**
```bash
# 使用 mysqldumpslow 分析慢查询日志
mysqldumpslow -s c -t 10 /var/log/mysql/mysql-slow.log

# 使用 pt-query-digest 分析（需要安装 Percona Toolkit）
pt-query-digest /var/log/mysql/mysql-slow.log
```

#### 2.3 索引使用情况分析

```sql
-- 查看表的索引使用情况
SHOW INDEX FROM sys_user;

-- 分析索引效率
EXPLAIN SELECT * FROM sys_user WHERE username = 'admin';
EXPLAIN SELECT * FROM sys_user WHERE dept_id = 1;

-- 查看未使用的索引
SELECT
    object_schema,
    object_name,
    index_name
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE object_schema = 'basebackend'
ORDER BY count_star ASC;
```

### 3. SQL 优化

#### 3.1 避免全表扫描

**❌ 错误的写法：**
```sql
SELECT * FROM sys_user WHERE status <> 0;

SELECT * FROM sys_user WHERE username LIKE '%admin%';
```

**✅ 正确的写法：**
```sql
SELECT id, username, nickname FROM sys_user WHERE status = 1;

SELECT id, username, nickname FROM sys_user WHERE username LIKE 'admin%';
```

#### 3.2 使用覆盖索引

**覆盖索引示例：**
```sql
-- 创建覆盖索引（包含所有查询字段）
CREATE INDEX idx_user_cover ON sys_user(status, dept_id, username, nickname, email, phone);
```

#### 3.3 分页优化

**❌ 传统的 LIMIT 分页：**
```sql
SELECT * FROM sys_user ORDER BY create_time DESC LIMIT 10000, 20;
-- 问题：偏移量越大，性能越差
```

**✅ 优化的分页：**
```sql
-- 方法1：使用子查询
SELECT * FROM sys_user
WHERE id > (SELECT id FROM sys_user ORDER BY create_time DESC LIMIT 10000, 1)
ORDER BY create_time DESC LIMIT 20;

-- 方法2：使用缓存（记录最后一页的ID）
SELECT * FROM sys_user WHERE last_id > 10000 ORDER BY create_time DESC LIMIT 20;
```

### 4. 读写分离

#### 4.1 配置主从复制

**主库（写）：**
```yaml
server-id = 1
log-bin = mysql-bin
binlog-do-db = basebackend
```

**从库（读）：**
```yaml
server-id = 2
relay-log = relay-bin
read-only = 1
log-bin = mysql-bin
```

#### 4.2 应用层读写分离

**配置：**
```yaml
spring:
  shardingsphere:
    rules:
      readwrite-splitting:
        data-sources:
          ds0:
            type: Static
            props:
              write-data-source-name: primary
              read-data-source-names: replica0,replica1
            load-balancer-name: round_robin
          load-balancers:
            round_robin:
              type: ROUND_ROBIN
```

**或者使用 MyBatis Plus 的读写分离：**
```java
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean("masterDataSource")
    public DataSource masterDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean("slaveDataSource")
    public DataSource slaveDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean
    public DataSource routingDataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put("master", masterDataSource());
        dataSourceMap.put("slave", slaveDataSource());
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(masterDataSource());
        return routingDataSource;
    }
}
```

---

## ⚡ 缓存优化

### 1. Redis 配置优化

#### 1.1 redis.conf 配置

```conf
# 内存配置
maxmemory 2gb                    # 最大内存
maxmemory-policy allkeys-lru     # 内存满时淘汰策略

# 持久化配置
save 900 1                       # 900秒内至少1个key变化时保存
save 300 10                      # 300秒内至少10个key变化时保存
save 60 10000                    # 60秒内至少10000个key变化时保存

# AOF 持久化
appendonly yes                   # 开启AOF
appendfsync everysec             # 每秒同步一次

# 网络优化
tcp-keepalive 300                # TCP保持连接时间
timeout 0                        # 不超时

# 慢日志
slowlog-log-slower-than 10000    # 记录超过10ms的命令
slowlog-max-len 128              # 最多记录128条慢日志
```

#### 1.2 Redis 连接池配置

```yaml
spring:
  data:
    redis:
      host: 1.117.67.222
      port: 6379
      password: redis_ycecQi
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8        # 最大连接数
          max-idle: 8          # 最大空闲连接数
          min-idle: 0          # 最小空闲连接数
          max-wait: -1ms       # 获取连接最大等待时间
```

### 2. 多级缓存架构

#### 2.1 缓存策略

**L1 Cache（本地缓存：Caffeine）**
- **存储内容**：热点数据，如用户权限、菜单列表
- **TTL**：10-30分钟
- **大小限制**：1000条记录

**L2 Cache（分布式缓存：Redis）**
- **存储内容**：用户信息、字典数据、会话信息
- **TTL**：1-24小时
- **持久化**：开启 AOF

**L3 Cache（数据库）**
- **存储内容**：所有数据
- **后备方案**：缓存穿透时回源

#### 2.2 Caffeine 本地缓存配置

```java
@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<String, Object> userCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)              // 最大缓存条数
            .expireAfterWrite(Duration.ofMinutes(10))  // 写后过期时间
            .expireAfterAccess(Duration.ofMinutes(5))   // 读后过期时间
            .recordStats()                  // 开启统计
            .build();
    }
}
```

#### 2.3 缓存更新策略

**方案1：Cache Aside（推荐）**
```java
public UserDTO getUserById(Long id) {
    // 1. 先查缓存
    UserDTO user = (UserDTO) redisTemplate.opsForValue().get("user:" + id);
    if (user != null) {
        return user;
    }

    // 2. 缓存未命中，查数据库
    user = userMapper.selectById(id);
    if (user != null) {
        // 3. 写入缓存
        redisTemplate.opsForValue().set("user:" + id, user, Duration.ofHours(1));
    }

    return user;
}

public void updateUser(UserDTO user) {
    // 1. 更新数据库
    userMapper.updateById(user);

    // 2. 删除缓存
    redisTemplate.delete("user:" + user.getId());
}
```

**方案2：Write Through**
```java
public void updateUser(UserDTO user) {
    // 1. 同时更新数据库和缓存
    userMapper.updateById(user);
    redisTemplate.opsForValue().set("user:" + user.getId(), user, Duration.ofHours(1));
}
```

**方案3：Write Behind**
```java
public void updateUser(UserDTO user) {
    // 1. 写入缓存
    redisTemplate.opsForValue().set("user:" + user.getId(), user, Duration.ofHours(1));

    // 2. 异步写入数据库
    asyncService.execute(() -> userMapper.updateById(user));
}
```

### 3. 缓存穿透防护

#### 3.1 布隆过滤器

**添加依赖：**
```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.3-jre</version>
</dependency>
```

**使用示例：**
```java
@Component
public class BloomFilterHelper {

    private BloomFilter<String> bloomFilter;

    @PostConstruct
    public void init() {
        // 预期插入10000个元素，误判率0.01
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 10000, 0.01);
    }

    public boolean mightContain(String key) {
        return bloomFilter.mightContain(key);
    }

    public void put(String key) {
        bloomFilter.put(key);
    }
}

// 使用布隆过滤器
public UserDTO getUserById(Long id) {
    String key = "user:" + id;

    // 1. 布隆过滤器检查
    if (!bloomFilterHelper.mightContain(key)) {
        return null;  // 直接返回，不查询缓存和数据库
    }

    // 2. 查询缓存
    UserDTO user = (UserDTO) redisTemplate.opsForValue().get(key);
    if (user != null) {
        return user;
    }

    // 3. 查询数据库
    user = userMapper.selectById(id);
    if (user != null) {
        redisTemplate.opsForValue().set(key, user, Duration.ofHours(1));
        bloomFilterHelper.put(key);
    }

    return user;
}
```

#### 3.2 缓存空值

```java
public UserDTO getUserById(Long id) {
    String key = "user:" + id;

    // 查询缓存
    UserDTO user = (UserDTO) redisTemplate.opsForValue().get(key);

    if (user == null) {
        // 缓存未命中，查询数据库
        user = userMapper.selectById(id);

        if (user != null) {
            // 缓存数据
            redisTemplate.opsForValue().set(key, user, Duration.ofHours(1));
        } else {
            // 缓存空值（防止缓存穿透）
            redisTemplate.opsForValue().set(key, new UserDTO(), Duration.ofMinutes(5));
        }
    }

    return user;
}
```

### 4. 缓存雪崩防护

#### 4.1 随机过期时间

```java
// 设置随机过期时间（±10%）
public void setCache(String key, Object value, Duration baseDuration) {
    long random = ThreadLocalRandom.current().nextLong(baseDuration.toMillis() / 10);
    Duration randomDuration = Duration.ofMillis(baseDuration.toMillis() + random);
    redisTemplate.opsForValue().set(key, value, randomDuration);
}
```

#### 4.2 分布式锁

```java
public UserDTO getUserByIdWithLock(Long id) {
    String key = "user:" + id;
    String lockKey = "lock:" + key;

    // 1. 查询缓存
    UserDTO user = (UserDTO) redisTemplate.opsForValue().get(key);
    if (user != null) {
        return user;
    }

    // 2. 获取分布式锁
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

    if (!acquired) {
        // 获取锁失败，等待后重试
        try {
            Thread.sleep(100);
            return getUserByIdWithLock(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    try {
        // 3. 双重检查（防止其他线程已经更新了缓存）
        user = (UserDTO) redisTemplate.opsForValue().get(key);
        if (user != null) {
            return user;
        }

        // 4. 查询数据库
        user = userMapper.selectById(id);

        // 5. 更新缓存
        if (user != null) {
            setCache(key, user, Duration.ofHours(1));
        }

        return user;
    } finally {
        // 6. 释放锁
        redisTemplate.delete(lockKey);
    }
}
```

---

## ⚙️ JVM 调优

### 1. 堆内存配置

#### 1.1 通用配置

```bash
# 启动参数（4GB内存服务器）
JAVA_OPTS="
  -Xms2g                     # 堆最小内存
  -Xmx2g                     # 堆最大内存
  -XX:NewRatio=3            # 新生代与老年代比例（1:3）
  -XX:SurvivorRatio=8       # Eden与Survivor比例（8:1:1）
  -XX:+UseG1GC              # 使用G1垃圾收集器
  -XX:MaxGCPauseMillis=200  # 最大GC暂停时间
  -XX:G1HeapRegionSize=16m  # G1堆区域大小
  -XX:+UseStringDeduplication  # 字符串去重
  -XX:+PrintGC              # 打印GC信息
  -XX:+PrintGCDetails       # 打印GC详情
  -XX:+PrintGCTimeStamps    # 打印GC时间戳
  -Xloggc:gc.log            # GC日志文件
"
```

#### 1.2 大内存配置

```bash
# 8GB内存服务器
JAVA_OPTS="
  -Xms4g
  -Xmx4g
  -XX:NewRatio=3
  -XX:SurvivorRatio=8
  -XX:+UseZGC              # 使用ZGC（Java 11+）
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseTransparentHugePages
"
```

#### 1.3 小内存配置

```bash
# 2GB内存服务器
JAVA_OPTS="
  -Xms512m
  -Xmx512m
  -XX:NewRatio=3
  -XX:SurvivorRatio=8
  -XX:+UseSerialGC         # 使用SerialGC
  -XX:+UseCompressedOops   # 启用压缩指针
  -XX:+UseCompressedClassPointers
"
```

### 2. 垃圾收集器选择

#### 2.1 G1GC（推荐）

**适用场景：**
- 堆内存大于4GB
- 需要低延迟
- 多核CPU

**配置：**
```bash
JAVA_OPTS="
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200    # 最大GC暂停时间目标
  -XX:G1HeapRegionSize=16m    # 堆区域大小
  -XX:+G1UseAdaptiveIHOP      # 自适应初始化堆占用阈值
  -XX:G1HeapWastePercent=5    # 堆浪费百分比
  -XX:+G1PrintHeapRegions     # 打印堆区域信息
"
```

#### 2.2 ZGC（Java 11+，实验性）

**适用场景：**
- 超大堆内存（>16GB）
- 极低延迟要求（<10ms）
- 支持着色指针的操作系统

**配置：**
```bash
JAVA_OPTS="
  -XX:+UseZGC
  -XX:SoftMaxHeapSize=4g      # 软最大堆大小
  -XX:+UnlockExperimentalVMOptions
"
```

#### 2.3 SerialGC

**适用场景：**
- 单核CPU
- 小内存（<512MB）
- 开发测试环境

**配置：**
```bash
JAVA_OPTS="
  -XX:+UseSerialGC
  -XX:+UseCompressedOops
  -XX:+UseCompressedClassPointers
"
```

### 3. 监控 JVM 性能

#### 3.1 JConsole 监控

```bash
# 启动时添加参数
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

然后使用 JConsole 连接到：`jconsole localhost:9999`

#### 3.2 VisualVM 监控

下载 VisualVM，连接远程 JVM：

```
jvisualvm --openjmx <hostname>:9999
```

#### 3.3 命令行工具

```bash
# 查看进程
jps -l

# 查看JVM参数
jinfo -flags <pid>

# 查看GC信息
jstat -gc <pid> 5s

# 查看堆内存
jstat -heap <pid>

# 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>

# 分析堆转储
jhat heap.hprof
```

---

## 🚪 Gateway 优化

### 1. 连接池配置

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        # 连接池配置
        pool:
          type: ELASTIC            # 连接池类型
          max-connections: 500     # 最大连接数
          max-idle-time: 20s       # 最大空闲时间
          max-life-time: 60s       # 连接最大生存时间
          acquire-timeout: 45s     # 获取连接超时时间
        # 代理配置
        proxy:
          use-proxy: false
        # SSL 配置
        ssl:
          use-insecure-trust-manager: true
```

### 2. 超时配置

```yaml
spring:
  cloud:
    gateway:
      # 全局超时配置
      httpclient:
        connect-timeout: 3000      # 连接超时时间
        response-timeout: 5s       # 响应超时时间
      # 路由特定超时配置
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100
            redis-rate-limiter.burst-capacity: 200
        - name: Retry
          args:
            retries: 3
            statuses: 500,502,504
            methods: GET,POST
            backoff:
              firstBackoff: 100ms
              maxBackoff: 1000ms
              factor: 2
```

### 3. 限流配置

```yaml
spring:
  cloud:
    gateway:
      redis-rate-limiter:
        include-headers: true
        replenish-rate: 100        # 令牌桶补充速率
        burst-capacity: 200        # 令牌桶容量
      # 基于用户ID限流
      default-filters:
        - name: RequestRateLimiter
          args:
            rate-limiter: "#{@userRateLimiter}"
            key-resolver: "#{@userKeyResolver}"
```

### 4. 缓存配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          cache-request-body: true    # 缓存请求体
          preserve-host-header: true  # 保留Host头
      # 全局缓存
      default-filters:
        - name: CacheRequestBody
          args:
            enabled: true
```

---

## 🔧 Feign 优化

### 1. 超时配置

```yaml
feign:
  client:
    config:
      # 全局配置
      default:
        connect-timeout: 5000      # 连接超时
        read-timeout: 10000        # 读取超时
        logger-level: BASIC        # 日志级别
      # 特定服务配置
      user-service:
        connect-timeout: 3000
        read-timeout: 5000
        retry-enabled: true
        retry-period: 2000
        max-attempts: 3
```

### 2. 连接池配置

```java
@Configuration
public class FeignConfig {

    @Bean
    public Client feignClient() {
        return new ApacheHttpClient(
            HttpClientBuilder.create()
                .setMaxConnTotal(100)        // 最大连接数
                .setMaxConnPerRoute(50)      // 每个路由最大连接数
                .setConnectionTimeToLive(30, TimeUnit.SECONDS)
                .setKeepAliveStrategy((response, headers) -> 30 * 1000)
                .build()
        );
    }
}
```

### 3. 压缩配置

```yaml
feign:
  compression:
    request:
      enabled: true
      min-request-size: 2048       # 最小压缩大小
      mime-types: text/xml,application/xml,application/json
    response:
      enabled: true
```

---

## 🧵 线程池优化

### 1. Spring Boot 线程池配置

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 10              # 核心线程数
        max-size: 20               # 最大线程数
        queue-capacity: 100        # 队列容量
        keep-alive: 60s            # 空闲线程存活时间
    scheduling:
      pool:
        size: 5                    # 调度线程池大小
```

### 2. 自定义线程池

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 3. 异步执行示例

```java
@Service
public class UserService {

    @Async("taskExecutor")
    public CompletableFuture<Void> sendNotification(Long userId) {
        // 异步发送通知
        notificationService.send(userId);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> updateStatistics() {
        // 异步更新统计信息
        statisticsService.update();
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## 📊 性能监控

### 1. Prometheus 指标

```java
@Component
public class CustomMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter userLoginCounter;
    private final Timer userLoginTimer;

    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.userLoginCounter = Counter.builder("user_login_total")
            .description("用户登录总次数")
            .register(meterRegistry);
        this.userLoginTimer = Timer.builder("user_login_duration")
            .description("用户登录耗时")
            .register(meterRegistry);
    }

    public void recordUserLogin(Duration duration) {
        userLoginCounter.increment();
        userLoginTimer.record(duration);
    }

    public void recordApiCall(String method, String uri, int statusCode) {
        meterRegistry.counter("api_calls_total",
            "method", method,
            "uri", uri,
            "status", String.valueOf(statusCode)
        ).increment();
    }
}
```

### 2. 自定义健康检查

```java
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                builder.up()
                    .withDetail("database", "Available")
                    .withDetail("validationQuery", "SELECT 1")
                    .build();
            } else {
                builder.down()
                    .withDetail("database", "Connection invalid")
                    .build();
            }
        } catch (Exception e) {
            builder.down()
                .withDetail("database", "Unavailable")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 3. 慢查询监控

```java
@Component
@ConditionalOnProperty("mybatis-plus.configuration.log-impl")
public class SlowSqlInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowSqlInterceptor.class);
    private static final long SLOW_SQL_THRESHOLD = 1000; // 1秒

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long executeTime = System.currentTimeMillis() - startTime;
            if (executeTime > SLOW_SQL_THRESHOLD) {
                MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
                log.warn("慢SQL检测: {} ms, SQL: {}",
                    executeTime,
                    mappedStatement.getSqlCommandType(),
                    mappedStatement.getBoundSql(invocation.getArgs()[1]).getSql()
                );

                // 发送告警
                alertService.sendSlowSqlAlert(mappedStatement, executeTime);
            }
        }
    }
}
```

---

## 📈 压测工具

### 1. Apache Bench (ab)

```bash
# 测试用户服务
ab -n 10000 -c 100 http://localhost:8180/api/users/by-username?username=admin

# 测试结果分析
# -n 请求总数
# -c 并发数
# -p POST数据文件
# -T 内容类型
```

### 2. wrk

```bash
# 安装 wrk
git clone https://github.com/wg/wrk.git
cd wrk && make

# 压测
./wrk -t12 -c400 -d30s http://localhost:8180/api/menus/tree

# 结果分析
# -t 线程数
# -c 连接数
# -d 测试时间
# --timeout 超时时间
```

### 3. JMeter

创建测试计划：

1. **线程组设置**
   - 线程数：100
   - Ramp-Up 时间：10秒
   - 循环次数：10

2. **HTTP 请求默认值**
   - 协议：http
   - 服务器：localhost
   - 端口：8180

3. **HTTP 请求**
   - 路径：`/api/users/by-username`
   - 参数：`username=admin`

4. **结果树**
   - 查看响应数据
   - 检查响应时间
   - 查看状态码

5. **聚合报告**
   - 平均响应时间
   - 吞吐量
   - 错误率

---

## 📝 调优检查清单

### 数据库优化
- [ ] 连接池配置合理
- [ ] 核心表索引已创建
- [ ] 慢查询已优化
- [ ] 读写分离已配置

### 缓存优化
- [ ] Redis 配置已优化
- [ ] 多级缓存已实现
- [ ] 缓存穿透已防护
- [ ] 缓存雪崩已防护

### JVM 调优
- [ ] 堆内存配置合理
- [ ] 垃圾收集器选择正确
- [ ] JVM 参数已优化
- [ ] 监控指标已配置

### Gateway 优化
- [ ] 连接池配置合理
- [ ] 超时时间已设置
- [ ] 限流规则已配置
- [ ] 缓存已启用

### Feign 优化
- [ ] 超时时间已设置
- [ ] 连接池已配置
- [ ] 压缩已启用
- [ ] 重试机制已配置

### 线程池优化
- [ ] 核心线程数合理
- [ ] 最大线程数合理
- [ ] 队列容量合理
- [ ] 拒绝策略合理

### 监控告警
- [ ] Prometheus 指标已配置
- [ ] Grafana 仪表板已创建
- [ ] 告警规则已配置
- [ ] 慢查询监控已启用

---

## 🎯 性能目标

| 指标 | 当前值 | 目标值 | 调优方案 |
|------|--------|--------|----------|
| API 平均响应时间 | 300ms | <100ms | 数据库优化 + 缓存 |
| API P95 响应时间 | 500ms | <200ms | JVM 调优 + 线程池优化 |
| 并发用户数 | 500 | 1000+ | Gateway 优化 + 连接池调优 |
| QPS | 1500 | 3000+ | 全链路优化 |
| 数据库查询时间 | 80ms | <50ms | 索引优化 + 读写分离 |
| 缓存命中率 | 70% | >95% | 多级缓存 + 预热 |
| 内存使用率 | 80% | <70% | JVM 调优 |
| CPU 使用率 | 75% | <60% | 代码优化 |

---

## 📚 参考资料

1. **MySQL 性能优化**
   - High Performance MySQL
   - MySQL 5.7 Reference Manual

2. **Redis 优化**
   - Redis 设计与实现
   - Redis 最佳实践

3. **JVM 调优**
   - 深入理解 Java 虚拟机
   - Java Performance: The Definitive Guide

4. **微服务性能优化**
   - Building Microservices
   - Microservices Patterns

---

**编制：** 浮浮酱 🐱
**日期：** 2025-11-14
**版本：** v1.0
