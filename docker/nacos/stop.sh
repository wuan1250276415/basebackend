#!/bin/bash

# Nacos集群停止脚本

echo "======================================"
echo "   停止Nacos集群"
echo "======================================"

# 检测是否需要sudo
if groups | grep -q docker; then
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="sudo docker compose"
fi

# 停止服务
$COMPOSE_CMD down

echo ""
echo "Nacos集群已停止"
