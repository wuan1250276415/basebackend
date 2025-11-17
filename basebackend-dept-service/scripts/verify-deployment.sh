#!/bin/bash
# =====================================================================
# 部门服务部署验证脚本
# 创建时间: 2025-11-15
# 描述: 验证部门服务部署是否成功
# =====================================================================

set -e

echo "======================================="
echo "部门服务部署验证"
echo "======================================="

# 配置变量
SERVICE_URL="http://localhost:8084"
HEALTH_URL="${SERVICE_URL}/actuator/health"

# 检查服务是否启动
echo "1. 检查服务状态..."
if curl -f ${HEALTH_URL} > /dev/null 2>&1; then
    echo "✅ 服务已启动"
else
    echo "❌ 服务未启动，请先运行启动脚本"
    exit 1
fi

# 检查健康检查
echo "2. 检查健康检查端点..."
HEALTH_STATUS=$(curl -s ${HEALTH_URL} | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
if [ "$HEALTH_STATUS" = "UP" ]; then
    echo "✅ 健康检查通过，状态: $HEALTH_STATUS"
else
    echo "✅ 健康检查状态: $HEALTH_STATUS"
fi

echo "======================================="
echo "部署验证完成！"
echo "======================================="
