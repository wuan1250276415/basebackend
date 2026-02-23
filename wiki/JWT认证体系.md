[< 返回首页](Home) | [< 上一页: Common公共模块详解](Common公共模块详解)

---

# JWT 认证体系

---

## 概述

BaseBackend 采用基于 JWT（JSON Web Token）的无状态认证体系，实现了双 Token 机制、密钥轮换、黑名单吊销、多设备管理等企业级安全特性。

核心模块位于 `basebackend-jwt/`，共 16 个核心文件。

---

## 双 Token 机制

### 设计原理

```
┌──────────┐   登录请求    ┌──────────┐
│  客户端   │ ──────────> │  服务端   │
│          │             │          │
│          │ <────────── │          │
│          │  Access Token (短期)    │
│          │  Refresh Token (长期)   │
└──────────┘             └──────────┘
```

| Token 类型 | 有效期 | 用途 | 存储位置 |
|-----------|--------|------|---------|
| Access Token | 短期（如 30 分钟） | API 请求认证 | 前端内存 / LocalStorage |
| Refresh Token | 长期（如 7 天） | 刷新 Access Token | HttpOnly Cookie / 安全存储 |

### Token 生命周期

```
1. 用户登录
   └─ 服务端生成 Access Token + Refresh Token
   └─ 返回给客户端

2. API 请求
   └─ 客户端携带 Access Token
   └─ Gateway AuthenticationFilter 验证
   └─ 有效则放行，过期则返回 401

3. Token 刷新
   └─ Access Token 过期后
   └─ 客户端使用 Refresh Token 请求新的 Access Token
   └─ 服务端验证 Refresh Token
   └─ 签发新的 Access Token（可选：轮换 Refresh Token）

4. 用户登出
   └─ 将 Access Token 和 Refresh Token 加入黑名单
   └─ 后续使用该 Token 的请求被拒绝
```

### 使用示例

```java
// 登录 - 生成 Token
@PostMapping("/auth/login")
public Result<LoginVO> login(@RequestBody LoginDTO dto) {
    // 验证用户名密码
    User user = authService.authenticate(dto.getUsername(), dto.getPassword());

    // 生成双 Token
    String accessToken = jwtTokenProvider.generateAccessToken(user);
    String refreshToken = jwtTokenProvider.generateRefreshToken(user);

    return Result.success(new LoginVO(accessToken, refreshToken));
}

// 刷新 Token
@PostMapping("/auth/refresh")
public Result<TokenVO> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
    String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken);
    return Result.success(new TokenVO(newAccessToken));
}

// 登出 - 吊销 Token
@PostMapping("/auth/logout")
public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
    String token = authorization.replace("Bearer ", "");
    jwtTokenProvider.revokeToken(token);
    return Result.success();
}
```

---

## Token 黑名单 / 吊销

### 机制说明

JWT 是无状态的，一旦签发无法直接撤销。通过维护一个黑名单实现 Token 吊销：

```
请求到达 → 提取 Token → 检查黑名单（Redis）
                              │
                     ┌────────┼────────┐
                   在黑名单中        不在黑名单中
                     │                │
                  拒绝请求         正常验证签名/过期
```

### 实现特点
- **存储**：使用 Redis 存储黑名单，Key 为 Token 或 Token ID（jti）
- **过期清理**：黑名单条目的 TTL 设置为 Token 的剩余有效期，自动过期清理
- **批量吊销**：支持按用户 ID 吊销所有 Token

---

## 密钥轮换

### 为什么需要密钥轮换？
- 降低密钥泄露风险
- 符合安全合规要求
- 限制密钥暴露窗口

### 轮换流程

```
时间 T1: 使用密钥 Key-A 签发 Token
         ├── 所有 Token 用 Key-A 签名

时间 T2: 轮换到密钥 Key-B
         ├── 新 Token 用 Key-B 签名
         ├── 验证时同时尝试 Key-A 和 Key-B（过渡期）

时间 T3: Key-A 的所有 Token 均已过期
         ├── 移除 Key-A
         ├── 仅使用 Key-B
```

### 配置

```yaml
jwt:
  # 密钥轮换配置
  key-rotation:
    enabled: true
    interval: 7d         # 每 7 天轮换一次
    overlap-period: 2h   # 新旧密钥重叠期
```

---

## 多设备管理

### 功能说明
- 同一用户可在多个设备上同时登录
- 支持查看所有在线设备
- 支持踢出指定设备

### 实现机制

```
用户 Alice 登录了 3 个设备：
├── 手机    → Token-A (device_id: mobile-xxx)
├── 电脑    → Token-B (device_id: desktop-yyy)
└── 平板    → Token-C (device_id: tablet-zzz)

Redis 中存储：
user:session:alice → {
    mobile-xxx:  Token-A-info,
    desktop-yyy: Token-B-info,
    tablet-zzz:  Token-C-info
}
```

### 使用

```java
// 查看当前用户所有在线设备
@GetMapping("/security/devices")
public Result<List<DeviceVO>> listDevices() {
    return Result.success(securityService.listOnlineDevices());
}

// 踢出指定设备
@DeleteMapping("/security/devices/{deviceId}")
public Result<Void> kickDevice(@PathVariable String deviceId) {
    securityService.kickDevice(deviceId);
    return Result.success();
}
```

---

## 事件审计

JWT 模块内置安全事件审计，记录所有关键认证操作：

| 事件类型 | 记录内容 |
|---------|---------|
| 登录成功 | 用户 ID、IP、设备信息、时间 |
| 登录失败 | 用户名、IP、失败原因、时间 |
| Token 刷新 | 用户 ID、旧 Token ID、新 Token ID |
| Token 吊销 | 用户 ID、Token ID、吊销原因 |
| 密钥轮换 | 旧密钥 ID、新密钥 ID、轮换时间 |
| 设备踢出 | 用户 ID、设备 ID、操作者 |

---

## 配置参考

```yaml
jwt:
  # Access Token 配置
  access-token:
    secret: ${JWT_SECRET:your-256-bit-secret}
    expiration: 1800         # 30 分钟（秒）
    issuer: basebackend

  # Refresh Token 配置
  refresh-token:
    secret: ${JWT_REFRESH_SECRET:your-refresh-secret}
    expiration: 604800       # 7 天（秒）

  # 黑名单配置
  blacklist:
    enabled: true
    prefix: "jwt:blacklist:"

  # 密钥轮换
  key-rotation:
    enabled: false
    interval: 7d

  # 多设备管理
  multi-device:
    enabled: true
    max-devices: 5          # 同一用户最多 5 个设备
```

---

| [< 上一页: Common公共模块详解](Common公共模块详解) | [下一页: 权限控制 RBAC >](权限控制RBAC) |
|---|---|
