#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
SCENARIO="all"
FAIL_REASON="MOCK_PAY_FAILED"
WAIT_SECONDS=1
CURL_TIMEOUT=20

usage() {
  cat <<'EOF'
商城事件回放脚本（成功 / 失败 / 超时）

用法：
  ./docs/mall/scripts/replay-mall-e2e.sh [选项]

选项：
  --base-url <url>       网关地址，默认 http://127.0.0.1:8080
  --scenario <name>      回放场景：success | fail | timeout | all（默认 all）
  --reason <text>        失败场景 reason 参数，默认 MOCK_PAY_FAILED
  --wait-seconds <num>   每步请求间隔秒数，默认 1
  -h, --help             查看帮助

示例：
  ./docs/mall/scripts/replay-mall-e2e.sh --scenario success
  ./docs/mall/scripts/replay-mall-e2e.sh --scenario fail --reason MANUAL_FAIL
  ./docs/mall/scripts/replay-mall-e2e.sh --scenario all --base-url http://127.0.0.1:8080
EOF
}

log_info() {
  printf '[INFO] %s\n' "$1"
}

log_warn() {
  printf '[WARN] %s\n' "$1"
}

log_error() {
  printf '[ERROR] %s\n' "$1" >&2
}

url_encode() {
  local raw="$1"
  python3 - "$raw" <<'PY'
import urllib.parse
import sys
print(urllib.parse.quote(sys.argv[1], safe=""))
PY
}

json_get() {
  local json="$1"
  local path="$2"
  local value

  # 优先使用 jq；若环境无 jq，则退化到 python3 解析，降低环境依赖。
  if command -v jq >/dev/null 2>&1; then
    value="$(printf '%s' "${json}" | jq -r "${path} // empty")"
    printf '%s' "${value}"
    return 0
  fi

  value="$(printf '%s' "${json}" | python3 - "${path}" <<'PY'
import json
import sys

path = sys.argv[1].strip(".")
obj = json.loads(sys.stdin.read())
if path:
    for key in path.split("."):
        if isinstance(obj, dict):
            obj = obj.get(key)
        else:
            obj = None
            break
        if obj is None:
            break

if obj is None:
    print("")
elif isinstance(obj, (dict, list)):
    print(json.dumps(obj, ensure_ascii=False))
else:
    print(obj)
PY
)"
  printf '%s' "${value}"
}

http_post() {
  local url="$1"
  local body="${2:-}"
  local response

  if [[ -n "${body}" ]]; then
    if ! response="$(curl -sS --connect-timeout 5 --max-time "${CURL_TIMEOUT}" \
      -X POST "${url}" \
      -H 'Content-Type: application/json' \
      -d "${body}")"; then
      log_error "请求失败：POST ${url}"
      exit 1
    fi
  else
    if ! response="$(curl -sS --connect-timeout 5 --max-time "${CURL_TIMEOUT}" \
      -X POST "${url}")"; then
      log_error "请求失败：POST ${url}"
      exit 1
    fi
  fi

  printf '%s' "${response}"
}

assert_success_result() {
  local action="$1"
  local response="$2"
  local code

  code="$(json_get "${response}" ".code")"
  if [[ "${code}" != "200" ]]; then
    log_error "${action}失败，返回码=${code:-unknown}"
    printf '%s\n' "${response}"
    exit 1
  fi
}

sleep_if_needed() {
  if [[ "${WAIT_SECONDS}" =~ ^[0-9]+$ ]] && (( WAIT_SECONDS > 0 )); then
    sleep "${WAIT_SECONDS}"
  fi
}

submit_order() {
  local user_id="$1"
  local pay_amount="$2"
  local items_json="$3"
  local submit_payload submit_response order_no

  submit_payload="$(cat <<JSON
{
  "userId": ${user_id},
  "payAmount": ${pay_amount},
  "items": ${items_json}
}
JSON
)"

  submit_response="$(http_post "${BASE_URL}/api/mall/trades/orders/submit" "${submit_payload}")"
  assert_success_result "提交订单" "${submit_response}"

  order_no="$(json_get "${submit_response}" ".data.orderNo")"
  if [[ -z "${order_no}" ]]; then
    log_error "提交订单成功但未解析到 orderNo"
    printf '%s\n' "${submit_response}"
    exit 1
  fi

  printf '%s' "${order_no}"
}

replay_success_flow() {
  local order_no pay_response
  log_info "开始回放：成功链路"

  order_no="$(submit_order 10001 198.00 '[{"skuId":10001,"quantity":1},{"skuId":10002,"quantity":1}]')"
  log_info "成功链路下单完成，orderNo=${order_no}"

  sleep_if_needed
  pay_response="$(http_post "${BASE_URL}/api/mall/payments/mock-success/${order_no}")"
  assert_success_result "模拟支付成功" "${pay_response}"
  log_info "成功链路回放完成，orderNo=${order_no}"
}

replay_fail_flow() {
  local order_no fail_response encoded_reason
  log_info "开始回放：失败链路"

  order_no="$(submit_order 10002 99.00 '[{"skuId":10001,"quantity":1}]')"
  log_info "失败链路下单完成，orderNo=${order_no}"

  encoded_reason="$(url_encode "${FAIL_REASON}")"
  sleep_if_needed
  fail_response="$(http_post "${BASE_URL}/api/mall/payments/mock-fail/${order_no}?reason=${encoded_reason}")"
  assert_success_result "模拟支付失败" "${fail_response}"
  log_info "失败链路回放完成，orderNo=${order_no}, reason=${FAIL_REASON}"
}

replay_timeout_flow() {
  local order_no timeout_response
  log_info "开始回放：超时链路"

  order_no="$(submit_order 10003 129.00 '[{"skuId":10002,"quantity":1}]')"
  log_info "超时链路下单完成，orderNo=${order_no}"

  sleep_if_needed
  timeout_response="$(http_post "${BASE_URL}/api/mall/trades/orders/${order_no}/timeout-close")"
  assert_success_result "手工触发超时关单" "${timeout_response}"
  log_info "超时链路回放完成，orderNo=${order_no}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    --scenario)
      SCENARIO="$2"
      shift 2
      ;;
    --reason)
      FAIL_REASON="$2"
      shift 2
      ;;
    --wait-seconds)
      WAIT_SECONDS="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      log_error "未知参数：$1"
      usage
      exit 1
      ;;
  esac
done

if [[ ! "${WAIT_SECONDS}" =~ ^[0-9]+$ ]]; then
  log_error "--wait-seconds 必须是非负整数"
  exit 1
fi

case "${SCENARIO}" in
  success|fail|timeout|all)
    ;;
  *)
    log_error "--scenario 仅支持 success | fail | timeout | all"
    exit 1
    ;;
esac

BASE_URL="${BASE_URL%/}"
log_info "网关地址：${BASE_URL}"
log_info "回放场景：${SCENARIO}"

case "${SCENARIO}" in
  success)
    replay_success_flow
    ;;
  fail)
    replay_fail_flow
    ;;
  timeout)
    replay_timeout_flow
    ;;
  all)
    replay_success_flow
    replay_fail_flow
    replay_timeout_flow
    ;;
esac

log_warn "回放结束。建议执行 docs/mall/mall-e2e-debug-runbook.md 中 SQL 进行落库核对。"
