# PII数据脱敏增强器

## 📖 概述

PII数据脱敏增强器是专为 basebackend-logging 模块设计的数据安全组件，通过自动识别和脱敏敏感信息，有效防止个人信息泄露，满足合规要求。

### 核心特性

- ✅ **多种敏感数据类型**：支持手机号、身份证、银行卡、邮箱等
- ✅ **多种脱敏策略**：掩码、部分、哈希、移除、自定义
- ✅ **多维度匹配**：正则表达式 + JSON路径
- ✅ **高性能**：处理时间<5ms/条，准确率>99%
- ✅ **自动脱敏**：注解式使用 + 日志输出自动脱敏
- ✅ **配置灵活**：支持自定义规则和热更新

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **处理时间** | <5ms/条 | 单条日志脱敏耗时 |
| **准确率** | >99% | 敏感信息识别准确率 |
| **内存使用** | 可控 | 预编译正则，避免频繁对象创建 |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                PII脱敏系统架构                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  Application Layer                   │  │
│  │  ┌──────────────┐  ┌──────────────┐                 │  │
│  │  │ @DataMasking │  │   Log Output │                 │  │
│  │  └──────┬───────┘  └──────┬───────┘                 │  │
│  └─────────┼─────────────────┼──────────────────────────┘  │
│            │                 │                              │
│            ▼                 ▼                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │               AOP 切面层                              │  │
│  │  ┌───────────────────────────────────────────────┐   │  │
│  │  │  PiiMaskingAspect                           │   │  │
│  │  │  - 解析@DataMasking注解                      │   │  │
│  │  │  - 调用脱敏服务                              │   │  │
│  │  └───────────────────────────────────────────────┘   │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │                                  │
│                         ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              PiiMaskingService                       │  │
│  │                                                       │  │
│  │  ┌───────────────────┐  ┌──────────────────────┐     │  │
│  │  │  正则匹配引擎      │  │   JSON路径匹配引擎   │     │  │
│  │  │  - 手机号         │  │   - user.phone       │     │  │
│  │  │  - 身份证         │  │   - order.creditCard │     │  │
│  │  │  - 银行卡         │  │   - customer.email   │     │  │
│  │  │  - 邮箱           │  │                      │     │  │
│  │  └───────┬───────────┘  └───────────┬─────────┘     │  │
│  │          │                          │               │  │
│  │          ▼                          ▼               │  │
│  │  ┌───────────────────────────────────────────────┐  │  │
│  │  │        脱敏策略执行器                          │  │  │
│  │  │  ┌────────┐ ┌────────┐ ┌────────┐            │  │  │
│  │  │  │ MASK  │ │ PARTIAL│ │ HASH   │            │  │  │
│  │  │  │ 掩码  │ │ 部分   │ │ 哈希   │            │  │  │
│  │  │  └────────┘ └────────┘ └────────┘            │  │  │
│  │  └───────────────────────────────────────────────┘  │  │
│  └──────────────────────┬──────────────────────────────┘  │
│                         │                                  │
│                         ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                 数据层                                │  │
│  │                                                       │  │
│  │  原始数据               脱敏后数据                     │  │
│  │  ┌──────────┐          ┌──────────┐                  │  │
│  │  │13812345678│    →    │138****5678│                  │  │
│  │  └──────────┘          └──────────┘                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 添加依赖

确保 `basebackend-logging` 模块已正确引入。

### 2. 配置脱敏规则

在 `application.yml` 中配置：

```yaml
basebackend:
  logging:
    masking:
      enabled: true
      slow-threshold-millis: 5
      ignore-loggers:
        - "org.hibernate.SQL"
      rules:
        - name: phone
          regex: "\\b1\\d{10}\\b"
          strategy: PARTIAL
          prefix-keep: 3
          suffix-keep: 2
        - name: email
          regex: "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+"
          strategy: MASK
          prefix-keep: 2
          suffix-keep: 6
        - name: card
          regex: "\\b\\d{16,19}\\b"
          strategy: PARTIAL
          prefix-keep: 4
          suffix-keep: 4
        - name: nested-phone
          json-path: "user.contact.phone"
          strategy: PARTIAL
          prefix-keep: 3
          suffix-keep: 2
```

### 3. 配置Logback输出脱敏

在 `logback-spring.xml` 中添加转换规则：

```xml
<configuration>
    <!-- 声明脱敏转换器 -->
    <conversionRule conversionWord="maskMsg"
                    converterClass="com.basebackend.logging.masking.MaskingMessageConverter"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 使用脱敏转换器 -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger - %maskMsg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### 4. 使用注解脱敏

```java
@Service
public class UserService {

    /**
     * 查询用户信息（自动脱敏）
     */
    @DataMasking
    public UserInfo getUserInfo(Long userId) {
        // 返回包含敏感信息的用户对象
        return userRepository.findById(userId);
    }

    /**
     * 查询用户列表（部分脱敏规则）
     */
    @DataMasking(ruleNames = {"phone", "email"})
    public List<UserInfo> getUserList() {
        return userRepository.findAll();
    }

    /**
     * 获取脱敏用户日志（跳过null值）
     */
    @DataMasking(skipNull = true)
    public UserLog getUserLog(Long userId) {
        return userLogRepository.findByUserId(userId);
    }
}
```

## ⚙️ 配置参数详解

### 基础配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | true | 是否启用脱敏功能 |
| `slow-threshold-millis` | long | 5 | 慢脱敏阈值（毫秒），超过此值会记录警告 |
| `ignore-loggers` | List<String> | [] | 忽略的日志器名称 |

### 脱敏规则配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | **必需** | 规则名称 |
| `regex` | String | null | 正则表达式（字符串匹配） |
| `json-path` | String | null | JSON路径（对象字段匹配） |
| `strategy` | Enum | MASK | 脱敏策略 |
| `replacement` | String | "*" | 替换字符 |
| `prefix-keep` | int | 3 | 保留前缀字符数 |
| `suffix-keep` | int | 2 | 保留后缀字符数 |
| `enabled` | boolean | true | 是否启用规则 |

## 📊 脱敏策略详解

### MASK（掩码）

**行为**：全部替换为指定字符

**示例**：
```
原值：13812345678
脱敏：**********
```

**配置**：
```yaml
strategy: MASK
replacement: "*"
```

### PARTIAL（部分保留）

**行为**：保留前缀和后缀，中间部分脱敏

**示例**：
```
原值：13812345678
脱敏：138****5678
```

**配置**：
```yaml
strategy: PARTIAL
prefix-keep: 3
suffix-keep: 4
replacement: "*"
```

### HASH（哈希）

**行为**：使用SHA-256哈希值替换原值

**示例**：
```
原值：13812345678
脱敏：a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3
```

**配置**：
```yaml
strategy: HASH
```

### REMOVE（移除）

**行为**：完全移除敏感字段

**示例**：
```
原值：13812345678
脱敏：（空字符串）
```

**配置**：
```yaml
strategy: REMOVE
```

### CUSTOM（自定义）

**行为**：使用自定义字符串替换

**示例**：
```
原值：13812345678
脱敏：已脱敏
```

**配置**：
```yaml
strategy: CUSTOM
replacement: "已脱敏"
```

## 🔧 高级用法

### 1. JSON路径脱敏

```java
public class OrderInfo {
    private UserInfo user;  // 包含敏感信息
    private String orderId;
    private BigDecimal amount;
}

// 在配置中指定JSON路径
rules:
  - name: "user-phone"
    json-path: "user.phone"
    strategy: PARTIAL
    prefix-keep: 3
    suffix-keep: 2
```

### 2. 自定义规则

```yaml
rules:
  - name: "custom-sensitive"
    regex: "secret:\\w+"
    strategy: CUSTOM
    replacement: "secret:[已脱敏]"
```

### 3. 多规则组合

```java
@DataMasking(ruleNames = {"phone", "email", "id-card"})
public UserInfo getUserInfo(Long userId) {
    return userRepository.findById(userId);
}
```

## 📊 监控指标

### 1. 通过代码获取指标

```java
@Autowired
private MaskingProperties properties;

@Autowired
private PiiMaskingService service;

public void printMetrics() {
    MaskingMetrics metrics = service.getMetrics();
    System.out.println(metrics.getSummary());
}
```

### 2. 通过 Micrometer 集成

```java
@Component
public class MaskingMetrics {

    private final PiiMaskingService service;

    public MaskingMetrics(PiiMaskingService service) {
        this.service = service;
        // 注册指标
        MeterRegistry.registry.gauge("logging.masking.rate",
            service, s -> s.getMetrics().getMaskingRate());
        MeterRegistry.registry.gauge("logging.masking.avg.micros",
            service, s -> (double) s.getMetrics().getAvgMicros());
    }
}
```

### 3. 关键指标说明

| 指标 | 类型 | 说明 | 正常范围 |
|------|------|------|----------|
| `masked` | Counter | 已脱敏次数 | 持续增长 |
| `hits` | Counter | 总请求数 | 持续增长 |
| `avgMicros` | Gauge | 平均脱敏时间（微秒） | < 5000 |
| `maskingRate` | Gauge | 脱敏率（百分比） | 0-100% |

## 🔧 性能调优

### 1. 调整规则优先级

将最常用的规则放在前面，提高匹配效率：

```yaml
rules:
  # 最常用的规则放前面
  - name: "phone"      # 高频匹配
    regex: "\\b1\\d{10}\\b"
    strategy: PARTIAL

  - name: "email"      # 中频匹配
    regex: "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+"
    strategy: MASK
```

### 2. 优化正则表达式

**✅ 推荐**：
```yaml
# 使用精确匹配
regex: "\\b1\\d{10}\\b"
```

**❌ 避免**：
```yaml
# 避免贪婪匹配
regex: "1.*\\d{10}.*"
```

### 3. 禁用不必要的规则

```yaml
# 如果不需要JSON路径脱敏，可以移除
rules:
  # 仅使用正则脱敏
  - name: "phone"
    regex: "\\b1\\d{10}\\b"
    strategy: PARTIAL
```

## 🛠️ 故障排查

### 常见问题

#### 1. 脱敏不生效

**检查**：
- 配置是否正确加载
- 注解是否添加
- 服务是否启动

**解决方案**：
```yaml
# 启用DEBUG日志
logging:
  level:
    com.basebackend.logging.masking: DEBUG
```

#### 2. 性能问题

**原因**：
- 正则表达式过于复杂
- 规则数量过多
- 单次处理数据量过大

**解决方案**：
```yaml
# 优化正则表达式
# 减少规则数量
# 调整slow-threshold-millis
```

#### 3. 内存使用过高

**解决方案**：
```yaml
# 减少规则复杂度
# 控制日志大小
# 适当调整MAX_BUFFER
```

## 📝 最佳实践

### 1. 规则命名规范

```yaml
# ✅ 推荐：使用有意义的名称
- name: "phone"
- name: "id-card"
- name: "bank-card"

# ❌ 避免：使用简单代号
- name: "rule1"
- name: "rule2"
```

### 2. 策略选择原则

- **手机号/身份证/银行卡**：使用PARTIAL策略
- **邮箱**：使用MASK策略
- **密码/密钥**：使用HASH策略
- **极度敏感信息**：使用REMOVE策略

### 3. 监控建议

- **必监控指标**：avgMicros、maskingRate
- **告警阈值**：
  - avgMicros > 5000μs
  - maskingRate < 10%（可能规则未生效）

### 4. 合规要求

- **等保2.0**：必须对个人信息进行脱敏
- **GDPR**：必须对欧盟居民信息脱敏
- **PCI DSS**：必须对支付卡信息脱敏

## 🔗 相关资源

- [Spring AOP 文档](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [正则表达式详解](https://www.oracle.com/technical-resources/articles/java/regex.html)
- [basebackend-logging 主页](./README.md)

## 📄 许可证

本项目遵循 Apache License 2.0 许可证。

---

**更多详细信息和更新，请访问 [basebackend 项目主页](https://github.com/basebackend/basebackend)**
