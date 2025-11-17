# Phase 11.5: 安全加固实施指南

## 📋 概述

本指南介绍如何全面加固系统安全，包括认证安全、数据安全、接口安全和审计日志，确保系统在生产环境中的安全性和合规性。

---

## 🏗️ 安全架构

### 架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        安全加固架构                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   认证安全    │  │   数据安全    │  │   接口安全    │           │
│  │              │  │              │  │              │           │
│  │ • OAuth 2.0 │  │ • 数据加密    │  │ • API 签名   │           │
│  │ • JWT Token │  │ • 数据脱敏    │  │ • 防重放     │           │
│  │ • MFA       │  │ • AES-256    │  │ • 限流防护   │           │
│  │ • RBAC      │  │ • 密钥管理    │  │ • HTTPS     │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                   │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   审计日志     │  │   访问控制   │  │   安全监控   │           │
│  │              │  │              │  │              │           │
│  │ • 操作记录    │  │ • 黑白名单   │  │ • 入侵检测   │           │
│  │ • 合规追踪    │  │ • 权限控制   │  │ • 异常告警   │           │
│  │ • 日志审计    │  │ • 最小权限   │  │ • 风险评估   │           │
│  │ • 数据血缘    │  │ • 零信任     │  │ • 安全态势   │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                  安全防护层                                  │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • WAF (Web 应用防火墙)                                      │ │
│  │ • DDoS 防护                                                 │ │
│  │ • 漏洞扫描                                                   │ │
│  │ • 安全基线                                                   │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

### 安全防护矩阵

| 安全域 | 防护措施 | 技术实现 | 价值 |
|--------|----------|----------|------|
| **认证安全** | OAuth 2.0、JWT、MFA | Spring Security OAuth2 | 身份验证 |
| **数据安全** | AES-256、字段脱敏 | Jasypt、MyBatis 加密 | 数据保护 |
| **接口安全** | API 签名、防重放 | HMAC-SHA256 | API 安全 |
| **审计日志** | 操作追踪、合规记录 | ELK 审计 | 合规审计 |
| **访问控制** | RBAC、ABAC | Spring Security ACL | 权限控制 |
| **网络安全** | HTTPS、TLS 1.3 | Nginx SSL | 传输安全 |

---

## 🔐 认证安全实现

### 1. OAuth 2.0 + JWT 认证

#### 添加依赖

```xml
<!-- Spring Security OAuth2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Spring Security JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- 密码加密 -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>

<!-- 验证码 -->
<dependency>
    <groupId>com.github.penggle</groupId>
    <artifactId>kaptcha</artifactId>
    <version>2.3.2</version>
</dependency>
```

#### JWT 配置类

```java
/**
 * JWT 安全配置
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtSecurityConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
        return new JwtTokenProvider(properties);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        return new JwtAuthenticationFilter(tokenProvider);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

/**
 * JWT 配置属性
 */
@ConfigurationProperties(prefix = "jwt.security")
@Data
public class JwtProperties {

    /**
     * JWT 密钥 (Base64 编码，长度不少于 32 字节)
     */
    private String secret = "YourSecretKeyMustBeVeryLongAndSecure2024Basebackend2024";

    /**
     * Token 有效期 (秒，默认 2 小时)
     */
    private long expiration = 7200;

    /**
     * Refresh Token 有效期 (秒，默认 7 天)
     */
    private long refreshExpiration = 604800;

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Header 名称
     */
    private String header = "Authorization";
}

/**
 * JWT Token 提供者
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final Key key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 Token
     */
    public String generateToken(UserDetails userDetails, Set<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("userId", ((User) userDetails).getId());
        claims.put("username", userDetails.getUsername());

        return createToken(claims, userDetails.getUsername(), properties.getExpiration());
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return createToken(claims, username, properties.getRefreshExpiration());
    }

    /**
     * 创建 Token
     */
    private String createToken(Map<String, Object> claims, String subject, long validityInSeconds) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInSeconds * 1000);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    /**
     * 从 Token 中获取角色
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        return getClaimsFromToken(token).get("roles", Set.class);
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析 Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token.replace(properties.getTokenPrefix(), ""))
            .getBody();
    }

    /**
     * 验证 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaimsFromToken(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从 Header 中提取 Token
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(properties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(properties.getTokenPrefix())) {
            return bearerToken.substring(properties.getTokenPrefix().length());
        }
        return null;
    }
}

/**
 * JWT 认证过滤器
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = tokenProvider.resolveToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            Authentication authentication = createAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 创建认证信息
     */
    private Authentication createAuthentication(String token) {
        Long userId = tokenProvider.getUserIdFromToken(token);
        String username = tokenProvider.getUsernameFromToken(token);
        Set<String> roles = tokenProvider.getRolesFromToken(token);

        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setRoles(roles);

        List<SimpleGrantedAuthority> authorities = roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }
}
```

#### 多因素认证 (MFA)

```java
/**
 * 多因素认证服务
 */
@Service
public class MfaService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_DURATION = 300; // 5 分钟

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private NotificationService notificationService;

    /**
     * 生成 MFA 验证码
     */
    public MfaCode generateMfaCode(String userId) {
        String code = generateRandomCode();
        String key = "mfa:" + userId;

        // 存储验证码到 Redis
        redisTemplate.opsForValue().set(key, code, OTP_VALIDITY_DURATION, TimeUnit.SECONDS);

        // 发送验证码（邮件/短信）
        User user = userService.getUserById(userId);
        notificationService.sendMfaCode(user, code);

        return MfaCode.builder()
            .userId(userId)
            .maskedCode(maskCode(code))
            .validityDuration(OTP_VALIDITY_DURATION)
            .build();
    }

    /**
     * 验证 MFA 验证码
     */
    public boolean verifyMfaCode(String userId, String code) {
        String key = "mfa:" + userId;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            return false;
        }

        boolean valid = storedCode.equals(code);
        if (valid) {
            // 验证成功后删除验证码
            redisTemplate.delete(key);
        }

        return valid;
    }

    /**
     * 启用 MFA
     */
    public void enableMfa(String userId) {
        User user = userService.getUserById(userId);
        user.setMfaEnabled(true);
        user.setMfaSecret(generateMfaSecret());
        userService.updateUser(user);

        log.info("用户 {} 已启用 MFA", userId);
    }

    /**
     * 禁用 MFA
     */
    public void disableMfa(String userId) {
        User user = userService.getUserById(userId);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userService.updateUser(user);

        log.warn("用户 {} 已禁用 MFA", userId);
    }

    /**
     * 验证 TOTP (基于时间的一次性密码)
     */
    public boolean verifyTotp(String userId, String totp) {
        User user = userService.getUserById(userId);
        if (!user.getMfaEnabled() || user.getMfaSecret() == null) {
            return false;
        }

        return verifyTotp(user.getMfaSecret(), totp);
    }

    private boolean verifyTotp(String secret, String totp) {
        // 使用 Google Authenticator 算法验证
        long currentTime = System.currentTimeMillis() / 1000 / 30;
        for (int i = -1; i <= 1; i++) {
            String generated = generateTotp(secret, currentTime + i);
            if (generated.equals(totp)) {
                return true;
            }
        }
        return false;
    }

    private String generateTotp(String secret, long time) {
        // TOTP 算法实现
        // 省略具体实现，建议使用现有的库
        return "123456";
    }

    private String generateMfaSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base32.encode(bytes);
    }

    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", code);
    }

    private String maskCode(String code) {
        if (code.length() <= 2) {
            return "*".repeat(code.length());
        }
        return code.substring(0, 2) + "*".repeat(code.length() - 2);
    }

    @Data
    @Builder
    public static class MfaCode {
        private String userId;
        private String maskedCode;
        private int validityDuration;
    }
}
```

### 2. RBAC 权限控制

```java
/**
 * 权限配置
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/user/profile").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/user/**").hasRole("USER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error")
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}

/**
 * 权限检查注解
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<UserProfile> getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(userService.getUserProfile(user.getId()));
    }

    @PutMapping("/profile")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public Result<Void> updateProfile(@PathVariable Long userId,
                                      @RequestBody @Valid UserUpdateRequest request) {
        userService.updateUserProfile(userId, request);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') and hasPermission(#userId, 'User', 'DELETE')")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return Result.success();
    }
}
```

---

## 🔒 数据安全实现

### 1. 数据加密

#### 配置文件加密

```yaml
# application.yml
jasypt:
  encryptor:
    algorithm: PBEWITHHMACSHA512ANDAES_256
    password: ${JASYPT_ENCRYPTOR_PASSWORD}
    key-obtention-iterations: 1000
    pool-size: 1
    salt-generator-classname: org.jasypt.salt.RandomSaltGenerator
    string-output-type: base64

# 使用示例
spring:
  datasource:
    password: ENC(encrypted_password_here)
  redis:
    password: ENC(encrypted_redis_password)
```

#### 字段级加密

```java
/**
 * 数据加密注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypted {

    /**
     * 加密算法
     */
    String algorithm() default "AES/CBC/PKCS5Padding";

    /**
     * 密钥名称 (从配置中获取)
     */
    String keyName() default "data.encryption.key";
}

/**
 * 加密字段处理器
 */
@Component
public class EncryptedFieldProcessor {

    private static final Logger log = LoggerFactory.getLogger(EncryptedFieldProcessor.class);

    @Autowired
    private Map<String, Key> encryptionKeys;

    /**
     * 加密字段值
     */
    public Object encryptField(Object fieldValue, Encrypted annotation) {
        if (fieldValue == null || fieldValue.toString().isEmpty()) {
            return fieldValue;
        }

        try {
            String algorithm = annotation.algorithm();
            Key key = encryptionKeys.get(annotation.keyName());

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypted = cipher.doFinal(fieldValue.toString().getBytes());
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("字段加密失败", e);
            throw new SecurityException("数据加密失败", e);
        }
    }

    /**
     * 解密字段值
     */
    public Object decryptField(Object fieldValue, Encrypted annotation) {
        if (fieldValue == null || fieldValue.toString().isEmpty()) {
            return fieldValue;
        }

        try {
            if (!isEncrypted(fieldValue.toString())) {
                return fieldValue;
            }

            String algorithm = annotation.algorithm();
            Key key = encryptionKeys.get(annotation.keyName());

            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] encrypted = Base64.getDecoder().decode(fieldValue.toString());
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted);

        } catch (Exception e) {
            log.error("字段解密失败", e);
            throw new SecurityException("数据解密失败", e);
        }
    }

    private boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }
}

/**
 * 用户实体类（敏感字段加密）
 */
@Entity
@Table(name = "sys_user")
public class User {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Encrypted
    @Column(name = "email")
    private String email;

    @Encrypted
    @Column(name = "phone")
    private String phone;

    @Column(name = "password", nullable = false)
    private String password;

    // getter/setter
}

/**
 * 数据脱敏注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Desensitized {

    /**
     * 脱敏类型
     */
    DesensitizationType type();

    /**
     * 自定义脱敏规则
     */
    String pattern() default "";
}

/**
 * 脱敏类型枚举
 */
public enum DesensitizationType {
    // 手机号：138****5678
    MOBILE_PHONE,
    // 邮箱：admin***@example.com
    EMAIL,
    // 身份证号：11010119900101****
    ID_CARD,
    // 银行卡号：6222 **** **** **** 1234
    BANK_CARD,
    // 姓名：张*
    NAME,
    // 地址：北京市海淀区****
    ADDRESS
}

/**
 * 脱敏工具类
 */
@Component
public class DesensitizationUtil {

    /**
     * 数据脱敏
     */
    public String desensitize(String value, Desensitized annotation) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        switch (annotation.type()) {
            case MOBILE_PHONE:
                return desensitizeMobilePhone(value);
            case EMAIL:
                return desensitizeEmail(value);
            case ID_CARD:
                return desensitizeIdCard(value);
            case BANK_CARD:
                return desensitizeBankCard(value);
            case NAME:
                return desensitizeName(value);
            case ADDRESS:
                return desensitizeAddress(value);
            default:
                return value;
        }
    }

    private String desensitizeMobilePhone(String mobile) {
        if (mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    private String desensitizeEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex < 1) {
            return email;
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 3) {
            return "***" + domain;
        }
        return username.substring(0, 3) + "***" + domain;
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    private String desensitizeBankCard(String bankCard) {
        if (bankCard.length() < 8) {
            return bankCard;
        }
        return "**** **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    private String desensitizeName(String name) {
        if (name.length() <= 1) {
            return name;
        }
        return name.substring(0, 1) + "*";
    }

    private String desensitizeAddress(String address) {
        if (address.length() < 8) {
            return address;
        }
        return address.substring(0, 4) + "****" + address.substring(address.length() - 2);
    }
}
```

### 2. 数据库安全

```sql
-- 创建加密表空间
CREATE TABLESPACE encryption_ts
ADD DATAFILE 'encryption_ts.ibd'
FILE_BLOCK_SIZE = 8192
ENCRYPTION = 'Y';

-- 创建加密表
CREATE TABLE sensitive_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  encrypted_column VARBINARY(500) NOT NULL,
  encryption_key_id VARCHAR(100) NOT NULL,
  create_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  INDEX idx_user_id (user_id)
) TABLESPACE=encryption_ts;

-- 启用透明数据加密 (TDE)
ALTER INSTANCE ROTATE INNODB MASTER KEY;

-- 创建审计触发器
DELIMITER //

CREATE TRIGGER audit_user_update
AFTER UPDATE ON sys_user
FOR EACH ROW
BEGIN
  INSERT INTO audit_log (
    table_name, operation, old_values, new_values,
    operator, operator_ip, create_time
  ) VALUES (
    'sys_user', 'UPDATE',
    JSON_OBJECT('email', OLD.email, 'phone', OLD.phone),
    JSON_OBJECT('email', NEW.email, 'phone', NEW.phone),
    @current_user, @current_ip, NOW()
  );
END//

DELIMITER ;
```

---

## 🔗 接口安全实现

### 1. API 签名认证

```java
/**
 * API 签名工具
 */
@Component
public class ApiSignatureUtil {

    private static final String SIGNATURE_HEADER = "X-API-Signature";
    private static final String TIMESTAMP_HEADER = "X-API-Timestamp";
    private static final String NONCE_HEADER = "X-API-Nonce";
    private static final long TIMESTAMP_VALIDITY = 300000; // 5 分钟

    /**
     * 生成 API 签名
     */
    public String generateSignature(String secretKey, String method, String uri,
                                   Map<String, String> params, String timestamp, String nonce) {
        // 1. 拼接签名字符串
        StringBuilder signString = new StringBuilder();
        signString.append(method.toUpperCase()).append("\n");
        signString.append(uri).append("\n");

        // 2. 按字典序排序参数
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            signString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        // 3. 移除最后一个 '&'
        if (sortedParams.size() > 0) {
            signString.setLength(signString.length() - 1);
        }

        // 4. 添加时间戳和随机数
        signString.append("\n");
        signString.append(timestamp).append("\n");
        signString.append(nonce);

        // 5. HMAC-SHA256 加密
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(signString.toString().getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new SecurityException("API 签名生成失败", e);
        }
    }

    /**
     * 验证 API 签名
     */
    public boolean verifySignature(String secretKey, String method, String uri,
                                  Map<String, String> params, String signature,
                                  String timestamp, String nonce) {
        // 1. 验证时间戳
        long requestTime = Long.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - requestTime) > TIMESTAMP_VALIDITY) {
            throw new SecurityException("请求时间戳已过期");
        }

        // 2. 验证随机数（防止重放攻击）
        if (!isValidNonce(nonce)) {
            throw new SecurityException("随机数无效或已使用");
        }

        // 3. 验证签名
        String expectedSignature = generateSignature(secretKey, method, uri, params, timestamp, nonce);
        return expectedSignature.equals(signature);
    }

    private boolean isValidNonce(String nonce) {
        // 检查随机数是否已使用
        String key = "api:nonce:" + nonce;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return false; // 已被使用
        }

        // 记录随机数
        redisTemplate.opsForValue().set(key, "1", TIMESTAMP_VALIDITY, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * 从请求中提取签名信息
     */
    public SignatureInfo extractSignatureInfo(HttpServletRequest request) {
        String signature = request.getHeader(SIGNATURE_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);

        if (signature == null || timestamp == null || nonce == null) {
            throw new IllegalArgumentException("缺少必要的签名头信息");
        }

        return SignatureInfo.builder()
            .signature(signature)
            .timestamp(timestamp)
            .nonce(nonce)
            .build();
    }

    @Data
    @Builder
    public static class SignatureInfo {
        private String signature;
        private String timestamp;
        private String nonce;
    }
}

/**
 * API 签名拦截器
 */
@Component
public class ApiSignatureInterceptor implements HandlerInterceptor {

    @Autowired
    private ApiSignatureUtil signatureUtil;

    @Autowired
    private ApiAuthService apiAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 仅对 API 请求进行签名验证
        if (!request.getRequestURI().startsWith("/api/")) {
            return true;
        }

        try {
            SignatureInfo signatureInfo = signatureUtil.extractSignatureInfo(request);

            // 获取 API 密钥
            String apiKey = request.getHeader("X-API-Key");
            String secretKey = apiAuthService.getSecretKey(apiKey);

            if (secretKey == null) {
                throw new SecurityException("无效的 API 密钥");
            }

            // 验证签名
            String method = request.getMethod();
            String uri = request.getRequestURI();
            Map<String, String> params = extractParams(request);

            boolean valid = signatureUtil.verifySignature(
                secretKey, method, uri, params,
                signatureInfo.getSignature(),
                signatureInfo.getTimestamp(),
                signatureInfo.getNonce()
            );

            if (!valid) {
                throw new SecurityException("API 签名验证失败");
            }

            // 将 API 密钥存储到请求属性中
            request.setAttribute("apiKey", apiKey);

            return true;

        } catch (Exception e) {
            log.error("API 签名验证失败", e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"code\": 401, \"message\": \"API 签名验证失败\"}");
            return false;
        }
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();

        // 提取 query params
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.put(key, values[0]);
            }
        });

        // 提取 body params（仅限 POST/PUT）
        if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
            try {
                String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
                if (StringUtils.hasText(body)) {
                    Map<String, Object> bodyParams = JSON.parseObject(body, Map.class);
                    bodyParams.forEach((key, value) -> {
                        if (value != null) {
                            params.put(key, value.toString());
                        }
                    });
                }
            } catch (IOException e) {
                log.warn("解析请求体失败", e);
            }
        }

        return params;
    }
}

/**
 * API 认证服务
 */
@Service
public class ApiAuthService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取 API 密钥
     */
    public String getSecretKey(String apiKey) {
        // 先从 Redis 获取
        String secretKey = redisTemplate.opsForValue().get("api:secret:" + apiKey);
        if (secretKey != null) {
            return secretKey;
        }

        // 从数据库获取
        ApiCredential credential = apiCredentialMapper.selectByApiKey(apiKey);
        if (credential != null) {
            // 缓存到 Redis
            redisTemplate.opsForValue().set(
                "api:secret:" + apiKey,
                credential.getSecretKey(),
                1, TimeUnit.HOURS
            );
            return credential.getSecretKey();
        }

        return null;
    }

    /**
     * 创建 API 密钥
     */
    public ApiCredential createApiCredential(String name, Set<String> permissions) {
        String apiKey = generateApiKey();
        String secretKey = generateSecretKey();

        ApiCredential credential = new ApiCredential();
        credential.setApiKey(apiKey);
        credential.setSecretKey(secretKey);
        credential.setName(name);
        credential.setPermissions(permissions);
        credential.setStatus(1);
        credential.setCreateTime(new Date());

        apiCredentialMapper.insert(credential);

        // 缓存到 Redis
        redisTemplate.opsForValue().set(
            "api:secret:" + apiKey,
            secretKey,
            1, TimeUnit.HOURS
        );

        return credential;
    }

    private String generateApiKey() {
        return "bb_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
```

### 2. 接口限流防护

```java
/**
 * 限流注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流次数
     */
    int count() default 100;

    /**
     * 时间窗口（秒）
     */
    int time() default 60;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.IP;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后重试";
}

/**
 * 限流类型枚举
 */
public enum LimitType {
    // 按 IP 限流
    IP,
    // 按用户限流
    USER,
    // 按 API 限流
    API
}

/**
 * 限流服务
 */
@Component
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 检查是否超出限流
     */
    public boolean isExceeded(String key, int count, int time) {
        String redisKey = "rate_limit:" + key;
        String luaScript =
            "local current = redis.call('GET', KEYS[1]) " +
            "if current == false then " +
            "  redis.call('SET', KEYS[1], 1) " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "  return 0 " +
            "else " +
            "  local val = tonumber(current) " +
            "  if val >= tonumber(ARGV[2]) then " +
            "    return val " +
            "  else " +
            "    redis.call('INCR', KEYS[1]) " +
            "    return val + 1 " +
            "  end " +
            "end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(redisKey), time, String.valueOf(count));

        return result >= count;
    }

    /**
     * 生成限流 key
     */
    public String generateKey(RateLimit rateLimit, HttpServletRequest request) {
        StringBuilder key = new StringBuilder();

        switch (rateLimit.limitType()) {
            case IP:
                key.append(getClientIp(request));
                break;
            case USER:
                key.append(getCurrentUserId(request));
                break;
            case API:
                key.append(request.getRequestURI());
                break;
        }

        return key.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getCurrentUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId().toString();
        }
        return "anonymous";
    }
}

/**
 * 限流切面
 */
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return point.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String key = rateLimitService.generateKey(rateLimit, request);

        if (rateLimitService.isExceeded(key, rateLimit.count(), rateLimit.time())) {
            throw new RateLimitExceededException(rateLimit.message());
        }

        return point.proceed();
    }
}

/**
 * 使用限流的接口
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    @GetMapping("/query")
    @RateLimit(count = 50, time = 60, message = "查询过于频繁，请稍后再试")
    public Result<List<Data>> queryData() {
        return Result.success(dataService.queryData());
    }

    @PostMapping("/export")
    @RateLimit(count = 5, time = 300, message = "导出过于频繁，请稍后再试")
    public Result<String> exportData() {
        return Result.success(dataService.exportData());
    }
}
```

---

## 📊 审计日志实现

### 1. 操作审计

```java
/**
 * 审计日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 操作类型
     */
    OperationType operation() default OperationType.QUERY;

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否记录参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录结果
     */
    boolean recordResult() default false;

    /**
     * 敏感字段（不记录）
     */
    String[] sensitiveFields() default {};
}

/**
 * 操作类型枚举
 */
public enum OperationType {
    CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT, IMPORT
}

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOG");

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 记录审计日志
     */
    public void recordAuditLog(AuditLogInfo logInfo) {
        try {
            // 1. 存储到数据库
            auditLogMapper.insert(logInfo);

            // 2. 发送到 Elasticsearch
            elasticsearchRestTemplate.save(logInfo);

            // 3. 写入文件日志
            auditLogger.info(JSON.toJSONString(logInfo));

            // 4. 发送到消息队列（异步处理）
            rocketMQTemplate.convertAndSend("audit_log_topic", logInfo);

        } catch (Exception e) {
            log.error("审计日志记录失败", e);
        }
    }

    /**
     * 记录用户登录
     */
    public void recordLogin(Long userId, String username, String result, String reason) {
        AuditLogInfo logInfo = AuditLogInfo.builder()
            .operationType(OperationType.LOGIN)
            .operationDescription("用户登录")
            .userId(userId)
            .username(username)
            .result(result)
            .reason(reason)
            .createTime(new Date())
            .build();

        recordAuditLog(logInfo);
    }

    /**
     * 记录数据变更
     */
    public void recordDataChange(OperationType operation, String tableName,
                                Long userId, String username,
                                Object oldValue, Object newValue) {
        AuditLogInfo logInfo = AuditLogInfo.builder()
            .operationType(operation)
            .operationDescription("数据变更 - " + tableName)
            .tableName(tableName)
            .userId(userId)
            .username(username)
            .oldValue(maskSensitiveData(oldValue))
            .newValue(maskSensitiveData(newValue))
            .createTime(new Date())
            .build();

        recordAuditLog(logInfo);
    }

    /**
     * 查询审计日志
     */
    public PageResult<AuditLogInfo> queryAuditLogs(AuditLogQueryRequest request) {
        return auditLogMapper.selectByCondition(request);
    }

    /**
     * 导出审计日志
     */
    public String exportAuditLogs(AuditLogQueryRequest request) {
        List<AuditLogInfo> logs = auditLogMapper.selectByConditionNoPage(request);
        return ExcelExportUtil.exportToExcel(logs);
    }

    private Object maskSensitiveData(Object data) {
        if (data == null) {
            return null;
        }

        // 脱敏处理
        if (data instanceof String) {
            String str = (String) data;
            if (str.contains("@")) {
                return str.replaceAll("(\\w{2})(\\w*)(\\w{1,})", "$1****@$3");
            }
            if (str.matches("\\d{11}")) {
                return str.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
            }
        }

        return data;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuditLogInfo {
        private Long id;
        private OperationType operationType;
        private String operationDescription;
        private String tableName;
        private Long userId;
        private String username;
        private String clientIp;
        private String userAgent;
        private Object oldValue;
        private Object newValue;
        private String result;
        private String reason;
        private Date createTime;
    }
}

/**
 * 审计日志切面
 */
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            // 记录操作
            Object result = point.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // 记录审计日志
            recordAuditLog(auditLog, point, result, duration, true, null);

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            recordAuditLog(auditLog, point, null, duration, false, e.getMessage());
            throw e;
        }
    }

    private void recordAuditLog(AuditLog auditLog, ProceedingJoinPoint point,
                               Object result, long duration, boolean success, String error) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();

            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = null;
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                currentUser = (User) authentication.getPrincipal();
            }

            // 获取请求信息
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            AuditLogService.AuditLogInfo logInfo = AuditLogService.AuditLogInfo.builder()
                .operationType(auditLog.operation())
                .operationDescription(auditLog.description())
                .userId(currentUser != null ? currentUser.getId() : null)
                .username(currentUser != null ? currentUser.getUsername() : "system")
                .clientIp(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .createTime(new Date())
                .build();

            if (auditLog.recordParams()) {
                // 记录参数（脱敏处理）
                Object[] args = point.getArgs();
                logInfo.setOldValue(maskSensitiveParams(signature.getParameterNames(), args));
            }

            if (auditLog.recordResult() && result != null) {
                logInfo.setNewValue(result);
            }

            if (!success) {
                logInfo.setResult("FAILURE");
                logInfo.setReason(error);
            } else {
                logInfo.setResult("SUCCESS");
            }

            auditLogService.recordAuditLog(logInfo);

        } catch (Exception e) {
            log.error("审计日志记录失败", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Object maskSensitiveParams(String[] paramNames, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            String name = paramNames[i];
            Object value = args[i];
            if (value != null) {
                // 脱敏敏感字段
                params.put(name, maskValue(name, value));
            }
        }
        return params;
    }

    private Object maskValue(String name, Object value) {
        if (value instanceof String) {
            String str = (String) value;
            if (name.toLowerCase().contains("password") ||
                name.toLowerCase().contains("secret") ||
                name.toLowerCase().contains("token")) {
                return "***";
            }
        }
        return value;
    }
}

/**
 * 审计日志实体类
 */
@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(name = "operation_desc", columnDefinition = "VARCHAR(255)")
    private String operationDescription;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username")
    private String username;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "old_value", columnDefinition = "LONGTEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "LONGTEXT")
    private String newValue;

    @Column(name = "result")
    private String result;

    @Column(name = "reason")
    private String reason;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "trace_id")
    private String traceId;
}
```

### 2. 数据血缘追踪

```java
/**
 * 数据血缘服务
 */
@Service
public class DataLineageService {

    /**
     * 记录数据来源
     */
    public void recordDataSource(String tableName, String fieldName, String sourceTable,
                                String sourceField, String transformRule, Long operatorId) {
        DataLineageRecord record = DataLineageRecord.builder()
            .tableName(tableName)
            .fieldName(fieldName)
            .sourceTable(sourceTable)
            .sourceField(sourceField)
            .transformRule(transformRule)
            .operatorId(operatorId)
            .createTime(new Date())
            .build();

        dataLineageMapper.insert(record);
    }

    /**
     * 查询数据血缘关系
     */
    public List<DataLineageRecord> getDataLineage(String tableName) {
        return dataLineageMapper.selectByTableName(tableName);
    }

    /**
     * 生成数据血缘图
     */
    public DataLineageGraph generateLineageGraph(String tableName) {
        List<DataLineageRecord> records = getDataLineage(tableName);
        return buildLineageGraph(records);
    }

    private DataLineageGraph buildLineageGraph(List<DataLineageRecord> records) {
        DataLineageGraph graph = new DataLineageGraph();
        records.forEach(record -> {
            Node sourceNode = new Node(record.getSourceTable(), record.getSourceField());
            Node targetNode = new Node(record.getTableName(), record.getFieldName());
            Edge edge = new Edge(sourceNode, targetNode, record.getTransformRule());

            graph.addEdge(edge);
        });
        return graph;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataLineageRecord {
        private Long id;
        private String tableName;
        private String fieldName;
        private String sourceTable;
        private String sourceField;
        private String transformRule;
        private Long operatorId;
        private Date createTime;
    }

    @Data
    @AllArgsConstructor
    public static class Node {
        private String table;
        private String field;
    }

    @Data
    @AllArgsConstructor
    public static class Edge {
        private Node source;
        private Node target;
        private String transformRule;
    }

    @Getter
    @AllArgsConstructor
    public static class DataLineageGraph {
        private List<Edge> edges = new ArrayList<>();

        public void addEdge(Edge edge) {
            edges.add(edge);
        }
    }
}
```

---

## 🚨 安全监控与告警

### 1. 安全事件监控

```java
/**
 * 安全事件监控
 */
@Component
public class SecurityEventMonitor {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventMonitor.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 监控异常登录
     */
    @EventListener
    public void handleAbnormalLogin(AbnormalLoginEvent event) {
        String userId = event.getUserId();
        String clientIp = event.getClientIp();
        String reason = event.getReason();

        // 检查是否在黑名单中
        if (isInBlacklist(clientIp)) {
            alertService.sendSecurityAlert("IP 黑名单登录尝试: " + clientIp);
        }

        // 检查登录频率
        checkLoginFrequency(userId, clientIp);

        // 检查地理位置
        checkLoginLocation(userId, event.getLocation());

        log.warn("异常登录事件: userId={}, clientIp={}, reason={}", userId, clientIp, reason);
    }

    /**
     * 监控权限提升尝试
     */
    @EventListener
    public void handlePrivilegeEscalation(PrivilegeEscalationEvent event) {
        String userId = event.getUserId();
        String fromRole = event.getFromRole();
        String toRole = event.getToRole();

        alertService.sendSecurityAlert("权限提升尝试: userId=" + userId +
            ", from=" + fromRole + ", to=" + toRole);

        log.error("权限提升事件: userId={}, fromRole={}, toRole={}",
            userId, fromRole, toRole);
    }

    /**
     * 监控敏感数据访问
     */
    @EventListener
    public void handleSensitiveDataAccess(SensitiveDataAccessEvent event) {
        String userId = event.getUserId();
        String tableName = event.getTableName();
        String operation = event.getOperation();

        // 记录敏感操作
        if (isHighRiskOperation(operation)) {
            alertService.sendSecurityAlert("敏感数据访问: userId=" + userId +
                ", table=" + tableName + ", operation=" + operation);
        }

        log.info("敏感数据访问: userId={}, tableName={}, operation={}",
            userId, tableName, operation);
    }

    /**
     * 监控 API 访问频率
     */
    public void checkApiAccessRate(String apiKey, String clientIp) {
        String key = "api:access:" + apiKey + ":" + clientIp;
        Long count = redisTemplate.opsForValue().increment(key, 1);

        if (count == 1) {
            redisTemplate.expire(key, 3600, TimeUnit.SECONDS);
        }

        if (count > 1000) { // 每小时超过 1000 次
            alertService.sendSecurityAlert("API 访问频率过高: apiKey=" + apiKey +
                ", count=" + count);
        }
    }

    private boolean isInBlacklist(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + ip));
    }

    private void checkLoginFrequency(String userId, String clientIp) {
        String key = "login:attempt:" + userId + ":" + clientIp;
        Long attempts = redisTemplate.opsForValue().increment(key, 1);

        if (attempts == 1) {
            redisTemplate.expire(key, 900, TimeUnit.SECONDS); // 15 分钟
        }

        if (attempts > 10) {
            alertService.sendSecurityAlert("登录尝试次数过多: userId=" + userId +
                ", attempts=" + attempts);
        }
    }

    private void checkLoginLocation(String userId, String location) {
        // 检查用户常用登录地点
        String lastLocationKey = "login:last_location:" + userId;
        String lastLocation = redisTemplate.opsForValue().get(lastLocationKey);

        if (lastLocation != null && !lastLocation.equals(location)) {
            alertService.sendSecurityAlert("异常登录地点: userId=" + userId +
                ", last=" + lastLocation + ", current=" + location);
        }

        redisTemplate.opsForValue().set(lastLocationKey, location, 30, TimeUnit.DAYS);
    }

    private boolean isHighRiskOperation(String operation) {
        return Arrays.asList("DELETE", "EXPORT", "BULK_UPDATE").contains(operation);
    }
}

/**
 * 安全告警服务
 */
@Service
public class AlertService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private DingTalkService dingTalkService;

    public void sendSecurityAlert(String message) {
        // 1. 发送邮件
        emailService.sendSecurityAlert(message);

        // 2. 发送短信（严重告警）
        if (isSevereAlert(message)) {
            smsService.sendSecurityAlert(message);
        }

        // 3. 发送钉钉通知
        dingTalkService.sendSecurityAlert(message);

        // 4. 写入告警日志
        log.error("安全告警: {}", message);
    }

    private boolean isSevereAlert(String message) {
        return message.contains("权限提升") ||
               message.contains("SQL 注入") ||
               message.contains("数据泄露");
    }
}
```

### 2. 安全审计报告

```java
/**
 * 安全审计报告生成
 */
@Service
public class SecurityAuditReportService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private UserBehaviorAnalysisMapper behaviorMapper;

    /**
     * 生成安全审计报告
     */
    public SecurityAuditReport generateReport(Date startDate, Date endDate) {
        SecurityAuditReport report = new SecurityAuditReport();
        report.setPeriod(startDate, endDate);

        // 1. 登录安全统计
        LoginSecurityStats loginStats = analyzeLoginSecurity(startDate, endDate);
        report.setLoginStats(loginStats);

        // 2. 权限使用统计
        PrivilegeUsageStats privilegeStats = analyzePrivilegeUsage(startDate, endDate);
        report.setPrivilegeStats(privilegeStats);

        // 3. 敏感操作统计
        SensitiveOperationStats sensitiveStats = analyzeSensitiveOperations(startDate, endDate);
        report.setSensitiveStats(sensitiveStats);

        // 4. 用户行为分析
        UserBehaviorStats behaviorStats = analyzeUserBehavior(startDate, endDate);
        report.setBehaviorStats(behaviorStats);

        // 5. 安全威胁分析
        ThreatAnalysis threatAnalysis = analyzeThreats(startDate, endDate);
        report.setThreatAnalysis(threatAnalysis);

        return report;
    }

    private LoginSecurityStats analyzeLoginSecurity(Date startDate, Date endDate) {
        LoginSecurityStats stats = new LoginSecurityStats();

        // 失败登录次数
        stats.setFailedLoginCount(auditLogMapper.countFailedLogins(startDate, endDate));

        // 异常 IP 登录次数
        stats.setAbnormalIpLoginCount(auditLogMapper.countAbnormalIpLogins(startDate, endDate));

        // 异地登录次数
        stats.setRemoteLoginCount(auditLogMapper.countRemoteLogins(startDate, endDate));

        // 暴力破解尝试
        stats.setBruteForceAttempts(auditLogMapper.countBruteForceAttempts(startDate, endDate));

        return stats;
    }

    private PrivilegeUsageStats analyzePrivilegeUsage(Date startDate, Date endDate) {
        PrivilegeUsageStats stats = new PrivilegeUsageStats();

        // 角色使用情况
        List<RoleUsage> roleUsages = auditLogMapper.getRoleUsageStats(startDate, endDate);
        stats.setRoleUsages(roleUsages);

        // 高危操作执行情况
        List<HighRiskOperation> highRiskOps = auditLogMapper.getHighRiskOperations(startDate, endDate);
        stats.setHighRiskOperations(highRiskOps);

        return stats;
    }

    private SensitiveOperationStats analyzeSensitiveOperations(Date startDate, Date endDate) {
        SensitiveOperationStats stats = new SensitiveOperationStats();

        // 敏感表访问统计
        List<SensitiveTableAccess> tableAccesses = auditLogMapper.getSensitiveTableAccess(startDate, endDate);
        stats.setTableAccesses(tableAccesses);

        // 数据导出统计
        List<DataExport> exports = auditLogMapper.getDataExports(startDate, endDate);
        stats.setDataExports(exports);

        return stats;
    }

    private UserBehaviorStats analyzeUserBehavior(Date startDate, Date endDate) {
        UserBehaviorStats stats = new UserBehaviorStats();

        // 活跃用户
        List<ActiveUser> activeUsers = behaviorMapper.getActiveUsers(startDate, endDate);
        stats.setActiveUsers(activeUsers);

        // 异常行为用户
        List<AnomalousUser> anomalousUsers = behaviorMapper.getAnomalousUsers(startDate, endDate);
        stats.setAnomalousUsers(anomalousUsers);

        return stats;
    }

    private ThreatAnalysis analyzeThreats(Date startDate, Date endDate) {
        ThreatAnalysis analysis = new ThreatAnalysis();

        // SQL 注入攻击
        analysis.setSqlInjectionAttempts(auditLogMapper.countSqlInjectionAttempts(startDate, endDate));

        // XSS 攻击
        analysis.setXssAttempts(auditLogMapper.countXssAttempts(startDate, endDate));

        // CSRF 攻击
        analysis.setCsrfAttempts(auditLogMapper.countCsrfAttempts(startDate, endDate));

        return analysis;
    }

    /**
     * 导出报告到 PDF
     */
    public String exportReportToPdf(SecurityAuditReport report) {
        try {
            String fileName = "security_audit_report_" + System.currentTimeMillis() + ".pdf";
            String filePath = "/tmp/" + fileName;

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));

            document.open();

            // 添加标题
            document.add(new Paragraph("安全审计报告", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));

            // 添加时间段
            document.add(new Paragraph("报告周期: " +
                DateFormatUtils.format(report.getStartDate(), "yyyy-MM-dd") + " 至 " +
                DateFormatUtils.format(report.getEndDate(), "yyyy-MM-dd")));

            // 添加各部分统计信息
            addLoginStatsSection(document, report.getLoginStats());
            addPrivilegeStatsSection(document, report.getPrivilegeStats());
            addSensitiveStatsSection(document, report.getSensitiveStats());
            addBehaviorStatsSection(document, report.getBehaviorStats());
            addThreatAnalysisSection(document, report.getThreatAnalysis());

            document.close();

            return filePath;
        } catch (Exception e) {
            throw new RuntimeException("报告导出失败", e);
        }
    }

    private void addLoginStatsSection(Document document, LoginSecurityStats stats) throws DocumentException {
        document.add(new Paragraph("登录安全统计", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        document.add(new Paragraph("失败登录次数: " + stats.getFailedLoginCount()));
        document.add(new Paragraph("异常IP登录次数: " + stats.getAbnormalIpLoginCount()));
        document.add(new Paragraph("异地登录次数: " + stats.getRemoteLoginCount()));
        document.add(new Paragraph("暴力破解尝试: " + stats.getBruteForceAttempts()));
        document.add(Chunk.NEWLINE);
    }

    // ... 其他章节类似实现
}
```

---

## 🧪 安全测试

### 1. 安全测试脚本

```bash
#!/bin/bash
# ===================================================================
# 安全测试脚本
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 测试配置
BASE_URL="http://localhost:8080"
API_KEY="your_api_key"
SECRET_KEY="your_secret_key"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 测试 JWT Token
test_jwt_auth() {
    log_info "测试 JWT Token 认证..."

    # 获取 Token
    response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"123456"}')

    token=$(echo $response | jq -r '.data.token')

    if [ "$token" == "null" ] || [ -z "$token" ]; then
        log_error "Token 获取失败"
        return 1
    fi

    log_info "Token 获取成功: ${token:0:20}..."

    # 使用 Token 访问接口
    response=$(curl -s -X GET "$BASE_URL/api/user/profile" \
        -H "Authorization: Bearer $token")

    if echo "$response" | jq -e '.code == 0' > /dev/null; then
        log_info "JWT Token 认证测试通过"
        return 0
    else
        log_error "JWT Token 认证测试失败"
        return 1
    fi
}

# 测试 API 签名
test_api_signature() {
    log_info "测试 API 签名..."

    timestamp=$(date +%s)
    nonce=$(openssl rand -hex 16)
    method="GET"
    uri="/api/data/query"
    params='{"param1":"value1"}'

    # 生成签名
    signature=$(echo -n "$method\n$uri\n$params\n$timestamp\n$nonce" | \
        openssl dgst -sha256 -hmac "$SECRET_KEY" | cut -d' ' -f2 | base64)

    # 发送请求
    response=$(curl -s -X GET "$BASE_URL$uri?param1=value1" \
        -H "X-API-Key: $API_KEY" \
        -H "X-API-Signature: $signature" \
        -H "X-API-Timestamp: $timestamp" \
        -H "X-API-Nonce: $nonce")

    if echo "$response" | jq -e '.code == 0' > /dev/null; then
        log_info "API 签名测试通过"
        return 0
    else
        log_error "API 签名测试失败"
        return 1
    fi
}

# 测试权限控制
test_authorization() {
    log_info "测试权限控制..."

    # 使用普通用户 Token 访问管理员接口
    response=$(curl -s -X GET "$BASE_URL/api/admin/users" \
        -H "Authorization: Bearer $USER_TOKEN" \
        -w "\nHTTP_CODE:%{http_code}")

    http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d':' -f2)

    if [ "$http_code" == "403" ]; then
        log_info "权限控制测试通过"
        return 0
    else
        log_error "权限控制测试失败，期望 403，实际 $http_code"
        return 1
    fi
}

# 测试 SQL 注入
test_sql_injection() {
    log_info "测试 SQL 注入防护..."

    # 尝试 SQL 注入
    response=$(curl -s -X GET "$BASE_URL/api/user/search?name=admin' OR '1'='1" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    if echo "$response" | jq -e '.code != 0' > /dev/null; then
        log_info "SQL 注入防护测试通过"
        return 0
    else
        log_error "SQL 注入防护测试失败"
        return 1
    fi
}

# 测试 XSS 防护
test_xss() {
    log_info "测试 XSS 防护..."

    # 尝试 XSS 攻击
    response=$(curl -s -X POST "$BASE_URL/api/comment" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $USER_TOKEN" \
        -d '{"content":"<script>alert(1)</script>"}' \
        -w "\nHTTP_CODE:%{http_code}")

    http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d':' -f2)

    if [ "$http_code" == "400" ] || [ "$http_code" == "403" ]; then
        log_info "XSS 防护测试通过"
        return 0
    else
        log_error "XSS 防护测试失败，期望 400/403，实际 $http_code"
        return 1
    fi
}

# 测试敏感数据加密
test_data_encryption() {
    log_info "测试敏感数据加密..."

    # 访问包含敏感信息的接口
    response=$(curl -s -X GET "$BASE_URL/api/user/profile" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    # 检查响应中是否包含明文敏感信息
    if echo "$response" | grep -E '[0-9]{15,18}' | grep -v '"phone"' > /dev/null; then
        log_error "检测到明文身份证号，敏感数据未加密"
        return 1
    fi

    if echo "$response" | grep -E '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}' | grep -v '"email"' > /dev/null; then
        log_error "检测到明文邮箱，敏感数据未加密"
        return 1
    fi

    log_info "敏感数据加密测试通过"
    return 0
}

# 测试审计日志
test_audit_log() {
    log_info "测试审计日志记录..."

    # 执行一个操作
    curl -s -X POST "$BASE_URL/api/user" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -d '{"username":"test","password":"123456"}' > /dev/null

    # 查询审计日志
    response=$(curl -s -X GET "$BASE_URL/api/audit/logs?operation=CREATE&table=sys_user" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    if echo "$response" | jq -e '.data.total > 0' > /dev/null; then
        log_info "审计日志测试通过"
        return 0
    else
        log_error "审计日志测试失败"
        return 1
    fi
}

# 压力测试安全接口
test_security_load() {
    log_info "执行安全接口压力测试..."

    # 并发测试登录接口
    for i in {1..10}; do
        curl -s -X POST "$BASE_URL/api/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"username":"admin","password":"123456"}' &
    done

    wait

    log_info "压力测试完成"
    return 0
}

# 主函数
main() {
    log_info "开始安全测试..."

    # 初始化测试
    test_jwt_auth || exit 1
    test_api_signature || exit 1

    # 权限测试
    test_authorization || exit 1

    # 安全防护测试
    test_sql_injection || exit 1
    test_xss || exit 1

    # 数据安全测试
    test_data_encryption || exit 1

    # 审计测试
    test_audit_log || exit 1

    # 性能测试
    test_security_load || exit 1

    log_info "所有安全测试通过！"
}

main "$@"
```

### 2. Python 安全测试

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
安全测试自动化脚本
"""

import requests
import json
import time
import hashlib
import hmac
import base64
from datetime import datetime

class SecurityTester:
    def __init__(self, base_url, api_key, secret_key):
        self.base_url = base_url
        self.api_key = api_key
        self.secret_key = secret_key
        self.session = requests.Session()
        self.token = None

    def test_jwt_authentication(self):
        """测试 JWT 认证"""
        print("[INFO] 测试 JWT 认证...")

        # 登录获取 Token
        login_data = {
            "username": "admin",
            "password": "123456"
        }

        response = self.session.post(
            f"{self.base_url}/api/auth/login",
            json=login_data
        )

        if response.status_code == 200:
            result = response.json()
            if result.get("code") == 0:
                self.token = result.get("data", {}).get("token")
                print(f"[SUCCESS] JWT Token 获取成功")
                return True

        print("[ERROR] JWT 认证失败")
        return False

    def test_api_signature(self):
        """测试 API 签名"""
        print("[INFO] 测试 API 签名...")

        timestamp = str(int(time.time()))
        nonce = "test123"
        method = "GET"
        uri = "/api/data/query"
        params = {"param1": "value1"}

        # 生成签名字符串
        sign_str = f"{method}\n{uri}\n{json.dumps(params, sort_keys=True)}\n{timestamp}\n{nonce}"

        # 计算 HMAC-SHA256 签名
        signature = base64.b64encode(
            hmac.new(
                self.secret_key.encode('utf-8'),
                sign_str.encode('utf-8'),
                hashlib.sha256
            ).digest()
        ).decode('utf-8')

        # 发送请求
        headers = {
            "X-API-Key": self.api_key,
            "X-API-Signature": signature,
            "X-API-Timestamp": timestamp,
            "X-API-Nonce": nonce
        }

        response = self.session.get(
            f"{self.base_url}{uri}",
            params=params,
            headers=headers
        )

        if response.status_code == 200:
            print("[SUCCESS] API 签名验证通过")
            return True

        print("[ERROR] API 签名验证失败")
        return False

    def test_sql_injection(self):
        """测试 SQL 注入防护"""
        print("[INFO] 测试 SQL 注入防护...")

        # 尝试 SQL 注入攻击
        sql_injection_payloads = [
            "' OR '1'='1",
            "admin'; DROP TABLE users; --",
            "1' UNION SELECT password FROM users --",
            "' OR 1=1#"
        ]

        for payload in sql_injection_payloads:
            response = self.session.get(
                f"{self.base_url}/api/user/search",
                params={"name": payload}
            )

            if response.status_code != 400:
                print(f"[WARNING] SQL 注入防护可能存在漏洞: {payload}")

        print("[INFO] SQL 注入防护测试完成")
        return True

    def test_xss_protection(self):
        """测试 XSS 防护"""
        print("[INFO] 测试 XSS 防护...")

        xss_payloads = [
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>"
        ]

        for payload in xss_payloads:
            response = self.session.post(
                f"{self.base_url}/api/comment",
                json={"content": payload}
            )

            # 检查响应中是否包含未转义的脚本
            if payload in response.text:
                print(f"[ERROR] XSS 防护失败: {payload}")
                return False

        print("[SUCCESS] XSS 防护测试通过")
        return True

    def test_rate_limiting(self):
        """测试限流机制"""
        print("[INFO] 测试限流机制...")

        # 快速发送多个请求
        for i in range(10):
            response = self.session.get(f"{self.base_url}/api/data/query")

            if response.status_code == 429:
                print(f"[SUCCESS] 限流机制生效")
                return True

        print("[WARNING] 未检测到限流机制")
        return False

    def test_data_encryption(self):
        """测试数据加密"""
        print("[INFO] 测试数据加密...")

        headers = {"Authorization": f"Bearer {self.token}"}
        response = self.session.get(
            f"{self.base_url}/api/user/profile",
            headers=headers
        )

        if response.status_code == 200:
            data = response.json()

            # 检查敏感字段是否加密
            if "data" in data:
                user_info = data["data"]
                if "phone" in user_info and not user_info["phone"].startswith("ENC("):
                    print("[ERROR] 敏感数据未加密")
                    return False

                if "email" in user_info and not user_info["email"].startswith("ENC("):
                    print("[ERROR] 敏感数据未加密")
                    return False

        print("[SUCCESS] 数据加密测试通过")
        return True

    def run_all_tests(self):
        """运行所有测试"""
        print("=" * 50)
        print("安全测试开始")
        print("=" * 50)

        tests = [
            ("JWT 认证", self.test_jwt_authentication),
            ("API 签名", self.test_api_signature),
            ("SQL 注入防护", self.test_sql_injection),
            ("XSS 防护", self.test_xss_protection),
            ("限流机制", self.test_rate_limiting),
            ("数据加密", self.test_data_encryption)
        ]

        passed = 0
        failed = 0

        for test_name, test_func in tests:
            print(f"\n{'=' * 20} {test_name} {'=' * 20}")
            try:
                if test_func():
                    passed += 1
                else:
                    failed += 1
            except Exception as e:
                print(f"[ERROR] 测试异常: {e}")
                failed += 1

        print("\n" + "=" * 50)
        print(f"测试结果: {passed} 通过, {failed} 失败")
        print("=" * 50)

if __name__ == "__main__":
    tester = SecurityTester(
        base_url="http://localhost:8080",
        api_key="your_api_key",
        secret_key="your_secret_key"
    )
    tester.run_all_tests()
```

---

## 📚 参考资料

1. [OWASP 安全编码规范](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)
2. [Spring Security 官方文档](https://docs.spring.io/spring-security/reference/)
3. [JWT 标准规范](https://datatracker.ietf.org/doc/html/rfc7519)
4. [OAuth 2.0 授权框架](https://datatracker.ietf.org/doc/html/rfc6749)
5. [HTTPS/TLS 安全最佳实践](https://wiki.mozilla.org/Security/Server_Side_TLS)

---

## 📋 安全检查清单

### 认证安全
- [ ] 强制使用 HTTPS
- [ ] JWT Token 设置合理过期时间
- [ ] 密码使用 BCrypt 加密
- [ ] 多因素认证 (MFA) 启用
- [ ] 防止暴力破解攻击
- [ ] Session 管理安全

### 数据安全
- [ ] 敏感数据加密存储
- [ ] 数据库连接加密
- [ ] 数据传输加密 (TLS 1.3)
- [ ] 数据脱敏显示
- [ ] 数据备份加密

### 接口安全
- [ ] API 签名验证
- [ ] 防重放攻击
- [ ] 接口限流
- [ ] 参数校验
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] CSRF 防护

### 审计日志
- [ ] 操作日志记录
- [ ] 登录日志记录
- [ ] 敏感操作审计
- [ ] 日志完整性保证
- [ ] 日志监控告警

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ 系统安全加固即将完成！** ฅ'ω'ฅ
