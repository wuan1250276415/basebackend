# PostgreSQL 内建物理回放编排（wal_physical_builtin）

## 1. 功能定位

`wal_physical_builtin` 模式用于在恢复阶段由 `basebackend-backup` 直接编排
`pg_ctl stop/start/promote`，并通过 `recovery_target_lsn` 执行实例级 WAL 回放。

当前能力已覆盖：

- `pg_basebackup` 物理基线创建（恢复前自动生成）
- `pg_basebackup` 失败残留自动清理（可开关）
- 回放成功后的基线保留裁剪
- 回放失败后的自动回滚启动（可开关，支持业务一致性探针）

适用于具备独立 PostgreSQL 实例控制权限的环境。

## 2. 配置示例

```yaml
backup:
  postgres:
    incremental-replay-enabled: true
    incremental-replay-mode: wal_physical_builtin

    host: 127.0.0.1
    port: 5432
    username: postgres
    password: "******"
    database: basebackend

    pg-ctl-path: pg_ctl
    pg-basebackup-path: pg_basebackup

    physical-replay-data-dir: /var/lib/postgresql/data
    physical-replay-archive-dir: /var/lib/postgresql/wal_archive
    physical-replay-baseline-dir: /var/lib/postgresql/physical_baselines
    physical-replay-restore-command-template: cp ${archiveDir}/%f "%p"

    physical-replay-basebackup-timeout-seconds: 300
    physical-replay-basebackup-fast-checkpoint: true
    physical-replay-baseline-cleanup-on-basebackup-failure: true

    physical-replay-stop-timeout-seconds: 60
    physical-replay-start-timeout-seconds: 120
    physical-replay-rollback-start-timeout-seconds: 120

    physical-replay-keep-latest-baselines: 3
    physical-replay-rollback-on-failure: true
    physical-replay-rollback-health-probe-enabled: true
    physical-replay-rollback-health-probe-max-attempts: 1
    physical-replay-rollback-health-probe-interval-seconds: 1
    physical-replay-rollback-health-probe-timeout-seconds: 1
    physical-replay-rollback-business-probe-enabled: false
    physical-replay-rollback-business-probe-sql: ""
    physical-replay-rollback-business-probe-expected-value: ""

    physical-replay-recovery-target-action: promote
    physical-replay-port: -1
```

## 3. 编排步骤

恢复 WAL 增量产物时，执行器会按顺序执行：

1. 校验增量产物格式与 WAL 区间（`wal-start`/`wal-end`）。
2. 校验内建回放配置（`pg_ctl/pg_basebackup`、目录、模板、超时、target action）。
3. 调用 `pg_basebackup` 在 `physical-replay-baseline-dir` 创建当次基线，并写入 `baseline.meta`。
   - 若创建失败且 `physical-replay-baseline-cleanup-on-basebackup-failure=true`，
     自动清理失败基线残留；清理失败则抛异常并保留现场。
   - 若创建失败且 `physical-replay-baseline-cleanup-on-basebackup-failure=false`，
     直接保留失败残留并抛异常。
   - 当 `backup.metrics.enabled=true` 时，上述分支会同步上报失败/清理结果指标。
4. 在 `physical-replay-data-dir` 下写入 `recovery.signal`。
5. 调用 `pg_ctl stop -m fast`（非零会告警并继续）。
6. 调用 `pg_ctl start -w -t <timeout> -o "-c restore_command=... -c recovery_target_lsn=..."`。
7. 调用 `pg_ctl promote -w -t <timeout>`。
8. 成功后按 `physical-replay-keep-latest-baselines` 清理旧基线。
9. 清理 `recovery.signal`。

若 `pg_ctl start` 或 `pg_ctl promote` 失败，且 `physical-replay-rollback-on-failure=true`：

1. 停止实例（`pg_ctl stop`）。
2. 将当前数据目录快照重命名为 `rollback-failed-<timestamp>`。
3. 从当次 `pg_basebackup` 基线回置数据目录。
4. 使用常规参数执行 `pg_ctl start -w -t <rollbackTimeout>` 尝试恢复服务可用性。
5. 若启用健康探针，则执行：
   - `pg_ctl -D <dataDir> status`（进程级探针）
   - `SELECT 1`（SQL 可用性探针）
   - 可选业务一致性探针 SQL（首列结果与期望值比对）

## 4. 模板规则

`physical-replay-restore-command-template` 规则：

- 必须包含 `%f` 与 `%p`。
- 支持占位符：`${archiveDir}` `${artifact}` `${walStart}` `${walEnd}`。

默认模板：

```text
cp ${archiveDir}/%f "%p"
```

## 5. 业务一致性探针推荐模板

建议优先选择“只读、轻量、快速返回”的 SQL，目标耗时建议 ≤1 秒。

前置约束：

- 开启业务探针前必须先开启 `physical-replay-rollback-health-probe-enabled`。
- `physical-replay-rollback-business-probe-sql` 不能为空，否则配置校验直接失败。
- 探针连接使用 `backup.postgres.host/port/database/username/password`；
  当 `database` 为空时回退到 `postgres`。

```yaml
backup:
  postgres:
    physical-replay-rollback-business-probe-enabled: true
    physical-replay-rollback-business-probe-sql: "SELECT COUNT(1) FROM orders WHERE status IN ('PAID','SHIPPED')"
    physical-replay-rollback-business-probe-expected-value: "128"
```

可选 SQL 示例（按业务域）：

- 订单域：`SELECT COUNT(1) FROM orders WHERE updated_at >= now() - interval '5 minute'`
- 账户域：`SELECT COUNT(1) FROM accounts WHERE balance < 0`
- 库存域：`SELECT COUNT(1) FROM stock WHERE available < 0`

阈值建议：

- 强一致业务：配置 `expected-value`，要求首列精确匹配；
- 弱一致业务：不配置 `expected-value`，仅要求首列非空；
- 任一探针连续失败即判定回滚后健康检查失败。

## 6. 风险与限制

- 该模式是实例级恢复，`targetDatabase` 参数会被忽略。
- 需要进程级控制权限，建议仅用于专用灾备实例。
- 已内建回滚后健康探针（进程 + SQL + 可选业务一致性探针）。
- 业务一致性探针默认关闭，建议在演练验证后再逐步放量到生产灾备流程。
- 默认会在 `pg_basebackup` 失败时自动清理残留；如需保留现场排障，
  可将 `physical-replay-baseline-cleanup-on-basebackup-failure` 设为 `false`。
- `physical-replay-recovery-target-action` 支持 `pause/promote/shutdown` 校验，
  但当前编排流程仍会在启动后执行 `pg_ctl promote`。
