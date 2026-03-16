# basebackend-database 模块全面审查报告（2026-03-06）

## 1. 审查范围与方法

- 审查范围：`database-core`、`database-multitenant`、`database-security`、`database-migration`、`database-failover`
- 审查方式：静态代码审查 + 配置审查 + 并行子审查 + 模块测试执行
- 测试命令：`mvn test -DskipITs`（工作目录：`basebackend-database`）

## 2. 总体结论

- 总体风险等级：**高风险**
- 主要风险集中在：
  1. **Fail-Open 安全策略**（租户隔离/数据权限在异常时放行）
  2. **迁移备份恢复正确性**（恢复过程可能污染数据）
  3. **加密基线不足**（默认密钥兜底、ECB 模式）
  4. **关键模块测试缺失**（`database-security`/`database-migration`/`database-failover`）

## 3. 问题分级统计

- **P0（严重）**：6 项
- **P1（高）**：9 项
- **P2（中）**：7 项
- **P3（低）**：2 项

---

## 4. 关键问题清单

### 4.1 P0 严重问题

1. **租户 SQL 重写失败后放行原 SQL（越权风险）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:71`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:73`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:130`
   - 说明：租户过滤失败仅记录日志，继续执行原 SQL。

2. **SELECT 仅处理 `PlainSelect`，复杂查询可能未注入租户条件**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:149`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:160`
   - 说明：对非 `PlainSelect` 场景未做强制处理，存在绕过过滤风险。

3. **SEPARATE_DB 路由键不一致（`dataSourceKey` vs `tenantId`）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/router/TenantDataSourceRouter.java:105`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/router/TenantDataSourceRouter.java:181`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/service/impl/TenantDataSourceServiceImpl.java:144`
   - 说明：查找与注册 key 口径不一致，可能导致路由错误。

4. **数据权限重写失败/提取失败后放行**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/DataScopeInterceptor.java:101`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/DataScopeInterceptor.java:130`
   - 说明：解析失败直接 `proceed`，属于 fail-open。

5. **开启加密但缺失密钥时使用默认硬编码密钥**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:40`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:41`
   - 说明：生产场景可能在未配置密钥下继续运行，形成伪加密。

6. **迁移备份“恢复”逻辑会污染数据**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:247`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:262`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:297`
   - 说明：恢复脚本会按当前表结构建 `_backup`，并向原表追加插入，可能导致重复/漂移。

### 4.2 P1 高优先问题

1. **迁移高危入口在模块内未见强制鉴权/确认门禁**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/controller/MigrationController.java:76`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/controller/MigrationController.java:123`

2. **确认 token 存在并发复用竞态（先验后删）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:132`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:142`

3. **迁移 token 明文写日志**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:193`

4. **备份恢复执行非原子，且 `;` 分割脚本不可靠**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:293`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:297`

5. **审计归档部分失败后仍按原条件删除，存在日志丢失风险**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/audit/service/impl/AuditLogArchiveServiceImpl.java:59`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/audit/service/impl/AuditLogArchiveServiceImpl.java:69`

6. **慢 SQL 记录原 SQL + 原参数，可能泄露敏感信息**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/logger/SlowQueryLogger.java:61`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/interceptor/SqlExecutionTimeInterceptor.java:114`

7. **加密算法基线偏弱（ECB + SHA1PRNG）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:28`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:56`

8. **权限脱敏/查询解密异常时均放行原数据**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/PermissionMaskingInterceptor.java:60`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/DecryptionInterceptor.java:54`

9. **健康状态变化告警逻辑失效（先写入后比较）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/scheduler/HealthCheckScheduler.java:89`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/scheduler/HealthCheckScheduler.java:92`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/scheduler/HealthCheckScheduler.java:188`

### 4.3 P2 中优先问题

1. **线程池包装未传播上下文，且无上下文时不清理**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/context/DataSourceContextHolder.java:154`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/context/DataSourceContextHolder.java:158`

2. **动态移除数据源后未关闭连接池资源**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/DynamicDataSource.java:236`

3. **`testDataSourceConnection` 为占位实现（固定返回 true）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/manager/DataSourceManager.java:168`

4. **自动配置总开关前缀与属性类前缀不一致**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/config/DatabaseEnhancedAutoConfiguration.java:23`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/config/DatabaseEnhancedProperties.java:16`

5. **租户字段默认值是列名，自动填充按属性名判断**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/config/DatabaseEnhancedProperties.java:156`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/handler/TenantMetaObjectHandler.java:32`

6. **加密前缀判断过于宽松，存在“伪已加密”绕过空间**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:79`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:128`

7. **`database-security` 中重复注册解密拦截器 Bean**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/config/EncryptionConfig.java:57`
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/config/DatabaseSecurityAutoConfiguration.java:34`

### 4.4 P3 低优先问题

1. **忽略路径匹配过宽（`startsWith`）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/filter/TenantContextFilter.java:88`

2. **测试环境判定逻辑不可靠（`getStackTrace().toString()`）**
   - 位置：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/DynamicDataSource.java:60`

---

## 5. 测试与验证结果

### 5.1 执行结果

- 命令：`mvn test -DskipITs`
- 结果：**BUILD SUCCESS**
- 统计：
  - `database-core`：45 tests, 0 failures
  - `database-multitenant`：32 tests, 0 failures
  - 全模块合计：77 tests, 0 failures

### 5.2 覆盖缺口

- `database-failover`：`src/test/java` 下无测试文件
- `database-security`：`src/test/java` 下无测试文件
- `database-migration`：`src/test/java` 下无测试文件
- `database-core` 多个集成测试文件整文件被注释，未实际执行：
  - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/dynamic/NestedDataSourceIntegrationTest.java:1`
  - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/health/HealthMonitoringIntegrationTest.java:1`
  - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/security/PermissionControlIntegrationTest.java:1`

---

## 6. 现有亮点

- 动态数据源上下文采用栈式结构，具备嵌套切换基础能力：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/context/DataSourceContextHolder.java:23`
- 数据源切换切面在 `finally` 清理上下文，基础防泄漏设计正确：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/aspect/DataSourceAspect.java:72`
- SQL 注入拦截器具备白名单、严格模式、统计能力：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/interceptor/SqlInjectionPreventionInterceptor.java:119`
- 连接池监控具备阈值告警与节流：`/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/monitor/ConnectionPoolMonitor.java:146`

---

## 7. 修复优先级建议（按顺序）

1. **先修 P0 安全与数据正确性**
   - 租户过滤/DataScope 全部改为 fail-close
   - 修复迁移恢复实现（避免污染数据）
   - 去除默认密钥兜底

2. **再修 P1 稳定性与审计完整性**
   - 修复归档删除逻辑
   - 修复 token 竞态与日志泄露
   - 修复健康状态变化检测与慢 SQL 脱敏日志

3. **最后补 P2/P3 工程质量**
   - 资源释放、配置前缀一致性、占位实现去除
   - 测试恢复与高风险场景补测

---

## 8. 已完成修复（第一批，2026-03-06）

> 说明：本节为本次审查后的已落地修复，作为“审查发现”后的整改进度更新。

1. **租户隔离由 fail-open 改为 fail-close**
   - `TenantInterceptor` 在 SQL 解析/重写失败时改为抛异常阻断，不再放行原 SQL。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:73`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:132`

2. **租户 SELECT 结构处理增强（复杂查询防绕过）**
   - 新增对 `PlainSelect` / `SetOperationList` / `ParenthesedSelect` 等结构的递归处理。
   - 对不支持结构直接抛异常阻断，避免静默放行。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:159`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/interceptor/TenantInterceptor.java:194`

3. **数据权限拦截由 fail-open 改为 fail-close**
   - `DataScopeInterceptor` 在受限权限（非 `ALL`）且无法解析主表时改为阻断执行。
   - 数据权限 SQL 重写失败改为抛异常阻断。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/DataScopeInterceptor.java:107`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/DataScopeInterceptor.java:137`

4. **SEPARATE_DB 路由键统一（tenantId 与 lookupKey 解耦）**
   - 路由器新增 tenantId→lookupKey 映射，注册/查询/移除统一按 lookupKey 生效。
   - `SEPARATE_DB` 场景下显式使用 `dataSourceKey` 作为路由键，修复历史不一致问题。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/router/TenantDataSourceRouter.java:183`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/service/impl/TenantDataSourceServiceImpl.java:59`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/service/impl/TenantDataSourceServiceImpl.java:147`

5. **新增针对性测试覆盖**
   - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/test/java/com/basebackend/database/tenant/interceptor/TenantInterceptorTest.java:50`
   - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/test/java/com/basebackend/database/tenant/router/TenantDataSourceRouterTest.java:44`
   - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/test/java/com/basebackend/database/security/interceptor/DataScopeInterceptorTest.java:38`
   - `database-security` 已补充测试依赖：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/pom.xml:35`

## 9. 最新回归验证（2026-03-06）

- 执行命令：`mvn -pl database-multitenant,database-security -am test -DskipITs`
- 执行时间：2026-03-06 15:14（Asia/Shanghai）
- 结果：**BUILD SUCCESS**
- 统计：
  - `database-core`：45 tests, 0 failures
  - `database-multitenant`：38 tests, 0 failures
  - `database-security`：2 tests, 0 failures

## 10. 已完成修复（第二批，2026-03-06）

1. **迁移高危入口统一门禁（生产环境）**
   - 在 `MigrationServiceImpl` 增加统一门禁：生产环境且开启 `requireConfirmation` 时，阻断 `migrate` / `migrateWithBackup` 直连执行，要求走确认入口。
   - 同时校验 `MigrationConfirmation` 的 `confirmationToken` / `confirmedBy` / `reason` 非空。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:60`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:92`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:392`

2. **确认 token 改为原子消费（防并发复用）**
   - 新增 `consumeConfirmationToken`，使用原子 `computeIfPresent` 校验+删除一步完成，修复“先验后删”并发竞态。
   - 过期 token 在校验路径中自动清理；日志不再明文输出 token。
   - token 有效期改为读取配置 `database.enhanced.migration.token-validity-minutes`（带安全默认值）。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:200`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:418`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationServiceImpl.java:442`

3. **恢复逻辑重构：事务化执行 + 安全脚本处理**
   - 恢复执行改为显式事务（`setAutoCommit(false)`），失败回滚，避免部分成功导致污染。
   - 执行器改为 SQL 语句级解析（支持字符串中的 `;`），不再使用不可靠的简单 `split(\";\")`。
   - 恢复过程增加备份ID格式校验，阻断路径穿越。
   - 备份脚本生成改为 `DELETE + INSERT(显式列名)` 模式；旧版脚本恢复时自动清表并跳过 `*_backup` 临时表语句，避免重复污染。
   - 恢复成功后写入 `.restored` 标记并回填 `restored/restoreTime`，增强可追踪性。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:93`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:256`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:302`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:394`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImpl.java:533`

4. **控制器侧有效期返回与配置一致**
   - `/generate-token` 接口返回的 `validityMinutes` 改为动态读取配置，不再硬编码 `30`。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/main/java/com/basebackend/database/migration/controller/MigrationController.java:168`

5. **新增测试覆盖（migration 模块）**
   - 新增：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/test/java/com/basebackend/database/migration/service/impl/MigrationServiceImplTest.java:46`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/src/test/java/com/basebackend/database/migration/service/impl/MigrationBackupServiceImplTest.java:56`
   - `database-migration` 新增测试依赖：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-migration/pom.xml:52`

## 11. 第二批回归验证（2026-03-06）

- 命令 1：`mvn -pl database-migration -am test -DskipITs`
  - 结果：**BUILD SUCCESS**
  - `database-migration`：7 tests, 0 failures

- 命令 2：`mvn -pl database-multitenant,database-security,database-migration -am test -DskipITs`
  - 结果：**BUILD SUCCESS**
  - `database-core`：45 tests, 0 failures
  - `database-multitenant`：38 tests, 0 failures
  - `database-security`：2 tests, 0 failures
  - `database-migration`：7 tests, 0 failures

## 12. 已完成修复（第三批，2026-03-06）

1. **加密基线升级（GCM）+ 移除默认密钥兜底**
   - `AESEncryptionService` 加密实现由 `AES/ECB/PKCS5Padding` 升级为 `AES/GCM/NoPadding`，新密文格式为 `ENC:v2:...`。
   - 启用加密但未配置密钥时，改为直接抛出异常并阻断启动，不再使用硬编码默认密钥。
   - 新增历史 ECB 密文兼容解密路径，确保存量数据可读。
   - `isEncrypted` 从“仅前缀判断”升级为“前缀+payload 格式校验”，降低伪前缀绕过风险。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:31`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:49`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:109`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/service/impl/AESEncryptionService.java:157`

2. **审计归档删除条件修复（避免误删主表）**
   - `AuditLogArchiveServiceImpl` 改为仅删除“成功归档”的主表记录（按 `originalLogId` 精确删除）。
   - 对删除数量与归档数量不一致场景增加告警，便于后续排查。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/audit/service/impl/AuditLogArchiveServiceImpl.java:59`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/audit/service/impl/AuditLogArchiveServiceImpl.java:74`

3. **健康状态变化检测修复（先比后告警）**
   - `HealthCheckScheduler` 先读取旧状态，再写入新状态，再进行变化检测，修复“先写后比导致检测失效”问题。
   - 手动触发健康检查路径也统一复用变化检测逻辑。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/scheduler/HealthCheckScheduler.java:88`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/scheduler/HealthCheckScheduler.java:191`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/scheduler/HealthCheckScheduler.java:231`

4. **新增测试覆盖（第三批）**
   - 新增：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/test/java/com/basebackend/database/security/service/impl/AESEncryptionServiceTest.java:1`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/audit/service/impl/AuditLogArchiveServiceImplTest.java:1`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/health/scheduler/HealthCheckSchedulerTest.java:1`
   - 覆盖点：
     - 缺失密钥阻断、v2 加解密、伪前缀防护、legacy 解密兼容、非法密文异常；
     - 部分归档失败时仅删除已归档记录；
     - 健康状态变化触发告警、状态不变不告警。

## 13. 第三批回归验证（2026-03-06）

- 命令 1：`mvn -pl database-core,database-security -am test -DskipITs`
  - 结果：**BUILD SUCCESS**
  - `database-core`：49 tests, 0 failures
  - `database-security`：7 tests, 0 failures

- 命令 2：`mvn -pl database-core,database-multitenant,database-security,database-migration -am test -DskipITs`
  - 结果：**BUILD SUCCESS**
  - `database-core`：49 tests, 0 failures
  - `database-multitenant`：38 tests, 0 failures
  - `database-security`：7 tests, 0 failures
  - `database-migration`：7 tests, 0 failures

## 14. 已完成修复（第四批，2026-03-06）

1. **慢 SQL 日志脱敏与参数元信息化（防敏感信息泄露）**
   - `SlowQueryLogger` 改为输出安全 SQL（空白归一化、字面量脱敏、长度限制），不再透传原始 SQL 字面量。
   - 慢 SQL 日志中的参数输出改为安全元信息（类型/数量/长度），禁止输出具体参数值。
   - 慢 SQL 告警改为发送安全 SQL，避免告警链路二次泄露。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/logger/SlowQueryLogger.java:24`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/health/interceptor/SqlExecutionTimeInterceptor.java:106`

2. **查询结果后处理改为 fail-close（解密/脱敏）**
   - `DecryptionInterceptor` 在解密异常时不再放行原结果，改为抛出异常阻断返回。
   - `PermissionMaskingInterceptor` 在权限脱敏异常时同样阻断返回，避免明文泄露。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/DecryptionInterceptor.java:57`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/interceptor/PermissionMaskingInterceptor.java:63`

3. **修复解密拦截器重复 Bean 注册**
   - 删除 `DatabaseSecurityAutoConfiguration` 中重复的 `DecryptionInterceptor` Bean，保留 `EncryptionConfig` 中单一来源，避免重复实例化与重复拦截风险。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/main/java/com/basebackend/database/security/config/DatabaseSecurityAutoConfiguration.java:26`

4. **新增测试覆盖（第四批）**
   - 新增：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/health/logger/SlowQueryLoggerTest.java:1`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/health/interceptor/SqlExecutionTimeInterceptorTest.java:1`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/test/java/com/basebackend/database/security/interceptor/DecryptionInterceptorTest.java:1`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-security/src/test/java/com/basebackend/database/security/interceptor/PermissionMaskingInterceptorTest.java:1`
   - 覆盖点：
     - 慢 SQL 日志/告警不泄露明文字面量与参数值；
     - SQL 参数仅输出元信息；
     - 解密失败阻断返回；
     - 权限脱敏失败阻断返回。

## 15. 第四批回归验证（2026-03-06）

- 命令：`mvn -pl database-core,database-security -DskipITs -Dtest=AESEncryptionServiceTest,AuditLogArchiveServiceImplTest,HealthCheckSchedulerTest,SlowQueryLoggerTest,SqlExecutionTimeInterceptorTest,DecryptionInterceptorTest,PermissionMaskingInterceptorTest test -Dsurefire.failIfNoSpecifiedTests=false`
  - 执行目录：`basebackend-database`
  - 结果：**BUILD SUCCESS**
  - 统计：
    - `database-core`：5 tests, 0 failures
    - `database-security`：2 tests, 0 failures

## 16. 已完成修复（第五批，2026-03-08）

1. **线程池上下文传播与清理修复**
   - `DataSourceContextHolder.wrapForExecutor` 改为基于快照传播提交线程上下文到工作线程。
   - 无提交上下文场景也执行清理，避免线程池复用导致脏上下文泄漏。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/context/DataSourceContextHolder.java:148`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/context/DataSourceContextHolder.java:170`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/context/DataSourceContextHolder.java:187`

2. **动态移除数据源后资源释放修复**
   - `DynamicDataSource.removeDataSource` 增加 `AutoCloseable` 资源关闭逻辑，避免连接池资源悬挂。
   - 增加刷新失败回滚保护：重建目标映射失败时回滚并抛出异常，避免状态不一致。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/DynamicDataSource.java:227`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/DynamicDataSource.java:276`

3. **测试环境判定逻辑修复**
   - 去除 `getStackTrace().toString()` 方式，改为系统属性/环境变量/Surefire/classpath 组合判定。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/DynamicDataSource.java:57`

4. **`testDataSourceConnection` 去占位化**
   - `DataSourceManager.testDataSourceConnection` 改为真实健康检测，不再固定返回 `true`。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/dynamic/manager/DataSourceManager.java:161`

5. **自动配置前缀一致性修复**
   - `DatabaseEnhancedAutoConfiguration` 的 `@ConditionalOnProperty` 前缀统一为 `database.enhanced`，与 `DatabaseEnhancedProperties` 对齐。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/main/java/com/basebackend/database/config/DatabaseEnhancedAutoConfiguration.java:23`

6. **租户字段自动填充兼容列名/属性名**
   - `TenantMetaObjectHandler` 新增 `tenantColumn -> camelCase` 回退解析，默认 `tenant_id` 可自动映射到实体 `tenantId`。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/handler/TenantMetaObjectHandler.java:30`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/handler/TenantMetaObjectHandler.java:70`

7. **忽略路径匹配收敛**
   - `TenantContextFilter` 从简单 `startsWith` 改为“精确命中或子路径命中”，避免 `/login2` 误命中 `/login`。
   - 关键位置：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/filter/TenantContextFilter.java:87`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/main/java/com/basebackend/database/tenant/filter/TenantContextFilter.java:91`

8. **新增测试覆盖（第五批）**
   - 新增：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/dynamic/manager/DataSourceManagerTest.java:1`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/config/DatabaseEnhancedAutoConfigurationTest.java:1`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/test/java/com/basebackend/database/tenant/handler/TenantMetaObjectHandlerTest.java:1`
   - 增强：
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/dynamic/context/DataSourceContextHolderTest.java:209`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-core/src/test/java/com/basebackend/database/dynamic/DynamicDataSourceTest.java:188`
     - `/Users/wuan/.openclaw/workspace/basebackend/basebackend-database/database-multitenant/src/test/java/com/basebackend/database/tenant/filter/TenantContextFilterTest.java:115`

## 17. 第五批回归验证（2026-03-08）

- 命令：`mvn -pl database-core,database-multitenant -DskipITs -Dtest=DataSourceContextHolderTest,DynamicDataSourceTest,DataSourceManagerTest,DatabaseEnhancedAutoConfigurationTest,TenantContextFilterTest,TenantMetaObjectHandlerTest test -Dsurefire.failIfNoSpecifiedTests=false`
  - 执行目录：`basebackend-database`
  - 结果：**BUILD SUCCESS**
  - 统计：
    - `database-core`：38 tests, 0 failures
    - `database-multitenant`：9 tests, 0 failures

- 命令：`mvn -pl database-core,database-multitenant,database-security,database-migration -DskipITs test`
  - 执行目录：`basebackend-database`
  - 结果：**BUILD SUCCESS**
  - 统计：
    - `database-core`：45 tests, 0 failures
    - `database-multitenant`：45 tests, 0 failures
    - `database-security`：9 tests, 0 failures
    - `database-migration`：7 tests, 0 failures

## 18. 问题闭环状态（截至 2026-03-08）

- 闭环统计（对应第 4 章问题清单）：
  - P0：6/6 已修复
  - P1：9/9 已修复
  - P2：7/7 已修复
  - P3：2/2 已修复
  - 合计：24/24 已修复，0 项待修复

- 当前遗留风险（非第 4 章分级问题）：
  - `database-failover` 测试覆盖仍较薄，建议后续补齐高风险场景自动化测试（主从切换、恢复、告警抑制）。
