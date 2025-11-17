#!/bin/bash

# 设置环境
ENV_FILE=${1:-env/.env.dev}
echo "Using environment: $ENV_FILE"

# 停止中间件
echo "Stopping middleware..."
docker-compose -f middleware/docker-compose.middleware.yml --env-file $ENV_FILE down

# 停止基础设施
echo "Stopping base infrastructure..."
docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE down

echo "All services stopped!"
echo ""
echo "To remove volumes (WARNING: This will delete all data):"
echo "  docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE down -v"
echo "  docker-compose -f middleware/docker-compose.middleware.yml --env-file $ENV_FILE down -v"
