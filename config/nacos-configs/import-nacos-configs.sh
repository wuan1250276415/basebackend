#!/bin/bash
# ================================================================================================
# Nacos 配置导入脚本
# ================================================================================================
#
# 使用说明：
# 1. 确保 Nacos 服务已启动（默认地址：http://localhost:8848/nacos）
# 2. 执行此脚本导入开发环境配置：bash import-nacos-configs.sh
# 3. 可选参数：
#    - NACOS_SERVER: Nacos 服务器地址（默认：localhost:8848）
#    - NAMESPACE: 命名空间 ID（默认：public，开发环境）
#    - USERNAME: Nacos 用户名（默认：nacos）
#    - PASSWORD: Nacos 密码（默认：nacos）
#    - GROUP: 配置分组（默认：DEFAULT_GROUP）
#    - CONFIG_DIR: 配置目录（默认：脚本所在目录下的 dev）
#
# 示例：
#    NACOS_SERVER=localhost:8848 bash import-nacos-configs.sh
#
# ================================================================================================

# 配置参数
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
NACOS_SERVER=${NACOS_SERVER:-localhost:8848}
NAMESPACE=${NAMESPACE:-public}  # public 或具体的命名空间 ID
USERNAME=${USERNAME:-nacos}
PASSWORD=${PASSWORD:-nacos}
GROUP=${GROUP:-DEFAULT_GROUP}
CONFIG_DIR=${CONFIG_DIR:-"${SCRIPT_DIR}/dev"}
NACOS_WAIT_TIMEOUT=${NACOS_WAIT_TIMEOUT:-90}
NACOS_WAIT_INTERVAL=${NACOS_WAIT_INTERVAL:-3}

ACCESS_TOKEN=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "================================================================================================"
echo " Nacos 配置导入脚本"
echo "================================================================================================"
echo ""
echo "Nacos 服务器: http://${NACOS_SERVER}"
echo "命名空间: ${NAMESPACE}"
echo "分组: ${GROUP}"
echo "配置目录: ${CONFIG_DIR}"
echo ""

# 检查配置目录是否存在
if [ ! -d "$CONFIG_DIR" ]; then
    echo -e "${RED}错误: 配置目录不存在: ${CONFIG_DIR}${NC}"
    exit 1
fi

split_response() {
    local response=$1
    RESPONSE_HTTP_CODE=$(printf '%s\n' "$response" | tail -n1)
    RESPONSE_BODY=$(printf '%s\n' "$response" | sed '$d')
}

wait_for_nacos() {
    local start_time
    start_time=$(date +%s)

    while true; do
        if curl -fsS "http://${NACOS_SERVER}/nacos/" >/dev/null 2>&1; then
            return 0
        fi

        if [ $(( $(date +%s) - start_time )) -ge "$NACOS_WAIT_TIMEOUT" ]; then
            echo -e "${RED}错误: 等待 Nacos 就绪超时（${NACOS_WAIT_TIMEOUT}s）${NC}"
            return 1
        fi

        sleep "$NACOS_WAIT_INTERVAL"
    done
}

is_success_response() {
    printf '%s' "$1" | grep -q '"code":0'
}

extract_access_token() {
    printf '%s' "$1" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

extract_message() {
    local message
    message=$(printf '%s' "$1" | sed -n 's/.*"message":"\([^"]*\)".*/\1/p')
    if [ -n "$message" ]; then
        printf '%s' "$message"
    else
        printf '%s' "$1"
    fi
}

ensure_access_token() {
    local response
    local login_url="http://${NACOS_SERVER}/nacos/v3/auth/user/login"
    local admin_init_url="http://${NACOS_SERVER}/nacos/v3/auth/user/admin"

    response=$(curl -sS -w "\n%{http_code}" -X POST \
        "$login_url" \
        -d "username=${USERNAME}" \
        -d "password=${PASSWORD}")
    split_response "$response"

    if [ "$RESPONSE_HTTP_CODE" = "200" ]; then
        ACCESS_TOKEN=$(extract_access_token "$RESPONSE_BODY")
        if [ -n "$ACCESS_TOKEN" ]; then
            return 0
        fi
    fi

    echo "首次登录未获取到 accessToken，尝试初始化管理员密码..."

    response=$(curl -sS -w "\n%{http_code}" -X POST \
        "$admin_init_url" \
        -d "password=${PASSWORD}")
    split_response "$response"

    if [ "$RESPONSE_HTTP_CODE" != "200" ] || ! is_success_response "$RESPONSE_BODY"; then
        echo -e "${YELLOW}管理员初始化未成功，继续尝试登录...${NC}"
    fi

    response=$(curl -sS -w "\n%{http_code}" -X POST \
        "$login_url" \
        -d "username=${USERNAME}" \
        -d "password=${PASSWORD}")
    split_response "$response"

    if [ "$RESPONSE_HTTP_CODE" = "200" ]; then
        ACCESS_TOKEN=$(extract_access_token "$RESPONSE_BODY")
    fi

    if [ -n "$ACCESS_TOKEN" ]; then
        return 0
    fi

    echo -e "${RED}错误: 无法获取 Nacos accessToken${NC}"
    echo "  响应: $(extract_message "$RESPONSE_BODY")"
    return 1
}

# 导入配置文件的函数
import_config() {
    local data_id=$1
    local config_file="${CONFIG_DIR}/${data_id}"

    if [ ! -f "$config_file" ]; then
        echo -e "${RED}✗ 跳过（文件不存在）: ${data_id}${NC}"
        return 1
    fi

    echo -n "导入: ${data_id} ... "

    local response
    response=$(curl -sS -w "\n%{http_code}" -X POST \
        "http://${NACOS_SERVER}/nacos/v3/admin/cs/config" \
        -H "accessToken: ${ACCESS_TOKEN}" \
        -d "dataId=${data_id}" \
        -d "groupName=${GROUP}" \
        -d "namespaceId=${NAMESPACE}" \
        -d "type=yaml" \
        --data-urlencode "content@${config_file}")

    split_response "$response"

    if [ "$RESPONSE_HTTP_CODE" = "200" ] && is_success_response "$RESPONSE_BODY"; then
        echo -e "${GREEN}✓ 成功${NC}"
        return 0
    fi

    echo -e "${RED}✗ 失败 (HTTP ${RESPONSE_HTTP_CODE})${NC}"
    echo "  响应: $(extract_message "$RESPONSE_BODY")"
    return 1
}

# 导入所有配置文件
echo "等待 Nacos 就绪..."
if ! wait_for_nacos; then
    exit 1
fi

echo -e "${GREEN}✓ Nacos 已就绪${NC}"
echo ""

echo "检查 Nacos 认证状态..."
if ! ensure_access_token; then
    exit 1
fi

echo -e "${GREEN}✓ 已获取 accessToken${NC}"
echo ""
echo "开始导入配置..."
echo ""

total=0
success=0
failed=0

configs=(
    "common-config.yml"
    "mysql-config.yml"
    "redis-config.yml"
    "rocketmq-config.yml"
    "observability-config.yml"
    "security-config.yml"
    "seata-config.yml"
)

for config in "${configs[@]}"; do
    total=$((total + 1))
    if import_config "$config"; then
        success=$((success + 1))
    else
        failed=$((failed + 1))
    fi
done

echo ""
echo "================================================================================================"
echo " 导入完成"
echo "================================================================================================"
echo -e "总计: ${total} | ${GREEN}成功: ${success}${NC} | ${RED}失败: ${failed}${NC}"
echo ""

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}✓ 所有配置导入成功！${NC}"
    echo ""
    echo "下一步："
    echo "1. 访问 Nacos 控制台验证配置: http://${NACOS_SERVER}/nacos"
    echo "2. 重启应用程序以加载 Nacos 配置"
    exit 0
else
    echo -e "${YELLOW}⚠ 部分配置导入失败，请检查错误信息${NC}"
    exit 1
fi
