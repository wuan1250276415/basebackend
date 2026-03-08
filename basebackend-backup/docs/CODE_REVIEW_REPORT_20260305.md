# basebackend-backup 全面审查报告（2026-03-06，持续整改）

## 1. 审查范围与方法

- 审查范围：`basebackend-backup` 模块源码、Mapper、配置、告警规则、测试。
- 核心链路：全量/增量备份、恢复（PITR/指定备份）、调度、存储、监控、可靠性。
- 审查方法：静态代码审查 + 本地测试验证（定向与全量）。

### 测试执行结果（本轮整改）

1. `mvn -pl basebackend-backup -Dtest=PostgresBackupExecutorTest test -DfailIfNoTests=false`
   - 结果：`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`
2. `mvn -pl basebackend-backup test -DfailIfNoTests=false`
   - 结果：`Tests run: 159, Failures: 0, Errors: 0, Skipped: 9`
3. `mvn -pl basebackend-backup -Dtest=BackupIntegrationTest test -DfailIfNoTests=false`
   - 结果：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 9`（无 Docker 环境自动跳过）

## 2. 总体结论

- P0 已全部闭环。
- P1 已完成核心闭环：MySQL 增量链路闭环，PostgreSQL 在开关开启时已具备“逻辑快照回放 + WAL 外部回放 + 内建物理回放（含 `pg_basebackup` 基线生命周期、失败自动回滚与可选业务一致性探针）”能力，并默认安全关闭以避免误用。
- P2 已全部完成（含集成测试启用改造）。

## 3. P0 修复闭环（已完成）

### P0-1 调度全量备份与可恢复流水线割裂（已修复）

**修复结果**：
- `AutoBackupScheduler` 已按数据源分发到执行器链路，`mysql/postgresql` 全量备份均走 `AbstractBackupExecutor.execute(...)`，落库 `backup_history` 与 `storage_locations`。
- 新增 `PostgresBackupExecutor`，实现 PostgreSQL 全量备份与恢复执行器。
- PostgreSQL 任务 ID 查询兼容别名：`postgresql -> postgres` 回退。

**证据（链路分组）**：
- 调度入口与执行器分发：`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:58`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:61`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:62`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:145`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:149`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:151`
- PostgreSQL 任务别名兼容：`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:266`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:269`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:274`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:318`
- PostgreSQL 执行器与单测：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:49`、`src/test/java/com/basebackend/backup/scheduler/AutoBackupSchedulerTest.java:74`、`src/test/java/com/basebackend/backup/scheduler/AutoBackupSchedulerTest.java:98`

### P0-2 PITR 在“无增量覆盖目标时间”时可能返回成功（已修复）

**修复结果**：
- `IncrementalChain.canRestoreTo(...)` 在无增量时仅允许恢复到全量快照时点本身，不再放行“全量之后”的目标时间。

**证据（链路分组）**：
- 规则实现：`src/main/java/com/basebackend/backup/infrastructure/executor/IncrementalChain.java:100`、`src/main/java/com/basebackend/backup/infrastructure/executor/IncrementalChain.java:110`
- 单测覆盖：`src/test/java/com/basebackend/backup/infrastructure/executor/IncrementalChainTest.java:16`、`src/test/java/com/basebackend/backup/infrastructure/executor/IncrementalChainTest.java:21`、`src/test/java/com/basebackend/backup/infrastructure/executor/IncrementalChainTest.java:25`、`src/test/java/com/basebackend/backup/infrastructure/executor/IncrementalChainTest.java:30`

### P0-3 恢复数据源类型硬编码 mysql（已修复）

**修复结果**：
- `RestoreService` 已按 `taskId -> backup_task.datasource_type` 与请求覆盖配置解析真实数据源。
- 恢复分发已支持 `mysql/postgresql`。

**证据（链路分组）**：
- 数据源类型解析与标准化：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:159`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:407`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:419`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:424`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:431`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:437`
- 恢复执行器分发：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:166`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:169`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:228`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:231`
- 单测覆盖：`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:155`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:171`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:189`

## 4. 按优先级整改结果

### P1-1 增量链路真实化（MySQL闭环，PG多模式回放闭环）

**已完成**：
- MySQL binlog 当前位点改为 JDBC 执行 `SHOW MASTER STATUS` 获取真实 `File + Position`。
- MySQL 事件中新增 `ROTATE` 跟踪，binlog 文件名不再固定空值。
- PostgreSQL WAL 位点改为 JDBC 查询真实 LSN（主库/只读库分支处理）。
- `MySqlBackupExecutor` 已支持基于 `mysqlbinlog --read-from-remote-server` 的真实增量导出，并写入 `binlogStart/binlogEnd`。
- `MySqlBackupExecutor` 已支持跨 binlog 文件区间导出（按 `SHOW BINARY LOGS` 解析范围并分段拉取）。
- `AutoBackupScheduler` MySQL 自动增量已改为执行器链路，按 `latestFull + latestIncremental` 推导增量起始位点。
- `AutoBackupScheduler` PostgreSQL 自动增量已接入执行器链路，并按 `latestFull/latestIncremental` 的 `walEnd` 计算增量起始位点。
- `PostgresBackupExecutor` 全量备份已记录 `walStart/walEnd`，为后续增量链路提供可追踪起点。
- `PostgresBackupExecutor` 增量备份已支持 `pg_waldump` 落盘（按 `startLSN -> endLSN` 导出），并写入 `walStart/walEnd`。
- 在 `incrementalReplayEnabled=true` 时，`PostgresBackupExecutor` 增量产物改为“可回放 SQL 快照脚本”：先生成 TRUNCATE 前导，再追加 `pg_dump --data-only` 导出结果。
- `PostgresBackupExecutor.restore(...)` 已支持在 `incrementalReplayEnabled=true` 时应用增量产物；关闭时仍保持 fail-fast。
- `RestoreService` 对 PostgreSQL 增量恢复改为“按开关控制”：`incrementalReplayEnabled=false` 保持 fail-fast，`true` 允许按链路应用全量+增量。
- 新增 `backup.postgres.incrementalReplayEnabled` 能力开关（默认 `false`）：关闭时不暴露 `incremental_backup`，开启后按 `incrementalReplayMode` 暴露对应回放能力（含 `wal_physical_builtin`）。
- `AutoBackupScheduler` 在 PostgreSQL 且 `incrementalReplayEnabled=false` 时主动跳过自动增量任务，避免持续产出不可恢复增量。
- `PostgresBackupExecutor.executeIncremental(...)` 在 `incrementalReplayEnabled=false` 时直接拒绝执行，阻断手工/误调用路径。
- `PostgresBackupExecutor.restore(...)` 对 PostgreSQL 增量恢复新增产物格式校验：仅接受带 `snapshot-v1` 标记的快照型增量文件，历史 `wal.log`/非快照文件将 fail-fast，避免误回放。
- `PostgresBackupExecutor.restore(...)` 兼容旧版快照增量文件头（`-- PostgreSQL replayable incremental snapshot`），避免历史快照被误拒绝。
- 新增 `backup.postgres.incrementalReplayMode`（`logical_snapshot`/`wal_external`）与 `backup.postgres.walReplayCommand` 配置项。
- `PostgresBackupExecutor.executeIncremental(...)` 在 `wal_external` 模式下改为落盘 `pg_waldump` WAL区间增量产物（`wal-dump-v1` 标记）。
- `PostgresBackupExecutor.restore(...)` 在 `wal_external` 模式下支持通过命令模板执行 WAL 物理/逻辑回放，支持 `${artifact}/${targetDatabase}/${walStart}/${walEnd}` 等占位符。
- `PostgresBackupExecutor.restore(...)` 对 `wal_external` 增强安全校验：命令模板必须包含 `${artifact}`；恢复前强校验 `walStart/walEnd`（来源可为 artifact 字段或文件头），并验证 LSN 格式与区间顺序。
- 新增 `wal_physical_builtin` 模式：`PostgresBackupExecutor.restore(...)` 可对 `wal-dump` 增量产物执行内建 `pg_ctl stop/start/promote` 物理回放编排。
- 内建物理回放新增配置项：`pgCtlPath/pgBasebackupPath/physicalReplayDataDir/physicalReplayArchiveDir/physicalReplayBaselineDir/physicalReplayRestoreCommandTemplate/physicalReplayBasebackupTimeoutSeconds/physicalReplayBasebackupFastCheckpoint/physicalReplayKeepLatestBaselines/physicalReplayRollbackOnFailure/physicalReplayRollbackStartTimeoutSeconds/physicalReplayRollbackHealthProbeEnabled/physicalReplayRollbackHealthProbeMaxAttempts/physicalReplayRollbackHealthProbeIntervalSeconds/physicalReplayRollbackHealthProbeTimeoutSeconds/physicalReplayRollbackBusinessProbeEnabled/physicalReplayRollbackBusinessProbeSql/physicalReplayRollbackBusinessProbeExpectedValue/physicalReplayStopTimeoutSeconds/physicalReplayStartTimeoutSeconds/physicalReplayRecoveryTargetAction/physicalReplayPort`。
- 内建物理回放新增配置校验：`restore_command` 模板必须包含 `%f/%p`，并限制 `recovery_target_action` 为 `pause/promote/shutdown`。
- `PostgresBackupExecutor` 已内建 `pg_basebackup` 基线创建与元数据写入（`baseline.meta`），回放成功后按保留数量自动清理历史基线。
- `PostgresBackupExecutor` 已支持回放失败后的自动回滚启动策略：停止实例、快照失败目录、回置基线、常规参数重新启动（可通过开关关闭）。
- `PostgresBackupExecutor` 已支持回滚启动后的健康探针联动：`pg_ctl status` 进程探针 + `SELECT 1` SQL 探针（支持重试、间隔、超时配置）。
- `PostgresBackupExecutor` 已支持可选业务一致性探针（SQL首列结果比对），与“进程 + SQL”形成分层健康校验。
- `AutoBackupScheduler` 对增量前置条件不足（无全量基线/无起始位点）由异常错误改为 `warn + skip`，避免定时任务持续报错噪音。
- `AbstractBackupExecutor` 已将增量链关键元数据真实落库：`baseFullId/binlogStart/binlogEnd/walStart/walEnd`。
- 新增 `mysqlbinlogPath` 配置项，支持执行器外部命令路径配置。
- 新增 `pgWalDumpPath` 配置项，支持 PostgreSQL WAL 导出命令路径配置。
- 新增 `docs/POSTGRES_WAL_REPLAY_EXTERNAL_GUIDE.md`，提供 `wal_external` 的命令模板、脚本示例与幂等落地建议。
- 新增 `docs/POSTGRES_WAL_REPLAY_PHYSICAL_BUILTIN_GUIDE.md`，提供内建物理回放编排配置与运维注意事项。

**后续建议**：
- 固化业务一致性探针 SQL 模板（订单/账户/库存）并形成值班手册。
- 补充真实 PostgreSQL 环境集成演练（当前主要覆盖命令编排与脚本桩验证）。

**证据**：
- MySQL 位点与增量导出链路：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBinlogParser.java:70`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBinlogParser.java:132`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBackupExecutor.java:183`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBackupExecutor.java:372`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBackupExecutor.java:428`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBackupExecutor.java:447`
- 调度与恢复分发链路：`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:106`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:149`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:163`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:205`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:230`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:167`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:170`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:229`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:232`
- PostgreSQL 多模式回放与物理编排：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:62`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:176`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:229`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:244`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:315`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:508`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:572`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:650`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:678`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:825`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:855`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:907`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresWalParser.java:54`
- 增量元数据落库与配置开关：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/AbstractBackupExecutor.java:167`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/AbstractBackupExecutor.java:276`、`src/main/java/com/basebackend/backup/config/BackupProperties.java:469`、`src/main/java/com/basebackend/backup/config/BackupProperties.java:477`、`src/main/java/com/basebackend/backup/config/BackupProperties.java:486`、`src/main/java/com/basebackend/backup/config/BackupProperties.java:523`、`src/main/resources/application-backup.yml:81`、`src/main/resources/application-backup.yml:83`、`src/main/resources/application-backup.yml:85`、`src/main/resources/application-backup.yml:102`、`src/main/resources/application-backup.yml:111`、`src/main/resources/application-backup.yml:116`、`src/main/resources/application-backup.yml:120`
- 关键测试覆盖：`src/test/java/com/basebackend/backup/scheduler/AutoBackupSchedulerTest.java:171`、`src/test/java/com/basebackend/backup/scheduler/AutoBackupSchedulerTest.java:213`、`src/test/java/com/basebackend/backup/scheduler/AutoBackupSchedulerTest.java:235`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBackupExecutorTest.java:68`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBackupExecutorTest.java:91`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:257`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:324`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:378`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:401`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:551`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:591`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:783`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:835`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:952`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:996`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:1040`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:1120`
- 文档与落地指南：`docs/POSTGRES_WAL_REPLAY_EXTERNAL_GUIDE.md`、`docs/POSTGRES_WAL_REPLAY_PHYSICAL_BUILTIN_GUIDE.md`

### P1-2 监控指标注册器接入业务调用（已完成）

**修复结果**：
- 备份主链路 `AbstractBackupExecutor.execute(...)` 已接入 `recordBackupStart/Success/Failure`。
- 恢复主链路 `RestoreService` 已接入 `recordRestoreStart/Success/Failure`。

**证据（链路分组）**：
- 备份主链路埋点：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/AbstractBackupExecutor.java:98`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/AbstractBackupExecutor.java:141`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/AbstractBackupExecutor.java:146`
- 恢复主链路埋点：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:71`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:136`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:442`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:454`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:456`
- 回归测试：`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:120`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:121`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:150`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:151`

### P1-3 自定义 Actuator 端点未暴露（已完成）

**修复结果**：
- 已将 `backup-metrics` 加入 Actuator 暴露列表。

**证据（链路分组）**：
- 端点定义与子路由：`src/main/java/com/basebackend/backup/infrastructure/monitoring/MetricsController.java:25`、`src/main/java/com/basebackend/backup/infrastructure/monitoring/MetricsController.java:87`、`src/main/java/com/basebackend/backup/infrastructure/monitoring/MetricsController.java:91`
- Actuator 暴露配置：`src/main/resources/application-backup.yml:231`

### P1-4 告警规则混杂与表达式错误（已完成）

**修复结果**：
- `prometheus-alerts.yml` 仅保留 Prometheus 规则。
- Alertmanager 路由/接收器已拆分到 `alertmanager.yml`。
- 修正多个表达式（失败率分母保护、无备份检测、时长统计、速度统计、积压表达式等）。

**证据（链路分组）**：
- Prometheus 规则修正：`prometheus-alerts.yml:9`、`prometheus-alerts.yml:35`、`prometheus-alerts.yml:61`、`prometheus-alerts.yml:73`、`prometheus-alerts.yml:97`、`prometheus-alerts.yml:164`、`prometheus-alerts.yml:211`
- PostgreSQL 物理基线告警：`prometheus-alerts.yml:109`、`prometheus-alerts.yml:122`
- Alertmanager 独立配置：`alertmanager.yml:3`、`alertmanager.yml:10`

### P1-5 分布式锁固定 leaseTime 风险（已完成）

**修复结果**：
- 默认 `withLock(...)` 改为 watchdog 自动续租模式（不再使用固定 lease）。
- 显式传入 leaseTime 时仍保留自定义租期能力。

**证据（链路分组）**：
- watchdog 续租实现：`src/main/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManager.java:40`、`src/main/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManager.java:151`、`src/main/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManager.java:153`、`src/main/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManager.java:156`
- 单测覆盖：`src/test/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManagerTest.java:92`、`src/test/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManagerTest.java:185`

### P1-6 PostgreSQL 物理基线生命周期与失败回滚观测（已完成）

**修复结果**：
- 新增 `physicalReplayBaselineCleanupOnBasebackupFailure`，`pg_basebackup` 失败时默认自动清理残留基线目录。
- 回滚启动探针已支持“进程 + SQL + 可选业务一致性 SQL”分层探测，并增加配置防呆校验。
- 新增物理基线失败与清理结果指标，并纳入自定义 `backup-metrics` 端点与 Prometheus 文本输出。
- 新增 `PostgresPhysicalBaselineCreationFailed` / `PostgresPhysicalBaselineCleanupFailed` 告警与 runbook 链接。

**证据（链路分组）**：
- 配置与防呆校验：`src/main/java/com/basebackend/backup/config/BackupProperties.java:523`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:591`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:602`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:617`
- 失败清理与回滚探针：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:679`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:681`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:690`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:694`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:825`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:855`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:907`
- 指标与端点输出：`src/main/java/com/basebackend/backup/infrastructure/monitoring/BackupMetricsRegistrar.java:212`、`src/main/java/com/basebackend/backup/infrastructure/monitoring/BackupMetricsRegistrar.java:219`、`src/main/java/com/basebackend/backup/infrastructure/monitoring/MetricsController.java:54`、`src/main/java/com/basebackend/backup/infrastructure/monitoring/MetricsController.java:134`
- 告警规则：`prometheus-alerts.yml:109`、`prometheus-alerts.yml:122`
- 单测覆盖：`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:835`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:952`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:996`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:1040`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:1120`
- 运维文档：`docs/POSTGRES_WAL_REPLAY_PHYSICAL_BUILTIN_GUIDE.md`

### P2-1 `selectTasksToExecute(now)` 参数未使用（已完成）

**修复结果**：
- Mapper 方法签名移除无效入参，避免误导。

**证据（链路分组）**：
- Mapper 接口签名：`src/main/java/com/basebackend/backup/domain/mapper/BackupTaskMapper.java:29`
- Mapper SQL 对应定义：`src/main/resources/mapper/BackupTaskMapper.xml:48`

### P2-2 集成测试禁用与断言不一致（已完成）

**修复结果**：
- 移除集成测试类级别 `@Disabled`，改为 `@Testcontainers(disabledWithoutDocker = true)`，无 Docker 环境自动跳过。
- 集成测试不再依赖不可用的 Spring 上下文自动装配，改为手动组装 `MySQLBackupServiceImpl + LocalStorageProvider`，避免上下文歧义。
- 修复本地存储集成用例中 `UploadRequest` 空参问题，统一补齐 `bucket/key/inputStream/size`，断言与真实行为一致。
- 对全量备份集成测试增加 `mysqldump` 可执行命令探测，命令缺失时按用例级别跳过，避免误报失败。

**证据（链路分组）**：
- 容器环境与禁用策略：`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:40`
- 手动组装依赖：`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:85`
- `mysqldump` 可用性探测：`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:94`
- `UploadRequest` 构造与调用覆盖：`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:129`、`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:146`、`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:191`、`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:230`

### 整改项负责人与验收标准速览（P0/P1/P2）

**P0**

- **P0-1 调度全量备份与可恢复流水线割裂**
  - 负责人：备份模块开发负责人
  - 验收标准：`mysql/postgresql` 全量调度均走执行器链路，`backup_history` 与 `storage_locations` 正常落库。
- **P0-2 PITR 无增量误判成功**
  - 负责人：备份恢复链路负责人
  - 验收标准：无增量场景仅允许恢复到全量快照时点本身，恢复到全量之后时点必须失败。
- **P0-3 恢复数据源类型硬编码**
  - 负责人：备份恢复链路负责人
  - 验收标准：恢复流程按 `taskId/request` 解析真实数据源，`mysql/postgresql` 分发正确且单测通过。

**P1**

- **P1-1 增量链路真实化**
  - 负责人：备份模块开发负责人
  - 验收标准：MySQL 位点/增量导出链路闭环，PostgreSQL `logical_snapshot/wal_external/wal_physical_builtin` 在开关开启下可执行且关键单测通过。
- **P1-2 监控指标注册器接入业务调用**
  - 负责人：可观测性负责人
  - 验收标准：备份/恢复主链路均调用 `record*Start/Success/Failure`，指标快照与 Prometheus 输出一致。
- **P1-3 Actuator 端点未暴露**
  - 负责人：平台工程负责人
  - 验收标准：`/actuator/backup-metrics`、`/actuator/backup-metrics/prometheus`、`/actuator/backup-metrics/health` 可访问。
- **P1-4 告警规则混杂与表达式错误**
  - 负责人：SRE 值班负责人
  - 验收标准：Prometheus 与 Alertmanager 配置拆分完成，关键告警表达式通过规则校验并可触发演练。
- **P1-5 分布式锁固定 leaseTime 风险**
  - 负责人：中间件与可靠性负责人
  - 验收标准：默认加锁路径使用 watchdog 自动续租，显式 leaseTime 仍可生效，相关单测通过。
- **P1-6 PostgreSQL 物理基线生命周期与失败回滚观测**
  - 负责人：备份模块开发负责人 + SRE 值班负责人
  - 验收标准：`pg_basebackup` 失败清理策略、回滚探针、指标与告警（含 runbook）全部生效并有测试覆盖。

**P2**

- **P2-1 `selectTasksToExecute(now)` 参数未使用**
  - 负责人：数据访问层负责人
  - 验收标准：Mapper 接口签名与 XML 定义一致，无无效参数残留。
- **P2-2 集成测试禁用与断言不一致**
  - 负责人：测试工程负责人
  - 验收标准：集成测试在无 Docker 环境自动跳过；`UploadRequest` 断言一致；`mysqldump` 缺失时按用例级跳过。

### 验收证据链接清单（评审快捷版）

**P0 快捷证据**

- **P0-1**：`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:61`、`src/main/java/com/basebackend/backup/scheduler/AutoBackupScheduler.java:145`、`src/test/java/com/basebackend/backup/scheduler/AutoBackupSchedulerTest.java:74`
- **P0-2**：`src/main/java/com/basebackend/backup/infrastructure/executor/IncrementalChain.java:110`、`src/test/java/com/basebackend/backup/infrastructure/executor/IncrementalChainTest.java:16`
- **P0-3**：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:407`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:166`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:155`

**P1 快捷证据**

- **P1-1**：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBinlogParser.java:70`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/MySqlBackupExecutor.java:372`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:244`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:551`
- **P1-2**：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/AbstractBackupExecutor.java:98`、`src/main/java/com/basebackend/backup/infrastructure/executor/impl/RestoreService.java:442`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/RestoreServiceTest.java:120`
- **P1-3**：`src/main/java/com/basebackend/backup/infrastructure/monitoring/MetricsController.java:25`、`src/main/resources/application-backup.yml:231`
- **P1-4**：`prometheus-alerts.yml:9`、`prometheus-alerts.yml:109`、`alertmanager.yml:3`
- **P1-5**：`src/main/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManager.java:153`、`src/test/java/com/basebackend/backup/infrastructure/reliability/impl/RedissonLockManagerTest.java:92`
- **P1-6**：`src/main/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutor.java:679`、`src/main/java/com/basebackend/backup/infrastructure/monitoring/MetricsController.java:134`、`prometheus-alerts.yml:122`、`src/test/java/com/basebackend/backup/infrastructure/executor/impl/PostgresBackupExecutorTest.java:1040`

**P2 快捷证据**

- **P2-1**：`src/main/java/com/basebackend/backup/domain/mapper/BackupTaskMapper.java:29`、`src/main/resources/mapper/BackupTaskMapper.xml:48`
- **P2-2**：`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:40`、`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:94`、`src/test/java/com/basebackend/backup/integration/BackupIntegrationTest.java:230`

## 5. 整改看板（按优先级）

### 已完成

- **业务探针模板落地（文档层）**
  - 负责人：备份模块开发负责人
  - 目标日期：2026-03-06（已完成）
  - 验收标准：`POSTGRES_WAL_REPLAY_PHYSICAL_BUILTIN_GUIDE.md` 已包含订单/账户/库存示例 SQL、阈值建议与启用约束说明。

### 进行中

- **扩展监控面板**
  - 负责人：SRE 值班负责人
  - 目标日期：2026-03-12
  - 验收标准：Grafana 已新增物理基线失败/清理面板，周报阈值规则已在值班手册登记并完成一次告警演练。

### 待办

- **CI 演练能力建设**
  - 负责人：平台工程负责人
  - 目标日期：2026-03-19
  - 验收标准：CI 流水线新增 PostgreSQL 回放演练作业（含 Docker 依赖），连续 5 次主干构建稳定通过并产出演练日志归档。

## 6. 说明

报告基于 2026-03-06 当前工作区代码与本地测试结果，不包含生产流量压测和真实灾备演练数据。
