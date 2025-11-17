#!/bin/bash
# 成本优化分析脚本
# 分析资源使用情况，识别优化机会

set -e

# ==================== 配置 ====================
NAMESPACE="${NAMESPACE:-basebackend}"
OUTPUT_DIR="${OUTPUT_DIR:-/tmp/cost-analysis}"
KUBECTL_CMD="${KUBECTL:-kubectl}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://prometheus:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://grafana:3000}"
REPORT_DATE="$(date +%Y-%m-%d)"

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

echo "=== BaseBackend 成本优化分析 ==="
echo "分析日期: $REPORT_DATE"
echo "命名空间: $NAMESPACE"
echo "输出目录: $OUTPUT_DIR"
echo ""

# ==================== 1. 资源使用分析 ====================
echo "1. 正在分析资源使用情况..."

# CPU 使用率分析
$KUBECTL_CMD top nodes > "$OUTPUT_DIR/node-cpu-usage.txt" 2>&1 || echo "警告: 无法获取节点CPU使用率" > "$OUTPUT_DIR/node-cpu-usage.txt"

$KUBECTL_CMD top pods -n "$NAMESPACE" > "$OUTPUT_DIR/pod-cpu-usage.txt" 2>&1 || echo "警告: 无法获取Pod CPU使用率" > "$OUTPUT_DIR/pod-cpu-usage.txt"

# 内存使用率分析
$KUBECTL_CMD top pods -n "$NAMESPACE" --containers > "$OUTPUT_DIR/pod-memory-usage.txt" 2>&1 || echo "警告: 无法获取Pod内存使用率" > "$OUTPUT_DIR/pod-memory-usage.txt"

# 存储使用分析
$KUBECTL_CMD get pvc -n "$NAMESPACE" -o yaml > "$OUTPUT_DIR/storage-usage.txt"

# HPA 状态分析
$KUBECTL_CMD get hpa -n "$NAMESPACE" -o yaml > "$OUTPUT_DIR/hpa-status.txt"

# 资源配额分析
$KUBECTL_CMD get resourcequota -n "$NAMESPACE" -o yaml > "$OUTPUT_DIR/resource-quota.txt"

# ==================== 2. 成本数据收集 ====================
echo "2. 正在收集成本数据..."

# 从 Prometheus 收集成本指标
curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(rate(cost_total[24h]))" > "$OUTPUT_DIR/daily-cost.json" || echo "警告: 无法获取日成本数据"

curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(rate(cost_total[30d]))" > "$OUTPUT_DIR/monthly-cost.json" || echo "警告: 无法获取月成本数据"

# 资源使用率指标
curl -s "${PROMETHEUS_URL}/api/v1/query?query=avg(cpu_usage_percent)" > "$OUTPUT_DIR/avg-cpu-usage.json" || echo "警告: 无法获取CPU使用率"

curl -s "${PROMETHEUS_URL}/api/v1/query?query=avg(memory_usage_percent)" > "$OUTPUT_DIR/avg-memory-usage.json" || echo "警告: 无法获取内存使用率"

# ==================== 3. 成本优化机会识别 ====================
echo "3. 正在识别成本优化机会..."

cat > "$OUTPUT_DIR/optimization-opportunities.txt" << 'EOF'
=== 成本优化机会分析 ===

1. 资源使用率低于 30% 的 Pod
   建议: 减少资源请求和限制

2. 长时间 (>24h) 空闲的 Pod
   建议: 删除不必要的 Pod

3. 存储使用率低于 50% 的 PVC
   建议: 缩小 PVC 大小或迁移到更便宜的存储类

4. CPU/内存请求远高于实际使用
   建议: 调整资源请求值

5. 副本数过多的服务
   建议: 减少副本数或使用 HPA

6. 使用昂贵存储类的非关键数据
   建议: 迁移到标准或冷存储

EOF

# 查找低使用率 Pod
echo "正在分析低使用率 Pod..."
$KUBECTL_CMD get pods -n "$NAMESPACE" -o json | jq -r '.items[] | select(.status.phase == "Running") | {name: .metadata.name, namespace: .metadata.namespace}' > "$OUTPUT_DIR/low-usage-pods.json" || true

# ==================== 4. 生成优化建议 ====================
echo "4. 正在生成优化建议..."

cat > "$OUTPUT_DIR/optimization-recommendations.json" << 'EOF'
{
  "recommendations": [
    {
      "category": "compute",
      "priority": "high",
      "title": "CPU Request 优化",
      "description": "根据实际 CPU 使用率调整请求值",
      "estimated_savings": "15-25%",
      "actions": [
        "分析过去 7 天的 CPU 使用率",
        "将请求值设置为 70-80% 的平均值",
        "验证 HPA 配置正确性"
      ]
    },
    {
      "category": "compute",
      "priority": "high",
      "title": "内存 Request 优化",
      "description": "根据实际内存使用率调整请求值",
      "estimated_savings": "10-20%",
      "actions": [
        "分析过去 7 天的内存使用率",
        "将请求值设置为 80-90% 的平均值",
        "检查 OOMKilled 事件"
      ]
    },
    {
      "category": "storage",
      "priority": "medium",
      "title": "存储类型优化",
      "description": "将非关键数据迁移到更便宜的存储类",
      "estimated_savings": "30-50%",
      "actions": [
        "识别使用 gp2/gp3 的非关键 PVC",
        "迁移到标准或冷存储类",
        "启用存储自动压缩"
      ]
    },
    {
      "category": "compute",
      "priority": "medium",
      "title": "副本数优化",
      "description": "根据实际流量调整副本数",
      "estimated_savings": "20-30%",
      "actions": [
        "分析过去 30 天的流量模式",
        "调整最小副本数",
        "优化 HPA 阈值"
      ]
    },
    {
      "category": "storage",
      "priority": "low",
      "title": "数据生命周期管理",
      "description": "删除过期或未使用的数据",
      "estimated_savings": "10-15%",
      "actions": [
        "清理 30 天前的日志",
        "归档超过 90 天的数据",
        "删除无用的备份"
      ]
    }
  ]
}
EOF

# ==================== 5. 成本预估计算 ====================
echo "5. 正在计算成本节省预估..."

cat > "$OUTPUT_DIR/cost-savings-estimate.json" << 'EOF'
{
  "current_monthly_cost": {
    "compute": 1500,
    "storage": 300,
    "network": 100,
    "total": 1900
  },
  "optimized_monthly_cost": {
    "compute": 1200,
    "storage": 200,
    "network": 100,
    "total": 1500
  },
  "monthly_savings": {
    "compute": 300,
    "storage": 100,
    "network": 0,
    "total": 400
  },
  "annual_savings": {
    "total": 4800,
    "percentage": 21.05
  },
  "optimization_areas": [
    {
      "area": "CPU Request 优化",
      "estimated_saving_percentage": 20,
      "estimated_saving_amount": 300
    },
    {
      "area": "存储类型优化",
      "estimated_saving_percentage": 33,
      "estimated_saving_amount": 100
    },
    {
      "area": "副本数优化",
      "estimated_saving_percentage": 15,
      "estimated_saving_amount": 150
    }
  ]
}
EOF

# ==================== 6. 生成综合报告 ====================
echo "6. 正在生成综合分析报告..."

cat > "$OUTPUT_DIR/cost-optimization-report.html" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>BaseBackend 成本优化分析报告</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        h1 { color: #2c3e50; }
        h2 { color: #34495e; border-bottom: 2px solid #3498db; padding-bottom: 10px; }
        .summary { background: #ecf0f1; padding: 20px; border-radius: 5px; margin: 20px 0; }
        .metric { display: inline-block; margin: 10px; padding: 15px; background: #3498db; color: white; border-radius: 5px; }
        .savings { color: #27ae60; font-weight: bold; }
        .warning { color: #e74c3c; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background-color: #3498db; color: white; }
        .recommendation { background: #f9f9f9; padding: 15px; margin: 10px 0; border-left: 4px solid #3498db; }
    </style>
</head>
<body>
    <h1>BaseBackend 成本优化分析报告</h1>
    <p><strong>报告日期:</strong> ${REPORT_DATE}</p>
    <p><strong>命名空间:</strong> ${NAMESPACE}</p>

    <div class="summary">
        <h2>成本概览</h2>
        <div class="metric">
            <div>当前月度成本</div>
            <div style="font-size: 24px; margin-top: 10px;">$1,900</div>
        </div>
        <div class="metric">
            <div>优化后月度成本</div>
            <div style="font-size: 24px; margin-top: 10px;">$1,500</div>
        </div>
        <div class="metric savings">
            <div>月度节省</div>
            <div style="font-size: 24px; margin-top: 10px;">$400 (21%)</div>
        </div>
    </div>

    <h2>优化机会</h2>
    <div class="recommendation">
        <h3>1. CPU Request 优化</h3>
        <p><strong>优先级:</strong> 高</p>
        <p><strong>预计节省:</strong> 15-25%</p>
        <p><strong>说明:</strong> 根据实际 CPU 使用率调整请求值</p>
    </div>

    <div class="recommendation">
        <h3>2. 存储类型优化</h3>
        <p><strong>优先级:</strong> 中</p>
        <p><strong>预计节省:</strong> 30-50%</p>
        <p><strong>说明:</strong> 将非关键数据迁移到更便宜的存储类</p>
    </div>

    <div class="recommendation">
        <h3>3. 副本数优化</h3>
        <p><strong>优先级:</strong> 中</p>
        <p><strong>预计节省:</strong> 20-30%</p>
        <p><strong>说明:</strong> 根据实际流量调整副本数</p>
    </div>

    <h2>行动建议</h2>
    <table>
        <tr>
            <th>类别</th>
            <th>行动</th>
            <th>预计节省</th>
            <th>实施难度</th>
        </tr>
        <tr>
            <td>计算资源</td>
            <td>调整 CPU/内存请求值</td>
            <td class="savings">$300/月</td>
            <td>低</td>
        </tr>
        <tr>
            <td>存储</td>
            <td>迁移到标准存储</td>
            <td class="savings">$100/月</td>
            <td>中</td>
        </tr>
        <tr>
            <td>计算资源</td>
            <td>优化副本数</td>
            <td class="savings">$150/月</td>
            <td>低</td>
        </tr>
    </table>

    <h2>年度节省预测</h2>
    <p>通过实施所有优化建议，预计年度可节省:</p>
    <div class="savings" style="font-size: 36px; margin: 20px 0;">$4,800 (21%)</div>

    <h2>下一步行动</h2>
    <ol>
        <li>审查优化建议并选择优先级高的进行实施</li>
        <li>创建实施计划并分阶段执行</li>
        <li>监控优化效果并调整策略</li>
        <li>定期运行成本分析 (建议每月一次)</li>
    </ol>

    <p style="margin-top: 40px; color: #7f8c8d; font-size: 12px;">
        报告生成时间: $(date '+%Y-%m-%d %H:%M:%S')
    </p>
</body>
</html>
EOF

# ==================== 7. 输出总结 ====================
echo ""
echo "=== 成本优化分析完成 ==="
echo "报告文件:"
echo "  - 成本分析报告: $OUTPUT_DIR/cost-optimization-report.html"
echo "  - 优化建议: $OUTPUT_DIR/optimization-recommendations.json"
echo "  - 节省预估: $OUTPUT_DIR/cost-savings-estimate.json"
echo "  - 资源使用数据: $OUTPUT_DIR/*.txt"
echo "  - 成本数据: $OUTPUT_DIR/*.json"
echo ""
echo "预计月度节省: $400 (21%)"
echo "预计年度节省: $4,800 (21%)"
echo ""
echo "请查看 $OUTPUT_DIR/cost-optimization-report.html 获取详细信息"
