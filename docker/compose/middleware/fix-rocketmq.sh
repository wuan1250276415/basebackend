#!/bin/bash

# RocketMQ快速修复脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "RocketMQ快速修复"
echo "=========================================="

# 停止服务
echo "1. 停止RocketMQ服务..."
docker-compose down rocketmq-broker rocketmq-namesrv 2>/dev/null || true

# 清理旧数据（可选）
read -p "是否清理旧数据？这将删除所有消息 (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "2. 清理旧数据..."
    docker volume rm middleware_rocketmq-broker-data 2>/dev/null || true
    docker volume rm middleware_rocketmq-namesrv-data 2>/dev/null || true
else
    echo "2. 保留旧数据"
fi

# 重新启动
echo "3. 启动NameServer..."
docker-compose up -d rocketmq-namesrv

echo "4. 等待NameServer启动..."
sleep 10

echo "5. 启动Broker..."
docker-compose up -d rocketmq-broker

echo "6. 等待Broker启动..."
sleep 10

# 检查状态
echo ""
echo "7. 检查服务状态..."

if docker exec basebackend-rocketmq-namesrv sh -c "netstat -an | grep 9876" > /dev/null 2>&1; then
    echo "✓ NameServer运行正常"
else
    echo "✗ NameServer启动失败"
    echo "查看日志："
    docker logs basebackend-rocketmq-namesrv
fi

if docker exec basebackend-rocketmq-broker sh -c "netstat -an | grep 10911" > /dev/null 2>&1; then
    echo "✓ Broker运行正常"
else
    echo "✗ Broker启动失败"
    echo "查看日志："
    docker logs basebackend-rocketmq-broker
    exit 1
fi

# 启动Console
echo ""
echo "8. 启动Console..."
docker-compose up -d rocketmq-console

echo ""
echo "=========================================="
echo "RocketMQ修复完成"
echo "=========================================="
echo ""
echo "服务地址："
echo "  NameServer: localhost:9876"
echo "  Broker: localhost:10911"
echo "  Console: http://localhost:8180"
echo ""
echo "运行测试："
echo "  ./test-rocketmq.sh"
echo ""
