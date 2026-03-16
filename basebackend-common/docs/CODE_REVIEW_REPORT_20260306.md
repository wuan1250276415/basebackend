# BaseBackend Common 模块全面审查报告

## 1. 审查概览

- 审查日期：2026-03-06
- 审查范围：`basebackend-common` 目录下全部子模块（含聚合声明模块与未纳入聚合的模块）
- 审查方式：
  - 架构与依赖一致性检查（`pom.xml`、AutoConfiguration 清单、文档）
  - 代码静态审查（并发、安全、异常处理、上下文隔离、可维护性）
  - 测试执行验证（`mvn -f basebackend-common/pom.xml test -q`）

## 2. 总体结论

当前 `basebackend-common` 能通过现有测试，但存在多项高风险问题，集中在：

- 安全边界（本地存储路径穿越、IP 来源信任）
- 并发正确性（内存锁 `forceUnlock`、上下文恢复）
- 功能可靠性（JDBC 事件重试链路失效、DataScope SQL 注入逻辑缺陷）
- 配置与模块治理（聚合模块与目录/文档长期漂移）

风险分布（本次报告确认项）：

- High：8
- Medium：9
- Low：5

## 3. 高风险问题（High）

### H-01 本地存储存在路径穿越风险

- 证据：`basebackend-common/basebackend-common-storage/src/main/java/com/basebackend/storage/provider/LocalStorageProvider.java:251`
- 说明：`resolvePath` 直接拼接 `basePath/bucket/key`，未做 `normalize` 和根路径约束。
- 影响：攻击者可构造 `../` 实现越界读写/删除。
- 建议：统一做路径规范化，并校验最终路径必须 `startsWith(basePath)`。

### H-02 DataScope SQL 拼接在已有 WHERE 场景会生成非法 SQL

- 证据：`basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/interceptor/DataScopeInterceptor.java:76`
- 说明：已有 `WHERE` 时直接在 SQL 尾部拼 `AND condition`，遇到 `ORDER BY/LIMIT` 会语法错误。
- 影响：查询异常、可用性下降。
- 建议：改为基于 SQL AST 注入条件，或至少在主查询排序/分页子句前插入。

### H-03 内存锁 `forceUnlock` 可破坏互斥语义

- 证据：`basebackend-common/basebackend-common-lock/src/main/java/com/basebackend/common/lock/provider/impl/InMemoryDistributedLockProvider.java:57`
- 说明：先从 `lockMap` 移除，再尝试按当前线程解锁；非持有线程调用时会导致旧锁仍持有但映射丢失。
- 影响：后续请求可创建新锁并进入临界区，造成并发安全问题。
- 建议：仅在确认安全解锁后再移除映射，或禁用此实现的 `forceUnlock`。

### H-04 幂等切面装配存在无 Redis 启动失败风险

- 证据：`basebackend-common/basebackend-common-idempotent/src/main/java/com/basebackend/common/idempotent/config/IdempotentAutoConfiguration.java:82`
- 说明：`IdempotentAspect` 强依赖 `IdempotentTokenService`，但该 Bean 仅在 Redis 分支创建。
- 影响：内存降级模式下可能启动失败（Bean 缺失）。
- 建议：将 `IdempotentTokenService` 设为可选注入（`ObjectProvider`/`@Nullable`）。

### H-05 JDBC 事件存储重试链路不可用

- 证据：
  - `basebackend-common/basebackend-common-event/src/main/java/com/basebackend/common/event/store/JdbcEventStore.java:63`
  - `basebackend-common/basebackend-common-event/src/main/java/com/basebackend/common/event/store/JdbcEventStore.java:79`
- 说明：`findPendingEvents/findFailedEvents` 返回空列表，导致调度器无法真实重试。
- 影响：可靠事件机制在 JDBC 模式下失效。
- 建议：实现可分页拉取与反序列化，补 JDBC 集成测试。

### H-06 租户忽略上下文恢复存在线程污染风险

- 证据：`basebackend-common/basebackend-common-context/src/main/java/com/basebackend/common/context/TenantContextHolder.java:272`
- 说明：`ignoreTenant` 的 finally 仅在 `backup != null` 时恢复；若执行体内重新设置了上下文且 `backup` 为 `null`，会残留。
- 影响：线程复用场景下可能串租户。
- 建议：finally 中对 `backup == null` 显式 `remove()`。

### H-07 IP 获取逻辑完全信任转发头

- 证据：`basebackend-common/basebackend-common-util/src/main/java/com/basebackend/common/util/IpUtil.java:41`
- 说明：无可信代理校验，直接读取 `X-Forwarded-For` 等头。
- 影响：可伪造来源 IP，影响风控、审计、限流。
- 建议：引入可信代理白名单，仅在可信链路解析转发头。

### H-08 DataScope 缺省策略偏 fail-open

- 证据：`basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/handler/DataScopeSqlBuilder.java:41`
- 说明：无用户上下文及 `AUTO` 分支默认返回空条件。
- 影响：调用方误用时可能放大数据权限范围。
- 建议：默认 fail-close（如 `1=0`）并实现 `AUTO` 实际策略解析。

## 4. 中风险问题（Medium）

### M-01 `ResponseResult.error()` 与 Result 语义不一致

- 证据：`basebackend-common/basebackend-common-starter/src/main/java/com/basebackend/common/web/ResponseResult.java:117`
- 说明：`error()` 返回默认构造对象，`code` 可能为 `null`。
- 影响：上层判错逻辑与序列化契约不稳定。
- 建议：与 `Result.error()` 保持一致，设置标准错误码。

### M-02 内存幂等存储过期替换非 CAS

- 证据：`basebackend-common/basebackend-common-idempotent/src/main/java/com/basebackend/common/idempotent/store/impl/InMemoryIdempotentStore.java:45`
- 说明：过期后直接 `put`，极端并发下可能放过多个请求。
- 建议：改为 `replace(old,new)` CAS 重试。

### M-03 DataScope 注解支持 TYPE 但切面仅拦截方法

- 证据：
  - `basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/annotation/DataScope.java:34`
  - `basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/aspect/DataScopeAspect.java:32`
- 影响：类级注解可能静默失效。
- 建议：增加 `@within/@target` 切点或收敛注解目标。

### M-04 SQL 片段拼接未限制标识符/条件来源

- 证据：`basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/handler/DataScopeSqlBuilder.java:76`
- 影响：扩展场景下存在注入面。
- 建议：字段/别名白名单校验，避免原始条件直接透传。

### M-05 `AsyncExportService` 配置项未生效

- 证据：`basebackend-common/basebackend-common-export/src/main/java/com/basebackend/common/export/AsyncExportService.java:32`
- 说明：`threadPoolSize` 未使用，`taskTtlMillis` 未参与真实 TTL 清理。
- 影响：行为与配置不一致。
- 建议：按配置构造线程池并基于任务时间戳执行 TTL 清理。

### M-06 `EventStore` 失败原因未落库

- 证据：`basebackend-common/basebackend-common-event/src/main/java/com/basebackend/common/event/store/JdbcEventStore.java:93`
- 影响：排障可观测性不足。
- 建议：补充 `fail_reason` 字段与持久化逻辑。

### M-07 `UserContext.hasPermission` 注释与实现不一致

- 证据：`basebackend-common/basebackend-common-context/src/main/java/com/basebackend/common/context/UserContext.java:117`
- 说明：注释写支持 `system:user:*`，实现仅支持全局通配。
- 建议：实现分段通配，或修正文档。

### M-08 固定窗口/令牌桶缺少参数边界保护

- 证据：
  - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/impl/FixedWindowRateLimiter.java:58`
  - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/impl/TokenBucketRateLimiter.java:55`
- 影响：`windowSeconds <= 0` 等非法参数可触发异常/绕过。
- 建议：入口统一校验 `limit > 0 && window > 0`。

### M-09 模块聚合清单与目录不一致

- 证据：
  - `basebackend-common/pom.xml:20`
  - `basebackend-common/basebackend-common-audit/pom.xml:14`
  - `basebackend-common/basebackend-common-masking/pom.xml:14`
  - `basebackend-common/basebackend-common-tree/pom.xml:14`
- 说明：3 个真实模块未纳入 `basebackend-common` 聚合构建。
- 影响：CI 构建与测试覆盖出现盲区。
- 建议：明确模块归属并统一到聚合清单。

## 5. 低风险问题（Low）

- `basebackend-common/basebackend-common-util/src/main/java/com/basebackend/common/util/NacosUtils.java:17`：示例密钥硬编码在源码中，建议移除敏感样例。
- `basebackend-common/basebackend-common-security/src/main/java/com/basebackend/common/util/SanitizationUtils.java:24`：仅做模式识别，建议明确边界并补充回归样例。
- `basebackend-common/basebackend-common-core/src/main/java/com/basebackend/common/util/JsonUtils.java:53`：序列化失败时回退 `toString()`，建议提供严格模式 API。
- `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/impl/RedisSlidingWindowRateLimiter.java:81`：Redis 异常默认放行，建议改为可配置策略。
- 文档漂移：`basebackend-common/README.md:10`、`basebackend-common/CLAUDE.md:7` 与当前模块实际状态不一致。

## 6. 测试与验证结果

已执行：

- `mvn -f basebackend-common/pom.xml test -q`

结果：

- 命令退出码为 0，当前声明模块测试通过。
- 但测试结构上仍有覆盖盲区：`JdbcEventStore`、`AsyncExportService`、DataScope SQL 改写边界。

## 7. 测试覆盖与模块治理观察

- 代码规模（目录扫描）：主代码约 155 个 Java 文件，测试约 33 个 Java 文件。
- `basebackend-common` 聚合声明模块为 12 个，但目录下存在 15 个 Maven 子模块。
- `audit/masking/tree` 虽有测试文件，但未纳入聚合 reactor，无法保证被主流水线持续执行。

## 8. 整改优先级建议

### P0（立即处理）

1. 修复本地存储路径穿越（H-01）
2. 修复 DataScope SQL 注入逻辑缺陷（H-02）
3. 修复内存锁 `forceUnlock` 并发语义（H-03）
4. 修复幂等自动配置 Bean 依赖缺陷（H-04）
5. 补全 JDBC 事件重试读取能力（H-05）

### P1（本周处理）

1. 修复租户上下文恢复污染（H-06）
2. 增加可信代理链 IP 解析（H-07）
3. 明确 DataScope fail-close 策略（H-08）
4. 修复 `ResponseResult.error()` 语义（M-01）
5. 治理模块聚合清单与文档漂移（M-09）

### P2（两周内）

1. 补齐事件/导出/数据权限边界测试
2. 清理低风险代码与日志暴露问题
3. 建立“模块清单一致性”CI 校验（目录 vs modules vs 文档）

## 9. 附：本次重点抽样文件

- `basebackend-common/basebackend-common-storage/src/main/java/com/basebackend/storage/provider/LocalStorageProvider.java`
- `basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/interceptor/DataScopeInterceptor.java`
- `basebackend-common/basebackend-common-lock/src/main/java/com/basebackend/common/lock/provider/impl/InMemoryDistributedLockProvider.java`
- `basebackend-common/basebackend-common-idempotent/src/main/java/com/basebackend/common/idempotent/config/IdempotentAutoConfiguration.java`
- `basebackend-common/basebackend-common-event/src/main/java/com/basebackend/common/event/store/JdbcEventStore.java`
- `basebackend-common/basebackend-common-context/src/main/java/com/basebackend/common/context/TenantContextHolder.java`
- `basebackend-common/basebackend-common-util/src/main/java/com/basebackend/common/util/IpUtil.java`
- `basebackend-common/basebackend-common-starter/src/main/java/com/basebackend/common/web/ResponseResult.java`

## 10. 修复进展更新（2026-03-06 晚间）

### 10.1 P1 修复完成项

1. **H-06 租户上下文恢复污染**
   - 已修复：`TenantContextHolder.ignoreTenant(...)` 在 `backup == null` 时显式清理上下文，避免执行体内残留污染。
   - 变更文件：
     - `basebackend-common/basebackend-common-context/src/main/java/com/basebackend/common/context/TenantContextHolder.java`
     - `basebackend-common/basebackend-common-context/src/test/java/com/basebackend/common/context/TenantContextHolderTest.java`

2. **H-07 IP 来源信任边界**
   - 已修复：`IpUtil` 新增“仅可信代理解析转发头”策略；非可信来源直接使用 `remoteAddr`，阻断伪造 `X-Forwarded-For`。
   - 变更文件：
     - `basebackend-common/basebackend-common-util/src/main/java/com/basebackend/common/util/IpUtil.java`
     - `basebackend-common/basebackend-common-util/src/test/java/com/basebackend/common/util/IpUtilTest.java`

3. **M-01 ResponseResult 错误语义不一致**
   - 已修复：`ResponseResult.error()` 与 `error(String)` 统一为 `INTERNAL_SERVER_ERROR(500)` 语义，与 `Result` 保持一致。
   - 变更文件：
     - `basebackend-common/basebackend-common-starter/src/main/java/com/basebackend/common/web/ResponseResult.java`
     - `basebackend-common/basebackend-common-starter/src/test/java/com/basebackend/common/web/ResponseResultTest.java`

4. **M-09 模块聚合清单与文档漂移**
   - 已修复：`basebackend-common/pom.xml` 纳入 `audit/masking/tree` 三个真实模块，并补齐 `dependencyManagement`。
   - 已修复：`README.md`、`CLAUDE.md` 模块清单与实际 15 子模块对齐，移除过时 `common-dto` 描述。
   - 变更文件：
     - `basebackend-common/pom.xml`
     - `basebackend-common/README.md`
     - `basebackend-common/CLAUDE.md`

### 10.2 本轮新增测试

- `basebackend-common-context`：`TenantContextHolderTest`（上下文恢复/清理边界）
- `basebackend-common-util`：`IpUtilTest`（可信代理、头部回退、伪造防护）
- `basebackend-common-starter`：`ResponseResultTest`（默认错误码语义一致性）

### 10.3 回归验证

已执行并通过：

- `mvn -pl basebackend-common/basebackend-common-context -am test -q`
- `mvn -pl basebackend-common/basebackend-common-util -am test -q`
- `mvn -pl basebackend-common/basebackend-common-starter -am test -q`
- `mvn -f basebackend-common/pom.xml test -q`

### 10.4 当前剩余风险建议（2026-03-06 晚间）

- 中风险（M）项已全部完成整改（含 `M-02 ~ M-09`）。
- 当前建议聚焦低风险（Low）项与长期治理项（如模块一致性 CI 校验）。

## 11. 第二轮修复进展（2026-03-06 深夜）

### 11.1 已完成整改项（M-02 ~ M-08）

1. **M-02 幂等内存存储并发 CAS**
   - 已修复：`InMemoryIdempotentStore.tryAcquire` 从“过期后直接 put”改为 `replace(old,new)` CAS 重试，避免并发放行。
   - 变更文件：
     - `basebackend-common/basebackend-common-idempotent/src/main/java/com/basebackend/common/idempotent/store/impl/InMemoryIdempotentStore.java`
     - `basebackend-common/basebackend-common-idempotent/src/test/java/com/basebackend/common/idempotent/store/impl/InMemoryIdempotentStoreTest.java`

2. **M-03 DataScope 类级注解切点覆盖**
   - 已修复：`DataScopeAspect` 切点覆盖方法级 + 类级注解，且方法级优先。
   - 变更文件：
     - `basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/aspect/DataScopeAspect.java`
     - `basebackend-common/basebackend-common-datascope/src/test/java/com/basebackend/common/datascope/aspect/DataScopeAspectTest.java`

3. **M-04 DataScope 条件片段来源约束**
   - 已修复：`DataScopeSqlBuilder` 增加 alias/field/table 安全校验；`CUSTOM` 条件增加安全表达式校验，不安全时 fail-close 为 `1 = 0`。
   - 变更文件：
     - `basebackend-common/basebackend-common-datascope/src/main/java/com/basebackend/common/datascope/handler/DataScopeSqlBuilder.java`
     - `basebackend-common/basebackend-common-datascope/src/test/java/com/basebackend/common/datascope/handler/DataScopeSqlBuilderTest.java`

4. **M-05 AsyncExportService 配置项生效**
   - 已修复：`threadPoolSize` 实际用于线程池；`taskTtlHours` 转换后的 TTL 参与终态任务过期清理。
   - 变更文件：
     - `basebackend-common/basebackend-common-export/src/main/java/com/basebackend/common/export/AsyncExportService.java`
     - `basebackend-common/basebackend-common-export/src/main/java/com/basebackend/common/export/ExportTaskStatus.java`
     - `basebackend-common/basebackend-common-export/src/test/java/com/basebackend/common/export/AsyncExportServiceTest.java`

5. **M-06 EventStore 失败原因落库**
   - 已修复：`JdbcEventStore.markAsFailed` 持久化 `fail_reason`；缺列场景自动回退旧 SQL，保障老库兼容。
   - 变更文件：
     - `basebackend-common/basebackend-common-event/src/main/java/com/basebackend/common/event/store/JdbcEventStore.java`
     - `basebackend-common/basebackend-common-event/src/main/resources/db/event_store.sql`
     - `basebackend-common/basebackend-common-event/src/test/java/com/basebackend/common/event/store/JdbcEventStoreTest.java`

6. **M-07 UserContext 权限匹配语义一致性**
   - 已修复：`hasPermission` 支持分段通配（如 `system:user:*`），与注释语义一致。
   - 变更文件：
     - `basebackend-common/basebackend-common-context/src/main/java/com/basebackend/common/context/UserContext.java`
     - `basebackend-common/basebackend-common-context/src/test/java/com/basebackend/common/context/UserContextTest.java`

7. **M-08 限流参数边界保护**
   - 已修复：`FixedWindowRateLimiter` 与 `TokenBucketRateLimiter` 对 `key`、`limit`、`windowSeconds` 增加边界校验，非法参数安全拒绝。
   - 变更文件：
     - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/impl/FixedWindowRateLimiter.java`
     - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/impl/TokenBucketRateLimiter.java`
     - `basebackend-common/basebackend-common-ratelimit/src/test/java/com/basebackend/common/ratelimit/impl/RateLimiterImplTest.java`

### 11.2 本轮回归验证结果

已执行并通过：

- `mvn -pl basebackend-common/basebackend-common-idempotent -am test -q`
- `mvn -pl basebackend-common/basebackend-common-datascope -am test -q`
- `mvn -pl basebackend-common/basebackend-common-export -am test -q`
- `mvn -pl basebackend-common/basebackend-common-event -am test -q`
- `mvn -pl basebackend-common/basebackend-common-context -am test -q`
- `mvn -pl basebackend-common/basebackend-common-ratelimit -am test -q`
- `mvn -f basebackend-common/pom.xml test -q`

### 11.3 阶段性收口结论（截至第二轮）

- 报告中 High 与 Medium 风险项已全部完成整改并通过回归。
- 剩余建议项为 Low 风险与长期工程治理（Low 项已在第 12 节完成收口）。

## 12. 第三轮修复进展（2026-03-06 深夜）

### 12.1 已完成整改项（Low-01 ~ Low-04）

1. **Low-01 NacosUtils 示例密钥硬编码**
   - 已修复：`NacosUtils` 不再内置示例密钥，改为从 `main(args[0])` 接收输入；新增 `encodeToBase64(String)` 供复用。
   - 变更文件：
     - `basebackend-common/basebackend-common-util/src/main/java/com/basebackend/common/util/NacosUtils.java`
     - `basebackend-common/basebackend-common-util/src/test/java/com/basebackend/common/util/NacosUtilsTest.java`

2. **Low-02 SanitizationUtils 危险内容识别边界**
   - 已修复：`containsUnsafeContent` 对危险标签、事件处理器属性、危险协议（`javascript:`/`vbscript:`/`data:text/html`）的识别边界更明确。
   - 变更文件：
     - `basebackend-common/basebackend-common-security/src/main/java/com/basebackend/common/util/SanitizationUtils.java`
     - `basebackend-common/basebackend-common-security/src/test/java/com/basebackend/common/util/SanitizationUtilsTest.java`

3. **Low-03 JsonUtils 严格序列化能力**
   - 已修复：新增 `toJsonStringStrict` / `toJsonBytesStrict`，在序列化失败时抛异常；原 `toJsonString`/`toJsonBytes` 保持兼容容错行为。
   - 变更文件：
     - `basebackend-common/basebackend-common-core/src/main/java/com/basebackend/common/util/JsonUtils.java`
     - `basebackend-common/basebackend-common-core/src/test/java/com/basebackend/common/util/JsonUtilsTest.java`

4. **Low-04 Redis 限流异常回退策略固定放行**
   - 已修复：新增配置 `rate-limit.allow-on-redis-failure`（默认 `false`），并注入 Redis 滑窗/令牌桶实现，异常时按配置执行放行或拒绝。
   - 变更文件：
     - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/config/RateLimitProperties.java`
     - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/config/RateLimitAutoConfiguration.java`
     - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/impl/RedisSlidingWindowRateLimiter.java`
     - `basebackend-common/basebackend-common-ratelimit/src/main/java/com/basebackend/common/ratelimit/impl/RedisTokenBucketRateLimiter.java`
     - `basebackend-common/basebackend-common-ratelimit/src/test/java/com/basebackend/common/ratelimit/config/RateLimitPropertiesTest.java`
     - `basebackend-common/basebackend-common-ratelimit/src/test/java/com/basebackend/common/ratelimit/impl/RedisRateLimiterFallbackTest.java`

### 12.2 本轮回归验证结果

已执行并通过（退出码均为 0）：

- `mvn -pl basebackend-common/basebackend-common-util -am test -q`
- `mvn -pl basebackend-common/basebackend-common-security -am test -q`
- `mvn -pl basebackend-common/basebackend-common-core -am test -q`
- `mvn -pl basebackend-common/basebackend-common-ratelimit -am test -q`
- `mvn -f basebackend-common/pom.xml test -q`

说明：日志中出现的 `JsonUtils`、Redis fallback、Mockito agent 相关 WARN 为测试场景预期输出，不影响结果判定。

### 12.3 当前收口结论（第三轮后）

- 报告中 High / Medium / Low 风险项已全部完成整改并通过回归验证。
- 当前剩余建议项为长期工程治理（如模块清单一致性 CI 校验、持续覆盖增强等）。

## 13. 长期治理落地（2026-03-07）

### 13.1 新增治理能力：模块清单一致性门禁

- 已新增脚本：`/.github/scripts/check-basebackend-common-consistency.sh`
  - 目录基线：扫描 `basebackend-common/basebackend-common-*` 且包含 `pom.xml` 的真实子模块。
  - 对比目标：
    1) `basebackend-common/pom.xml` 的 `<modules>`
    2) `basebackend-common/pom.xml` 的 `dependencyManagement` 中 `basebackend-common-*` `artifactId`
    3) `basebackend-common/README.md` 模块清单
    4) `basebackend-common/CLAUDE.md` 模块清单
  - 校验策略：集合全量一致（缺失/多余任一出现即失败）。

- 已新增工作流：`/.github/workflows/common-module-consistency.yml`
  - 触发条件：PR / Push 命中 common 模块清单相关路径时自动校验，支持手工触发。
  - 执行命令：`bash .github/scripts/check-basebackend-common-consistency.sh`
- 已接入主流水线：`/.github/workflows/ci.yml` 的 `build-and-test` Job 前置执行一致性校验（fail-fast）。
- 已新增 PR 模板提醒：`/.github/pull_request_template.md`，在评审入口显式要求同步 common 模块四方清单并执行一致性脚本。
- 已新增 CODEOWNERS 规则：`/.github/CODEOWNERS`，对 common 清单治理关键文件强制指定评审人。

### 13.2 本地验证

已执行并通过：

- `bash .github/scripts/check-basebackend-common-consistency.sh`

### 13.3 治理收益

- 防止“目录实际模块 / 聚合 modules / dependencyManagement / 文档清单”再次漂移。
- 将人工审查结论固化为 CI 门禁，降低后续回归成本与漏检风险。
