# 快速修复403问题

## 问题
调用 `http://localhost:8081/api/user/auth/login` 返回403 Forbidden

## 原因
请求包含Cookie但没有Origin头，被OriginValidationFilter拦截。

## 已修复
已在配置文件中添加了允许的Origin列表：
- `basebackend-user-api/src/main/resources/application.yml`
- `basebackend-system-api/src/main/resources/application.yml`
- `basebackend-auth-api/src/main/resources/application.yml`

## 应用修复

### 方法1：重启服务（推荐）

```bash
# 停止所有服务
pkill -f basebackend

# 重新启动
./bin/start/start-microservices.sh

# 或手动启动
cd basebackend-user-api && mvn spring-boot:run &
cd basebackend-system-api && mvn spring-boot:run &
cd basebackend-auth-api && mvn spring-boot:run &
```

### 方法2：使用正确的请求（无需重启）

#### 选项A：不发送Cookie（推荐）

```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username": "admin","password": "password"}'
```

#### 选项B：添加Origin头

```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'Origin: http://localhost:8081' \
  -H 'Cookie: JSESSIONID=xxx; XSRF-TOKEN=xxx' \
  -d '{"username": "admin","password": "password"}'
```

## 在Postman/Apifox中修复

### 方法1：添加Origin头
在Headers中添加：
```
Origin: http://localhost:8081
```

### 方法2：移除Cookie
1. 打开Cookie管理器
2. 删除localhost相关的Cookie
3. 重新发送请求

## 验证修复

```bash
# 测试登录接口
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username": "admin","password": "password"}'

# 预期响应（200 OK）
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600,
    "userInfo": {
      "userId": 1,
      "username": "admin"
    }
  }
}
```

## 详细文档

查看完整的排查指南：[403-FORBIDDEN-ISSUE.md](./403-FORBIDDEN-ISSUE.md)

---

**快速修复时间**: 2025-11-17  
**问题类型**: Origin验证失败
