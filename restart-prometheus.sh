#!/bin/bash

echo "========================================"
echo "重启 Prometheus 服务"
echo "========================================"
echo ""

cd /home/wuan/IdeaProjects/basebackend/docker/observability

# 尝试使用 docker-compose 或 docker compose
if command -v docker-compose &> /dev/null; then
    echo "使用 docker-compose 重启..."
    sudo docker-compose restart prometheus
elif docker compose version &> /dev/null 2>&1; then
    echo "使用 docker compose 重启..."
    sudo docker compose restart prometheus
else
    echo "尝试直接使用 docker 命令重启..."
    CONTAINER_ID=$(sudo docker ps | grep "basebackend-prometheus" | awk '{print $1}')
    if [ -n "$CONTAINER_ID" ]; then
        echo "找到容器 ID: $CONTAINER_ID"
        sudo docker restart $CONTAINER_ID
    else
        echo "❌ 未找到 Prometheus 容器"
        echo "请手动运行："
        echo "  cd docker/observability"
        echo "  sudo docker-compose restart prometheus"
        exit 1
    fi
fi

echo ""
echo "等待 Prometheus 启动..."
sleep 5

echo ""
echo "检查 Prometheus 目标状态..."
curl -s "http://141.98.196.113:9190/api/v1/targets" | jq '.data.activeTargets[] | {job: .labels.job, health: .health, lastError: .lastError}' 2>/dev/null || \
curl -s "http://141.98.196.113:9190/api/v1/targets" | grep -o '"health":"[^"]*"' | head -5

echo ""
echo "========================================"
echo "完成"
echo "========================================"
