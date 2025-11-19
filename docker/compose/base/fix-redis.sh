#!/bin/bash

# Redis快速修复脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Redis快速修复"
echo "=========================================="

# 停止Redis
echo "1. 停止Redis容器..."
docker-compose down redis 2>/dev/null || true

# 重新启动
echo "2. 重新启动Redis..."
docker-compose up -d redis

# 等待启动
echo "3. 等待Redis启动..."
sleep 3

# 测试连接
echo "4. 测试连接..."
if docker exec basebackend-redis redis-cli -a redis2025 ping > /dev/null 2>&1; then
    echo "✓ Redis启动成功"
else
    echo "✗ Redis启动失败"
    echo ""
    echo "查看日志："
    docker logs basebackend-redis
    exit 1
fi

# 运行完整测试
echo ""
echo "5. 运行完整测试..."
./test-redis-connection.sh

echo ""
echo "=========================================="
echo "Redis修复完成"
echo "=========================================="
echo ""
echo "连接信息："
echo "  Host: localhost"
echo "  Port: 6379"
echo "  Password: redis2025"
echo ""
echo "连接命令："
echo "  redis-cli -h localhost -p 6379 -a redis2025"
echo ""
