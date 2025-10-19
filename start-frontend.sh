#!/bin/bash

echo "启动前端管理系统..."

# 进入前端项目目录
cd /home/wuan/IdeaProjects/basebackend/basebackend-admin-web

# 检查是否已安装依赖
if [ ! -d "node_modules" ]; then
    echo "首次运行，正在安装依赖..."
    npm install
    if [ $? -ne 0 ]; then
        echo "依赖安装失败，请检查 npm 配置"
        exit 1
    fi
fi

# 启动开发服务器
echo "启动开发服务器..."
npm run dev
