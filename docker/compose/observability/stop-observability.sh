#!/bin/bash

# BaseBackend可观测性栈停止脚本

set -e

echo "=========================================="
echo "Stopping BaseBackend Observability Stack"
echo "=========================================="

# 停止服务
docker-compose -f docker-compose.observability.yml down

echo ""
echo "Observability Stack Stopped Successfully!"
echo ""
echo "To remove volumes (WARNING: This will delete all data):"
echo "  docker-compose -f docker-compose.observability.yml down -v"
echo ""
