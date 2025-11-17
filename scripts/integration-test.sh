#!/bin/bash

# ===================================================================
# 微服务集成测试脚本
# 测试所有微服务的启动状态和 API 调用
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED_TESTS++))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED_TESTS++))
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 测试函数
test_service_health() {
    local service_name=$1
    local port=$2
    local url="http://localhost:$port/actuator/health"

    ((TOTAL_TESTS++))

    log_info "测试 $service_name 健康检查: $url"

    if curl -s -f "$url" > /dev/null 2>&1; then
        local response=$(curl -s "$url")
        log_success "$service_name 健康检查通过"
        echo "  响应: $response"
        return 0
    else
        log_fail "$service_name 健康检查失败"
        return 1
    fi
}

test_api_endpoint() {
    local service_name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_code=$5

    ((TOTAL_TESTS++))

    log_info "测试 $service_name API: $method $endpoint"

    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" -H "Content-Type: application/json")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" -H "Content-Type: application/json" -d "$data")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" == "$expected_code" ]; then
        log_success "$service_name API 调用成功 (HTTP $http_code)"
        return 0
    else
        log_fail "$service_name API 调用失败 (期望 HTTP $expected_code, 实际 HTTP $http_code)"
        echo "  响应: $body"
        return 1
    fi
}

test_gateway_route() {
    local route_name=$1
    local endpoint=$2

    ((TOTAL_TESTS++))

    log_info "测试 Gateway 路由: $route_name -> $endpoint"

    if curl -s -f "$endpoint" > /dev/null 2>&1; then
        log_success "Gateway 路由 $route_name 可达"
        return 0
    else
        log_fail "Gateway 路由 $route_name 不可达"
        return 1
    fi
}

# ===================================================================
# 开始测试
# ===================================================================

echo "==================================================================="
echo "              微服务集成测试开始"
echo "==================================================================="
echo ""

# 1. 测试服务健康检查
echo ""
echo "==================================================================="
echo "  1. 服务健康检查"
echo "==================================================================="

test_service_health "Gateway" 8180
test_service_health "Nacos" 8848
test_service_health "User Service" 8081
test_service_health "Auth Service" 8082
test_service_health "Menu Service" 8088
test_service_health "Profile Service" 8090
test_service_health "Dept Service" 8083
test_service_health "Dict Service" 8084
test_service_health "Log Service" 8085
test_service_health "Monitor Service" 8086
test_service_health "Application Service" 8087
test_service_health "Notification Service" 8089

# 2. 测试 Gateway 路由
echo ""
echo "==================================================================="
echo "  2. Gateway 路由测试"
echo "==================================================================="

test_gateway_route "用户服务" "http://localhost:8180/api/users/test"
test_gateway_route "认证服务" "http://localhost:8180/api/auth/info"
test_gateway_route "菜单服务" "http://localhost:8180/api/menus/tree"
test_gateway_route "档案服务" "http://localhost:8180/api/profile/preference"

# 3. 测试 API 接口
echo ""
echo "==================================================================="
echo "  3. API 接口测试"
echo "==================================================================="

# 测试用户服务
test_api_endpoint "用户服务" "GET" "http://localhost:8180/api/users/by-username?username=admin" "" 200

# 测试认证服务
test_api_endpoint "认证服务" "GET" "http://localhost:8180/api/auth/info" "" 401

# 测试菜单服务
test_api_endpoint "菜单服务" "GET" "http://localhost:8180/api/menus/tree" "" 200

# 测试档案服务
test_api_endpoint "档案服务" "GET" "http://localhost:8180/api/profile/preference" "" 401

# 4. 测试数据库连接
echo ""
echo "==================================================================="
echo "  4. 数据库连接测试"
echo "==================================================================="

# 测试 MySQL 连接
((TOTAL_TESTS++))
log_info "测试 MySQL 数据库连接"

if mysql -u root -p123456 -e "SELECT 1;" > /dev/null 2>&1; then
    log_success "MySQL 连接成功"
else
    log_fail "MySQL 连接失败"
fi

# 测试 Redis 连接
((TOTAL_TESTS++))
log_info "测试 Redis 连接"

if redis-cli -h 1.117.67.222 -a redis_ycecQi ping > /dev/null 2>&1; then
    log_success "Redis 连接成功"
else
    log_fail "Redis 连接失败"
fi

# 5. 测试 Nacos 配置
echo ""
echo "==================================================================="
echo "  5. Nacos 配置测试"
echo "==================================================================="

((TOTAL_TESTS++))
log_info "测试 Nacos 控制台访问"

if curl -s "http://localhost:8848/nacos/v1/console/health/readiness" > /dev/null 2>&1; then
    log_success "Nacos 控制台可访问"
else
    log_fail "Nacos 控制台不可访问"
fi

# 6. 生成测试报告
echo ""
echo "==================================================================="
echo "              测试报告"
echo "==================================================================="
echo ""
echo "总测试数: $TOTAL_TESTS"
echo "通过: $PASSED_TESTS"
echo "失败: $FAILED_TESTS"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}✗ 有 $FAILED_TESTS 个测试失败${NC}"
    echo ""
    exit 1
fi
