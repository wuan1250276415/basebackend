#!/bin/bash
# =====================================================================
# 用户服务测试脚本
# 创建时间: 2025-11-15
# 描述: 测试用户服务API接口
# =====================================================================

set -e

# 配置变量
API_URL="http://localhost:8081/api/users"
HEALTH_URL="http://localhost:8081/actuator/health"

echo "======================================="
echo "用户服务 API 测试"
echo "======================================="

# 检查服务健康状态
echo "1. 检查服务健康状态..."
if curl -f ${HEALTH_URL} > /dev/null 2>&1; then
    echo "✅ 服务健康"
else
    echo "❌ 服务不可用"
    exit 1
fi

# 测试查询用户列表
echo "2. 测试查询用户列表..."
curl -X GET "${API_URL}" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据用户名查询用户
echo "3. 测试根据用户名查询用户..."
curl -X GET "${API_URL}/by-username/admin" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试检查用户名唯一性
echo "4. 测试检查用户名唯一性..."
curl -X GET "${API_URL}/check-username?username=testuser" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据手机号查询用户
echo "5. 测试根据手机号查询用户..."
curl -X GET "${API_URL}/by-phone/13800138000" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据邮箱查询用户
echo "6. 测试根据邮箱查询用户..."
curl -X GET "${API_URL}/by-email/admin@example.com" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试批量查询用户
echo "7. 测试批量查询用户..."
curl -X GET "${API_URL}/batch?ids=1,2" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试获取用户角色
echo "8. 测试获取用户角色..."
curl -X GET "${API_URL}/1/roles" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

echo "======================================="
echo "测试完成！"
echo "======================================="
