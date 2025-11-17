#!/bin/bash

# 项目健康检查脚本
# 用于检查项目的各个组件是否正常运行

echo "========================================="
echo "  Base Backend 健康检查"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_service() {
    local name=$1
    local url=$2
    local expected=$3
    
    echo -n "检查 $name ... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    
    if [ "$response" = "$expected" ]; then
        echo -e "${GREEN}✓ 正常${NC} (HTTP $response)"
        return 0
    else
        echo -e "${RED}✗ 异常${NC} (HTTP $response, 期望 $expected)"
        return 1
    fi
}

check_port() {
    local name=$1
    local host=$2
    local port=$3
    
    echo -n "检查 $name 端口 ... "
    
    if nc -z "$host" "$port" 2>/dev/null || (echo > /dev/tcp/$host/$port) 2>/dev/null; then
        echo -e "${GREEN}✓ 可访问${NC} ($host:$port)"
        return 0
    else
        echo -e "${RED}✗ 不可访问${NC} ($host:$port)"
        return 1
    fi
}

# 计数器
total=0
passed=0

# 1. 检查基础设施
echo "1. 基础设施检查"
echo "-------------------"

# MySQL
((total++))
if check_port "MySQL" "localhost" "3306"; then
    ((passed++))
fi

# Redis
((total++))
if check_port "Redis" "localhost" "6379"; then
    ((passed++))
fi

echo ""

# 2. 检查中间件
echo "2. 中间件检查"
echo "-------------------"

# Nacos
((total++))
if check_service "Nacos" "http://localhost:8848/nacos/v1/console/health/readiness" "200"; then
    ((passed++))
fi

# RocketMQ NameServer
((total++))
if check_port "RocketMQ NameServer" "localhost" "9876"; then
    ((passed++))
fi

# RocketMQ Console
((total++))
if check_service "RocketMQ Console" "http://localhost:8180" "200"; then
    ((passed++))
fi

echo ""

# 3. 检查应用服务
echo "3. 应用服务检查"
echo "-------------------"

# Gateway
((total++))
if check_service "Gateway" "http://localhost:8080/actuator/health" "200"; then
    ((passed++))
fi

# Admin API
((total++))
if check_service "Admin API" "http://localhost:8081/actuator/health" "200"; then
    ((passed++))
fi

echo ""

# 4. 检查 Docker 容器
echo "4. Docker 容器检查"
echo "-------------------"

if command -v docker &> /dev/null; then
    containers=("basebackend-mysql" "basebackend-redis" "basebackend-nacos" "basebackend-rocketmq-namesrv")
    
    for container in "${containers[@]}"; do
        ((total++))
        echo -n "检查容器 $container ... "
        
        if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null)
            if [ "$status" = "healthy" ] || [ -z "$status" ]; then
                echo -e "${GREEN}✓ 运行中${NC}"
                ((passed++))
            else
                echo -e "${YELLOW}⚠ 运行中但不健康${NC} (状态: $status)"
            fi
        else
            echo -e "${RED}✗ 未运行${NC}"
        fi
    done
else
    echo -e "${YELLOW}⚠ Docker 未安装，跳过容器检查${NC}"
fi

echo ""

# 5. 总结
echo "========================================="
echo "  检查结果汇总"
echo "========================================="
echo ""
echo "总检查项: $total"
echo -e "通过: ${GREEN}$passed${NC}"
echo -e "失败: ${RED}$((total - passed))${NC}"
echo ""

if [ $passed -eq $total ]; then
    echo -e "${GREEN}✓ 所有检查通过！系统运行正常。${NC}"
    exit 0
elif [ $passed -ge $((total * 3 / 4)) ]; then
    echo -e "${YELLOW}⚠ 大部分检查通过，但有些服务可能未启动。${NC}"
    exit 1
else
    echo -e "${RED}✗ 多个服务异常，请检查日志。${NC}"
    exit 2
fi
