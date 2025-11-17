#!/bin/bash

# 微服务集成测试脚本

echo "========================================="
echo "微服务集成测试"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 基础URL
BASE_URL="http://localhost:8080"

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local data=$4
    local description=$5
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "\n${YELLOW}测试 $TOTAL_TESTS: $description${NC}"
    echo "端点: $method $endpoint"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$endpoint")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    if [ "$response" = "$expected_status" ]; then
        echo -e "${GREEN}✓ 通过 (状态码: $response)${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}✗ 失败 (期望: $expected_status, 实际: $response)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# 健康检查函数
health_check() {
    local service=$1
    local port=$2
    
    echo -e "\n${YELLOW}检查服务: $service (端口: $port)${NC}"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null)
    
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ $service 运行正常${NC}"
        return 0
    else
        echo -e "${RED}✗ $service 未响应${NC}"
        return 1
    fi
}

echo -e "\n${YELLOW}=== 第一步：检查服务健康状态 ===${NC}"

# 检查各个服务是否运行
health_check "Gateway" 8080
health_check "User API" 8081
health_check "System API" 8082
health_check "Auth API" 8083
health_check "Admin API" 8084

echo -e "\n${YELLOW}=== 第二步：测试认证服务 ===${NC}"

# 测试登录
test_endpoint "POST" "/api/auth/login" "200" \
    '{"username":"admin","password":"admin123"}' \
    "用户登录"

# 测试Token验证
test_endpoint "GET" "/api/auth/verify?token=test-token" "200" "" \
    "验证Token"

# 测试获取用户信息
test_endpoint "GET" "/api/auth/info" "401" "" \
    "获取用户信息（未认证）"

echo -e "\n${YELLOW}=== 第三步：测试用户服务 ===${NC}"

# 测试用户列表
test_endpoint "GET" "/api/user/users" "401" "" \
    "获取用户列表（需要认证）"

# 测试角色列表
test_endpoint "GET" "/api/user/roles" "401" "" \
    "获取角色列表（需要认证）"

echo -e "\n${YELLOW}=== 第四步：测试系统服务 ===${NC}"

# 测试部门树
test_endpoint "GET" "/api/system/depts/tree" "401" "" \
    "获取部门树（需要认证）"

# 测试菜单树
test_endpoint "GET" "/api/system/menus/tree" "401" "" \
    "获取菜单树（需要认证）"

# 测试字典列表
test_endpoint "GET" "/api/system/dicts" "401" "" \
    "获取字典列表（需要认证）"

echo -e "\n${YELLOW}=== 第五步：测试服务间调用 ===${NC}"

# 这里可以添加更复杂的集成测试场景

echo -e "\n========================================="
echo -e "${YELLOW}测试结果统计${NC}"
echo "========================================="
echo -e "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "\n${GREEN}所有测试通过！${NC}"
    exit 0
else
    echo -e "\n${RED}有 $FAILED_TESTS 个测试失败！${NC}"
    exit 1
fi
