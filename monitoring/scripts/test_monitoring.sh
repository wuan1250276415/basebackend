#!/bin/bash
# 监控系统测试脚本
# 验证 Prometheus、Grafana、AlertManager 是否正常运行

set -e

echo "================================"
echo "BaseBackend 监控系统测试"
echo "================================"
echo ""

# 检查服务健康状态
check_service() {
    local name=$1
    local url=$2
    local expected_status=$3

    echo -n "检查 $name... "

    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "$expected_status"; then
        echo "✅ OK"
        return 0
    else
        echo "❌ FAIL"
        return 1
    fi
}

# 检查Prometheus
echo "1. Prometheus 健康检查"
echo "----------------------------------------"
check_service "Prometheus" "http://localhost:9090/-/healthy" "200" || true
check_service "Prometheus Metrics" "http://localhost:9090/metrics" "200" || true
echo ""

# 检查Grafana
echo "2. Grafana 健康检查"
echo "----------------------------------------"
check_service "Grafana" "http://localhost:3000/api/health" "200" || true
echo ""

# 检查AlertManager
echo "3. AlertManager 健康检查"
echo "----------------------------------------"
check_service "AlertManager" "http://localhost:9093/-/healthy" "200" || true
check_service "AlertManager API" "http://localhost:9093/api/v1/status" "200" || true
echo ""

# 测试Prometheus数据采集
echo "4. Prometheus 数据采集测试"
echo "----------------------------------------"

# 检查Prometheus配置
echo -n "检查Prometheus配置文件... "
if [ -f "monitoring/prometheus/prometheus.yml" ]; then
    echo "✅ 配置文件存在"
else
    echo "❌ 配置文件不存在"
fi

# 检查告警规则
echo -n "检查告警规则文件... "
if [ -f "monitoring/prometheus/alert_rules.yml" ]; then
    echo "✅ 告警规则文件存在"
else
    echo "❌ 告警规则文件不存在"
fi

# 验证Prometheus配置文件语法
echo -n "验证Prometheus配置语法... "
if docker run --rm -v "$(pwd)/monitoring/prometheus:/prometheus" prom/prometheus:v2.45.0 promtool check config /prometheus/prometheus.yml > /dev/null 2>&1; then
    echo "✅ 配置语法正确"
else
    echo "⚠️ 配置语法可能存在问题"
fi

# 验证告警规则语法
echo -n "验证告警规则语法... "
if docker run --rm -v "$(pwd)/monitoring/prometheus:/prometheus" prom/prometheus:v2.45.0 promtool check rules /prometheus/alert_rules.yml > /dev/null 2>&1; then
    echo "✅ 告警规则语法正确"
else
    echo "⚠️ 告警规则语法可能存在问题"
fi
echo ""

# 测试Grafana仪表板
echo "5. Grafana 仪表板测试"
echo "----------------------------------------"

# 检查仪表板配置
echo -n "检查仪表板配置文件... "
if [ -f "monitoring/grafana/provisioning/dashboards/dashboard.yml" ]; then
    echo "✅ 仪表板配置存在"
else
    echo "❌ 仪表板配置不存在"
fi

# 检查数据源配置
echo -n "检查数据源配置文件... "
if [ -f "monitoring/grafana/provisioning/datasources/prometheus.yml" ]; then
    echo "✅ 数据源配置存在"
else
    echo "❌ 数据源配置不存在"
fi

# 检查仪表板JSON
echo -n "检查仪表板JSON文件... "
if [ -f "monitoring/grafana/dashboards/dag_engine_overview.json" ]; then
    echo "✅ 仪表板JSON存在"
else
    echo "❌ 仪表板JSON不存在"
fi
echo ""

# 测试AlertManager配置
echo "6. AlertManager 配置测试"
echo "----------------------------------------"

# 检查AlertManager配置
echo -n "检查AlertManager配置文件... "
if [ -f "monitoring/alertmanager/alertmanager.yml" ]; then
    echo "✅ AlertManager配置文件存在"
else
    echo "❌ AlertManager配置文件不存在"
fi

# 验证AlertManager配置文件语法
echo -n "验证AlertManager配置语法... "
if docker run --rm -v "$(pwd)/monitoring/alertmanager:/alertmanager" prom/alertmanager:v0.25.0 amtool config check /alertmanager/alertmanager.yml > /dev/null 2>&1; then
    echo "✅ AlertManager配置语法正确"
else
    echo "⚠️ AlertManager配置语法可能存在问题"
fi
echo ""

# 检查Docker容器状态
echo "7. Docker 容器状态检查"
echo "----------------------------------------"

# 检查Prometheus容器
echo -n "Prometheus容器... "
if docker ps --filter "name=basebackend-prometheus" --filter "status=running" | grep -q "basebackend-prometheus"; then
    echo "✅ 运行中"
else
    echo "❌ 未运行"
fi

# 检查Grafana容器
echo -n "Grafana容器... "
if docker ps --filter "name=basebackend-grafana" --filter "status=running" | grep -q "basebackend-grafana"; then
    echo "✅ 运行中"
else
    echo "❌ 未运行"
fi

# 检查AlertManager容器
echo -n "AlertManager容器... "
if docker ps --filter "name=basebackend-alertmanager" --filter "status=running" | grep -q "basebackend-alertmanager"; then
    echo "✅ 运行中"
else
    echo "❌ 未运行"
fi
echo ""

# 显示访问地址
echo "================================"
echo "📊 监控系统访问地址"
echo "================================"
echo ""
echo "Prometheus:"
echo "  - Web UI: http://localhost:9090"
echo "  - 状态页面: http://localhost:9090/-/healthy"
echo "  - 告警规则: http://localhost:9090/alerts"
echo ""
echo "Grafana:"
echo "  - Web UI: http://localhost:3000"
echo "  - 用户名: admin"
echo "  - 密码: admin123"
echo "  - 状态页面: http://localhost:3000/api/health"
echo ""
echo "AlertManager:"
echo "  - Web UI: http://localhost:9093"
echo "  - 状态页面: http://localhost:9093/-/healthy"
echo "  - API: http://localhost:9093/api/v1/status"
echo ""

# 显示告警规则统计
echo "================================"
echo "📊 告警规则统计"
echo "================================"
echo ""

# 统计各组件告警规则数量
dag_engine_count=$(grep -c "DAGTopologicalSort\|LargeDAG\|ConcurrentDAG" monitoring/prometheus/alert_rules.yml || echo "0")
retry_count=$(grep -c "RetrySuccessRate\|RetryAttempts\|RetryDelay" monitoring/prometheus/alert_rules.yml || echo "0")
system_count=$(grep -c "CPUUsage\|MemoryUsage\|ThreadPool" monitoring/prometheus/alert_rules.yml || echo "0")
workflow_count=$(grep -c "WorkflowFailure\|WorkflowTimeout\|ActiveWorkflows" monitoring/prometheus/alert_rules.yml || echo "0")
app_count=$(grep -c "ApplicationDown\|ApplicationResponseTime\|HTTP5xx" monitoring/prometheus/alert_rules.yml || echo "0")

echo "DAG 引擎告警规则: $dag_engine_count 条"
echo "重试机制告警规则: $retry_count 条"
echo "系统资源告警规则: $system_count 条"
echo "工作流执行告警规则: $workflow_count 条"
echo "应用健康告警规则: $app_count 条"
echo ""
total_count=$((dag_engine_count + retry_count + system_count + workflow_count + app_count))
echo "总告警规则数: $total_count 条"
echo ""

# 测试结论
echo "================================"
if [ $total_count -gt 0 ]; then
    echo "✅ 监控系统配置完成!"
    echo "================================"
    echo ""
    echo "下一步操作:"
    echo "1. 访问 Grafana (http://localhost:3000) 查看监控仪表板"
    echo "2. 在 Prometheus 中查看告警规则和指标"
    echo "3. 在 AlertManager 中管理告警通知"
    echo "4. 根据需要修改告警阈值和通知方式"
    echo ""
else
    echo "❌ 监控系统配置可能存在问题"
    echo "================================"
    echo "请检查配置文件是否正确"
    echo ""
fi

echo "================================"
echo "测试完成"
echo "================================"
