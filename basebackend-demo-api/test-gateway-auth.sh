#!/bin/bash

# 测试脚本：验证Gateway认证流程
# 1. 直接调用demo-api的登录接口（绕过Gateway）
# 2. 通过Gateway调用登录接口
# 3. 使用获取的token通过Gateway访问受保护资源

GATEWAY_URL="http://localhost:8180"
DEMO_API_URL="http://localhost:8081"

echo "======================================"
echo "测试1: 直接访问demo-api登录接口（绕过Gateway）"
echo "======================================"
echo "请求: POST $DEMO_API_URL/api/auth/login"
RESPONSE=$(curl -s -X POST "$DEMO_API_URL/api/auth/login" \
  -d "username=admin&password=123456")
echo "响应: $RESPONSE"
echo ""

# 提取token
TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
echo "提取的Token: $TOKEN"
echo ""

echo "======================================"
echo "测试2: 通过Gateway访问登录接口"
echo "======================================"
echo "请求: POST $GATEWAY_URL/api/auth/login"
GATEWAY_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/auth/login" \
  -d "username=admin&password=123456")
echo "响应: $GATEWAY_RESPONSE"
echo ""

# 提取gateway返回的token
GATEWAY_TOKEN=$(echo $GATEWAY_RESPONSE | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
echo "Gateway返回的Token: $GATEWAY_TOKEN"
echo ""

if [ -z "$GATEWAY_TOKEN" ]; then
    echo "❌ 错误: 通过Gateway无法获取token！"
    echo "这说明Gateway的认证过滤器可能拦截了登录请求。"
    echo ""
    echo "请检查Gateway日志中的以下信息："
    echo "  - 请求路径是否为 /api/auth/login"
    echo "  - 白名单匹配是否成功"
    echo ""
    exit 1
fi

echo "======================================"
echo "测试3: 使用Token通过Gateway访问用户列表"
echo "======================================"
echo "请求: GET $GATEWAY_URL/api/users"
echo "Token: Bearer $GATEWAY_TOKEN"
PROTECTED_RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/users" \
  -H "Authorization: Bearer $GATEWAY_TOKEN")
echo "响应: $PROTECTED_RESPONSE"
echo ""

echo "======================================"
echo "测试4: 不带Token访问受保护资源（应该失败）"
echo "======================================"
echo "请求: GET $GATEWAY_URL/api/users （无Token）"
NO_TOKEN_RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/users")
echo "响应: $NO_TOKEN_RESPONSE"
echo ""

echo "======================================"
echo "测试完成"
echo "======================================"
