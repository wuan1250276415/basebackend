#!/bin/bash

# BaseBackend Demo API 测试脚本

BASE_URL="http://localhost:8081"
TOKEN=""

echo "=========================================="
echo "BaseBackend Demo API 测试"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local name=$1
    local method=$2
    local url=$3
    local data=$4
    local header=$5

    echo -e "${BLUE}测试: $name${NC}"
    echo "URL: $method $url"

    if [ -n "$data" ]; then
        if [ -n "$header" ]; then
            response=$(curl -s -X $method "$BASE_URL$url" -H "Content-Type: application/json" -H "$header" -d "$data")
        else
            response=$(curl -s -X $method "$BASE_URL$url" -H "Content-Type: application/json" -d "$data")
        fi
    else
        if [ -n "$header" ]; then
            response=$(curl -s -X $method "$BASE_URL$url" -H "$header")
        else
            response=$(curl -s -X $method "$BASE_URL$url")
        fi
    fi

    echo "响应: $response"
    echo ""
}

echo "=========================================="
echo "1. 健康检查测试"
echo "=========================================="
test_api "健康检查" "GET" "/api/health"
test_api "Hello World" "GET" "/api/hello?name=测试用户"
test_api "系统信息" "GET" "/api/system/info"

echo "=========================================="
echo "2. JWT认证测试"
echo "=========================================="
test_api "用户登录" "POST" "/api/auth/login?username=admin&password=123456"

# 提取Token（需要安装jq）
if command -v jq &> /dev/null; then
    TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login?username=admin&password=123456" | jq -r '.data.token')
    echo -e "${GREEN}Token已获取: $TOKEN${NC}"
    echo ""

    test_api "验证Token" "GET" "/api/auth/validate" "" "Authorization: Bearer $TOKEN"
    test_api "刷新Token" "POST" "/api/auth/refresh" "" "Authorization: Bearer $TOKEN"
else
    echo -e "${RED}注意: 未安装jq，跳过Token测试${NC}"
    echo ""
fi

echo "=========================================="
echo "3. 缓存功能测试"
echo "=========================================="
test_api "设置缓存" "POST" "/api/cache/set?key=test_key&value=hello_world&ttl=60"
test_api "获取缓存" "GET" "/api/cache/get?key=test_key"
test_api "检查缓存" "GET" "/api/cache/exists?key=test_key"

test_api "设置Hash缓存" "POST" "/api/cache/hash/set?key=user_info" '{"name":"张三","age":25,"city":"北京"}'
test_api "获取Hash缓存" "GET" "/api/cache/hash/get?key=user_info"

echo "=========================================="
echo "4. 消息队列测试"
echo "=========================================="
test_api "发送同步消息" "POST" "/api/message/sync?topic=demo-topic" '{"type":"order","orderId":"001","amount":100.00}'
test_api "发送异步消息" "POST" "/api/message/async?topic=demo-topic" '{"type":"notification","userId":"12345"}'
test_api "发送延迟消息" "POST" "/api/message/delay?topic=demo-topic&delayLevel=3" '{"type":"reminder","content":"延迟消息"}'
test_api "批量发送消息" "POST" "/api/message/batch?topic=demo-topic"

echo "=========================================="
echo "测试完成"
echo "=========================================="
