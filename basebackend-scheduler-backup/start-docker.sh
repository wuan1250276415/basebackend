#!/bin/bash

# =====================================================
# BaseBackend Scheduler - Docker 启动脚本
# 版本: 1.0
# 描述: 快速启动和停止 Docker 容器服务
# 依赖: Docker, Docker Compose
# =====================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 检查端口是否被占用
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null 2>&1; then
        log_warn "端口 $1 已被占用"
        return 1
    fi
    return 0
}

# 检查 Docker 环境
check_docker_env() {
    log_step "检查 Docker 环境..."
    check_command docker
    check_command docker-compose

    # 检查 Docker 是否运行
    if ! docker info &> /dev/null; then
        log_error "Docker 服务未运行，请启动 Docker 服务"
        exit 1
    fi

    log_info "Docker 环境检查通过"
}

# 创建 .env 文件
create_env_file() {
    if [ ! -f .env ]; then
        log_step "创建 .env 文件..."
        cp .env.example .env
        log_info "已创建 .env 文件，请根据需要修改配置"
        read -p "是否现在修改 .env 文件? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            ${EDITOR:-nano} .env
        fi
    fi
}

# 创建必要的目录
create_directories() {
    log_step "创建必要的目录..."
    mkdir -p docker/mysql/data
    mkdir -p docker/mysql/conf
    mkdir -p docker/scheduler/logs
    log_info "目录创建完成"
}

# 启动所有服务
start_all() {
    log_step "启动所有服务..."
    docker-compose up -d

    if [ $? -eq 0 ]; then
        log_info "所有服务启动成功"
        show_urls
    else
        log_error "服务启动失败"
        exit 1
    fi
}

# 启动指定服务
start_service() {
    local service=$1
    log_step "启动服务: $service"
    docker-compose up -d $service
}

# 停止所有服务
stop_all() {
    log_step "停止所有服务..."
    docker-compose down
    log_info "所有服务已停止"
}

# 重启服务
restart_service() {
    local service=$1
    if [ -z "$service" ]; then
        log_step "重启所有服务..."
        docker-compose restart
    else
        log_step "重启服务: $service"
        docker-compose restart $service
    fi
    log_info "服务重启完成"
}

# 查看服务状态
show_status() {
    log_step "服务状态:"
    docker-compose ps
}

# 查看服务日志
show_logs() {
    local service=$1
    if [ -z "$service" ]; then
        docker-compose logs --tail=100 -f
    else
        docker-compose logs --tail=100 -f $service
    fi
}

# 查看服务 URL
show_urls() {
    echo
    echo -e "${GREEN}=================================${NC}"
    echo -e "${GREEN}  服务访问地址${NC}"
    echo -e "${GREEN}=================================${NC}"
    echo -e "${BLUE}Scheduler 应用:${NC} http://localhost:8080"
    echo -e "${BLUE}Camunda Web UI:${NC} http://localhost:8090/camunda"
    echo -e "${BLUE}Nacos 控制台:${NC} http://localhost:8848/nacos"
    echo -e "${BLUE}MySQL:${NC} localhost:3306"
    echo -e "${BLUE}Redis:${NC} localhost:6379"
    echo
    echo -e "${YELLOW}默认账号:${NC}"
    echo -e "${YELLOW}Camunda: admin / admin${NC}"
    echo -e "${GREEN}=================================${NC}"
}

# 清理资源
cleanup() {
    log_step "清理 Docker 资源..."
    docker-compose down -v --remove-orphans
    docker system prune -f
    log_info "清理完成"
}

# 进入容器
enter_container() {
    local service=$1
    if [ -z "$service" ]; then
        log_error "请指定服务名称 (scheduler/mysql/redis/nacos)"
        exit 1
    fi
    docker exec -it basebackend-$service /bin/bash
}

# 备份数据库
backup_database() {
    local backup_file="backup_$(date +%Y%m%d_%H%M%S).sql"
    log_step "备份数据库到: $backup_file"
    docker exec basebackend-mysql mysqldump -u root -p${DB_PASSWORD:-BaseBackend@123} basebackend_scheduler > $backup_file
    log_info "数据库备份完成: $backup_file"
}

# 恢复数据库
restore_database() {
    local backup_file=$1
    if [ -z "$backup_file" ] || [ ! -f "$backup_file" ]; then
        log_error "请指定有效的备份文件路径"
        exit 1
    fi
    log_step "从 $backup_file 恢复数据库"
    docker exec -i basebackend-mysql mysql -u root -p${DB_PASSWORD:-BaseBackend@123} basebackend_scheduler < $backup_file
    log_info "数据库恢复完成"
}

# 显示帮助信息
show_help() {
    echo -e "${GREEN}BaseBackend Scheduler - Docker 启动脚本${NC}"
    echo
    echo "用法: $0 [命令]"
    echo
    echo "命令:"
    echo "  start             启动所有服务"
    echo "  start <service>   启动指定服务 (scheduler/mysql/redis/nacos)"
    echo "  stop              停止所有服务"
    echo "  restart           重启所有服务"
    echo "  restart <service> 重启指定服务"
    echo "  status            查看服务状态"
    echo "  logs              查看所有服务日志"
    echo "  logs <service>    查看指定服务日志"
    echo "  urls              显示服务访问地址"
    echo "  enter <service>   进入容器 (scheduler/mysql/redis/nacos)"
    echo "  backup            备份数据库"
    echo "  restore <file>    恢复数据库"
    echo "  cleanup           清理 Docker 资源"
    echo "  help              显示帮助信息"
    echo
    echo "示例:"
    echo "  $0 start                    # 启动所有服务"
    echo "  $0 start scheduler          # 只启动 scheduler 服务"
    echo "  $0 logs                     # 查看所有日志"
    echo "  $0 logs scheduler           # 查看 scheduler 日志"
    echo "  $0 enter mysql              # 进入 MySQL 容器"
}

# 主函数
main() {
    # 切换到脚本所在目录
    cd "$(dirname "$0")"

    # 检查 Docker 环境
    check_docker_env

    # 创建必要的文件和目录
    create_env_file
    create_directories

    # 处理命令
    case "${1:-start}" in
        start)
            if [ -z "$2" ]; then
                start_all
            else
                start_service $2
            fi
            ;;
        stop)
            stop_all
            ;;
        restart)
            if [ -z "$2" ]; then
                restart_service
            else
                restart_service $2
            fi
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs $2
            ;;
        urls)
            show_urls
            ;;
        enter)
            enter_container $2
            ;;
        backup)
            backup_database
            ;;
        restore)
            restore_database $2
            ;;
        cleanup)
            cleanup
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知命令: $1"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
