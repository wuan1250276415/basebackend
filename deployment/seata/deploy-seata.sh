#!/bin/bash

# ===================================================================
# Seata 分布式事务环境部署脚本
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
SEATA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SEATA_DIR/docker-compose-seata.yml"
ENV_FILE="$SEATA_DIR/.env"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
Seata 分布式事务环境部署脚本

用法:
  $0 [命令]

命令:
  start       启动 Seata 环境
  stop        停止 Seata 环境
  restart     重启 Seata 环境
  status      查看服务状态
  logs        查看日志
  clean       清理环境
  health      健康检查

环境变量:
  SEATA_VERSION    Seata 版本 (默认: 2.0.0)
  MYSQL_VERSION    MySQL 版本 (默认: 8.0)
  NACOS_VERSION    Nacos 版本 (默认: v2.3.2)
  MYSQL_ROOT_PASSWORD  MySQL root 密码 (默认: 123456)

示例:
  $0 start
  $0 logs seata-server
  $0 health
EOF
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."

    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    # 检查 Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi

    log_success "依赖检查通过"
}

# 启动服务
start_services() {
    log_info "启动 Seata 分布式事务环境..."

    # 创建必要的目录
    mkdir -p "$SEATA_DIR/data"
    mkdir -p "$SEATA_DIR/logs"

    # 启动服务
    docker-compose -f "$COMPOSE_FILE" up -d

    log_success "Seata 环境启动成功"
}

# 停止服务
stop_services() {
    log_info "停止 Seata 环境..."

    docker-compose -f "$COMPOSE_FILE" down

    log_success "Seata 环境已停止"
}

# 重启服务
restart_services() {
    log_info "重启 Seata 环境..."

    docker-compose -f "$COMPOSE_FILE" restart

    log_success "Seata 环境重启成功"
}

# 查看状态
check_status() {
    log_info "查看服务状态..."

    echo ""
    echo "============================================"
    echo "              服务状态                      "
    echo "============================================"
    echo ""

    docker-compose -f "$COMPOSE_FILE" ps

    echo ""
    echo "============================================"
    echo "              端口映射                      "
    echo "============================================"
    echo ""
    echo "Seata Server:  http://localhost:7091"
    echo "MySQL:         localhost:3307"
    echo "Nacos:         http://localhost:8888"
    echo "Prometheus:    http://localhost:9091"
    echo "Grafana:       http://localhost:3001"
    echo ""
}

# 查看日志
view_logs() {
    local service="${1:-}"

    if [ -z "$service" ]; then
        log_info "查看所有服务日志..."
        docker-compose -f "$COMPOSE_FILE" logs -f
    else
        log_info "查看服务日志: $service"
        docker-compose -f "$COMPOSE_FILE" logs -f "$service"
    fi
}

# 健康检查
health_check() {
    log_info "执行健康检查..."

    echo ""
    echo "============================================"
    echo "              健康检查                      "
    echo "============================================"
    echo ""

    # 检查 MySQL
    log_info "检查 MySQL..."
    if docker exec mysql-seata mysql -u root -p123456 -e "SELECT 1;" &> /dev/null; then
        log_success "MySQL 正常"
    else
        log_error "MySQL 异常"
    fi

    # 检查 Seata Server
    log_info "检查 Seata Server..."
    if curl -s http://localhost:7091/health > /dev/null; then
        log_success "Seata Server 正常"
    else
        log_error "Seata Server 异常"
    fi

    # 检查 Nacos
    log_info "检查 Nacos..."
    if curl -s http://localhost:8888/nacos/v1/console/health/readiness > /dev/null; then
        log_success "Nacos 正常"
    else
        log_error "Nacos 异常"
    fi

    # 检查 Prometheus
    log_info "检查 Prometheus..."
    if curl -s http://localhost:9091/-/healthy > /dev/null; then
        log_success "Prometheus 正常"
    else
        log_error "Prometheus 异常"
    fi

    # 检查 Grafana
    log_info "检查 Grafana..."
    if curl -s http://localhost:3001/api/health > /dev/null; then
        log_success "Grafana 正常"
    else
        log_error "Grafana 异常"
    fi

    echo ""
    echo "============================================"
    echo "            微服务集成检查                  "
    echo "============================================"
    echo ""

    # 检查微服务
    services=("basebackend-user-service:8081" "basebackend-auth-service:8082"
              "basebackend-menu-service:8088" "basebackend-application-service:8087")

    for service in "${services[@]}"; do
        name=$(echo $service | cut -d: -f1)
        port=$(echo $service | cut -d: -f2)
        log_info "检查 $name..."

        if curl -s http://localhost:$port/actuator/health > /dev/null; then
            log_success "$name 正常"
        else
            log_warn "$name 未运行或未集成 Seata"
        fi
    done

    echo ""
    log_success "健康检查完成"
}

# 清理环境
clean_environment() {
    log_warn "此操作将删除所有数据，确认继续？ (y/N)"
    read -r response

    if [[ "$response" =~ ^[Yy]$ ]]; then
        log_info "清理 Seata 环境..."

        docker-compose -f "$COMPOSE_FILE" down -v
        docker system prune -f

        log_success "Seata 环境已清理"
    else
        log_info "取消清理操作"
    fi
}

# 应用配置到微服务
apply_service_config() {
    log_info "正在生成微服务 Seata 配置..."

    # 创建配置文件目录
    mkdir -p "$SEATA_DIR/generated-configs"

    cat << EOF > "$SEATA_DIR/generated-configs/user-service-seata.yml"
# Seata 配置 for basebackend-user-service
seata:
  tx-service-group: basebackend_user_tx_group
  service:
    vgroup-mapping:
      basebackend_user_tx_group: default
    grouplist:
      default: localhost:8091
  data-source-proxy-mode: AT
  client:
    undo:
      log-table: undo_log
EOF

    cat << EOF > "$SEATA_DIR/generated-configs/auth-service-seata.yml"
# Seata 配置 for basebackend-auth-service
seata:
  tx-service-group: basebackend_auth_tx_group
  service:
    vgroup-mapping:
      basebackend_auth_tx_group: default
    grouplist:
      default: localhost:8091
  data-source-proxy-mode: AT
  client:
    undo:
      log-table: undo_log
EOF

    log_success "配置文件已生成到: $SEATA_DIR/generated-configs/"
}

# 显示访问信息
show_access_info() {
    echo ""
    echo "============================================"
    echo "              访问信息                      "
    echo "============================================"
    echo ""
    echo "Seata Server 控制台:"
    echo "  URL:      http://localhost:7091"
    echo "  用户名:   admin"
    echo "  密码:     admin"
    echo ""
    echo "Nacos 控制台:"
    echo "  URL:      http://localhost:8888"
    echo "  用户名:   nacos"
    echo "  密码:     nacos"
    echo ""
    echo "Prometheus:"
    echo "  URL:      http://localhost:9091"
    echo ""
    echo "Grafana:"
    echo "  URL:      http://localhost:3001"
    echo "  用户名:   admin"
    echo "  密码:     admin123"
    echo ""
    echo "MySQL:"
    echo "  主机:     localhost:3307"
    echo "  用户名:   root"
    echo "  密码:     123456"
    echo "  数据库:   seata"
    echo ""
    echo "============================================"
    echo ""
}

# 主函数
main() {
    case "${1:-}" in
        start)
            check_dependencies
            start_services
            show_access_info
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            ;;
        status)
            check_status
            ;;
        logs)
            view_logs "$2"
            ;;
        health)
            health_check
            ;;
        clean)
            clean_environment
            ;;
        config)
            apply_service_config
            ;;
        -h|--help|help)
            show_help
            ;;
        *)
            log_error "未知命令: ${1:-}"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
