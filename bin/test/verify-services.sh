#!/bin/bash

# 微服务验证脚本
# 用于验证三个核心微服务是否正常运行

set -e

echo "=========================================="
echo "微服务验证脚本"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 服务配置
USER_API_URL="http://localhost:8081"
SYSTEM_API_URL="http://localhost:8082"
AUTH_API_URL="http://localhost:8083"
NACOS_URL="http://localhost:8848"

# 检查服务健康状态
check_health() {
    local service_name=$1
    local url=$2
    
    echo -n "检查 $service_name 健康状态... "
    
    if curl -s -f "$url/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 正常${NC}"
        return 0
    else
        echo -e "${RED}✗ 异常${NC}"
        return 1
    fi
}

# 检查Nacos注册
check_nacos_registration() {
    local service_name=$1
    
    echo -n "检查 $service_name 在Nacos的注册状态... "
    
    response=$(curl -s "$NACOS_URL/nacos/v1/ns/instance/list?serviceName=$service_name")
    
    if echo "$response" | grep -q "\"count\":[1-9]"; then
        echo -e "${GREEN}✓ 已注册${NC}"
        return 0
    else
        echo -e "${RED}✗ 未注册${NC}"
        return 1
    fi
}

# 测试登录接口
test_login() {
    echo -n "测试登录接口... "
    
    response=$(curl -s -X POST "$AUTH_API_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}')
    
    if echo "$response" | grep -q "accessToken"; then
        echo -e "${GREEN}✓ 成功${NC}"
        # 提取token
        TOKEN=$(echo "$response" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        echo "  Token: ${TOKEN:0:50}..."
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        echo "  响应: $response"
        return 1
    fi
}

# 主流程
main() {
    echo "1. 检查基础设施"
    echo "----------------------------------------"
    
    # 检查Nacos
    echo -n "检查Nacos... "
    if curl -s -f "$NACOS_URL/nacos/v1/console/health/readiness" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 运行中${NC}"
    else
        echo -e "${RED}✗ 未运行${NC}"
        echo -e "${YELLOW}请先启动Nacos: docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d nacos${NC}"
        exit 1
    fi
    
    echo ""
    echo "2. 检查微服务健康状态"
    echo "----------------------------------------"
    
    health_check_failed=0
    
    check_health "User API" "$USER_API_URL" || health_check_failed=1
    check_health "System API" "$SYSTEM_API_URL" || health_check_failed=1
    check_health "Auth API" "$AUTH_API_URL" || health_check_failed=1
    
    if [ $health_check_failed -eq 1 ]; then
        echo ""
        echo -e "${YELLOW}部分服务健康检查失败，请检查服务是否启动${NC}"
        echo -e "${YELLOW}启动命令: ./bin/start/start-microservices.sh${NC}"
        exit 1
    fi
    
    echo ""
    echo "3. 检查Nacos服务注册"
    echo "----------------------------------------"
    
    registration_failed=0
    
    check_nacos_registration "basebackend-user-api" || registration_failed=1
    check_nacos_registration "basebackend-system-api" || registration_failed=1
    check_nacos_registration "basebackend-auth-api" || registration_failed=1
    
    if [ $registration_failed -eq 1 ]; then
        echo ""
        echo -e "${YELLOW}部分服务未注册到Nacos，请检查配置${NC}"
    fi
    
    echo ""
    echo "4. 测试API接口"
    echo "----------------------------------------"
    
    test_login
    
    echo ""
    echo "=========================================="
    echo "验证完成"
    echo "=========================================="
    echo ""
    echo "服务访问地址:"
    echo "  - User API:   $USER_API_URL"
    echo "  - System API: $SYSTEM_API_URL"
    echo "  - Auth API:   $AUTH_API_URL"
    echo ""
    echo "API文档:"
    echo "  - User API:   $USER_API_URL/doc.html"
    echo "  - System API: $SYSTEM_API_URL/doc.html"
    echo "  - Auth API:   $AUTH_API_URL/doc.html"
    echo ""
    echo "Nacos控制台:"
    echo "  - URL: $NACOS_URL/nacos"
    echo "  - 账号: nacos / nacos"
    echo ""
}

# 执行主流程
main
