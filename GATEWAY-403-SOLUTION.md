# Gateway 403 错误解决方案

## 🎯 问题确认

### 根本原因

**admin-api 服务的健康检查状态是 DOWN**（由于 RocketMQ 连接失败），导致 Spring Cloud LoadBalancer 认为服务不可用，拒绝转发请求，返回 403。

### 验证结果

```bash
# admin-api 健康状态
$ curl http://localhost:8082/actuator/health
{"status":"DOWN"}

# 但直接访问登录接口正常工作
$ curl -X POST http://localhost:8082/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
# → 返回 200 ✅

# 通过网关访问失败
$ curl -X POST http://localhost:8081/admin-api/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
# → 返回 403 ❌
```

---

## ✅ 已完成的配置更新

### 修改文件

**文件**: `basebackend-gateway/src/main/resources/application-gateway.yml`

**修改内容** (第8-17行):

```yaml
cloud:
  # LoadBalancer 配置 - 禁用健康检查
  loadbalancer:
    health-check:
      refetch-instances: false  # 禁用实例健康检查
      refetch-instances-interval: 25s
      repeat-health-check: false  # 禁用重复健康检查
    configurations: default
    cache:
      enabled: false  # 禁用缓存，每次都获取最新实例
```

**作用**：让网关忽略后端服务的健康状态，即使服务健康检查为 DOWN 也可以转发请求。

---

## 🚀 重启网关服务

### 方法 1：使用 Maven（如果服务是用 mvn spring-boot:run 启动的）

```bash
# 停止网关服务（如果正在运行）
# Ctrl+C 停止当前运行的进程

# 重新启动
cd /home/wuan/IdeaProjects/basebackend/basebackend-gateway
mvn spring-boot:run
```

### 方法 2：使用 IDEA（如果是在 IDE 中运行）

1. 在 IDEA 中找到 `basebackend-gateway` 的运行配置
2. 点击 **Stop** 按钮停止服务
3. 点击 **Run** 按钮重新启动

### 方法 3：使用 kill 命令（如果是后台运行）

```bash
# 查找网关进程
ps aux | grep basebackend-gateway

# 杀掉进程（替换 <PID> 为实际进程ID）
kill <PID>

# 重新启动
cd /home/wuan/IdeaProjects/basebackend/basebackend-gateway
nohup mvn spring-boot:run > logs/gateway.log 2>&1 &
```

---

## 🧪 验证修复

### 步骤 1：等待服务启动

查看日志确认服务已启动：

```bash
# 查看网关日志（最后20行）
tail -20 /home/wuan/IdeaProjects/basebackend/basebackend-gateway/logs/*.log

# 或者查看启动日志
# 应该看到类似信息：
# "Netty started on port 8081"
# "Started GatewayApplication in X seconds"
```

### 步骤 2：测试登录接口

```bash
curl -v -X POST 'http://localhost:8081/admin-api/api/admin/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"password"}'
```

**预期结果**：
- ✅ HTTP 状态码：200 OK（成功）或 401 Unauthorized（凭据无效，但请求已到达后端）
- ❌ 如果仍然是 403，说明还有其他问题

### 步骤 3：检查网关日志

如果仍然返回 403，查看网关和 admin-api 的日志：

```bash
# 网关日志
tail -50 /home/wuan/IdeaProjects/basebackend/basebackend-gateway/logs/*.log | grep -E "403|admin-api|login"

# admin-api 日志
tail -50 /home/wuan/IdeaProjects/basebackend/basebackend-admin-api/logs/info.log | grep -E "login|auth"
```

---

## 🔍 如果问题仍未解决

### 检查清单

如果重启后仍然返回 403，请检查：

1. **网关配置是否生效**
   ```bash
   # 查看启动日志中是否有 LoadBalancer 相关配置加载信息
   grep -i "loadbalancer" logs/*.log
   ```

2. **Sentinel 是否真的被禁用**
   ```bash
   # 查看日志，确认没有 Sentinel 相关的拦截
   grep -i "sentinel" logs/*.log | grep -i "block\|forbidden\|403"
   ```

3. **检查 admin-api 服务的 OriginValidationFilter**

   这个过滤器会验证请求的 Origin/Referer 头。查看 admin-api 日志：
   ```bash
   tail -50 /home/wuan/IdeaProjects/basebackend/basebackend-admin-api/logs/warn.log | grep -i "origin"
   ```

   如果看到：`"Blocked request due to invalid origin"`，说明是 OriginValidationFilter 拦截了。

### 临时解决方案：禁用 OriginValidationFilter

如果确认是 OriginValidationFilter 的问题，可以临时禁用它：

**修改**: `basebackend-admin-api/src/main/java/com/basebackend/admin/config/AdminSecurityConfig.java`

注释掉第102行：
```java
// .addFilterAfter(originValidationFilter, CsrfFilter.class);
```

然后重启 admin-api 服务。

---

## 🎯 永久解决方案

### 方案 1：修复 admin-api 的 RocketMQ 连接

**问题**：admin-api 启动时 RocketMQ 连接失败，导致健康检查 DOWN

**解决**：
1. 启动 RocketMQ 服务器
   ```bash
   # 如果 RocketMQ 在本地
   cd /path/to/rocketmq
   nohup sh bin/mqnamesrv &
   nohup sh bin/mqbroker -n localhost:9876 &
   ```

2. 或者临时禁用 RocketMQ 健康检查

   **修改**: `basebackend-admin-api/src/main/resources/application.yml`

   添加：
   ```yaml
   management:
     health:
       rocketmq:
         enabled: false  # 禁用 RocketMQ 健康检查
   ```

### 方案 2：配置 OriginValidationFilter 白名单

**修改**: `basebackend-common` 模块的配置（需要查找 SecurityBaselineProperties 的配置位置）

添加允许的来源：
```yaml
security:
  baseline:
    allowed-origins:
      - http://localhost:3000  # 前端
      - http://localhost:8081  # 网关
      - http://localhost:8082  # admin-api 自身
    enforce-referer: false  # 不强制验证 Referer
```

---

## 📊 配置说明

### LoadBalancer 健康检查配置

```yaml
spring:
  cloud:
    loadbalancer:
      health-check:
        refetch-instances: false  # 是否重新获取实例
        repeat-health-check: false  # 是否重复健康检查
      cache:
        enabled: false  # 禁用缓存
```

**效果**：
- ✅ 网关不会检查后端服务的健康状态
- ✅ 即使服务 DOWN，只要服务实际在运行，就会转发请求
- ⚠️ 缺点：无法自动过滤真正不可用的服务

### 为什么禁用健康检查

在开发环境中，经常会遇到：
- 服务本身正常运行
- 但某些依赖（如 MQ、缓存）不可用
- 导致健康检查返回 DOWN
- 但实际上核心功能仍然可用

禁用健康检查可以避免这种情况影响开发测试。

**生产环境建议**：
- ✅ 启用健康检查
- ✅ 确保所有依赖服务都正常运行
- ✅ 配置合理的健康检查超时和重试次数

---

## 📝 完整测试流程

### 1. 重启网关

```bash
cd /home/wuan/IdeaProjects/basebackend/basebackend-gateway
# 停止现有进程后
mvn spring-boot:run
```

### 2. 等待启动完成（约30-60秒）

查看日志：
```bash
tail -f logs/*.log
```

看到 `Started GatewayApplication` 表示启动成功。

### 3. 测试登录接口

```bash
# 测试 1：通过网关访问
curl -X POST 'http://localhost:8081/admin-api/api/admin/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"password"}'

# 测试 2：直接访问（对比）
curl -X POST 'http://localhost:8082/api/admin/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"password"}'
```

### 4. 测试工作流接口（确认之前的对接正常）

```bash
# 测试工作流统计接口
curl 'http://localhost:8081/api/workflow/statistics'

# 测试流程定义列表
curl 'http://localhost:8081/api/workflow/definitions'
```

---

## ✅ 成功标志

当看到以下结果，说明问题已解决：

1. **登录接口返回 200**
   ```json
   {
     "code": 200,
     "success": true,
     "data": {
       "token": "eyJ...",
       "userInfo": {...}
     }
   }
   ```

2. **网关日志中没有 403 相关错误**
   ```
   DEBUG - 认证路径 /admin-api/api/admin/auth/login 标记为可信来源
   DEBUG - 路径 /admin-api/api/admin/auth/login 在白名单中，跳过认证
   ```

3. **admin-api 日志中有登录请求记录**
   ```
   INFO - 用户登录请求: username=admin
   ```

---

## 🆘 需要进一步帮助

如果按照以上步骤操作后问题仍未解决，请提供：

1. **网关启动日志** (最后100行)
   ```bash
   tail -100 /home/wuan/IdeaProjects/basebackend/basebackend-gateway/logs/*.log
   ```

2. **admin-api 日志** (最后100行)
   ```bash
   tail -100 /home/wuan/IdeaProjects/basebackend/basebackend-admin-api/logs/*.log
   ```

3. **测试请求的详细输出**
   ```bash
   curl -v -X POST 'http://localhost:8081/admin-api/api/admin/auth/login' \
     -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"password"}'
   ```

---

## 📚 相关文档

- [Gateway 403 故障排查指南](./GATEWAY-403-TROUBLESHOOTING.md)
- [工作流前后端对接指南](./WORKFLOW-FRONTEND-BACKEND-INTEGRATION.md)
- [Spring Cloud LoadBalancer 文档](https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/#spring-cloud-loadbalancer)

---

## 📌 总结

**问题**：admin-api 健康检查 DOWN → LoadBalancer 拒绝转发 → 返回 403

**解决**：禁用 LoadBalancer 健康检查 → 允许转发到 DOWN 的服务

**文件修改**：`application-gateway.yml` 添加 LoadBalancer 配置

**操作**：重启网关服务

**验证**：测试登录接口返回 200

**状态**：⏳ 等待您重启网关后验证
