#!/bin/bash

echo "========================================="
echo "测试指标采集和查询 - 完整版"
echo "========================================="
echo ""

# 0. 前置检查：Prometheus 连接状态
echo "0. 检查 Prometheus 是否能连接到 admin-api..."
echo "-----------------------------------"
PROMETHEUS_HEALTH=$(curl -s "http://141.98.196.113:9190/api/v1/targets" | grep -o '"health":"[^"]*"' | head -1)

if echo "$PROMETHEUS_HEALTH" | grep -q '"health":"up"'; then
    echo "✅ Prometheus 连接正常：$PROMETHEUS_HEALTH"
else
    echo "❌ Prometheus 连接失败：$PROMETHEUS_HEALTH"
    echo ""
    echo "可能的原因："
    echo "  1. Prometheus 配置文件中使用了错误的主机地址"
    echo "  2. admin-api 服务未启动或端口不对"
    echo "  3. 防火墙阻止了连接"
    echo ""
    echo "解决方案："
    echo "  1. 检查 docker/observability/prometheus.yml 配置"
    echo "  2. 运行: ./restart-prometheus.sh"
    echo ""
    read -p "是否继续检查其他项？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo ""
echo ""

# 1. 检查 Actuator 端点是否暴露自定义指标
echo "1. 检查 /actuator/prometheus 端点中的自定义指标..."
echo "-----------------------------------"
CUSTOM_METRICS=$(curl -s "http://localhost:8080/actuator/prometheus" | grep -E "^(api_calls_total|api_response_time|api_errors_total|api_active_requests)" | head -10)

if [ -z "$CUSTOM_METRICS" ]; then
    echo "❌ 未找到自定义指标！请确保："
    echo "   1. admin-api 服务已启动"
    echo "   2. ApiMetricsAspect 切面已生效"
    echo "   3. 至少发起过一次 API 请求"
    echo ""
    echo "尝试发起一个测试请求..."
    curl -s "http://localhost:8080/actuator/health" > /dev/null
    sleep 2
    echo ""
    echo "再次检查指标..."
    CUSTOM_METRICS=$(curl -s "http://localhost:8080/actuator/prometheus" | grep -E "^(api_calls_total|api_response_time|api_errors_total|api_active_requests)" | head -10)
fi

if [ -z "$CUSTOM_METRICS" ]; then
    echo "❌ 仍然未找到自定义指标！"
    echo ""
    echo "调试信息："
    echo "- 检查 Spring AOP 是否启用："
    curl -s "http://localhost:8080/actuator/beans" | grep -i "aspect" | head -5
else
    echo "✅ 找到自定义指标："
    echo "$CUSTOM_METRICS"
fi

echo ""
echo ""

# 2. 检查 Prometheus 中的指标
echo "2. 检查 Prometheus 中的自定义指标..."
echo "-----------------------------------"

METRICS_IN_PROMETHEUS=$(curl -s "http://141.98.196.113:9190/api/v1/label/__name__/values" | grep -o '"api_[^"]*"' | head -10)

if [ -z "$METRICS_IN_PROMETHEUS" ]; then
    echo "❌ Prometheus 中未找到自定义指标！"
    echo "   可能原因："
    echo "   1. Prometheus 还未抓取到新指标（等待下一个抓取周期）"
    echo "   2. Prometheus 配置中未添加 admin-api 的 scrape target"
    echo ""
    echo "   建议操作："
    echo "   - 等待 30-60 秒后重新运行此脚本"
    echo "   - 检查 Prometheus 配置文件中的 scrape_configs"
else
    echo "✅ Prometheus 中找到的自定义指标："
    echo "$METRICS_IN_PROMETHEUS"
fi

echo ""
echo ""

# 3. 测试 MetricsQueryService 查询
echo "3. 测试 getSystemOverview 方法..."
echo "-----------------------------------"

# 这里需要调用 admin-api 的相应接口
# 如果有相应的 REST 接口，可以取消注释以下代码
# OVERVIEW=$(curl -s "http://localhost:8080/api/observability/metrics/overview")
# echo "$OVERVIEW"

echo "✅ 请手动调用 MetricsQueryService.getSystemOverview() 方法进行测试"

echo ""
echo "========================================="
echo "测试完成"
echo "========================================="
