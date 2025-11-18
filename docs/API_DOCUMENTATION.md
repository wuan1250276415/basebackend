# BaseBackend API 文档

> **版本**: v1.0  
> **最后更新**: 2025-11-18  
> **网关地址**: http://localhost:8080

---

## 📖 文档说明

所有API请求都应通过API网关（8080端口）访问。网关会自动进行服务发现、负载均衡和熔断降级。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1700000000000
}
```

### 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用（熔断降级） |

---

## 1. 用户服务 API (User API)

**基础路径**: `/api/user`  
**服务端口**: 8081  
**API文档**: http://localhost:8081/doc.html

### 1.1 认证接口

#### 用户登录
```http
POST /api/user/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员"
    }
  }
}
```

#### 用户登出
```http
POST /api/user/auth/logout
Authorization: Bearer {token}
```

### 1.2 用户管理

#### 查询用户列表
```http
GET /api/user/users?current=1&size=10
Authorization: Bearer {token}
```

#### 创建用户
```http
POST /api/user/users
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "deptId": 1,
  "roleIds": [2, 3]
}
```

#### 更新用户
```http
PUT /api/user/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "nickname": "张三三",
  "email": "zhangsan@example.com"
}
```

#### 删除用户
```http
DELETE /api/user/users/{id}
Authorization: Bearer {token}
```

### 1.3 角色管理

#### 查询角色列表
```http
GET /api/user/roles
Authorization: Bearer {token}
```

#### 创建角色
```http
POST /api/user/roles
Authorization: Bearer {token}
Content-Type: application/json

{
  "roleName": "测试角色",
  "roleCode": "test",
  "description": "测试角色描述",
  "permissionIds": [1, 2, 3]
}
```

---

## 2. 系统服务 API (System API)

**基础路径**: `/api/system`  
**服务端口**: 8082  
**API文档**: http://localhost:8082/doc.html

### 2.1 部门管理

#### 获取部门树
```http
GET /api/system/depts/tree
Authorization: Bearer {token}
```

#### 创建部门
```http
POST /api/system/depts
Authorization: Bearer {token}
Content-Type: application/json

{
  "deptName": "技术部",
  "parentId": 0,
  "orderNum": 1,
  "leader": "张三",
  "phone": "13800138000"
}
```

### 2.2 菜单管理

#### 获取菜单树
```http
GET /api/system/menus/tree
Authorization: Bearer {token}
```

### 2.3 字典管理

#### 查询字典列表
```http
GET /api/system/dicts?dictType=sys_user_status
Authorization: Bearer {token}
```

---

## 3. 认证服务 API (Auth API)

**基础路径**: `/api/auth`  
**服务端口**: 8083  
**API文档**: http://localhost:8083/doc.html

### 3.1 认证接口

#### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

#### 刷新Token
```http
POST /api/auth/refresh
Authorization: Bearer {refresh_token}
```

#### 获取当前用户信息
```http
GET /api/auth/info
Authorization: Bearer {token}
```

---

## 4. 通知服务 API (Notification Service)

**基础路径**: `/api/notifications`  
**服务端口**: 8086  
**API文档**: http://localhost:8086/doc.html

### 4.1 通知管理

#### 获取通知列表
```http
GET /api/notifications?limit=50
Authorization: Bearer {token}
```

#### 获取未读数量
```http
GET /api/notifications/unread-count
Authorization: Bearer {token}
```

#### 标记已读
```http
PUT /api/notifications/{id}/read
Authorization: Bearer {token}
```

#### 创建通知
```http
POST /api/notifications
Authorization: Bearer {token}
Content-Type: application/json

{
  "userId": 1,
  "title": "系统通知",
  "content": "这是一条测试通知",
  "type": "system",
  "level": "info"
}
```

### 4.2 SSE实时推送

#### 建立SSE连接
```http
GET /api/notifications/stream?token={token}
```

**响应** (Server-Sent Events):
```
event: connected
data: {"message": "连接成功", "timestamp": 1700000000000}

event: notification
data: {"id": 1, "title": "新通知", "content": "..."}

event: heartbeat
data: {"timestamp": 1700000000000}
```

---

## 5. 可观测性服务 API (Observability Service)

**基础路径**: `/api/metrics`, `/api/traces`, `/api/logs`, `/api/alerts`  
**服务端口**: 8087  
**API文档**: http://localhost:8087/doc.html

### 5.1 指标查询

#### 查询指标数据
```http
POST /api/metrics/query
Authorization: Bearer {token}
Content-Type: application/json

{
  "metricName": "jvm.memory.used",
  "startTime": 1700000000000,
  "endTime": 1700003600000,
  "aggregation": "avg"
}
```

#### 获取系统概览
```http
GET /api/metrics/overview
Authorization: Bearer {token}
```

### 5.2 追踪查询

#### 查询追踪详情
```http
GET /api/traces/{traceId}
Authorization: Bearer {token}
```

#### 搜索追踪
```http
POST /api/traces/search
Authorization: Bearer {token}
Content-Type: application/json

{
  "serviceName": "basebackend-user-api",
  "startTime": 1700000000000,
  "endTime": 1700003600000,
  "limit": 100
}
```

### 5.3 日志查询

#### 搜索日志
```http
POST /api/logs/search
Authorization: Bearer {token}
Content-Type: application/json

{
  "serviceName": "basebackend-user-api",
  "level": "ERROR",
  "keyword": "exception",
  "startTime": 1700000000000,
  "endTime": 1700003600000,
  "limit": 100
}
```

#### 实时日志流
```http
GET /api/logs/tail?serviceName=basebackend-user-api&lines=100
Authorization: Bearer {token}
```

### 5.4 告警管理

#### 注册告警规则
```http
POST /api/alerts/rules
Authorization: Bearer {token}
Content-Type: application/json

{
  "ruleName": "High CPU Usage",
  "metricName": "system.cpu.usage",
  "threshold": 0.8,
  "operator": "gt",
  "duration": 300,
  "severity": "warning",
  "enabled": true
}
```

#### 获取告警规则
```http
GET /api/alerts/rules
Authorization: Bearer {token}
```

#### 获取告警事件
```http
GET /api/alerts/events
Authorization: Bearer {token}
```

---

## 6. 文件服务 API (File Service)

**基础路径**: `/api/files`  
**服务端口**: 8084  
**API文档**: http://localhost:8084/doc.html

### 6.1 文件操作

#### 上传文件
```http
POST /api/files/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: (binary)
```

#### 下载文件
```http
GET /api/files/download/{fileId}
Authorization: Bearer {token}
```

#### 删除文件
```http
DELETE /api/files/{fileId}
Authorization: Bearer {token}
```

---

## 7. 认证说明

### 7.1 获取Token

通过登录接口获取Token：
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 7.2 使用Token

在请求头中添加Token：
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 7.3 Token刷新

Token过期前可以刷新：
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer {refresh_token}"
```

---

## 8. Postman集合

### 导入Postman集合

1. 下载Postman集合文件: [BaseBackend.postman_collection.json](./postman/BaseBackend.postman_collection.json)
2. 在Postman中点击 Import
3. 选择下载的JSON文件
4. 配置环境变量：
   - `base_url`: http://localhost:8080
   - `token`: (登录后获取)

### 环境变量

```json
{
  "base_url": "http://localhost:8080",
  "token": "",
  "user_id": "",
  "trace_id": ""
}
```

---

## 9. 在线API文档

### Knife4j文档

每个服务都提供了Knife4j在线文档：

- **User API**: http://localhost:8081/doc.html
- **System API**: http://localhost:8082/doc.html
- **Auth API**: http://localhost:8083/doc.html
- **File Service**: http://localhost:8084/doc.html
- **Notification Service**: http://localhost:8086/doc.html
- **Observability Service**: http://localhost:8087/doc.html

### Swagger UI

也可以通过Swagger UI访问：

- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html
- ...

---

## 10. 测试示例

### 完整流程示例

```bash
# 1. 用户登录
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | jq -r '.data.token')

# 2. 查询用户列表
curl -X GET "http://localhost:8080/api/user/users?current=1&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 3. 创建用户
curl -X POST http://localhost:8080/api/user/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "nickname": "测试用户",
    "email": "test@example.com"
  }'

# 4. 查询系统概览
curl -X GET http://localhost:8080/api/metrics/overview \
  -H "Authorization: Bearer $TOKEN"

# 5. 获取通知列表
curl -X GET http://localhost:8080/api/notifications \
  -H "Authorization: Bearer $TOKEN"
```

---

## 11. 错误处理

### 常见错误

#### 401 未认证
```json
{
  "code": 401,
  "message": "未登录或登录已过期",
  "data": null
}
```

**解决方案**: 重新登录获取Token

#### 403 无权限
```json
{
  "code": 403,
  "message": "无权限访问",
  "data": null
}
```

**解决方案**: 检查用户角色和权限配置

#### 503 服务不可用
```json
{
  "code": 503,
  "message": "服务暂时不可用，请稍后重试",
  "data": null
}
```

**解决方案**: 服务熔断降级，等待服务恢复

---

## 12. 相关文档

- [网关路由配置](../basebackend-gateway/GATEWAY_ROUTES.md)
- [服务功能检查](./SERVICE_FUNCTIONALITY_CHECK.md)
- [性能优化建议](./PERFORMANCE_OPTIMIZATION.md)
- [部署指南](./DEPLOYMENT_GUIDE.md)
- [运维手册](./OPERATIONS_GUIDE.md)

---

**文档维护**: 架构团队  
**最后更新**: 2025-11-18
