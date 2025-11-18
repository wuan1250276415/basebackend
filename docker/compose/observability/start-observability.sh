#!/bin/bash

# BaseBackend可观测性栈启动脚本

set -e

echo "=========================================="
echo "Starting BaseBackend Observability Stack"
echo "=========================================="

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running"
    exit 1
fi

# 创建网络（如果不存在）
if ! docker network inspect basebackend-network > /dev/null 2>&1; then
    echo "Creating basebackend-network..."
    docker network create basebackend-network
fi

# 启动可观测性栈
echo "Starting observability services..."
docker-compose -f docker-compose.observability.yml up -d

# 等待服务启动
echo "Waiting for services to be ready..."
sleep 10

# 检查服务状态
echo ""
echo "Checking service status..."
docker-compose -f docker-compose.observability.yml ps

echo ""
echo "=========================================="
echo "Observability Stack Started Successfully!"
echo "=========================================="
echo ""
echo "Access URLs:"
echo "  - Prometheus:  http://localhost:9090"
echo "  - Grafana:     http://localhost:3000 (admin/admin)"
echo "  - Jaeger UI:   http://localhost:16686"
echo "  - Loki:        http://localhost:3100"
echo ""
echo "To stop the stack, run:"
echo "  docker-compose -f docker-compose.observability.yml down"
echo ""
