# PostgreSQL WAL 外部回放（wal_external）运维模板

## 1. 适用范围

`basebackend-backup` 在 `wal_external` 模式下会导出 `pg_waldump` 区间产物，
恢复时通过 `backup.postgres.wal-replay-command` 调用外部脚本执行回放。

该模式用于对接已有灾备编排体系，支持物理/逻辑级回放流程。

## 2. 配置项

```yaml
backup:
  postgres:
    incremental-replay-enabled: true
    incremental-replay-mode: wal_external
    wal-replay-command: >-
      /opt/basebackend/scripts/pg-wal-replay.sh
      --artifact ${artifact}
      --target-db ${targetDatabase}
      --wal-start ${walStart}
      --wal-end ${walEnd}
      --host ${host}
      --port ${port}
      --username ${username}
```

说明：
- 命令模板必须包含 `${artifact}`。
- 推荐同时使用 `${walStart}`、`${walEnd}` 做区间校验。

## 3. 可用占位符

- `${artifact}`：WAL 导出产物路径（必填）
- `${targetDatabase}`：目标数据库名
- `${walStart}`：起始 LSN
- `${walEnd}`：结束 LSN
- `${host}`：数据库主机
- `${port}`：数据库端口
- `${username}`：数据库用户名
- `${password}`：数据库密码

## 4. 回放脚本示例（逻辑级）

```bash
#!/usr/bin/env bash
set -euo pipefail

ARTIFACT=""
TARGET_DB=""
WAL_START=""
WAL_END=""
HOST="localhost"
PORT="5432"
USER_NAME="postgres"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --artifact) ARTIFACT="$2"; shift 2 ;;
    --target-db) TARGET_DB="$2"; shift 2 ;;
    --wal-start) WAL_START="$2"; shift 2 ;;
    --wal-end) WAL_END="$2"; shift 2 ;;
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --username) USER_NAME="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

[[ -n "$ARTIFACT" ]] || { echo "artifact is required" >&2; exit 2; }
[[ -s "$ARTIFACT" ]] || { echo "artifact not found or empty" >&2; exit 2; }
[[ -n "$WAL_START" && -n "$WAL_END" ]] || { echo "wal range is required" >&2; exit 2; }

# 幂等保护：按 WAL 区间落盘标记，避免重复回放
STAMP_DIR="/var/lib/basebackend/replay-stamps"
mkdir -p "$STAMP_DIR"
STAMP_FILE="$STAMP_DIR/${WAL_START}_${WAL_END}.done"
if [[ -f "$STAMP_FILE" ]]; then
  echo "already replayed: ${WAL_START} -> ${WAL_END}" >&2
  exit 0
fi

# 这里替换为企业实际回放实现（示例仅占位）
# 例如：解析 ARTIFACT 后调用自研回放器，或触发内部灾备平台 API
cat "$ARTIFACT" >/dev/null

touch "$STAMP_FILE"
```

## 5. 生产建议

- 回放脚本需显式校验 `artifact`、`walStart`、`walEnd`。
- 建议实现幂等（基于 LSN 区间打点）。
- 建议把执行日志输出到独立文件，并关联恢复任务 ID。
- 回放失败应返回非 0，交由恢复服务统一判失败。
