#!/bin/bash

# 停止可观测性基础设施

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

# 检查权限
if ! docker ps &> /dev/null; then
    USE_SUDO="sudo"
else
    USE_SUDO=""
fi

echo "正在停止可观测性服务..."
$USE_SUDO docker compose down

echo "✅ 服务已停止"
echo ""
echo "如需删除数据卷，运行："
echo "  docker compose down -v"
