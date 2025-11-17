#!/bin/bash

# ============================================
# 监控功能验证脚本
# ============================================
# 用途: 验证 Druid 监控和慢查询监控功能
# 作者: 浮浮酱 🐱
# 日期: 2025-11-13
# ============================================

echo "========================================"
echo "  监控功能验证脚本"
echo "========================================"
echo ""

# 配置
BASE_URL="${BASE_URL:-http://localhost:8080}"
DRUID_USERNAME="${DRUID_USERNAME:-admin}"
DRUID_PASSWORD="${DRUID_PASSWORD:-admin123}"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试计数
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试 Druid 监控页面
test_druid_monitor() {
    echo -e "${YELLOW}[测试 1/5]${NC} 测试 Druid 监控页面访问..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    DRUID_URL="${BASE_URL}/druid/index.html"
    RESPONSE=$(curl -s -w "\n%{http_code}" "$DRUID_URL")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 401 ]; then
        echo -e "${GREEN}✅ Druid 监控页面可访问${NC}"
        echo "   URL: $DRUID_URL"
        echo "   用户名: $DRUID_USERNAME"
        echo "   密码: $DRUID_PASSWORD"
        echo ""
        echo "📝 请在浏览器中访问上述 URL 查看监控数据"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ Druid 监控页面访问失败 (HTTP $HTTP_CODE)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

# 测试慢查询监控 API
test_slow_sql_api() {
    echo -e "${YELLOW}[测试 2/5]${NC} 测试慢查询监控 API..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    SLOW_SQL_URL="${BASE_URL}/api/database/slow-sql/top?topN=10"
    RESPONSE=$(curl -s -w "\n%{http_code}" "$SLOW_SQL_URL")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✅ 慢查询 API 调用成功${NC}"
        echo "   响应: $BODY"
        echo ""

        # 检查是否有慢查询数据
        if echo "$BODY" | grep -q '"total"'; then
            echo "📊 检测到慢查询数据"
        else
            echo "ℹ️  当前暂无慢查询记录（这是正常的）"
        fi
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ 慢查询 API 调用失败 (HTTP $HTTP_CODE)${NC}"
        echo "   响应: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

# 测试慢查询统计 API
test_slow_sql_statistics() {
    echo -e "${YELLOW}[测试 3/5]${NC} 测试慢查询统计 API..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    STATS_URL="${BASE_URL}/api/database/slow-sql/statistics"
    RESPONSE=$(curl -s -w "\n%{http_code}" "$STATS_URL")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✅ 慢查询统计 API 调用成功${NC}"
        echo "   响应: $BODY"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ 慢查询统计 API 调用失败 (HTTP $HTTP_CODE)${NC}"
        echo "   响应: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

# 测试健康检查 API
test_health_check() {
    echo -e "${YELLOW}[测试 4/5]${NC} 测试健康检查 API..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    HEALTH_URL="${BASE_URL}/api/database/slow-sql/health"
    RESPONSE=$(curl -s -w "\n%{http_code}" "$HEALTH_URL")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✅ 健康检查 API 调用成功${NC}"
        echo "   响应: $BODY"

        if echo "$BODY" | grep -q '"status":"UP"'; then
            echo -e "${GREEN}   服务状态: UP (正常运行)${NC}"
        fi
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ 健康检查 API 调用失败 (HTTP $HTTP_CODE)${NC}"
        echo "   响应: $BODY"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

# 测试 Prometheus 指标端点
test_prometheus_metrics() {
    echo -e "${YELLOW}[测试 5/5]${NC} 测试 Prometheus 指标端点..."
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    PROMETHEUS_URL="${BASE_URL}/actuator/prometheus"
    RESPONSE=$(curl -s -w "\n%{http_code}" "$PROMETHEUS_URL")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -eq 200 ]; then
        echo -e "${GREEN}✅ Prometheus 指标端点可访问${NC}"

        # 检查关键指标
        echo ""
        echo "📊 关键指标检查:"

        if echo "$BODY" | grep -q "mybatis_slow_sql_count"; then
            echo -e "${GREEN}   ✓ mybatis_slow_sql_count_total - 慢查询总数${NC}"
        else
            echo -e "${RED}   ✗ mybatis_slow_sql_count_total - 未找到${NC}"
        fi

        if echo "$BODY" | grep -q "mybatis_sql_execution_time"; then
            echo -e "${GREEN}   ✓ mybatis_sql_execution_time_seconds - SQL 执行时间${NC}"
        else
            echo -e "${RED}   ✗ mybatis_sql_execution_time_seconds - 未找到${NC}"
        fi

        if echo "$BODY" | grep -q "jvm_memory"; then
            echo -e "${GREEN}   ✓ jvm_memory_* - JVM 内存指标${NC}"
        fi

        if echo "$BODY" | grep -q "http_server_requests"; then
            echo -e "${GREEN}   ✓ http_server_requests_* - HTTP 请求指标${NC}"
        fi

        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ Prometheus 指标端点访问失败 (HTTP $HTTP_CODE)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "========================================"
}

# 主函数
main() {
    echo "📌 测试配置:"
    echo "   Base URL: $BASE_URL"
    echo "   Druid 用户名: $DRUID_USERNAME"
    echo ""
    echo "开始测试..."
    echo ""

    # 运行测试
    test_druid_monitor
    sleep 1
    test_slow_sql_api
    sleep 1
    test_slow_sql_statistics
    sleep 1
    test_health_check
    sleep 1
    test_prometheus_metrics

    # 输出结果
    echo ""
    echo "========================================"
    echo "  测试结果汇总"
    echo "========================================"
    echo "总测试数: $TOTAL_TESTS"
    echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
    echo -e "${RED}失败: $FAILED_TESTS${NC}"
    echo ""

    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}✅ 所有监控功能正常工作喵～${NC}"
        echo ""
        echo "📊 监控访问指南:"
        echo ""
        echo "1. Druid 监控面板:"
        echo "   URL: ${BASE_URL}/druid/index.html"
        echo "   功能: SQL 统计、连接池监控、URI 监控"
        echo ""
        echo "2. 慢查询 API:"
        echo "   TOP 10: GET ${BASE_URL}/api/database/slow-sql/top?topN=10"
        echo "   统计: GET ${BASE_URL}/api/database/slow-sql/statistics"
        echo "   健康: GET ${BASE_URL}/api/database/slow-sql/health"
        echo ""
        echo "3. Prometheus 指标:"
        echo "   URL: ${BASE_URL}/actuator/prometheus"
        echo "   用途: Grafana 数据源、监控告警"
        echo ""
        echo "4. 应用健康检查:"
        echo "   URL: ${BASE_URL}/actuator/health"
        echo ""
        exit 0
    else
        echo -e "${RED}❌ 部分监控功能异常,请检查配置${NC}"
        echo ""
        echo "🔧 故障排查:"
        echo "   1. 确认应用已启动"
        echo "   2. 检查端口是否正确 (默认 8080)"
        echo "   3. 查看应用日志中的错误信息"
        echo "   4. 检查 Nacos 配置是否已加载"
        echo ""
        exit 1
    fi
}

# 运行主函数
main
