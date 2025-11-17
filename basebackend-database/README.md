# basebackend-database 模块

数据库基础设施模块，提供数据库读写分离、动态数据源切换、连接池管理等功能。

## 功能特性

### 1. 数据库读写分离

通过主从数据库架构实现读写分离，提升系统并发能力和查询性能。

- **主库（Master）**：处理所有写操作（INSERT、UPDATE、DELETE）
- **从库（Slave）**：处理只读查询操作（SELECT）
- **动态路由**：基于注解和方法名自动切换数据源
- **透明切换**：业务代码无需感知数据源切换逻辑

### 2. 连接池管理

使用 Alibaba Druid 连接池，提供：
- 高性能连接池管理
- 慢 SQL 监控（默认 >1000ms）
- SQL 防火墙
- 监控统计（访问 `/druid` 查看）

### 3. 动态数据源

基于 Spring 的 `AbstractRoutingDataSource` 实现动态数据源路由，支持：
- ThreadLocal 上下文管理
- AOP 自动切面
- 优先级策略
- 事务感知

## 快速开始

### 1. 启用读写分离

在 Nacos 配置中心的 `common-config.yml` 中设置：

```yaml
spring:
  datasource:
    read-write-separation:
      enabled: true  # 启用读写分离
```

### 2. 配置主从数据源

```yaml
spring:
  datasource:
    # 主库配置
    master:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://master-host:3306/basebackend?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
      username: root
      password: your_password

    # 从库配置
    slave:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://slave-host:3307/basebackend?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
      username: root
      password: your_password
```

### 3. 使用注解控制数据源

#### 方法级别注解

```java
import com.basebackend.database.routing.ReadOnly;
import com.basebackend.database.routing.MasterOnly;

@Service
public class UserService {

    /**
     * 查询操作 - 使用从库
     */
    @ReadOnly
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 强制使用主库查询（实时性要求高）
     */
    @MasterOnly
    public User getUserByIdFromMaster(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 写操作 - 自动使用主库
     */
    @Transactional
    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

#### 类级别注解

```java
import com.basebackend.database.routing.ReadOnly;

/**
 * 整个类的所有方法都使用从库
 */
@ReadOnly
@Service
public class ReportService {

    public List<Report> getDailyReport() {
        return reportMapper.selectDailyReport();
    }

    public List<Report> getMonthlyReport() {
        return reportMapper.selectMonthlyReport();
    }

    /**
     * 特定方法需要主库时，使用 @MasterOnly 覆盖
     */
    @MasterOnly
    public Report getRealtimeReport() {
        return reportMapper.selectRealtimeReport();
    }
}
```

### 4. 自动路由规则

当没有显式注解时，系统根据方法名自动判断：

#### 自动使用从库的方法名前缀：
- `get*` - 如 `getUserById`
- `find*` - 如 `findUsersByRole`
- `select*` - 如 `selectUserList`
- `query*` - 如 `queryUsersByCondition`
- `count*` - 如 `countActiveUsers`
- `list*` - 如 `listAllUsers`
- `page*` - 如 `pageUsers`
- `search*` - 如 `searchUsers`
- `check*` - 如 `checkUserExists`
- `exist*` - 如 `existsByUsername`

#### 自动使用主库的场景：
- 带有 `@Transactional` 注解的方法
- 非查询方法名（如 `create*`、`update*`、`delete*`、`save*` 等）
- 带有 `@MasterOnly` 注解的方法

## 数据源路由优先级

系统按以下优先级决定数据源：

1. **@MasterOnly** - 最高优先级，强制使用主库
2. **@ReadOnly** - 使用从库
3. **@Transactional** - 使用主库（事务必须在主库执行）
4. **类级别注解** - 应用于整个类的所有方法
5. **方法名模式匹配** - 根据方法名前缀自动判断
6. **默认策略** - 写操作使用主库

## 使用建议

### ✅ 推荐做法

1. **明确标注查询方法**
```java
@ReadOnly
public List<User> findActiveUsers() {
    return userMapper.selectActiveUsers();
}
```

2. **实时性要求高的查询使用主库**
```java
@MasterOnly  // 避免主从延迟
public User getUserAfterCreate(Long id) {
    return userMapper.selectById(id);
}
```

3. **统计报表使用从库**
```java
@ReadOnly
public class ReportService {
    // 所有报表查询都使用从库，减轻主库压力
}
```

4. **事务操作明确使用 @Transactional**
```java
@Transactional
public void updateUserAndLog(User user, Log log) {
    userMapper.updateById(user);
    logMapper.insert(log);
}
```

### ❌ 避免的做法

1. **在事务中混用读写分离**
```java
// ❌ 错误：事务中不要使用 @ReadOnly
@Transactional
public void createUserAndQuery(User user) {
    userMapper.insert(user);  // 主库

    @ReadOnly  // 这个注解会被忽略！事务方法强制使用主库
    User result = userMapper.selectById(user.getId());
}
```

2. **写后立即读使用从库**
```java
// ❌ 错误：可能读到旧数据（主从延迟）
public void createAndVerify(User user) {
    userMapper.insert(user);  // 主库写入

    @ReadOnly  // 从库可能还没同步
    User verify = userMapper.selectById(user.getId());  // 可能返回 null！
}

// ✅ 正确：写后立即读使用主库
public void createAndVerify(User user) {
    userMapper.insert(user);

    @MasterOnly  // 从主库读取，确保能读到刚写入的数据
    User verify = userMapper.selectById(user.getId());
}
```

3. **过度使用 @MasterOnly**
```java
// ❌ 错误：普通查询没必要用主库
@MasterOnly
public List<User> getAllUsers() {
    return userMapper.selectList(null);
}

// ✅ 正确：普通查询使用从库
@ReadOnly
public List<User> getAllUsers() {
    return userMapper.selectList(null);
}
```

## 监控和调试

### 1. 查看 Druid 监控

访问：`http://localhost:8080/druid`
- 用户名：`admin`
- 密码：`admin123`

可以查看：
- SQL 执行统计
- 慢 SQL 记录
- 连接池状态
- URI 监控

### 2. 日志输出

系统会自动记录数据源切换日志（DEBUG 级别）：

```
2025-11-13 10:00:00.123 [main] DEBUG c.b.d.r.DataSourceAspect - 检测到 @ReadOnly 注解，使用从库: getUserById
2025-11-13 10:00:00.124 [main] DEBUG c.b.d.r.DynamicDataSource - 当前数据源: SLAVE
```

在 `application.yml` 或 Nacos 配置中设置日志级别：

```yaml
logging:
  level:
    com.basebackend.database.routing: DEBUG
```

### 3. 慢 SQL 监控

Druid 自动记录执行时间超过 1000ms 的 SQL：

```yaml
spring:
  datasource:
    druid:
      filter:
        stat:
          log-slow-sql: true
          slow-sql-millis: 1000  # 超过 1 秒记录为慢 SQL
```

## 性能优化建议

### 1. 连接池参数调优

根据实际负载调整连接池参数：

```yaml
spring:
  datasource:
    druid:
      initial-size: 10       # 初始连接数
      min-idle: 10           # 最小空闲连接
      max-active: 100        # 最大活跃连接
      max-wait: 60000        # 获取连接最大等待时间（ms）
```

### 2. 从库负载均衡

如果有多个从库，可以扩展 `DynamicDataSource` 实现负载均衡：

```java
// 未来扩展：支持多个从库
targetDataSources.put(DataSourceType.SLAVE_1, slaveDataSource1);
targetDataSources.put(DataSourceType.SLAVE_2, slaveDataSource2);
```

### 3. 读写比例监控

通过 Druid 监控查看主从库的访问比例，优化注解使用。

## 故障处理

### 从库不可用

系统配置了默认数据源为主库，当从库不可用时：

1. 设置 `read-write-separation.enabled=false` 禁用读写分离
2. 所有操作自动切换到主库
3. 无需修改业务代码

### 主从延迟

对实时性要求高的场景：

```java
// 写入后立即查询，强制使用主库
@Transactional
public User createUserAndReturn(User user) {
    userMapper.insert(user);
    return userMapper.selectById(user.getId());  // 事务中自动使用主库
}

// 或明确标注
public User createUserAndQuery(User user) {
    userMapper.insert(user);

    @MasterOnly  // 避免主从延迟
    return userMapper.selectById(user.getId());
}
```

## 技术架构

### 核心组件

1. **DataSourceType** - 数据源类型枚举（MASTER/SLAVE）
2. **DataSourceContextHolder** - ThreadLocal 上下文管理
3. **@ReadOnly / @MasterOnly** - 数据源路由注解
4. **DynamicDataSource** - 动态数据源实现
5. **DataSourceAspect** - AOP 切面自动路由
6. **DataSourceConfig** - Spring 配置类

### 工作流程

```
1. 请求进入 Service 方法
   ↓
2. DataSourceAspect 切面拦截
   ↓
3. 分析注解和方法名，确定数据源类型
   ↓
4. 设置 ThreadLocal 上下文
   ↓
5. DynamicDataSource 根据上下文路由到主库/从库
   ↓
6. 执行数据库操作
   ↓
7. finally 块清除 ThreadLocal 上下文
```

## 常见问题 (FAQ)

### Q1: 如何禁用读写分离？

A: 在 Nacos 配置中设置：
```yaml
spring:
  datasource:
    read-write-separation:
      enabled: false
```

### Q2: 事务中能使用 @ReadOnly 吗？

A: 不建议。`@Transactional` 方法会强制使用主库，`@ReadOnly` 注解会被忽略。

### Q3: 如何确认当前使用的是哪个数据源？

A: 开启 DEBUG 日志：
```yaml
logging:
  level:
    com.basebackend.database.routing: DEBUG
```

### Q4: 主从延迟如何处理？

A: 对实时性要求高的查询使用 `@MasterOnly` 强制从主库读取。

### Q5: 可以在 Mapper 层使用注解吗？

A: 建议在 Service 层使用，因为切面配置为 `execution(* com.basebackend..service..*.*(..))`。

## 贡献者

- 浮浮酱 - 架构设计与实现

## 更新日志

### 2025-11-13
- ✅ 实现数据库读写分离
- ✅ 添加动态数据源切换
- ✅ 集成 Druid 连接池
- ✅ 实现慢 SQL 监控
