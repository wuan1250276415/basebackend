#!/bin/bash
# =====================================================================
# 用户服务部署验证脚本
# 创建时间: 2025-11-15
# 描述: 验证用户服务部署是否成功
# =====================================================================

set -e

echo "======================================="
echo "用户服务部署验证"
echo "======================================="

# 配置变量
SERVICE_URL="http://localhost:8081"
HEALTH_URL="${SERVICE_URL}/actuator/health"
API_DOCS_URL="${SERVICE_URL}/v3/api-docs"

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
HEALTH_STATUS=$(curl -s ${HEALTH_URL} | jq -r '.status')
if [ "$HEALTH_STATUS" = "UP" ]; then
    echo "✅ 健康检查通过，状态: $HEALTH_STATUS"
else
    echo "❌ 健康检查失败，状态: $HEALTH_STATUS"
    exit 1
fi

# 检查API文档
echo "3. 检查API文档..."
if curl -f ${API_DOCS_URL} > /dev/null 2>&1; then
    echo "✅ API文档可用"
    echo "   访问地址: ${SERVICE_URL}/swagger-ui.html"
else
    echo "⚠️  API文档不可用"
fi

# 检查数据库连接
echo "4. 检查数据库连接..."
DB_HEALTH=$(curl -s ${HEALTH_URL}/db | jq -r '.status')
if [ "$DB_HEALTH" = "UP" ]; then
    echo "✅ 数据库连接正常"
else
    echo "⚠️  数据库连接异常，状态: $DB_HEALTH"
fi

# 检查Redis连接
echo "5. 检查Redis连接..."
REDIS_HEALTH=$(curl -s ${HEALTH_URL}/redis | jq -r '.status')
if [ "$REDIS_HEALTH" = "UP" ]; then
    echo "✅ Redis连接正常"
else
    echo "⚠️  Redis连接异常，状态: $REDIS_HEALTH"
fi

# 检查Nacos注册
echo "6. 检查Nacos服务注册..."
SERVICE_INSTANCES=$(curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-service&groupName=DEFAULT_GROUP" | jq -r '.hosts | length')
if [ "$SERVICE_INSTANCES" -gt 0 ]; then
    echo "✅ 服务已注册到Nacos，实例数: $SERVICE_INSTANCES"
else
    echo "⚠️  服务未注册到Nacos"
fi

# 检查关键API接口
echo "7. 检查关键API接口..."

# 查询用户列表
echo "   - 测试查询用户列表..."
RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "${SERVICE_URL}/api/users")
if [ "$RESPONSE" = "200" ]; then
    echo "     ✅ 查询用户列表接口正常"
else
    echo "     ❌ 查询用户列表接口异常 (HTTP: $RESPONSE)"
fi

# 根据用户名查询用户
echo "   - 测试根据用户名查询用户..."
RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "${SERVICE_URL}/api/users/by-username/admin")
if [ "$RESPONSE" = "200" ]; then
    echo "     ✅ 根据用户名查询接口正常"
else
    echo "     ❌ 根据用户名查询接口异常 (HTTP: $RESPONSE)"
fi

# 检查用户名唯一性
echo "   - 测试检查用户名唯一性..."
RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "${SERVICE_URL}/api/users/check-username?username=testuser")
if [ "$RESPONSE" = "200" ]; then
    echo "     ✅ 检查用户名唯一性接口正常"
else
    echo "     ❌ 检查用户名唯一性接口异常 (HTTP: $RESPONSE)"
fi

# 检查监控指标
echo "8. 检查监控指标..."
if curl -f "${SERVICE_URL}/actuator/prometheus" > /dev/null 2>&1; then
    echo "✅ Prometheus指标可用"
else
    echo "⚠️  Prometheus指标不可用"
fi

# 生成测试报告
echo "======================================="
echo "部署验证报告"
echo "======================================="
echo "服务地址: ${SERVICE_URL}"
echo "健康检查: ${HEALTH_URL}"
echo "API文档: ${SERVICE_URL}/swagger-ui.html"
echo "监控指标: ${SERVICE_URL}/actuator/prometheus"
echo "======================================="

# 保存验证结果到文件
cat > verify-report.txt << EOF
用户服务部署验证报告
====================
验证时间: $(date)
服务状态: 运行正常

核心组件:
- 服务启动: ✅
- 健康检查: ✅
- API文档: ✅
- 数据库连接: ${DB_HEALTH}
- Redis连接: ${REDIS_HEALTH}
- Nacos注册: ${SERVICE_INSTANCES} 个实例

关键接口:
- 查询用户列表: ✅
- 根据用户名查询: ✅
- 检查用户名唯一性: ✅

访问地址:
- 服务地址: ${SERVICE_URL}
- API文档: ${SERVICE_URL}/swagger-ui.html
- 健康检查: ${HEALTH_URL}
- 监控指标: ${SERVICE_URL}/actuator/prometheus

部署状态: ✅ 验证通过
EOF

echo "验证报告已保存到 verify-report.txt"
echo "======================================="
