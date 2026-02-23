[< 返回首页](Home) | [< 上一页: 消息队列](消息队列)

---

# API 网关

---

## 概述

`basebackend-gateway` 基于 Spring Cloud Gateway 构建，作为所有微服务的统一入口，运行在端口 **8180**，提供路由转发、认证鉴权、灰度发布、熔断限流、安全防护等能力。

---

## 核心配置

```yaml
server:
  port: 8080

spring:
  application:
    name: basebackend-gateway
  main:
    web-application-type: reactive    # WebFlux 响应式
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true               # 基于 Nacos 服务发现
          lower-case-service-id: true  # 服务名小写
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "*"
            allowed-methods: [GET, POST, PUT, DELETE, OPTIONS]
            allowed-headers: "*"
            max-age: 3600
```

---

## 过滤器链详解

Gateway 的过滤器按顺序执行，每个过滤器负责一个安全或功能关注点：

### 1. AuthenticationFilter

**职责**：JWT Token 验证

```
请求到达 → 检查路径是否在白名单
              │
         ┌────┼────┐
       白名单中    不在白名单
         │         │
       直接放行   提取 Authorization Header
                   │
              验证 Token
                   │
           ┌───────┼───────┐
         Token 有效      Token 无效/过期
              │              │
         提取用户信息      返回 401
         写入请求头
         继续执行
```

**白名单配置**：

```yaml
gateway:
  security:
    whitelist:
      - /basebackend-user-api/api/user/auth/**
      - /basebackend-user-api/swagger-ui/**
      - /basebackend-system-api/api/system/depts/tree
      - /basebackend-system-api/api/dicts/**
    actuator-whitelist:
      - /actuator/health
      - /actuator/health/**
      - /actuator/info
```

### 2. HeaderSanitizationFilter

**职责**：清理危险请求头，防止头注入攻击

移除或清理可能被利用的请求头（如 `X-Forwarded-*` 伪造等）。

### 3. SecurityHeadersFilter

**职责**：添加安全响应头

为每个响应添加安全相关的 HTTP 头（CSP、X-Frame-Options、HSTS 等）。

### 4. SignatureVerifyFilter

**职责**：API 请求签名验证

对支付、订单等敏感接口进行请求签名校验，防止请求篡改。

```yaml
gateway:
  security:
    signature:
      enabled: ${GATEWAY_SIGNATURE_ENABLED:false}
      secret-key: ${GATEWAY_SIGNATURE_SECRET:your-secret-key}
      timestamp-validity: 300000     # 签名有效期 5 分钟
      paths:
        - /api/payment/**
        - /api/orders/**
```

### 5. RequestSizeLimitFilter

**职责**：限制请求体大小，防止大请求攻击。

### 6. IdempotencyFilter

**职责**：接口幂等性检查，防止重复提交

```yaml
gateway:
  idempotency:
    enabled: ${GATEWAY_IDEMPOTENCY_ENABLED:false}
    ttl: 300                        # 幂等 Key 有效期 5 分钟
    paths:
      - /api/orders/create
      - /api/payment/**
```

### 7. ApiVersionFilter

**职责**：API 版本路由，支持按版本号路由到不同服务实例。

### 8. SlowRequestFilter

**职责**：慢请求检测与告警

```yaml
gateway:
  monitor:
    slow-request-threshold: 1000     # 1 秒
    very-slow-request-threshold: 3000 # 3 秒
    slow-request-log-enabled: true
```

### 9. AccessLogFilter

**职责**：记录所有请求的访问日志（请求路径、方法、耗时、状态码等）。

### 10. ResponseCacheFilter

**职责**：响应缓存，对 GET 请求的响应进行缓存。

### 11. TraceIdFilter

**职责**：注入链路追踪 ID（TraceId），传递到下游微服务。

### 12. SeataGatewayFilterConfig

**职责**：Seata 分布式事务上下文在网关层的传播。

---

## 路由配置

路由定义在 `application-routes.yml` 中：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-api
          uri: lb://basebackend-user-api
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/user

        - id: system-api
          uri: lb://basebackend-system-api
          predicates:
            - Path=/api/system/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: systemServiceCircuitBreaker
                fallbackUri: forward:/fallback/system
```

---

## 灰度发布

### 支持的灰度策略

| 策略 | 说明 | 参数 |
|------|------|------|
| `weight` | 按权重分配流量 | `weight: 10`（10% 灰度） |
| `header` | 按请求头路由 | `headerName` + `headerValue` |
| `ip` | 按 IP 路由 | IP 列表 |
| `user` | 按用户路由 | 用户 ID 列表 |

### 配置示例

```yaml
gateway:
  gray:
    enabled: true
    rules:
      # 按权重：10% 流量到 v2.0.0
      - serviceName: admin-api
        grayVersion: v2.0.0
        stableVersion: v1.0.0
        strategy: weight
        weight: 10

      # 按 Header：特定标记走灰度
      - serviceName: basebackend-demo-api
        grayVersion: v2.0.0
        stableVersion: v1.0.0
        strategy: header
        headerName: X-Gray-Flag
        headerValue: "true"
```

---

## 动态路由（Nacos）

Gateway 支持从 Nacos 动态加载路由配置，无需重启即可新增/修改路由。

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

服务注册到 Nacos 后，Gateway 自动生成路由：`/basebackend-xxx-api/**` → `lb://basebackend-xxx-api`。

---

## 熔断与限流

### Resilience4j 熔断器

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10          # 滑动窗口大小
        minimumNumberOfCalls: 5        # 最小请求数
        failureRateThreshold: 50       # 失败率阈值 (%)
        waitDurationInOpenState: 5s    # 熔断后等待时间
        automaticTransitionFromOpenToHalfOpenEnabled: true
  timelimiter:
    configs:
      default:
        timeoutDuration: 10s           # 超时时间
```

### 熔断状态

```
CLOSED (正常) ──失败率>50%──→ OPEN (熔断)
                                   │
                              等待 5 秒
                                   │
                                   ▼
                          HALF_OPEN (半开)
                           │            │
                     请求成功        请求失败
                           │            │
                      CLOSED        OPEN
```

---

| [< 上一页: 消息队列](消息队列) | [下一页: 调度系统 >](调度系统) |
|---|---|
