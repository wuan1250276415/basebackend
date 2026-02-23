# Nacos 模块改进计划

## Context

`basebackend-nacos` 模块（39个源文件，5519行代码，5个测试类）封装 Nacos 配置管理、服务发现、灰度发布能力。经研究审计发现 5 个 P0 级问题（安全/崩溃/资源泄露）、多个 P1 级问题（死代码/设计缺陷）、以及若干可扩展方向。

**约束**：
- 所有改动限于 `basebackend-nacos` 模块内部
- 不改变对外公共 API 签名（`NacosConfigService`, `ServiceDiscoveryManager`, `GrayReleaseService`, `NacosConfigProperties`）
- 现有 5 个测试类必须继续通过
- 编译验证：`mvn compile -pl basebackend-nacos`
- 测试验证：`mvn test -pl basebackend-nacos`

---

## Phase 1: P0 — 崩溃与安全修复（5 个任务）

### Task 1.1: 修复 NacosConfigConfiguration NPE

**文件**: `config/NacosConfigConfiguration.java:41-46`

**问题**: `Properties.put()` 继承自 `Hashtable`，不接受 null value。当 `username` 或 `password` 为 null 时，`properties.put("username", config.getUsername())` 抛出 NPE。

**改动**:
```java
// 替换 lines 41-46，从直接 put 改为条件 put
Properties properties = new Properties();
properties.put("serverAddr", config.getServerAddr());
properties.put("namespace", config.getNamespace());
properties.put("group", config.getGroup());
if (config.getUsername() != null) {
    properties.put("username", config.getUsername());
}
if (config.getPassword() != null) {
    properties.put("password", config.getPassword());
}
```

**验证**: 单元测试验证 username=null 时不抛 NPE。

---

### Task 1.2: 修复 NacosDiscoveryConfiguration NPE

**文件**: `config/NacosDiscoveryConfiguration.java:41-46`

**问题**: 与 Task 1.1 完全相同的 bug，`discovery.getUsername()` / `discovery.getPassword()` 可能为 null。

**改动**: 同 Task 1.1 模式，对 username/password 做 null 检查后再 put。

---

### Task 1.3: 修复 NacosConfigRefresher 线程池泄露

**文件**: `refresh/NacosConfigRefresher.java:41`

**问题**:
1. `Executor executor = Executors.newFixedThreadPool(2)` — 声明为 `Executor` 类型，`destroy()` 中的 `instanceof ExecutorService` 虽然能匹配，但语义不清晰
2. 线程池创建的线程是非 daemon 线程，阻止 JVM 正常退出
3. 池大小硬编码为 2

**改动**:
```java
// 1. 字段类型改为 ExecutorService
private final ExecutorService executor;

// 2. 构造中用 ThreadFactory 设置 daemon=true，池大小可配
public NacosConfigRefresher(...) {
    // ...其他注入...
    int poolSize = properties.getConfig().getRefreshThreadPoolSize() != null
            ? properties.getConfig().getRefreshThreadPoolSize() : 2;
    ThreadFactory tf = r -> {
        Thread t = new Thread(r, "nacos-config-refresh");
        t.setDaemon(true);
        return t;
    };
    this.executor = Executors.newFixedThreadPool(poolSize, tf);
}

// 3. destroy() 简化
@PreDestroy
public void destroy() {
    executor.shutdown();
    log.info("Nacos配置刷新监听器已关闭");
}
```

**附带**: 在 `NacosConfigProperties.Config` 中添加字段：
```java
private Integer refreshThreadPoolSize;
```

---

### Task 1.4: 修复 CredentialEncryptionService 硬编码密钥

**文件**: `security/CredentialEncryptionService.java:43,174`

**问题**:
1. `DEFAULT_KEY_SEED = "basebackend-nacos-encryption-key"` — 硬编码密钥种子，攻击者可直接从源码获得
2. `"nacos-credential-salt".getBytes()` — 固定 salt，违反密码学最佳实践
3. 构造函数中仅用 `System.getenv` / `System.getProperty`，未集成 Spring 配置体系

**改动**:
1. 移除 `DEFAULT_KEY_SEED` 常量
2. 构造函数改为接收 Spring 注入的密钥参数（`@Value("${nacos.encryption.key:}")`)
3. 当密钥参数为空时，生成随机密钥并打印 WARN（仅用于开发环境），不再使用可预测的默认值
4. salt 改为从密钥种子 + 固定前缀派生的 SHA-256 摘要（不再是纯字符串字面量）

```java
@Component
public class CredentialEncryptionService {

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(
            @Value("${nacos.encryption.key:}") String keySeed) {
        if (keySeed == null || keySeed.isBlank()) {
            keySeed = UUID.randomUUID().toString();
            log.warn("No encryption key configured (nacos.encryption.key). "
                    + "Using random key — encrypted values will NOT survive restart. "
                    + "Set NACOS_ENCRYPTION_KEY or nacos.encryption.key for production.");
        }
        this.secretKey = deriveKey(keySeed);
        log.info("CredentialEncryptionService initialized");
    }

    private SecretKey deriveKey(String keySeed) {
        try {
            // salt 从 keySeed 派生，避免固定字面量
            byte[] salt = MessageDigest.getInstance("SHA-256")
                    .digest(("nacos-kdf-salt:" + keySeed).getBytes(StandardCharsets.UTF_8));
            byte[] saltTruncated = Arrays.copyOf(salt, 16);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(keySeed.toCharArray(), saltTruncated, ITERATION_COUNT, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }
}
```

---

### Task 1.5: 集成 CredentialEncryptionService 到初始化流程

**文件**: `config/NacosConfigConfiguration.java`, `config/NacosDiscoveryConfiguration.java`

**问题**: `CredentialEncryptionService` 已存在但从未被使用（死代码）。Config/Discovery 配置类直接使用明文 username/password，未经过解密。

**改动**:
1. 两个 Configuration 类注入 `ObjectProvider<CredentialEncryptionService>`（使用 ObjectProvider 避免强依赖）
2. 在构建 Properties 时，对 password 调用 `decryptIfNeeded()`

```java
// NacosConfigConfiguration 增加注入
private final ObjectProvider<CredentialEncryptionService> encryptionServiceProvider;

// configService() 方法中
String password = config.getPassword();
CredentialEncryptionService encryptionService = encryptionServiceProvider.getIfAvailable();
if (password != null && encryptionService != null) {
    password = encryptionService.decryptIfNeeded(password);
}
if (password != null) {
    properties.put("password", password);
}
```

**同样修改 NacosDiscoveryConfiguration。**

---

## Phase 2: P1 — 设计缺陷修复（6 个任务）

### Task 2.1: 移除 NacosConfigProperties 上的 @RefreshScope

**文件**: `config/NacosConfigProperties.java:22`

**问题**: `@RefreshScope` 用在 `@ConfigurationProperties` Bean 上是 Spring Cloud 反模式。`@ConfigurationProperties` 已支持属性绑定更新，`@RefreshScope` 会导致 Bean 代理化，在初始化阶段可能引发循环依赖。

**改动**: 删除 `@RefreshScope` 注解（第 22 行），保留其余不变。

---

### Task 2.2: 移除默认凭证

**文件**: `config/NacosConfigProperties.java:99,104,207,212`

**问题**: `username = "nacos"` 和 `password = "nacos"` 作为默认值，生产环境可能遗忘配置，使用默认凭证连接。

**改动**:
```java
// Config 类
private String username;   // 移除默认值 "nacos"
private String password;   // 移除默认值 "nacos"

// Discovery 类
private String username;   // 移除默认值 "nacos"
private String password;   // 移除默认值 "nacos"
```

配合 Task 1.1/1.2 的 null 检查，不配置 username/password 时不传入 Properties，Nacos SDK 会以匿名模式连接。

---

### Task 2.3: 修复 NacosMetrics 标签基数爆炸

**文件**: `metrics/NacosMetrics.java`

**问题**: 所有 metric 方法使用 `dataId`、`serviceName` 作为 tag 值。在多租户/多服务场景下，tag 基数无上限，导致 Prometheus 时间序列爆炸和 OOM。

**改动**:
1. `recordConfigGetSuccess/Failure/Latency` — 移除 `dataId` tag，保留 `group` 和 `status`
2. `recordServiceDiscovery` — 保留 `service` tag（服务名基数有限）
3. `handleConfigChange` — 移除 `dataId` tag
4. 若需 dataId 级监控，改用 `log.info` 记录

```java
public void recordConfigGetSuccess(String dataId, String group) {
    Counter.builder(NacosConstants.METRIC_CONFIG_GET)
            .tag("group", group)
            .tag("status", "success")
            .register(registry)
            .increment();
    log.debug("Config get success: dataId={}, group={}", dataId, group);
}
```

---

### Task 2.4: NacosConfigRefresher 改为 @RequiredArgsConstructor 注入

**文件**: `refresh/NacosConfigRefresher.java`

**问题**: 当前 `executor` 字段是内联初始化的 `Executors.newFixedThreadPool(2)`，无法使用 `@RequiredArgsConstructor`。Task 1.3 已将其改为构造注入，此任务确保该类的构造函数参数列表正确。

**改动**: 已在 Task 1.3 中覆盖。此任务仅验证：移除 `@RequiredArgsConstructor`，改为显式构造函数（因为 executor 需要在构造中创建而非注入）。

---

### Task 2.5: 统一 ConfigChangeEvent 引用

**文件**: 模块内搜索所有 `ConfigChangeEvent` 引用

**问题**: 确认模块内只有 `com.basebackend.nacos.event.ConfigChangeEvent` 一个类。若 admin-api 中存在同名类，不属于本模块范围。

**改动**: 确认无需改动（仅搜索验证）。

---

### Task 2.6: 移除 example 包

**文件**: `example/AnnotationDrivenExample.java`, `example/BasicUsageExample.java`, `example/GrayReleaseExample.java`

**问题**: 479 行示例代码打包进 JAR，不会被运行，增加 JAR 体积。

**改动**: 删除 `src/main/java/com/basebackend/nacos/example/` 整个目录。示例代码可移入测试目录或文档。

---

## Phase 3: P2 — 健壮性增强（4 个任务）

### Task 3.1: EnhancedNacosConfigService 集成 RetryExecutor

**文件**: `service/EnhancedNacosConfigService.java`

**问题**: `EnhancedNacosConfigService` 已有方法但未使用 `RetryExecutor`。配置获取应具备自动重试能力。

**改动**: 在 `getConfig`/`publishConfig` 方法中包装 `RetryExecutor.executeWithRetry()`，重试 3 次，间隔 1s，指数退避。

---

### Task 3.2: GrayReleaseService 百分比策略确定性

**文件**: `service/GrayReleaseService.java:452-453`

**问题**: `selectByPercentage()` 使用 `Collections.shuffle(shuffled)` 随机选择实例。同一请求参数每次返回不同结果，无法复现和调试。

**改动**: 使用基于 dataId 的确定性种子：
```java
long seed = grayConfig.getDataId().hashCode();
Collections.shuffle(shuffled, new Random(seed));
```

---

### Task 3.3: ConfigPublisher 使用 EnhancedNacosConfigService

**文件**: `service/ConfigPublisher.java:18`

**问题**: `ConfigPublisher` 注入 `NacosConfigService`（基础版），未利用 `EnhancedNacosConfigService` 的增强功能（重试、指标等）。

**改动**: 将依赖从 `NacosConfigService` 改为 `EnhancedNacosConfigService`。

---

### Task 3.4: InMemoryGrayReleaseHistoryRepository 添加 @ConditionalOnProperty 保护

**文件**: `config/NacosAutoConfiguration.java:60-65`

**问题**: 生产环境可能误用内存实现导致数据丢失。当前仅靠 `@ConditionalOnMissingBean` 保护。

**改动**: 添加日志警告级别提升：
```java
@Bean
@ConditionalOnMissingBean(GrayReleaseHistoryRepository.class)
public GrayReleaseHistoryRepository grayReleaseHistoryRepository() {
    log.warn("Using IN-MEMORY GrayReleaseHistoryRepository. "
            + "Data will be lost on restart. "
            + "Provide a persistent implementation for production.");
    return new InMemoryGrayReleaseHistoryRepository();
}
```

---

## Phase 4: 测试补充（2 个任务）

### Task 4.1: 为 Phase 1 修复添加单元测试

**新建文件**: `src/test/java/com/basebackend/nacos/config/NacosConfigConfigurationTest.java`

测试用例：
1. `configService_shouldNotThrowNPE_whenUsernameIsNull` — username=null 时正常创建
2. `configService_shouldNotThrowNPE_whenPasswordIsNull` — password=null 时正常创建
3. `configService_shouldUseProvidedCredentials` — 正常传入时属性正确

**新建文件**: `src/test/java/com/basebackend/nacos/security/CredentialEncryptionServiceTest.java`

测试用例：
1. `encrypt_decrypt_roundTrip` — 加密后解密得到原文
2. `decrypt_returnsPlaintext_whenNotEncrypted` — 非加密文本原样返回
3. `constructor_usesRandomKey_whenNoKeyConfigured` — 未配置密钥时不崩溃
4. `isEncrypted_detectsPrefix` — 正确识别 `ENC(...)` 格式

**新建文件**: `src/test/java/com/basebackend/nacos/refresh/NacosConfigRefresherTest.java`

测试用例：
1. `destroy_shutdownsExecutor` — destroy 后线程池关闭
2. `afterPropertiesSet_skipsRefresh_whenNoListeners` — 无监听器时不注册

---

### Task 4.2: 运行全量测试

```bash
mvn test -pl basebackend-nacos
```

确保所有新增 + 原有 5 个测试类全部通过。

---

## 验证命令

```bash
# 编译验证
mvn compile -pl basebackend-nacos

# 单元测试
mvn test -pl basebackend-nacos

# 确认对外 API 签名未变
# NacosConfigService, ServiceDiscoveryManager, GrayReleaseService 的 public 方法签名不变
```

## 涉及文件汇总

| 操作 | 文件路径 |
|------|---------|
| 修改 | `config/NacosConfigConfiguration.java` |
| 修改 | `config/NacosDiscoveryConfiguration.java` |
| 修改 | `config/NacosConfigProperties.java` |
| 修改 | `config/NacosAutoConfiguration.java` |
| 修改 | `refresh/NacosConfigRefresher.java` |
| 修改 | `security/CredentialEncryptionService.java` |
| 修改 | `metrics/NacosMetrics.java` |
| 修改 | `service/ConfigPublisher.java` |
| 修改 | `service/EnhancedNacosConfigService.java` |
| 修改 | `service/GrayReleaseService.java` |
| 删除 | `example/AnnotationDrivenExample.java` |
| 删除 | `example/BasicUsageExample.java` |
| 删除 | `example/GrayReleaseExample.java` |
| 新建 | `test: NacosConfigConfigurationTest.java` |
| 新建 | `test: CredentialEncryptionServiceTest.java` |
| 新建 | `test: NacosConfigRefresherTest.java` |
| 不改 | `service/NacosConfigService.java` (公共API) |
| 不改 | `service/ServiceDiscoveryManager.java` (公共API) |

## 实施顺序依赖

```
Phase 1 (顺序执行):
  Task 1.1 → Task 1.2 (同一模式，可并行)
  Task 1.3 (独立)
  Task 1.4 → Task 1.5 (1.5 依赖 1.4 的改造)

Phase 2 (Phase 1 完成后):
  Task 2.1, 2.2 (并行，均改 NacosConfigProperties)
  Task 2.3 (独立)
  Task 2.4 (依赖 1.3)
  Task 2.5 (验证，独立)
  Task 2.6 (独立)

Phase 3 (Phase 2 完成后):
  Task 3.1 (独立)
  Task 3.2 (独立)
  Task 3.3 (依赖 3.1)
  Task 3.4 (独立)

Phase 4 (所有 Phase 完成后):
  Task 4.1 → Task 4.2
```
