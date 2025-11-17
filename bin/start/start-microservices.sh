#!/bin/bash

# 微服务启动脚本
# 用于启动拆分后的三个微服务

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查服务健康状态
check_health() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=0

    log_info "等待 $service_name 启动..."

    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            log_info "$service_name 已启动 (端口: $port)"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    log_error "$service_name 启动超时"
    return 1
}

# 检查依赖服务
check_dependencies() {
    log_info "检查依赖服务..."

    # 检查MySQL
    if ! docker ps | grep -q basebackend-mysql; then
        log_error "MySQL未运行，请先启动: docker-compose -f docker/compose/base/docker-compose.base.yml up -d mysql"
        exit 1
    fi

    # 检查Redis
    if ! docker ps | grep -q basebackend-redis; then
        log_error "Redis未运行，请先启动: docker-compose -f docker/compose/base/docker-compose.base.yml up -d redis"
        exit 1
    fi

    # 检查Nacos
    if ! docker ps | grep -q basebackend-nacos; then
        log_error "Nacos未运行，请先启动: docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d nacos"
        exit 1
    fi

    log_info "所有依赖服务已就绪"
}

# 启动服务
start_service() {
    local service_name=$1
    local service_dir=$2
    local port=$3

    log_info "启动 $service_name..."

    cd $service_dir

    # 后台启动服务
    nohup mvn spring-boot:run > logs/$service_name.log 2>&1 &
    local pid=$!
    echo $pid > logs/$service_name.pid

    cd - > /dev/null

    # 等待服务启动
    if check_health $service_name $port; then
        log_info "$service_name 启动成功 (PID: $pid)"
    else
        log_error "$service_name 启动失败，查看日志: $service_dir/logs/$service_name.log"
        exit 1
    fi
}

# 停止服务
stop_service() {
    local service_name=$1
    local service_dir=$2

    if [ -f "$service_dir/logs/$service_name.pid" ]; then
        local pid=$(cat $service_dir/logs/$service_name.pid)
        if ps -p $pid > /dev/null 2>&1; then
            log_info "停止 $service_name (PID: $pid)..."
            kill $pid
            rm $service_dir/logs/$service_name.pid
        fi
    fi
}

# 主函数
main() {
    local action=${1:-start}

    case $action in
        start)
            log_info "=== 启动微服务 ==="
            
            # 检查依赖
            check_dependencies

            # 创建日志目录
            mkdir -p basebackend-user-api/logs
            mkdir -p basebackend-system-api/logs
            mkdir -p basebackend-auth-api/logs

            # 启动服务
            start_service "user-api" "basebackend-user-api" 8081
            start_service "system-api" "basebackend-system-api" 8082
            start_service "auth-api" "basebackend-auth-api" 8083

            log_info "=== 所有服务启动完成 ==="
            log_info "User API:   http://localhost:8081/doc.html"
            log_info "System API: http://localhost:8082/doc.html"
            log_info "Auth API:   http://localhost:8083/doc.html"
            ;;

        stop)
            log_info "=== 停止微服务 ==="
            stop_service "user-api" "basebackend-user-api"
            stop_service "system-api" "basebackend-system-api"
            stop_service "auth-api" "basebackend-auth-api"
            log_info "=== 所有服务已停止 ==="
            ;;

        restart)
            log_info "=== 重启微服务 ==="
            $0 stop
            sleep 3
            $0 start
            ;;

        status)
            log_info "=== 服务状态 ==="
            
            services=("user-api:8081" "system-api:8082" "auth-api:8083")
            
            for service in "${services[@]}"; do
                IFS=':' read -r name port <<< "$service"
                if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
                    echo -e "${GREEN}✓${NC} $name (端口: $port) - 运行中"
                else
                    echo -e "${RED}✗${NC} $name (端口: $port) - 未运行"
                fi
            done
            ;;

        *)
            echo "用法: $0 {start|stop|restart|status}"
            echo ""
            echo "命令说明:"
            echo "  start   - 启动所有微服务"
            echo "  stop    - 停止所有微服务"
            echo "  restart - 重启所有微服务"
            echo "  status  - 查看服务状态"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
