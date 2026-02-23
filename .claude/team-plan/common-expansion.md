# Team Plan: common-expansion

## 概述

在 basebackend-common 聚合模块下新增 6 个子模块（masking, ratelimit, audit, event, tree, export），统一企业级通用能力，消除跨业务模块的碎片化重复实现。

## Codex 分析摘要

> Codex 不可用（OPENAI_API_KEY 未设置），用户已授权跳过多模型协作。

## Gemini 分析摘要

> Gemini 不可用（OAuth 认证过期），用户已授权跳过多模型协作。

## 技术方案

### 架构模式（统一遵循 common-storage 的 SPI 模式）

每个新子模块遵循相同的三层结构：
1. **注解层**：`@Annotation` + Retention(RUNTIME) + Target(METHOD/FIELD)
2. **SPI 层**：核心接口 + 默认实现
3. **集成层**：`*AutoConfiguration` + `*Properties` + 条件装配

### 自动配置策略

- 每个子模块内置独立的 `*AutoConfiguration`（参考 `StorageAutoConfiguration`）
- 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册（Spring Boot 3.x 规范）
- 配置前缀：`basebackend.common.{module-name}.*`
- 不修改 common-starter 的 CommonAutoConfiguration

### 依赖图

```
common-core ──→ common-masking   (Result, ErrorCode)
common-core ──→ common-ratelimit (Result, BusinessException)
common-core ──→ common-audit     (无直接模型依赖，仅用 core 基类)
common-core ──→ common-event     (无直接模型依赖，仅用 core 基类)
common-core ──→ common-tree      (无直接模型依赖)
common-core ──→ common-export    (Result)
common-util ──→ common-masking   (StringUtils)
```

无循环依赖，6 个新模块之间互不依赖。

## 子任务列表

---

### Task 1: 更新父 POM — 注册 6 个新子模块

- **类型**: 后端/构建
- **文件范围**:
  - `basebackend-common/pom.xml`（修改：添加 6 个 `<module>` + 6 个 `<dependencyManagement>` 条目）
- **依赖**: 无
- **实施步骤**:
  1. 在 `<modules>` 块末尾追加 6 个 module 声明：
     ```xml
     <module>basebackend-common-masking</module>
     <module>basebackend-common-ratelimit</module>
     <module>basebackend-common-audit</module>
     <module>basebackend-common-event</module>
     <module>basebackend-common-tree</module>
     <module>basebackend-common-export</module>
     ```
  2. 在 `<dependencyManagement>` 块末尾追加 6 个 dependency 声明（groupId=com.basebackend, version=${project.version}）
- **验收标准**: `pom.xml` 语法正确，`mvn validate -pl basebackend-common` 不报错

---

### Task 2: common-masking — 数据脱敏统一抽象

- **类型**: 后端
- **文件范围**: `basebackend-common/basebackend-common-masking/` (全部新建)
- **依赖**: Task 1
- **实施步骤**:
  1. 创建 `pom.xml`，parent=basebackend-common，依赖 common-core + common-util + jackson-databind + spring-boot-starter(optional) + lombok(provided) + spring-boot-starter-test(test)
  2. 创建包结构 `com.basebackend.common.masking`
  3. 创建 `MaskType.java` — 枚举：`PHONE, EMAIL, ID_CARD, BANK_CARD, ADDRESS, CUSTOM, NONE`
  4. 创建 `Mask.java` — 注解：
     ```java
     @Target(ElementType.FIELD)
     @Retention(RetentionPolicy.RUNTIME)
     @JacksonAnnotationsInside
     @JsonSerialize(using = MaskingJsonSerializer.class)
     public @interface Mask {
         MaskType value();
         String customPattern() default "";
         char maskChar() default '*';
     }
     ```
  5. 创建 `MaskingStrategy.java` — SPI 接口：`String mask(String value, char maskChar)`
  6. 创建 `impl/` 目录，实现 5 个默认策略：
     - `PhoneMaskingStrategy` — 保留前3后4，中间用maskChar
     - `EmailMaskingStrategy` — 保留首字母+@后域名
     - `IdCardMaskingStrategy` — 保留前3后4
     - `BankCardMaskingStrategy` — 保留后4位
     - `AddressMaskingStrategy` — 保留前6个字符
  7. 创建 `MaskingStrategyRegistry.java` — 策略注册中心（Map<MaskType, MaskingStrategy>），支持注册自定义策略
  8. 创建 `jackson/MaskingJsonSerializer.java` — 继承 `JsonSerializer<String>` + `ContextualSerializer`，从字段注解获取 MaskType 并调用对应策略
  9. 创建 `config/MaskingAutoConfiguration.java` — @AutoConfiguration + @EnableConfigurationProperties + 注册默认策略 Bean
  10. 创建 `config/MaskingProperties.java` — `@ConfigurationProperties("basebackend.common.masking")`，属性：enabled(默认true)
  11. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  12. 创建测试 `MaskingStrategyTest.java` — 验证 5 种默认策略的脱敏结果
- **文件清单**:
  ```
  pom.xml
  src/main/java/com/basebackend/common/masking/MaskType.java
  src/main/java/com/basebackend/common/masking/Mask.java
  src/main/java/com/basebackend/common/masking/MaskingStrategy.java
  src/main/java/com/basebackend/common/masking/MaskingStrategyRegistry.java
  src/main/java/com/basebackend/common/masking/impl/PhoneMaskingStrategy.java
  src/main/java/com/basebackend/common/masking/impl/EmailMaskingStrategy.java
  src/main/java/com/basebackend/common/masking/impl/IdCardMaskingStrategy.java
  src/main/java/com/basebackend/common/masking/impl/BankCardMaskingStrategy.java
  src/main/java/com/basebackend/common/masking/impl/AddressMaskingStrategy.java
  src/main/java/com/basebackend/common/masking/jackson/MaskingJsonSerializer.java
  src/main/java/com/basebackend/common/masking/config/MaskingAutoConfiguration.java
  src/main/java/com/basebackend/common/masking/config/MaskingProperties.java
  src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/com/basebackend/common/masking/MaskingStrategyTest.java
  ```
- **验收标准**: `@Mask(MaskType.PHONE)` 标注字段后，Jackson 序列化输出脱敏结果；5 种策略单元测试通过

---

### Task 3: common-ratelimit — 限流抽象层

- **类型**: 后端
- **文件范围**: `basebackend-common/basebackend-common-ratelimit/` (全部新建)
- **依赖**: Task 1
- **实施步骤**:
  1. 创建 `pom.xml`，依赖 common-core + spring-boot-starter-aop + lombok(provided) + spring-boot-starter-test(test)
  2. 创建包结构 `com.basebackend.common.ratelimit`
  3. 创建 `RateLimit.java` — 注解：
     ```java
     @Target(ElementType.METHOD)
     @Retention(RetentionPolicy.RUNTIME)
     public @interface RateLimit {
         String key() default "";        // 限流key，支持SpEL
         int limit() default 100;        // 窗口内允许请求数
         int window() default 60;        // 窗口大小（秒）
         String fallbackMethod() default ""; // 降级方法名
         String message() default "请求过于频繁";
     }
     ```
  4. 创建 `RateLimiter.java` — SPI 接口：`boolean tryAcquire(String key, int limit, int windowSeconds)`
  5. 创建 `RateLimitExceededException.java` — 继承 BusinessException
  6. 创建 `impl/SlidingWindowRateLimiter.java` — 基于 ConcurrentHashMap + 时间戳队列的本地滑动窗口实现
  7. 创建 `aspect/RateLimitAspect.java` — AOP 切面，拦截 @RateLimit 注解方法，调用 RateLimiter SPI，超限时抛异常或调降级方法
  8. 创建 `config/RateLimitAutoConfiguration.java` — 注册默认 SlidingWindowRateLimiter Bean（@ConditionalOnMissingBean）
  9. 创建 `config/RateLimitProperties.java` — `@ConfigurationProperties("basebackend.common.ratelimit")`
  10. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  11. 创建测试 `SlidingWindowRateLimiterTest.java`
- **文件清单**:
  ```
  pom.xml
  src/main/java/com/basebackend/common/ratelimit/RateLimit.java
  src/main/java/com/basebackend/common/ratelimit/RateLimiter.java
  src/main/java/com/basebackend/common/ratelimit/RateLimitExceededException.java
  src/main/java/com/basebackend/common/ratelimit/impl/SlidingWindowRateLimiter.java
  src/main/java/com/basebackend/common/ratelimit/aspect/RateLimitAspect.java
  src/main/java/com/basebackend/common/ratelimit/config/RateLimitAutoConfiguration.java
  src/main/java/com/basebackend/common/ratelimit/config/RateLimitProperties.java
  src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/com/basebackend/common/ratelimit/SlidingWindowRateLimiterTest.java
  ```
- **验收标准**: `@RateLimit(limit=5, window=60)` 标注方法后，第 6 次调用抛 RateLimitExceededException；滑动窗口单元测试通过

---

### Task 4: common-audit — 审计日志统一框架

- **类型**: 后端
- **文件范围**: `basebackend-common/basebackend-common-audit/` (全部新建)
- **依赖**: Task 1
- **实施步骤**:
  1. 创建 `pom.xml`，依赖 common-core + spring-boot-starter-aop + spring-context + lombok(provided) + spring-boot-starter-test(test)
  2. 创建包结构 `com.basebackend.common.audit`
  3. 创建 `AuditLog.java` — 注解：
     ```java
     @Target(ElementType.METHOD)
     @Retention(RetentionPolicy.RUNTIME)
     public @interface AuditLog {
         String module() default "";
         String action();                  // 操作类型：CREATE/UPDATE/DELETE/QUERY/EXPORT/LOGIN
         String description() default "";  // 支持SpEL：如 "删除用户 #{#id}"
         boolean recordParams() default true;
         boolean recordResult() default false;
     }
     ```
  4. 创建 `AuditEvent.java` — 审计事件模型（eventId, module, action, description, operator, operatorIp, params, result, timestamp, duration）
  5. 创建 `AuditEventPublisher.java` — 接口：`void publish(AuditEvent event)`
  6. 创建 `AuditEventListener.java` — SPI 接口：`void onAuditEvent(AuditEvent event)`，由各业务模块实现持久化
  7. 创建 `impl/SpringAuditEventPublisher.java` — 基于 Spring `ApplicationEventPublisher` 发布事件
  8. 创建 `aspect/AuditLogAspect.java` — AOP 切面：
     - 拦截 @AuditLog 方法
     - SpEL 解析 description 表达式
     - 从 UserContextHolder 获取操作人信息
     - 记录方法执行时间
     - 调用 AuditEventPublisher 发布事件
  9. 创建 `config/AuditAutoConfiguration.java`
  10. 创建 `config/AuditProperties.java` — `@ConfigurationProperties("basebackend.common.audit")`
  11. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  12. 创建测试 `AuditLogAspectTest.java` — 验证 SpEL 解析和事件发布
- **文件清单**:
  ```
  pom.xml
  src/main/java/com/basebackend/common/audit/AuditLog.java
  src/main/java/com/basebackend/common/audit/AuditEvent.java
  src/main/java/com/basebackend/common/audit/AuditEventPublisher.java
  src/main/java/com/basebackend/common/audit/AuditEventListener.java
  src/main/java/com/basebackend/common/audit/impl/SpringAuditEventPublisher.java
  src/main/java/com/basebackend/common/audit/aspect/AuditLogAspect.java
  src/main/java/com/basebackend/common/audit/config/AuditAutoConfiguration.java
  src/main/java/com/basebackend/common/audit/config/AuditProperties.java
  src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/com/basebackend/common/audit/AuditLogAspectTest.java
  ```
- **验收标准**: `@AuditLog(module="user", action="DELETE", description="删除用户 #{#id}")` 标注方法后，执行时自动发布 AuditEvent；SpEL 表达式正确解析

---

### Task 5: common-event — 统一领域事件抽象

- **类型**: 后端
- **文件范围**: `basebackend-common/basebackend-common-event/` (全部新建)
- **依赖**: Task 1
- **实施步骤**:
  1. 创建 `pom.xml`，依赖 common-core + spring-context + lombok(provided) + spring-boot-starter-test(test)
  2. 创建包结构 `com.basebackend.common.event`
  3. 创建 `DomainEvent.java` — 基类：
     ```java
     @Data
     public abstract class DomainEvent {
         private final String eventId;
         private final String eventType;
         private final LocalDateTime timestamp;
         private final String source;
     }
     ```
  4. 创建 `DomainEventPublisher.java` — 接口：`void publish(DomainEvent event)`
  5. 创建 `DomainEventListener.java` — 标记注解（@Target(METHOD), @EventListener 的语义别名）
  6. 创建 `impl/SpringDomainEventPublisher.java` — 基于 `ApplicationEventPublisher` 的默认实现
  7. 创建 `config/EventAutoConfiguration.java`
  8. 创建 `config/EventProperties.java` — `@ConfigurationProperties("basebackend.common.event")`
  9. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  10. 创建测试 `DomainEventPublisherTest.java`
- **文件清单**:
  ```
  pom.xml
  src/main/java/com/basebackend/common/event/DomainEvent.java
  src/main/java/com/basebackend/common/event/DomainEventPublisher.java
  src/main/java/com/basebackend/common/event/DomainEventListener.java
  src/main/java/com/basebackend/common/event/impl/SpringDomainEventPublisher.java
  src/main/java/com/basebackend/common/event/config/EventAutoConfiguration.java
  src/main/java/com/basebackend/common/event/config/EventProperties.java
  src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/com/basebackend/common/event/DomainEventPublisherTest.java
  ```
- **验收标准**: 自定义 DomainEvent 子类可通过 DomainEventPublisher 发布，@DomainEventListener 标注的方法可接收事件

---

### Task 6: common-tree — 树形结构工具

- **类型**: 后端
- **文件范围**: `basebackend-common/basebackend-common-tree/` (全部新建)
- **依赖**: Task 1
- **实施步骤**:
  1. 创建 `pom.xml`，依赖 common-core + lombok(provided) + spring-boot-starter-test(test)（最轻量，无 Spring 依赖）
  2. 创建包结构 `com.basebackend.common.tree`
  3. 创建 `TreeNode.java` — 泛型接口：
     ```java
     public interface TreeNode<T> {
         T getId();
         T getParentId();
         List<? extends TreeNode<T>> getChildren();
         void setChildren(List<? extends TreeNode<T>> children);
         default Integer getSort() { return 0; }
     }
     ```
  4. 创建 `TreeField.java` — 注解（标记 id/parentId/children/sort 字段，用于反射构建）
  5. 创建 `TreeBuilder.java` — 工具类：
     - `<T extends TreeNode<ID>, ID> List<T> buildTree(List<T> nodes)` — 列表 → 树
     - `<T extends TreeNode<ID>, ID> List<T> flatten(List<T> roots)` — 树 → 扁平列表
     - `<T extends TreeNode<ID>, ID> T findNode(List<T> roots, ID id)` — 在树中查找节点
     - `<T extends TreeNode<ID>, ID> List<T> findPath(List<T> roots, ID id)` — 查找根到目标节点的路径
     - `<T extends TreeNode<ID>, ID> List<T> findChildren(List<T> roots, ID parentId)` — 查找所有子孙节点
  6. 创建 `SimpleTreeNode.java` — 默认 TreeNode 实现（id, parentId, label, children, sort）
  7. 创建测试 `TreeBuilderTest.java` — 验证建树、扁平化、查找、路径追溯
- **文件清单**:
  ```
  pom.xml
  src/main/java/com/basebackend/common/tree/TreeNode.java
  src/main/java/com/basebackend/common/tree/TreeField.java
  src/main/java/com/basebackend/common/tree/TreeBuilder.java
  src/main/java/com/basebackend/common/tree/SimpleTreeNode.java
  src/test/java/com/basebackend/common/tree/TreeBuilderTest.java
  ```
- **验收标准**: `TreeBuilder.buildTree(flatList)` 正确构建多级树；路径追溯和子树查找单元测试通过

---

### Task 7: common-export — 数据导入导出

- **类型**: 后端
- **文件范围**: `basebackend-common/basebackend-common-export/` (全部新建)
- **依赖**: Task 1
- **实施步骤**:
  1. 创建 `pom.xml`，依赖 common-core + spring-boot-starter(optional) + lombok(provided) + commons-csv(optional) + easyexcel(optional) + spring-boot-starter-test(test)
  2. 创建包结构 `com.basebackend.common.export`
  3. 创建 `ExportField.java` — 注解：
     ```java
     @Target(ElementType.FIELD)
     @Retention(RetentionPolicy.RUNTIME)
     public @interface ExportField {
         String label();             // 列标题
         int order() default 0;      // 列顺序
         String format() default ""; // 格式化模式（如日期格式）
         String converter() default ""; // 自定义转换器 Bean 名
     }
     ```
  4. 创建 `ExportFormat.java` — 枚举：`CSV, XLSX`
  5. 创建 `ExportResult.java` — 导出结果模型（fileName, contentType, content as byte[] or InputStream）
  6. 创建 `ImportResult.java` — 导入结果模型（totalRows, successRows, failedRows, errors as List）
  7. 创建 `ExportService.java` — SPI 接口：
     ```java
     public interface ExportService {
         ExportFormat supportedFormat();
         <T> ExportResult export(List<T> data, Class<T> clazz);
         <T> ExportResult exportStreaming(Iterator<T> data, Class<T> clazz); // 流式导出
     }
     ```
  8. 创建 `ImportService.java` — SPI 接口：
     ```java
     public interface ImportService {
         ExportFormat supportedFormat();
         <T> ImportResult importData(InputStream input, Class<T> clazz);
     }
     ```
  9. 创建 `FieldConverter.java` — 转换器接口：`String convert(Object value)`
  10. 创建 `impl/CsvExportService.java` — 基于 commons-csv 的 CSV 导出实现（@ConditionalOnClass）
  11. 创建 `impl/CsvImportService.java` — 基于 commons-csv 的 CSV 导入实现
  12. 创建 `config/ExportAutoConfiguration.java`
  13. 创建 `config/ExportProperties.java` — `@ConfigurationProperties("basebackend.common.export")`
  14. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  15. 创建测试 `CsvExportServiceTest.java`
- **文件清单**:
  ```
  pom.xml
  src/main/java/com/basebackend/common/export/ExportField.java
  src/main/java/com/basebackend/common/export/ExportFormat.java
  src/main/java/com/basebackend/common/export/ExportResult.java
  src/main/java/com/basebackend/common/export/ImportResult.java
  src/main/java/com/basebackend/common/export/ExportService.java
  src/main/java/com/basebackend/common/export/ImportService.java
  src/main/java/com/basebackend/common/export/FieldConverter.java
  src/main/java/com/basebackend/common/export/impl/CsvExportService.java
  src/main/java/com/basebackend/common/export/impl/CsvImportService.java
  src/main/java/com/basebackend/common/export/config/ExportAutoConfiguration.java
  src/main/java/com/basebackend/common/export/config/ExportProperties.java
  src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/com/basebackend/common/export/CsvExportServiceTest.java
  ```
- **验收标准**: `@ExportField` 标注实体字段后，ExportService 可正确导出 CSV；导入可解析 CSV 并返回 ImportResult

---

## 文件冲突检查

✅ **无冲突**

| Task | 文件范围 | 冲突? |
|------|---------|-------|
| Task 1 | `basebackend-common/pom.xml` (唯一修改点) | ❌ 独占 |
| Task 2 | `basebackend-common/basebackend-common-masking/**` | ❌ 独立目录 |
| Task 3 | `basebackend-common/basebackend-common-ratelimit/**` | ❌ 独立目录 |
| Task 4 | `basebackend-common/basebackend-common-audit/**` | ❌ 独立目录 |
| Task 5 | `basebackend-common/basebackend-common-event/**` | ❌ 独立目录 |
| Task 6 | `basebackend-common/basebackend-common-tree/**` | ❌ 独立目录 |
| Task 7 | `basebackend-common/basebackend-common-export/**` | ❌ 独立目录 |

## 并行分组

```
Layer 1 (串行): Task 1 — 更新父 POM（前置依赖）
Layer 2 (6路并行): Task 2, Task 3, Task 4, Task 5, Task 6, Task 7
```

## Builder 分配建议

- **Builder 数量**: 建议 3-4 个（受限于 API 并发和上下文窗口）
- **分配策略**:
  - Builder A: Task 2 (masking) → Task 5 (event)
  - Builder B: Task 3 (ratelimit) → Task 6 (tree)
  - Builder C: Task 4 (audit) → Task 7 (export)
  - Task 1 由 Team Lead 直接完成（简单 POM 修改，1 个文件）

## 估算文件数

| 子模块 | 源文件 | 测试文件 | 配置文件 | 合计 |
|--------|--------|---------|---------|------|
| parent pom | 1 (修改) | 0 | 0 | 1 |
| masking | 12 | 1 | 1 | 14 |
| ratelimit | 7 | 1 | 1 | 9 |
| audit | 8 | 1 | 1 | 10 |
| event | 6 | 1 | 1 | 8 |
| tree | 4 | 1 | 0 | 5 |
| export | 10 | 1 | 1 | 12 |
| **总计** | **48** | **6** | **5** | **59** |
