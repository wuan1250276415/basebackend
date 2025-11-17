#!/bin/bash
set -e

echo "======================================="
echo "字典服务启动脚本"
echo "======================================="

SERVICE_PORT=8083
SERVICE_NAME="basebackend-dict-service"
SERVICE_LOG="logs/dict-service.log"

mkdir -p logs

# 检查依赖服务
echo "检查依赖服务..."
if ! nc -z localhost 3306; then echo "MySQL不可用"; exit 1; fi
if ! nc -z localhost 6379; then echo "Redis不可用"; exit 1; fi
if ! nc -z localhost 8848; then echo "Nacos不可用"; exit 1; fi

# 检查端口
if lsof -i :${SERVICE_PORT} > /dev/null 2>&1; then
    echo "端口 ${SERVICE_PORT} 已被占用"
    lsof -ti :${SERVICE_PORT} | xargs kill -9
    sleep 2
fi

# 编译和启动
echo "编译字典服务..."
mvn clean compile -DskipTests

echo "启动字典服务..."
nohup mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" > ${SERVICE_LOG} 2>&1 &

echo "字典服务已启动，PID: $!"
sleep 10

if curl -f http://localhost:${SERVICE_PORT}/actuator/health > /dev/null 2>&1; then
    echo "✅ 字典服务启动成功!"
else
    echo "❌ 字典服务启动失败"
    exit 1
fi
