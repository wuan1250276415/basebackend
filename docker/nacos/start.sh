#!/bin/bash

# Nacos集群启动脚本

echo "======================================"
echo "   启动Nacos集群 (3节点)"
echo "======================================"

# 检测是否需要sudo
if groups | grep -q docker; then
    DOCKER_CMD="docker"
    COMPOSE_CMD="docker compose"
else
    DOCKER_CMD="sudo docker"
    COMPOSE_CMD="sudo docker compose"
fi


# 创建必要的目录
mkdir -p mysql/data mysql/init
mkdir -p nacos1/logs nacos2/logs nacos3/logs

# 启动服务
echo "正在启动Nacos集群..."
$COMPOSE_CMD up -d

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "======================================"
echo "   服务状态"
echo "======================================"
$COMPOSE_CMD ps

echo ""
echo "======================================"
echo "   访问信息"
echo "======================================"
echo "Nacos控制台:"
echo "  - Node 1: http://localhost:8848/nacos"
echo "  - Node 2: http://localhost:8849/nacos"
echo "  - Node 3: http://localhost:8850/nacos"
echo ""
echo "默认账号: nacos / nacos"
echo ""
echo "MySQL连接信息:"
echo "  - Host: localhost:3307"
echo "  - Database: nacos_config"
echo "  - User: nacos / nacos123"
echo "======================================"
