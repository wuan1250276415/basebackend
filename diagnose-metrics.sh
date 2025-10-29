#!/bin/bash

echo "========================================="
echo "指标采集问题诊断工具"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查项计数
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

check() {
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -n "检查 $TOTAL_CHECKS: $1 ... "
}

pass() {
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
    echo -e "${GREEN}✅ 通过${NC}"
    if [ -n "$1" ]; then
        echo "    $1"
    fi
}

fail() {
    FAILED_CHECKS=$((FAILED_CHECKS + 1))
    echo -e "${RED}❌ 失败${NC}"
    if [ -n "$1" ]; then
        echo -e "    ${YELLOW}$1${NC}"
    fi
}

# 1. 检查 admin-api 服务
check "admin-api 服务是否运行在 8080 端口"
if ss -tuln | grep -q ":8080"; then
    pass "端口 8080 已监听"
else
    fail "端口 8080 未监听，请启动 admin-api 服务"
fi

# 2. 检查 Actuator 端点
check "Actuator prometheus 端点是否可访问"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/prometheus)
if [ "$HTTP_CODE" = "200" ]; then
    pass "端点返回 200 OK"
else
    fail "端点返回 $HTTP_CODE，请检查 Spring Boot Actuator 配置"
fi

# 3. 检查 AOP 是否启用
check "Spring AOP 是否启用"
if curl -s http://localhost:8080/actuator/beans 2>/dev/null | grep -q "apiMetricsAspect"; then
    pass "找到 apiMetricsAspect Bean"
else
    fail "未找到 apiMetricsAspect Bean，AOP 可能未启用"
fi

# 4. 检查自定义指标
check "自定义指标是否被采集"
METRICS_COUNT=$(curl -s http://localhost:8080/actuator/prometheus 2>/dev/null | grep -c "^api_calls_total")
if [ "$METRICS_COUNT" -gt 0 ]; then
    pass "找到 $METRICS_COUNT 条 api_calls_total 指标"
else
    fail "未找到自定义指标，请检查：
        1. AOP 切面是否生效
        2. 是否至少调用过一次 API
        3. CustomMetrics Bean 是否正确注入"
    echo ""
    echo "    尝试触发一次 API 调用..."
    curl -s http://localhost:8080/actuator/health > /dev/null
    sleep 1
    METRICS_COUNT=$(curl -s http://localhost:8080/actuator/prometheus 2>/dev/null | grep -c "^api_calls_total")
    if [ "$METRICS_COUNT" -gt 0 ]; then
        echo -e "    ${GREEN}✅ 现在找到了指标！${NC}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        FAILED_CHECKS=$((FAILED_CHECKS - 1))
    fi
fi

# 5. 检查 Prometheus 服务
check "Prometheus 服务是否运行"
if curl -s http://141.98.196.113:9190/-/healthy > /dev/null 2>&1; then
    pass "Prometheus 服务正常"
else
    fail "Prometheus 服务不可达，请检查 Docker 容器"
fi

# 6. 检查 Prometheus 配置
check "Prometheus 配置文件"
if grep -q "192.168.66.13:8080" /home/wuan/IdeaProjects/basebackend/docker/observability/prometheus.yml 2>/dev/null; then
    pass "配置使用了正确的 IP 地址"
elif grep -q "host.docker.internal" /home/wuan/IdeaProjects/basebackend/docker/observability/prometheus.yml 2>/dev/null; then
    fail "配置仍使用 host.docker.internal，需要改为实际 IP
        运行: ip addr show | grep 'inet ' | grep -v '127.0.0.1' | head -1"
else
    fail "无法读取配置文件或配置异常"
fi

# 7. 检查 Prometheus 目标状态
check "Prometheus 是否能成功抓取指标"
PROM_HEALTH=$(curl -s "http://141.98.196.113:9190/api/v1/targets" 2>/dev/null | grep -o '"health":"[^"]*"' | head -1)
if echo "$PROM_HEALTH" | grep -q '"health":"up"'; then
    pass "目标状态为 UP"
else
    fail "目标状态为 DOWN 或未知
        运行: ./restart-prometheus.sh
        错误详情: $PROM_HEALTH"
fi

# 8. 检查 Prometheus 中的指标
check "Prometheus 中是否有自定义指标"
PROM_METRICS=$(curl -s "http://141.98.196.113:9190/api/v1/label/__name__/values" 2>/dev/null | grep -o '"api_[^"]*"' | wc -l)
if [ "$PROM_METRICS" -gt 0 ]; then
    pass "找到 $PROM_METRICS 个自定义指标"
else
    fail "Prometheus 中没有自定义指标，可能原因：
        1. Prometheus 还未抓取（等待 15-60 秒）
        2. 目标状态为 DOWN
        3. 应用中没有采集到指标"
fi

echo ""
echo "========================================="
echo "诊断结果汇总"
echo "========================================="
echo "总检查项: $TOTAL_CHECKS"
echo -e "通过: ${GREEN}$PASSED_CHECKS${NC}"
echo -e "失败: ${RED}$FAILED_CHECKS${NC}"
echo ""

if [ "$FAILED_CHECKS" -eq 0 ]; then
    echo -e "${GREEN}🎉 所有检查通过！指标采集应该正常工作。${NC}"
    exit 0
else
    echo -e "${RED}⚠️  发现 $FAILED_CHECKS 个问题，请根据上述提示进行修复。${NC}"
    echo ""
    echo "常见解决步骤："
    echo "1. 确保 admin-api 服务已启动"
    echo "2. 修改 docker/observability/prometheus.yml 中的 IP 地址"
    echo "3. 运行: ./restart-prometheus.sh"
    echo "4. 等待 30 秒后重新运行此诊断脚本"
    exit 1
fi
