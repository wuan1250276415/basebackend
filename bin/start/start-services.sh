#!/bin/bash

# 启动所有服务脚本

echo "=== BaseBackend 微服务启动脚本 ==="

# 检查 Nacos 是否运行
echo "检查 Nacos 服务状态..."
if curl -s http://localhost:8848/nacos/v1/ns/operator/servers > /dev/null; then
    echo "✅ Nacos 服务正常运行"
    
    # 上传配置到 Nacos
    echo "上传配置到 Nacos..."
    ../maintenance/upload-nacos-configs.sh
else
    echo "❌ Nacos 服务未启动，请先启动 Nacos"
    echo "运行: ./start-nacos.sh 或 docker-compose up -d"
    exit 1
fi

echo ""
echo "启动 Demo-API 服务..."
cd ../../basebackend-demo-api
mvn spring-boot:run &
DEMO_API_PID=$!
echo "Demo-API PID: $DEMO_API_PID"

# 等待 Demo-API 启动
echo "等待 Demo-API 启动..."
sleep 15

echo ""
echo "启动 Gateway 服务..."
cd ../basebackend-gateway
mvn spring-boot:run &
GATEWAY_PID=$!
echo "Gateway PID: $GATEWAY_PID"

echo ""
echo "=== 服务启动完成 ==="
echo "Demo-API: http://localhost:8081"
echo "Gateway: http://localhost:8180"
echo "Nacos Console: http://localhost:8848/nacos"
echo ""
echo "测试登录接口:"
echo "curl -X POST 'http://localhost:8180/api/auth/login' -H 'Content-Type: application/x-www-form-urlencoded' -d 'username=admin&password=123456'"
echo ""
echo "按 Ctrl+C 停止所有服务"

# 等待用户中断
trap "echo '正在停止服务...'; kill $DEMO_API_PID $GATEWAY_PID; exit" INT
wait
