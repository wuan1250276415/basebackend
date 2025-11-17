#!/bin/bash

# 停止所有微服务脚本

echo "========================================="
echo "停止所有微服务"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 项目根目录
PROJECT_ROOT=$(cd "$(dirname "$0")/../.." && pwd)

# 停止函数
stop_service() {
    local service_name=$1
    local pid_file="$PROJECT_ROOT/logs/$service_name.pid"
    
    echo -e "\n${YELLOW}停止 $service_name${NC}"
    
    if [ -f "$pid_file" ]; then
        PID=$(cat "$pid_file")
        if kill -0 $PID 2>/dev/null; then
            kill $PID
            echo -e "${GREEN}✓ $service_name (PID: $PID) 已停止${NC}"
            rm "$pid_file"
        else
            echo -e "${YELLOW}$service_name 进程不存在${NC}"
            rm "$pid_file"
        fi
    else
        echo -e "${YELLOW}未找到 $service_name PID文件${NC}"
        # 尝试通过端口查找进程
        case $service_name in
            "gateway") port=8080 ;;
            "user-api") port=8081 ;;
            "system-api") port=8082 ;;
            "auth-api") port=8083 ;;
            "admin-api") port=8084 ;;
            *) port=0 ;;
        esac
        
        if [ $port -ne 0 ]; then
            PID=$(lsof -t -i:$port)
            if [ ! -z "$PID" ]; then
                kill $PID
                echo -e "${GREEN}✓ 通过端口 $port 找到并停止 $service_name (PID: $PID)${NC}"
            fi
        fi
    fi
}

# 停止所有服务
stop_service "gateway"
stop_service "user-api"
stop_service "system-api"
stop_service "auth-api"
stop_service "admin-api"

echo -e "\n========================================="
echo -e "${GREEN}所有服务已停止${NC}"
echo "========================================="
