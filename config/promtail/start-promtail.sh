#!/bin/bash

# Promtail启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "启动Promtail日志收集服务"
echo "=========================================="

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker未运行，请先启动Docker"
    exit 1
fi

# 创建网络（如果不存在）
if ! docker network inspect basebackend-network > /dev/null 2>&1; then
    echo "创建Docker网络: basebackend-network"
    docker network create basebackend-network
fi

# 创建日志目录
echo "创建日志目录..."
mkdir -p ../../logs/{gateway,auth-api,user-api,system-api,notification-service,observability-service}

# 启动服务
echo "启动Loki和Promtail..."
docker-compose -f docker-compose.promtail.yml up -d

# 等待服务启动
echo "等待服务启动..."
sleep 5

# 检查服务状态
echo ""
echo "检查服务状态..."

if curl -s http://localhost:3100/ready > /dev/null; then
    echo "✓ Loki运行正常 (http://localhost:3100)"
else
    echo "✗ Loki启动失败"
fi

if curl -s http://localhost:9080/ready > /dev/null; then
    echo "✓ Promtail运行正常 (http://localhost:9080)"
else
    echo "✗ Promtail启动失败"
fi

echo ""
echo "=========================================="
echo "Promtail启动完成"
echo "=========================================="
echo ""
echo "服务地址:"
echo "  - Loki: http://localhost:3100"
echo "  - Promtail: http://localhost:9080"
echo ""
echo "查看日志:"
echo "  docker logs -f promtail"
echo "  docker logs -f loki"
echo ""
echo "查看Targets:"
echo "  curl http://localhost:9080/targets"
echo ""
echo "停止服务:"
echo "  docker-compose -f docker-compose.promtail.yml down"
echo ""
