# Phase 11+: 数据加密实施指南

## 📋 概述

本文档详细描述了BaseBackend项目的数据加密实施，包括静态数据加密、传输加密、字段级加密和配置加密。数据加密是保障数据安全的重要手段，能够有效防止数据泄露和未授权访问。

---

## 🎯 实施目标

### 核心目标
1. ✅ 实现AES-256-GCM对称加密
2. ✅ 实现RSA-2048非对称加密
3. ✅ 实现字段级数据库加密
4. ✅ 实现传输层SSL/TLS加密
5. ✅ 实现配置文件加密 (Jasypt)
6. ✅ 提供密钥管理和轮换机制

### 技术栈
- **加密算法**: AES-256-GCM, RSA-2048/4096
- **加密库**: BouncyCastle, Google Tink, Jasypt
- **SSL/TLS**: OpenSSL, Java SSL
- **密钥管理**: 安全配置中心, 环境变量

---

## 🏗️ 架构设计

### 加密架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      数据加密架构                               │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   应用层加密   │  │   字段级加密   │  │  配置加密    │    │
│  │              │  │              │  │              │    │
│  │ • AES加密    │  │ • @Encrypted │  │ • Jasypt     │    │
│  │ • RSA加密    │  │ • 自动加密    │  │ • 环境变量   │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
└─────────┼─────────────────┼─────────────────┼─────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   数据库层    │  │   传输层      │  │   密钥管理    │    │
│  │              │  │              │  │              │    │
│  │ • 字段加密   │  │ • TLS 1.3    │  │ • KMS        │    │
│  │ • 列级加密   │  │ • 双向认证   │  │ • 密钥轮换    │    │
│  │ • TDE        │  │ • 证书管理   │  │ • 密钥存储    │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 加密流程

#### 1. 静态数据加密流程
```
明文数据 -> AES加密 -> 加密数据 -> 存储到数据库
存储数据 -> 解密 -> 明文数据 -> 应用处理
```

#### 2. 字段级加密流程
```
实体对象 -> 检查@Encrypted注解 -> 加密敏感字段 -> 保存到数据库
数据库查询 -> 解密敏感字段 -> 实体对象 -> 返回给客户端
```

#### 3. 传输加密流程
```
客户端请求 -> HTTPS/TLS加密 -> 网络传输 -> 服务端解密
响应数据 -> HTTPS/TLS加密 -> 网络传输 -> 客户端解密
```

---

## 📦 模块结构

### basebackend-security 模块
```
basebackend-security/
├── src/main/java/com/basebackend/security/
│   ├── encryption/
│   │   ├── AESEncryptionService.java         # AES加密服务
│   │   ├── RSAEncryptionService.java         # RSA加密服务
│   │   └── FieldEncryptionService.java       # 字段级加密服务
│   ├── config/
│   │   ├── SSLConfig.java                    # SSL/TLS配置
│   │   └── JasyptConfig.java                 # Jasypt配置
│   └── SecurityApplication.java              # 安全模块启动类
├── src/main/resources/
│   └── security-config.yml                   # 安全配置示例
└── scripts/
    └── generate-keys.sh                      # 密钥和证书生成脚本
```

---

## 🔧 详细配置

### 1. 生成密钥和证书

#### 运行密钥生成脚本
```bash
cd /opt/basebackend/basebackend-security
chmod +x scripts/generate-keys.sh
./scripts/generate-keys.sh
```

#### 生成的文件
```
/opt/basebackend/security/
├── keys/
│   ├── rsa-private-key-base64.txt           # RSA私钥(Base64)
│   ├── rsa-public-key-base64.txt            # RSA公钥(Base64)
│   └── rsa-private-key.pem                  # RSA私钥(PEM)
├── ssl/
│   ├── basebackend-cert.pem                 # SSL证书
│   ├── basebackend-key.pem                  # SSL私钥
│   ├── basebackend-keystore.p12             # KeyStore
│   └── truststore.jks                       # TrustStore
└── encryption/
    ├── aes-key.txt                          # AES密钥
    ├── security-config-template.properties  # 配置模板
    ├── application-security.yml             # 应用配置示例
    └── keys-summary.txt                     # 密钥摘要
```

### 2. AES加密配置

#### 注入AES加密服务
```java
@RestController
public class ExampleController {

    @Autowired
    private AESEncryptionService aesEncryptionService;

    private static final String AES_KEY = "从配置文件读取AES密钥";

    @PostMapping("/encrypt")
    public String encryptData(@RequestBody String data) {
        return aesEncryptionService.encrypt(data, AES_KEY);
    }

    @PostMapping("/decrypt")
    public String decryptData(@RequestBody String encryptedData) {
        return aesEncryptionService.decrypt(encryptedData, AES_KEY);
    }
}
```

#### 生成新的AES密钥
```java
@Service
public class KeyManagementService {

    @Autowired
    private AESEncryptionService aesEncryptionService;

    public String generateNewAESKey() throws NoSuchAlgorithmException {
        return aesEncryptionService.generateKey();
    }
}
```

### 3. RSA加密配置

#### RSA密钥生成和使用
```java
@Service
public class RSAService {

    @Autowired
    private RSAEncryptionService rsaEncryptionService;

    // 生成密钥对
    public KeyPair generateRSAKeyPair() {
        return rsaEncryptionService.generateKeyPair();
    }

    // 加密数据
    public String encryptWithRSA(String data, String publicKey) {
        return rsaEncryptionService.encrypt(data, publicKey);
    }

    // 解密数据
    public String decryptWithRSA(String encryptedData, String privateKey) {
        return rsaEncryptionService.decrypt(encryptedData, privateKey);
    }

    // 数字签名
    public String signData(String data, String privateKey) {
        return rsaEncryptionService.sign(data, privateKey);
    }

    // 验证签名
    public boolean verifySignature(String data, String sign, String publicKey) {
        return rsaEncryptionService.verify(data, sign, publicKey);
    }
}
```

### 4. 字段级加密

#### 在实体类中使用@Encrypted注解
```java
@Entity
@Table(name = "user")
public class User {

    @Id
    private Long id;

    private String username;

    // 对身份证号进行AES加密
    @Encrypted(algorithm = EncryptionType.AES)
    private String idCard;

    // 对银行账号进行RSA加密
    @Encrypted(algorithm = EncryptionType.RSA, publicKey = "RSA公钥")
    private String bankAccount;

    // 对手机号进行AES加密
    @Encrypted(algorithm = EncryptionType.AES)
    private String phone;

    // Getters and Setters
}
```

#### 在Service中使用字段加密
```java
@Service
public class UserService {

    @Autowired
    private FieldEncryptionService fieldEncryptionService;

    private static final String AES_MASTER_KEY = "AES主密钥";

    public User saveUser(User user) {
        // 加密敏感字段
        fieldEncryptionService.encryptFields(user, AES_MASTER_KEY);

        // 保存到数据库
        return userRepository.save(user);
    }

    public User getUser(Long id) {
        User user = userRepository.findById(id).orElse(null);

        if (user != null) {
            // 解密敏感字段
            fieldEncryptionService.decryptFields(user, AES_MASTER_KEY);
        }

        return user;
    }

    // 批量加密/解密
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();

        // 批量解密
        fieldEncryptionService.decryptBatch(users, AES_MASTER_KEY);

        return users;
    }
}
```

### 5. 传输加密 - SSL/TLS配置

#### application.yml中的SSL配置
```yaml
server:
  # 启用HTTPS
  ssl:
    enabled: true
    port: 8443
    # KeyStore配置
    key-store: classpath:ssl/basebackend-keystore.p12
    key-store-password: basebackend-pass
    key-store-type: PKCS12
    key-alias: basebackend
    # 双向SSL认证
    client-auth: want
    # SSL协议版本
    enabled-protocols: TLSv1.2,TLSv1.3
    # 密码套件
    ciphers: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384

  # HTTP到HTTPS重定向
  http:
    port: 8080

# 客户端SSL配置
spring:
  ssl:
    bundle:
      jks:
        client:
          trust-store: classpath:ssl/truststore.jks
          trust-store-password: truststore-pass
          trust-store-type: JKS

# 强制HTTPS
security:
  force-https: true
```

#### 创建安全的WebClient
```java
@Configuration
public class SSLWebClientConfig {

    @Bean
    public WebClient secureWebClient(SslBundles sslBundles) {
        SslBundle sslBundle = sslBundles.getBundle("client");

        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslBundle.getSslContext()))
                .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
```

### 6. 配置加密 - Jasypt

#### 使用Jasypt加密配置文件
```yaml
# 数据库配置 - 使用Jasypt加密
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/basebackend?useUnicode=true&characterEncoding=utf8&useSSL=false
    username: root
    password: ENC(加密后的数据库密码)

# Redis配置 - 使用Jasypt加密
spring:
  redis:
    host: localhost
    port: 6379
    password: ENC(加密后的Redis密码)
    ssl:
      enabled: true

# JWT密钥 - 使用Jasypt加密
jwt:
  secret: ENC(加密后的JWT密钥)

# 加密因子配置
jasypt:
  encryptor:
    algorithm: PBEWITHHMACSHA512ANDAES_256
    password: basebackend-encrypt-password
```

#### 加密和解密工具
```java
@Component
public class JasyptUtil {

    @Autowired
    private StringEncryptor stringEncryptor;

    public String encrypt(String plainText) {
        return stringEncryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        return stringEncryptor.decrypt(encryptedText);
    }
}
```

#### 命令行加密工具
```bash
# 使用jasypt命令行工具加密
jasypt encrypt input="数据库密码" password="basebackend-encrypt-password"

# 或者使用脚本
./mvnw -Dexec.mainClass="com.ulisesbocchio.jasyptspringboot.BootstrapConfiguration" \
       -Dexec.args="encrypt input=数据库密码 password=basebackend-encrypt-password"
```

---

## 🔐 使用示例

### 1. 完整加密示例

#### 创建加密服务
```java
@Service
public class DataEncryptionService {

    @Autowired
    private AESEncryptionService aesEncryptionService;

    @Autowired
    private RSAEncryptionService rsaEncryptionService;

    @Autowired
    private FieldEncryptionService fieldEncryptionService;

    @Value("${security.encryption.aes.key}")
    private String aesKey;

    @Value("${security.encryption.rsa.privateKey}")
    private String rsaPrivateKey;

    @Value("${security.encryption.rsa.publicKey}")
    private String rsaPublicKey;

    /**
     * 加密用户敏感信息
     */
    public UserInfo encryptUserInfo(UserInfo userInfo) {
        try {
            // 使用RSA加密身份证号
            userInfo.setIdCard(rsaEncryptionService.encrypt(userInfo.getIdCard(), rsaPublicKey));

            // 使用AES加密手机号
            userInfo.setPhone(aesEncryptionService.encrypt(userInfo.getPhone(), aesKey));

            // 使用AES加密邮箱
            userInfo.setEmail(aesEncryptionService.encrypt(userInfo.getEmail(), aesKey));

            return userInfo;
        } catch (Exception e) {
            throw new SecurityException("用户信息加密失败", e);
        }
    }

    /**
     * 解密用户敏感信息
     */
    public UserInfo decryptUserInfo(UserInfo userInfo) {
        try {
            // 使用RSA解密身份证号
            userInfo.setIdCard(rsaEncryptionService.decrypt(userInfo.getIdCard(), rsaPrivateKey));

            // 使用AES解密手机号
            userInfo.setPhone(aesEncryptionService.decrypt(userInfo.getPhone(), aesKey));

            // 使用AES解密邮箱
            userInfo.setEmail(aesEncryptionService.decrypt(userInfo.getEmail(), aesKey));

            return userInfo;
        } catch (Exception e) {
            throw new SecurityException("用户信息解密失败", e);
        }
    }

    /**
     * 数字签名
     */
    public String signData(String data) {
        return rsaEncryptionService.sign(data, rsaPrivateKey);
    }

    /**
     * 验证签名
     */
    public boolean verifySignature(String data, String signature) {
        return rsaEncryptionService.verify(data, signature, rsaPublicKey);
    }
}
```

### 2. 数据库操作示例

#### 使用JPA进行加密/解密
```java
@Entity
public class UserEntity {

    @Id
    private Long id;

    private String username;

    @Encrypted(algorithm = EncryptionType.AES)
    private String email;

    @Encrypted(algorithm = EncryptionType.AES)
    private String phone;

    @Convert(converter = PhoneConverter.class)
    private String phoneConverted;
}

/**
 * 自定义转换器
 */
public class PhoneConverter implements AttributeConverter<String, String> {

    @Autowired
    private FieldEncryptionService fieldEncryptionService;

    private static final String AES_KEY = "AES密钥";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute != null) {
            return fieldEncryptionService.encryptFields(attribute, AES_KEY);
        }
        return null;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData != null) {
            return fieldEncryptionService.decryptFields(dbData, AES_KEY);
        }
        return null;
    }
}
```

### 3. 配置文件示例

#### 完整的安全配置
```yaml
# application-security.yml
server:
  port: 8080
  ssl:
    enabled: true
    port: 8443
    key-store: classpath:ssl/basebackend-keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD:basebackend-pass}
    key-store-type: PKCS12
    key-alias: basebackend
  http:
    port: 8080

spring:
  # 数据库加密配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/basebackend?useSSL=true&requireSSL=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:ENC(加密后的密码)}
    # Druid连接池
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 20
      # 连接初始化SQL
      connection-init-sqls:
        - "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci"
        - "SET sql_mode='STRICT_TRANS_TABLES'"

  # Redis加密配置
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:ENC(加密后的密码)}
    ssl:
      enabled: ${REDIS_SSL_ENABLED:true}
    database: 0
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 1000ms

  # 客户端SSL
  ssl:
    bundle:
      jks:
        client:
          trust-store: classpath:ssl/truststore.jks
          trust-store-password: ${TRUSTSTORE_PASSWORD:truststore-pass}

# 安全加密配置
security:
  encryption:
    # AES加密
    aes:
      enabled: true
      key: ${AES_KEY:ENC(加密后的AES密钥)}
      key-length: 256
      iv-length: 12
      algorithm: AES/GCM/NoPadding

    # RSA加密
    rsa:
      enabled: true
      key-size: 2048
      algorithm: RSA/ECB/PKCS1Padding
      private-key: ${RSA_PRIVATE_KEY:ENC(加密后的RSA私钥)}
      public-key: ${RSA_PUBLIC_KEY:ENC(加密后的RSA公钥)}

    # 字段级加密
    field:
      enabled: true
      default-algorithm: AES
      encrypt-null-values: false

  # 传输加密
  transport:
    # HTTPS配置
    https:
      enabled: ${HTTPS_ENABLED:true}
      port: 8443
      redirect-http: true
      force-https: true

    # 双向SSL
    mutual-tls:
      enabled: ${MUTUAL_TLS_ENABLED:false}
      client-cert-required: false

  # 安全审计
  audit:
    enabled: true
    log-level: INFO
    log-encryptions: true
    log-decryptions: true

  # CORS配置
  cors:
    allowed-origins: https://localhost:8080
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: *
    allow-credentials: true

# Jasypt配置
jasypt:
  encryptor:
    algorithm: PBEWITHHMACSHA512ANDAES_256
    password: ${JASYPT_ENCRYPTOR_PASSWORD:basebackend-encrypt-password}
    key-obtention-iterations: 1000
    pool-size: 1

# Actuator安全
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      security:
        enabled: true
  security:
    enabled: true

# 日志配置
logging:
  level:
    com.basebackend.security: INFO
    org.springframework.security: WARN
    javax.net.ssl: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
  file:
    name: /opt/basebackend/logs/security/security.log
    max-size: 100MB
    max-history: 30
```

---

## 🧪 测试验证

### 1. 加密测试

#### AES加密测试
```java
@Test
public void testAESEncryption() throws NoSuchAlgorithmException {
    String aesKey = aesEncryptionService.generateKey();
    String plainText = "Hello, BaseBackend!";
    String encryptedText = aesEncryptionService.encrypt(plainText, aesKey);
    String decryptedText = aesEncryptionService.decrypt(encryptedText, aesKey);

    assertThat(decryptedText).isEqualTo(plainText);
    assertThat(encryptedText).isNotEqualTo(plainText);
}
```

#### RSA加密测试
```java
@Test
public void testRSAEncryption() {
    KeyPair keyPair = rsaEncryptionService.generateKeyPair();
    String publicKey = rsaEncryptionService.getPublicKey(keyPair);
    String privateKey = rsaEncryptionService.getPrivateKey(keyPair);

    String plainText = "Hello, RSA!";
    String encryptedText = rsaEncryptionService.encrypt(plainText, publicKey);
    String decryptedText = rsaEncryptionService.decrypt(encryptedText, privateKey);

    assertThat(decryptedText).isEqualTo(plainText);
}
```

#### 字段加密测试
```java
@Test
public void testFieldEncryption() throws NoSuchAlgorithmException {
    String aesKey = aesEncryptionService.generateKey();

    User user = new User();
    user.setId(1L);
    user.setUsername("admin");
    user.setEmail("admin@basebackend.com");
    user.setPhone("13800138000");

    User encryptedUser = (User) fieldEncryptionService.encryptFields(user, aesKey);
    assertThat(encryptedUser.getEmail()).isNotEqualTo(user.getEmail());

    User decryptedUser = (User) fieldEncryptionService.decryptFields(encryptedUser, aesKey);
    assertThat(decryptedUser.getEmail()).isEqualTo(user.getEmail());
}
```

### 2. SSL/TLS测试

#### 检查HTTPS配置
```bash
# 检查SSL证书
openssl s_client -connect localhost:8443 -servername localhost

# 检查支持的协议
nmap --script ssl-enum-ciphers -p 8443 localhost

# 使用curl测试HTTPS
curl -k https://localhost:8443/actuator/health
```

#### 双向SSL测试
```java
@TestConfiguration
public class SSLTestConfig {

    @Bean
    public RestTemplate sslRestTemplate() throws Exception {
        SSLContext sslContext = SSLContextBuilder
                .create()
                .loadKeyMaterial(
                    new FileSystemResource("src/test/resources/ssl/client-keystore.p12"),
                    "client-pass".toCharArray(),
                    "client-pass".toCharArray()
                )
                .loadTrustMaterial(
                    new FileSystemResource("src/test/resources/ssl/truststore.jks"),
                    "truststore-pass".toCharArray()
                )
                .build();

        return RestTemplateBuilder.newBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(
                    HttpClient.create()
                        .secure(sslSpec -> sslSpec.sslContext(sslContext))
                ))
                .build();
    }
}
```

### 3. 性能测试

#### 加密性能测试
```java
@Test
public void testEncryptionPerformance() throws NoSuchAlgorithmException {
    String aesKey = aesEncryptionService.generateKey();
    String testData = "测试数据".repeat(1000);

    // AES性能测试
    Instant start = Instant.now();
    for (int i = 0; i < 10000; i++) {
        aesEncryptionService.encrypt(testData, aesKey);
    }
    Instant end = Instant.now();
    long aesTime = Duration.between(start, end).toMillis();

    log.info("AES加密10000次耗时: {} ms", aesTime);
    log.info("平均每次耗时: {} ms", aesTime / 10000.0);
}
```

---

## 📊 监控指标

### 1. 加密指标

#### 自定义指标
```java
@Component
public class EncryptionMetrics {

    private final Counter aesEncryptCounter;
    private final Timer aesEncryptTimer;
    private final Counter rsaEncryptCounter;
    private final Timer rsaEncryptTimer;
    private final MeterRegistry meterRegistry;

    public EncryptionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        aesEncryptCounter = Counter.builder("security.encryption.aes.count")
                .description("AES encryption count")
                .register(meterRegistry);

        aesEncryptTimer = Timer.builder("security.encryption.aes.duration")
                .description("AES encryption duration")
                .register(meterRegistry);

        rsaEncryptCounter = Counter.builder("security.encryption.rsa.count")
                .description("RSA encryption count")
                .register(meterRegistry);

        rsaEncryptTimer = Timer.builder("security.encryption.rsa.duration")
                .description("RSA encryption duration")
                .register(meterRegistry);
    }

    public void recordAESEncryption(Duration duration) {
        aesEncryptCounter.increment();
        aesEncryptTimer.record(duration);
    }

    public void recordRSAEncryption(Duration duration) {
        rsaEncryptCounter.increment();
        rsaEncryptTimer.record(duration);
    }
}
```

### 2. 安全指标

#### 密钥使用统计
```
# 密钥使用次数
security.keys.aes.usage.count=12345
security.keys.rsa.usage.count=6789

# 密钥轮换时间
security.keys.aes.last.rotation=2025-11-15T10:00:00Z
security.keys.rsa.last.rotation=2025-11-15T10:00:00Z

# SSL连接统计
security.ssl.connections.active=50
security.ssl.connections.total=12345
```

---

## 🔄 密钥管理

### 1. 密钥存储

#### 环境变量存储
```yaml
# 使用环境变量
security:
  encryption:
    aes:
      key: ${AES_ENCRYPTION_KEY}
    rsa:
      private-key: ${RSA_PRIVATE_KEY}
      public-key: ${RSA_PUBLIC_KEY}
```

#### 配置中心存储
```yaml
# 使用Nacos配置中心
security:
  encryption:
    aes:
      key: ${NACOS:security.encryption.aes.key}
    rsa:
      private-key: ${NACOS:security.encryption.rsa.privateKey}
```

### 2. 密钥轮换

#### 自动密钥轮换
```java
@Component
public class KeyRotationService {

    @Scheduled(cron = "0 0 2 1 * ?") // 每月1日凌晨2点执行
    public void rotateKeys() {
        log.info("开始密钥轮换...");

        // 生成新密钥
        String newAESKey = aesEncryptionService.generateKey();

        // 更新配置
        updateKeyInConfigCenter("security.encryption.aes.key", newAESKey);

        // 重新加载配置
        refreshApplicationContext();

        log.info("密钥轮换完成");
    }

    private void updateKeyInConfigCenter(String key, String value) {
        // 更新到Nacos配置中心
        nacosConfigService.publishConfig(
            dataId,
            group,
            key + "=" + value
        );
    }
}
```

### 3. 密钥备份

#### 密钥备份脚本
```bash
#!/bin/bash
# 密钥备份脚本

BACKUP_DIR="/opt/basebackend/security/backup/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# 备份密钥文件
cp -r /opt/basebackend/security/keys/* $BACKUP_DIR/
cp -r /opt/basebackend/security/ssl/* $BACKUP_DIR/
cp -r /opt/basebackend/security/encryption/* $BACKUP_DIR/

# 加密备份文件
tar -czf $BACKUP_DIR.tar.gz $BACKUP_DIR
openssl enc -aes-256-cbc -salt -in $BACKUP_DIR.tar.gz -out $BACKUP_DIR.tar.gz.enc -k $BACKUP_PASSWORD

# 删除临时文件
rm -rf $BACKUP_DIR $BACKUP_DIR.tar.gz

# 上传到安全存储
# aws s3 cp $BACKUP_DIR.tar.gz.enc s3://basebackend-security-backup/
```

---

## 📚 最佳实践

### 1. 加密选择

- **对称加密**: 适用于大量数据加密(AES-256)
- **非对称加密**: 适用于小量数据加密和数字签名(RSA-2048/4096)
- **字段级加密**: 适用于数据库敏感字段
- **传输加密**: 使用TLS 1.2或1.3

### 2. 密钥管理

- **分离职责**: 加密和解密服务分离
- **安全存储**: 使用KMS或安全配置中心
- **定期轮换**: 定期更新密钥
- **最小权限**: 只授权必要的访问权限

### 3. 性能优化

- **缓存密钥**: 避免重复生成
- **批量操作**: 批量加密/解密
- **异步处理**: 非关键业务异步加密
- **监控延迟**: 监控加密性能

### 4. 安全考虑

- **随机IV**: 每次加密使用随机IV
- **完整性验证**: 使用GCM模式
- **错误处理**: 避免信息泄露
- **日志记录**: 记录加密操作

---

## 🔧 故障排除

### 1. 常见错误

#### 密钥错误
```
java.security.InvalidKeyException: Illegal key size
```
**解决**: 安装JCE无限制强度策略文件

#### SSL握手失败
```
javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException
```
**解决**: 检查证书是否正确，信任链是否完整

#### 解密失败
```
org.jasypt.exceptions.EncryptionOperationNotPossibleException
```
**解决**: 检查密钥是否正确，数据是否被篡改

### 2. 调试日志

```yaml
logging:
  level:
    com.basebackend.security: DEBUG
    javax.net.ssl: DEBUG
    sun.security.ssl: DEBUG
```

### 3. 健康检查

```java
@RestController
public class EncryptionHealthCheck {

    @GetMapping("/actuator/health/encryption")
    public Map<String, Object> checkEncryptionHealth() {
        Map<String, Object> result = new HashMap<>();

        // 检查AES密钥
        try {
            aesEncryptionService.encrypt("test", aesKey);
            result.put("aes", "OK");
        } catch (Exception e) {
            result.put("aes", "ERROR: " + e.getMessage());
        }

        // 检查RSA密钥
        try {
            rsaEncryptionService.encrypt("test", rsaPublicKey);
            rsaEncryptionService.decrypt("test", rsaPrivateKey);
            result.put("rsa", "OK");
        } catch (Exception e) {
            result.put("rsa", "ERROR: " + e.getMessage());
        }

        return result;
    }
}
```

---

## 📞 技术支持

### 联系方式
- **技术支持邮箱**: support@basebackend.com
- **技术文档**: https://docs.basebackend.com/encryption
- **GitHub**: https://github.com/basebackend/data-encryption

### 参考资料
- [NIST Cryptographic Standards](https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Java Cryptography Architecture](https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/)

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
