#!/bin/bash

# Redis连接测试脚本

set -e

REDIS_PASSWORD=${REDIS_PASSWORD:-redis2025}
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}

echo "=========================================="
echo "Redis连接测试"
echo "=========================================="
echo "Host: $REDIS_HOST"
echo "Port: $REDIS_PORT"
echo "Password: $REDIS_PASSWORD"
echo ""

# 测试1: 使用docker exec
echo "测试1: 使用docker exec连接..."
if docker exec basebackend-redis redis-cli -a "$REDIS_PASSWORD" ping > /dev/null 2>&1; then
    echo "✓ Docker exec连接成功"
else
    echo "✗ Docker exec连接失败"
fi

# 测试2: 使用redis-cli从宿主机连接
echo ""
echo "测试2: 从宿主机连接..."
if command -v redis-cli &> /dev/null; then
    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping > /dev/null 2>&1; then
        echo "✓ 宿主机连接成功"
    else
        echo "✗ 宿主机连接失败"
    fi
else
    echo "⚠ redis-cli未安装，跳过此测试"
fi

# 测试3: 设置和获取值
echo ""
echo "测试3: 测试读写操作..."
docker exec basebackend-redis redis-cli -a "$REDIS_PASSWORD" SET test_key "test_value" > /dev/null 2>&1
RESULT=$(docker exec basebackend-redis redis-cli -a "$REDIS_PASSWORD" GET test_key 2>/dev/null)
if [ "$RESULT" = "test_value" ]; then
    echo "✓ 读写操作成功"
    docker exec basebackend-redis redis-cli -a "$REDIS_PASSWORD" DEL test_key > /dev/null 2>&1
else
    echo "✗ 读写操作失败"
fi

# 测试4: 查看Redis信息
echo ""
echo "测试4: 查看Redis信息..."
docker exec basebackend-redis redis-cli -a "$REDIS_PASSWORD" INFO server 2>/dev/null | grep -E "redis_version|os|tcp_port" || true

echo ""
echo "=========================================="
echo "连接信息"
echo "=========================================="
echo "使用redis-cli连接命令："
echo "  redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD"
echo ""
echo "使用Docker exec连接命令："
echo "  docker exec -it basebackend-redis redis-cli -a $REDIS_PASSWORD"
echo ""
echo "Spring Boot配置："
echo "  spring.redis.host=$REDIS_HOST"
echo "  spring.redis.port=$REDIS_PORT"
echo "  spring.redis.password=$REDIS_PASSWORD"
echo ""
