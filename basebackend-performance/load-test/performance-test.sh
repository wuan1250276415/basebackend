#!/bin/bash
# =====================================================================
# 性能测试脚本
# 创建时间: 2025-11-15
# 描述: 对所有微服务进行压力测试
# =====================================================================

set -e

echo "======================================="
echo "微服务性能测试"
echo "======================================="

# 服务配置
declare -A SERVICES
SERVICES=(
    ["user-service"]="8081:/api/users"
    ["auth-service"]="8082:/api/auth"
    ["dict-service"]="8083:/api/dict"
    ["dept-service"]="8084:/api/dept"
    ["log-service"]="8085:/api/log"
    ["menu-service"]="8088:/api/menu"
    ["monitor-service"]="8089:/api/monitor"
    ["notification-service"]="8090:/api/notification"
    ["profile-service"]="8091:/api/profile"
)

# 测试参数
CONCURRENCY=100
REQUESTS=1000
TEST_DURATION=60  # 秒

# 创建结果目录
mkdir -p performance-results

echo "开始性能测试..."
echo "并发数: $CONCURRENCY"
echo "总请求数: $REQUESTS"
echo "测试持续时间: ${TEST_DURATION}秒"
echo ""

# 测试每个服务
for service in "${!SERVICES[@]}"; do
    IFS=':' read -r port endpoint <<< "${SERVICES[$service]}"
    url="http://localhost:${port}${endpoint}"

    echo "======================================="
    echo "测试服务: $service"
    echo "URL: $url"
    echo "======================================="

    # 检查服务是否可访问
    if ! curl -f -s "http://localhost:${port}/actuator/health" > /dev/null; then
        echo "❌ 服务不可访问，跳过测试"
        continue
    fi

    # 执行压力测试
    echo "执行压力测试..."
    ab -n $REQUESTS -c $CONCURRENCY -t $TEST_DURATION -g "performance-results/${service}-performance.tsv" "$url" > "performance-results/${service}-ab-result.txt" 2>&1 || true

    # 测试健康检查端点
    echo "测试健康检查端点..."
    ab -n 1000 -c 50 -g "performance-results/${service}-health.tsv" "http://localhost:${port}/actuator/health" > "performance-results/${service}-health-ab-result.txt" 2>&1 || true

    # 测试 API 文档端点
    echo "测试 API 文档端点..."
    ab -n 500 -c 20 -g "performance-results/${service}-api-docs.tsv" "http://localhost:${port}/v3/api-docs" > "performance-results/${service}-api-docs-result.txt" 2>&1 || true

    echo "✅ $service 测试完成"
    echo ""
done

echo "======================================="
echo "性能测试完成！"
echo "======================================="
echo "结果文件保存在 performance-results/ 目录"
echo ""

# 生成性能报告
echo "生成性能报告..."
cat > performance-results/performance-summary.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>微服务性能测试报告</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        .good { color: green; font-weight: bold; }
        .warning { color: orange; font-weight: bold; }
        .error { color: red; font-weight: bold; }
    </style>
</head>
<body>
    <h1>微服务性能测试报告</h1>
    <p>测试时间: $(date)</p>
    <h2>服务性能指标</h2>
    <table>
        <tr>
            <th>服务名称</th>
            <th>端口</th>
            <th>响应时间 (ms)</th>
            <th>吞吐量 (req/s)</th>
            <th>错误率 (%)</th>
            <th>状态</th>
        </tr>
EOF

for service in "${!SERVICES[@]}"; do
    IFS=':' read -r port endpoint <<< "${SERVICES[$service]}"
    result_file="performance-results/${service}-ab-result.txt"

    if [ -f "$result_file" ]; then
        # 提取测试结果
        time_per_req=$(grep "Time per request:" "$result_file" | head -1 | awk '{print $4}')
        requests_per_sec=$(grep "Requests per second:" "$result_file" | awk '{print $4}')
        failed=$(grep "Failed requests:" "$result_file" | awk '{print $3}')
        failed_pct=$(echo "scale=2; $failed * 100 / 1000" | bc)

        status="good"
        if (( $(echo "$time_per_req > 200" | bc -l) )); then
            status="warning"
        fi
        if (( $(echo "$time_per_req > 500" | bc -l) )); then
            status="error"
        fi

        echo "        <tr>
            <td>$service</td>
            <td>$port</td>
            <td>$time_per_req</td>
            <td>$requests_per_sec</td>
            <td>$failed_pct</td>
            <td class=\"$status\">$status</td>
        </tr>" >> performance-results/performance-summary.html
    fi
done

cat >> performance-results/performance-summary.html << 'EOF'
    </table>
    <h2>测试说明</h2>
    <ul>
        <li><span class="good">good</span>: 性能优秀 (响应时间 < 200ms)</li>
        <li><span class="warning">warning</span>: 性能一般 (响应时间 200-500ms)</li>
        <li><span class="error">error</span>: 性能较差 (响应时间 > 500ms)</li>
    </ul>
</body>
</html>
EOF

echo "性能报告已生成: performance-results/performance-summary.html"
echo "======================================="
