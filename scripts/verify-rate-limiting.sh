#!/bin/bash

# ============================================
# Gateway 限流功能验证脚本
# ============================================
# 用途: 验证 API Gateway 限流功能是否正常工作
# 作者: 浮浮酱 🐱
# 日期: 2025-11-13
# ============================================

echo "========================================"
echo "  Gateway 限流功能验证脚本"
echo "========================================"
echo ""

# 配置
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8180}"
API_PREFIX="/api"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试计数
TOTAL_REQUESTS=0
SUCCESS_REQUESTS=0
RATE_LIMITED_REQUESTS=0

# 测试登录接口限流 (5 req/s)
test_login_rate_limit() {
    echo -e "${YELLOW}[测试 1/3]${NC} 测试登录接口限流（限制: 5 req/s，突发: 10）..."
    echo ""

    local limit_triggered=false
    local request_count=15  # 发送 15 个请求，应该有部分被限流

    for i in $(seq 1 $request_count); do
        TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))

        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -d '{"username":"test","password":"123456"}' \
            "${GATEWAY_URL}${API_PREFIX}/auth/login")
        HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

        if [ "$HTTP_CODE" -eq 429 ]; then
            echo -e "${BLUE}[请求 $i/$request_count]${NC} ${RED}✅ 触发限流 (429 Too Many Requests)${NC}"
            RATE_LIMITED_REQUESTS=$((RATE_LIMITED_REQUESTS + 1))
            limit_triggered=true
        elif [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 401 ]; then
            echo -e "${BLUE}[请求 $i/$request_count]${NC} ${GREEN}✓ 请求成功 (HTTP $HTTP_CODE)${NC}"
            SUCCESS_REQUESTS=$((SUCCESS_REQUESTS + 1))
        else
            echo -e "${BLUE}[请求 $i/$request_count]${NC} 其他响应 (HTTP $HTTP_CODE)"
        fi

        # 不加延迟,快速发送请求以触发限流
    done

    echo ""
    if [ "$limit_triggered" = true ]; then
        echo -e "${GREEN}✅ 登录接口限流功能正常工作！${NC}"
        echo "   成功请求: $SUCCESS_REQUESTS"
        echo "   限流拦截: $RATE_LIMITED_REQUESTS"
    else
        echo -e "${RED}❌ 未触发限流,可能配置未生效${NC}"
    fi
    echo "========================================"
}

# 测试注册接口限流 (3 req/s)
test_register_rate_limit() {
    echo -e "${YELLOW}[测试 2/3]${NC} 测试注册接口限流（限制: 3 req/s，突发: 6）..."
    echo ""

    local limit_triggered=false
    local request_count=10

    local success_count=0
    local limited_count=0

    for i in $(seq 1 $request_count); do
        TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))

        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -d '{"username":"newuser'$i'","password":"123456","email":"test'$i'@example.com"}' \
            "${GATEWAY_URL}${API_PREFIX}/auth/register")
        HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

        if [ "$HTTP_CODE" -eq 429 ]; then
            echo -e "${BLUE}[请求 $i/$request_count]${NC} ${RED}✅ 触发限流 (429 Too Many Requests)${NC}"
            limited_count=$((limited_count + 1))
            limit_triggered=true
        elif [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 400 ]; then
            echo -e "${BLUE}[请求 $i/$request_count]${NC} ${GREEN}✓ 请求成功 (HTTP $HTTP_CODE)${NC}"
            success_count=$((success_count + 1))
        else
            echo -e "${BLUE}[请求 $i/$request_count]${NC} 其他响应 (HTTP $HTTP_CODE)"
        fi
    done

    echo ""
    if [ "$limit_triggered" = true ]; then
        echo -e "${GREEN}✅ 注册接口限流功能正常工作！${NC}"
        echo "   成功请求: $success_count"
        echo "   限流拦截: $limited_count"
    else
        echo -e "${RED}❌ 未触发限流,可能配置未生效${NC}"
    fi
    echo "========================================"
}

# 测试文件上传限流 (5 req/s)
test_file_upload_rate_limit() {
    echo -e "${YELLOW}[测试 3/3]${NC} 测试文件上传限流（限制: 5 req/s，突发: 10）..."
    echo ""

    local limit_triggered=false
    local request_count=12

    local success_count=0
    local limited_count=0

    for i in $(seq 1 $request_count); do
        TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))

        # 创建临时文件
        echo "test content $i" > /tmp/test_upload_$i.txt

        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
            -F "file=@/tmp/test_upload_$i.txt" \
            "${GATEWAY_URL}${API_PREFIX}/files/upload")
        HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

        if [ "$HTTP_CODE" -eq 429 ]; then
            echo -e "${BLUE}[请求 $i/$request_count]${NC} ${RED}✅ 触发限流 (429 Too Many Requests)${NC}"
            limited_count=$((limited_count + 1))
            limit_triggered=true
        elif [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 404 ]; then
            echo -e "${BLUE}[请求 $i/$request_count]${NC} ${GREEN}✓ 请求成功 (HTTP $HTTP_CODE)${NC}"
            success_count=$((success_count + 1))
        else
            echo -e "${BLUE}[请求 $i/$request_count]${NC} 其他响应 (HTTP $HTTP_CODE)"
        fi

        # 清理临时文件
        rm -f /tmp/test_upload_$i.txt
    done

    echo ""
    if [ "$limit_triggered" = true ]; then
        echo -e "${GREEN}✅ 文件上传限流功能正常工作！${NC}"
        echo "   成功请求: $success_count"
        echo "   限流拦截: $limited_count"
    else
        echo -e "${RED}❌ 未触发限流,可能配置未生效${NC}"
    fi
    echo "========================================"
}

# 主函数
main() {
    echo "📌 测试配置:"
    echo "   Gateway URL: $GATEWAY_URL"
    echo "   API Prefix: $API_PREFIX"
    echo ""
    echo "开始测试..."
    echo ""

    # 运行测试
    test_login_rate_limit
    echo ""
    sleep 2  # 等待 2 秒让令牌桶恢复

    test_register_rate_limit
    echo ""
    sleep 2

    test_file_upload_rate_limit

    # 输出结果
    echo ""
    echo "========================================"
    echo "  测试结果汇总"
    echo "========================================"
    echo "总请求数: $TOTAL_REQUESTS"
    echo -e "${GREEN}成功请求: $SUCCESS_REQUESTS${NC}"
    echo -e "${RED}限流拦截: $RATE_LIMITED_REQUESTS${NC}"
    echo ""

    if [ $RATE_LIMITED_REQUESTS -gt 0 ]; then
        echo -e "${GREEN}✅ Gateway 限流功能正常工作喵～${NC}"
        echo ""
        echo "📊 限流统计:"
        echo "   限流拦截率: $(echo "scale=2; $RATE_LIMITED_REQUESTS * 100 / $TOTAL_REQUESTS" | bc)%"
        echo ""
        echo "📝 下一步:"
        echo "   1. 查看 Redis 中的限流数据:"
        echo "      redis-cli -h 1.117.67.222 -p 6379 -a redis_ycecQi"
        echo "      KEYS *rate*"
        echo ""
        echo "   2. 访问 Gateway Actuator 监控:"
        echo "      ${GATEWAY_URL}/actuator/metrics/gateway.request.total"
        echo ""
        exit 0
    else
        echo -e "${RED}❌ 限流功能未触发,请检查配置${NC}"
        echo ""
        echo "🔧 故障排查:"
        echo "   1. 确认 Gateway 应用已重启"
        echo "   2. 检查 Nacos 配置中 gateway.rate-limit.enabled 是否为 true"
        echo "   3. 检查 Redis 连接是否正常"
        echo "   4. 查看 Gateway 日志中是否有错误信息"
        echo ""
        exit 1
    fi
}

# 运行主函数
main
