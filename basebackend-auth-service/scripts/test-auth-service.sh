#!/bin/bash
# =====================================================================
# 权限服务测试脚本
# 创建时间: 2025-11-15
# 描述: 测试权限服务API接口
# =====================================================================

set -e

# 配置变量
AUTH_API_URL="http://localhost:8082"
HEALTH_URL="http://localhost:8082/actuator/health"

echo "======================================="
echo "权限服务 API 测试"
echo "======================================="

# 检查服务健康状态
echo "1. 检查服务健康状态..."
if curl -f ${HEALTH_URL} > /dev/null 2>&1; then
    echo "✅ 服务健康"
else
    echo "❌ 服务不可用"
    exit 1
fi

# 测试获取所有角色
echo "2. 测试获取所有角色..."
curl -X GET "${AUTH_API_URL}/api/auth/roles" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据ID获取角色
echo "3. 测试根据ID获取角色..."
curl -X GET "${AUTH_API_URL}/api/auth/roles/1" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试获取所有权限
echo "4. 测试获取所有权限..."
curl -X GET "${AUTH_API_URL}/api/auth/permissions" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据权限标识获取权限
echo "5. 测试根据权限标识获取权限..."
curl -X GET "${AUTH_API_URL}/api/auth/permissions/role:list" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试检查角色名唯一性
echo "6. 测试检查角色名唯一性..."
curl -X GET "${AUTH_API_URL}/api/auth/roles/check-name?roleName=test" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试检查权限标识唯一性
echo "7. 测试检查权限标识唯一性..."
curl -X GET "${AUTH_API_URL}/api/auth/permissions/check-permission?permission=test:permission" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据用户ID获取角色
echo "8. 测试根据用户ID获取角色..."
curl -X GET "${AUTH_API_URL}/api/auth/roles/by-user/1" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据用户ID获取权限
echo "9. 测试根据用户ID获取权限..."
curl -X GET "${AUTH_API_URL}/api/auth/permissions/by-user/1" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

echo "======================================="
echo "测试完成！"
echo "======================================="
