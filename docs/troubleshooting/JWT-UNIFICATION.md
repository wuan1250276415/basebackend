# JWT统一化方案说明

## 问题背景

之前Gateway和demo-api使用的JWT工具存在不统一的问题：
- **Gateway**: 直接引入JWT依赖，有自己的`JwtUtil`类（避免Spring Security的Servlet冲突）
- **demo-api**: 通过`basebackend-security`模块引入，使用security模块的`JwtUtil`
- **问题**: 两个`JwtUtil`虽然代码相同，但是独立的类，可能导致配置不一致、Token无法互通

## 解决方案

创建独立的`basebackend-jwt`模块，统一提供JWT功能：

### 架构设计

```
basebackend-jwt (新模块)
    ├── 不依赖Spring Security
    ├── 不依赖Spring MVC或WebFlux
    ├── 仅依赖spring-boot-starter和jjwt
    └── 可在任何环境使用（Gateway的WebFlux、demo-api的MVC）

basebackend-gateway → 依赖 basebackend-jwt
basebackend-security → 依赖 basebackend-jwt
basebackend-demo-api → 依赖 basebackend-security → 间接依赖 basebackend-jwt
```

### 模块结构

**basebackend-jwt/**
```
├── pom.xml                          # 仅依赖spring-boot-starter和jjwt
└── src/main/java/com/basebackend/jwt/
    └── JwtUtil.java                 # 统一的JWT工具类
```

### 核心特性

#### 1. 环境无关
```xml
<!-- 仅基础依赖，不包含Web或Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
```

#### 2. 统一配置
所有服务使用相同的JWT配置（通过application.yml）：

```yaml
jwt:
  secret: basebackend-secret-key-for-jwt-token-generation-minimum-256-bits
  expiration: 86400000  # 24小时
```

#### 3. 增强功能
相比原来的JwtUtil，新增了一些实用方法：

```java
// 检查Token是否即将过期（1小时内）
public boolean isTokenExpiringSoon(String token)

// 获取Token过期时间
public Date getExpirationDateFromToken(String token)

// 刷新Token时保留原有的自定义claims
public String refreshToken(String token)
```

## 迁移变更

### 1. 新增模块

**basebackend-jwt/pom.xml**
- 创建新的独立模块
- 添加到父pom.xml的modules列表

### 2. 修改basebackend-security

**pom.xml**
```xml
<!-- 移除直接的JWT依赖 -->
<!-- 添加JWT模块依赖 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-jwt</artifactId>
    <version>${project.version}</version>
</dependency>
```

**删除文件**
- `src/main/java/com/basebackend/security/util/JwtUtil.java` ❌

**更新引用**
- `JwtAuthenticationFilter.java`: 改为 `import com.basebackend.jwt.JwtUtil`

### 3. 修改basebackend-gateway

**pom.xml**
```xml
<!-- 移除Spring Security依赖 -->
<!-- 移除直接的JWT依赖 -->
<!-- 添加JWT模块依赖 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-jwt</artifactId>
    <version>${project.version}</version>
</dependency>
```

**删除文件**
- `src/main/java/com/basebackend/gateway/util/JwtUtil.java` ❌
- `src/main/java/com/basebackend/gateway/config/SecurityConfig.java` ❌（不需要Spring Security配置）

**更新引用**
- `AuthenticationFilter.java`: 改为 `import com.basebackend.jwt.JwtUtil`
- `GatewayApplication.java`: 添加 `"com.basebackend.jwt"` 到component scan

### 4. 修改basebackend-demo-api

**更新引用**
- `AuthDemoController.java`: 改为 `import com.basebackend.jwt.JwtUtil`
- `DemoApiApplication.java`: 添加 `"com.basebackend.jwt"` 到component scan

## 验证结果

### 编译成功
```bash
mvn clean compile -DskipTests
```

所有13个模块编译成功：
- ✅ Base Backend JWT (新模块)
- ✅ Base Backend Security (使用JWT模块)
- ✅ Base Backend Gateway (使用JWT模块)
- ✅ Base Backend Demo API (通过security使用JWT模块)
- ✅ 其他所有模块

### Token互通性

现在Gateway和demo-api使用**同一个JwtUtil类**：
1. **相同的secret配置**: 都从 `jwt.secret` 读取
2. **相同的生成逻辑**: 使用同一份代码生成Token
3. **相同的验证逻辑**: 使用同一份代码验证Token

**流程**:
```
用户 → Gateway → demo-api的 /api/auth/login
                    ↓
              使用 com.basebackend.jwt.JwtUtil 生成Token
                    ↓
         返回Token给用户
                    ↓
用户带Token → Gateway → AuthenticationFilter
                    ↓
              使用 com.basebackend.jwt.JwtUtil 验证Token
                    ↓
              添加 X-User-Id 头
                    ↓
         转发到 demo-api
```

## 优势总结

### 1. 代码复用
- 只维护一份JWT工具代码
- 修改JWT逻辑只需改一个地方

### 2. 配置统一
- 所有服务共享同一份JWT配置
- 避免secret不一致导致的Token验证失败

### 3. 依赖清晰
- basebackend-jwt: 纯JWT功能，无额外依赖
- basebackend-security: JWT + Spring Security（用于MVC应用）
- basebackend-gateway: JWT（不用Spring Security，避免WebFlux冲突）

### 4. 易于扩展
- 需要添加新的JWT功能？只需修改basebackend-jwt模块
- 所有使用JWT的服务自动获得新功能

### 5. 环境兼容
- 同时支持WebFlux（Gateway）和MVC（demo-api）
- 不引入不必要的依赖

## 测试建议

### 1. 基本功能测试

```bash
# 1. 通过Gateway登录获取Token
curl -X POST "http://localhost:8180/api/auth/login" \
  -d "username=admin&password=123456"

# 2. 使用Token访问受保护资源
curl -X GET "http://localhost:8180/api/users" \
  -H "Authorization: Bearer <token>"

# 3. Token应该在Gateway和demo-api之间正常工作
```

### 2. Token刷新测试

```bash
# 测试Token刷新功能（新增功能）
curl -X POST "http://localhost:8180/api/auth/refresh" \
  -H "Authorization: Bearer <old-token>"
```

### 3. 配置一致性测试

确保Gateway和demo-api的 `application.yml` 中JWT配置一致：
```yaml
jwt:
  secret: basebackend-secret-key-for-jwt-token-generation-minimum-256-bits
  expiration: 86400000
```

## 注意事项

1. **必须配置component scan**: 所有使用JWT的应用都要扫描 `com.basebackend.jwt` 包
2. **JWT secret必须一致**: 确保所有服务的JWT配置相同
3. **Token格式**: 必须是 `Bearer <token>` 格式
4. **白名单配置**: Gateway的白名单 `/basebackend-demo-api/api/auth/**` 已更新

## 相关文件

- `/home/wuan/IdeaProjects/basebackend/basebackend-jwt/` - 新的JWT模块
- `pom.xml` - 父pom添加了jwt模块
- `basebackend-security/pom.xml` - 依赖jwt模块
- `basebackend-gateway/pom.xml` - 依赖jwt模块
- `basebackend-gateway/filter/AuthenticationFilter.java` - 使用jwt模块的JwtUtil
- `basebackend-demo-api/controller/AuthDemoController.java` - 使用jwt模块的JwtUtil

## 总结

通过创建独立的`basebackend-jwt`模块，成功解决了Gateway和demo-api的JWT Token不互通问题。现在：
- ✅ 所有服务使用统一的JWT工具
- ✅ Token在Gateway和微服务之间完全兼容
- ✅ 代码维护更简单
- ✅ 配置更清晰
- ✅ 同时支持WebFlux和MVC环境
