#!/bin/bash

# RocketMQ连接测试脚本

set -e

NAMESRV_ADDR=${NAMESRV_ADDR:-localhost:9876}

echo "=========================================="
echo "RocketMQ连接测试"
echo "=========================================="
echo "NameServer: $NAMESRV_ADDR"
echo ""

# 测试1: 检查NameServer
echo "测试1: 检查NameServer状态..."
if docker exec basebackend-rocketmq-namesrv sh -c "netstat -an | grep 9876" > /dev/null 2>&1; then
    echo "✓ NameServer运行正常"
else
    echo "✗ NameServer未运行"
fi

# 测试2: 检查Broker
echo ""
echo "测试2: 检查Broker状态..."
if docker exec basebackend-rocketmq-broker sh -c "netstat -an | grep 10911" > /dev/null 2>&1; then
    echo "✓ Broker运行正常"
else
    echo "✗ Broker未运行"
fi

# 测试3: 查看集群信息
echo ""
echo "测试3: 查看集群信息..."
docker exec basebackend-rocketmq-namesrv sh mqadmin clusterList -n localhost:9876 2>/dev/null || echo "⚠ 无法获取集群信息"

# 测试4: 创建测试主题
echo ""
echo "测试4: 创建测试主题..."
docker exec basebackend-rocketmq-broker sh mqadmin updateTopic -n localhost:9876 -t TestTopic -c DefaultCluster 2>/dev/null && echo "✓ 主题创建成功" || echo "⚠ 主题创建失败"

# 测试5: 查看主题列表
echo ""
echo "测试5: 查看主题列表..."
docker exec basebackend-rocketmq-namesrv sh mqadmin topicList -n localhost:9876 2>/dev/null | head -20 || echo "⚠ 无法获取主题列表"

echo ""
echo "=========================================="
echo "连接信息"
echo "=========================================="
echo "NameServer地址: $NAMESRV_ADDR"
echo "Broker地址: localhost:10911"
echo "Console地址: http://localhost:8180"
echo ""
echo "Spring Boot配置："
echo "  rocketmq.name-server=$NAMESRV_ADDR"
echo ""
echo "查看日志："
echo "  docker logs -f basebackend-rocketmq-namesrv"
echo "  docker logs -f basebackend-rocketmq-broker"
echo ""
