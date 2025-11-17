# Metrics 指标体系使用指南

## 📊 概述

本项目提供了完整的可观测性指标体系，包括：

1. **系统指标** - JVM 内存、GC、线程、CPU、磁盘等
2. **业务指标** - 用户、订单、支付、消息等业务操作
3. **性能指标** - API 响应时间、QPS、错误率等
4. **自定义指标** - 通过注解轻松添加指标

## 🎯 快速开始

### 1. 系统指标（自动采集）

系统指标会自动采集，无需任何配置：

- `jvm_memory_used_bytes` - JVM 内存使用量
- `jvm_gc_pause_seconds` - GC 暂停时间
- `jvm_threads_live` - 线程数量
- `process_cpu_usage` - CPU 使用率
- `disk_free_bytes` - 磁盘剩余空间

### 2. API 指标（自动采集）

所有 Controller 方法会自动采集以下指标：

- `api_calls_total` - API 调用次数
- `api_response_time_seconds` - API 响应时间
- `api_errors_total` - API 错误次数
- `api_active_requests` - 当前活跃请求数

### 3. 业务指标（手动调用）

在业务代码中注入 `BusinessMetrics` 即可使用：

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final BusinessMetrics businessMetrics;

    public void registerUser(UserDTO user) {
        // 业务逻辑
        userRepository.save(user);

        // 记录用户注册指标
        businessMetrics.recordUserRegistration("web", true);
    }

    public boolean login(String username, String password) {
        boolean success = authenticate(username, password);

        // 记录登录指标
        businessMetrics.recordUserLogin("password", success);

        return success;
    }
}
```

### 4. 自定义注解（推荐）

使用注解可以更简洁地添加指标：

#### @Timed - 记录方法执行时间

```java
@Service
public class OrderService {

    @Timed(
        name = "order.processing.time",
        description = "Order processing time",
        tags = {"type", "standard"},
        percentiles = true
    )
    public void processOrder(Order order) {
        // 业务逻辑
        // 执行时间会自动记录到 Prometheus
    }
}
```

**生成的指标：**
- `order_processing_time_seconds_count` - 调用次数
- `order_processing_time_seconds_sum` - 总耗时
- `order_processing_time_seconds_max` - 最大耗时
- `order_processing_time_seconds{quantile="0.5"}` - P50
- `order_processing_time_seconds{quantile="0.9"}` - P90
- `order_processing_time_seconds{quantile="0.99"}` - P99

#### @Counted - 记录方法调用次数

```java
@Service
public class PaymentService {

    @Counted(
        name = "payment.attempts",
        description = "Payment attempts",
        tags = {"method", "alipay"},
        recordFailures = true
    )
    public void processPayment(PaymentRequest request) {
        // 业务逻辑
        // 成功和失败次数会分别记录
    }
}
```

**生成的指标：**
- `payment_attempts_total{result="success"}` - 成功次数
- `payment_attempts_total{result="failure"}` - 失败次数
- `payment_attempts_total{result="failure",exception="PaymentException"}` - 按异常类型统计

#### @Metered - 记录速率和响应时间

```java
@Service
public class NotificationService {

    @Metered(
        name = "notification.push",
        description = "Notification push rate",
        tags = {"channel", "email"}
    )
    public void pushNotification(Notification notification) {
        // 业务逻辑
        // 会同时记录调用次数和响应时间
    }
}
```

**生成的指标：**
- `notification_push_calls_total` - 调用次数
- `notification_push_time_seconds` - 响应时间（包含 P50/P90/P99）

## 📋 业务指标详解

### 用户相关指标

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final BusinessMetrics businessMetrics;

    // 用户注册
    public void registerUser(UserDTO user) {
        businessMetrics.recordUserRegistration("web", true);
        // 或 "mobile", "api" 等不同来源
    }

    // 用户登录
    public void login(String username, String password) {
        boolean success = authenticate(username, password);
        businessMetrics.recordUserLogin("password", success);
        // 或 "sms", "oauth" 等不同登录方式
    }

    // 更新在线用户数（定时任务）
    @Scheduled(fixedRate = 60000)
    public void updateOnlineUsers() {
        long count = countOnlineUsers();
        businessMetrics.updateOnlineUsers(count);
    }
}
```

**可用方法：**
- `recordUserRegistration(source, success)` - 记录用户注册
- `recordUserLogin(method, success)` - 记录用户登录
- `recordUserLogout()` - 记录用户登出
- `updateOnlineUsers(count)` - 更新在线用户数
- `updateActiveUsers(count)` - 更新活跃用户数
- `updateTotalUsers(count)` - 更新总用户数

### 订单相关指标

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final BusinessMetrics businessMetrics;

    // 创建订单
    public void createOrder(Order order) {
        businessMetrics.recordOrderCreation("standard", true);
        // 或 "express", "custom" 等订单类型
    }

    // 完成订单
    public void completeOrder(Order order, long processingTime) {
        businessMetrics.recordOrderCompletion("standard", processingTime);
    }

    // 取消订单
    public void cancelOrder(Order order, String reason) {
        businessMetrics.recordOrderCancellation("standard", reason);
        // reason: "user_cancelled", "timeout", "out_of_stock" 等
    }
}
```

**可用方法：**
- `recordOrderCreation(type, success)` - 记录订单创建
- `recordOrderCompletion(type, processingTimeMs)` - 记录订单完成
- `recordOrderCancellation(type, reason)` - 记录订单取消
- `updatePendingOrders(count)` - 更新待处理订单数

### 支付相关指标

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BusinessMetrics businessMetrics;

    // 支付请求
    public void requestPayment(PaymentRequest request) {
        businessMetrics.recordPaymentRequest(
            "alipay",  // 支付方式
            "CNY",     // 货币
            request.getAmount()
        );
    }

    // 支付成功
    public void onPaymentSuccess(Payment payment) {
        businessMetrics.recordPaymentSuccess(
            payment.getMethod(),
            payment.getCurrency(),
            payment.getAmount()
        );
    }

    // 支付失败
    public void onPaymentFailure(Payment payment, String reason) {
        businessMetrics.recordPaymentFailure(
            payment.getMethod(),
            reason  // "insufficient_funds", "network_error" 等
        );
    }
}
```

### 缓存相关指标

```java
@Service
@RequiredArgsConstructor
public class CacheService {

    private final BusinessMetrics businessMetrics;

    public User getUser(String userId) {
        User user = cache.get(userId);

        // 记录缓存命中/未命中
        businessMetrics.recordCacheAccess("userCache", user != null);

        if (user == null) {
            user = database.findById(userId);
            cache.put(userId, user);
        }

        return user;
    }
}
```

### 消息和通知指标

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BusinessMetrics businessMetrics;

    // 发送消息
    public void sendMessage(Message message) {
        boolean success = messageSender.send(message);
        businessMetrics.recordMessageSent(
            "email",      // 或 "sms", "push"
            "smtp",       // 渠道
            success
        );
    }

    // 推送通知
    public void pushNotification(Notification notification) {
        boolean success = notificationPusher.push(notification);
        businessMetrics.recordNotificationPushed(
            "order_status",  // 通知类型
            success
        );
    }
}
```

### 通用业务操作指标

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final BusinessMetrics businessMetrics;

    public void createProduct(Product product) {
        long startTime = System.currentTimeMillis();

        try {
            productRepository.save(product);

            // 记录操作成功
            businessMetrics.recordBusinessOperationResult("create", "product", true);
        } catch (Exception e) {
            // 记录操作失败
            businessMetrics.recordBusinessOperationResult("create", "product", false);
            throw e;
        } finally {
            // 记录操作耗时
            long duration = System.currentTimeMillis() - startTime;
            businessMetrics.recordBusinessOperationTime("create", "product", duration);
        }
    }
}
```

## 🔍 查询 Prometheus 指标

### 查看所有指标

访问：`http://localhost:8080/actuator/prometheus`

### 常用 PromQL 查询

#### API 性能查询

```promql
# API 平均响应时间（按接口）
rate(api_response_time_seconds_sum[5m]) / rate(api_response_time_seconds_count[5m])

# API 每秒请求数（QPS）
rate(api_calls_total[1m])

# API 错误率
rate(api_errors_total[5m]) / rate(api_calls_total[5m])

# API P99 响应时间
histogram_quantile(0.99, rate(api_response_time_seconds_bucket[5m]))
```

#### 业务指标查询

```promql
# 用户注册速率（每分钟）
rate(business_user_registrations_total{status="success"}[1m]) * 60

# 订单完成率
rate(business_order_completions_total[5m]) / rate(business_order_creations_total[5m])

# 支付成功率
rate(business_payment_success_total[5m]) / rate(business_payment_requests_total[5m])

# 缓存命中率
business_cache_hit_rate{cache="userCache"}
```

#### JVM 指标查询

```promql
# JVM 堆内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# GC 暂停时间（P99）
histogram_quantile(0.99, rate(jvm_gc_pause_seconds_bucket[5m]))

# 线程数
jvm_threads_live
```

## 🎨 Grafana 仪表板

### 推荐的仪表板布局

#### 1. API 性能仪表板
- QPS 趋势图
- 平均响应时间
- P95/P99 响应时间
- 错误率
- 活跃请求数

#### 2. 业务指标仪表板
- 用户注册/登录趋势
- 订单创建/完成趋势
- 支付成功率
- 消息发送统计

#### 3. 系统监控仪表板
- CPU 使用率
- 内存使用率
- GC 频率和暂停时间
- 线程数
- 磁盘空间

## ⚙️ 配置说明

### 启用/禁用指标

在 `observability-config.yml` 中配置：

```yaml
observability:
  metrics:
    enabled: true  # 全局开关
    custom-tags:
      enabled: true  # 自定义标签
```

### 自定义标签

所有指标都会自动添加以下标签：

- `application` - 应用名称
- `environment` - 环境（dev/test/prod）
- `region` - 区域
- `instance` - 实例标识

### 性能优化

**慢方法告警阈值：**

在 `MetricsAnnotationAspect` 中配置：

```java
if (duration > 500_000_000L) {  // 500ms
    log.warn("Slow method detected: ...");
}
```

**慢 API 告警阈值：**

在 `ApiMetricsAspect` 中配置：

```java
if (duration > 1000) {  // 1000ms
    log.warn("Slow API detected: ...");
}
```

## 📚 最佳实践

### 1. 合理使用标签

✅ **好的做法：**
```java
@Timed(name = "order.processing", tags = {"type", "express"})
```

❌ **不好的做法（高基数标签）：**
```java
@Timed(name = "order.processing", tags = {"orderId", orderId})
// orderId 是唯一值，会导致指标数量爆炸
```

### 2. 使用语义化的指标名称

✅ **好的做法：**
```java
@Timed(name = "user.registration.time")
@Counted(name = "user.login.attempts")
```

❌ **不好的做法：**
```java
@Timed(name = "method1")
@Counted(name = "count1")
```

### 3. 合理记录业务指标

```java
// ✅ 在关键业务节点记录
public void processOrder(Order order) {
    businessMetrics.recordOrderCreation("standard", true);
    // ...
}

// ❌ 在频繁调用的工具方法中记录
public String formatDate(Date date) {
    businessMetrics.incrementCounter("date.format");  // 过于频繁
    // ...
}
```

### 4. 异常处理

```java
@Counted(name = "payment.process", recordFailures = true)
public void processPayment(Payment payment) {
    // 异常会自动记录到 result="failure" 标签
    // 并添加 exception 标签
    if (payment.getAmount() <= 0) {
        throw new PaymentException("Invalid amount");
    }
}
```

## 🆘 常见问题

### Q1: 指标没有出现在 Prometheus 中？

**A:** 检查以下几点：
1. 确认 `observability.metrics.enabled=true`
2. 确认 `/actuator/prometheus` 端点可访问
3. 确认 Prometheus 配置了正确的抓取目标
4. 指标可能需要至少调用一次才会出现

### Q2: 注解不生效？

**A:**
1. 确认方法是 `public` 的
2. 确认类被 Spring 管理（有 `@Service`、`@Component` 等注解）
3. 确认方法不是在同一个类内部调用（AOP 限制）

### Q3: 如何减少指标数量？

**A:**
1. 避免使用高基数标签（如 ID、UUID）
2. 合理归类标签值（如将具体错误码归类为 "client_error" 和 "server_error"）
3. 使用 `sanitizeUri()` 将路径参数替换为占位符

## 🔗 参考资料

- [Micrometer 官方文档](https://micrometer.io/docs)
- [Prometheus 查询语法](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)

---

*最后更新: 2025-01-13*
*维护者: BaseBackend Team*
