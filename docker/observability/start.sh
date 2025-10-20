#!/bin/bash

# 可观测性基础设施启动脚本
# 需要 Docker 和 Docker Compose 支持

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "BaseBackend 可观测性基础设施部署"
echo "========================================="
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

echo "✅ Docker 已安装: $(docker --version)"

# 检查 Docker Compose
if ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

echo "✅ Docker Compose 已安装: $(docker compose version)"
echo ""

# 检查权限
if ! docker ps &> /dev/null; then
    echo "⚠️  当前用户没有 Docker 权限，需要使用 sudo"
    USE_SUDO="sudo"
else
    echo "✅ Docker 权限检查通过"
    USE_SUDO=""
fi

echo ""
echo "正在启动可观测性服务..."
echo ""

cd "$SCRIPT_DIR"

# 创建数据目录
mkdir -p loki-data prometheus-data tempo-data grafana-data

# 启动服务
$USE_SUDO docker compose up -d

echo ""
echo "========================================="
echo "服务启动完成！"
echo "========================================="
echo ""

# 等待服务就绪
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "服务状态检查："
echo "-------------------"

check_service() {
    local name=$1
    local port=$2
    local path=${3:-/}

    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port$path" | grep -q "200\|404\|405"; then
        echo "✅ $name: http://localhost:$port"
    else
        echo "⏳ $name: 正在启动... http://localhost:$port"
    fi
}

check_service "Loki" 3100 "/ready"
check_service "Prometheus" 9090
check_service "Tempo" 3200 "/ready"
check_service "Grafana" 3000

echo ""
echo "========================================="
echo "访问地址："
echo "========================================="
echo "Prometheus: http://localhost:9090"
echo "Loki:       http://localhost:3100"
echo "Tempo:      http://localhost:3200"
echo "Grafana:    http://localhost:3000 (admin/admin)"
echo ""
echo "Zipkin 端点: http://localhost:9411/api/v2/spans"
echo ""

echo "========================================="
echo "下一步："
echo "========================================="
echo "1. 配置应用的 application.yml"
echo "2. 启动 basebackend-admin-api"
echo "3. 访问前端监控页面"
echo ""

echo "查看日志："
echo "  docker compose logs -f"
echo ""

echo "停止服务："
echo "  docker compose down"
echo ""
