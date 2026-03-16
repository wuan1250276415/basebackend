#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${ROOT_DIR}"

SEGMENT_TIMEOUT_SEC="${SEGMENT_TIMEOUT_SEC:-60}"
SEGMENT_START="${SEGMENT_START:-}"
SEGMENT_ONLY="${SEGMENT_ONLY:-}"
SEGMENT_LIST_ONLY=0
SEGMENT_DRY_RUN=0
SEGMENT_JSON_OUTPUT=0

SEGMENT_IDS=(
  "common-foundation"
  "common-event-security"
  "database-observability"
  "cache-modules"
  "logging-gateway"
  "scheduler-camunda"
  "business-tail"
)

SEGMENT_NAMES=(
  "Common基础段"
  "CommonEvent+JWT+Security"
  "Database+Observability"
  "Cache三子模块"
  "Logging到Gateway"
  "SchedulerCamunda"
  "Codegen到Mall尾段"
)

SEGMENT_CMDS=(
  "mvn -B -pl basebackend-common/basebackend-common-core,basebackend-common/basebackend-common-util,basebackend-common/basebackend-common-context,basebackend-common/basebackend-common-security,basebackend-common/basebackend-common-starter,basebackend-common/basebackend-common-storage,basebackend-common/basebackend-common-lock,basebackend-common/basebackend-common-idempotent,basebackend-common/basebackend-common-datascope,basebackend-common/basebackend-common-ratelimit,basebackend-common/basebackend-common-export verify"
  "mvn -B -pl basebackend-common/basebackend-common-event,basebackend-jwt,basebackend-security verify"
  "mvn -B -pl basebackend-database/database-core,basebackend-database/database-failover,basebackend-database/database-multitenant,basebackend-database/database-security,basebackend-database/database-migration,basebackend-observability/observability-slo,basebackend-observability/observability-core verify"
  "mvn -B -pl basebackend-cache/basebackend-cache-core,basebackend-cache/basebackend-cache-advanced,basebackend-cache/basebackend-cache-admin verify"
  "mvn -B -pl basebackend-logging/basebackend-logging-core,basebackend-logging/basebackend-logging-audit,basebackend-logging/basebackend-logging-advanced,basebackend-logging/basebackend-logging-monitoring,basebackend-observability/observability-metrics,basebackend-observability/observability-logging,basebackend-observability/observability-alert,basebackend-messaging,basebackend-api-model,basebackend-file-service,basebackend-backup,basebackend-nacos,basebackend-service-client,basebackend-gateway verify"
  "mvn -B -pl basebackend-scheduler-parent/scheduler-camunda verify"
  "mvn -B -pl basebackend-code-generator,basebackend-user-api,basebackend-system-api,basebackend-notification-service,basebackend-ai,basebackend-websocket,basebackend-search,basebackend-workflow,basebackend-observability-service,basebackend-chat-api,basebackend-album-api,basebackend-ticket-api,basebackend-mall-product-api,basebackend-mall-trade-api,basebackend-mall-pay-api verify"
)

usage() {
  cat <<'EOF'
用法:
  verify-segmented-ci.sh [--start <segment_id>] [--only <segment_id>] [--timeout <seconds>] [--list] [--dry-run] [--json]

可用 segment_id:
  common-foundation
  common-event-security
  database-observability
  cache-modules
  logging-gateway
  scheduler-camunda
  business-tail

环境变量:
  SEGMENT_TIMEOUT_SEC  每段超时秒数，默认 60
  SEGMENT_START        等价于 --start
  SEGMENT_ONLY         等价于 --only
EOF
}

print_available_segments() {
  echo "可用分段列表："
  printf '%-24s | %s\n' "segment_id" "name"
  printf '%-24s-+-%s\n' "------------------------" "--------------------------------"
  local i
  for i in "${!SEGMENT_IDS[@]}"; do
    printf '%-24s | %s\n' "${SEGMENT_IDS[$i]}" "${SEGMENT_NAMES[$i]}"
  done
}

json_escape() {
  local value="${1:-}"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "${value}"
}

collect_selected_segment_indexes() {
  SELECTED_SEGMENT_INDEXES=()
  local started=0
  local i
  local segment_id

  if [[ -z "${SEGMENT_START}" ]]; then
    started=1
  fi

  for i in "${!SEGMENT_IDS[@]}"; do
    segment_id="${SEGMENT_IDS[$i]}"

    if [[ -n "${SEGMENT_ONLY}" && "${SEGMENT_ONLY}" != "${segment_id}" ]]; then
      continue
    fi

    if [[ "${started}" -eq 0 ]]; then
      if [[ "${segment_id}" == "${SEGMENT_START}" ]]; then
        started=1
      else
        continue
      fi
    fi

    SELECTED_SEGMENT_INDEXES+=("${i}")
  done

  if [[ "${#SELECTED_SEGMENT_INDEXES[@]}" -eq 0 ]]; then
    echo "未匹配到可执行分段，请检查 --start/--only 参数组合。" >&2
    return 1
  fi
  return 0
}

print_available_segments_json() {
  local i
  local first=1

  echo "{"
  echo "  \"mode\": \"list\","
  echo "  \"count\": ${#SEGMENT_IDS[@]},"
  echo "  \"segments\": ["
  for i in "${!SEGMENT_IDS[@]}"; do
    if [[ "${first}" -eq 0 ]]; then
      echo ","
    fi
    printf '    {"id":"%s","name":"%s"}' \
      "$(json_escape "${SEGMENT_IDS[$i]}")" \
      "$(json_escape "${SEGMENT_NAMES[$i]}")"
    first=0
  done
  echo ""
  echo "  ]"
  echo "}"
}

print_selected_segments_json() {
  local i
  local first=1
  local segment_id
  local segment_name
  local segment_cmd

  if ! collect_selected_segment_indexes; then
    return 1
  fi

  echo "{"
  echo "  \"mode\": \"plan\","
  printf '  "start": "%s",\n' "$(json_escape "${SEGMENT_START}")"
  printf '  "only": "%s",\n' "$(json_escape "${SEGMENT_ONLY}")"
  echo "  \"count\": ${#SELECTED_SEGMENT_INDEXES[@]},"
  echo "  \"segments\": ["
  for i in "${SELECTED_SEGMENT_INDEXES[@]}"; do
    segment_id="${SEGMENT_IDS[$i]}"
    segment_name="${SEGMENT_NAMES[$i]}"
    segment_cmd="${SEGMENT_CMDS[$i]}"
    if [[ "${first}" -eq 0 ]]; then
      echo ","
    fi
    printf '    {"id":"%s","name":"%s","command":"%s"}' \
      "$(json_escape "${segment_id}")" \
      "$(json_escape "${segment_name}")" \
      "$(json_escape "${segment_cmd}")"
    first=0
  done
  echo ""
  echo "  ]"
  echo "}"
}

print_dry_run_plan() {
  local i
  local segment_id
  local segment_name
  local segment_cmd

  if ! collect_selected_segment_indexes; then
    return 1
  fi

  echo "Dry Run 模式：仅打印将执行的分段命令，不实际运行。"
  echo ""
  printf '%-24s | %-32s | %s\n' "segment_id" "name" "command"
  printf '%-24s-+-%-32s-+-%s\n' "------------------------" "--------------------------------" "--------------------------------"

  for i in "${SELECTED_SEGMENT_INDEXES[@]}"; do
    segment_id="${SEGMENT_IDS[$i]}"
    segment_name="${SEGMENT_NAMES[$i]}"
    segment_cmd="${SEGMENT_CMDS[$i]}"

    printf '%-24s | %-32s | %s\n' "${segment_id}" "${segment_name}" "${segment_cmd}"
  done
  return 0
}

contains_segment_id() {
  local target="$1"
  local current
  for current in "${SEGMENT_IDS[@]}"; do
    if [[ "${current}" == "${target}" ]]; then
      return 0
    fi
  done
  return 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --start)
      SEGMENT_START="$2"
      shift 2
      ;;
    --only)
      SEGMENT_ONLY="$2"
      shift 2
      ;;
    --timeout)
      SEGMENT_TIMEOUT_SEC="$2"
      shift 2
      ;;
    -l|--list)
      SEGMENT_LIST_ONLY=1
      shift
      ;;
    -n|--dry-run)
      SEGMENT_DRY_RUN=1
      shift
      ;;
    -j|--json)
      SEGMENT_JSON_OUTPUT=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ "${SEGMENT_LIST_ONLY}" -eq 1 ]]; then
  if [[ "${SEGMENT_JSON_OUTPUT}" -eq 1 ]]; then
    print_available_segments_json
  else
    print_available_segments
  fi
  exit 0
fi

if [[ -n "${SEGMENT_START}" ]] && ! contains_segment_id "${SEGMENT_START}"; then
  echo "无效 --start 值: ${SEGMENT_START}" >&2
  exit 2
fi

if [[ -n "${SEGMENT_ONLY}" ]] && ! contains_segment_id "${SEGMENT_ONLY}"; then
  echo "无效 --only 值: ${SEGMENT_ONLY}" >&2
  exit 2
fi

if [[ -n "${SEGMENT_START}" && -n "${SEGMENT_ONLY}" && "${SEGMENT_START}" != "${SEGMENT_ONLY}" ]]; then
  echo "--start 与 --only 同时指定时必须一致，当前为: start=${SEGMENT_START}, only=${SEGMENT_ONLY}" >&2
  exit 2
fi

if ! [[ "${SEGMENT_TIMEOUT_SEC}" =~ ^[0-9]+$ ]]; then
  echo "超时时间必须是整数秒: ${SEGMENT_TIMEOUT_SEC}" >&2
  exit 2
fi

if [[ "${SEGMENT_JSON_OUTPUT}" -eq 1 ]]; then
  if print_selected_segments_json; then
    exit 0
  fi
  exit 2
fi

if [[ "${SEGMENT_DRY_RUN}" -eq 1 ]]; then
  if print_dry_run_plan; then
    exit 0
  fi
  exit 2
fi

TIMEOUT_BIN=""
if command -v timeout >/dev/null 2>&1; then
  TIMEOUT_BIN="timeout"
elif command -v gtimeout >/dev/null 2>&1; then
  TIMEOUT_BIN="gtimeout"
fi

if [[ -z "${TIMEOUT_BIN}" ]]; then
  echo "未检测到 timeout/gtimeout，将不启用单段超时控制。"
fi

ensure_module_has_testcases() {
  local module="$1"
  local report_dir="${module}/target/surefire-reports"
  local report_count
  local testcase_count

  if [[ ! -d "${report_dir}" ]]; then
    echo "[${module}] 缺少 surefire 报告目录: ${report_dir}" >&2
    return 1
  fi

  report_count="$(find "${report_dir}" -maxdepth 1 -name 'TEST-*.xml' | wc -l | tr -d ' ')"
  if [[ "${report_count}" -eq 0 ]]; then
    echo "[${module}] 未生成 TEST-*.xml 报告，疑似测试空跑。" >&2
    return 1
  fi

  testcase_count="$(grep -h '<testcase ' "${report_dir}"/TEST-*.xml | wc -l | tr -d ' ')"
  if [[ "${testcase_count}" -eq 0 ]]; then
    echo "[${module}] 测试报告中没有任何 testcase，疑似测试空跑。" >&2
    return 1
  fi

  echo "[${module}] 测试门禁通过：report=${report_count}, testcase=${testcase_count}"
  return 0
}

SEGMENT_STATUS=()
SEGMENT_CODE=()
for _ in "${SEGMENT_IDS[@]}"; do
  SEGMENT_STATUS+=("SKIPPED")
  SEGMENT_CODE+=("0")
done

started=0
if [[ -z "${SEGMENT_START}" ]]; then
  started=1
fi

overall_exit=0

for i in "${!SEGMENT_IDS[@]}"; do
  segment_id="${SEGMENT_IDS[$i]}"
  segment_name="${SEGMENT_NAMES[$i]}"
  segment_cmd="${SEGMENT_CMDS[$i]}"

  if [[ -n "${SEGMENT_ONLY}" && "${SEGMENT_ONLY}" != "${segment_id}" ]]; then
    continue
  fi

  if [[ "${started}" -eq 0 ]]; then
    if [[ "${segment_id}" == "${SEGMENT_START}" ]]; then
      started=1
    else
      continue
    fi
  fi

  echo ""
  echo "========== [${segment_id}] ${segment_name} =========="
  echo "${segment_cmd}"
  echo ""

  set +e
  if [[ -n "${TIMEOUT_BIN}" && "${SEGMENT_TIMEOUT_SEC}" -gt 0 ]]; then
    "${TIMEOUT_BIN}" "${SEGMENT_TIMEOUT_SEC}s" bash -lc "${segment_cmd}"
  else
    bash -lc "${segment_cmd}"
  fi
  code=$?
  set -e

  SEGMENT_CODE[$i]="${code}"
  if [[ "${code}" -eq 0 ]]; then
    if [[ "${segment_id}" == "logging-gateway" ]]; then
      if ! ensure_module_has_testcases "basebackend-nacos"; then
        SEGMENT_STATUS[$i]="FAIL"
        SEGMENT_CODE[$i]="201"
        overall_exit=201
        break
      fi
    fi
    SEGMENT_STATUS[$i]="PASS"
    continue
  fi

  if [[ "${code}" -eq 124 ]]; then
    SEGMENT_STATUS[$i]="TIMEOUT"
    overall_exit=124
  else
    SEGMENT_STATUS[$i]="FAIL"
    overall_exit="${code}"
  fi
  break
done

echo ""
echo "================ Segmented Verify Summary ================"
printf '%-24s | %-32s | %-8s | %s\n' "segment_id" "name" "status" "exit_code"
printf '%-24s-+-%-32s-+-%-8s-+-%s\n' "------------------------" "--------------------------------" "--------" "---------"

for i in "${!SEGMENT_IDS[@]}"; do
  printf '%-24s | %-32s | %-8s | %s\n' \
    "${SEGMENT_IDS[$i]}" \
    "${SEGMENT_NAMES[$i]}" \
    "${SEGMENT_STATUS[$i]}" \
    "${SEGMENT_CODE[$i]}"
done

if [[ "${overall_exit}" -ne 0 ]]; then
  echo ""
  echo "分段门禁未通过，退出码: ${overall_exit}"
  exit "${overall_exit}"
fi

echo ""
echo "分段门禁全部通过。"
