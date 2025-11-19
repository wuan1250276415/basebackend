# API路由使用指南

## 概述

本文档说明如何通过网关访问各个微服务的API接口。

## 网关地址

- 开发环境：`http://localhost:8080`
- 生产环境：根据实际部署配置

## 路由规则

### 标准路由（推荐）

直接使用API路径，不包含服务名：

```
http://localhost:8080/api/{resource}/**
```

### 带服务名前缀的路由（兼容模式）

包含完整服务名的路径：

```
http://localhost:8080/{service-name}/api/{resource}/**
```

网关会自动去除服务名前缀（使用StripPrefix=1）。

## 服务路由映射

### 1. 认证服务 (Auth API)

**服务名：** `basebackend-auth-api`

**标准路由：**
```bash
# 登录
POST http://localhost:8080/api/auth/login

# 登出
POST http://localhost:8080/api/auth/logout

# 刷新Token
POST http://localhost:8080/api/auth/refresh
```

**兼容路由：**
```bash
POST http://localhost:8080/basebackend-auth-api/api/auth/login
```

### 2. 用户服务 (User API)

**服务名：** `basebackend-user-api`

**标准路由：**
```bash
# 用户管理
GET  http://localhost:8080/api/users
POST http://localhost:8080/api/users
PUT  http://localhost:8080/api/users/{id}

# 角色管理
GET  http://localhost:8080/api/roles
POST http://localhost:8080/api/roles

# 应用管理
GET  http://localhost:8080/api/applications
POST http://localhost:8080/api/applications
```

**兼容路由：**
```bash
GET http://localhost:8080/basebackend-user-api/api/users
```

### 3. 系统服务 (System API)

**服务名：** `basebackend-system-api`

**标准路由：**
```bash
# 部门管理
GET  http://localhost:8080/api/depts
POST http://localhost:8080/api/depts

# 字典管理
GET  http://localhost:8080/api/dicts
POST http://localhost:8080/api/dicts

# 菜单管理
GET  http://localhost:8080/api/menus
POST http://localhost:8080/api/menus
```

**兼容路由：**
```bash
GET http://localhost:8080/basebackend-system-api/api/depts
```

### 4. 通知服务 (Notification Service)

**服务名：** `basebackend-notification-service`

**标准路由：**
```bash
# 通知管理
GET  http://localhost:8080/api/notifications
POST http://localhost:8080/api/notifications/send

# SSE实时推送
GET  http://localhost:8080/api/notifications/stream
```

**兼容路由：**
```bash
GET http://localhost:8080/basebackend-notification-service/api/notifications
```

### 5. 可观测性服务 (Observability Service)

**服务名：** `basebackend-observability-service`

**标准路由：**
```bash
# 指标查询
POST http://localhost:8080/api/metrics/query
GET  http://localhost:8080/api/metrics/services

# 日志查询
POST http://localhost:8080/api/logs/search
GET  http://localhost:8080/api/logs/services

# 追踪查询
POST http://localhost:8080/api/traces/search
GET  http://localhost:8080/api/traces/{traceId}

# 告警管理
GET  http://localhost:8080/api/alerts
POST http://localhost:8080/api/alerts/rules
```

**兼容路由：**
```bash
POST http://localhost:8080/basebackend-observability-service/api/logs/search
```

## 完整请求示例

### 示例1：日志搜索（标准路由）

```bash
curl --location --request POST 'http://localhost:8080/api/logs/search' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...' \
  --data-raw '{
    "serviceName": "basebackend-user-api",
    "level": "ERROR",
    "startTime": "2024-11-19T00:00:00",
    "endTime": "2024-11-19T23:59:59"
  }'
```

### 示例2：日志搜索（兼容路由）

```bash
curl --location --request POST 'http://localhost:8080/basebackend-observability-service/api/logs/search' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...' \
  --data-raw '{
    "serviceName": "basebackend-user-api"
  }'
```

### 示例3：用户登录

```bash
curl --location --request POST 'http://localhost:8080/api/auth/login' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 示例4：获取用户列表

```bash
curl --location --request GET 'http://localhost:8080/api/users?page=1&size=10' \
  --header 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...'
```

### 示例5：发送通知

```bash
curl --location --request POST 'http://localhost:8080/api/notifications/send' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...' \
  --data-raw '{
    "userId": 1,
    "title": "系统通知",
    "content": "这是一条测试通知",
    "type": "SYSTEM"
  }'
```

## 认证说明

### 公开接口（无需认证）

以下接口无需JWT Token：

```
/api/auth/login          # 登录
/api/auth/register       # 注册（如果启用）
/api/public/**           # 公开API
/actuator/**             # 健康检查
/doc.html                # API文档
/swagger-ui/**           # Swagger UI
/druid/**                # Druid监控
```

### 受保护接口（需要认证）

其他所有接口都需要在请求头中携带JWT Token：

```
Authorization: Bearer <your-jwt-token>
```

### 获取Token

1. 调用登录接口获取Token：
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

2. 响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresIn": 86400
  }
}
```

3. 使用Token访问受保护接口：
```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

## CSRF保护

网关启用了CSRF保护，对于POST/PUT/DELETE请求，需要：

1. 首次访问时获取CSRF Token（自动通过Cookie设置）
2. 后续请求携带CSRF Token

**方式1：通过Cookie（推荐）**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <token>" \
  -H "Cookie: XSRF-TOKEN=<csrf-token>" \
  -H "X-XSRF-TOKEN: <csrf-token>"
```

**方式2：CSRF豁免接口**

以下接口已豁免CSRF检查：
- `/api/auth/**`
- `/api/public/**`
- `/actuator/**`

## 跨域配置

网关已配置CORS，允许以下来源：
- `http://localhost:3000` (前端开发环境)
- `http://localhost:8080` (网关地址)

如需添加其他来源，请修改网关配置。

## 熔断降级

所有服务路由都配置了熔断器（Circuit Breaker）：

- **熔断阈值：** 50%错误率
- **最小请求数：** 10
- **等待时间：** 60秒
- **降级响应：** 返回友好的错误信息

当服务不可用时，会自动触发降级，返回：
```json
{
  "code": 503,
  "message": "服务暂时不可用，请稍后重试"
}
```

## 负载均衡

网关使用Ribbon进行客户端负载均衡：

- **策略：** 轮询（Round Robin）
- **重试：** 启用
- **超时：** 30秒

## 路由优先级

路由匹配按照配置顺序进行，更具体的路由应该放在前面：

1. 带服务名前缀的路由（`/basebackend-xxx-service/**`）
2. 特殊路由（如SSE）
3. 标准API路由（`/api/**`）

## 故障排查

### 问题1：404 Not Found

**原因：** 路由配置不匹配

**解决：**
1. 检查URL是否正确
2. 确认使用标准路由格式：`/api/{resource}/**`
3. 或使用兼容格式：`/{service-name}/api/{resource}/**`

### 问题2：401 Unauthorized

**原因：** 未提供或Token无效

**解决：**
1. 确认请求头包含：`Authorization: Bearer <token>`
2. 检查Token是否过期
3. 重新登录获取新Token

### 问题3：403 Forbidden

**原因：** CSRF验证失败或权限不足

**解决：**
1. 检查CSRF Token是否正确
2. 确认用户有访问该接口的权限
3. 查看服务日志获取详细错误信息

### 问题4：503 Service Unavailable

**原因：** 服务不可用或熔断器打开

**解决：**
1. 检查目标服务是否运行
2. 查看服务健康状态：`http://localhost:8080/actuator/health`
3. 等待熔断器恢复（默认60秒）

## API文档

访问Swagger UI查看完整的API文档：

```
http://localhost:8080/doc.html
```

## 监控和日志

### 查看网关日志

```bash
# Docker环境
docker logs -f basebackend-gateway

# 本地运行
tail -f logs/gateway/application.log
```

### 查看路由信息

```bash
# 获取所有路由
curl http://localhost:8080/actuator/gateway/routes

# 获取特定路由
curl http://localhost:8080/actuator/gateway/routes/{route-id}
```

### 查看熔断器状态

```bash
curl http://localhost:8080/actuator/health
```

## 最佳实践

1. **使用标准路由**：推荐使用`/api/**`格式，更简洁清晰
2. **Token管理**：实现Token自动刷新机制
3. **错误处理**：统一处理网关返回的错误响应
4. **超时设置**：根据业务需求设置合理的超时时间
5. **重试策略**：对幂等接口启用重试
6. **日志记录**：记录所有API调用日志，便于排查问题

## 更新日志

- 2024-11-19: 添加带服务名前缀的兼容路由
- 2024-11-19: 完善路由配置和文档
