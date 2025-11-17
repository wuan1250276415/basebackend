#!/bin/bash

# 集成测试脚本
# 用于验证所有服务的集成是否正常

echo "========================================="
echo "  Base Backend 集成测试"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 计数器
total=0
passed=0
failed=0

# 测试函数
test_api() {
    local name=$1
    local method=$2
    local url=$3
    local expected_code=$4
    local data=$5
    
    ((total++))
    echo -n "测试 $name ... "
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -d "$data" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" 2>/dev/null)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "$expected_code" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $http_code)"
        ((passed++))
        return 0
    else
        echo -e "${RED}✗ 失败${NC} (HTTP $http_code, 期望 $expected_code)"
        echo "  响应: $body"
        ((failed++))
        return 1
    fi
}

# 1. 健康检查测试
echo -e "${BLUE}1. 健康检查测试${NC}"
echo "-------------------"

test_api "Gateway 健康检查" "GET" "http://localhost:8080/actuator/health" "200"
test_api "Admin API 健康检查" "GET" "http://localhost:8081/actuator/health" "200"

echo ""

# 2. 认证测试
echo -e "${BLUE}2. 认证功能测试${NC}"
echo "-------------------"

# 测试登录
login_response=$(curl -s -X POST "http://localhost:8080/api/admin/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' 2>/dev/null)

if echo "$login_response" | grep -q "token"; then
    echo -e "测试 管理员登录 ... ${GREEN}✓ 通过${NC}"
    ((total++))
    ((passed++))
    
    # 提取 token
    token=$(echo "$login_response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "  Token: ${token:0:20}..."
else
    echo -e "测试 管理员登录 ... ${RED}✗ 失败${NC}"
    echo "  响应: $login_response"
    ((total++))
    ((failed++))
    token=""
fi

echo ""

# 3. API 访问测试（需要认证）
if [ -n "$token" ]; then
    echo -e "${BLUE}3. API 访问测试${NC}"
    echo "-------------------"
    
    # 测试用户列表
    ((total++))
    echo -n "测试 用户列表 API ... "
    user_response=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $token" \
        "http://localhost:8080/api/users" 2>/dev/null)
    
    http_code=$(echo "$user_response" | tail -n1)
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $http_code)"
        ((passed++))
    else
        echo -e "${RED}✗ 失败${NC} (HTTP $http_code)"
        ((failed++))
    fi
    
    # 测试字典列表
    ((total++))
    echo -n "测试 字典列表 API ... "
    dict_response=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $token" \
        "http://localhost:8080/api/dict" 2>/dev/null)
    
    http_code=$(echo "$dict_response" | tail -n1)
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $http_code)"
        ((passed++))
    else
        echo -e "${RED}✗ 失败${NC} (HTTP $http_code)"
        ((failed++))
    fi
    
    echo ""
fi

# 4. Nacos 集成测试
echo -e "${BLUE}4. Nacos 集成测试${NC}"
echo "-------------------"

test_api "Nacos 服务列表" "GET" "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10" "200"

echo ""

# 5. 总结
echo "========================================="
echo "  测试结果汇总"
echo "========================================="
echo ""
echo "总测试数: $total"
echo -e "通过: ${GREEN}$passed${NC}"
echo -e "失败: ${RED}$failed${NC}"
echo ""

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    exit 0
else
    echo -e "${RED}✗ 有 $failed 个测试失败。${NC}"
    exit 1
fi
