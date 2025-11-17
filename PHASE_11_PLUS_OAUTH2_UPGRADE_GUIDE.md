# Phase 11+: OAuth2.0认证升级实施指南

## 📋 概述

本文档详细描述了BaseBackend项目从传统JWT认证升级到OAuth2.0/OpenID Connect标准的过程。OAuth2.0提供了更完善的认证授权框架，支持多种认证模式、客户端类型和标准化的令牌管理。

---

## 🎯 升级目标

### 核心目标
1. ✅ 引入OAuth2.0授权服务器 (Spring Authorization Server)
2. ✅ 支持多种认证模式（授权码、密码、客户端模式）
3. ✅ 集成OpenID Connect (OIDC) 标准
4. ✅ 实现JWT令牌管理和验证
5. ✅ 提供标准化的用户信息端点
6. ✅ 支持多客户端类型（Web、移动、微服务）

### 技术栈
- **授权服务器**: Spring Security OAuth2 Authorization Server 1.2.3
- **资源服务器**: Spring Security OAuth2 Resource Server
- **JWT库**: jjwt
- **数据库**: MySQL (OAuth2.0表)
- **注册中心**: Nacos 2.2.3

---

## 🏗️ 架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    OAuth2.0 授权服务器 (8082)                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Authorization Server                                 │  │
│  │  • 授权码模式 (Authorization Code)                     │  │
│  │  • 密码模式 (Password)                                │  │
│  │  • 客户端模式 (Client Credentials)                     │  │
│  │  • 刷新令牌 (Refresh Token)                            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
┌───────▼──────┐ ┌────▼────┐ ┌─────▼────┐
│  Web应用     │ │ 移动应用 │ │ 微服务   │
│ 客户端       │ │ 客户端   │ │ 客户端   │
│              │ │          │ │          │
│ basebackend  │ │basebackend│ │basebackend│
│ -web         │ │-mobile   │ │-service  │
└──────────────┘ └──────────┘ └──────────┘
```

### OAuth2.0流程

#### 1. 授权码模式 (Authorization Code Flow)
```
用户 -> Web应用 -> 授权服务器 -> 用户授权 -> 授权码 -> 访问令牌
```

#### 2. 密码模式 (Password Flow)
```
用户 -> 移动应用 -> 授权服务器 (直接传用户名密码) -> 访问令牌
```

#### 3. 客户端模式 (Client Credentials Flow)
```
微服务 -> 授权服务器 (客户端认证) -> 访问令牌
```

---

## 📦 模块结构

### basebackend-oauth2 模块
```
basebackend-oauth2/
├── src/main/java/com/basebackend/oauth2/
│   ├── config/
│   │   ├── AuthorizationServerConfig.java      # 授权服务器配置
│   │   └── ResourceServerConfig.java           # 资源服务器配置
│   ├── provider/
│   │   └── OAuth2UserDetailsService.java       # 用户详情服务
│   ├── user/
│   │   └── OAuth2UserDetails.java              # OAuth2.0用户详情
│   ├── controller/
│   │   └── UserInfoController.java             # 用户信息端点
│   └── OAuth2Application.java                  # 启动类
├── src/main/resources/
│   ├── application.yml                         # 应用配置
│   └── db/migration/
│       └── V1__Create_oauth2_tables.sql        # 数据库初始化
└── scripts/
    └── start-oauth2-service.sh                 # 启动脚本
```

---

## 🔧 详细配置

### 1. 授权服务器配置

#### 客户端配置

**Web应用客户端**:
```yaml
Client ID: basebackend-web
Client Secret: web-client-secret
认证方式: Client Authentication Method (Basic)
授权模式:
  - Authorization Code
  - Refresh Token
  - Password
重定向URI: http://localhost:8080/login/oauth2/code/basebackend-web
作用域: openid, profile, email, read, write, user_info
```

**移动应用客户端**:
```yaml
Client ID: basebackend-mobile
Client Secret: mobile-client-secret
认证方式: Client Authentication Method (Post)
授权模式:
  - Authorization Code
  - Refresh Token
  - Password
重定向URI: myapp://oauth2/callback
作用域: openid, profile, email, read, write
```

**微服务客户端**:
```yaml
Client ID: basebackend-service
Client Secret: service-client-secret
认证方式: Client Authentication Method (Basic)
授权模式:
  - Client Credentials
作用域: service
```

#### JWT配置
```java
@Bean
public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = generateRsaKey();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
}

@Bean
public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
            .issuer("http://localhost:8082")
            .authorizationEndpoint("/oauth2/authorize")
            .tokenEndpoint("/oauth2/token")
            .tokenIntrospectionEndpoint("/oauth2/introspect")
            .tokenRevocationEndpoint("/oauth2/revoke")
            .jwkSetEndpoint("/oauth2/jwks")
            .oidcUserInfoEndpoint("/userinfo")
            .build();
}
```

### 2. 数据库表结构

#### oauth2_registered_client (客户端表)
```sql
CREATE TABLE oauth2_registered_client (
    id varchar(100) NOT NULL PRIMARY KEY,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP,
    client_secret varchar(200),
    client_name varchar(200),
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000),
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL
);
```

#### oauth2_authorization (授权表)
```sql
CREATE TABLE oauth2_authorization (
    id varchar(100) NOT NULL PRIMARY KEY,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000),
    attributes blob,
    state varchar(500),
    authorization_code_value blob,
    authorization_code_issued_at timestamp,
    authorization_code_expires_at timestamp,
    access_token_value blob,
    access_token_issued_at timestamp,
    access_token_expires_at timestamp,
    oidc_id_token_value blob,
    oidc_id_token_issued_at timestamp,
    oidc_id_token_expires_at timestamp,
    refresh_token_value blob,
    refresh_token_issued_at timestamp,
    refresh_token_expires_at timestamp
);
```

#### oauth2_authorization_consent (授权同意表)
```sql
CREATE TABLE oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);
```

---

## 📝 使用指南

### 1. 启动OAuth2.0授权服务器

```bash
cd /opt/basebackend/basebackend-oauth2
chmod +x scripts/start-oauth2-service.sh
./scripts/start-oauth2-service.sh
```

验证启动:
```bash
curl http://localhost:8082/actuator/health
```

### 2. 客户端注册

#### 手动注册新客户端
```java
@Bean
public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
    JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

    RegisteredClient newClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("your-client-id")
            .clientSecret(passwordEncoder.encode("your-client-secret"))
            .clientName("Your Client Name")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/callback")
            .scope(OidcScopes.OPENID)
            .scope("read")
            .scope("write")
            .build();

    repository.save(newClient);
    return repository;
}
```

### 3. 获取访问令牌

#### 授权码模式
```bash
# 1. 浏览器访问授权端点
http://localhost:8082/oauth2/authorize?
    response_type=code&
    client_id=basebackend-web&
    redirect_uri=http://localhost:8080/login/oauth2/code/basebackend-web&
    scope=openid profile email read write&
    state=xyz

# 2. 获取访问令牌
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-web:web-client-secret \
  -d "grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=http://localhost:8080/login/oauth2/code/basebackend-web"
```

#### 密码模式
```bash
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-mobile:mobile-client-secret \
  -d "grant_type=password&username=admin&password=123456&scope=openid profile email read write"
```

#### 客户端模式
```bash
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-service:service-client-secret \
  -d "grant_type=client_credentials&scope=service"
```

#### 刷新令牌
```bash
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-web:web-client-secret \
  -d "grant_type=refresh_token&refresh_token=YOUR_REFRESH_TOKEN"
```

### 4. 访问用户信息
```bash
curl http://localhost:8082/oauth2/userinfo \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

响应示例:
```json
{
  "sub": "123456",
  "userId": 1,
  "username": "admin",
  "nickname": "管理员",
  "email": "admin@basebackend.com",
  "roles": ["ADMIN", "USER"],
  "permissions": ["user:read", "user:write"],
  "deptId": 1,
  "deptName": "技术部"
}
```

### 5. 令牌验证

#### 验证访问令牌
```bash
curl http://localhost:8082/oauth2/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-service:service-client-secret \
  -d "token=YOUR_ACCESS_TOKEN"
```

#### 获取JWK集
```bash
curl http://localhost:8082/oauth2/jwks
```

---

## 🔐 安全特性

### 1. 令牌安全
- **JWT签名**: 使用RSA-2048算法签名
- **密钥轮换**: 支持JWK集密钥自动轮换
- **令牌过期**: 访问令牌2小时，刷新令牌24小时
- **令牌撤销**: 支持令牌主动撤销

### 2. 客户端安全
- **客户端认证**: 支持Basic和Post两种认证方式
- **重定向URI验证**: 严格验证重定向URI
- **作用域控制**: 细粒度权限控制

### 3. 用户认证
- **密码加密**: 使用BCrypt加密
- **账户状态**: 支持账户禁用和锁定
- **多因子认证**: 支持扩展多因子认证

---

## 🚀 性能优化

### 1. JWT优化
```java
// 自定义JWT令牌
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
    return context -> {
        // 添加自定义声明
        context.getClaims()
            .claim("userId", userDetails.getUserId())
            .claim("username", userDetails.getUsername())
            .claim("roles", userDetails.getRoles())
            .claim("permissions", userDetails.getPermissions());
    };
}
```

### 2. 缓存配置
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

### 3. 数据库优化
```sql
-- 创建索引
CREATE INDEX idx_oauth2_registered_client_client_id ON oauth2_registered_client(client_id);
CREATE INDEX idx_oauth2_authorization_principal ON oauth2_authorization(principal_name);
CREATE INDEX idx_oauth2_authorization_client ON oauth2_authorization(registered_client_id);
```

---

## 🧪 测试验证

### 1. 功能测试

#### 测试授权码模式
```bash
# 使用浏览器访问
http://localhost:8082/oauth2/authorize?
    response_type=code&
    client_id=basebackend-web&
    redirect_uri=http://localhost:8080/login/oauth2/code/basebackend-web&
    scope=openid profile email&
    state=test123

# 获取令牌
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-web:web-client-secret \
  -d "grant_type=authorization_code&code=CODE&redirect_uri=http://localhost:8080/login/oauth2/code/basebackend-web"
```

#### 测试密码模式
```bash
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-mobile:mobile-client-secret \
  -d "grant_type=password&username=admin&password=123456&scope=openid"
```

#### 测试客户端模式
```bash
curl -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u basebackend-service:service-client-secret \
  -d "grant_type=client_credentials&scope=service"
```

### 2. 性能测试
```bash
# 令牌生成性能测试
for i in {1..100}; do
    curl -s -X POST http://localhost:8082/oauth2/token \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -u basebackend-service:service-client-secret \
      -d "grant_type=client_credentials&scope=service" > /dev/null &
done
wait

# 令牌验证性能测试
ACCESS_TOKEN="YOUR_ACCESS_TOKEN"
for i in {1..100}; do
    curl -s http://localhost:8082/oauth2/introspect \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -u basebackend-service:service-client-secret \
      -d "token=$ACCESS_TOKEN" > /dev/null &
done
wait
```

---

## 📊 监控指标

### 1. Actuator端点
```
/actuator/oauth2-authorization-server
/actuator/health
/actuator/metrics
/actuator/prometheus
```

### 2. 关键指标
- `oauth2.authorizations.count`: 授权数量
- `oauth2.tokens.issued`: 令牌签发数量
- `oauth2.clients.registered`: 已注册客户端数量
- `oauth2.jwt.signature.verify`: JWT签名验证次数

### 3. 自定义指标
```java
@Component
public class OAuth2Metrics {

    private final MeterRegistry meterRegistry;
    private final Counter tokenIssuedCounter;
    private final Timer tokenValidationTimer;

    public OAuth2Metrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.tokenIssuedCounter = Counter.builder("oauth2.tokens.issued")
                .description("Number of tokens issued")
                .register(meterRegistry);
        this.tokenValidationTimer = Timer.builder("oauth2.tokens.validation")
                .description("Token validation time")
                .register(meterRegistry);
    }

    public void recordTokenIssued(String grantType) {
        tokenIssuedCounter.increment(Tags.of("grant_type", grantType));
    }
}
```

---

## 🔄 从JWT迁移

### 1. 现有服务升级步骤

#### Step 1: 更新依赖
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-resource-server</artifactId>
</dependency>
```

#### Step 2: 配置资源服务器
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt());
        return http.build();
    }
}
```

#### Step 3: 更新配置
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8082
          jwk-set-uri: http://localhost:8082/oauth2/jwks
```

### 2. 代码更新

#### 旧版本 (JWT)
```java
// 从请求头获取JWT
String token = request.getHeader("Authorization").replace("Bearer ", "");
Claims claims = Jwts.parser()
    .setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
    .parseClaimsJws(token)
    .getBody();

String userId = claims.get("userId", String.class);
```

#### 新版本 (OAuth2.0)
```java
// 从SecurityContext获取用户信息
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
String userId = jwtToken.getToken().getClaimAsString("userId");
```

### 3. 测试迁移

#### 使用Postman测试
1. 导入OAuth2.0配置
2. 配置认证模式: OAuth 2.0
3. 授权服务: `http://localhost:8082/oauth2/authorize`
4. 令牌服务: `http://localhost:8082/oauth2/token`
5. 客户端ID: `basebackend-web`
6. 客户端密钥: `web-client-secret`
7. 作用域: `openid profile email read write`

---

## 📚 最佳实践

### 1. 客户端设计
- **最小权限**: 只申请必要的权限
- **安全存储**: 安全存储客户端密钥
- **定期轮换**: 定期轮换客户端密钥

### 2. 令牌管理
- **短期令牌**: 使用短期访问令牌
- **刷新令牌**: 使用长期刷新令牌
- **令牌撤销**: 实现令牌撤销机制

### 3. 安全配置
- **HTTPS**: 生产环境必须使用HTTPS
- **CORS**: 配置正确的CORS策略
- **CSRF**: 防止CSRF攻击

### 4. 错误处理
```java
@RestControllerAdvice
public class OAuth2ExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "invalid_client", "error_description", "客户端认证失败"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "insufficient_scope", "error_description", "权限不足"));
    }
}
```

---

## 🔧 故障排除

### 1. 常见错误

#### invalid_client
```json
{
  "error": "invalid_client",
  "error_description": "客户端ID或密钥错误"
}
```
**解决**: 检查客户端ID和密钥是否正确

#### invalid_grant
```json
{
  "error": "invalid_grant",
  "error_description": "授权码或刷新令牌无效或已过期"
}
```
**解决**: 检查令牌是否过期或已使用

#### insufficient_scope
```json
{
  "error": "insufficient_scope",
  "error_description": "权限不足"
}
```
**解决**: 检查客户端是否申请了足够的权限

### 2. 调试日志
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
    com.basebackend.oauth2: DEBUG
```

### 3. 健康检查
```bash
# 检查服务状态
curl http://localhost:8082/actuator/health

# 检查授权服务器配置
curl http://localhost:8082/actuator/oauth2-authorization-server

# 检查JWK集
curl http://localhost:8082/oauth2/jwks
```

---

## 📞 技术支持

### 联系方式
- **技术支持邮箱**: support@basebackend.com
- **技术文档**: https://docs.basebackend.com/oauth2
- **GitHub**: https://github.com/basebackend/oauth2-upgrade

### 参考资料
- [OAuth2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)
- [Spring Security OAuth2](https://docs.spring.io/spring-authorization-server/)
- [JWT Handbook](https://auth0.com/learn/json-web-tokens/)

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
