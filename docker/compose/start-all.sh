#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CALLER_DIR="$(pwd)"

# 设置环境
ENV_FILE_INPUT=${1:-env/.env.dev}
if [[ "$ENV_FILE_INPUT" = /* ]]; then
    ENV_FILE="$ENV_FILE_INPUT"
else
    ENV_FILE="$CALLER_DIR/$ENV_FILE_INPUT"
fi
NETWORK_NAME=basebackend-network
echo "Using environment: $ENV_FILE"

cd "$SCRIPT_DIR"

# 检查环境文件
if [ ! -f "$ENV_FILE" ]; then
    echo "Environment file not found: $ENV_FILE"
    echo "Please copy env/.env.example to $ENV_FILE and configure it"
    exit 1
fi

# 创建共享网络（首次启动时）
if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
    echo "Creating docker network: $NETWORK_NAME"
    docker network create "$NETWORK_NAME" >/dev/null
fi

# 启动基础设施
echo "Starting base infrastructure (MySQL + Redis)..."
docker-compose -f base/docker-compose.base.yml --env-file "$ENV_FILE" up -d

# 等待基础设施就绪
echo "Waiting for infrastructure to be ready..."
sleep 30

# 启动中间件
echo "Starting middleware (Nacos + RocketMQ)..."
docker-compose -f middleware/docker-compose.middleware.yml --env-file "$ENV_FILE" up -d

# 等待中间件就绪
echo "Waiting for middleware to be ready..."
sleep 60

echo "Base infrastructure and middleware started successfully!"
echo ""
echo "Service URLs:"
echo "  MySQL:              localhost:3306"
echo "  Redis:              localhost:6379"
echo "  Nacos Console:      http://localhost:8848/nacos (nacos/nacos)"
echo "  RocketMQ Console:   http://localhost:8180"
echo ""
echo "Check status with:"
echo "  docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE ps"
echo "  docker-compose -f middleware/docker-compose.middleware.yml --env-file $ENV_FILE ps"
