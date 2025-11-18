# BaseBackend Gateway 路由配置

## 路由规则

### 用户服务 (User API)
- **路径**: `/api/user/**`
- **目标服务**: `basebackend-user-api`
- **端口**: 8081
- **功能**: 用户管理、角色管理、权限管理

**示例**:
```bash
# 用户登录
POST http://localhost:8080/api/user/auth/login

# 获取用户列表
GET http://localhost:8080/api/user/users
```

### 系统服务 (System API)
- **路径**: `/api/system/**`
- **目标服务**: `basebackend-system-api`
- **端口**: 8082
- **功能**: 部门管理、菜单管理、字典管理

**示例**:
```bash
# 获取部门树
GET http://localhost:8080/api/system/depts/tree

# 获取字典列表
GET http://localhost:8080/api/system/dicts
```

### 认证服务 (Auth API)
- **路径**: `/api/auth/**`
- **目标服务**: `basebackend-auth-api`
- **端口**: 8083
- **功能**: 认证、授权、Token管理

**示例**:
```bash
# 用户登录
POST http://localhost:8080/api/auth/login

# 刷新Token
POST http://localhost:8080/api/auth/refresh
```

### 通知服务 (Notification Service)
- **路径**: `/api/notifications/**`
- **目标服务**: `basebackend-notification-service`
- **端口**: 8086
- **功能**: 通知管理、邮件发送、SSE推送

**示例**:
```bash
# 获取通知列表
GET http://localhost:8080/api/notifications

# 创建通知
POST http://localhost:8080/api/notifications

# SSE连接
GET http://localhost:8080/api/notifications/stream?token=xxx
```

### 可观测性服务 (Observability Service)
- **路径**: `/api/metrics/**`, `/api/traces/**`, `/api/logs/**`, `/api/alerts/**`
- **目标服务**: `basebackend-observability-service`
- **端口**: 8087
- **功能**: 指标查询、追踪查询、日志查询、告警管理

**示例**:
```bash
# 查询指标
POST http://localhost:8080/api/metrics/query

# 查询追踪
GET http://localhost:8080/api/traces/{traceId}

# 搜索日志
POST http://localhost:8080/api/logs/search

# 获取告警规则
GET http://localhost:8080/api/alerts/rules
```

### 文件服务 (File Service)
- **路径**: `/api/files/**`
- **目标服务**: `basebackend-file-service`
- **端口**: 8084
- **功能**: 文件上传、下载、管理

**示例**:
```bash
# 上传文件
POST http://localhost:8080/api/files/upload

# 下载文件
GET http://localhost:8080/api/files/download/{fileId}
```

### Admin API (兼容保留)
- **路径**: `/api/admin/**`
- **目标服务**: `basebackend-admin-api`
- **端口**: 8085
- **功能**: 管理功能（逐步迁移到其他服务）

## 路由特性

### 1. 负载均衡
所有路由都使用 `lb://` 协议，通过Nacos实现服务发现和负载均衡。

### 2. 熔断降级
每个服务都配置了熔断器（Circuit Breaker），当服务不可用时自动降级。

**熔断配置**:
- 滑动窗口大小: 10
- 最小调用次数: 5
- 失败率阈值: 50%
- 半开状态等待时间: 5秒
- 超时时间: 10秒

### 3. 降级响应
当服务熔断时，返回统一的降级响应：

```json
{
  "code": 503,
  "message": "服务暂时不可用，请稍后重试",
  "data": null,
  "timestamp": 1700000000000
}
```

### 4. CORS支持
网关配置了全局CORS，支持跨域请求。

### 5. 灰度发布
支持基于权重、Header、用户、IP的灰度路由策略。

## 服务端口映射

| 服务名称 | 内部端口 | 网关路径 | 说明 |
|---------|---------|---------|------|
| Gateway | 8080 | - | API网关 |
| User API | 8081 | /api/user/** | 用户服务 |
| System API | 8082 | /api/system/** | 系统服务 |
| Auth API | 8083 | /api/auth/** | 认证服务 |
| File Service | 8084 | /api/files/** | 文件服务 |
| Admin API | 8085 | /api/admin/** | 管理服务 |
| Notification Service | 8086 | /api/notifications/** | 通知服务 |
| Observability Service | 8087 | /api/metrics/**, /api/traces/**, /api/logs/**, /api/alerts/** | 可观测性服务 |

## 配置文件

### application-routes.yml
包含所有路由规则和熔断器配置。

### application-gateway.yml
包含网关基础配置、CORS配置、灰度路由配置。

## 使用建议

### 1. 统一入口
所有外部请求都应该通过网关（8080端口）访问，不要直接访问后端服务。

### 2. 路径规范
- 使用 `/api/{service}/**` 的路径格式
- service名称使用小写，用连字符分隔

### 3. 超时配置
- 默认超时时间: 10秒
- 对于长时间操作（如文件上传），可以在服务端配置更长的超时时间

### 4. 监控
- 通过 `/actuator/gateway/routes` 查看所有路由
- 通过 `/actuator/health` 查看网关健康状态
- 通过 `/actuator/metrics` 查看网关指标

## 故障排查

### 1. 服务不可达
```bash
# 检查服务是否注册到Nacos
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api

# 检查网关路由
curl http://localhost:8080/actuator/gateway/routes
```

### 2. 熔断器触发
```bash
# 查看熔断器状态
curl http://localhost:8080/actuator/health

# 查看熔断器指标
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

### 3. 路由不匹配
检查请求路径是否符合路由规则，注意大小写和斜杠。

## 扩展

### 添加新服务路由

1. 在 `application-routes.yml` 中添加路由规则
2. 在 `FallbackController` 中添加降级处理
3. 配置熔断器实例
4. 更新本文档

### 示例

```yaml
- id: new-service
  uri: lb://basebackend-new-service
  predicates:
    - Path=/api/new/**
  filters:
    - StripPrefix=1
    - name: CircuitBreaker
      args:
        name: newServiceCircuitBreaker
        fallbackUri: forward:/fallback/new
```

## 相关文档

- [Spring Cloud Gateway文档](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Resilience4j文档](https://resilience4j.readme.io/)
- [Nacos文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
