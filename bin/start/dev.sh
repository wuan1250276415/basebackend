#!/bin/bash

# 开发环境一键启动脚本
# 启动所有必要的服务用于本地开发

echo "========================================="
echo "  Base Backend 开发环境启动"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 获取项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

# 1. 检查 Docker 服务
echo -e "${BLUE}1. 检查基础设施${NC}"
echo "-------------------"

if docker ps | grep -q "basebackend-mysql"; then
    echo -e "${GREEN}✓ 基础设施已运行${NC}"
else
    echo -e "${YELLOW}⚠ 基础设施未运行，正在启动...${NC}"
    cd docker/compose
    ./start-all.sh
    cd "$PROJECT_ROOT"
    
    echo "等待服务就绪（90秒）..."
    sleep 90
fi

echo ""

# 2. 健康检查
echo -e "${BLUE}2. 健康检查${NC}"
echo "-------------------"

./bin/maintenance/health-check.sh

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ 健康检查失败，请检查服务状态${NC}"
    exit 1
fi

echo ""

# 3. 编译项目
echo -e "${BLUE}3. 编译项目${NC}"
echo "-------------------"

echo "编译项目（跳过测试）..."
if mvn clean install -DskipTests -q; then
    echo -e "${GREEN}✓ 编译成功${NC}"
else
    echo -e "${RED}✗ 编译失败${NC}"
    exit 1
fi

echo ""

# 4. 启动服务
echo -e "${BLUE}4. 启动应用服务${NC}"
echo "-------------------"

echo "启动 Gateway (端口 8080)..."
cd basebackend-gateway
mvn spring-boot:run > "$PROJECT_ROOT/logs/gateway.log" 2>&1 &
GATEWAY_PID=$!
echo "  Gateway PID: $GATEWAY_PID"

sleep 10

echo "启动 Admin API (端口 8081)..."
cd "$PROJECT_ROOT/basebackend-admin-api"
mvn spring-boot:run > "$PROJECT_ROOT/logs/admin-api.log" 2>&1 &
ADMIN_API_PID=$!
echo "  Admin API PID: $ADMIN_API_PID"

echo ""
echo "等待服务启动（30秒）..."
sleep 30

echo ""

# 5. 验证服务
echo -e "${BLUE}5. 验证服务${NC}"
echo "-------------------"

services=("Gateway:8080" "Admin API:8081")
all_ok=true

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    echo -n "检查 $name ... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null)
    
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ 运行中${NC}"
    else
        echo -e "${RED}✗ 未响应${NC} (HTTP $response)"
        all_ok=false
    fi
done

echo ""

# 6. 完成
echo "========================================="
echo "  启动完成"
echo "========================================="
echo ""

if [ "$all_ok" = true ]; then
    echo -e "${GREEN}✓ 所有服务启动成功！${NC}"
    echo ""
    echo "服务地址:"
    echo "  Gateway:         http://localhost:8080"
    echo "  Admin API:       http://localhost:8081"
    echo "  API 文档:        http://localhost:8080/doc.html"
    echo "  Nacos 控制台:    http://localhost:8848/nacos"
    echo "  RocketMQ 控制台: http://localhost:8180"
    echo ""
    echo "日志文件:"
    echo "  Gateway:   logs/gateway.log"
    echo "  Admin API: logs/admin-api.log"
    echo ""
    echo "停止服务:"
    echo "  kill $GATEWAY_PID $ADMIN_API_PID"
    echo "  或按 Ctrl+C"
    echo ""
    
    # 保存 PID
    echo "$GATEWAY_PID" > logs/gateway.pid
    echo "$ADMIN_API_PID" > logs/admin-api.pid
    
    # 等待用户中断
    trap "echo ''; echo '正在停止服务...'; kill $GATEWAY_PID $ADMIN_API_PID 2>/dev/null; rm -f logs/*.pid; exit" INT TERM
    
    echo "按 Ctrl+C 停止所有服务"
    wait
else
    echo -e "${RED}✗ 部分服务启动失败${NC}"
    echo ""
    echo "请检查日志:"
    echo "  tail -f logs/gateway.log"
    echo "  tail -f logs/admin-api.log"
    exit 1
fi
