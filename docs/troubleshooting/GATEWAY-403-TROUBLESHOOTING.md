# Gateway 403 错误排查指南

## 🔍 问题描述

访问登录接口时出现 403 错误：
- ❌ 通过网关访问: `http://localhost:8081/admin-api/api/admin/auth/login` → 返回 403
- ✅ 直接访问服务: `http://localhost:8082/api/admin/auth/login` → 正常工作

## 📊 问题分析

### 根本原因

403 错误是由 **Sentinel 的权限控制（AuthorityException）** 触发的，不是认证过滤器（AuthenticationFilter）的问题。

**关键发现**：
1. `AuthenticationFilter` 的白名单配置是正确的，认证路径已被排除
2. 但 Sentinel 的权限控制在请求到达 `AuthenticationFilter` **之前** 就已经拦截了请求
3. Sentinel 的权限规则通常配置在 **Nacos** 中（`application-gateway.yml` 第35-43行配置了 Nacos 数据源）

### 请求处理流程

```
请求 → Gateway
  ↓
Sentinel 过滤器（触发 AuthorityException ❌ 403）
  ↓
[永远到不了这里] → AuthenticationFilter（白名单检查）
  ↓
[永远到不了这里] → 目标服务
```

---

## ✅ 解决方案

### 方案 1：修改 Nacos 中的 Sentinel 权限规则（推荐）

#### 步骤 1：登录 Nacos 控制台

访问：`http://1.117.67.222:8848/nacos`（用户名/密码：nacos/nacos）

#### 步骤 2：查找并修改权限规则

1. 进入 **配置管理** → **配置列表**
2. 查找以下配置项：
   - `dataId`: `basebackend-gateway-gw-flow-rules`
   - `group`: `SENTINEL_GROUP`
3. 点击 **编辑**，查看是否有类似以下的权限规则：

```json
[
  {
    "resource": "admin-api-route",
    "resourceMode": 0,
    "limitApp": "某个特定来源",
    "strategy": 0
  }
]
```

#### 步骤 3：添加可信来源

如果发现有限制 `limitApp` 的规则，需要将 `trusted-auth-request` 添加到允许列表中：

```json
[
  {
    "resource": "admin-api-route",
    "resourceMode": 0,
    "limitApp": "trusted-auth-request,其他已有来源",
    "strategy": 0
  }
]
```

或者，**更简单的做法**：直接删除所有 authority 相关的规则（如果不需要权限控制）。

#### 步骤 4：保存并发布配置

配置会实时生效，无需重启网关。

---

### 方案 2：在 Sentinel 控制台中配置白名单

#### 步骤 1：访问 Sentinel 控制台

访问：`http://1.117.67.222:8858`

#### 步骤 2：找到网关规则

1. 在左侧菜单找到 **basebackend-gateway** 应用
2. 进入 **API 管理** 或 **授权规则**

#### 步骤 3：添加授权规则

为 `auth_api` API 组添加白名单规则：
- 资源名：`auth_api`
- 流控应用：`trusted-auth-request`
- 授权类型：白名单

---

### 方案 3：临时禁用 Sentinel 权限控制（用于测试）

#### 修改 `application-gateway.yml`

在 `basebackend-gateway/src/main/resources/application-gateway.yml` 中临时注释掉 Sentinel 配置：

```yaml
spring:
  cloud:
    sentinel:
      # 临时禁用 Sentinel 进行测试
      enabled: false
      # transport:
      #   dashboard: 1.117.67.222:8858
```

**注意**：这只是临时测试方案，生产环境不应该禁用 Sentinel。

---

### 方案 4：代码层面完全绕过（不推荐）

如果以上方案都不可行，可以在代码中添加过滤器，在 Sentinel 之前放行认证请求。

创建 `AuthBypassFilter.java`：

```java
@Component
@Order(-2)  // 确保在 Sentinel 过滤器之前执行
public class AuthBypassFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 如果是认证路径，添加特殊属性跳过 Sentinel
        if (path.contains("/auth/")) {
            exchange.getAttributes().put("bypass-sentinel", true);
        }

        return chain.filter(exchange);
    }
}
```

---

## 🔧 已完成的配置更新

### 1. SentinelGatewayRuleConfig.java

**位置**: `basebackend-gateway/src/main/java/com/basebackend/gateway/config/SentinelGatewayRuleConfig.java`

**更新内容**:
- ✅ 添加了 `RequestOriginParser`，将认证请求标记为 `trusted-auth-request` 来源
- ✅ 定义了 `auth_api` 和 `workflow_api` API 组
- ✅ 认证相关路径（login、register、logout 等）自动标记为可信来源

**关键代码** (第60-71行):
```java
private String parseOrigin(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().value();

    if (isAuthPath(path)) {
        log.debug("认证路径 {} 标记为可信来源", path);
        return "trusted-auth-request";
    }

    return "default";
}
```

### 2. SentinelConfig.java

**位置**: `basebackend-gateway/src/main/java/com/basebackend/gateway/config/SentinelConfig.java`

**更新内容**:
- ✅ 在 AuthorityException 的日志中添加了更明确的提示信息（第72行）
- ✅ 提示检查 Nacos 中的 Sentinel 权限规则配置

---

## 🧪 测试步骤

### 步骤 1：重启网关服务

配置更新后需要重启网关：

```bash
cd basebackend-gateway
mvn spring-boot:run
```

### 步骤 2：检查日志

启动时应该看到：

```
INFO  - Sentinel网关规则配置完成，已定义API组: auth_api, workflow_api
```

### 步骤 3：测试登录接口

```bash
curl -X POST http://localhost:8081/admin-api/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

### 步骤 4：查看日志输出

#### 如果仍然返回 403：

在日志中查找：

```
WARN - 触发权限控制: /admin-api/api/admin/auth/login - 请检查Nacos中的Sentinel权限规则配置
DEBUG - 认证路径 /admin-api/api/admin/auth/login 标记为可信来源
```

**说明**：
- ✅ 第一条日志表示 Sentinel 权限控制被触发了
- ✅ 第二条日志表示 RequestOriginParser 工作正常
- ❌ **问题**：Nacos 中的权限规则不允许 `trusted-auth-request` 来源

**解决**：按照 **方案 1** 修改 Nacos 配置。

#### 如果返回 200 或其他状态码（不是 403）：

✅ **问题已解决！** 请求已经通过 Sentinel 检查。

---

## 🎯 推荐的解决路径

### 快速测试（5分钟）

1. ✅ **方案 3**：临时禁用 Sentinel，验证问题确实是 Sentinel 导致的
2. 如果禁用后正常，说明确认是 Sentinel 权限规则的问题

### 正式修复（10分钟）

1. ✅ 重新启用 Sentinel
2. ✅ **方案 1**：登录 Nacos，修改或删除权限规则
3. ✅ 确保 `trusted-auth-request` 被允许访问，或者完全移除权限限制规则

### 验证修复（5分钟）

1. ✅ 测试通过网关访问登录接口
2. ✅ 测试其他认证相关接口（注册、登出、刷新token）
3. ✅ 测试工作流接口是否正常

---

## 📋 检查清单

在排查问题时，请按顺序检查以下项目：

- [ ] **网关服务已重启**，确保最新配置生效
- [ ] **查看网关日志**，确认是否有 "Sentinel网关规则配置完成" 日志
- [ ] **登录 Nacos 控制台**，查找 `basebackend-gateway-gw-flow-rules` 配置
- [ ] **查看 Sentinel 控制台**（http://1.117.67.222:8858），检查授权规则
- [ ] **测试直接访问服务**（8082端口），确认服务本身正常
- [ ] **测试通过网关访问**（8081端口），查看是否还返回 403
- [ ] **检查网关日志**，确认是否仍触发 AuthorityException

---

## 🔑 关键文件位置

| 文件 | 路径 | 说明 |
|------|------|------|
| Sentinel 规则配置 | `basebackend-gateway/.../SentinelGatewayRuleConfig.java` | 定义 API 组和来源解析 |
| Sentinel 异常处理 | `basebackend-gateway/.../SentinelConfig.java` | 配置限流降级回调 |
| Gateway 配置 | `basebackend-gateway/.../application-gateway.yml` | Sentinel 和 Nacos 集成配置 |
| 认证过滤器 | `basebackend-gateway/.../AuthenticationFilter.java` | JWT 认证和白名单 |
| Nacos 规则 | Nacos 控制台配置 | `basebackend-gateway-gw-flow-rules` |

---

## 💡 理解 Sentinel 的权限控制

### Sentinel 权限控制工作原理

```
请求 → Sentinel 获取请求来源（通过 RequestOriginParser）
  ↓
检查是否有该资源的权限规则
  ↓
如果有权限规则，检查该来源是否在白名单/黑名单中
  ↓
不在白名单或在黑名单中 → 抛出 AuthorityException → 返回 403
  ↓
在白名单且不在黑名单中 → 放行
```

### 我们的解决方案

1. **自定义 RequestOriginParser**（已完成）
   - 认证路径 → 返回 `"trusted-auth-request"`
   - 其他路径 → 返回 `"default"`

2. **配置权限规则**（需要在 Nacos 中操作）
   - 将 `trusted-auth-request` 添加到白名单
   - 或者删除不必要的权限规则

---

## 🆘 如果问题仍未解决

### 收集诊断信息

1. **Gateway 日志**：
   ```bash
   tail -f basebackend-gateway/logs/*.log
   ```

2. **Nacos 配置快照**：
   - 导出 `basebackend-gateway-gw-flow-rules` 配置内容

3. **Sentinel 控制台截图**：
   - 授权规则页面
   - 网关 API 管理页面

4. **测试请求完整输出**：
   ```bash
   curl -v -X POST http://localhost:8081/admin-api/api/admin/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"password"}'
   ```

### 提供以上信息可以帮助进一步诊断问题

---

## 📚 相关文档

- [Sentinel 官方文档 - 网关限流](https://sentinelguard.io/zh-cn/docs/api-gateway-flow-control.html)
- [Spring Cloud Gateway + Sentinel 集成](https://github.com/alibaba/spring-cloud-alibaba/wiki/Sentinel)
- [工作流前后端对接指南](./WORKFLOW-FRONTEND-BACKEND-INTEGRATION.md)

---

## ✅ 总结

**问题根源**：Sentinel 的权限控制规则阻止了认证接口的访问

**已完成的代码修改**：
- ✅ `SentinelGatewayRuleConfig.java` - 添加来源解析器
- ✅ `SentinelConfig.java` - 改进日志提示

**需要的配置修改**：
- ⏳ Nacos 中添加 `trusted-auth-request` 到权限规则白名单
- ⏳ 或删除不必要的权限限制规则

**预期结果**：配置修改后，通过网关访问认证接口应该正常工作（返回 200 或 401，而不是 403）

