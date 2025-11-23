# BaseBackend Common Starter

## 📖 模块简介

BaseBackend Common Starter 是一个 Spring Boot Starter，为项目提供开箱即用的通用功能集成。只需引入一个依赖，即可自动启用全局异常处理、Jackson 配置、上下文管理等核心功能。

### 🎯 核心特性

- ✅ **全局异常处理** - 统一异常响应格式，自动处理参数校验、HTTP 异常等
- ✅ **Jackson 序列化配置** - 统一日期格式、Long 转 String、空值处理等
- ✅ **上下文自动清理** - 自动清除用户/租户上下文，防止内存泄漏
- ✅ **配置属性管理** - 所有功能支持外部化配置，可灵活开关
- ✅ **零侵入集成** - 仅需引入依赖，无需额外配置即可使用

---

## 📦 快速开始

### 1. 添加依赖

在项目的 `pom.xml` 中添加 starter 依赖：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-common-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**注意：** 此 starter 会自动聚合以下子模块，无需重复引入：
- `basebackend-common-core` - 错误码、异常、分页等
- `basebackend-common-dto` - 通用响应结构（Result、PageResult）
- `basebackend-common-util` - 日期、字符串、ID 生成等工具类
- `basebackend-common-context` - 用户/租户上下文管理
- `basebackend-common-security` - 密钥管理、数据脱敏

### 2. 使用示例

引入依赖后，所有功能将自动启用，无需额外配置。

#### 示例 1：全局异常处理

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @PostMapping
    public Result<User> createUser(@Validated @RequestBody UserDTO userDTO) {
        // 参数校验失败时，GlobalExceptionHandler 会自动返回统一的错误响应
        // 无需手动捕获 MethodArgumentNotValidException

        User user = userService.create(userDTO);
        return Result.success(user);
    }

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        // 业务异常会被自动捕获并转换为统一格式
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(CommonErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        return Result.success(user);
    }
}
```

**错误响应示例：**

```json
{
  "code": 1001,
  "message": "参数校验失败: username: 用户名不能为空; email: 邮箱格式错误",
  "data": null,
  "success": false,
  "timestamp": "2025-11-24 09:40:00"
}
```

#### 示例 2：Jackson 序列化配置

```java
@Data
public class UserVO {
    private Long id;                    // 自动转为 String，避免前端精度丢失
    private String username;
    private LocalDateTime createTime;   // 自动格式化为 yyyy-MM-dd HH:mm:ss
    private String nickname;            // null 值默认不包含在 JSON 中
}
```

**JSON 响应示例：**

```json
{
  "code": 200,
  "message": "请求成功",
  "data": {
    "id": "1234567890123456789",
    "username": "admin",
    "createTime": "2025-11-24 09:40:00"
  },
  "success": true,
  "timestamp": "2025-11-24 09:40:00"
}
```

#### 示例 3：用户上下文管理

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/current-user")
    public Result<Map<String, Object>> getCurrentUser() {
        // 从上下文中获取当前用户信息
        // ContextCleanupFilter 会在请求结束时自动清理

        Long userId = UserContextHolder.getUserId();
        String username = UserContextHolder.getUsername();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", username);

        return Result.success(userInfo);
    }

    @GetMapping("/tenant-info")
    public Result<Map<String, Object>> getTenantInfo() {
        // 获取当前租户信息
        Long tenantId = TenantContextHolder.getTenantId();
        String tenantCode = TenantContextHolder.getTenantCode();

        Map<String, Object> tenantInfo = new HashMap<>();
        tenantInfo.put("tenantId", tenantId);
        tenantInfo.put("tenantCode", tenantCode);

        return Result.success(tenantInfo);
    }
}
```

---

## ⚙️ 配置说明

所有功能支持通过 `application.yml` 进行外部化配置：

```yaml
basebackend:
  common:
    # 是否启用通用模块（默认 true）
    enabled: true

    # 全局异常处理配置
    exception:
      # 是否启用全局异常处理（默认 true）
      enabled: true
      # 是否在响应中包含异常堆栈（默认 false，生产环境建议关闭）
      include-stack-trace: false
      # 是否记录异常日志（默认 true）
      log-enabled: true
      # 是否记录请求信息（URI、参数等）（默认 true）
      log-request-info: true

    # Jackson 序列化配置
    jackson:
      # 是否启用 Jackson 自动配置（默认 true）
      enabled: true
      # 日期时间格式（默认 yyyy-MM-dd HH:mm:ss）
      date-format: yyyy-MM-dd HH:mm:ss
      # 时区（默认 GMT+8）
      time-zone: GMT+8
      # 序列化时是否包含 null 值字段（默认 false）
      include-nulls: false
      # 是否启用驼峰命名转下划线（默认 false）
      snake-case-enabled: false
      # 是否在遇到未知属性时失败（默认 false）
      fail-on-unknown-properties: false

    # 上下文管理配置
    context:
      # 是否启用上下文自动清理（默认 true）
      auto-cleanup: true
      # 上下文清理过滤器的执行顺序（默认 Integer.MIN_VALUE + 100）
      filter-order: -2147483548
```

### 配置项说明

#### 异常处理配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `basebackend.common.exception.enabled` | Boolean | true | 是否启用全局异常处理 |
| `basebackend.common.exception.include-stack-trace` | Boolean | false | 是否在响应中包含异常堆栈信息（生产环境建议关闭） |
| `basebackend.common.exception.log-enabled` | Boolean | true | 是否记录异常日志 |
| `basebackend.common.exception.log-request-info` | Boolean | true | 是否在日志中记录请求信息（URI、参数等） |

#### Jackson 配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `basebackend.common.jackson.enabled` | Boolean | true | 是否启用 Jackson 自动配置 |
| `basebackend.common.jackson.date-format` | String | yyyy-MM-dd HH:mm:ss | 日期时间格式 |
| `basebackend.common.jackson.time-zone` | String | GMT+8 | 时区设置 |
| `basebackend.common.jackson.include-nulls` | Boolean | false | 序列化时是否包含 null 值字段 |
| `basebackend.common.jackson.snake-case-enabled` | Boolean | false | 是否启用驼峰命名转下划线 |
| `basebackend.common.jackson.fail-on-unknown-properties` | Boolean | false | 遇到未知属性时是否抛出异常 |

#### 上下文配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `basebackend.common.context.auto-cleanup` | Boolean | true | 是否启用上下文自动清理 |
| `basebackend.common.context.filter-order` | Integer | -2147483548 | 上下文清理过滤器的执行顺序 |

---

## 📚 功能详解

### 1. 全局异常处理

`GlobalExceptionHandler` 统一处理所有未捕获的异常，将其转换为标准的 `Result` 响应。

#### 支持的异常类型

| 异常类型 | HTTP 状态码 | 说明 |
|----------|-------------|------|
| `BusinessException` | 根据错误码 | 业务异常，使用异常中的错误码和消息 |
| `MethodArgumentNotValidException` | 400 | @Validated 参数校验失败（表单对象） |
| `BindException` | 400 | @Validated 参数校验失败（绑定异常） |
| `ConstraintViolationException` | 400 | @Validated 参数校验失败（方法参数） |
| `MissingServletRequestParameterException` | 400 | 缺少必需请求参数 |
| `MethodArgumentTypeMismatchException` | 400 | 参数类型不匹配 |
| `HttpMessageNotReadableException` | 400 | HTTP 消息不可读（JSON 解析失败） |
| `HttpRequestMethodNotSupportedException` | 405 | 请求方法不支持 |
| `HttpMediaTypeNotSupportedException` | 415 | 媒体类型不支持 |
| `NoHandlerFoundException` | 404 | 请求的资源不存在 |
| `IllegalArgumentException` | 400 | 非法参数异常 |
| `IllegalStateException` | 500 | 非法状态异常 |
| `Exception` | 500 | 其他未知异常 |

#### 异常处理示例

```java
// 业务异常
throw new BusinessException(CommonErrorCode.DATA_NOT_FOUND, "用户不存在");

// 参数错误
throw BusinessException.paramError("参数不能为空");

// 业务规则违反
throw new BusinessException(CommonErrorCode.BUSINESS_RULE_VIOLATION, "账户余额不足");
```

### 2. Jackson 序列化配置

`JacksonAutoConfiguration` 提供统一的 JSON 序列化/反序列化规则。

#### 核心功能

1. **日期时间格式化**
   - `LocalDateTime` → `yyyy-MM-dd HH:mm:ss`
   - `LocalDate` → `yyyy-MM-dd`
   - `LocalTime` → `HH:mm:ss`

2. **Long 类型转 String**
   - 避免前端 JavaScript 精度丢失（JavaScript 的 Number 类型最大安全整数为 2^53 - 1）
   - 自动将 `Long`、`long`、`BigInteger` 序列化为字符串

3. **空值处理**
   - 默认不包含 `null` 字段（可配置）
   - 减少响应体大小

4. **命名策略**
   - 支持驼峰命名转下划线（可选）
   - 例如：`userName` → `user_name`

5. **未知属性处理**
   - 默认忽略未知字段（反序列化时不报错）

### 3. 上下文自动清理

`ContextCleanupFilter` 在请求结束时自动清除用户/租户上下文，防止内存泄漏。

#### 工作原理

1. 过滤器在请求链的最前端执行（Order = Integer.MIN_VALUE + 100）
2. 在请求处理完成后（finally 块），自动清除上下文
3. 无论是否发生异常，都会执行清理操作

#### 清理的上下文

- `UserContextHolder` - 用户上下文
- `TenantContextHolder` - 租户上下文

---

## 🔧 高级用法

### 1. 自定义错误码

```java
@Getter
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(10001, "用户不存在", 404),
    USER_DISABLED(10002, "用户已被禁用", 403),
    USERNAME_EXISTS(10003, "用户名已存在", 409);

    private final Integer code;
    private final String message;
    private final Integer httpStatus;

    UserErrorCode(Integer code, String message, Integer httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getModule() {
        return "user";
    }
}
```

### 2. 禁用某个功能

```yaml
# 禁用全局异常处理
basebackend:
  common:
    exception:
      enabled: false

# 禁用 Jackson 自动配置
basebackend:
  common:
    jackson:
      enabled: false

# 禁用上下文自动清理
basebackend:
  common:
    context:
      auto-cleanup: false
```

### 3. 生产环境配置建议

```yaml
basebackend:
  common:
    exception:
      enabled: true
      # 生产环境不暴露堆栈信息
      include-stack-trace: false
      log-enabled: true
      log-request-info: true
    jackson:
      enabled: true
      # 不包含 null 字段，减少响应体大小
      include-nulls: false
      # 忽略未知属性，增强兼容性
      fail-on-unknown-properties: false
    context:
      auto-cleanup: true
```

---

## 📝 常见问题

### Q1: 如何查看 starter 是否生效？

**A:** 启动应用时，查看日志输出：

```
INFO  c.b.c.s.c.JacksonAutoConfiguration - Initializing Jackson auto-configuration with dateFormat=yyyy-MM-dd HH:mm:ss, timeZone=GMT+8, includeNulls=false, snakeCaseEnabled=false
INFO  c.b.c.s.f.ContextCleanupFilter - Context cleanup filter initialized with order=-2147483548
```

### Q2: 如何自定义异常响应格式？

**A:** 创建自己的 `@RestControllerAdvice`，并设置更高的优先级（`@Order(-1)`）：

```java
@Slf4j
@RestControllerAdvice
@Order(-1)  // 优先级高于 GlobalExceptionHandler
public class CustomExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public CustomResult handleBusinessException(BusinessException e) {
        // 自定义响应格式
        return CustomResult.error(e.getCode(), e.getMessage());
    }
}
```

### Q3: Long 类型为什么要转成 String？

**A:** JavaScript 的 `Number` 类型最大安全整数为 `2^53 - 1`（即 `9007199254740991`），超过此值会丢失精度。将 Long 转为 String 可以避免前端精度丢失问题。

### Q4: 如何在过滤器中设置用户上下文？

**A:** 创建一个认证过滤器，在 `ContextCleanupFilter` 之后执行：

```java
@Component
@Order(Integer.MIN_VALUE + 200)  // 在 ContextCleanupFilter 之后
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 从请求中解析用户信息
        Long userId = extractUserId(request);
        String username = extractUsername(request);

        // 设置用户上下文
        UserContextInfo userContext = new SimpleUserContext(userId, username);
        UserContextHolder.set(userContext);

        // 继续执行过滤器链
        chain.doFilter(request, response);

        // 无需手动清理，ContextCleanupFilter 会自动清理
    }
}
```

---

## 🎓 最佳实践

1. **异常处理**
   - 业务异常统一使用 `BusinessException` 抛出
   - 自定义错误码实现 `ErrorCode` 接口
   - 不要在 Controller 层捕获异常，交给 `GlobalExceptionHandler` 处理

2. **上下文管理**
   - 只在 Filter/Interceptor 中设置上下文
   - 不要在业务代码中手动清理上下文（由 `ContextCleanupFilter` 自动清理）
   - 跨租户查询使用 `TenantContextHolder.ignoreTenant()` 方法

3. **配置管理**
   - 开发环境可以开启 `include-stack-trace` 便于调试
   - 生产环境关闭 `include-stack-trace` 保护系统安全
   - 根据实际需求调整 `include-nulls` 和 `snake-case-enabled`

---

## 📄 模块结构

```
basebackend-common-starter
├── config/
│   └── JacksonAutoConfiguration.java       # Jackson 序列化配置
├── exception/
│   └── GlobalExceptionHandler.java         # 全局异常处理器
├── filter/
│   └── ContextCleanupFilter.java           # 上下文清理过滤器
├── properties/
│   └── CommonProperties.java               # 统一配置属性
└── CommonAutoConfiguration.java            # 自动配置聚合入口
```

---

## 📚 相关文档

- [basebackend-common-core](../basebackend-common-core/README.md) - 核心模块（错误码、异常、分页等）
- [basebackend-common-dto](../basebackend-common-dto/README.md) - 通用 DTO（Result、PageResult）
- [basebackend-common-util](../basebackend-common-util/README.md) - 工具类（日期、字符串、ID 生成等）
- [basebackend-common-context](../basebackend-common-context/README.md) - 上下文管理（用户/租户上下文）
- [basebackend-common-security](../basebackend-common-security/README.md) - 安全模块（密钥管理、数据脱敏）

---

## 📞 联系我们

如有问题或建议，请联系 BaseBackend Team。

---

**BaseBackend Common Starter** - 让 Spring Boot 开发更简单 🚀
