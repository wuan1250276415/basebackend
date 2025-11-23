# 审计日志增强系统

## 📖 概述

审计日志增强系统是专为 basebackend-logging 模块设计的核心安全组件，提供完整的操作审计能力，满足等保2.0三级要求。系统采用数字签名和哈希链技术，确保审计日志的完整性和不可抵赖性。

### 核心特性

- ✅ **完整性保护**：SHA-256 哈希链 + 数字签名
- ✅ **加密存储**：AES-256-GCM 加密
- ✅ **异步批量写入**：高吞吐、低延迟
- ✅ **多级存储**：本地文件 + Redis + 数据库
- ✅ **自动轮转**：按大小/时间滚动
- ✅ **实时监控**：Micrometer 指标集成
- ✅ **自动验证**：定期完整性校验
- ✅ **AOP 集成**：注解式自动审计

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **写入延迟** | <10ms | 单条审计日志写入耗时 |
| **吞吐量** | >10K/s | 每秒处理的审计事件数 |
| **存储效率** | 节省60% | 压缩 + 批量写入 |
| **查询速度** | <100ms | 单次查询平均耗时 |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    审计系统架构                                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              应用层 (Application Layer)               │  │
│  │  ┌──────────────┐  ┌──────────────┐                 │  │
│  │  │ @Auditable  │  │  Manual API  │                 │  │
│  │  └──────┬───────┘  └──────┬───────┘                 │  │
│  └─────────┼─────────────────┼──────────────────────────┘  │
│            │                 │                              │
│            ▼                 ▼                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                AOP 切面层                             │  │
│  │  ┌───────────────────────────────────────────────┐   │  │
│  │  │        AuditAspect                           │   │  │
│  │  │  - 参数提取        - 异常捕获                │   │  │
│  │  │  - 用户识别        - 上下文采集              │   │  │
│  │  └───────────────────────────────────────────────┘   │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │                                  │
│                         ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              审计服务层 (Audit Service)               │  │
│  │                                                       │  │
│  │  ┌──────────────┐  ┌──────────────┐                  │  │
│  │  │ Hash Chain   │  │  Signature   │                  │  │
│  │  │  哈希链计算   │  │  数字签名    │                  │  │
│  │  └──────┬───────┘  └──────┬───────┘                  │  │
│  │         │                 │                          │  │
│  │         ▼                 ▼                          │  │
│  │  ┌───────────────────────────────────────────────┐  │  │
│  │  │          AuditService                        │  │  │
│  │  │  - 异步队列  - 批量写入  - 自动刷盘           │  │  │
│  │  └───────────────────────────────────────────────┘  │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │                                  │
│                         ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              存储层 (Storage Layer)                   │  │
│  │                                                       │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │  │
│  │  │ File Storage │  │ Redis Storage│  │ DB Storage  │ │  │
│  │  │   本地文件   │  │     Redis    │  │   数据库    │ │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘ │  │
│  │         │                 │                  │        │  │
│  │         └─────────┬───────┴────────┬────────┘        │  │
│  │                   │                │                 │  │
│  │                   ▼                ▼                 │  │
│  │         ┌────────────────────────────────────────┐  │  │
│  │         │      CompositeAuditStorage             │  │  │
│  │         │      - 多级存储  - 故障转移           │  │  │
│  │         └────────────────────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 添加依赖

确保 `basebackend-logging` 模块已正确引入。

### 2. 配置审计规则

在 `application.yml` 中配置：

```yaml
basebackend:
  logging:
    audit:
      enabled: true
      storage-path: /var/log/audit
      retention-days: 180
      batch-size: 500
      queue-capacity: 20000
      flush-interval: 500ms
      roll-size-bytes: 67108864
      encryption-key-base64: "ZHVtbXktMzItYnl0ZS1iYXNlNjQta2V5LWZvci1kZW1vLW9ubHk="
      signature-algorithm: "SHA256withRSA"
      hash-algorithm: "SHA-256"
      enable-compression: true
      enable-multi-tier-storage: false
```

### 3. 启用审计功能

```java
@SpringBootApplication
@EnableConfigurationProperties(AuditProperties.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 4. 使用 @Auditable 注解

```java
@Service
public class UserService {

    /**
     * 创建用户（自动审计）
     */
    @Auditable(
        value = AuditEventType.CREATE,
        resource = "用户管理",
        recordParams = true,
        recordResult = false
    )
    public User createUser(UserCreateRequest request) {
        // 业务逻辑
        return userRepository.save(user);
    }

    /**
     * 删除用户（高优先级审计）
     */
    @Auditable(
        value = AuditEventType.DELETE,
        resource = "用户管理",
        minSeverityLevel = 3  // 至少中级别才审计
    )
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
```

### 5. 手动记录审计日志

```java
@RestController
public class OrderController {

    @Autowired
    private AuditService auditService;

    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderCreateRequest request) {
        // 创建订单
        Order order = orderService.create(request);

        // 手动记录审计日志
        auditService.record(
            getCurrentUserId(),
            AuditEventType.CREATE,
            "订单管理",
            "SUCCESS",
            getClientIp(),
            getUserAgent(),
            "createOrder",
            order.getId().toString(),
            getSessionId(),
            Map.of("orderAmount", order.getAmount())
        );

        return order;
    }
}
```

## ⚙️ 配置参数详解

### 基础配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | true | 是否启用审计功能 |
| `storage-path` | String | logs/audit | 审计日志存储路径 |
| `retention-days` | int | 180 | 日志保留天数 |
| `batch-size` | int | 500 | 批量写入大小 |
| `queue-capacity` | int | 20000 | 异步队列容量 |
| `flush-interval` | Duration | 500ms | 刷盘间隔 |
| `io-threads` | int | 2 | I/O 线程数 |

### 安全配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `encryption-key-base64` | String | 必需 | AES-256 密钥（Base64） |
| `signature-algorithm` | String | SHA256withRSA | 数字签名算法 |
| `hash-algorithm` | String | SHA-256 | 哈希链算法 |

### 存储配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `roll-size-bytes` | long | 64MB | 文件滚动大小 |
| `roll-interval` | Duration | 1h | 文件滚动时间 |
| `enable-compression` | boolean | true | 是否启用压缩 |
| `enable-multi-tier-storage` | boolean | false | 是否启用多级存储 |

## 📊 审计事件类型

### 认证事件

- **LOGIN**：用户登录
- **LOGOUT**：用户登出
- **LOGIN_FAILED**：登录失败
- **PASSWORD_CHANGE**：密码修改
- **TOKEN_REFRESH**：令牌刷新

### 数据操作

- **CREATE**：数据创建
- **UPDATE**：数据更新
- **DELETE**：数据删除
- **BATCH_CREATE**：批量创建
- **BATCH_UPDATE**：批量更新
- **BATCH_DELETE**：批量删除

### 文件操作

- **UPLOAD**：文件上传
- **DOWNLOAD**：文件下载
- **EXPORT**：数据导出
- **IMPORT**：数据导入

### 安全事件

- **ACCESS_DENIED**：访问被拒绝
- **PRIVILEGE_ESCALATION**：权限提升
- **SUSPICIOUS_ACTIVITY**：可疑活动
- **SECURITY_VIOLATION**：安全违规

## 🔐 安全特性

### 1. 哈希链完整性保护

```java
// 哈希链自动维护
entry.setPrevHash(lastHash);
String entryHash = hashChainCalculator.computeHash(entry, lastHash);
entry.setEntryHash(entryHash);
lastHash = entryHash;
```

**特性**：
- 每条日志包含前一条的哈希值
- 形成不可篡改的链式结构
- 支持完整性验证和篡改检测

### 2. 数字签名

```java
// 自动签名
signatureService.sign(entry);

// 验证签名
boolean valid = signatureService.verify(entry);
```

**特性**：
- 使用 RSA/ECDSA 数字签名
- 支持密钥轮换
- 保证日志的不可抵赖性

### 3. 加密存储

```java
// AES-256-GCM 加密
String encrypted = aesEncryptor.encrypt(json);
```

**特性**：
- AES-256-GCM 加密算法
- 同时保证机密性和完整性
- 支持加密密钥管理

## 📊 监控指标

### 1. 通过代码获取指标

```java
@Autowired
private AuditService auditService;

public void printMetrics() {
    AuditQueueStatus status = auditService.getQueueStatus();
    System.out.println(status);
}
```

### 2. 通过 Micrometer 集成

```java
// 指标自动注册到 Micrometer
// 可通过 Prometheus/Grafana 监控

// 关键指标：
// - audit.events.total: 总审计事件数
// - audit.events.success: 成功事件数
// - audit.events.failure: 失败事件数
// - audit.latency: 操作延迟
// - audit.throughput: 吞吐量
```

### 3. 关键指标说明

| 指标 | 类型 | 说明 | 正常范围 |
|------|------|------|----------|
| `audit.events.total` | Counter | 总审计事件数 | 持续增长 |
| `audit.events.success` | Counter | 成功事件数 | 持续增长 |
| `audit.queue.current.size` | Gauge | 当前队列大小 | < 10000 |
| `audit.storage.errors` | Counter | 存储错误次数 | 持续增长 |
| `audit.latency` | Timer | 操作延迟 | < 10ms |

## 🔧 性能调优

### 1. 调整批量参数

```yaml
# 适合高吞吐量场景
batch-size: 1000
queue-capacity: 50000
flush-interval: 200ms

# 适合低延迟场景
batch-size: 200
queue-capacity: 10000
flush-interval: 100ms
```

### 2. 优化存储配置

```yaml
# 压缩存储（节省空间）
enable-compression: true
roll-size-bytes: 104857600  # 100MB

# 快速存储（减少延迟）
enable-compression: false
roll-size-bytes: 67108864   # 64MB
```

### 3. 多级存储

```yaml
enable-multi-tier-storage: true

# Redis 配置
redis:
  enabled: true
  host: redis.example.com
  port: 6379

# 数据库配置
database:
  enabled: true
  url: jdbc:mysql://db.example.com/audit
```

## 🛠️ 故障排查

### 常见问题

#### 1. 审计日志丢失

**检查**：
- 队列是否已满
- 存储是否正常
- 权限是否足够

**解决方案**：
```yaml
# 增加队列容量
queue-capacity: 50000

# 启用多级存储
enable-multi-tier-storage: true
```

#### 2. 性能问题

**原因**：
- 批量大小不合理
- 刷盘频率过高
- 存储 I/O 瓶颈

**解决方案**：
```yaml
# 调整批量大小
batch-size: 1000

# 延长刷盘间隔
flush-interval: 1000ms

# 启用压缩
enable-compression: true
```

#### 3. 验证失败

**原因**：
- 哈希链断裂
- 数字签名错误
- 存储损坏

**解决方案**：
```java
// 检查验证结果
VerificationReport report = verificationService.verifyChain(entries);
if (!report.isValid()) {
    log.error("验证失败: {}", report);
}
```

## 📝 最佳实践

### 1. 注解使用规范

```java
// ✅ 推荐：明确指定审计参数
@Auditable(
    value = AuditEventType.CREATE,
    resource = "用户管理",
    recordParams = true,
    recordException = true
)
public User createUser(UserCreateRequest request) { ... }

// ❌ 避免：过度审计低优先级操作
@Auditable(AuditEventType.LOGIN, recordResult = true)
public void login() { ... }
```

### 2. 敏感信息处理

```java
// ✅ 推荐：自动脱敏敏感参数
@Auditable(value = AuditEventType.UPDATE, recordParams = true)
public void updatePassword(String oldPassword, String newPassword) {
    // 密码参数自动脱敏
}

// ✅ 推荐：高危操作强制审计
@Auditable(
    value = AuditEventType.DELETE,
    minSeverityLevel = 3  // 至少中级别
)
public void deleteUser(Long userId) { ... }
```

### 3. 监控建议

- **必监控指标**：队列大小、延迟、错误率
- **告警阈值**：
  - 队列使用率 > 90%
  - 平均延迟 > 10ms
  - 错误率 > 5%

### 4. 合规要求

- **等保2.0三级**：必须记录所有重要操作
- **GDPR**：必须记录数据访问和修改
- **SOX**：必须记录财务相关操作
- **PCI DSS**：必须记录支付卡相关操作

## 🔗 相关资源

- [Spring AOP 文档](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [Java 安全编程指南](https://docs.oracle.com/en/java/security/)
- [哈希链技术详解](https://en.wikipedia.org/wiki/Hash_chain)
- [数字签名原理](https://en.wikipedia.org/wiki/Digital_signature)
- [basebackend-logging 主页](./README.md)

## 📄 许可证

本项目遵循 Apache License 2.0 许可证。

---

**更多详细信息和更新，请访问 [basebackend 项目主页](https://github.com/basebackend/basebackend)**
