#!/bin/bash

# JWT互通性诊断脚本

GATEWAY_URL="http://localhost:8180"
DEMO_API_URL="http://localhost:8081"

echo "======================================"
echo "JWT互通性诊断测试"
echo "======================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "测试1: 直接访问demo-api登录（验证JWT生成）"
echo "======================================"
echo "请求: POST $DEMO_API_URL/basebackend-demo-api/api/auth/login"
DIRECT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$DEMO_API_URL/basebackend-demo-api/api/auth/login" \
  -d "username=admin&password=123456")

HTTP_CODE=$(echo "$DIRECT_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
BODY=$(echo "$DIRECT_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP状态码: $HTTP_CODE"
echo "响应体: $BODY"

if [ "$HTTP_CODE" == "200" ]; then
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
    echo -e "${GREEN}✅ 成功: demo-api可以生成Token${NC}"
    echo "Token: $TOKEN"
else
    echo -e "${RED}❌ 失败: demo-api无法生成Token${NC}"
    exit 1
fi
echo ""

echo "======================================"
echo "测试2: 通过Gateway访问登录接口"
echo "======================================"
echo "请求: POST $GATEWAY_URL/basebackend-demo-api/api/auth/login"
GATEWAY_LOGIN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$GATEWAY_URL/basebackend-demo-api/api/auth/login" \
  -d "username=admin&password=123456")

GATEWAY_HTTP_CODE=$(echo "$GATEWAY_LOGIN_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
GATEWAY_BODY=$(echo "$GATEWAY_LOGIN_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP状态码: $GATEWAY_HTTP_CODE"
echo "响应体: $GATEWAY_BODY"

if [ "$GATEWAY_HTTP_CODE" == "200" ]; then
    GATEWAY_TOKEN=$(echo "$GATEWAY_BODY" | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
    echo -e "${GREEN}✅ 成功: 通过Gateway可以获取Token${NC}"
    echo "Gateway Token: $GATEWAY_TOKEN"
elif [ "$GATEWAY_HTTP_CODE" == "401" ]; then
    echo -e "${RED}❌ 失败: Gateway拦截了登录请求（白名单未生效）${NC}"
    echo -e "${YELLOW}提示: 检查AuthenticationFilter的白名单配置${NC}"
    exit 1
else
    echo -e "${RED}❌ 失败: 未知错误${NC}"
    exit 1
fi
echo ""

echo "======================================"
echo "测试3: 验证两个Token是否可以互相识别"
echo "======================================"

# 测试3a: 使用直接获取的Token通过Gateway访问
echo "3a. 使用demo-api生成的Token通过Gateway访问用户列表"
echo "Token: $TOKEN"
GATEWAY_VALIDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$GATEWAY_URL/basebackend-demo-api/api/users" \
  -H "Authorization: Bearer $TOKEN")

VALIDATE_HTTP_CODE=$(echo "$GATEWAY_VALIDATE_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
VALIDATE_BODY=$(echo "$GATEWAY_VALIDATE_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP状态码: $VALIDATE_HTTP_CODE"
echo "响应体前100字符: $(echo $VALIDATE_BODY | cut -c1-100)"

if [ "$VALIDATE_HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✅ 成功: Gateway可以验证demo-api生成的Token${NC}"
elif [ "$VALIDATE_HTTP_CODE" == "401" ]; then
    echo -e "${RED}❌ 失败: Gateway无法验证demo-api生成的Token${NC}"
    echo -e "${YELLOW}可能原因:${NC}"
    echo "  1. JWT secret不一致"
    echo "  2. JwtUtil实现不同"
    echo "  3. Token签名算法不同"
else
    echo -e "${YELLOW}⚠️  收到HTTP $VALIDATE_HTTP_CODE${NC}"
fi
echo ""

# 测试3b: 使用Gateway获取的Token直接访问demo-api
echo "3b. 使用Gateway获取的Token直接访问demo-api"
echo "Token: $GATEWAY_TOKEN"
DIRECT_VALIDATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$DEMO_API_URL/basebackend-demo-api/api/users" \
  -H "Authorization: Bearer $GATEWAY_TOKEN")

DIRECT_VALIDATE_HTTP_CODE=$(echo "$DIRECT_VALIDATE_RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)
DIRECT_VALIDATE_BODY=$(echo "$DIRECT_VALIDATE_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP状态码: $DIRECT_VALIDATE_HTTP_CODE"
echo "响应体前100字符: $(echo $DIRECT_VALIDATE_BODY | cut -c1-100)"

if [ "$DIRECT_VALIDATE_HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✅ 成功: demo-api可以验证Gateway获取的Token${NC}"
elif [ "$DIRECT_VALIDATE_HTTP_CODE" == "401" ]; then
    echo -e "${RED}❌ 失败: demo-api无法验证Gateway获取的Token${NC}"
else
    echo -e "${YELLOW}⚠️  收到HTTP $DIRECT_VALIDATE_HTTP_CODE${NC}"
fi
echo ""

echo "======================================"
echo "测试4: 检查JWT配置一致性"
echo "======================================"
echo "检查Gateway配置文件..."
GATEWAY_SECRET=$(grep -A1 "^jwt:" /home/wuan/IdeaProjects/basebackend/basebackend-gateway/src/main/resources/application.yml | grep "secret:" | awk '{print $2}')
echo "Gateway JWT Secret: $GATEWAY_SECRET"

echo ""
echo "检查demo-api配置文件..."
DEMO_SECRET=$(grep -A1 "^jwt:" /home/wuan/IdeaProjects/basebackend/basebackend-demo-api/src/main/resources/application.yml | grep "secret:" | awk '{print $2}')
echo "Demo-API JWT Secret: $DEMO_SECRET"

echo ""
if [ "$GATEWAY_SECRET" == "$DEMO_SECRET" ]; then
    echo -e "${GREEN}✅ JWT Secret配置一致${NC}"
else
    echo -e "${RED}❌ JWT Secret配置不一致！${NC}"
    echo "这是Token无法互通的主要原因"
fi
echo ""

echo "======================================"
echo "测试5: 解码Token查看内容"
echo "======================================"

# 解码Token的payload部分（Base64）
decode_jwt_payload() {
    local token=$1
    # JWT格式: header.payload.signature
    local payload=$(echo $token | cut -d'.' -f2)
    # 添加padding if needed
    local mod=$((${#payload} % 4))
    if [ $mod -eq 2 ]; then
        payload="${payload}=="
    elif [ $mod -eq 3 ]; then
        payload="${payload}="
    fi
    echo $payload | base64 -d 2>/dev/null | jq . 2>/dev/null || echo "无法解码"
}

echo "demo-api生成的Token内容:"
decode_jwt_payload "$TOKEN"
echo ""

echo "Gateway获取的Token内容:"
decode_jwt_payload "$GATEWAY_TOKEN"
echo ""

echo "======================================"
echo "诊断总结"
echo "======================================"
if [ "$VALIDATE_HTTP_CODE" == "200" ] && [ "$DIRECT_VALIDATE_HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}🎉 成功: JWT Token完全互通！${NC}"
else
    echo -e "${RED}❌ JWT Token无法互通${NC}"
    echo ""
    echo "请检查以下项目:"
    echo "1. Gateway和demo-api的JWT secret是否完全一致"
    echo "2. Gateway日志中是否有Token验证失败的错误信息"
    echo "3. demo-api日志中是否有Token验证失败的错误信息"
    echo "4. 确认两个服务都使用了basebackend-jwt模块的JwtUtil"
fi
