#!/bin/bash

# 设置环境
ENV_FILE=${1:-env/.env.dev}
echo "Using environment: $ENV_FILE"

# 检查环境文件
if [ ! -f "$ENV_FILE" ]; then
    echo "Environment file not found: $ENV_FILE"
    echo "Please copy env/.env.example to $ENV_FILE and configure it"
    exit 1
fi

# 启动基础设施
echo "Starting base infrastructure..."
docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE up -d

# 等待基础设施就绪
echo "Waiting for infrastructure to be ready..."
sleep 30

# 启动中间件
echo "Starting middleware..."
docker-compose -f middleware/docker-compose.middleware.yml --env-file $ENV_FILE up -d

# 等待中间件就绪
echo "Waiting for middleware to be ready..."
sleep 60

echo "All services started successfully!"
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
