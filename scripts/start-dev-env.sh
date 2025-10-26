#!/bin/bash
# Docker Compose + Flyway 快速启动脚本

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

cd "$(dirname "$0")"

log_info "========================================"
log_info "启动开发环境 (MySQL + Redis + Flyway)"
log_info "========================================"

# 启动服务
log_info "启动Docker Compose服务..."
docker-compose -f docker-compose-flyway.yml up -d

# 等待Flyway完成
log_info "等待Flyway迁移完成..."
docker-compose -f docker-compose-flyway.yml logs -f flyway-admin &
LOGS_PID=$!

# 等待容器退出
while docker ps --format '{{.Names}}' | grep -q flyway-admin; do
    sleep 2
done

kill $LOGS_PID 2>/dev/null || true

# 检查Flyway退出码
EXIT_CODE=$(docker inspect flyway-admin --format='{{.State.ExitCode}}')

if [ "$EXIT_CODE" -eq 0 ]; then
    log_info "========================================"
    log_info "✅ 开发环境启动成功！"
    log_info "========================================"
    log_info "MySQL:  localhost:3308"
    log_info "  用户: root / root"
    log_info "  数据库: basebackend_admin"
    log_info ""
    log_info "Redis:  localhost:6379"
    log_info "  密码: redis123"
    log_info "========================================"
else
    log_info "❌ Flyway迁移失败，退出码: $EXIT_CODE"
    log_info "查看日志: docker-compose -f docker-compose-flyway.yml logs flyway-admin"
    exit 1
fi
