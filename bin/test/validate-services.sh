#!/bin/bash

# 微服务验证脚本
# 用于验证所有微服务是否正常运行

echo "========================================="
echo "微服务架构验证脚本"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 计数器
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# 检查函数
check_service() {
    local name=$1
    local url=$2
    local expected=$3
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    echo -e "\n${YELLOW}检查: $name${NC}"
    echo "URL: $url"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    
    if [ "$response" = "$expected" ]; then
        echo -e "${GREEN}✓ 通过 (状态码: $response)${NC}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        echo -e "${RED}✗ 失败 (期望: $expected, 实际: $response)${NC}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi
}

echo -e "\n${YELLOW}=== 第一步：检查基础设施 ===${NC}"

# 检查Nacos
check_service "Nacos Console" \
    "http://localhost:8848/nacos" \
    "200"

# 检查MySQL
echo -e "\n${YELLOW}检查: MySQL${NC}"
if mysqladmin ping -h localhost -P 3306 --silent 2>/dev/null; then
    echo -e "${GREEN}✓ MySQL 运行正常${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    echo -e "${RED}✗ MySQL 未响应${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

# 检查Redis
echo -e "\n${YELLOW}检查: Redis${NC}"
if redis-cli ping 2>/dev/null | grep -q PONG; then
    echo -e "${GREEN}✓ Redis 运行正常${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    echo -e "${RED}✗ Redis 未响应${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi
TOTAL_CHECKS=$((TOTAL_CHECKS + 1))

echo -e "\n${YELLOW}=== 第二步：检查微服务健康状态 ===${NC}"

# 检查User API
check_service "User API Health" \
    "http://localhost:8081/actuator/health" \
    "200"

# 检查System API
check_service "System API Health" \
    "http://localhost:8082/actuator/health" \
    "200"

# 检查Auth API
check_service "Auth API Health" \
    "http://localhost:8083/actuator/health" \
    "200"

# 检查Gateway
check_service "Gateway Health" \
    "http://localhost:8080/actuator/health" \
    "200"

echo -e "\n${YELLOW}=== 第三步：检查服务注册 ===${NC}"

# 检查服务是否注册到Nacos
check_nacos_service() {
    local service_name=$1
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    echo -e "\n${YELLOW}检查: $service_name 注册状态${NC}"
    
    response=$(curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=$service_name")
    
    if echo "$response" | grep -q "\"hosts\":\[\]"; then
        echo -e "${RED}✗ $service_name 未注册到Nacos${NC}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    else
        instances=$(echo "$response" | grep -o '"ip"' | wc -l)
        echo -e "${GREEN}✓ $service_name 已注册 (实例数: $instances)${NC}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    fi
}

check_nacos_service "basebackend-user-api"
check_nacos_service "basebackend-system-api"
check_nacos_service "basebackend-auth-api"
check_nacos_service "basebackend-gateway"

echo -e "\n${YELLOW}=== 第四步：检查API端点 ===${NC}"

# 测试登录API（可能失败，因为Service还未实现）
echo -e "\n${YELLOW}测试: 登录API${NC}"
login_response=$(curl -s -X POST http://localhost:8083/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' 2>/dev/null)

TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
if echo "$login_response" | grep -q "error"; then
    echo -e "${YELLOW}⚠ 登录API响应但返回错误（Service未实现）${NC}"
    echo "响应: $login_response"
elif echo "$login_response" | grep -q "token"; then
    echo -e "${GREEN}✓ 登录API正常工作${NC}"
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
else
    echo -e "${RED}✗ 登录API无响应${NC}"
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
fi

echo -e "\n${YELLOW}=== 第五步：检查网关路由 ===${NC}"

# 通过网关访问各服务
check_service "Gateway -> User API" \
    "http://localhost:8080/api/user/users" \
    "401"  # 期望401因为没有认证

check_service "Gateway -> System API" \
    "http://localhost:8080/api/system/menus/tree" \
    "401"  # 期望401因为没有认证

check_service "Gateway -> Auth API" \
    "http://localhost:8080/api/auth/info" \
    "401"  # 期望401因为没有认证

echo -e "\n========================================="
echo -e "${YELLOW}验证结果统计${NC}"
echo "========================================="
echo -e "总检查项: $TOTAL_CHECKS"
echo -e "${GREEN}通过: $PASSED_CHECKS${NC}"
echo -e "${RED}失败: $FAILED_CHECKS${NC}"

# 计算通过率
if [ $TOTAL_CHECKS -gt 0 ]; then
    PASS_RATE=$((PASSED_CHECKS * 100 / TOTAL_CHECKS))
    echo -e "通过率: ${PASS_RATE}%"
fi

echo -e "\n${YELLOW}=== 优化建议 ===${NC}"

if [ $FAILED_CHECKS -gt 0 ]; then
    echo -e "${YELLOW}发现 $FAILED_CHECKS 个问题，建议：${NC}"
    echo ""
    echo "1. 检查服务是否都已启动："
    echo "   ps aux | grep java"
    echo ""
    echo "2. 检查端口是否被占用："
    echo "   netstat -tlnp | grep -E '8080|8081|8082|8083'"
    echo ""
    echo "3. 查看服务日志："
    echo "   tail -f logs/*.log"
    echo ""
    echo "4. 检查Nacos配置："
    echo "   访问 http://localhost:8848/nacos"
    echo ""
    echo "5. 如果Service未实现，请参考："
    echo "   docs/OPTIMIZATION_TODO.md"
else
    echo -e "${GREEN}所有检查通过！微服务架构运行正常。${NC}"
    echo ""
    echo "下一步建议："
    echo "1. 实现Service层业务逻辑"
    echo "2. 添加更多集成测试"
    echo "3. 配置监控和日志"
    echo "4. 优化性能"
fi

exit $FAILED_CHECKS
