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
echo "Using environment: $ENV_FILE"

cd "$SCRIPT_DIR"

# 停止中间件
echo "Stopping middleware..."
docker-compose -f middleware/docker-compose.middleware.yml --env-file "$ENV_FILE" down

# 停止基础设施
echo "Stopping base infrastructure..."
docker-compose -f base/docker-compose.base.yml --env-file "$ENV_FILE" down

echo "All services stopped!"
echo ""
echo "To remove volumes (WARNING: This will delete all data):"
echo "  docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE down -v"
echo "  docker-compose -f middleware/docker-compose.middleware.yml --env-file $ENV_FILE down -v"
