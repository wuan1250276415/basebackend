# basebackend-logging 模块改进执行计划

> 生成时间: 2026-02-21
> 状态: 待审批
> 来源: spec-research + spec-plan 分析

---

## 1. 模块现状

### 1.1 模块结构

```
basebackend-logging/
├── audit/           # 审计日志 (哈希链+签名+AES-256-GCM加密)
│   ├── config/      # AuditAutoConfiguration, AuditProperties
│   ├── crypto/      # AesEncryptor, HashChainCalculator, AuditSignatureService
│   ├── metrics/     # AuditMetrics
│   ├── model/       # AuditLogEntry
│   ├── service/     # AuditService, AuditVerificationService
│   ├── storage/     # AuditStorage, FileAuditStorage, CompositeAuditStorage
│   ├── aspect/      # AuditAspect
│   └── annotation/  # @Auditable
├── masking/         # PII 数据脱敏
├── cache/           # Redis 热日志缓存
├── monitoring/      # Prometheus/Grafana 集成
├── statistics/      # 日志统计引擎 (趋势预测、模式分析)
├── appender/        # AsyncBatchAppender
├── rollover/        # AsyncGzipSizeAndTimeRollingPolicy
├── configcenter/    # Nacos/Apollo 动态配置
├── benchmark/       # 性能基准测试
├── aspect/          # WebLogAspect, OperationLogAspect
├── config/          # LoggingAutoConfiguration (空), LoggingUnifiedProperties
├── context/         # LogContext
├── filter/          # LogContextFilter
├── annotation/      # @OperationLog
├── model/           # OperationLogInfo
└── service/         # OperationLogService
```

**源文件数**: ~90+
**测试文件数**: 7 (覆盖率极低)

### 1.2 依赖关系

**被以下 7 个模块直接依赖**:

| # | 模块 | 风险等级 |
|---|------|---------|
| 1 | `basebackend-admin-api` | 高 (主服务) |
| 2 | `basebackend-system-api` | 高 |
| 3 | `basebackend-user-api` | 高 |
| 4 | `basebackend-notification-service` | 中 |
| 5 | `basebackend-observability-service` | 中 |
| 6 | `basebackend-scheduler-parent/scheduler-integration` | 中 |
| 7 | `basebackend-code-generator` | 低 |

**注**: `basebackend-scheduler-old` 和 `basebackend-scheduler-backup` 已注释掉依赖，备注"basebackend-logging 已被 observability 包含"。

### 1.3 与 basebackend-observability 的重叠

| 能力 | basebackend-logging | basebackend-observability/logging |
|------|--------------------|------------------------------------|
| 脱敏 | `PiiMaskingService` (注解驱动) | `MaskingConverter` (Logback管道) |
| 配置 | `basebackend.logging.*` | `observability.logging.*` |
| 自动配置 | 空文件 (未实现) | `LoggingAutoConfiguration` (已实现) |
| MDC填充 | 无 | `LogAttributeEnricher` |
| 日志采样 | 无 | `LogSamplingTurboFilter` |
| 日志路由 | 无 | `LogRoutingAppender` |

---

## 2. 发现的问题

### P0 - 紧急缺陷

#### 2.1 定时刷盘双重触发

**位置**: `audit/service/AuditService.java:201, :384-391`

**问题**: `scheduledFlush()` 被两种机制同时调度:
- 构造函数中 `ScheduledExecutorService.scheduleAtFixedRate()` (audit-scheduler 线程)
- `@Scheduled(fixedDelayString = ...)` 注解 (Spring 调度线程)

**影响**: flush 以双倍频率执行，浪费 I/O 资源，可能导致并发写入冲突。

**修复**: 移除 `@Scheduled` 注解，保留 `ScheduledExecutorService` 调度 (因为 `AuditService` 通过 `new` 创建，`@Scheduled` 实际上也不会生效 — 但如果未来改为 Spring Bean 则会触发)。

#### 2.2 CAS 哈希链竞态条件

**位置**: `audit/service/AuditService.java:116-124`

**问题**: CAS 循环中 `entry.setPrevHash(prevHash)` 在 CAS 确认前修改 entry 状态:
```java
do {
    prevHash = lastHash.get();
    entry.setPrevHash(prevHash);  // <-- CAS失败后entry已被污染
    entryHash = hashChainCalculator.computeHash(entry, prevHash);
} while (!lastHash.compareAndSet(prevHash, entryHash));
```
如果 `computeHash` 内部读取 `entry.prevHash`，CAS 重试时哈希计算输入不一致。

**修复**:
```java
String prevHash, entryHash;
do {
    prevHash = lastHash.get();
    entryHash = hashChainCalculator.computeHash(entry, prevHash);
} while (!lastHash.compareAndSet(prevHash, entryHash));
entry.setPrevHash(prevHash);
entry.setEntryHash(entryHash);
```
**前置验证**: 检查 `HashChainCalculator.computeHash()` 是否依赖 `entry.prevHash` 字段。

#### 2.3 gnu-crypto 废弃依赖

**位置**: `pom.xml:27-32`

**问题**: `org.gnu:gnu-crypto:2.0.1` 是 2004 年发布的废弃库，最后更新近 20 年。

**分析**: `AesEncryptor.java` 使用 `javax.crypto` (JDK 内置)，不依赖 gnu-crypto。需确认全模块无 `import gnu.crypto` 引用。

**修复**: 确认无引用后直接删除依赖。

#### 2.4 LoggingAutoConfiguration 空实现

**位置**: `config/LoggingAutoConfiguration.java` (1行空文件)

**问题**: 模块缺少有效的自动配置入口和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

**修复**:
1. 实现 `LoggingAutoConfiguration`，Import 子配置类 (`AuditAutoConfiguration`, `MaskingAutoConfiguration`, `HotLogCacheConfiguration`, `MonitoringAutoConfiguration`)
2. 注册 `LoggingUnifiedProperties`
3. 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

---

### P1 - 重要问题

#### 2.5 线程池未纳入 Spring 管理

**位置**: `audit/service/AuditService.java:75-84`

**问题**:
- 手动创建 `ScheduledExecutorService` (2线程) + `ExecutorService` (4固定线程)
- `shutdown()` 方法需手动调用
- `AuditAutoConfiguration.registerShutdownHook()` 使用 `Runtime.addShutdownHook` 但**从未被调用** (不是 @Bean，无显式调用)

**修复**:
1. `AuditService` 实现 `DisposableBean`, `destroy()` 调用 `shutdown()`
2. 删除 `registerShutdownHook()` 死方法

#### 2.6 AuditAutoConfiguration 双 Bean 冲突

**位置**: `audit/config/AuditAutoConfiguration.java:141-196`

**问题**: 定义两个 `AuditStorage` 类型 Bean:
- `fileAuditStorage()` — `@Bean @ConditionalOnMissingBean`
- `auditStorage()` — `@Bean @ConditionalOnMissingBean` (接收 `AuditStorage fileStorage` 参数)

第一个注册后第二个永远不会创建。`CompositeAuditStorage` 路径死代码。

**修复**: `fileAuditStorage()` 改为私有辅助方法 (非 Bean)，仅在 `auditStorage()` 内调用。

#### 2.7 auditSignatureService 方法签名重复 throws

**位置**: `audit/config/AuditAutoConfiguration.java:93`

**问题**: `throws NoSuchAlgorithmException, NoSuchAlgorithmException` 重复声明。

**修复**: 去重。

#### 2.8 残留代码

- `configcenter/ApolloConfigManager.java.disabled` — 项目已标准化 Nacos，应删除
- `pom.xml` 中 `apollo-client:2.1.0` optional 依赖 — 一并删除

#### 2.9 hutool-all 重量级依赖

**位置**: `pom.xml:103-105`

**问题**: `hutool-all` 引入整个 Hutool 工具包 (几十 MB)。

**修复**: 识别实际使用的 Hutool 类，替换为按需子模块 (如 `hutool-core`, `hutool-crypto`)。

---

### P2 - 改进建议

#### 2.10 模块职责膨胀 (scope creep)

日志库不应包含: Redis 缓存系统、Grafana Dashboard 配置生成、Prometheus 告警规则、时间序列分析器、趋势预测器、性能基准测试。

**建议**: 未来拆分时:
- `cache/` → 归入 `basebackend-cache` 或删除
- `monitoring/` → 归入 `basebackend-observability`
- `statistics/` + `benchmark/` → 独立工具或删除

#### 2.11 测试覆盖率不足

90+ 源文件仅 7 个测试。核心组件 `AsyncBatchAppender`、`HashChainCalculator`、`AesEncryptor`、`RedisHotLogCache`、`FileAuditStorage` 无测试。

---

## 3. 扩展方向

### 3.1 审计日志合规增强
- GDPR/个保法: 数据主体访问请求 (DSAR) 接口、保留策略与自动清理
- 审计日志导出: CEF、LEEF、OCSF 标准格式 (供 SIEM 系统消费)
- 多租户隔离: 按租户分片存储审计日志
- JDBC 持久化: 实现 `DatabaseAuditStorage` (当前仅 TODO 注释)

### 3.2 日志流水线 (Log Pipeline)
- Kafka/Pulsar 传输层 (替代直连 Loki/File)
- Pipeline 链式处理 (过滤→富化→转换→输出)
- 本地 WAL 保障 (异步发送失败时不丢数据)

### 3.3 动态日志级别调整
- REST API / Actuator Endpoint 运行时调整
- Nacos 配置变更监听驱动日志级别热更新
- 按包名/traceId/用户维度的临时调试级别

### 3.4 日志成本治理
- 按服务/租户统计日志量，输出成本归因指标
- 采样率自适应调整 (基于吞吐量和存储压力)
- 分级存储: 热日志(实时查询) → 温日志(压缩) → 冷日志(归档/S3)

---

## 4. 执行计划

### Phase 1: P0 紧急缺陷修复 (可并行)

```
Task 1.1: 修复定时刷盘双重触发
  文件: AuditService.java
  变更: 删除 @Scheduled 注解
  验证: 审计刷盘频率 == 配置的 flushIntervalMs

Task 1.2: 修复 CAS 哈希链竞态
  文件: AuditService.java
  前置: 检查 HashChainCalculator.computeHash() 实现
  变更: 分离 CAS 与 entry 状态修改
  验证: 并发写入 1000 条, 验证哈希链连续性

Task 1.3: 移除 gnu-crypto 依赖
  文件: pom.xml
  前置: 确认无 import gnu.crypto 引用
  变更: 删除 dependency 声明
  验证: mvn compile -pl basebackend-logging

Task 1.4: 实现 LoggingAutoConfiguration
  文件: config/LoggingAutoConfiguration.java (重写)
  新增: META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  验证: 应用启动确认 Bean 自动装配
```

### Phase 2: P1 架构治理 (部分串行)

```
Task 2.1: 线程池纳入 Spring 管理
  依赖: Task 1.1 (共享 scheduledFlush 改动)
  文件: AuditService.java, AuditAutoConfiguration.java
  变更: 实现 DisposableBean, 删除 registerShutdownHook()
  验证: 应用优雅关停日志输出"审计服务已关闭"

Task 2.2: 修复 AuditStorage 双 Bean 冲突
  文件: AuditAutoConfiguration.java
  变更: fileAuditStorage 改为私有方法
  验证: enableMultiTierStorage=true 时 CompositeAuditStorage 正确创建

Task 2.3: 清理残留代码与依赖
  文件: ApolloConfigManager.java.disabled (删除), pom.xml
  变更: 删除 Apollo 残留, 替换 hutool-all, 修复 throws 重复
  验证: mvn compile 通过

Task 2.4: 文档化模块边界
  文件: CLAUDE.md (两个模块), docs/MODULE_BOUNDARY.md (新建)
  变更: 明确 logging vs observability/logging 职责划分
  验证: 文档审查
```

### Phase 3: P2 测试补全

```
Task 3.1: 核心组件单元测试
  依赖: Phase 1 + 2 完成
  目标: LINE >= 50%, BRANCH >= 30%
  覆盖:
    - AesEncryptor: 加解密往返, 无效密钥, 篡改检测
    - HashChainCalculator: 哈希连续性, 并发安全
    - AuditService: 入队/刷盘/队列溢出/shutdown
    - PiiMaskingService: 各脱敏策略
    - FileAuditStorage: 写入/滚动/压缩
  PBT 属性:
    - Round-trip: decrypt(encrypt(x)) == x
    - Idempotency: mask(mask(x)) == mask(x)
    - Monotonicity: 审计日志 timestamp 单调递增
    - Invariant: queue.size() <= queueCapacity
```

### Phase 4: 扩展 (未来迭代)

| 方向 | 前置条件 | 可并行 |
|------|---------|--------|
| DatabaseAuditStorage | Task 2.2 完成 | 是 |
| 动态日志级别 API | 独立 | 是 |
| 审计导出 CEF/OCSF | Phase 2 完成 | 是 |
| 日志成本治理 | 需 observability 配合 | 否 |
| 独立 basebackend-audit 模块 | Phase 1-3 全部完成 | 否 |

### 依赖图

```
Phase 1 (P0, 全部可并行):
  Task 1.1 ──┐
  Task 1.2 ──┤── 无互相依赖
  Task 1.3 ──┤
  Task 1.4 ──┘
      │
Phase 2 (P1):
  Task 2.1 ←── 依赖 Task 1.1
  Task 2.2 ←── 独立
  Task 2.3 ←── 独立
  Task 2.4 ←── 独立
      │
Phase 3 (P2):
  Task 3.1 ←── 依赖 Phase 1 + 2
      │
Phase 4 (扩展):
  各方向 ←── 见上表前置条件
```

---

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Task 1.2 修改哈希链逻辑导致已有审计日志链断裂 | 高 | 仅影响修复后新写入的日志，旧链不受影响 |
| Task 1.4 自动配置可能导致 Bean 冲突 | 中 | 所有 Bean 使用 @ConditionalOnMissingBean |
| Task 2.3 替换 hutool-all 可能遗漏使用点 | 中 | 编译验证 + 全文搜索 hutool import |
| 7 个下游模块受影响 | 高 | 所有变更保持 API 兼容，不改公共接口签名 |

---

## 6. 验收标准

- [ ] 所有 P0 缺陷修复后 `mvn compile -pl basebackend-logging` 通过
- [ ] 所有 P0 缺陷修复后 `mvn test -pl basebackend-logging` 通过
- [ ] 7 个下游模块编译不受影响: `mvn compile -pl basebackend-admin-api,basebackend-system-api,basebackend-user-api,basebackend-notification-service,basebackend-observability-service,basebackend-code-generator`
- [ ] 无新增 API 破坏性变更
- [ ] Phase 3 完成后测试覆盖率 LINE >= 50%
