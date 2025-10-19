#!/bin/bash

echo "启动后台管理API服务..."

# 设置环境变量
export NACOS_SERVER=localhost:8848
export NACOS_NAMESPACE=
export NACOS_GROUP=DEFAULT_GROUP

# 进入项目目录
cd /home/wuan/IdeaProjects/basebackend

# 启动服务
cd basebackend-admin-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev

echo "后台管理API服务启动完成"
