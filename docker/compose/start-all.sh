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

wait_for_container() {
    local container_name="$1"
    local mode="$2"
    local timeout_seconds="$3"
    local start_time
    start_time=$(date +%s)

    while true; do
        if ! docker inspect "$container_name" >/dev/null 2>&1; then
            if [ $(( $(date +%s) - start_time )) -ge "$timeout_seconds" ]; then
                echo "Container not found within timeout: $container_name"
                return 1
            fi
            sleep 2
            continue
        fi

        if [ "$mode" = "healthy" ]; then
            local health_status
            health_status=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_name" 2>/dev/null)
            if [ "$health_status" = "healthy" ] || [ "$health_status" = "running" ]; then
                return 0
            fi
        else
            local state_status exit_code
            state_status=$(docker inspect --format='{{.State.Status}}' "$container_name" 2>/dev/null)
            exit_code=$(docker inspect --format='{{.State.ExitCode}}' "$container_name" 2>/dev/null)
            if [ "$state_status" = "exited" ]; then
                if [ "$exit_code" = "0" ]; then
                    return 0
                fi
                echo "Container exited with failure: $container_name"
                docker logs "$container_name" || true
                return 1
            fi
        fi

        if [ $(( $(date +%s) - start_time )) -ge "$timeout_seconds" ]; then
            echo "Timeout waiting for container: $container_name"
            docker logs "$container_name" || true
            return 1
        fi

        sleep 3
    done
}

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

# 等待基础设施与迁移完成
echo "Waiting for MySQL to become healthy..."
wait_for_container "basebackend-mysql" "healthy" 180

echo "Waiting for Flyway migrations to complete..."
docker-compose -f base/docker-compose.base.yml -f docker-compose-flyway.yml --env-file "$ENV_FILE" up --abort-on-container-exit --exit-code-from flyway flyway
wait_for_container "basebackend-flyway" "completed" 240
docker-compose -f base/docker-compose.base.yml -f docker-compose-flyway.yml --env-file "$ENV_FILE" rm -fsv flyway >/dev/null 2>&1 || true

# 启动中间件
echo "Starting middleware (Nacos + RocketMQ)..."
docker-compose -f middleware/docker-compose.middleware.yml --env-file "$ENV_FILE" up -d

# 等待中间件就绪
echo "Waiting for middleware to be ready..."
sleep 60

echo "Base infrastructure, Flyway migrations and middleware started successfully!"
echo ""
echo "Service URLs:"
echo "  MySQL:              localhost:3306"
echo "  Redis:              localhost:6379"
echo "  Flyway:             completed during startup"
echo "  Nacos Console:      http://localhost:8848/nacos (nacos/nacos)"
echo "  RocketMQ Console:   http://localhost:8180"
echo ""
echo "Check status with:"
echo "  docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE ps"
echo "  docker-compose -f middleware/docker-compose.middleware.yml --env-file $ENV_FILE ps"
