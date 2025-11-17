#!/bin/bash
# =====================================================================
# 部门服务测试脚本
# 创建时间: 2025-11-15
# 描述: 测试部门服务API接口
# =====================================================================

set -e

# 配置变量
DEPT_API_URL="http://localhost:8084/api/dept"
HEALTH_URL="http://localhost:8084/actuator/health"

echo "======================================="
echo "部门服务 API 测试"
echo "======================================="

# 检查服务健康状态
echo "1. 检查服务健康状态..."
if curl -f ${HEALTH_URL} > /dev/null 2>&1; then
    echo "✅ 服务健康"
else
    echo "❌ 服务不可用"
    exit 1
fi

# 测试获取部门列表
echo "2. 测试获取部门列表..."
curl -X GET "${DEPT_API_URL}" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据ID获取部门
echo "3. 测试根据ID获取部门..."
curl -X GET "${DEPT_API_URL}/1" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

# 测试根据父部门ID获取子部门
echo "4. 测试根据父部门ID获取子部门..."
curl -X GET "${DEPT_API_URL}/children/0" \
    -H "Content-Type: application/json" \
    -w "\nHTTP状态码: %{http_code}\n"

echo "======================================="
echo "测试完成！"
echo "======================================="
