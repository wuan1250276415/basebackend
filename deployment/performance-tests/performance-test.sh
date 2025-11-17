#!/bin/bash
# ================================================================================================
# BaseBackend 性能测试脚本 (Bash)
# ================================================================================================
#
# 用途：对 BaseBackend API 进行基础性能压测
#
# 依赖：
# - curl
# - Apache Bench (ab) - 可选，用于更详细的压测
#
# 使用方法：
# ./performance-test.sh [target-url] [concurrency] [requests]
#
# 示例：
# ./performance-test.sh http://localhost:8080 10 1000
#
# ================================================================================================

# 默认配置
TARGET_URL="${1:-http://localhost:8080}"
CONCURRENCY="${2:-10}"
TOTAL_REQUESTS="${3:-1000}"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "================================================================================================"
echo " BaseBackend 性能测试"
echo "================================================================================================"
echo ""
echo "目标 URL: $TARGET_URL"
echo "并发数: $CONCURRENCY"
echo "总请求数: $TOTAL_REQUESTS"
echo ""

# ========================================
# 1. 健康检查端点测试
# ========================================
echo -e "${GREEN}【1/5】健康检查端点性能测试${NC}"
echo "测试端点: ${TARGET_URL}/actuator/health"
echo ""

if command -v ab &> /dev/null; then
    ab -n $TOTAL_REQUESTS -c $CONCURRENCY "${TARGET_URL}/actuator/health" 2>&1 | grep -E "Requests per second|Time per request|Transfer rate"
else
    echo -e "${YELLOW}⚠ Apache Bench (ab) 未安装，使用 curl 简单测试${NC}"
    START_TIME=$(date +%s%N)
    for i in $(seq 1 100); do
        curl -s -o /dev/null -w "%{http_code}\n" "${TARGET_URL}/actuator/health" > /dev/null
    done
    END_TIME=$(date +%s%N)
    DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    echo "100 requests in ${DURATION}ms"
    echo "Average: $(( DURATION / 100 ))ms per request"
fi

echo ""
echo "------------------------------------------------------------------------------------------------"
echo ""

# ========================================
# 2. Metrics 端点测试
# ========================================
echo -e "${GREEN}【2/5】Metrics 端点性能测试${NC}"
echo "测试端点: ${TARGET_URL}/actuator/prometheus"
echo ""

if command -v ab &> /dev/null; then
    ab -n 100 -c 5 "${TARGET_URL}/actuator/prometheus" 2>&1 | grep -E "Requests per second|Time per request|Transfer rate"
else
    START_TIME=$(date +%s%N)
    for i in $(seq 1 50); do
        curl -s -o /dev/null "${TARGET_URL}/actuator/prometheus" > /dev/null
    done
    END_TIME=$(date +%s%N)
    DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    echo "50 requests in ${DURATION}ms"
    echo "Average: $(( DURATION / 50 ))ms per request"
fi

echo ""
echo "------------------------------------------------------------------------------------------------"
echo ""

# ========================================
# 3. API 端点响应时间测试
# ========================================
echo -e "${GREEN}【3/5】API 响应时间测试${NC}"
echo "测试 API 端点的平均响应时间..."
echo ""

API_ENDPOINTS=(
    "/actuator/health"
    "/actuator/health/liveness"
    "/actuator/health/readiness"
    "/actuator/metrics"
)

for endpoint in "${API_ENDPOINTS[@]}"; do
    echo -n "测试: $endpoint ... "
    TOTAL_TIME=0
    SUCCESS=0

    for i in $(seq 1 10); do
        RESPONSE_TIME=$(curl -o /dev/null -s -w "%{time_total}\n" "${TARGET_URL}${endpoint}")
        if [ $? -eq 0 ]; then
            TOTAL_TIME=$(echo "$TOTAL_TIME + $RESPONSE_TIME" | bc)
            ((SUCCESS++))
        fi
    done

    if [ $SUCCESS -gt 0 ]; then
        AVG_TIME=$(echo "scale=3; $TOTAL_TIME / $SUCCESS" | bc)
        echo -e "${GREEN}平均响应时间: ${AVG_TIME}s${NC}"
    else
        echo -e "${RED}失败${NC}"
    fi
done

echo ""
echo "------------------------------------------------------------------------------------------------"
echo ""

# ========================================
# 4. 并发压力测试
# ========================================
echo -e "${GREEN}【4/5】并发压力测试${NC}"
echo "模拟并发请求..."
echo ""

if command -v ab &> /dev/null; then
    echo "使用 Apache Bench 进行并发测试..."
    ab -n $TOTAL_REQUESTS -c $CONCURRENCY "${TARGET_URL}/actuator/health" > /tmp/ab_result.txt 2>&1

    echo ""
    echo "【测试结果汇总】"
    grep "Complete requests:" /tmp/ab_result.txt
    grep "Failed requests:" /tmp/ab_result.txt
    grep "Requests per second:" /tmp/ab_result.txt
    grep "Time per request:" /tmp/ab_result.txt
    grep "Transfer rate:" /tmp/ab_result.txt

    echo ""
    echo "【百分位响应时间】"
    grep -A 5 "Percentage of the requests" /tmp/ab_result.txt | tail -6

    rm -f /tmp/ab_result.txt
else
    echo -e "${YELLOW}⚠ Apache Bench (ab) 未安装，跳过详细并发测试${NC}"
    echo "建议安装: sudo apt-get install apache2-utils (Ubuntu/Debian)"
    echo "         或 brew install ab (macOS)"
fi

echo ""
echo "------------------------------------------------------------------------------------------------"
echo ""

# ========================================
# 5. Metrics 验证
# ========================================
echo -e "${GREEN}【5/5】验证 Metrics 数据${NC}"
echo "检查 Prometheus metrics 是否正常暴露..."
echo ""

METRICS_RESPONSE=$(curl -s "${TARGET_URL}/actuator/prometheus")

if echo "$METRICS_RESPONSE" | grep -q "jvm_memory_used_bytes"; then
    echo -e "${GREEN}✓ JVM 内存指标存在${NC}"
else
    echo -e "${RED}✗ JVM 内存指标缺失${NC}"
fi

if echo "$METRICS_RESPONSE" | grep -q "api_calls_total"; then
    echo -e "${GREEN}✓ API 调用指标存在${NC}"
else
    echo -e "${YELLOW}⚠ API 调用指标缺失（可能未调用过 API）${NC}"
fi

if echo "$METRICS_RESPONSE" | grep -q "business_users_total"; then
    echo -e "${GREEN}✓ 业务指标存在${NC}"
else
    echo -e "${YELLOW}⚠ 业务指标缺失（可能未初始化）${NC}"
fi

echo ""
echo "================================================================================================"
echo -e "${GREEN}性能测试完成！${NC}"
echo "================================================================================================"
echo ""
echo "下一步："
echo "1. 查看 Grafana 仪表板: http://localhost:3000 (admin/admin123)"
echo "2. 查看 Prometheus: http://localhost:9090"
echo "3. 检查应用日志中的慢接口告警"
echo ""
