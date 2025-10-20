#!/bin/bash

# RabbitMQ停止脚本

echo "正在停止RabbitMQ服务..."

# 检测是否需要sudo
if groups | grep -q docker; then
  DOCKER_CMD="docker compose"
else
  echo "当前用户不在docker组中，需要使用sudo"
  DOCKER_CMD="sudo docker compose"
fi

# 进入脚本所在目录
cd "$(dirname "$0")"

# 停止服务
$DOCKER_CMD down

if [ $? -eq 0 ]; then
  echo "RabbitMQ服务已停止"
else
  echo "RabbitMQ服务停止失败！"
  exit 1
fi
