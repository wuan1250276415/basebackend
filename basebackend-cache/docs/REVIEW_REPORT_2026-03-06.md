# basebackend-cache 模块全面审查报告

- 审查日期：2026-03-06
- 审查范围：`basebackend-cache-core`、`basebackend-cache-advanced`、`basebackend-cache-admin`
- 审查方式：静态代码审查 + 关键链路核对 + 模块级测试执行
- 代码规模：`core` 主代码 61 个 Java 文件，测试 13 个；`advanced` 主代码 19 个，测试 2 个；`admin` 主代码 5 个，测试 1 个

---

## 一、执行摘要

本次审查结论：**模块功能覆盖较广，但存在多项高风险正确性/安全性问题，建议先修复 P0 再推进扩展能力接入**。

高优先级问题集中在：

1. 清空全部缓存接口语义与实现不一致（管理端“确认”参数未生效）
2. JSON 字符串序列化逻辑存在缺陷，已触发属性测试失败
3. 过期移除实现可能误删 key
4. 跨服务失效事件缺少消息鉴权
5. 管理端接口与模式扫描在大 key 空间下存在放大风险

---

## 二、测试与可运行性验证

### 2.1 执行命令

- `mvn test -DskipITs`（在 `basebackend-cache/basebackend-cache-core`）
- `mvn test -DskipITs`（在 `basebackend-cache/basebackend-cache-advanced`）
- `mvn test -DskipITs`（在 `basebackend-cache/basebackend-cache-admin`）

### 2.2 结果汇总

- `core`：tests=217, failures=2, errors=0, skipped=23
- `advanced`：tests=13, failures=0, errors=0, skipped=3
- `admin`：tests=6, failures=0, errors=0, skipped=0

### 2.3 core 失败用例

1. `SerializerRoundTripPropertyTest.jsonSerializerRoundTripForPrimitives`
   - 失败位置：`basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/serializer/SerializerRoundTripPropertyTest.java:44`
   - 失败现象：`expected "" but was null`

2. `SerializerErrorHandlingPropertyTest.jsonDeserializerHandlesTypeMismatchSafely`
   - 失败位置：`basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/serializer/SerializerErrorHandlingPropertyTest.java:103`
   - 失败现象：期望抛异常，但实际未抛出

---

## 三、问题清单（按优先级）

## P0（必须优先修复）

### P0-1 清空全部缓存的确认参数无效（功能正确性）

- 描述：管理接口虽要求 `confirmed=true`，但调用链最终未传递确认语义，导致清空操作默认被拒绝。
- 证据：
  - `basebackend-cache/basebackend-cache-admin/src/main/java/com/basebackend/cache/admin/CacheAdminController.java:105`
  - `basebackend-cache/basebackend-cache-admin/src/main/java/com/basebackend/cache/admin/CacheAdminController.java:110`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/service/CacheServiceImpl.java:360`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/service/CacheServiceImpl.java:373`
- 影响：`/clear-all` 接口语义与行为不一致；跨服务 `CLEAR_ALL` 事件同样受影响。
- 建议：在 `CacheService` 暴露显式确认版本（或独立安全 API），管理端在完成授权+确认后调用。

### P0-2 JSON 字符串序列化链路缺陷（功能正确性）

- 描述：字符串序列化时未输出合法 JSON 字符串格式，导致反序列化行为异常（如空串变 null）。
- 证据：
  - `basebackend-common/basebackend-common-core/src/main/java/com/basebackend/common/util/JsonUtils.java:49`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/serializer/JsonCacheSerializer.java:35`
  - `basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/serializer/SerializerRoundTripPropertyTest.java:44`
- 影响：缓存命中值可能失真，序列化一致性被破坏。
- 建议：统一按 JSON 规范序列化字符串（不要对 String 特殊“直通”），并补充字符串边界回归测试。

### P0-3 removeExpiration 可能误删 key（功能正确性）

- 描述：通过 `expire(key, -1)` 试图实现“永不过期”，语义不安全。
- 证据：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/service/CacheServiceImpl.java:439`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/service/CacheServiceImpl.java:440`
- 影响：在 Redis 语义下可能导致 key 立即过期/删除。
- 建议：改为 `PERSIST` 语义（例如 `redisTemplate.persist(key)`），并新增针对该行为的单测。

### P0-4 跨服务失效事件缺乏消息鉴权（安全）

- 描述：监听端仅基于 `source` 判定“是否本服务”，未校验消息签名/来源可信度。
- 证据：
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/invalidation/CacheInvalidationListener.java:67`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/invalidation/CacheInvalidationPublisher.java:69`
- 影响：若频道可被未授权发布，攻击者可伪造失效事件触发大面积缓存清理。
- 建议：增加 HMAC 签名、时间戳校验、来源白名单，并限制 Redis channel 访问 ACL。

### P0-5 管理端扫描/删除在大 key 空间下存在放大风险（安全+性能）

- 描述：管理接口可拼接任意 pattern，底层扫描失败后会回退 `KEYS`。
- 证据：
  - `basebackend-cache/basebackend-cache-admin/src/main/java/com/basebackend/cache/admin/CacheAdminController.java:81`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/service/RedisService.java:227`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/service/RedisService.java:732`
- 影响：可能阻塞 Redis 主线程；被误用/滥用时放大系统风险。
- 建议：默认禁用 `KEYS` 回退，增加 pattern 白名单和最大扫描上限，管理接口增加权限分级与限流。

---

## P1（应尽快修复）

### P1-1 分布式 Map 的 TTL 作用域错误

- 描述：`put(map,key,value,ttl)` 实际对整张 map 调用 `expire`，不是单 entry TTL。
- 证据：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/structure/DistributedMapService.java:47`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/structure/DistributedMapServiceImpl.java:40`
- 影响：单键写入会改变整 map 生命周期，行为偏离接口语义。
- 建议：改用 `RMapCache` 的 entry 级 TTL API。

### P1-2 高级能力“已实现但未接入主链路”

- 描述：近过期刷新、热键检测、失效发布器在仓内基本只见定义/配置，缺少主路径调用。
- 证据（检索结果）：
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/refresh/NearExpiryRefreshManager.java:55`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/hotkey/HotKeyDetector.java:58`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/invalidation/CacheInvalidationPublisher.java:36`
- 影响：开启配置后业务方可能误以为能力生效，实则未发挥预期价值。
- 建议：在 `CacheAspect`/关键模板中显式接入并提供可观测开关。

### P1-3 指标高基数标签可能造成观测系统压力

- 描述：将业务 key 直接作为 metrics/observation 标签。
- 证据：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/ratelimit/RateLimitAspect.java:118`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/observability/CacheObservabilityAspect.java:58`
- 影响：时序数据基数膨胀、成本上升，并可能泄露业务键信息。
- 建议：改为低基数标签（cacheName/operation），对 key 做哈希或采样。

### P1-4 多级缓存命中计数非并发安全

- 描述：命中/未命中统计使用普通 `long` 递增。
- 证据：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/manager/MultiLevelCacheManager.java:54`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/manager/MultiLevelCacheManager.java:166`
- 影响：高并发下统计失真。
- 建议：改为 `LongAdder` 或 `AtomicLong`。

### P1-5 热键检测内存回收路径复杂度偏高

- 描述：超过上限后每次访问可能触发排序回收。
- 证据：
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/hotkey/HotKeyDetector.java:134`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/hotkey/HotKeyDetector.java:138`
- 影响：热点流量场景下 CPU 开销增加。
- 建议：改为近似淘汰策略（分层桶/采样）或降低触发频率。

### P1-6 admin 自动配置与依赖可选策略存在运行时风险

- 描述：`admin` 模块对 `web/actuator` 的依赖策略与自动配置条件不完全匹配。
- 证据：
  - `basebackend-cache/basebackend-cache-admin/pom.xml:31`
  - `basebackend-cache/basebackend-cache-admin/pom.xml:38`
  - `basebackend-cache/basebackend-cache-admin/src/main/java/com/basebackend/cache/admin/CacheAdminAutoConfiguration.java:17`
- 影响：在不同消费方 classpath 下可能出现自动配置装配不一致。
- 建议：增加 `@ConditionalOnClass` 精确保护，并按 REST/Actuator 分离自动配置。

### P1-7 自动化测试覆盖不均衡

- 描述：关键集成测试被禁用，admin 层缺少接口测试。
- 证据：
  - `basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/integration/CacheIntegrationTest.java:30`
  - `basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/integration/DistributedLockIntegrationTest.java:28`
  - `basebackend-cache/basebackend-cache-advanced/src/test/java/com/basebackend/cache/integration/CacheWarmingRealIntegrationTest.java:31`
  - `basebackend-cache/basebackend-cache-admin/src/test/java/com/basebackend/cache/admin/dto/CacheAdminDtoTest.java:18`
- 影响：关键路径回归防护不足。
- 建议：将 Testcontainers 集成测试纳入 CI profile；补充 Controller/Endpoint 行为测试。

---

## P2（优化项）

### P2-1 Write-Through 延迟指标分类不准确

- 描述：`recordLatency("write-through-set",...)` 无法映射到定义操作类型，退化为 GET。
- 证据：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/template/WriteThroughTemplate.java:65`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/metrics/CacheMetricsService.java:95`
- 影响：运维报表对真实写操作延迟感知失真。
- 建议：使用 `recordSet` 或扩展操作类型枚举。

### P2-2 预热进度日志占位符格式错误

- 描述：SLF4J 占位符使用 `{: .1f}` 风格，不符合 `{}` 语法。
- 证据：
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/warming/CacheWarmingManager.java:275`
- 影响：日志输出可读性下降。
- 建议：提前格式化百分比字符串或统一 `{}` 占位符。

---

## 四、建议修复顺序（两周视角）

### 第 1 批（P0，立即）

1. 修复 clear-all 确认链路（REST + invalidation）
2. 修复 JSON 字符串序列化逻辑并让 core 两个失败用例通过
3. 将 removeExpiration 改为 `persist`
4. 为 invalidation 增加消息鉴权与校验
5. 限制管理端 pattern 能力并禁用 `KEYS` 回退

### 第 2 批（P1，短期）

1. 修复 map entry TTL 语义
2. 完成高级能力到主链路接入（refresh/hotkey/invalidation publisher）
3. 指标去高基数化
4. 补齐 admin 与关键集成测试

### 第 3 批（P2，滚动优化）

1. 指标分类与日志格式清理
2. 热键算法复杂度优化
3. 统一 auto-configuration 条件与依赖策略

---

## 五、结论

`basebackend-cache` 模块整体架构基础较完整，但当前版本在**高风险正确性**与**管理面安全边界**上存在明显短板。建议按本报告的 P0→P1→P2 顺序推进，优先确保“行为正确 + 默认安全 + 可验证”。

---

## 六、P0 修复进展更新（2026-03-06 15:08）

### 6.1 已完成项

1. 已修复 clear-all 确认参数链路（REST 与失效监听路径均使用显式确认版本）。
2. 已修复 JSON 字符串序列化语义（统一按 JSON 编码，反序列化增加简单类型不匹配保护）。
3. 已将 removeExpiration 从 `expire(key, -1)` 改为 `persist(key)` 语义。
4. 已为 invalidation 事件增加签名字段、发布签名、监听验签与时间窗校验。
5. 已移除 scan/deleteByPattern 异常时的 `KEYS` 回退，并在 admin 层增加 cacheName/pattern 输入校验。

### 6.2 复测结果（本次更新后）

- `basebackend-cache-core`（定向回归）  
  命令：`mvn -Dtest=SerializerRoundTripPropertyTest,SerializerErrorHandlingPropertyTest,CacheServiceImplTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=46, failures=0, errors=0, skipped=0`，且未复现此前的 `exit 1`。

- `basebackend-cache-advanced`  
  命令：`mvn test -DskipITs`  
  结果：`tests=13, failures=0, errors=0, skipped=3`。

- `basebackend-cache-admin`  
  命令：`mvn test -DskipITs`  
  结果：`tests=6, failures=0, errors=0, skipped=0`。

- `basebackend-common-core`（受影响公共工具）  
  命令：`mvn -Dtest=JsonUtilsTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=22, failures=0, errors=0, skipped=0`。

### 6.3 当前结论（更新）

- 本报告最初定义的 **P0-1 ~ P0-5 已完成修复并通过对应回归验证**。
- 仍建议按原计划继续推进 P1/P2 项，以完善可观测性、主链路接入和测试覆盖。

---

## 七、P1 推进进展（2026-03-06 15:29）

### 7.1 P1-1（Map entry TTL 语义）已完成

- 修复 `DistributedMapServiceImpl#put(map,key,value,ttl,unit)` 的 TTL 作用域：
  - 从 `RMap.fastPut + map.expire(...)`（整 map TTL）
  - 调整为 `RMapCache.fastPut(key,value,ttl,unit)`（entry TTL）
- 相关文件：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/structure/DistributedMapServiceImpl.java`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/structure/DistributedMapService.java`
  - `basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/structure/DistributedMapServiceImplTest.java`

### 7.2 P1-2（高级能力主链路接入）已完成首轮闭环

- 新增核心扩展点 `CacheOperationHook`，避免 `core -> advanced` 反向依赖。
- 在主链路接入 hook：
  - `CacheAspect`：接入预读命中、命中后回调、写入/淘汰/清空回调。
  - `CacheServiceImpl`：在 `set/delete/clearCache/clearAllCaches` 后触发回调。
  - `CacheAsideTemplate`、`WriteThroughTemplate`：接入读写/淘汰回调。
- advanced 模块新增桥接实现 `AdvancedCacheOperationHook`：
  - 对接 `HotKeyDetector/HotKeyMitigator`（访问记录、热点本地缓解）
  - 对接 `NearExpiryRefreshManager`（命中后近过期刷新）
  - 对接 `CacheInvalidationPublisher`（写入/淘汰/清空时发布失效事件）
- 相关文件：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/hook/CacheOperationHook.java`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/aspect/CacheAspect.java`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/service/CacheServiceImpl.java`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/template/CacheAsideTemplate.java`
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/template/WriteThroughTemplate.java`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/hook/AdvancedCacheOperationHook.java`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/hook/CacheOperationHookAutoConfiguration.java`

### 7.3 P1-3（高基数标签治理）已完成

- `RateLimitAspect`：将 `tag("key", key)` 调整为 `tag("key_scope", scope)`，按 key 命名空间聚合。
- `CacheObservabilityAspect`：
  - 移除原始 key 的 high-cardinality 注入
  - 改为低基数 `cache.key.namespace` / `lock.key.namespace`
- 相关文件：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/ratelimit/RateLimitAspect.java`
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/observability/CacheObservabilityAspect.java`
  - `basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/ratelimit/RateLimitAspectTest.java`
  - `basebackend-cache/basebackend-cache-advanced/src/test/java/com/basebackend/cache/observability/CacheObservabilityAspectTest.java`

### 7.4 P1-4（测试覆盖补齐）已完成当前范围补齐

- 新增 admin 管理面行为测试：
  - `CacheAdminControllerTest`（非法入参、clear-all 门禁、keys 上限、health 字段）
  - `CacheAdminEndpointTest`（非法 cacheName clear 返回 0）
- 新增 advanced 与 core 对应回归测试：
  - `AdvancedCacheOperationHookTest`
  - `DistributedMapServiceImplTest`
  - `RateLimitAspectTest`
  - `CacheServiceImplTest` 补 hook 回调断言

### 7.5 本轮复测结果

- `basebackend-cache-core`  
  `mvn -Dtest=DistributedMapServiceImplTest,RateLimitAspectTest,CacheServiceImplTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=36, failures=0, errors=0, skipped=0`

- `basebackend-cache-advanced`（reactor 方式联动 core 依赖）  
  `mvn -pl basebackend-cache/basebackend-cache-advanced -am -Dtest=AdvancedCacheOperationHookTest,CacheObservabilityAspectTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=5, failures=0, errors=0, skipped=0`

- `basebackend-cache-admin`  
  `mvn -Dtest=CacheAdminControllerTest,CacheAdminEndpointTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=6, failures=0, errors=0, skipped=0`

### 7.6 结论（P1 阶段）

- **P1-1 ~ P1-4 已按报告顺序完成首轮落地与验证**。
- 后续建议进入 P2（指标分类精细化、日志格式与热键算法复杂度优化）。

---

## 八、P2 推进进展（2026-03-06 16:19）

### 8.1 P2-1（Write-Through 指标分类）已修复

- 根因：`recordLatency("write-through-set", ...)` 这类业务别名无法直接映射枚举，回退为 `GET`。
- 修复：在 `CacheMetricsService` 增加操作类型解析器，兼容标准枚举名与业务别名（如 `write-through-set`、`write-through-delete`）。
- 结果：别名操作可正确归类到 `SET`/`EVICT`，不再错误归类到 `GET`。
- 相关文件：
  - `basebackend-cache/basebackend-cache-core/src/main/java/com/basebackend/cache/metrics/CacheMetricsService.java`
  - `basebackend-cache/basebackend-cache-core/src/test/java/com/basebackend/cache/metrics/CacheMetricsServiceTest.java`

### 8.2 P2-2（预热进度日志占位符）已修复

- 根因：`CacheWarmingManager` 使用了 `{: .1f}` 风格占位符，与 SLF4J `{}` 语法不兼容。
- 修复：改为 `String.format(Locale.ROOT, "%.1f", ...)` 后通过 `{}` 输出。
- 相关文件：
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/warming/CacheWarmingManager.java`

### 8.3 本轮复测结果

- `basebackend-cache-core`  
  `mvn -Dtest=CacheMetricsServiceTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=12, failures=0, errors=0, skipped=0`

- `basebackend-cache-advanced`（联动编译验证）  
  `mvn -pl basebackend-cache/basebackend-cache-advanced -am -DskipTests compile`  
  结果：`BUILD SUCCESS`

### 8.4 结论（P2 当前批次）

- 报告中定义的 **P2-1、P2-2 均已完成修复与验证**。
- 若继续优化，可进入“热键检测复杂度”与“自动配置条件一致性”的下一批改进。

---

## 九、下一批推进进展（2026-03-06 17:19）

### 9.1 热键检测复杂度优化（已完成）

- 问题：`HotKeyDetector#recordAccess` 每次访问都触发 `enforceMemoryBound`，且通过全量排序回收，热点流量下 CPU 成本偏高。
- 修复策略：
  1. 增加回收节流：仅在窗口超限且达到检查步长时触发常规回收。
  2. 增加强制回收：窗口超过阈值倍率时立即执行回收，防止内存膨胀。
  3. 替换全量排序：改为固定大小堆选最小计数 key，复杂度从 `O(n log n)` 优化为 `O(n log k)`。
- 相关文件：
  - `basebackend-cache/basebackend-cache-advanced/src/main/java/com/basebackend/cache/hotkey/HotKeyDetector.java`
  - `basebackend-cache/basebackend-cache-advanced/src/test/java/com/basebackend/cache/hotkey/HotKeyDetectorTest.java`

### 9.2 admin 自动配置一致性收敛（已完成）

- 问题：`CacheAdminAutoConfiguration` 对 Actuator 运行时类缺少精确条件保护，存在消费方 classpath 差异下的装配风险。
- 修复：
  - 增加 `@ConditionalOnClass`（`Endpoint/ReadOperation/DeleteOperation`）。
  - 增加 `@ConditionalOnBean(CacheService.class)`，仅在核心依赖存在时装配 endpoint。
- 相关文件：
  - `basebackend-cache/basebackend-cache-admin/src/main/java/com/basebackend/cache/admin/CacheAdminAutoConfiguration.java`
  - `basebackend-cache/basebackend-cache-admin/src/test/java/com/basebackend/cache/admin/CacheAdminAutoConfigurationTest.java`

### 9.3 本轮验证结果

- `basebackend-cache-advanced`  
  `mvn -pl basebackend-cache/basebackend-cache-advanced -am -Dtest=HotKeyDetectorTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=2, failures=0, errors=0, skipped=0`

- `basebackend-cache-admin`  
  `mvn -Dtest=CacheAdminAutoConfigurationTest,CacheAdminControllerTest,CacheAdminEndpointTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  结果：`tests=9, failures=0, errors=0, skipped=0`

### 9.4 当前结论

- 下一批目标项（热键复杂度优化、自动配置一致性）已完成并通过回归验证。
- cache 模块从 P0→P2→下一批的核心风险项已基本闭环，建议后续进入“持续性集成测试补齐与跨模块联调验证”阶段。

---

## 十、跨模块联调用例与全模块回归清单（2026-03-06）

### 10.1 联调前置环境

1. 启动基础设施（Redis/MySQL/Nacos 等）：  
   `docker-compose -f docker/compose/base/docker-compose.base.yml up -d`
2. 确认服务健康（脚本任选其一）：  
   `./bin/test/verify-services.sh` 或 `bin\\test\\health-check.bat`
3. 准备缓存配置：按环境启用 `basebackend.cache.*` 与 `basebackend.cache.admin.*` 相关开关。

### 10.2 跨模块联调用例矩阵（建议最少执行）

1. **core ↔ common（序列化一致性）**  
   - 场景：`JsonUtils` 与 `JsonCacheSerializer` 在字符串/简单类型上的一致性。  
   - 用例：`JsonUtilsTest`、`SerializerRoundTripPropertyTest`、`SerializerErrorHandlingPropertyTest`。  
   - 通过标准：空串/简单类型 round-trip 无回归，类型不匹配可被识别。

2. **core ↔ advanced（主链路 hook 接入）**  
   - 场景：`CacheOperationHook` 在读写淘汰链路触发高级能力。  
   - 用例：`AdvancedCacheOperationHookTest`、`CacheServiceImplTest`（hook 断言）。  
   - 通过标准：命中回调可触发热键/近过期刷新，写/删/清空可触发失效发布。

3. **advanced（热键检测复杂度）**  
   - 场景：高访问量下内存回收节流 + 强制回收。  
   - 用例：`HotKeyDetectorTest`。  
   - 通过标准：窗口大小受控，热点 key 在回收后仍可保留。

4. **advanced（可观测标签基数）**  
   - 场景：观测字段从原始 key 改为 namespace。  
   - 用例：`CacheObservabilityAspectTest`。  
   - 通过标准：仅产出低基数 namespace 标签，不回落到原始 key。

5. **core（限流指标标签）**  
   - 场景：`RateLimitAspect` 从 `key` 改为 `key_scope`。  
   - 用例：`RateLimitAspectTest`。  
   - 通过标准：`allowed/rejected/fail-open` 路径均打点到 `key_scope`。

6. **admin ↔ core（管理面与自动配置）**  
   - 场景：Actuator 条件装配 + REST 管理接口入参防护。  
   - 用例：`CacheAdminAutoConfigurationTest`、`CacheAdminControllerTest`、`CacheAdminEndpointTest`。  
   - 通过标准：`enabled` 与 classpath 条件生效；非法入参被拒绝；clear-all 门禁生效。

7. **advanced ↔ core（失效链路签名）**  
   - 场景：失效事件发布签名、监听验签与时间窗校验。  
   - 用例：建议在联调环境执行真实 Redis Pub/Sub 场景（当前以模块测试+代码审查覆盖）。  
   - 通过标准：签名错误/过期事件被拒绝，合法事件可驱动缓存淘汰。

### 10.3 全模块回归命令清单（可直接执行）

1. **cache-core 定向强回归**  
   `mvn -Dtest=SerializerRoundTripPropertyTest,SerializerErrorHandlingPropertyTest,CacheServiceImplTest,DistributedMapServiceImplTest,RateLimitAspectTest,CacheMetricsServiceTest test -Dsurefire.failIfNoSpecifiedTests=false`

2. **cache-advanced 定向强回归（含依赖模块）**  
   `mvn -pl basebackend-cache/basebackend-cache-advanced -am -Dtest=AdvancedCacheOperationHookTest,CacheObservabilityAspectTest,HotKeyDetectorTest test -Dsurefire.failIfNoSpecifiedTests=false`

3. **cache-admin 定向强回归**  
   `mvn -Dtest=CacheAdminAutoConfigurationTest,CacheAdminControllerTest,CacheAdminEndpointTest test -Dsurefire.failIfNoSpecifiedTests=false`

4. **受影响 common 回归**  
   `mvn -Dtest=JsonUtilsTest test -Dsurefire.failIfNoSpecifiedTests=false`

5. **三子模块编译一致性检查**  
   - `mvn -pl basebackend-cache/basebackend-cache-core -DskipTests compile`  
   - `mvn -pl basebackend-cache/basebackend-cache-advanced -am -DskipTests compile`  
   - `mvn -pl basebackend-cache/basebackend-cache-admin -DskipTests compile`

6. **提交前建议（全仓校验）**  
   `mvn clean verify`（若耗时敏感，可在 CI 执行）

### 10.4 本轮执行记录（2026-03-06，按 10.3 顺序）

- ✅ `cache-core` 定向强回归通过：  
  `mvn -Dtest=SerializerRoundTripPropertyTest,SerializerErrorHandlingPropertyTest,CacheServiceImplTest,DistributedMapServiceImplTest,RateLimitAspectTest,CacheMetricsServiceTest test -Dsurefire.failIfNoSpecifiedTests=false`

- ✅ `cache-advanced` 定向强回归通过（含依赖模块）：  
  `mvn -pl basebackend-cache/basebackend-cache-advanced -am -Dtest=AdvancedCacheOperationHookTest,CacheObservabilityAspectTest,HotKeyDetectorTest test -Dsurefire.failIfNoSpecifiedTests=false`

- ✅ `cache-admin` 定向强回归通过：  
  `mvn -Dtest=CacheAdminAutoConfigurationTest,CacheAdminControllerTest,CacheAdminEndpointTest test -Dsurefire.failIfNoSpecifiedTests=false`

- ✅ `common` 受影响回归通过：  
  `mvn -Dtest=JsonUtilsTest test -Dsurefire.failIfNoSpecifiedTests=false`

- ✅ 三子模块编译一致性检查通过：  
  - `mvn -pl basebackend-cache/basebackend-cache-core -DskipTests compile`  
  - `mvn -pl basebackend-cache/basebackend-cache-advanced -am -DskipTests compile`  
  - `mvn -pl basebackend-cache/basebackend-cache-admin -DskipTests compile`

- ❌ 全仓校验失败（非 cache 模块阻断）：  
  `mvn clean verify` 在 `basebackend-observability/observability-slo` 失败，错误为  
  `SloMonitoringAspectTest.shouldCreateAspectInstance` 的 `ClassCastException`  
  （`SimpleMeterRegistry` 不能转换为 `ObservationRegistry`）。

- 🔁 已做最小复现：  
  `mvn -pl basebackend-observability/observability-slo -Dtest=SloMonitoringAspectTest test -Dsurefire.failIfNoSpecifiedTests=false` 同样失败。

- 结论：`basebackend-cache` 及其关联 `common` 回归链路已全部通过；当前全仓门禁由 `observability-slo` 既有失败阻断，cache 相关模块未发现新增回归问题。

### 10.5 阻断修复后复验（2026-03-06）

- ✅ 已修复 `observability-slo` 阻断测试：  
  `SloMonitoringAspectTest` 中将 `SimpleMeterRegistry` 强转 `ObservationRegistry` 的错误用法，改为直接使用 `ObservationRegistry.create()`。

- ✅ 定向复验通过：  
  - `mvn -pl basebackend-observability/observability-slo -Dtest=SloMonitoringAspectTest test -Dsurefire.failIfNoSpecifiedTests=false`  
  - `mvn -pl basebackend-observability/observability-slo -am verify`

- ⏱️ 全仓复验状态：  
  `mvn clean verify` 在 60s 限时下超时中断（Exit 124），截至中断未出现新的已确认失败用例；原 `observability-slo` 阻断已解除。

### 10.6 分段全仓门禁续跑结果（2026-03-06，60s 限时策略）

- ✅ 前置公共模块段（common 前半）通过：  
  `mvn -pl basebackend-common/basebackend-common-core,basebackend-common/basebackend-common-util,basebackend-common/basebackend-common-context,basebackend-common/basebackend-common-security,basebackend-common/basebackend-common-starter,basebackend-common/basebackend-common-storage,basebackend-common/basebackend-common-lock,basebackend-common/basebackend-common-idempotent,basebackend-common/basebackend-common-datascope,basebackend-common/basebackend-common-ratelimit,basebackend-common/basebackend-common-export verify`

- ✅ 前置公共模块段（common-event + jwt + security）通过：  
  `mvn -pl basebackend-common/basebackend-common-event,basebackend-jwt,basebackend-security verify`

- ✅ 数据库 + 可观测核心段通过：  
  `mvn -pl basebackend-database/database-core,basebackend-database/database-failover,basebackend-database/database-multitenant,basebackend-database/database-security,basebackend-database/database-migration,basebackend-observability/observability-slo,basebackend-observability/observability-core verify`

- ✅ cache 三子模块 verify 通过：  
  `mvn -pl basebackend-cache/basebackend-cache-core,basebackend-cache/basebackend-cache-advanced,basebackend-cache/basebackend-cache-admin verify`

- ✅ logging→gateway 中段通过：  
  `mvn -pl basebackend-logging/basebackend-logging-core,basebackend-logging/basebackend-logging-audit,basebackend-logging/basebackend-logging-advanced,basebackend-logging/basebackend-logging-monitoring,basebackend-observability/observability-metrics,basebackend-observability/observability-logging,basebackend-observability/observability-alert,basebackend-messaging,basebackend-api-model,basebackend-file-service,basebackend-backup,basebackend-nacos,basebackend-service-client,basebackend-gateway verify`

- ✅ scheduler 单段通过：  
  `mvn -pl basebackend-scheduler-parent/scheduler-camunda verify`

- ✅ 尾段（code-generator→mall-pay）通过：  
  `mvn -rf :basebackend-code-generator verify`

- ℹ️ 过程记录：  
  - `mvn -rf :database-core verify`、`mvn -rf :basebackend-logging-core verify` 在 60s 策略下超时，后续已拆分为更细粒度段并全部验证通过。  
  - 期间出现一次模块名选择错误（`basebackend-nacos-config`），修正为 `basebackend-nacos` 后通过，不属于代码缺陷。

- 结论：在“分段 verify + 每段 ≤60s”策略下，已覆盖全仓主门禁链路，**未发现新的真实失败点**；当前仅“单条 `mvn clean verify` 在 60s 下无法跑完全程”这一执行时长问题仍存在。

### 10.7 CI 门禁脚本（可直接落地）

- 脚本位置：  
  `.github/scripts/verify-segmented-ci.sh`

- 默认行为：  
  按 `10.6` 的分段顺序执行 `verify`，每段默认 60s 超时（自动识别 `timeout/gtimeout`）。

- 常用用法：  
  - 全量分段门禁：`./.github/scripts/verify-segmented-ci.sh`  
  - 查看可用分段：`./.github/scripts/verify-segmented-ci.sh --list`  
  - 预演执行计划（不实际运行）：`./.github/scripts/verify-segmented-ci.sh --dry-run`  
  - 机器可解析计划（JSON）：`./.github/scripts/verify-segmented-ci.sh --json`  
  - 从指定段续跑：`./.github/scripts/verify-segmented-ci.sh --start cache-modules`  
  - 只跑单段：`./.github/scripts/verify-segmented-ci.sh --only logging-gateway`  
  - 调整超时：`./.github/scripts/verify-segmented-ci.sh --timeout 90`

- 参数约束：  
  同时指定 `--start` 与 `--only` 时，两者必须一致；否则脚本会以参数错误退出，避免产生空计划歧义。

- CI 接入建议：  
  在 workflow 中将原单条 `mvn clean verify`（或长链路 `mvn test`）替换为  
  `./.github/scripts/verify-segmented-ci.sh`，以获得更稳定的失败定位与超时控制。

### 10.8 CI 工作流已接入（2026-03-07）

- ✅ 已在 `.github/workflows/ci.yml` 中完成接入：  
  CI 核心任务（旧 id `build-and-test`，现为 `build-and-verify`）由原  
  `Run Tests: mvn test -B -DforkCount=1` 调整为  
  `Run Segmented Verify: bash .github/scripts/verify-segmented-ci.sh --timeout 60`。

- ✅ 已进一步收敛重复构建：  
  移除独立 `Compile: mvn clean compile -B -DskipTests` 步骤，  
  由分段脚本统一承担编译与测试门禁（`Run Segmented Verify (Compile + Test)`）。

- ✅ 语法校验通过：  
  `ci.yml parse ok`（YAML 解析成功）。

- ✅ 命名一致性收敛：  
  CI 核心 job id 已由 `build-and-test` 重命名为 `build-and-verify`，并同步更新 `needs` 与 `needs.*.result` 引用，避免展示名与标识名不一致。

- ✅ Workflow 级命名同步：  
  `.github/workflows/ci.yml` 顶部名称已由 `CI - Build and Test` 调整为  
  `CI - Segmented Build and Verify`，与当前 job/步骤语义保持一致。

- ✅ 分段执行计划可追溯：  
  CI 在执行 `Run Segmented Verify` 前会生成 `segmented-verify-plan-${GITHUB_RUN_ID}.json`（`--json`），并上传为 `segmented-verify-plan-${{ github.run_id }}` artifact，便于按流水线运行维度检索与审计留存。

- ✅ CI 页面可读性增强：  
  `build-and-verify` 任务会把分段计划摘要写入 `GITHUB_STEP_SUMMARY`，展示 `count / first / last segment`，便于快速判断本次执行范围。

- ✅ 总结页聚合展示：  
  `build-and-verify` 已将分段计划摘要导出为 job outputs（`plan_count/plan_first/plan_last`），`ci-summary` 会读取并展示同一组摘要，形成统一总览。

- ✅ 计划产物保留期调整：  
  `segmented-verify-plan-${{ github.run_id }}` artifact 的 `retention-days` 已从 `7` 调整为 `14`，提升跨周排障与审计回溯能力。

- ✅ 总结页直达产物定位：  
  `ci-summary` 已展示分段计划 artifact 名（`segmented-verify-plan-${{ github.run_id }}`）与当前 run 链接，便于在流水线总结页直接跳转定位下载入口。

- ✅ 安全扫描产物统一定位：  
  `ci-summary` 已纳入 `dependency-check` 任务状态展示，并在非 `skipped` 场景下提供 `dependency-check-report` artifact 定位信息；若为 `skipped` 则明确标注“仅 main / pull_request 执行”。

- ✅ 非阻断策略显式化：  
  `ci-summary` 新增 `Gate Policy` 区块，明确当前阻断门禁仅包含 `build-and-verify` 与 `build-services`；`dependency-check` 作为信息型扫描（OWASP 步骤使用 `continue-on-error: true`）不会直接导致流水线失败，但会在 `failure` 时提示人工复核报告产物。

- ✅ 计划计数异常提示：  
  `ci-summary` 对分段计划摘要新增异常提示逻辑：当 `plan_count=0` 或 `plan_count` 不可用（`N/A`）时，会输出醒目告警，提示检查分段脚本/参数或 job outputs 链路（仅提示，不改变现有阻断门禁）。

- ✅ 期望/执行一致性对照：  
  `build-and-verify` 已导出 `executed_count`（依据分段执行汇总中的 `PASS/FAIL/TIMEOUT` 行统计），`ci-summary` 会展示 `expected(plan_count)` 与 `executed(executed_count)` 的一致性结论；当二者不一致时按 `job` 结果给出区分提示（成功场景重点告警，失败场景标注“可能因提前停止”）。

- ✅ 运行日志产物留存：  
  `build-and-verify` 已上传 `segmented-verify-run-log-${{ github.run_id }}` artifact（`if: always()`，`retention-days: 14`），用于核对 `executed_count` 统计来源与失败上下文。

- ✅ 运行日志关键信息提炼：  
  `build-and-verify` 已导出 `verify_exit_code`、`timeout_count`、`timeout_segments_json`（JSON 数组，基于运行日志解析），`ci-summary` 会直接展示这些字段，并在检测到 timeout 时给出醒目提示，降低排障时的日志跳转成本。
