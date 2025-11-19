#!/bin/bash

# 修复Loki权限问题

set -e

echo "=========================================="
echo "修复Loki权限问题"
echo "=========================================="

# 停止Loki容器
echo "停止Loki容器..."
docker-compose down loki 2>/dev/null || true

# 删除旧的volume
echo "删除旧的Loki数据卷..."
docker volume rm observability_loki-data 2>/dev/null || true

# 重新启动
echo "重新启动Loki..."
docker-compose up -d loki

# 等待启动
echo "等待Loki启动..."
sleep 5

# 检查状态
if curl -s http://localhost:3100/ready > /dev/null; then
    echo "✓ Loki启动成功"
else
    echo "✗ Loki启动失败，查看日志："
    docker logs basebackend-loki
    exit 1
fi

echo ""
echo "=========================================="
echo "权限问题已修复"
echo "=========================================="
