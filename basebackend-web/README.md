# BaseBackend Web 模块

Web层基础设施模块，提供全面的安全、性能优化和监控能力。

## 📋 功能概览

### 1. 限流系统 ⭐️

基于 **Sentinel** 的分布式限流组件

**特性：**
- QPS 限流控制
- 并发线程数限流
- 热点参数限流
- 熔断降级策略
- 动态规则配置

**使用方式：**
```java
@RestController
public class UserController {

    @RateLimit(
        resource = "user-api",
        threshold = 100.0,
        message = "请求过于频繁，请稍后重试"
    )
    @GetMapping("/api/user/{id}")
    public ApiResult<User> getUser(@PathVariable Long id) {
        // 业务逻辑
        return userService.getUser(id);
    }
}
```

### 2. 缓存系统

基于 **Redis + Redisson** 的分布式缓存

**特性：**
- 多级缓存支持（L1: Caffeine, L2: Redis）
- 布隆过滤器防穿透
- 分布式锁
- 缓存统计监控

**使用方式：**
```java
@Service
public class UserService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "user", key = "#id")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
}
```

### 3. 安全防护

**XSS 防护：**
- 自动过滤恶意脚本
- HTML 实体编码
- 注解式控制

**安全头设置：**
- X-Frame-Options
- X-Content-Type-Options
- Content-Security-Policy
- Strict-Transport-Security

**使用方式：**
```java
@RestController
@XssClean(strategy = XssClean.CleanStrategy.ESCAPE)
public class UserController {
    // 所有方法都会自动进行XSS防护
}
```

### 4. 跨域处理（CORS）

**特性：**
- 自动CORS配置
- 支持动态跨域策略
- 预检请求优化
- 凭证支持

**配置方式：**
```yaml
web:
  cors:
    enabled: true
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
    allow-credentials: true
    max-age: 3600
```

### 5. 性能监控

**拦截器：**
- 请求日志拦截器（记录请求详情）
- 性能监控拦截器（收集响应时间、并发数）
- 链路追踪拦截器（OpenTelemetry）

**指标收集：**
- HTTP 请求总量
- 响应时间统计
- 错误率统计
- 并发连接数

**使用方式：**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: metrics,health,prometheus
```

### 6. Gzip 压缩

**特性：**
- 自动压缩响应体
- 压缩策略配置
- 内容类型过滤
- 智能阈值控制

### 7. 幂等性控制 ⭐️

基于注解的幂等性保证

**特性：**
- 注解式配置
- 分布式锁集成
- 自动去重
- 过期时间控制

**使用方式：**
```java
@Idempotent(
    keyPrefix = "order-create",
    expireTime = 300L,
    strategy = Idempotent.Strategy.REJECT,
    message = "订单已提交，请勿重复操作"
)
@PostMapping("/api/order")
public ApiResult<Order> createOrder(@RequestBody OrderRequest request) {
    // 业务逻辑
    return orderService.createOrder(request);
}
```

## 📦 依赖引入

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-web</artifactId>
    <version>${project.version}</version>
</dependency>
```

## ⚙️ 配置说明

### Sentinel 配置

```yaml
sentinel:
  dashboard:
    url: http://localhost:8080
  transport:
    port: 8719
  datasource:
    flow:
      nacos:
        server-addr: localhost:8848
        dataId: sentinel-flow-rule
        groupId: SENTINEL_GROUP
```

### Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:
    database: 0

redisson:
  config: classpath:redisson-config.yml
```

### 安全头配置

```yaml
web:
  security:
    header:
      enabled: true
      frame-options: DENY
      content-security-policy: "default-src 'self'; script-src 'self'"
```

## 📊 监控端点

- `GET /actuator/metrics` - 指标查询
- `GET /actuator/prometheus` - Prometheus 格式指标
- `GET /actuator/health` - 健康检查

## 🧪 测试

```bash
# 编译项目
mvn clean compile

# 运行单元测试
mvn test

# 运行集成测试
mvn integration-test
```

## 📚 更多文档

- [Sentinel 官方文档](https://sentinelguard.io/)
- [Micrometer 文档](https://micrometer.io/)
- [OpenTelemetry 文档](https://opentelemetry.io/)

## 📄 版本记录

### v1.0.0 (2025-11-23)

- ✨ 初始版本发布
- 🎯 集成 Sentinel 限流系统
- 🎯 集成 Redis + Redisson 缓存
- 🎯 XSS 安全防护
- 🎯 跨域处理（CORS）
- 🎯 Gzip 压缩
- 🎯 性能监控拦截器
- 🎯 幂等性控制

---

**BaseBackend Web 模块** - 让你的 Web 应用更安全、更高效、更可观测 🚀
