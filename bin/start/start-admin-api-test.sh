#!/bin/bash

echo "启动后台管理API服务测试..."

# 设置环境变量
export NACOS_SERVER_ADDR=localhost:8848
export NACOS_NAMESPACE=public
export NACOS_GROUP=DEFAULT_GROUP
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos

# 进入项目目录
cd /home/wuan/IdeaProjects/basebackend

# 启动服务
echo "启动 admin-api 服务..."
cd basebackend-admin-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev &
ADMIN_API_PID=$!

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 测试服务是否启动成功
echo "测试服务启动状态..."
curl -s http://localhost:8082/actuator/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ admin-api 服务启动成功"
else
    echo "❌ admin-api 服务启动失败"
    kill $ADMIN_API_PID 2>/dev/null
    exit 1
fi

# 测试登录接口
echo "测试登录接口..."
curl -X POST http://localhost:8082/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' \
  -w "\nHTTP状态码: %{http_code}\n" \
  -s

echo ""
echo "测试完成！"
echo "按 Ctrl+C 停止服务"
echo "服务PID: $ADMIN_API_PID"

# 等待用户中断
wait $ADMIN_API_PID
