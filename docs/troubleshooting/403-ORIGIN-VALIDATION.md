# 403 Forbidden - Origin验证问题解决方案

## 问题描述

直接访问服务（不通过网关）时返回403 Forbidden错误：

```bash
curl -X POST http://localhost:8087/api/logs/search \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "basebackend-user-api"}'

# 返回：
{
  "code": 403,
  "message": "Invalid request origin"
}
```

## 错误原因

### 1. Origin验证失败

`OriginValidationFilter`会验证请求的Origin头，如果Origin不在允许列表中，会返回403错误。

**触发条件：**
- 请求方法是POST/PUT/DELETE/PATCH（非GET/HEAD/OPTIONS）
- 请求包含Cookie头
- Origin头的值不在`security.baseline.allowed-origins`配置中

### 2. 日志信息

服务日志中会看到：
```
WARN  c.b.s.f.OriginValidationFilter - Blocked request due to invalid origin. 
method=POST, uri=/api/logs/search, origin=http://localhost:3000, referer=null
```

## 解决方案

### 方案1：配置允许的Origin（推荐）

在服务的`application.yml`中添加配置：

```yaml
security:
  baseline:
    # 允许的请求来源
    allowed-origins:
      - http://localhost:3000      # 前端开发环境
      - http://localhost:8080      # 网关地址
      - http://localhost:8087      # 本服务地址（如果需要直接访问）
    # 是否强制校验Referer
    enforce-referer: false
```

### 方案2：通过Nacos配置中心

在Nacos中为服务添加配置：

**Data ID:** `basebackend-observability-service.yml`
**Group:** `DEFAULT_GROUP`

```yaml
security:
  baseline:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
      - http://localhost:8087
      - https://your-production-domain.com
    enforce-referer: false
```

### 方案3：禁用Origin验证（不推荐）

如果确实不需要Origin验证，可以设置空列表：

```yaml
security:
  baseline:
    allowed-origins: []  # 空列表表示不验证
```

### 方案4：移除Cookie头

如果不需要Cookie，可以在请求中移除Cookie头：

```bash
curl -X POST http://localhost:8087/api/logs/search \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "basebackend-user-api"}'
```

**注意：** 这样会跳过Origin验证，但可能影响CSRF保护。

## 各服务配置示例

### Observability Service (8087)

```yaml
# basebackend-observability-service/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
      - http://localhost:8087
    enforce-referer: false
```

### Notification Service (8086)

```yaml
# basebackend-notification-service/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
      - http://localhost:8086
    enforce-referer: false
```

### User API (8082)

```yaml
# basebackend-user-api/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
      - http://localhost:8082
    enforce-referer: false
```

### System API (8083)

```yaml
# basebackend-system-api/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
      - http://localhost:8083
    enforce-referer: false
```

### Auth API (8081)

```yaml
# basebackend-auth-api/src/main/resources/application.yml
security:
  baseline:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
      - http://localhost:8081
    enforce-referer: false
```

## 生产环境配置

生产环境应该配置实际的域名：

```yaml
security:
  baseline:
    allowed-origins:
      - https://www.example.com
      - https://admin.example.com
      - https://api.example.com
    enforce-referer: true  # 生产环境建议启用
```

## Origin验证逻辑

### 验证流程

1. **检查是否需要验证：**
   - 请求方法是POST/PUT/DELETE/PATCH
   - 请求包含Cookie头

2. **检查Origin头：**
   - 提取Origin头的值
   - 与allowed-origins列表比较
   - 匹配成功则放行

3. **检查Referer头（如果enforce-referer=true）：**
   - 如果Origin验证失败
   - 提取Referer头的值
   - 与allowed-origins列表比较
   - 匹配成功则放行

4. **拒绝请求：**
   - 如果都不匹配，返回403

### URL规范化

Origin和Referer会被规范化后再比较：

```
http://localhost:3000/path?query=1  →  http://localhost:3000
https://example.com:443/path        →  https://example.com
http://example.com:80/path          →  http://example.com
```

## 测试验证

### 测试1：有效的Origin

```bash
curl -X POST http://localhost:8087/api/logs/search \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Cookie: XSRF-TOKEN=test" \
  -d '{"serviceName": "basebackend-user-api"}'

# 应该返回200
```

### 测试2：无效的Origin

```bash
curl -X POST http://localhost:8087/api/logs/search \
  -H "Origin: http://evil.com" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -H "Cookie: XSRF-TOKEN=test" \
  -d '{"serviceName": "basebackend-user-api"}'

# 应该返回403
{
  "code": 403,
  "message": "Invalid request origin"
}
```

### 测试3：无Cookie（跳过验证）

```bash
curl -X POST http://localhost:8087/api/logs/search \
  -H "Origin: http://evil.com" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"serviceName": "basebackend-user-api"}'

# 应该返回200（因为没有Cookie，跳过Origin验证）
```

## 常见问题

### Q1: 为什么GET请求不需要验证？

A: GET请求通常是幂等的，不会修改服务器状态，因此不需要CSRF保护。Origin验证主要针对可能修改状态的请求（POST/PUT/DELETE）。

### Q2: 为什么没有Cookie就不验证？

A: Origin验证是CSRF保护的一部分。如果请求不包含Cookie，就不会自动携带session信息，CSRF攻击的风险较低。

### Q3: 生产环境应该如何配置？

A: 
1. 只添加实际需要的域名
2. 启用enforce-referer
3. 使用HTTPS
4. 定期审查allowed-origins列表

### Q4: 如何添加多个域名？

A:
```yaml
security:
  baseline:
    allowed-origins:
      - https://www.example.com
      - https://admin.example.com
      - https://api.example.com
      - https://mobile.example.com
```

### Q5: 支持通配符吗？

A: 当前实现不支持通配符。如果需要支持子域名，需要明确列出每个子域名。

## 安全建议

### 1. 最小权限原则

只添加真正需要的Origin：

```yaml
# 好的做法
allowed-origins:
  - https://www.example.com
  - https://admin.example.com

# 不好的做法
allowed-origins:
  - http://localhost:3000
  - http://localhost:3001
  - http://localhost:3002
  - http://192.168.1.100
  - http://192.168.1.101
  # ... 太多了
```

### 2. 使用HTTPS

生产环境必须使用HTTPS：

```yaml
allowed-origins:
  - https://www.example.com  # ✓
  - http://www.example.com   # ✗ 不安全
```

### 3. 不要禁用验证

除非有充分理由，否则不要设置空列表：

```yaml
# 不推荐
allowed-origins: []
```

### 4. 定期审查

定期检查和更新allowed-origins列表，移除不再使用的域名。

### 5. 环境隔离

不同环境使用不同的配置：

```yaml
# 开发环境
allowed-origins:
  - http://localhost:3000

# 生产环境
allowed-origins:
  - https://www.example.com
```

## 调试技巧

### 1. 查看日志

启用DEBUG日志查看详细信息：

```yaml
logging:
  level:
    com.basebackend.security.filter.OriginValidationFilter: DEBUG
```

### 2. 检查请求头

使用浏览器开发者工具或curl -v查看实际发送的请求头：

```bash
curl -v -X POST http://localhost:8087/api/logs/search \
  -H "Origin: http://localhost:3000" \
  ...
```

### 3. 临时禁用验证

测试时可以临时设置空列表：

```yaml
security:
  baseline:
    allowed-origins: []
```

**注意：** 测试完成后记得恢复配置！

## 相关文档

- [403 Forbidden问题排查](./403-FORBIDDEN-ISSUE.md)
- [CSRF保护配置](./CSRF-PROTECTION.md)
- [Spring Security配置](../SPRING-SECURITY-MULTI-SERVLET.md)

## 更新日志

- 2024-11-19: 添加Origin验证配置到observability-service和notification-service
- 2024-11-19: 创建本文档
