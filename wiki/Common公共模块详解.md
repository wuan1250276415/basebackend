[< 返回首页](Home) | [< 上一页: 基础设施模块说明](基础设施模块说明)

---

# Common 公共模块详解

---

## 模块总览

`basebackend-common` 是一个聚合模块，包含 12 个子模块，为所有微服务提供公共基础能力。

| 子模块 | 职责 | 核心类/注解 |
|--------|------|------------|
| `common-core` | 核心模型 | `Result`, `PageResult`, `PageQuery`, `BusinessException`, `ErrorCode`, `BaseEntity` |
| `common-dto` | DTO 定义 | 跨服务共享的数据传输对象 |
| `common-util` | 工具类 | 通用工具方法 |
| `common-context` | 用户/租户上下文 | `UserContextHolder`, `TenantContextHolder` |
| `common-security` | 安全工具 | 公共安全相关工具 |
| `common-starter` | 自动配置 | Spring Boot Starter 聚合 |
| `common-storage` | 文件存储 SPI | Local / MinIO / S3 / OSS Provider |
| `common-lock` | 分布式锁 | `@DistributedLock` |
| `common-idempotent` | 幂等性 | `@Idempotent` |
| `common-datascope` | 数据权限 | `@DataScope` |
| `common-ratelimit` | 限流 | `@RateLimit` |
| `common-export` | 导出 | `ExportManager`, `AsyncExportService` |
| `common-event` | 领域事件 | `DomainEvent`, `ReliableDomainEventPublisher` |

---

## common-core 核心模型

### 统一响应 `Result<T>`

所有 API 接口使用统一的响应格式：

```java
// 成功响应
Result<UserVO> result = Result.success(userVO);

// 失败响应
Result<Void> result = Result.failure(ErrorCode.USER_NOT_FOUND);

// 带自定义消息
Result<Void> result = Result.failure("用户不存在");
```

响应 JSON 格式：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1708675200000
}
```

### 分页查询 `PageQuery` 与 `PageResult<T>`

```java
// 控制器接收分页参数
@GetMapping("/users")
public Result<PageResult<UserVO>> listUsers(PageQuery pageQuery) {
    PageResult<UserVO> result = userService.listUsers(pageQuery);
    return Result.success(result);
}
```

### 异常体系

```java
// 业务异常
throw new BusinessException(ErrorCode.USER_NOT_FOUND);
throw new BusinessException("自定义错误消息");

// ErrorCode 枚举
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    USER_NOT_FOUND(40401, "用户不存在"),
    PERMISSION_DENIED(40301, "权限不足"),
    // ...
}
```

### 基础实体 `BaseEntity`

```java
@Data
public abstract class BaseEntity {
    private Long id;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer deleted;  // 逻辑删除标记
}
```

---

## common-context 用户上下文

### `UserContextHolder`

在请求链路中传递当前登录用户信息：

```java
// 获取当前用户 ID
Long userId = UserContextHolder.getUserId();

// 获取当前用户名
String username = UserContextHolder.getUsername();

// 获取完整用户上下文
UserContext context = UserContextHolder.getContext();
```

### `TenantContextHolder`

多租户场景下传递租户信息：

```java
// 获取当前租户 ID
Long tenantId = TenantContextHolder.getTenantId();

// 设置租户（通常由过滤器自动设置）
TenantContextHolder.setTenantId(tenantId);
```

---

## common-lock 分布式锁

### `@DistributedLock` 注解

```java
/**
 * 使用分布式锁保护方法
 * key 支持 SpEL 表达式
 */
@DistributedLock(
    key = "'user:update:' + #userId",
    waitTime = 5,
    leaseTime = 30,
    timeUnit = TimeUnit.SECONDS
)
public void updateUser(Long userId, UserUpdateDTO dto) {
    // 业务逻辑 - 同一时间只有一个线程可以执行
}
```

### 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | String | - | 锁的 Key，支持 SpEL |
| `waitTime` | long | 3 | 等待获取锁的超时时间 |
| `leaseTime` | long | 30 | 持有锁的最长时间 |
| `timeUnit` | TimeUnit | SECONDS | 时间单位 |

### 实现
- **Redis 实现**（生产环境）：基于 Redisson 的 RLock
- **内存实现**（测试环境）：基于 JUC ReentrantLock

---

## common-idempotent 幂等性

### `@Idempotent` 注解

```java
/**
 * TOKEN 策略：客户端先获取 Token，请求时携带
 */
@Idempotent(strategy = IdempotentStrategy.TOKEN)
@PostMapping("/orders")
public Result<OrderVO> createOrder(@RequestBody OrderCreateDTO dto) {
    return Result.success(orderService.create(dto));
}

/**
 * PARAM 策略：基于请求参数生成唯一标识
 */
@Idempotent(
    strategy = IdempotentStrategy.PARAM,
    timeout = 5,
    timeUnit = TimeUnit.MINUTES,
    message = "请勿重复提交"
)
@PostMapping("/payment")
public Result<Void> pay(@RequestBody PaymentDTO dto) {
    paymentService.pay(dto);
    return Result.success();
}

/**
 * SPEL 策略：使用 SpEL 表达式生成唯一标识
 */
@Idempotent(
    strategy = IdempotentStrategy.SPEL,
    key = "'pay:' + #dto.orderId"
)
@PostMapping("/payment/confirm")
public Result<Void> confirm(@RequestBody PayConfirmDTO dto) {
    paymentService.confirm(dto);
    return Result.success();
}
```

### 三种策略

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| `TOKEN` | 客户端先获取幂等 Token | 表单提交 |
| `PARAM` | 基于请求参数 Hash | API 调用 |
| `SPEL` | 自定义 SpEL 表达式 | 需要精确控制 Key 的场景 |

---

## common-datascope 数据权限

### `@DataScope` 注解

```java
/**
 * 数据权限过滤 - 只查看本部门数据
 */
@DataScope(type = DataScopeType.DEPT)
public List<UserVO> listUsers() {
    // MyBatis 拦截器自动追加 SQL 条件
    // WHERE dept_id = #{currentUserDeptId}
    return userMapper.selectList(null);
}

/**
 * 数据权限 - 查看本部门及下级部门
 */
@DataScope(type = DataScopeType.DEPT_AND_BELOW)
public List<OrderVO> listOrders() {
    return orderMapper.selectList(null);
}

/**
 * 数据权限 - 仅查看本人数据
 */
@DataScope(type = DataScopeType.SELF)
public List<TaskVO> listMyTasks() {
    return taskMapper.selectList(null);
}
```

### 五种数据范围

| 类型 | 说明 | SQL 效果 |
|------|------|---------|
| `ALL` | 全部数据 | 无额外条件 |
| `DEPT` | 本部门数据 | `WHERE dept_id = ?` |
| `DEPT_AND_BELOW` | 本部门及下级 | `WHERE dept_id IN (?, ?, ...)` |
| `SELF` | 仅本人数据 | `WHERE create_by = ?` |
| `CUSTOM` | 自定义范围 | 自定义 SQL 条件 |

### 实现原理
通过 MyBatis 拦截器（Interceptor）在 SQL 执行前自动追加数据权限条件。

---

## common-ratelimit 限流

### `@RateLimit` 注解

```java
/**
 * 滑动窗口限流 - 每分钟最多 100 次
 */
@RateLimit(
    key = "'api:user:list'",
    count = 100,
    period = 60,
    algorithm = RateLimitAlgorithm.SLIDING_WINDOW
)
@GetMapping("/users")
public Result<List<UserVO>> listUsers() {
    return Result.success(userService.list());
}

/**
 * 令牌桶限流 - 每秒 10 个令牌
 */
@RateLimit(
    key = "'api:order:create'",
    count = 10,
    period = 1,
    algorithm = RateLimitAlgorithm.TOKEN_BUCKET
)
@PostMapping("/orders")
public Result<OrderVO> createOrder(@RequestBody OrderDTO dto) {
    return Result.success(orderService.create(dto));
}

/**
 * 基于用户 ID 的限流
 */
@RateLimit(
    key = "'api:upload:' + #userId",
    count = 5,
    period = 60,
    algorithm = RateLimitAlgorithm.FIXED_WINDOW,
    message = "上传频率过高，请稍后重试"
)
public Result<Void> upload(Long userId, MultipartFile file) {
    // ...
}
```

### 三种算法

| 算法 | 说明 | 特点 |
|------|------|------|
| `SLIDING_WINDOW` | 滑动窗口 | 精确限流，无突发 |
| `TOKEN_BUCKET` | 令牌桶 | 允许一定突发 |
| `FIXED_WINDOW` | 固定窗口 | 简单，有窗口边界问题 |

### 双实现
- **Redis 实现**（生产环境）：使用 Lua 脚本保证原子性
- **内存实现**（单机/测试）：基于本地 Map + 原子计数

---

## common-export 导出

### 使用 `ExportManager`

```java
@Autowired
private ExportManager exportManager;

/**
 * 同步导出 CSV
 */
@GetMapping("/export/csv")
public void exportCsv(HttpServletResponse response) {
    List<UserVO> data = userService.list();
    exportManager.exportCsv(response, "用户列表", UserVO.class, data);
}

/**
 * 同步导出 Excel
 */
@GetMapping("/export/excel")
public void exportExcel(HttpServletResponse response) {
    List<UserVO> data = userService.list();
    exportManager.exportExcel(response, "用户列表", UserVO.class, data);
}
```

### 异步大数据量导出

```java
@Autowired
private AsyncExportService asyncExportService;

/**
 * 异步导出（适合大数据量场景）
 */
@PostMapping("/export/async")
public Result<String> asyncExport() {
    String taskId = asyncExportService.submit(
        "用户导出",
        UserVO.class,
        pageNum -> userService.listByPage(pageNum, 10000)
    );
    return Result.success(taskId);
}

/**
 * 查询导出进度
 */
@GetMapping("/export/progress/{taskId}")
public Result<ExportProgress> getProgress(@PathVariable String taskId) {
    return Result.success(asyncExportService.getProgress(taskId));
}
```

---

## common-event 领域事件

### 定义领域事件

```java
public class UserCreatedEvent extends DomainEvent {
    private Long userId;
    private String username;

    public UserCreatedEvent(Long userId, String username) {
        super();
        this.userId = userId;
        this.username = username;
    }
}
```

### 发布事件

```java
@Autowired
private ReliableDomainEventPublisher eventPublisher;

public void createUser(UserCreateDTO dto) {
    // 业务逻辑
    User user = userMapper.insert(toEntity(dto));

    // 发布领域事件（可靠发布）
    eventPublisher.publish(new UserCreatedEvent(user.getId(), user.getUsername()));
}
```

### 订阅事件

```java
@Component
public class UserEventListener {

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        // 处理事件：发送欢迎邮件、初始化用户配置等
        log.info("用户创建: userId={}, username={}", event.getUserId(), event.getUsername());
    }
}
```

### 可靠发布机制

```
事件发布 → EventStore 持久化 → 异步推送
                                    │
                              ┌─────▼─────┐
                              │ 推送成功？  │
                              └─────┬─────┘
                                    │
                          ┌─────────┼─────────┐
                         成功      失败
                          │         │
                    标记已发布  EventRetryScheduler
                                    │
                              指数退避重试
                              (1s → 2s → 4s → ...)
```

- **EventStore**：JDBC 或内存实现，持久化事件
- **ReliableDomainEventPublisher**：保证事件至少发布一次
- **EventRetryScheduler**：定时重试失败事件，指数退避策略

---

## common-storage 文件存储

### 四种 Provider

| Provider | 说明 | 适用场景 |
|----------|------|---------|
| `Local` | 本地文件系统 | 开发/测试 |
| `MinIO` | MinIO 对象存储 | 自建对象存储 |
| `S3` | AWS S3 | AWS 云环境 |
| `OSS` | 阿里云 OSS | 阿里云环境 |

### 使用示例

```java
@Autowired
private StorageService storageService;

// 上传文件
String fileUrl = storageService.upload(file, "avatars/");

// 下载文件
InputStream stream = storageService.download(fileKey);

// 删除文件
storageService.delete(fileKey);
```

---

| [< 上一页: 基础设施模块说明](基础设施模块说明) | [下一页: JWT 认证体系 >](JWT认证体系) |
|---|---|
