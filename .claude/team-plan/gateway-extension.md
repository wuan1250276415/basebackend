# Team Plan: Gateway Extension

## 概述
对 basebackend-gateway 进行 10 项能力扩展：安全响应头、CORS 收紧、敏感头清洗、请求体限制、OTel 替换、结构化访问日志、恢复 Observability 集成、API 版本路由、响应缓存、Sentinel 重新启用并修正规则。

## Codex 分析摘要
Codex 进程超时（模型刷新失败），未获取有效输出。

## Gemini 分析摘要
Gemini API 返回 403 PERMISSION_DENIED，不可用。

## 技术方案

### 架构决策
- 所有新增能力均实现为独立的 `GlobalFilter`（WebFlux 响应式），通过 `@ConfigurationProperties` 绑定配置，支持 Nacos 热更新
- Filter 执行顺序按安全→追踪→业务→监控排列
- OTel 集成采用 `opentelemetry-javaagent` 自动检测 + SDK 手动补充的混合模式
- 响应缓存基于 Caffeine 本地缓存（已有依赖），避免引入额外 Redis 网络开销
- Sentinel 修正需对齐当前路由表（user-api / system-api / auth-api / notification / observability / file）

### Filter 执行顺序设计
```
Order                Filter                     职责
───────────────────────────────────────────────────────────
HIGHEST_PRECEDENCE   SlowRequestFilter          性能计时（保留）
HIGHEST_PRECEDENCE+1 OTelTraceFilter            OTel traceparent 注入（替换 TraceIdFilter）
-200                 HeaderSanitizationFilter   剥离伪造敏感头
-190                 SecurityHeadersFilter      注入安全响应头
-180                 RequestSizeLimitFilter     请求体大小限制
-150                 SignatureVerifyFilter       签名验证（已有）
-100                 AuthenticationFilter        JWT 认证（已有）
-50                  IdempotencyFilter           幂等检查（已有）
-40                  ApiVersionFilter            API 版本路由
-30                  ResponseCacheFilter         GET 响应缓存
-20                  AccessLogFilter             结构化访问日志
```

## 子任务列表

### Task 1: Dependencies Update (pom.xml)
- **类型**: 后端基础设施
- **文件范围**:
  - `basebackend-gateway/pom.xml`
- **依赖**: 无
- **实施步骤**:
  1. 取消注释 `basebackend-observability` 依赖
  2. 添加 OpenTelemetry 依赖:
     - `io.opentelemetry:opentelemetry-api`
     - `io.opentelemetry:opentelemetry-sdk`
     - `io.opentelemetry.instrumentation:opentelemetry-reactor-3.1` (WebFlux 支持)
  3. 确认 `caffeine` 依赖已存在（用于响应缓存）
  4. 确认 `spring-boot-starter-data-redis-reactive` 已存在
- **验收标准**: `mvn dependency:tree -pl basebackend-gateway` 无冲突，所有新依赖正确引入

### Task 2: Security Headers Filter
- **类型**: 后端
- **文件范围**:
  - NEW: `src/main/java/com/basebackend/gateway/filter/SecurityHeadersFilter.java`
  - NEW: `src/main/java/com/basebackend/gateway/config/SecurityHeadersProperties.java`
- **依赖**: 无
- **实施步骤**:
  1. 创建 `SecurityHeadersProperties`，前缀 `gateway.security.headers`，字段:
     - `enabled` (boolean, default true)
     - `hstsMaxAge` (long, default 31536000)
     - `hstsIncludeSubdomains` (boolean, default true)
     - `contentSecurityPolicy` (String, default "default-src 'self'")
     - `frameOptions` (String, default "DENY")
     - `contentTypeOptions` (boolean, default true)
     - `referrerPolicy` (String, default "strict-origin-when-cross-origin")
     - `permissionsPolicy` (String, default "")
  2. 创建 `SecurityHeadersFilter` (order -190):
     - 在响应中注入: `Strict-Transport-Security`, `Content-Security-Policy`, `X-Frame-Options`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy`
     - 所有值从 Properties 读取，支持热更新
- **验收标准**: 任意 API 请求的响应头中包含全部安全头

### Task 3: Sensitive Header Sanitization Filter
- **类型**: 后端
- **文件范围**:
  - NEW: `src/main/java/com/basebackend/gateway/filter/HeaderSanitizationFilter.java`
- **依赖**: 无
- **实施步骤**:
  1. 创建 `HeaderSanitizationFilter` (order -200):
     - 在 AuthenticationFilter 之前执行
     - 从入站请求中剥离以下头（防伪造）:
       - `X-User-Id`
       - `X-Tenant-Id`
       - `X-User-Roles`
       - `X-Real-IP`（非可信代理时）
     - 清洗头列表通过常量数组定义，可后续扩展为配置
     - 白名单路径（如内部服务间调用）跳过清洗
  2. 确保 `AuthenticationFilter` 后续重新注入 `X-User-Id` 不受影响
- **验收标准**: 外部请求携带 `X-User-Id: 999` 到达下游服务时该头已被替换为认证后的真实值

### Task 4: Request Body Size Limit Filter
- **类型**: 后端
- **文件范围**:
  - NEW: `src/main/java/com/basebackend/gateway/filter/RequestSizeLimitFilter.java`
  - NEW: `src/main/java/com/basebackend/gateway/config/RequestSizeLimitProperties.java`
- **依赖**: 无
- **实施步骤**:
  1. 创建 `RequestSizeLimitProperties`，前缀 `gateway.request-size-limit`:
     - `enabled` (boolean, default true)
     - `maxBodySize` (DataSize, default "10MB")
     - `excludePaths` (List<String>, default ["/api/files/**"])
  2. 创建 `RequestSizeLimitFilter` (order -180):
     - 检查 `Content-Length` 头，超过阈值直接拒绝 (413 Payload Too Large)
     - 对无 `Content-Length` 的请求（chunked），通过装饰 DataBuffer 流式计数
     - 文件上传路径排除（通过 excludePaths 配置）
- **验收标准**: 发送超过 10MB 的 POST 请求返回 413；文件上传路径不受限制

### Task 5: OTel Trace Integration (替换 TraceIdFilter)
- **类型**: 后端
- **文件范围**:
  - MODIFY: `src/main/java/com/basebackend/gateway/filter/TraceIdFilter.java` (重写)
  - NEW: `src/main/java/com/basebackend/gateway/config/OTelGatewayConfig.java`
- **依赖**: Task 1 (pom.xml OTel 依赖)
- **实施步骤**:
  1. 重写 `TraceIdFilter`:
     - 从 OTel `Context.current()` 获取当前 Span
     - 如果 OTel agent 已注入 traceparent，直接使用；否则回退到手动创建 Span
     - 将 `traceId` 和 `spanId` 注入下游请求头（W3C Trace Context 格式: `traceparent`, `tracestate`）
     - 同时保留 `X-Trace-Id` / `X-Request-Id` 头用于向后兼容
     - 响应头注入 `X-Trace-Id`
  2. 创建 `OTelGatewayConfig`:
     - 配置 `TextMapPropagator`（W3C + B3 双格式支持）
     - 条件化加载：`@ConditionalOnClass(Span.class)`
  3. 清理 `TraceIdFilter` 中自定义的 UUID 生成逻辑
- **验收标准**: 响应头 `X-Trace-Id` 格式为 32 位 hex（OTel trace ID）；下游服务收到 `traceparent` 头

### Task 6: Structured Access Log Filter
- **类型**: 后端
- **文件范围**:
  - NEW: `src/main/java/com/basebackend/gateway/filter/AccessLogFilter.java`
  - NEW: `src/main/java/com/basebackend/gateway/config/AccessLogProperties.java`
  - MODIFY: `src/main/java/com/basebackend/gateway/filter/SlowRequestFilter.java` (移除重复计时日志)
- **依赖**: Task 5 (需要 OTel trace context)
- **实施步骤**:
  1. 创建 `AccessLogProperties`，前缀 `gateway.access-log`:
     - `enabled` (boolean, default true)
     - `excludePaths` (List<String>, default ["/actuator/**"])
     - `logHeaders` (boolean, default false)
     - `logBody` (boolean, default false)
  2. 创建 `AccessLogFilter` (order -20):
     - 在请求完成后输出 JSON 结构化日志:
       ```json
       {
         "type": "ACCESS",
         "traceId": "...",
         "method": "GET",
         "path": "/api/user/info",
         "status": 200,
         "latencyMs": 42,
         "userId": "123",
         "clientIp": "1.2.3.4",
         "userAgent": "...",
         "routeId": "user-api",
         "requestSize": 0,
         "responseSize": 1234,
         "timestamp": "2026-02-20T07:00:00Z"
         }
       ```
     - 使用 `exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR)` 获取 routeId
  3. 修改 `SlowRequestFilter`:
     - 移除 `doFinally` 中的 `checkSlowRequest` 日志（由 AccessLogFilter 接管）
     - 保留 Micrometer 指标记录功能
- **验收标准**: 每个请求在日志中产生一行 JSON 格式的访问记录

### Task 7: Observability Integration
- **类型**: 后端
- **文件范围**:
  - NEW: `src/main/java/com/basebackend/gateway/config/ObservabilityGatewayConfig.java`
  - MODIFY: `src/main/resources/application.yml` (添加 observability 配置段)
- **依赖**: Task 1 (pom.xml observability 依赖)
- **实施步骤**:
  1. 创建 `ObservabilityGatewayConfig`:
     - 条件化加载 observability 模块的 WebFlux 兼容组件
     - 排除 Servlet-only 的自动配置（参考 GatewayApplication 的 exclude 列表）
     - 配置 Micrometer 与 OTel 桥接
  2. 在 `application.yml` 添加:
     ```yaml
     management:
       tracing:
         sampling:
           probability: 1.0
       otlp:
         tracing:
           endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
     ```
  3. 验证 `basebackend-observability` 模块中无 Servlet 依赖泄漏
- **验收标准**: Gateway 启动无 WebFlux/Servlet 冲突；Actuator `/actuator/metrics` 可用

### Task 8: API Version Routing
- **类型**: 后端
- **文件范围**:
  - NEW: `src/main/java/com/basebackend/gateway/filter/ApiVersionFilter.java`
  - NEW: `src/main/java/com/basebackend/gateway/config/ApiVersionProperties.java`
- **依赖**: 无
- **实施步骤**:
  1. 创建 `ApiVersionProperties`，前缀 `gateway.api-version`:
     - `enabled` (boolean, default false)
     - `headerName` (String, default "Api-Version")
     - `defaultVersion` (String, default "v1")
     - `versionMappings` (Map<String, Map<String, String>>): 服务名→版本→目标元数据
  2. 创建 `ApiVersionFilter` (order -40):
     - 支持两种版本识别方式:
       a. Header: `Api-Version: v2` → 选择对应版本实例
       b. URL 前缀: `/v2/api/user/**` → strip 版本前缀后路由
     - 将版本信息写入 Exchange attributes，供 GrayLoadBalancer 消费
     - 与现有灰度路由协作（版本路由优先级高于灰度）
- **验收标准**: `Api-Version: v2` 头或 `/v2/api/user/info` 路径均能路由到元数据 version=v2 的实例

### Task 9: Response Cache Filter
- **类型**: 后端
- **文件范围**:
  - NEW: `src/main/java/com/basebackend/gateway/filter/ResponseCacheFilter.java`
  - NEW: `src/main/java/com/basebackend/gateway/config/ResponseCacheProperties.java`
- **依赖**: 无
- **实施步骤**:
  1. 创建 `ResponseCacheProperties`，前缀 `gateway.response-cache`:
     - `enabled` (boolean, default false)
     - `defaultTtl` (Duration, default 60s)
     - `maxCacheSize` (long, default 10000)
     - `cachePaths` (List<String>, default []): 需要缓存的 GET 路径模式
     - `excludePaths` (List<String>, default [])
  2. 创建 `ResponseCacheFilter` (order -30):
     - 仅缓存 GET 请求
     - 使用 Caffeine 本地缓存，key = path + queryString + userId (可选)
     - 尊重 `Cache-Control: no-cache` / `no-store` 头
     - 缓存命中时直接返回，注入 `X-Cache: HIT` 响应头
     - 缓存未命中时装饰响应 body，写入缓存后返回，注入 `X-Cache: MISS`
     - 支持通过 Admin API 手动清除缓存
- **验收标准**: 同一 GET 请求第二次调用返回 `X-Cache: HIT`，延迟显著降低

### Task 10: CORS Tightening
- **类型**: 后端配置
- **文件范围**:
  - MODIFY: `src/main/resources/application-gateway.yml` (CORS 段)
- **依赖**: 无
- **实施步骤**:
  1. 将 CORS 配置从通配符改为环境变量注入:
     ```yaml
     globalcors:
       cors-configurations:
         '[/**]':
           allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
           allowed-methods: [GET, POST, PUT, DELETE, OPTIONS]
           allowed-headers: [Authorization, Content-Type, X-Idempotency-Key, Api-Version, X-Trace-Id]
           exposed-headers: [X-Trace-Id, X-Cache, X-Request-Id]
           allow-credentials: true
           max-age: 3600
     ```
  2. 生产环境通过 `CORS_ALLOWED_ORIGINS` 环境变量或 Nacos 配置注入实际域名
- **验收标准**: `curl -H "Origin: http://evil.com"` 不返回 `Access-Control-Allow-Origin`

### Task 11: Sentinel Re-enable & Fix
- **类型**: 后端
- **文件范围**:
  - MODIFY: `src/main/resources/application-gateway.yml` (sentinel.enabled → true)
  - MODIFY: `src/main/java/com/basebackend/gateway/ratelimit/RateLimitRuleManager.java`
  - MODIFY: `src/main/java/com/basebackend/gateway/config/SentinelGatewayRuleConfig.java`
- **依赖**: 无
- **实施步骤**:
  1. `application-gateway.yml`: 将 `spring.cloud.sentinel.enabled` 改为 `${SENTINEL_ENABLED:true}`
  2. 修正 `RateLimitRuleManager.initApiDefinitions()`:
     - 移除过期的 `admin-api` / `basebackend-demo-api` 路径引用
     - 添加当前路由对应的 API 定义:
       - `user_api`: `/api/user/**`
       - `system_api`: `/api/system/**`
       - `auth_api`: `/api/auth/**` (替换旧路径)
       - `notification_api`: `/api/notifications/**`
       - `observability_api`: `/api/metrics/**`, `/api/traces/**`, `/api/logs/**`, `/api/alerts/**`
       - `file_api`: `/api/files/**` (保留)
  3. 修正 `RateLimitRuleManager.initGatewayRules()`:
     - IP 限流 route ID: `admin-api` → `user-api`
     - 用户限流 route ID: `admin-api` → 全局或按 route 分别配置
     - 为新服务路由添加对应限流规则
  4. 修正 `SentinelGatewayRuleConfig`:
     - 更新 `auth_api` 路径模式为 `/api/auth/**`
     - 添加更多 API 组定义
- **验收标准**: Gateway 启动时 Sentinel 正常初始化；高频请求触发 429 限流响应

## 文件冲突检查

| 文件 | 涉及任务 | 冲突处理 |
|------|---------|---------|
| `pom.xml` | Task 1 only | ✅ 无冲突 |
| `application.yml` | Task 7 only | ✅ 无冲突 |
| `application-gateway.yml` | Task 10, Task 11 | ⚠️ 不同 YAML 段，通过 Layer 隔离 |
| `TraceIdFilter.java` | Task 5 only | ✅ 无冲突 |
| `SlowRequestFilter.java` | Task 6 only | ✅ 无冲突 |
| `RateLimitRuleManager.java` | Task 11 only | ✅ 无冲突 |
| `SentinelGatewayRuleConfig.java` | Task 11 only | ✅ 无冲突 |

**结论**: ✅ 无不可解决的文件冲突，通过 Layer 依赖关系保证顺序正确性。

## 并行分组

```
Layer 1 (并行, 6 Builder): Task 1, Task 2, Task 3, Task 4, Task 8, Task 9
   │
   ├── Task 1: pom.xml (基础依赖)
   ├── Task 2: SecurityHeadersFilter (新文件)
   ├── Task 3: HeaderSanitizationFilter (新文件)
   ├── Task 4: RequestSizeLimitFilter (新文件)
   ├── Task 8: ApiVersionFilter (新文件)
   └── Task 9: ResponseCacheFilter (新文件)

Layer 2 (并行, 依赖 Task 1): Task 5, Task 7, Task 10, Task 11
   │
   ├── Task 5: OTel 替换 TraceIdFilter (依赖 OTel 依赖)
   ├── Task 7: Observability 集成 (依赖 observability 依赖)
   ├── Task 10: CORS 收紧 (独立 YAML 段)
   └── Task 11: Sentinel 修正 (独立 Java + YAML 段)

Layer 3 (串行, 依赖 Task 5): Task 6
   │
   └── Task 6: AccessLogFilter + SlowRequestFilter 瘦身 (需要 OTel traceId)
```

## 预计 Builder 数量
- Layer 1: 最多 6 个并行 Builder (推荐 3-4 个，合并小任务)
- Layer 2: 最多 4 个并行 Builder
- Layer 3: 1 个 Builder

## 新增文件清单 (12 个新文件)
```
src/main/java/com/basebackend/gateway/
├── config/
│   ├── SecurityHeadersProperties.java      (Task 2)
│   ├── RequestSizeLimitProperties.java     (Task 4)
│   ├── OTelGatewayConfig.java              (Task 5)
│   ├── AccessLogProperties.java            (Task 6)
│   ├── ObservabilityGatewayConfig.java     (Task 7)
│   ├── ApiVersionProperties.java           (Task 8)
│   └── ResponseCacheProperties.java        (Task 9)
├── filter/
│   ├── SecurityHeadersFilter.java          (Task 2)
│   ├── HeaderSanitizationFilter.java       (Task 3)
│   ├── RequestSizeLimitFilter.java         (Task 4)
│   ├── ApiVersionFilter.java               (Task 8)
│   ├── ResponseCacheFilter.java            (Task 9)
│   └── AccessLogFilter.java               (Task 6)
```

## 修改文件清单 (7 个已有文件)
```
pom.xml                                     (Task 1)
filter/TraceIdFilter.java                   (Task 5 - 重写)
filter/SlowRequestFilter.java              (Task 6 - 瘦身)
config/SentinelGatewayRuleConfig.java       (Task 11)
ratelimit/RateLimitRuleManager.java         (Task 11)
resources/application.yml                   (Task 7)
resources/application-gateway.yml           (Task 10, 11)
```
