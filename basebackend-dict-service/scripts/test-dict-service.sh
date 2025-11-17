#!/bin/bash
set -e

API_URL="http://localhost:8083/api/dict"
HEALTH_URL="http://localhost:8083/actuator/health"

echo "======================================="
echo "字典服务 API 测试"
echo "======================================="

if ! curl -f ${HEALTH_URL} > /dev/null 2>&1; then
    echo "❌ 服务不可用"
    exit 1
fi
echo "✅ 服务健康"

echo "测试获取字典类型列表..."
curl -X GET "${API_URL}/types" -H "Content-Type: application/json" -w "\nHTTP状态码: %{http_code}\n"

echo "测试完成！"
