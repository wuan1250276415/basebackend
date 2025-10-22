#!/bin/bash

# Gateway模块启动测试脚本

echo "======================================"
echo "   测试Gateway模块启动"
echo "======================================"

# 设置环境变量
export SPRING_PROFILES_ACTIVE=test
export JWT_SECRET="test-secret-key-for-jwt-token-generation-minimum-256-bits"

# 编译Gateway模块
echo "正在编译Gateway模块..."
mvn clean package -pl basebackend-gateway -am -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo "✅ 编译成功"

# 启动Gateway (后台运行，5秒后自动终止)
echo ""
echo "正在启动Gateway..."
timeout 5 java -jar basebackend-gateway/target/basebackend-gateway-*.jar --server.port=8081 2>&1 | head -50

# 检查是否有启动错误
if grep -q "APPLICATION FAILED TO START" <<< "$output"; then
    echo "❌ Gateway启动失败"
    exit 1
else
    echo "✅ Gateway启动测试通过"
fi

echo ""
echo "======================================"
echo "   测试完成"
echo "======================================"