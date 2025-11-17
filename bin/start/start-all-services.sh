#!/bin/bash

# 启动所有微服务脚本

echo "========================================="
echo "启动所有微服务"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 项目根目录
PROJECT_ROOT=$(cd "$(dirname "$0")/../.." && pwd)

# Java选项
JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=dev"

# 启动函数
start_service() {
    local service_name=$1
    local service_path=$2
    local port=$3
    
    echo -e "\n${YELLOW}启动 $service_name (端口: $port)${NC}"
    
    # 检查JAR文件是否存在
    if [ ! -f "$service_path" ]; then
        echo -e "${RED}✗ JAR文件不存在: $service_path${NC}"
        echo "请先执行: mvn clean package"
        return 1
    fi
    
    # 检查端口是否被占用
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${YELLOW}端口 $port 已被占用，跳过启动 $service_name${NC}"
        return 0
    fi
    
    # 启动服务
    nohup java $JAVA_OPTS -jar "$service_path" > "$PROJECT_ROOT/logs/$service_name.log" 2>&1 &
    
    echo -e "${GREEN}✓ $service_name 启动命令已执行 (PID: $!)${NC}"
    echo $! > "$PROJECT_ROOT/logs/$service_name.pid"
    
    # 等待服务启动
    echo "等待服务启动..."
    sleep 10
    
    # 检查服务是否启动成功
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" | grep -q "200"; then
        echo -e "${GREEN}✓ $service_name 启动成功${NC}"
    else
        echo -e "${YELLOW}! $service_name 可能还在启动中，请稍后检查${NC}"
    fi
}

# 创建日志目录
mkdir -p "$PROJECT_ROOT/logs"

# 启动Nacos（如果未运行）
echo -e "${YELLOW}检查 Nacos...${NC}"
if ! curl -s -o /dev/null -w "%{http_code}" "http://localhost:8848/nacos" | grep -q "200"; then
    echo -e "${RED}Nacos 未运行，请先启动 Nacos${NC}"
    echo "可以使用 Docker 启动: docker-compose -f docker/compose/base/docker-compose.base.yml up -d nacos"
    exit 1
else
    echo -e "${GREEN}✓ Nacos 运行正常${NC}"
fi

# 编译项目
echo -e "\n${YELLOW}编译项目...${NC}"
cd "$PROJECT_ROOT"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}编译失败，请检查代码${NC}"
    exit 1
fi

# 启动服务
echo -e "\n${YELLOW}=== 启动微服务 ===${NC}"

# 1. 启动网关
start_service "gateway" \
    "$PROJECT_ROOT/basebackend-gateway/target/basebackend-gateway-1.0.0-SNAPSHOT.jar" \
    8080

# 2. 启动用户服务
start_service "user-api" \
    "$PROJECT_ROOT/basebackend-user-api/target/basebackend-user-api-1.0.0-SNAPSHOT.jar" \
    8081

# 3. 启动系统服务
start_service "system-api" \
    "$PROJECT_ROOT/basebackend-system-api/target/basebackend-system-api-1.0.0-SNAPSHOT.jar" \
    8082

# 4. 启动认证服务
start_service "auth-api" \
    "$PROJECT_ROOT/basebackend-auth-api/target/basebackend-auth-api-1.0.0-SNAPSHOT.jar" \
    8083

# 5. 启动管理服务（兼容旧API）
start_service "admin-api" \
    "$PROJECT_ROOT/basebackend-admin-api/target/basebackend-admin-api-1.0.0-SNAPSHOT.jar" \
    8084

echo -e "\n========================================="
echo -e "${GREEN}所有服务启动完成${NC}"
echo "========================================="
echo ""
echo "服务地址:"
echo "- Gateway: http://localhost:8080"
echo "- User API: http://localhost:8081"
echo "- System API: http://localhost:8082"
echo "- Auth API: http://localhost:8083"
echo "- Admin API: http://localhost:8084"
echo "- Nacos: http://localhost:8848/nacos"
echo ""
echo "API文档:"
echo "- User API: http://localhost:8081/swagger-ui.html"
echo "- System API: http://localhost:8082/swagger-ui.html"
echo "- Auth API: http://localhost:8083/swagger-ui.html"
echo ""
echo "日志文件位置: $PROJECT_ROOT/logs/"
echo ""
echo "停止服务请运行: $PROJECT_ROOT/bin/stop/stop-all-services.sh"
