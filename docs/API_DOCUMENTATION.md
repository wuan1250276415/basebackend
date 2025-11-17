# BaseBackend 微服务 API 文档

## 📋 概述

本文档提供了 BaseBackend 微服务架构中所有服务的 API 接口说明，包括请求参数、响应格式、示例代码等。

---

## 🏗️ 服务架构

### 服务列表

| 服务名称 | 端口 | 服务名 | 状态 |
|----------|------|--------|------|
| 用户服务 | 8081 | basebackend-user-service | ✅ 运行中 |
| 权限服务 | 8082 | basebackend-auth-service | ✅ 运行中 |
| 字典服务 | 8083 | basebackend-dict-service | ✅ 运行中 |
| 部门服务 | 8084 | basebackend-dept-service | ✅ 运行中 |
| 日志服务 | 8085 | basebackend-log-service | ✅ 运行中 |
| 应用服务 | 8086 | basebackend-application-service | ✅ 运行中 |
| 菜单服务 | 8088 | basebackend-menu-service | ✅ 运行中 |
| 监控服务 | 8089 | basebackend-monitor-service | ✅ 运行中 |
| 通知服务 | 8090 | basebackend-notification-service | ✅ 运行中 |
| 个人配置服务 | 8091 | basebackend-profile-service | ✅ 运行中 |

---

## 🔐 认证方式

所有 API 请求需要在 HTTP Header 中添加认证信息：

```http
Authorization: Bearer <token>
```

### Token 获取

```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "admin123"
}
```

**响应**:
```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "expiresIn": 86400
    }
}
```

---

## 👤 用户服务 API (8081)

### 基础 URL
```
http://localhost:8081/api/users
```

### 接口列表

#### 1. 查询用户列表

```http
GET /api/users
```

**请求参数**:
- `pageNum` (可选): 页码，默认 1
- `pageSize` (可选): 每页大小，默认 10
- `username` (可选): 用户名模糊查询
- `status` (可选): 用户状态

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "list": [
            {
                "id": 1,
                "username": "admin",
                "nickname": "管理员",
                "email": "admin@example.com",
                "phone": "13800138000",
                "status": 0,
                "createTime": "2025-11-15 10:00:00"
            }
        ],
        "total": 1,
        "pageNum": 1,
        "pageSize": 10
    }
}
```

#### 2. 根据用户名查询用户

```http
GET /api/users/by-username/{username}
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "id": 1,
        "username": "admin",
        "nickname": "管理员",
        "email": "admin@example.com"
    }
}
```

#### 3. 检查用户名唯一性

```http
GET /api/users/check-username
```

**请求参数**:
- `username`: 用户名
- `userId` (可选): 用户 ID（用于更新时排除自己）

**响应示例**:
```json
{
    "code": 200,
    "message": "校验成功",
    "data": true
}
```

#### 4. 根据手机号查询用户

```http
GET /api/users/by-phone/{phone}
```

#### 5. 根据邮箱查询用户

```http
GET /api/users/by-email/{email}
```

#### 6. 批量查询用户

```http
GET /api/users/batch
```

**请求参数**:
- `ids`: 用户 ID 列表，用逗号分隔，如 `1,2,3`

#### 7. 获取用户角色列表

```http
GET /api/users/{userId}/roles
```

#### 8. 更新用户

```http
PUT /api/users/{id}
Content-Type: application/json

{
    "nickname": "新昵称",
    "email": "new@example.com",
    "phone": "13800138001"
}
```

#### 9. 修改密码

```http
PUT /api/users/{id}/password
Content-Type: application/json

{
    "oldPassword": "old123",
    "newPassword": "new123"
}
```

---

## 🔒 权限服务 API (8082)

### 基础 URL
```
http://localhost:8082/api/auth
```

### 接口列表

#### 1. 获取所有角色

```http
GET /api/auth/roles
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": [
        {
            "id": 1,
            "roleName": "超级管理员",
            "roleCode": "ROLE_ADMIN",
            "status": "0"
        }
    ]
}
```

#### 2. 根据ID获取角色

```http
GET /api/auth/roles/{id}
```

#### 3. 获取所有权限

```http
GET /api/auth/permissions
```

#### 4. 根据权限标识获取权限

```http
GET /api/auth/permissions/{permissionCode}
```

#### 5. 检查角色名唯一性

```http
GET /api/auth/roles/check-name
```

**请求参数**:
- `roleName`: 角色名
- `id` (可选): 角色 ID

#### 6. 检查权限标识唯一性

```http
GET /api/auth/permissions/check-permission
```

**请求参数**:
- `permission`: 权限标识
- `id` (可选): 权限 ID

#### 7. 根据用户ID获取角色

```http
GET /api/auth/roles/by-user/{userId}
```

#### 8. 根据用户ID获取权限

```http
GET /api/auth/permissions/by-user/{userId}
```

---

## 📚 字典服务 API (8083)

### 基础 URL
```
http://localhost:8083/api/dict
```

### 接口列表

#### 1. 获取字典类型列表

```http
GET /api/dict/types
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": [
        {
            "dictName": "用户性别",
            "dictType": "user_gender",
            "status": "0"
        }
    ]
}
```

#### 2. 获取字典项列表

```http
GET /api/dict/items/{type}
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": [
        {
            "dictLabel": "男",
            "dictValue": "1",
            "dictType": "user_gender",
            "orderNum": 1
        },
        {
            "dictLabel": "女",
            "dictValue": "2",
            "dictType": "user_gender",
            "orderNum": 2
        }
    ]
}
```

#### 3. 根据字典类型查询字典项

```http
GET /api/dict/data
```

**请求参数**:
- `dictType`: 字典类型

---

## 🏢 部门服务 API (8084)

### 基础 URL
```
http://localhost:8084/api/dept
```

### 接口列表

#### 1. 获取部门列表

```http
GET /api/dept
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": [
        {
            "id": 1,
            "deptName": "总公司",
            "parentId": 0,
            "orderNum": 1,
            "status": "0"
        }
    ]
}
```

#### 2. 根据ID获取部门

```http
GET /api/dept/{id}
```

#### 3. 根据父部门ID获取子部门

```http
GET /api/dept/children/{parentId}
```

#### 4. 获取部门树

```http
GET /api/dept/tree
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": [
        {
            "id": 1,
            "deptName": "总公司",
            "children": [
                {
                    "id": 2,
                    "deptName": "技术部",
                    "parentId": 1,
                    "children": []
                }
            ]
        }
    ]
}
```

---

## 📝 日志服务 API (8085)

### 基础 URL
```
http://localhost:8085/api/log
```

### 接口列表

#### 1. 获取日志列表

```http
GET /api/log
```

**请求参数**:
- `pageNum` (可选): 页码，默认 1
- `pageSize` (可选): 每页大小，默认 10
- `operation` (可选): 操作内容模糊查询
- `userName` (可选): 操作人模糊查询

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "list": [
            {
                "id": 1,
                "userName": "admin",
                "operation": "登录系统",
                "method": "com.basebackend.controller.LoginController.login",
                "params": "{}",
                "ip": "127.0.0.1",
                "location": "本地",
                "operationTime": 100,
                "status": "0",
                "createTime": "2025-11-15 10:00:00"
            }
        ],
        "total": 1,
        "pageNum": 1,
        "pageSize": 10
    }
}
```

#### 2. 根据ID获取日志

```http
GET /api/log/{id}
```

#### 3. 删除日志

```http
DELETE /api/log/{id}
```

---

## 📋 菜单服务 API (8088)

### 基础 URL
```
http://localhost:8088/api/menu
```

### 接口列表

#### 1. 获取菜单列表

```http
GET /api/menu
```

#### 2. 根据ID获取菜单

```http
GET /api/menu/{id}
```

#### 3. 获取菜单树

```http
GET /api/menu/tree
```

#### 4. 根据用户ID获取菜单

```http
GET /api/menu/by-user/{userId}
```

---

## 📊 监控服务 API (8089)

### 基础 URL
```
http://localhost:8089/api/monitor
```

### 接口列表

#### 1. 获取监控指标

```http
GET /api/monitor/metrics
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "cpu": {
            "usage": 45.6,
            "cores": 8
        },
        "memory": {
            "total": 16777216000,
            "used": 8452556800,
            "usage": 50.4
        },
        "disk": {
            "total": 107374182400,
            "used": 64383506432,
            "usage": 59.9
        }
    }
}
```

#### 2. 获取健康状态

```http
GET /api/monitor/health
```

#### 3. 获取服务状态

```http
GET /api/monitor/services
```

---

## 📢 通知服务 API (8090)

### 基础 URL
```
http://localhost:8090/api/notification
```

### 接口列表

#### 1. 获取通知列表

```http
GET /api/notification
```

#### 2. 发送通知

```http
POST /api/notification
Content-Type: application/json

{
    "title": "系统通知",
    "content": "这是一条测试通知",
    "type": "info",
    "userIds": [1, 2, 3]
}
```

#### 3. 标记通知为已读

```http
PUT /api/notification/{id}/read
```

---

## 👤 个人配置服务 API (8091)

### 基础 URL
```
http://localhost:8091/api/profile
```

### 接口列表

#### 1. 获取用户配置

```http
GET /api/profile/{userId}
```

**响应示例**:
```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "userId": 1,
        "theme": "dark",
        "language": "zh_CN",
        "pageSize": 10,
        "notificationEnabled": true
    }
}
```

#### 2. 更新用户配置

```http
PUT /api/profile/{userId}
Content-Type: application/json

{
    "theme": "light",
    "language": "zh_CN",
    "pageSize": 20,
    "notificationEnabled": false
}
```

---

## 🔧 公共端点

### 健康检查

所有服务都提供健康检查端点：

```http
GET /actuator/health
```

**响应示例**:
```json
{
    "status": "UP",
    "components": {
        "db": {
            "status": "UP",
            "details": {
                "database": "MySQL",
                "validationQuery": "isValid()"
            }
        },
        "redis": {
            "status": "UP",
            "details": {
                "version": "7.0.0"
            }
        }
    }
}
```

### Prometheus 指标

```http
GET /actuator/prometheus
```

**响应示例**:
```
# HELP jvm_memory_used_bytes The amount of used memory in bytes
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{id="Code Cache",} 5242880.0
jvm_memory_used_bytes{id="Compressed Class Space",} 3145728.0
jvm_memory_used_bytes{id="Metaspace",} 52428800.0
```

### API 文档 (Swagger)

每个服务都提供 Swagger UI 界面：

```http
http://localhost:<port>/swagger-ui.html
```

---

## 📦 响应格式

### 成功响应

```json
{
    "code": 200,
    "message": "操作成功",
    "data": {},
    "timestamp": "2025-11-15 10:00:00"
}
```

### 错误响应

```json
{
    "code": 500,
    "message": "操作失败",
    "timestamp": "2025-11-15 10:00:00"
}
```

### 分页响应

```json
{
    "code": 200,
    "message": "查询成功",
    "data": {
        "list": [],
        "total": 0,
        "pageNum": 1,
        "pageSize": 10
    }
}
```

---

## 🐛 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 📚 SDK 使用示例

### Java (Spring Cloud OpenFeign)

```java
@FeignClient(name = "basebackend-user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDTO getById(@PathVariable Long id);

    @GetMapping("/api/users/by-username/{username}")
    UserDTO getByUsername(@PathVariable String username);

    @GetMapping("/api/users/check-username")
    boolean checkUsernameUnique(
        @RequestParam String username,
        @RequestParam(required = false) Long userId
    );
}
```

### JavaScript (Axios)

```javascript
import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8081',
    headers: {
        'Authorization': `Bearer ${token}`
    }
});

// 查询用户
export async function getUserById(id) {
    const response = await api.get(`/api/users/${id}`);
    return response.data;
}

// 检查用户名唯一性
export async function checkUsername(username) {
    const response = await api.get('/api/users/check-username', {
        params: { username }
    });
    return response.data;
}
```

### Python (Requests)

```python
import requests

class UserServiceClient:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }

    def get_by_id(self, user_id):
        url = f'{self.base_url}/api/users/{user_id}'
        response = requests.get(url, headers=self.headers)
        response.raise_for_status()
        return response.json()

    def check_username(self, username):
        url = f'{self.base_url}/api/users/check-username'
        params = {'username': username}
        response = requests.get(url, headers=self.headers, params=params)
        response.raise_for_status()
        return response.json()
```

---

## 📝 注意事项

1. **认证**: 所有 API 请求都需要在 Header 中携带有效的 Token
2. **限流**: API 有默认的限流策略，具体限流值请参考各服务的配置
3. **幂等性**: GET、PUT、DELETE 请求是幂等的，POST 请求不是幂等的
4. **分页**: 查询列表接口支持分页，未指定分页参数时使用默认值
5. **时区**: 所有时间格式均为 `yyyy-MM-dd HH:mm:ss`，时区为东八区

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
