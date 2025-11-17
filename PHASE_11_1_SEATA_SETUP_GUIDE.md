# Phase 11.1: Seata 分布式事务实施指南

## 📋 概述

Seata 是阿里巴巴开源的分布式事务解决方案，提供 AT、TCC、SAGA 和 XA 四种事务模式。本项目采用 **AT 模式**（自动回滚），因为它对代码侵入性小，配置简单。

---

## 🏗️ Seata AT 模式原理

### 工作流程
```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  TM (开始)   │────▶│   TC (协调器)  │◀────│  RM (资源)   │
│ 全局事务发起  │     │   事务管理    │     │  分支事务   │
└─────────────┘     └──────────────┘     └─────────────┘
     │                     │                    │
     │                     │                    │
     ▼                     ▼                    ▼
1. 生成全局事务ID     2. 协调分支事务      3. 执行本地事务
     │                     │                    │
     │                     │                    │
     ▼                     ▼                    ▼
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ 提交/回滚    │     │  记录日志      │     │  释放锁      │
│ 全局事务     │     │  (undo_log)   │     │  (本地锁)    │
└─────────────┘     └──────────────┘     └─────────────┘
```

### 核心概念
- **TC (Transaction Coordinator)**: 事务协调器，维护全局事务的运行状态
- **TM (Transaction Manager)**: 事务管理器，定义全局事务的范围
- **RM (Resource Manager)**: 资源管理器，管理分支事务处理的资源

---

## 🚀 Seata Server 部署

### 方式一：Docker 部署（推荐）

#### 1. 创建 docker-compose 配置

**docker-compose-seata.yml**:
```yaml
version: '3.8'

services:
  seata-server:
    image: seataio/seata-server:2.0.0
    container_name: seata-server
    ports:
      - "7091:7091"  # Seata Server 端口
      - "8091:8091"  # 注册与配置端口
    environment:
      - SEATA_PORT=8091
      - STORE_MODE=db
      - SERVER_MODE=standalone
    volumes:
      - ./seata-config:/seata-server/resources
    networks:
      - seata-network

  mysql-seata:
    image: mysql:8.0
    container_name: mysql-seata
    ports:
      - "3307:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: seata
    volumes:
      - ./seata-db:/docker-entrypoint-initdb.d
    networks:
      - seata-network

  nacos-seata:
    image: nacos/nacos-server:v2.3.2
    container_name: nacos-seata
    ports:
      - "8888:8848"
    environment:
      MODE: standalone
    networks:
      - seata-network

networks:
  seata-network:
    driver: bridge
```

#### 2. 初始化 Seata 数据库

**seata-db/init.sql**:
```sql
-- Seata 事务日志表
CREATE TABLE IF NOT EXISTS `global_table` (
  `xid` VARCHAR(128) NOT NULL,
  `transaction_id` BIGINT,
  `status` TINYINT NOT NULL,
  `application_id` VARCHAR(64),
  `transaction_service_group` VARCHAR(64),
  `transaction_name` VARCHAR(64),
  `timeout` INT,
  `begin_time` BIGINT,
  `application_data` VARCHAR(2000),
  `gmt_create` DATETIME,
  `gmt_modified` DATETIME,
  PRIMARY KEY (`xid`),
  KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
  KEY `idx_transaction_id` (`transaction_id`)
);

-- 分支事务表
CREATE TABLE IF NOT EXISTS `branch_table` (
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(128) NOT NULL,
  `transaction_id` BIGINT,
  `resource_group_id` VARCHAR(32),
  `resource_id` VARCHAR(256),
  `lock_key` VARCHAR(128),
  `lock_type` VARCHAR(16),
  `status` TINYINT NOT NULL,
  `client_id` VARCHAR(64),
  `application_data` VARCHAR(2000),
  `gmt_create` DATETIME,
  `gmt_modified` DATETIME,
  PRIMARY KEY (`branch_id`),
  KEY `idx_xid` (`xid`)
);

-- 锁表
CREATE TABLE IF NOT EXISTS `lock_table` (
  `row_key` VARCHAR(128) NOT NULL,
  `xid` VARCHAR(128),
  `transaction_id` BIGINT,
  `branch_id` BIGINT,
  `resource_id` VARCHAR(256),
  `table_name` VARCHAR(32),
  `pk` VARCHAR(36),
  `gmt_create` DATETIME,
  `gmt_modified` DATETIME,
  PRIMARY KEY (`row_key`)
);

-- 分布式会话表
CREATE TABLE IF NOT EXISTS `distributed_lock` (
  `lock_key` VARCHAR(128) NOT NULL,
  `locked` TINYINT NOT NULL DEFAULT 0,
  `locker` VARCHAR(64),
  `gmt_create` DATETIME,
  `gmt_modified` DATETIME,
  PRIMARY KEY (`lock_key`)
);

-- 插入初始数据
INSERT INTO distributed_lock (lock_key, locked) VALUES ('seata_flush_lock', 0);
```

#### 3. Seata Server 配置

**seata-config/application.yml**:
```yaml
server:
  port: 7091
  address:
    ip: 0.0.0.0
    port: 7091

spring:
  application:
    name: seata-server

logging:
  config: classpath:logback-spring.xml
  level:
    io.seata: INFO

console:
  user:
    username: admin
    password: admin

seata:
  config:
    type: nacos
    nacos:
      server-addr: nacos-seata:8848
      namespace:
      group: SEATA_GROUP
      username: nacos
      password: nacos
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: nacos-seata:8848
      namespace:
      group: SEATA_GROUP
      username: nacos
      password: nacos
  store:
    mode: db
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://mysql-seata:3306/seata?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
      user: root
      password: 123456
      min-conn: 1
      max-conn: 10
      global-table: global_table
      branch-table: branch_table
      lock-table: lock_table
      distributed-lock-table: distributed_lock
      query-limit: 100
      max-wait: 5000

security:
  secret:
    key: seataSecretKey0e228282319204768856573234047381747127281
    token-validity-in-seconds: 18000
  ignore:
    urls:
      - /**
```

#### 4. 启动命令

```bash
# 启动 Seata
docker-compose -f docker-compose-seata.yml up -d

# 查看日志
docker logs -f seata-server

# 访问控制台
# http://localhost:7091
# 用户名: admin, 密码: admin
```

### 方式二：Jar 包部署

#### 1. 下载 Seata

```bash
wget https://github.com/seata/seata/releases/download/v2.0.0/seata-server-2.0.0.tar.gz
tar -xzf seata-server-2.0.0.tar.gz
```

#### 2. 修改配置

修改 `seata-server/conf/application.yml`（参考上面配置）

#### 3. 初始化数据库

执行 `seata-db/init.sql` 脚本

#### 4. 启动服务

```bash
cd seata-server/bin
sh seata-server.sh
```

---

## 🔧 微服务集成 Seata

### 步骤 1: 添加依赖

**在每个微服务的 pom.xml 中添加**:
```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 步骤 2: 配置 application.yml

**basebackend-user-service**:
```yaml
seata:
  # 事务群组（可以按服务划分）
  tx-service-group: basebackend_tx_group
  # Seata Server 地址
  service:
    vgroup-mapping:
      basebackend_tx_group: default
    grouplist:
      default: seata-server:8091
  # 数据源配置
  data-source-proxy-mode: AT
  client:
    rm:
      async-commit-buffer-limit: 10000
      report-retry-count: 5
      tm-commit-retry-count: 3
      rollback-retry-count: 5
```

**basebackend-auth-service**:
```yaml
seata:
  tx-service-group: basebackend_auth_tx_group
  service:
    vgroup-mapping:
      basebackend_auth_tx_group: default
    grouplist:
      default: seata-server:8091
  data-source-proxy-mode: AT
```

**basebackend-application-service**:
```yaml
seata:
  tx-service-group: basebackend_app_tx_group
  service:
    vgroup-mapping:
      basebackend_app_tx_group: default
    grouplist:
      default: seata-server:8091
  data-source-proxy-mode: AT
```

### 步骤 3: 创建 undo_log 表

在每个微服务的数据库中创建 undo_log 表：

```sql
CREATE TABLE `undo_log` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(100) NOT NULL,
  `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGTEXT NOT NULL,
  `log_status` INT NOT NULL,
  `log_created_by` VARCHAR(32) NOT NULL,
  `log_modified_by` VARCHAR(32) NOT NULL,
  `ext` VARCHAR(100) DEFAULT NULL,
  `gmt_create` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 📝 分布式事务场景实现

### 场景 1: 用户创建 + 角色分配

**用户服务**:
```java
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleServiceClient roleServiceClient;

    /**
     * 创建用户并分配角色（分布式事务）
     */
    @GlobalTransactional(name = "create-user-and-assign-role", timeoutMills = 300000)
    public UserDTO createUserWithRole(UserCreateRequest request) {
        // 1. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCreateTime(new Date());
        userMapper.insert(user);

        // 2. 调用角色服务分配角色
        if (StringUtils.hasText(request.getRoleCode())) {
            roleServiceClient.assignRoleToUser(user.getId(), request.getRoleCode());
        }

        return convertToDTO(user);
    }
}
```

**角色服务**:
```java
@Service
public class RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    /**
     * 为用户分配角色（分支事务）
     */
    public void assignRoleToUser(Long userId, String roleCode) {
        // 1. 查询角色
        Role role = roleMapper.selectByCode(roleCode);
        if (role == null) {
            throw new BusinessException("角色不存在: " + roleCode);
        }

        // 2. 分配角色
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRole.setCreateTime(new Date());
        userRoleMapper.insert(userRole);
    }
}
```

### 场景 2: 权限变更 + 缓存刷新

**权限服务**:
```java
@Service
public class PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private CacheService cacheService;

    /**
     * 更新权限并刷新缓存（分布式事务）
     */
    @GlobalTransactional(name = "update-permission-and-refresh-cache")
    public void updatePermissionWithCache(PermissionUpdateRequest request) {
        // 1. 更新权限
        Permission permission = permissionMapper.selectById(request.getId());
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permissionMapper.updateById(permission);

        // 2. 刷新缓存
        cacheService.evictPermissionCache(request.getId());
    }
}
```

### 场景 3: 跨服务业务流程

**订单服务**:
```java
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    /**
     * 创建订单流程（跨多个服务）
     */
    @GlobalTransactional(name = "create-order-flow", timeoutMills = 600000)
    public OrderDTO createOrder(OrderCreateRequest request) {
        // 1. 扣减库存
        inventoryServiceClient.deductInventory(request.getItems());

        // 2. 创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setItems(request.getItems());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);
        orderMapper.insert(order);

        // 3. 调用支付服务（异步）
        paymentServiceClient.createPayment(order.getId(), request.getPaymentMethod());

        return convertToDTO(order);
    }
}
```

---

## 🔄 事务补偿机制

### 补偿接口定义

```java
/**
 * 事务补偿接口
 */
public interface TransactionCompensation {

    /**
     * 补偿操作
     */
    void compensate();

    /**
     * 补偿名称
     */
    String getCompensationName();
}
```

### 补偿实现示例

```java
/**
 * 用户创建补偿
 */
@Component
public class UserCreationCompensation implements TransactionCompensation {

    private final UserMapper userMapper;
    private Long userId;
    private String username;

    public UserCreationCompensation(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GlobalTransactional(name = "create-user-compensation", timeoutMills = 300000)
    public void execute(Long userId, String username) {
        this.userId = userId;
        this.username = username;

        try {
            // 1. 删除用户
            userMapper.deleteById(userId);

            // 2. 清理相关数据（角色关联、权限等）
            // ... 其他清理逻辑

        } catch (Exception e) {
            log.error("补偿操作失败，用户ID: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void compensate() {
        log.warn("执行用户创建补偿操作，用户ID: {}", userId);
        try {
            userMapper.deleteById(userId);
            log.info("用户创建补偿成功，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("用户创建补偿失败，用户ID: {}", userId, e);
            // 记录补偿失败日志
            logCompensationFailure(userId, e.getMessage());
        }
    }

    @Override
    public String getCompensationName() {
        return "用户创建补偿";
    }
}
```

### 全局事务异常处理

```java
@RestControllerAdvice
public class GlobalTransactionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalTransactionExceptionHandler.class);

    @ExceptionHandler(GlobalTransactionException.class)
    public Result<Void> handleGlobalTransactionException(GlobalTransactionException e) {
        log.error("全局事务异常: {}", e.getMessage(), e);

        return Result.failed("事务处理失败: " + e.getMessage());
    }

    @ExceptionHandler(TransactionException.class)
    public Result<Void> handleTransactionException(TransactionException e) {
        log.error("事务异常: {}", e.getMessage(), e);

        return Result.failed("事务执行失败: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);

        return Result.failed("系统错误: " + e.getMessage());
    }
}
```

---

## 📊 监控与告警

### 1. Seata Server 监控指标

**application.yml**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2. 自定义监控

```java
@Component
public class SeataMonitor {

    private final MeterRegistry meterRegistry;
    private final Counter transactionCommitCounter;
    private final Counter transactionRollbackCounter;
    private final Timer transactionTimer;

    public SeataMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transactionCommitCounter = Counter.builder("seata_transaction_commit_total")
                .description("Seata 事务提交总数")
                .register(meterRegistry);
        this.transactionRollbackCounter = Counter.builder("seata_transaction_rollback_total")
                .description("Seata 事务回滚总数")
                .register(meterRegistry);
        this.transactionTimer = Timer.builder("seata_transaction_duration")
                .description("Seata 事务执行时长")
                .register(meterRegistry);
    }

    public void recordTransaction(String type, Duration duration, boolean success) {
        if (success) {
            transactionCommitCounter.increment(Tags.of("type", type));
        } else {
            transactionRollbackCounter.increment(Tags.of("type", type));
        }
        transactionTimer.record(duration, Tags.of("type", type, "status", success ? "success" : "failure"));
    }
}
```

### 3. 告警规则

**Prometheus 告警规则** (seata-alerts.yml):
```yaml
groups:
  - name: seata_transaction
    rules:
      - alert: SeataTransactionFailureRateHigh
        expr: rate(seata_transaction_rollback_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Seata 事务回滚率过高"
          description: "事务回滚率: {{ $value }}"

      - alert: SeataTransactionTimeout
        expr: seata_transaction_duration_seconds{quantile="0.95"} > 30
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Seata 事务执行超时"
          description: "P95 事务执行时间: {{ $value }}s"
```

### 4. 健康检查接口

```java
@RestController
@RequestMapping("/api/monitor/seata")
public class SeataHealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Seata 事务健康检查
     */
    @GetMapping("/health")
    public Result<SeataHealth> checkHealth() {
        SeataHealth health = new SeataHealth();

        try {
            // 检查 Seata Server 连接
            boolean seataConnected = checkSeataServer();
            health.setSeataServerStatus(seataConnected);

            // 检查数据库连接
            boolean dbConnected = checkDatabase();
            health.setDatabaseStatus(dbConnected);

            // 检查活跃事务
            int activeTransactions = getActiveTransactionCount();
            health.setActiveTransactionCount(activeTransactions);

            // 统计信息
            health.setTransactionStats(getTransactionStats());

            if (seataConnected && dbConnected) {
                return Result.success(health);
            } else {
                return Result.failed("Seata 服务异常");
            }

        } catch (Exception e) {
            log.error("Seata 健康检查失败", e);
            return Result.failed("健康检查失败: " + e.getMessage());
        }
    }

    private boolean checkSeataServer() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "http://seata-server:8091/health", String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Seata Server 连接失败", e);
            return false;
        }
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1);
        } catch (Exception e) {
            log.error("数据库连接失败", e);
            return false;
        }
    }

    private int getActiveTransactionCount() {
        // 查询活跃事务数量
        // SELECT COUNT(*) FROM global_table WHERE status = 1;
        return 0;
    }

    private TransactionStats getTransactionStats() {
        // 查询事务统计信息
        // SELECT
        //   SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS committed,
        //   SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS rolled_back,
        //   AVG(CASE WHEN status IN (1, 2) THEN (gmt_modified - gmt_create) END) AS avg_duration
        // FROM global_table;
        return new TransactionStats();
    }
}
```

---

## 🧪 测试与验证

### 1. 单元测试

```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class DistributedTransactionTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleServiceClient roleServiceClient;

    @Test
    @Order(1)
    @Transactional
    @Rollback
    public void testCreateUserWithRole_Success() {
        // 准备数据
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("test_user");
        request.setPassword("123456");
        request.setEmail("test@example.com");
        request.setRoleCode("USER");

        // 执行
        UserDTO result = userService.createUserWithRole(request);

        // 验证
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test_user");
        // 验证角色分配成功
    }

    @Test
    @Order(2)
    @Transactional
    @Rollback
    public void testCreateUserWithRole_Rollback() {
        // 模拟角色服务调用失败
        doThrow(new RuntimeException("角色服务异常"))
            .when(roleServiceClient).assignRoleToUser(any(), any());

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("test_user_fail");
        request.setPassword("123456");
        request.setEmail("test@example.com");
        request.setRoleCode("USER");

        // 期望事务回滚
        assertThatThrownBy(() -> userService.createUserWithRole(request))
            .isInstanceOf(RuntimeException.class);

        // 验证用户未创建
        User user = userMapper.selectByUsername("test_user_fail");
        assertThat(user).isNull();
    }
}
```

### 2. 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SeataIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testDistributedTransaction() {
        // 通过 API 调用测试分布式事务
        // ...
    }
}
```

### 3. 压力测试

```java
@Component
public class SeataLoadTest {

    public void runLoadTest() {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                try {
                    // 执行分布式事务
                    userService.createUserWithRole(...);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }
}
```

---

## ⚠️ 注意事项

### 1. 事务分组配置
- 相同事务分组的服务共享同一个 Seata Server
- 建议按业务模块划分事务分组

### 2. 超时时间配置
- `@GlobalTransactional` 的 `timeoutMills` 要大于各分支事务的总执行时间
- 建议设置为 5-10 分钟

### 3. 异常处理
- Seata 会自动捕获 RuntimeException 进行回滚
- 如果使用 `try-catch`，需要手动抛出异常或调用 `GlobalTransactionContext.reload(xid).rollback()`

### 4. 数据源代理
- Seata 会自动代理 DataSource
- 不要手动配置其他数据源代理（如 DruidLogFilter）

### 5. 锁机制
- Seata 使用行级锁
- 高并发场景下注意避免热点数据

### 6. 性能影响
- AT 模式会记录 undo_log，对性能有轻微影响
- 建议在事务执行时间 < 3s 的场景使用

---

## 📈 性能优化建议

### 1. 优化事务粒度
- 减少跨服务的事务范围
- 将非必要操作移到事务外

### 2. 优化数据库
- 为 undo_log 表添加索引
- 定期清理过期日志

### 3. 优化 Seata Server
- 调整心跳间隔
- 优化线程池配置

### 4. 监控指标
- 实时监控事务成功率
- 关注事务执行时间

---

## 📚 参考资料

1. [Seata 官方文档](https://seata.io/)
2. [Seata AT 模式详解](https://seata.io/zh-cn/docs/overview/what-is-seata)
3. [Seata 配置详解](https://seata.io/zh-cn/docs/user/configurations)

---

**实施日期：** 2025-11-14
**负责人：** 浮浮酱 🐱（猫娘工程师）
**状态：** 📋 指南完成，准备实施
