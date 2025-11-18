# 403 Forbidden 问题排查指南

## 问题描述

当调用微服务API时返回403 Forbidden错误，特别是登录接口。

## 常见原因

### 1. Origin验证失败（最常见）

**症状**：
- 请求包含Cookie头
- 请求是POST/PUT/DELETE等非GET方法
- 没有Origin或Referer头，或者Origin不在允许列表中

**原因**：
`OriginValidationFilter` 会验证请求的Origin/Referer头，防止CSRF攻击。

**解决方案**：

#### 方案A：添加Origin头（推荐用于API测试）

```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'Origin: http://localhost:8081' \
  -d '{"username": "admin","password": "password"}'
```

#### 方案B：移除Cookie头（推荐用于无状态API）

```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username": "admin","password": "password"}'
```

注意：不要发送Cookie头（如JSESSIONID、XSRF-TOKEN等）

#### 方案C：配置允许的Origin（推荐用于开发环境）

在 `application.yml` 中添加：

```yaml
security:
  baseline:
    allowed-origins:
      - http://localhost:8081
      - http://localhost:8080
      - http://localhost:3000
      - http://127.0.0.1:8081
      - http://127.0.0.1:8080
      - http://127.0.0.1:3000
    enforce-referer: false
```

### 2. CSRF Token验证失败

**症状**：
- 请求需要CSRF Token但未提供
- CSRF Token无效或过期

**解决方案**：

登录接口已配置CSRF忽略，但如果仍有问题，检查Security配置：

```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers(
        new AntPathRequestMatcher("/api/user/auth/**"),
        new AntPathRequestMatcher("/api/public/**")
    )
)
```

### 3. 权限不足

**症状**：
- 已登录但访问某些接口返回403
- Token有效但权限不够

**解决方案**：

检查用户角色和权限配置。

### 4. JWT Token无效

**症状**：
- Token过期
- Token签名无效
- Token格式错误

**解决方案**：

重新登录获取新Token。

## 快速诊断

### 步骤1：检查请求头

```bash
# 查看完整的请求和响应
curl -v -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username": "admin","password": "password"}'
```

检查：
- 是否有Cookie头？
- 是否有Origin头？
- 是否有Referer头？

### 步骤2：查看服务日志

```bash
tail -f logs/error.log | grep "Blocked request"
```

如果看到类似日志：
```
Blocked request due to invalid origin. method=POST, uri=/api/user/auth/login, origin=null, referer=null
```

说明是Origin验证失败。

### 步骤3：测试不同的请求方式

#### 测试1：不带Cookie
```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username": "admin","password": "password"}'
```

#### 测试2：带Origin头
```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'Origin: http://localhost:8081' \
  -d '{"username": "admin","password": "password"}'
```

#### 测试3：带Cookie和Origin
```bash
curl -X POST 'http://localhost:8081/api/user/auth/login' \
  -H 'Content-Type: application/json' \
  -H 'Origin: http://localhost:8081' \
  -H 'Cookie: JSESSIONID=xxx' \
  -d '{"username": "admin","password": "password"}'
```

## 各微服务的配置

### User API (8081)

```yaml
# basebackend-user-api/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:8081
      - http://localhost:8080
      - http://localhost:3000
    enforce-referer: false
```

### System API (8082)

```yaml
# basebackend-system-api/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:8082
      - http://localhost:8080
      - http://localhost:3000
    enforce-referer: false
```

### Auth API (8083)

```yaml
# basebackend-auth-api/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:8083
      - http://localhost:8080
      - http://localhost:3000
    enforce-referer: false
```

## Postman/Apifox配置

### 方法1：添加Origin头

在请求头中添加：
```
Origin: http://localhost:8081
```

### 方法2：禁用Cookie自动发送

在Postman/Apifox设置中：
1. 打开设置
2. 找到Cookie管理
3. 禁用自动发送Cookie

### 方法3：清除Cookie

在Postman/Apifox中：
1. 打开Cookie管理器
2. 删除localhost相关的Cookie

## 生产环境配置

在生产环境中，应该配置实际的域名：

```yaml
security:
  baseline:
    allowed-origins:
      - https://www.example.com
      - https://admin.example.com
      - https://api.example.com
    enforce-referer: true
```

## 安全建议

1. **开发环境**：可以设置 `enforce-referer: false` 方便测试
2. **生产环境**：必须设置 `enforce-referer: true` 并配置准确的域名
3. **API测试**：使用不带Cookie的请求，或添加正确的Origin头
4. **前端应用**：浏览器会自动发送正确的Origin头

## 相关代码

### OriginValidationFilter

位置：`basebackend-security/src/main/java/com/basebackend/security/filter/OriginValidationFilter.java`

关键逻辑：
```java
private boolean requiresValidation(HttpServletRequest request) {
    HttpMethod method = HttpMethod.resolve(request.getMethod());
    if (method == null) {
        return false;
    }
    // 只对非GET请求且包含Cookie的请求进行验证
    return !(HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) || HttpMethod.OPTIONS.equals(method))
            && request.getHeader("Cookie") != null;
}
```

### Security配置

位置：`basebackend-user-api/src/main/java/com/basebackend/user/config/AdminSecurityConfig.java`

CSRF忽略配置：
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers(
        new AntPathRequestMatcher("/api/user/auth/**"),
        new AntPathRequestMatcher("/api/public/**"),
        new AntPathRequestMatcher("/actuator/**")
    )
)
```

## 总结

403错误最常见的原因是：
1. ✅ **请求包含Cookie但没有Origin头** - 添加Origin头或移除Cookie
2. ✅ **Origin不在允许列表中** - 配置allowed-origins
3. CSRF Token验证失败 - 检查CSRF配置
4. 权限不足 - 检查用户权限

**推荐解决方案**：
- API测试工具：不发送Cookie，或添加Origin头
- 开发环境：配置allowed-origins并设置enforce-referer: false
- 生产环境：配置准确的域名并设置enforce-referer: true

---

**文档版本**: v1.0  
**更新时间**: 2025-11-17  
**相关问题**: Origin验证、CSRF保护、403 Forbidden
