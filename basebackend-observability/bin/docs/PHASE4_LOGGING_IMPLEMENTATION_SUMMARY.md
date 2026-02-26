# Phase 4: 日志系统增强 - 实施总结

## 已完成的组件

### 1. 配置系统 (2个)
- `LoggingProperties.java` - 日志配置属性（格式、脱敏、采样、路由）
- `LoggingAutoConfiguration.java` - Spring Boot 自动配置

### 2. 日志格式增强 (2个)
- `LogAttributeEnricher.java` - MDC 属性填充器（traceId/spanId/业务上下文）
- `MaskingConverter.java` - Logback 脱敏转换器（支持 PARTIAL/HIDE/HASH 策略）

### 3. 日志采样 (1个)
- `LogSamplingTurboFilter.java` - Logback TurboFilter，按级别/包名采样

### 4. 日志路由 (1个)
- `LogRoutingAppender.java` - Logback Appender，多目标路由

## 使用方式

### 1. 基础配置

```yaml
observability:
  logging:
    enabled: true

    # 日志格式
    format:
      type: json                        # json/text
      include-trace-context: true       # 包含 traceId/spanId
      include-business-context: true    # 包含 tenantId/userId
      timestamp-format: "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    # 敏感信息脱敏
    masking:
      enabled: true
      rules:
        - field-pattern: "creditCard|cardNumber"
          strategy: PARTIAL
          partial-pattern: "4-4"        # 保留前4后4位
        - field-pattern: "ssn|socialSecurity"
          strategy: HASH                # SHA-256 哈希
        - field-pattern: "apiKey|secret"
          strategy: HIDE                # 完全隐藏

    # 日志采样
    sampling:
      enabled: true
      rules:
        - level: ERROR
          rate: 1.0                     # ERROR 100% 采样
        - level: WARN
          rate: 1.0                     # WARN 100% 采样
        - level: INFO
          rate: 0.1                     # INFO 10% 采样
        - level: DEBUG
          rate: 0.01                    # DEBUG 1% 采样
        - level: INFO
          rate: 0.5
          package-name: com.basebackend.user  # 指定包 50% 采样

    # 日志路由
    routing:
      enabled: true
      destinations:
        - name: console
          type: console
          enabled: true
          level: INFO
        - name: error-file
          type: file
          enabled: true
          level: ERROR
          path: /var/log/app/error.log
        - name: audit
          type: file
          enabled: true
          level: INFO
          category: AUDIT               # 仅路由审计日志
          path: /var/log/app/audit.log
```

### 2. 在 Filter 中使用 LogAttributeEnricher

```java
@Component
public class TracingFilter implements Filter {

    @Autowired
    private LogAttributeEnricher enricher;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 填充追踪上下文到 MDC
            enricher.enrichFromCurrentSpan();

            // 填充业务上下文
            HttpServletRequest httpReq = (HttpServletRequest) req;
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Tenant-Id", httpReq.getHeader("X-Tenant-Id"));
            headers.put("X-User-Id", httpReq.getHeader("X-User-Id"));
            enricher.enrichBusinessContext(headers);

            chain.doFilter(req, res);
        } finally {
            // 清理 MDC（仅清理本类管理的键）
            enricher.clearAll();
        }
    }
}
```

### 3. 在 logback-spring.xml 中配置脱敏转换器

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 注册脱敏转换器 -->
    <conversionRule conversionWord="maskedMsg"
                    converterClass="com.basebackend.observability.logging.masking.MaskingConverter" />

    <!-- 控制台输出（使用脱敏后的消息） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %maskedMsg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

### 4. 标记审计日志类别

```java
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void logAuditEvent(String action, String resource) {
        try {
            // 标记日志类别为 AUDIT
            LogRoutingAppender.setCategory("AUDIT");
            log.info("审计事件: action={}, resource={}", action, resource);
        } finally {
            // 清除类别标记
            LogRoutingAppender.clearCategory();
        }
    }
}
```

## 脱敏策略说明

| 策略 | 说明 | 示例 |
|------|------|------|
| PARTIAL | 部分显示 | 13800138000 → 138****8000 |
| HIDE | 完全隐藏 | password=xxx → password=****** |
| HASH | SHA-256 哈希 | sensitive → SHA256(sensitive) |

### 预置脱敏规则

- 手机号: `138****8000` (前3后4)
- 身份证: `110101********1234` (前6后4)
- 邮箱: `u***@example.com` (首字符+域名)
- 密码: `password=******` (完全隐藏)
- 银行卡: `6222********7890` (前4后4)

## 采样策略说明

采样优先级：**包名+级别 > 级别 > 默认 1.0**

```yaml
# 示例：对 user 模块的 INFO 日志进行 50% 采样，其他 INFO 日志 10% 采样
sampling:
  rules:
    - level: INFO
      rate: 0.5
      package-name: com.basebackend.user  # 高优先级
    - level: INFO
      rate: 0.1                           # 低优先级
```

## 路由目标类型

| 类型 | 说明 | 必需配置 |
|------|------|----------|
| console | 控制台输出 | - |
| file | 文件输出 | path |
| loki | Grafana Loki | url |

## 文件清单

```
basebackend-observability/src/main/java/com/basebackend/observability/logging/
├── config/
│   ├── LoggingProperties.java          # 配置属性
│   └── LoggingAutoConfiguration.java   # 自动配置
├── format/
│   └── LogAttributeEnricher.java       # MDC 填充器
├── masking/
│   └── MaskingConverter.java           # 脱敏转换器
├── sampling/
│   └── LogSamplingTurboFilter.java     # 采样过滤器
└── routing/
    └── LogRoutingAppender.java         # 路由 Appender
```

## 验证结果

```
[INFO] BUILD SUCCESS
[INFO] Compiling 76 source files
```

所有组件已通过编译验证。
