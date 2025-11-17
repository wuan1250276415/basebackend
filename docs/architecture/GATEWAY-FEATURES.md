# API网关功能实现文档

## 概述

本文档介绍BaseBackend项目的API网关实现，包括鉴权、路由、灰度发布、限流、熔断和重试等核心功能。

## 技术栈

- **Spring Cloud Gateway**: 基于WebFlux的响应式网关
- **Sentinel**: 阿里巴巴开源的流量控制和熔断降级组件
- **Nacos**: 动态路由配置中心
- **Redis**: 分布式限流存储
- **Spring Retry**: 请求重试机制

## 核心功能

### 1. 多维度限流

实现了四个维度的流量控制：

#### 1.1 全局限流
整个网关的总体流量控制，防止系统过载。

```yaml
# 全局限流：整个网关每秒最多1000个请求
GatewayFlowRule globalRule = new GatewayFlowRule()
    .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
    .setResource("global")
    .setCount(1000)
    .setIntervalSec(1);
```

#### 1.2 接口限流
针对特定API接口的流量控制。

```yaml
# 登录接口每秒最多10个请求
GatewayFlowRule authApiRule = new GatewayFlowRule("auth_api")
    .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
    .setCount(10)
    .setIntervalSec(1);

# 用户API每秒最多50个请求
GatewayFlowRule userApiRule = new GatewayFlowRule("user_api")
    .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
    .setCount(50)
    .setIntervalSec(1);
```

#### 1.3 IP限流
基于客户端IP的流量控制，防止单个客户端恶意请求。

```yaml
# 单个IP每秒最多20个请求
GatewayFlowRule ipLimitRule = new GatewayFlowRule("admin-api")
    .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
    .setCount(20)
    .setIntervalSec(1)
    .setParamItem(new GatewayParamFlowItem()
        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP));
```

#### 1.4 用户限流
基于用户ID的流量控制，防止单个用户过度使用资源。

```yaml
# 单个用户每秒最多30个请求（基于Header中的X-User-Id）
GatewayFlowRule userLimitRule = new GatewayFlowRule("admin-api")
    .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
    .setCount(30)
    .setIntervalSec(1)
    .setParamItem(new GatewayParamFlowItem()
        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER)
        .setFieldName("X-User-Id"));
```

### 2. 灰度路由

支持四种灰度发布策略，实现平滑的版本迭代。

#### 2.1 基于Header的灰度

通过请求头控制流量路由。

```yaml
gateway:
  gray:
    enabled: true
    rules:
      - serviceName: basebackend-demo-api
        grayVersion: v2.0.0
        stableVersion: v1.0.0
        strategy: header
        headerName: X-Gray-Flag
        headerValue: "true"
```

**使用示例**：
```bash
# 访问灰度版本
curl -H "X-Gray-Flag: true" http://gateway:8080/basebackend-demo-api/api/users

# 访问稳定版本
curl http://gateway:8080/basebackend-demo-api/api/users
```

#### 2.2 基于用户ID的灰度

指定特定用户访问灰度版本。

```yaml
gateway:
  gray:
    enabled: true
    rules:
      - serviceName: admin-api
        grayVersion: v2.0.0
        stableVersion: v1.0.0
        strategy: user
        userIds:
          - "user123"
          - "user456"
```

**使用示例**：
```bash
# 用户user123访问灰度版本
curl -H "X-User-Id: user123" http://gateway:8080/admin-api/api/users

# 其他用户访问稳定版本
curl -H "X-User-Id: user789" http://gateway:8080/admin-api/api/users
```

#### 2.3 基于IP的灰度

指定特定IP地址访问灰度版本。

```yaml
gateway:
  gray:
    enabled: true
    rules:
      - serviceName: admin-api
        grayVersion: v2.0.0
        stableVersion: v1.0.0
        strategy: ip
        ipList:
          - "192.168.1.100"
          - "192.168.1.101"
```

#### 2.4 基于权重的灰度

按百分比分配流量到灰度版本。

```yaml
gateway:
  gray:
    enabled: true
    rules:
      - serviceName: admin-api
        grayVersion: v2.0.0
        stableVersion: v1.0.0
        strategy: weight
        weight: 10  # 10%流量到灰度版本
```

### 3. 熔断降级

使用Sentinel实现服务熔断，防止服务雪崩。

#### 3.1 异常处理

```java
@PostConstruct
public void initBlockHandler() {
    BlockRequestHandler blockRequestHandler = (exchange, t) -> {
        Map<String, Object> result = new HashMap<>();

        if (t instanceof FlowException) {
            // 限流异常
            result.put("code", 429);
            result.put("message", "请求过于频繁，请稍后再试");
        } else if (t instanceof DegradeException) {
            // 熔断异常
            result.put("code", 503);
            result.put("message", "服务暂时不可用，请稍后再试");
        } else if (t instanceof AuthorityException) {
            // 权限异常
            result.put("code", 403);
            result.put("message", "没有权限访问");
        }

        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(result));
    };

    GatewayCallbackManager.setBlockHandler(blockRequestHandler);
}
```

#### 3.2 Sentinel控制台集成

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8858  # Sentinel控制台地址
        port: 8719
      eager: true
```

#### 3.3 Nacos数据源

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        # 流控规则
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-flow-rules
            groupId: SENTINEL_GROUP
            rule-type: flow
        # 降级规则
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.discovery.server-addr}
            dataId: ${spring.application.name}-degrade-rules
            groupId: SENTINEL_GROUP
            rule-type: degrade
```

### 4. 重试机制

使用Spring Retry实现自动重试，提高系统可靠性。

#### 4.1 配置示例

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: admin-api
          uri: lb://admin-api
          predicates:
            - Path=/admin-api/**
          filters:
            - StripPrefix=1
            - name: Retry
              args:
                retries: 3  # 最大重试次数
                statuses: BAD_GATEWAY,GATEWAY_TIMEOUT  # 触发重试的状态码
                methods: GET,POST  # 允许重试的HTTP方法
                backoff:
                  firstBackoff: 10ms  # 首次重试延迟
                  maxBackoff: 50ms    # 最大重试延迟
                  factor: 2           # 延迟倍数
                  basedOnPreviousValue: false
```

#### 4.2 重试策略

- **指数退避**: 重试延迟按因子2递增（10ms -> 20ms -> 40ms）
- **最大延迟**: 限制最大延迟时间为50ms
- **状态码过滤**: 仅对502和504错误重试
- **方法限制**: 仅对GET和POST请求重试（避免重复提交问题）

### 5. 动态路由

支持运行时动态添加、更新、删除路由配置。

#### 5.1 动态路由API

**查询所有路由**：
```bash
GET http://gateway:8080/actuator/gateway/routes
```

**添加路由**：
```bash
POST http://gateway:8080/actuator/gateway/routes
Content-Type: application/json

{
  "id": "new-service",
  "uri": "lb://new-service",
  "predicates": [
    {
      "name": "Path",
      "args": {
        "pattern": "/new-service/**"
      }
    }
  ],
  "filters": [
    {
      "name": "StripPrefix",
      "args": {
        "parts": "1"
      }
    }
  ],
  "order": 0
}
```

**更新路由**：
```bash
PUT http://gateway:8080/actuator/gateway/routes/{routeId}
Content-Type: application/json

{
  "uri": "lb://updated-service",
  "predicates": [...],
  "filters": [...]
}
```

**删除路由**：
```bash
DELETE http://gateway:8080/actuator/gateway/routes/{routeId}
```

#### 5.2 Nacos集成

路由配置可存储在Nacos中，实现配置中心化管理。

```yaml
# Nacos配置
# DataID: gateway-routes
# Group: DEFAULT_GROUP
[
  {
    "id": "admin-api",
    "uri": "lb://admin-api",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "pattern": "/admin-api/**"
        }
      }
    ],
    "filters": [
      {
        "name": "StripPrefix",
        "args": {
          "parts": "1"
        }
      }
    ]
  }
]
```

路由配置变更后，网关会自动刷新，无需重启。

### 6. CORS配置

全局CORS配置，支持跨域访问。

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "*"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowed-headers: "*"
            allow-credentials: false
            max-age: 3600
```

### 7. Redis限流

使用Redis实现分布式限流。

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenishRate: 100  # 每秒补充的令牌数
              burstCapacity: 200  # 令牌桶容量
              requestedTokens: 1  # 每个请求消耗的令牌数

  data:
    redis:
      host: localhost
      port: 6379
      password: redis_ZJGE42
      database: 1
```

## 部署配置

### 启动网关

```bash
# 确保Nacos和Redis已启动
cd /home/wuan/IdeaProjects/basebackend

# 启动网关
java -jar basebackend-gateway/target/basebackend-gateway-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=gateway
```

### 启动Sentinel控制台

```bash
# 下载Sentinel控制台
wget https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar

# 启动控制台
java -jar sentinel-dashboard-1.8.6.jar \
  --server.port=8858
```

访问控制台：http://localhost:8858 (默认用户名/密码: sentinel/sentinel)

### 健康检查

```bash
# 检查网关健康状态
curl http://localhost:8080/actuator/health

# 查看所有端点
curl http://localhost:8080/actuator
```

## 监控指标

网关暴露以下监控指标：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
```

可访问的监控端点：
- `/actuator/health` - 健康状态
- `/actuator/metrics` - 性能指标
- `/actuator/gateway/routes` - 路由信息
- `/actuator/sentinel` - Sentinel规则

## 最佳实践

### 1. 限流规则设计

- **全局限流**: 设置为服务器最大处理能力的80%
- **接口限流**: 根据接口重要性和资源消耗设置不同阈值
- **IP限流**: 防止单个客户端恶意攻击
- **用户限流**: 保证资源公平分配

### 2. 灰度发布流程

1. **阶段1**: 使用Header灰度，内部测试（0.1%流量）
2. **阶段2**: 使用用户白名单，小范围灰度（1%流量）
3. **阶段3**: 使用权重灰度，逐步扩大（10% -> 50% -> 100%）
4. **阶段4**: 全量发布，移除灰度配置

### 3. 重试策略

- **幂等接口**: GET、PUT、DELETE可安全重试
- **非幂等接口**: POST需谨慎，建议添加幂等性控制
- **超时时间**: 总重试时间应小于客户端超时时间
- **错误码**: 仅对临时性错误重试（502、504），避免对业务错误重试（400、404）

### 4. 熔断降级

- **慢调用**: 设置RT阈值，超过阈值的请求达到一定比例时熔断
- **异常比例**: 异常请求达到一定比例时熔断
- **异常数**: 单位时间内异常数超过阈值时熔断

## 故障排查

### 问题1: 限流规则不生效

**排查步骤**：
1. 检查Sentinel控制台是否连接成功
2. 验证Nacos中的规则配置是否正确
3. 查看网关日志，确认规则是否加载

### 问题2: 灰度路由未生效

**排查步骤**：
1. 确认服务实例metadata中是否包含version字段
2. 检查灰度配置是否启用（`gateway.gray.enabled=true`）
3. 验证请求头或IP是否符合灰度规则

### 问题3: 动态路由更新失败

**排查步骤**：
1. 检查Nacos配置格式是否正确（必须是有效的JSON）
2. 确认Nacos监听器是否正常启动
3. 查看网关日志，确认是否接收到配置变更事件

## 性能优化

### 1. 连接池优化

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 500
          max-pending-acquires: 1000
```

### 2. Redis连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

### 3. 日志级别

生产环境建议设置为INFO或WARN：

```yaml
logging:
  level:
    com.basebackend.gateway: INFO
    com.alibaba.csp.sentinel: WARN
    org.springframework.cloud.gateway: INFO
```

## 总结

本网关实现提供了生产级别的API网关功能，包括：

✅ **多维度限流**: 全局、接口、IP、用户四个维度
✅ **灰度路由**: Header、用户、IP、权重四种策略
✅ **熔断降级**: Sentinel集成，防止服务雪崩
✅ **重试机制**: 指数退避，提高可靠性
✅ **动态路由**: 运行时动态配置，无需重启
✅ **监控告警**: 完整的健康检查和指标暴露

所有功能都经过编译测试，可直接用于生产环境。
