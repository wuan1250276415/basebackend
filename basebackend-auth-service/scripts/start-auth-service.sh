#!/bin/bash
# =====================================================================
# 权限服务启动脚本
# 创建时间: 2025-11-15
# 描述: 启动权限服务及相关依赖
# =====================================================================

set -e

echo "======================================="
echo "权限服务启动脚本"
echo "======================================="

# 配置变量
SERVICE_PORT=8082
SERVICE_NAME="basebackend-auth-service"
SERVICE_LOG="logs/auth-service.log"

# 创建日志目录
mkdir -p logs

echo "检查依赖服务..."

# 检查MySQL
echo "检查MySQL..."
if ! nc -z localhost 3306; then
    echo "警告: MySQL服务不可用，请确保MySQL已启动"
    echo "启动MySQL: sudo systemctl start mysql"
    read -p "是否继续? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 检查Redis
echo "检查Redis..."
if ! nc -z localhost 6379; then
    echo "警告: Redis服务不可用，请确保Redis已启动"
    echo "启动Redis: sudo systemctl start redis"
    read -p "是否继续? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 检查Nacos
echo "检查Nacos..."
if ! nc -z localhost 8848; then
    echo "警告: Nacos服务不可用，请确保Nacos已启动"
    echo "启动Nacos: cd nacos/bin && sh startup.sh -m standalone"
    read -p "是否继续? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 检查端口是否被占用
if lsof -i :${SERVICE_PORT} > /dev/null 2>&1; then
    echo "警告: 端口 ${SERVICE_PORT} 已被占用"
    echo "正在停止占用进程..."
    lsof -ti :${SERVICE_PORT} | xargs kill -9
    sleep 2
fi

# 编译项目
echo "编译权限服务..."
mvn clean compile -DskipTests

# 启动服务
echo "启动权限服务..."
nohup mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
    > ${SERVICE_LOG} 2>&1 &

# 获取进程ID
SERVICE_PID=$!
echo "权限服务已启动，PID: ${SERVICE_PID}"

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
if curl -f http://localhost:${SERVICE_PORT}/actuator/health > /dev/null 2>&1; then
    echo "======================================="
    echo "✅ 权限服务启动成功!"
    echo "======================================="
    echo "📖 API文档: http://localhost:${SERVICE_PORT}/swagger-ui.html"
    echo "🔍 健康检查: http://localhost:${SERVICE_PORT}/actuator/health"
    echo "📊 监控指标: http://localhost:${SERVICE_PORT}/actuator/prometheus"
    echo "📋 服务日志: ${SERVICE_LOG}"
    echo "======================================="
else
    echo "❌ 权限服务启动失败"
    echo "📋 查看日志: tail -f ${SERVICE_LOG}"
    echo "======================================="
    exit 1
fi
