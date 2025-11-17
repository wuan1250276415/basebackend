#!/bin/bash
# =====================================================================
# 稳定性测试脚本
# 创建时间: 2025-11-15
# 描述: 对所有微服务进行长时间稳定性测试
# =====================================================================

set -e

echo "======================================="
echo "微服务稳定性测试"
echo "======================================="

# 服务配置
declare -A SERVICES
SERVICES=(
    ["user-service"]="8081:/api/users"
    ["auth-service"]="8082:/api/auth"
    ["dict-service"]="8083:/api/dict"
    ["dept-service"]="8084:/api/dept"
)

# 测试参数
TEST_DURATION=3600  # 1小时
CHECK_INTERVAL=60   # 60秒检查一次
CONCURRENCY=10
REQUESTS=100

# 创建结果目录
mkdir -p stability-results

echo "开始稳定性测试..."
echo "测试持续时间: ${TEST_DURATION}秒 (1小时)"
echo "检查间隔: ${CHECK_INTERVAL}秒"
echo ""

# 记录测试开始时间
START_TIME=$(date +%s)
END_TIME=$((START_TIME + TEST_DURATION))

# 测试循环
while [ $(date +%s) -lt $END_TIME ]; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    REMAINING=$((END_TIME - CURRENT_TIME))

    echo "======================================="
    echo "稳定性测试进行中..."
    echo "已运行: ${ELAPSED}秒"
    echo "剩余: ${REMAINING}秒"
    echo "======================================="

    # 测试每个服务
    for service in "${!SERVICES[@]}"; do
        IFS=':' read -r port endpoint <<< "${SERVICES[$service]}"
        url="http://localhost:${port}${endpoint}"

        # 执行短期压力测试
        echo "测试 $service..."
        response_time=$(curl -o /dev/null -s -w "%{time_total}" -X GET "$url" || echo "999")
        status_code=$(curl -o /dev/null -s -w "%{http_code}" -X GET "$url" || echo "000")

        # 记录结果
        timestamp=$(date '+%Y-%m-%d %H:%M:%S')
        echo "$timestamp,$service,$response_time,$status_code" >> "stability-results/${service}-stability.csv"

        # 检查响应时间
        if (( $(echo "$response_time > 1.0" | bc -l) )); then
            echo "⚠️  $service 响应时间过长: ${response_time}s"
        fi

        # 检查 HTTP 状态码
        if [ "$status_code" != "200" ]; then
            echo "❌ $service 响应异常: HTTP $status_code"
        fi
    done

    # 等待检查间隔
    sleep $CHECK_INTERVAL
done

echo "======================================="
echo "稳定性测试完成！"
echo "======================================="

# 生成稳定性报告
echo "生成稳定性报告..."

cat > stability-results/stability-summary.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>微服务稳定性测试报告</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .summary { background: #f0f0f0; padding: 15px; margin: 20px 0; }
        .chart { margin: 20px 0; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        .good { color: green; }
        .warning { color: orange; }
        .error { color: red; }
    </style>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <h1>微服务稳定性测试报告</h1>
    <div class="summary">
        <h2>测试概要</h2>
        <p>测试时间: $(date)</p>
        <p>测试时长: 1小时</p>
        <p>测试服务: 4个核心服务</p>
        <p>检查间隔: 60秒</p>
    </div>
    <h2>稳定性指标</h2>
EOF

for service in "${!SERVICES[@]}"; do
    csv_file="stability-results/${service}-stability.csv"
    if [ -f "$csv_file" ]; then
        total_requests=$(wc -l < "$csv_file")
        failed_requests=$(grep -v ",200$" "$csv_file" | wc -l)
        success_rate=$(echo "scale=2; ($total_requests - $failed_requests) * 100 / $total_requests" | bc)
        avg_response_time=$(awk -F',' '{sum+=$3} END {print sum/NR}' "$csv_file")
        max_response_time=$(awk -F',' '{if($3>max) max=$3} END {print max}' "$csv_file")

        echo "    <h3>$service</h3>
        <table>
            <tr>
                <th>指标</th>
                <th>值</th>
            </tr>
            <tr>
                <td>总请求数</td>
                <td>$total_requests</td>
            </tr>
            <tr>
                <td>成功请求数</td>
                <td>$((total_requests - failed_requests))</td>
            </tr>
            <tr>
                <td>失败请求数</td>
                <td class=\"$([ $failed_requests -gt 0 ] && echo 'error' || echo 'good')\">$failed_requests</td>
            </tr>
            <tr>
                <td>成功率</td>
                <td class=\"$([ $(echo "$success_rate > 99" | bc) -eq 1 ] && echo 'good' || echo 'warning')\">$success_rate%</td>
            </tr>
            <tr>
                <td>平均响应时间</td>
                <td>${avg_response_time}s</td>
            </tr>
            <tr>
                <td>最大响应时间</td>
                <td>${max_response_time}s</td>
            </tr>
        </table>" >> stability-results/stability-summary.html
    fi
done

cat >> stability-results/stability-summary.html << 'EOF'
</body>
</html>
EOF

echo "稳定性报告已生成: stability-results/stability-summary.html"
echo ""
echo "结果文件保存在 stability-results/ 目录"
echo "======================================="
