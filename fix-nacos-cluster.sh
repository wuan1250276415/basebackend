#!/bin/bash

# Nacos集群修复脚本
# 解决HealthHandler bean创建失败的问题

echo "======================================"
echo "   Nacos集群修复脚本"
echo "======================================"

# 检测是否需要sudo
if groups | grep -q docker; then
    DOCKER_CMD="docker"
    COMPOSE_CMD="docker compose"
else
    DOCKER_CMD="sudo docker"
    COMPOSE_CMD="sudo docker compose"
fi

# 停止并清理现有容器
echo "正在停止现有容器..."
$COMPOSE_CMD -f docker/nacos/docker-compose.yml down -v

# 清理数据目录（可选，如果需要完全重置）
read -p "是否要清理数据目录？这将删除所有配置数据 (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "清理数据目录..."
    rm -rf docker/nacos/mysql/data
    rm -rf docker/nacos/nacos1/logs
    rm -rf docker/nacos/nacos2/logs
    rm -rf docker/nacos/nacos3/logs
fi

# 创建必要的目录
echo "创建必要的目录..."
mkdir -p docker/nacos/mysql/data docker/nacos/mysql/init
mkdir -p docker/nacos/nacos1/logs docker/nacos/nacos2/logs docker/nacos/nacos3/logs

# 启动服务
echo "正在启动Nacos集群..."
$COMPOSE_CMD -f docker/nacos/docker-compose.yml up -d

# 等待MySQL启动
echo "等待MySQL启动..."
sleep 15

# 等待Nacos启动
echo "等待Nacos启动..."
sleep 30

# 检查服务状态
echo ""
echo "======================================"
echo "   服务状态"
echo "======================================"
$COMPOSE_CMD -f docker/nacos/docker-compose.yml ps

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

# 检查Nacos日志
echo ""
echo "======================================"
echo "   检查Nacos启动日志"
echo "======================================"
echo "Nacos1 日志:"
$COMPOSE_CMD -f docker/nacos/docker-compose.yml logs nacos1 --tail=20

echo ""
echo "如果仍有问题，请检查具体错误日志："
echo "$COMPOSE_CMD -f docker/nacos/docker-compose.yml logs nacos1"
