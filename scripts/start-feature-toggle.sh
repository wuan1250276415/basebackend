#!/bin/bash
# Feature Toggle服务快速启动脚本

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

cd "$(dirname "$0")/.."

log_info "========================================"
log_info "启动Feature Toggle服务"
log_info "========================================"

# 启动服务
log_info "启动Docker Compose服务..."
docker compose -f docker-compose-feature-toggle.yml up -d

# 等待服务就绪
log_info "等待服务就绪..."
sleep 10

# 检查Unleash状态
if curl -f http://localhost:4242/health > /dev/null 2>&1; then
    log_info "✅ Unleash服务已就绪"
else
    log_warn "⚠️  Unleash服务启动中，请稍后..."
fi

# 检查Flagsmith状态
if curl -f http://localhost:8000/health > /dev/null 2>&1; then
    log_info "✅ Flagsmith服务已就绪"
else
    log_warn "⚠️  Flagsmith服务启动中，请稍后..."
fi

log_info "========================================"
log_info "✅ Feature Toggle服务启动完成"
log_info "========================================"
log_info ""
log_info "🚀 Unleash:"
log_info "   Web UI:   http://localhost:4242"
log_info "   用户名:   admin"
log_info "   密码:     unleash4all"
log_info "   API URL:  http://localhost:4242/api"
log_info ""
log_info "🚀 Flagsmith:"
log_info "   Web UI:   http://localhost:8000"
log_info "   首次访问需要创建管理员账户"
log_info "   API URL:  http://localhost:8000/api/v1/"
log_info ""
log_info "========================================"
log_info "查看日志:"
log_info "  docker-compose -f docker-compose-feature-toggle.yml logs -f"
log_info ""
log_info "停止服务:"
log_info "  docker-compose -f docker-compose-feature-toggle.yml down"
log_info "========================================"
