#!/bin/bash

# ===================================================================
# XXL-Job 分布式任务调度部署脚本
# ===================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
XXL_JOB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$XXL_JOB_DIR/docker-compose-xxljob.yml"

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
XXL-Job 分布式任务调度部署脚本

用法:
  $0 [命令]

命令:
  start       启动 XXL-Job 环境
  stop        停止 XXL-Job 环境
  restart     重启 XXL-Job 环境
  status      查看服务状态
  logs        查看日志
  clean       清理环境
  health      健康检查

环境变量:
  MYSQL_ROOT_PASSWORD  MySQL root 密码 (默认: 123456)
  XXL_JOB_VERSION      XXL-Job 版本 (默认: 2.4.0)

示例:
  $0 start
  $0 logs xxl-job-server
  $0 health
EOF
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi

    log_success "依赖检查通过"
}

# 启动服务
start_services() {
    log_info "启动 XXL-Job 分布式任务调度环境..."

    # 创建必要的目录
    mkdir -p "$XXL_JOB_DIR/xxljob-logs"
    mkdir -p "$XXL_JOB_DIR/xxljob-db"
    mkdir -p "$XXL_JOB_DIR/mysql-data"
    mkdir -p "$XXL_JOB_DIR/xxljob-executor-logs"
    mkdir -p "$XXL_JOB_DIR/monitoring"

    # 启动服务
    docker-compose -f "$COMPOSE_FILE" up -d

    log_success "XXL-Job 环境启动成功"
}

# 停止服务
stop_services() {
    log_info "停止 XXL-Job 环境..."

    docker-compose -f "$COMPOSE_FILE" down

    log_success "XXL-Job 环境已停止"
}

# 重启服务
restart_services() {
    log_info "重启 XXL-Job 环境..."

    docker-compose -f "$COMPOSE_FILE" restart

    log_success "XXL-Job 环境重启成功"
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
    echo "XXL-Job 调度中心: http://localhost:8080/xxl-job-admin"
    echo "MySQL:            localhost:3308"
    echo "执行器示例:       http://localhost:8081/actuator/health"
    echo "Prometheus:       http://localhost:9092"
    echo "Grafana:          http://localhost:3002"
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
    if docker exec mysql-xxljob mysql -u root -p123456 -e "SELECT 1;" &> /dev/null; then
        log_success "MySQL 正常"
    else
        log_error "MySQL 异常"
    fi

    # 检查 XXL-Job Server
    log_info "检查 XXL-Job Server..."
    if curl -s http://localhost:8080/xxl-job-admin/toLogin > /dev/null; then
        log_success "XXL-Job Server 正常"
    else
        log_error "XXL-Job Server 异常"
    fi

    # 检查 XXL-Job 执行器
    log_info "检查 XXL-Job 执行器..."
    if curl -s http://localhost:8081/actuator/health > /dev/null; then
        log_success "XXL-Job 执行器正常"
    else
        log_warn "XXL-Job 执行器未运行或异常"
    fi

    # 检查 Prometheus
    log_info "检查 Prometheus..."
    if curl -s http://localhost:9092/-/healthy > /dev/null; then
        log_success "Prometheus 正常"
    else
        log_warn "Prometheus 异常"
    fi

    # 检查 Grafana
    log_info "检查 Grafana..."
    if curl -s http://localhost:3002/api/health > /dev/null; then
        log_success "Grafana 正常"
    else
        log_warn "Grafana 异常"
    fi

    echo ""
    echo "============================================"
    echo "            XXL-Job 控制台访问信息           "
    echo "============================================"
    echo ""
    echo "调度中心:"
    echo "  URL:      http://localhost:8080/xxl-job-admin"
    echo "  用户名:   admin"
    echo "  密码:     123456"
    echo ""
    echo "数据库:"
    echo "  主机:     localhost:3308"
    echo "  用户名:   root"
    echo "  密码:     123456"
    echo "  数据库:   xxl_job"
    echo ""
    echo "============================================"
    echo ""
    log_success "健康检查完成"
}

# 清理环境
clean_environment() {
    log_warn "此操作将删除所有数据，确认继续？ (y/N)"
    read -r response

    if [[ "$response" =~ ^[Yy]$ ]]; then
        log_info "清理 XXL-Job 环境..."

        docker-compose -f "$COMPOSE_FILE" down -v
        docker system prune -f

        log_success "XXL-Job 环境已清理"
    else
        log_info "取消清理操作"
    fi
}

# 测试任务
test_jobs() {
    log_info "测试 XXL-Job 任务..."

    echo ""
    echo "============================================"
    echo "           XXL-Job 任务测试                 "
    echo "============================================"
    echo ""

    # 触发测试任务
    log_info "触发测试任务..."

    # 获取任务 ID (需要从控制台获取，这里假设为 1)
    local job_id=1

    if curl -X POST "http://localhost:8080/xxl-job-admin/jobinfo/trigger" \
        -d "id=$job_id" \
        -d "executorParam=test" \
        > /dev/null 2>&1; then
        log_success "测试任务触发成功"
    else
        log_warn "任务触发失败，请检查 XXL-Job 控制台"
    fi

    echo ""
    log_success "任务测试完成，请查看 XXL-Job 控制台的任务执行日志"
}

# 显示任务配置指南
show_task_guide() {
    cat << EOF

============================================
        XXL-Job 任务配置指南
============================================

1. 在微服务中集成 XXL-Job:
   - 添加依赖: xxl-job-core:2.4.0
   - 配置 application.yml
   - 创建 XxlJobConfig 配置类
   - 编写 @XxlJob 注解的任务处理器

2. 示例任务配置:
   - 任务名称: userDataSyncJob
   - 执行模式: BEAN
   - 调度类型: CRON
   - 表达式: 0 0 2 * * ? (每天凌晨2点)

3. 任务类型:
   - 普通任务: 按定时表达式执行
   - 分片任务: 分布式并行执行
   - 子任务: 任务依赖执行
   - 广播任务: 所有节点都执行

4. 监控和告警:
   - 任务执行成功率
   - 任务执行耗时
   - 失败重试次数
   - 告警通知设置

5. 常见问题:
   - 执行器注册失败: 检查 appname 和 accessToken
   - 任务不执行: 检查 Cron 表达式和触发状态
   - 任务执行失败: 查看任务日志定位问题

============================================

EOF
}

# 主函数
main() {
    case "${1:-}" in
        start)
            check_dependencies
            start_services
            show_task_guide
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
        test)
            test_jobs
            ;;
        clean)
            clean_environment
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
