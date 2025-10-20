#!/bin/bash

# RabbitMQ启动脚本

echo "正在启动RabbitMQ服务..."

# 检测是否需要sudo
if groups | grep -q docker; then
  DOCKER_CMD="docker compose"
else
  echo "当前用户不在docker组中，需要使用sudo"
  DOCKER_CMD="sudo docker compose"
fi

# 进入脚本所在目录
cd "$(dirname "$0")"

# 启动服务
$DOCKER_CMD up -d

if [ $? -eq 0 ]; then
  echo "RabbitMQ服务启动成功！"
  echo ""
  echo "服务信息："
  echo "  AMQP端口: 5672"
  echo "  管理界面: http://localhost:15672"
  echo "  默认用户名: admin"
  echo "  默认密码: admin123"
  echo "  虚拟主机: basebackend"
  echo ""
  echo "等待服务健康检查..."
  sleep 10

  echo ""
  echo "检查服务状态："
  $DOCKER_CMD ps

  echo ""
  echo "查看服务日志："
  echo "  $DOCKER_CMD logs -f rabbitmq"
else
  echo "RabbitMQ服务启动失败！"
  exit 1
fi
