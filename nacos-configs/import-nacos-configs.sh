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
#
# 示例：
#    NACOS_SERVER=192.168.66.126:8848 bash import-nacos-configs.sh
#
# ================================================================================================

# 配置参数
NACOS_SERVER=${NACOS_SERVER:-localhost:8848}
NAMESPACE=${NAMESPACE:-public}  # public 或具体的命名空间 ID
USERNAME=${USERNAME:-nacos}
PASSWORD=${PASSWORD:-nacos}
GROUP="DEFAULT_GROUP"

# 配置文件目录
CONFIG_DIR="./nacos-configs/dev"

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

# 导入配置文件的函数
import_config() {
    local data_id=$1
    local config_type=${2:-yaml}
    local config_group=${3:-$GROUP}
    local config_file="${CONFIG_DIR}/${data_id}"

    if [ ! -f "$config_file" ]; then
        echo -e "${RED}✗ 跳过（文件不存在）: ${data_id}${NC}"
        return 1
    fi

    echo -n "导入: ${data_id} ... "

    # URL 编码内容
    local content=$(cat "$config_file" | python3 -c "import sys; import urllib.parse; print(urllib.parse.quote(sys.stdin.read()))" 2>/dev/null || cat "$config_file" | perl -MURI::Escape -e 'print uri_escape(<STDIN>);' 2>/dev/null)

    if [ -z "$content" ]; then
        echo -e "${RED}失败（无法编码内容）${NC}"
        return 1
    fi

    # 发送请求到 Nacos
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "http://${NACOS_SERVER}/nacos/v1/cs/configs" \
        -d "dataId=${data_id}" \
        -d "group=${config_group}" \
        -d "content=${content}" \
        -d "type=${config_type}" \
        -d "tenant=${NAMESPACE}" \
        -d "username=${USERNAME}" \
        -d "password=${PASSWORD}")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "200" ] && [ "$body" = "true" ]; then
        echo -e "${GREEN}✓ 成功${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败 (HTTP ${http_code})${NC}"
        echo "  响应: $body"
        return 1
    fi
}

# 导入 Sentinel 规则的函数
import_sentinel_rule() {
    local data_id=$1
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local config_file="${script_dir}/${data_id}"

    if [ ! -f "$config_file" ]; then
        echo -e "${RED}✗ 跳过（文件不存在）: ${data_id}${NC}"
        return 1
    fi

    echo -n "导入 Sentinel 规则: ${data_id} ... "

    # URL 编码内容
    local content=$(cat "$config_file" | python3 -c "import sys; import urllib.parse; print(urllib.parse.quote(sys.stdin.read()))" 2>/dev/null || cat "$config_file" | perl -MURI::Escape -e 'print uri_escape(<STDIN>);' 2>/dev/null)

    if [ -z "$content" ]; then
        echo -e "${RED}失败（无法编码内容）${NC}"
        return 1
    fi

    # 发送请求到 Nacos
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "http://${NACOS_SERVER}/nacos/v1/cs/configs" \
        -d "dataId=${data_id}" \
        -d "group=SENTINEL_GROUP" \
        -d "content=${content}" \
        -d "type=json" \
        -d "tenant=${NAMESPACE}" \
        -d "username=${USERNAME}" \
        -d "password=${PASSWORD}")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "200" ] && [ "$body" = "true" ]; then
        echo -e "${GREEN}✓ 成功${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败 (HTTP ${http_code})${NC}"
        echo "  响应: $body"
        return 1
    fi
}

# 导入所有配置文件
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
echo "------------------------------------------------------------------------------------------------"
echo " 导入 Sentinel 规则"
echo "------------------------------------------------------------------------------------------------"
echo ""

sentinel_rules=(
    "basebackend-gateway-flow-rules.json"
    "basebackend-gateway-degrade-rules.json"
    "basebackend-gateway-gw-flow-rules.json"
    "admin-api-flow-rules.json"
    "admin-api-degrade-rules.json"
    "admin-api-param-flow-rules.json"
    "admin-api-system-rules.json"
    "admin-api-authority-rules.json"
)

for rule in "${sentinel_rules[@]}"; do
    total=$((total + 1))
    if import_sentinel_rule "$rule"; then
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
