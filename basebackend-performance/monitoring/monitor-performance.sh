#!/bin/bash
# =====================================================================
# 性能监控脚本
# 创建时间: 2025-11-15
# 描述: 实时监控系统性能指标
# =====================================================================

set -e

echo "======================================="
echo "微服务性能监控系统"
echo "======================================="

# 服务配置
declare -A SERVICES
SERVICES=(
    ["user-service"]="8081"
    ["auth-service"]="8082"
    ["dict-service"]="8083"
    ["dept-service"]="8084"
    ["log-service"]="8085"
    ["menu-service"]="8088"
    ["monitor-service"]="8089"
    ["notification-service"]="8090"
    ["profile-service"]="8091"
)

# 监控间隔 (秒)
MONITOR_INTERVAL=10

# 创建监控结果目录
mkdir -p monitoring-results

echo "开始监控..."
echo "监控间隔: ${MONITOR_INTERVAL}秒"
echo "监控服务: ${#SERVICES[@]}个"
echo ""

# 监控循环
while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    echo "======================================="
    echo "监控时间: $TIMESTAMP"
    echo "======================================="

    # 系统资源监控
    echo "系统资源:"
    CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    MEM_TOTAL=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    MEM_USED=$(free -m | awk 'NR==2{printf "%.0f", $3}')
    MEM_USAGE=$(echo "scale=2; $MEM_USED * 100 / $MEM_TOTAL" | bc)
    DISK_USAGE=$(df -h / | awk 'NR==2{print $5}' | cut -d'%' -f1)

    echo "  CPU 使用率: ${CPU_USAGE}%"
    echo "  内存使用率: ${MEM_USAGE}% (${MEM_USED}MB/${MEM_TOTAL}MB)"
    echo "  磁盘使用率: ${DISK_USAGE}%"
    echo ""

    # 服务监控
    echo "服务状态:"
    for service in "${!SERVICES[@]}"; do
        port=${SERVICES[$service]}

        # 检查端口是否可访问
        if curl -f -s "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
            # 获取健康状态
            health_status=$(curl -s "http://localhost:${port}/actuator/health" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")

            # 获取响应时间
            response_time=$(curl -o /dev/null -s -w "%{time_total}" "http://localhost:${port}/actuator/health" 2>/dev/null || echo "999")

            # 获取 JVM 指标 (如果可用)
            jvm_memory=$(curl -s "http://localhost:${port}/actuator/metrics/jvm.memory.used" 2>/dev/null | jq -r '.measurements[0].value' 2>/dev/null || echo "N/A")

            # 记录监控数据
            echo "$TIMESTAMP,$service,$health_status,$response_time,$jvm_memory,$CPU_USAGE,$MEM_USAGE" >> "monitoring-results/${service}-monitor.csv"

            # 显示状态
            if [ "$health_status" = "UP" ]; then
                echo "  ✅ $service (端口:$port) - 响应时间: ${response_time}s"
            else
                echo "  ❌ $service (端口:$port) - 状态: $health_status"
            fi
        else
            echo "  ❌ $service (端口:$port) - 不可访问"
        fi
    done
    echo ""

    # Redis 监控
    if command -v redis-cli &> /dev/null; then
        echo "Redis 状态:"
        redis_info=$(redis-cli info 2>/dev/null || echo "")
        if [ -n "$redis_info" ]; then
            redis_connected_clients=$(echo "$redis_info" | grep "connected_clients:" | cut -d':' -f2)
            redis_used_memory=$(echo "$redis_info" | grep "used_memory_human:" | cut -d':' -f2)
            redis_keyspace_hits=$(echo "$redis_info" | grep "keyspace_hits:" | cut -d':' -f2)
            redis_keyspace_misses=$(echo "$redis_info" | grep "keyspace_misses:" | cut -d':' -f2)

            echo "  连接客户端: $redis_connected_clients"
            echo "  内存使用: $redis_used_memory"
            echo "  命中率: $redis_keyspace_hits / $(($redis_keyspace_hits + $redis_keyspace_misses))"
        else
            echo "  ⚠️  Redis 不可访问"
        fi
    fi
    echo ""

    # 生成监控报告
    if [ $(ls monitoring-results/*.csv 2>/dev/null | wc -l) -gt 0 ]; then
        echo "生成监控报告..."

        cat > monitoring-results/performance-dashboard.html << EOF
<!DOCTYPE html>
<html>
<head>
    <title>微服务性能监控仪表板</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        .metrics { display: flex; flex-wrap: wrap; gap: 20px; }
        .metric-card { background: #f5f5f5; padding: 15px; border-radius: 8px; min-width: 300px; }
        .metric-title { font-weight: bold; margin-bottom: 10px; }
        .good { color: green; }
        .warning { color: orange; }
        .error { color: red; }
        table { border-collapse: collapse; width: 100%; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
    </style>
</head>
<body>
    <h1>微服务性能监控仪表板</h1>
    <p>更新时间: $(date '+%Y-%m-%d %H:%M:%S')</p>

    <div class="metrics">
        <div class="metric-card">
            <div class="metric-title">系统资源</div>
            <p>CPU 使用率: ${CPU_USAGE}%</p>
            <p>内存使用率: ${MEM_USAGE}%</p>
            <p>磁盘使用率: ${DISK_USAGE}%</p>
        </div>

        <div class="metric-card">
            <div class="metric-title">服务状态</div>
            <table>
                <tr>
                    <th>服务名称</th>
                    <th>端口</th>
                    <th>状态</th>
                    <th>响应时间</th>
                </tr>
EOF

        for service in "${!SERVICES[@]}"; do
            port=${SERVICES[$service]}
            csv_file="monitoring-results/${service}-monitor.csv"

            if [ -f "$csv_file" ]; then
                # 获取最新的监控数据
                latest=$(tail -n 1 "$csv_file")
                IFS=',' read -r timestamp svc health response_time jvm cpu mem <<< "$latest"

                status_class="good"
                if [ "$health" != "UP" ]; then
                    status_class="error"
                elif (( $(echo "$response_time > 1.0" | bc -l) )); then
                    status_class="warning"
                fi

                echo "                <tr>
                    <td>$service</td>
                    <td>$port</td>
                    <td class=\"$status_class\">$health</td>
                    <td>${response_time}s</td>
                </tr>" >> monitoring-results/performance-dashboard.html
            fi
        done

        cat >> monitoring-results/performance-dashboard.html << 'EOF'
            </table>
        </div>
    </div>
</body>
</html>
EOF

        echo "  仪表板已更新: monitoring-results/performance-dashboard.html"
    fi

    echo "======================================="
    echo "等待 ${MONITOR_INTERVAL} 秒后进行下一轮监控..."
    echo "======================================="
    echo ""

    sleep $MONITOR_INTERVAL
done
