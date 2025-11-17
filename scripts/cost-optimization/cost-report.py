#!/usr/bin/env python3
"""
成本报告生成器
生成月度/季度成本分析报告

功能:
1. 收集成本数据
2. 分析成本趋势
3. 识别成本异常
4. 生成可视化报告
5. 发送报告通知
"""

import json
import argparse
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Any
import smtplib
from email.mime.text import MimeText
from email.mime.multipart import MimeMultipart
from jinja2 import Template
import sys

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/tmp/cost-report.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class CostReporter:
    """成本报告生成器"""

    def __init__(self, report_type: str = "monthly", namespace: str = "basebackend"):
        self.report_type = report_type
        self.namespace = namespace
        self.start_date = self._get_start_date()
        self.end_date = datetime.now()

    def _get_start_date(self) -> datetime:
        """根据报告类型获取开始日期"""
        if self.report_type == "monthly":
            return (datetime.now().replace(day=1) - timedelta(days=1)).replace(day=1)
        elif self.report_type == "quarterly":
            current_quarter = (datetime.now().month - 1) // 3
            quarter_start_month = current_quarter * 3 + 1
            return datetime.now().replace(month=quarter_start_month, day=1)
        elif self.report_type == "weekly":
            return datetime.now() - timedelta(days=7)
        else:
            return datetime.now() - timedelta(days=30)

    def collect_cost_data(self) -> Dict[str, Any]:
        """收集成本数据"""
        logger.info("正在收集成本数据...")

        # 模拟从 Prometheus/云账单获取数据
        cost_data = {
            "total_cost": {
                "current_period": 1900,
                "previous_period": 2100,
                "change": -200,
                "change_percentage": -9.52
            },
            "cost_breakdown": {
                "compute": {
                    "amount": 1500,
                    "percentage": 78.95,
                    "details": {
                        "cpu": 900,
                        "memory": 400,
                        "storage": 200
                    }
                },
                "storage": {
                    "amount": 300,
                    "percentage": 15.79,
                    "details": {
                        "hot_storage": 200,
                        "warm_storage": 80,
                        "cold_storage": 20
                    }
                },
                "network": {
                    "amount": 100,
                    "percentage": 5.26,
                    "details": {
                        "egress": 70,
                        "ingress": 30
                    }
                }
            },
            "service_costs": [
                {"service": "basebackend-admin-api", "cost": 600, "percentage": 31.58},
                {"service": "basebackend-auth-service", "cost": 500, "percentage": 26.32},
                {"service": "basebackend-user-service", "cost": 400, "percentage": 21.05},
                {"service": "basebackend-gateway", "cost": 300, "percentage": 15.79},
                {"service": "database", "cost": 100, "percentage": 5.26}
            ]
        }

        return cost_data

    def analyze_cost_trends(self, cost_data: Dict) -> Dict[str, Any]:
        """分析成本趋势"""
        logger.info("正在分析成本趋势...")

        # 模拟成本趋势分析
        trends = {
            "daily_costs": [
                {"date": "2024-11-01", "cost": 65},
                {"date": "2024-11-02", "cost": 68},
                {"date": "2024-11-03", "cost": 62},
                {"date": "2024-11-04", "cost": 70},
                {"date": "2024-11-05", "cost": 72},
                {"date": "2024-11-06", "cost": 69},
                {"date": "2024-11-07", "cost": 71},
                # ... 更多日期
            ],
            "trend_analysis": {
                "direction": "decreasing",
                "percentage": -9.52,
                "significant_changes": [
                    {"date": "2024-11-04", "change": 12.9, "reason": "增加了副本数"},
                    {"date": "2024-11-05", "change": 2.86, "reason": "流量增加"}
                ]
            },
            "seasonal_patterns": {
                "peak_days": ["Tuesday", "Wednesday", "Thursday"],
                "low_days": ["Saturday", "Sunday"],
                "peak_hours": ["10:00-12:00", "14:00-16:00"]
            }
        }

        return trends

    def identify_cost_anomalies(self, cost_data: Dict) -> List[Dict]:
        """识别成本异常"""
        logger.info("正在识别成本异常...")

        anomalies = [
            {
                "date": "2024-11-04",
                "type": "sudden_increase",
                "severity": "medium",
                "description": "成本突然增加 12.9%",
                "reason": "HPA 扩容增加了副本数",
                "recommendation": "检查流量模式和 HPA 配置"
            },
            {
                "date": "2024-11-06",
                "type": "storage_growth",
                "severity": "low",
                "description": "存储使用量持续增长",
                "reason": "日志数据未及时清理",
                "recommendation": "执行数据清理作业"
            }
        ]

        return anomalies

    def generate_optimization_suggestions(self, cost_data: Dict) -> List[Dict]:
        """生成优化建议"""
        logger.info("正在生成优化建议...")

        suggestions = [
            {
                "category": "compute",
                "priority": "high",
                "title": "CPU Request 优化",
                "description": "当前 CPU 请求值平均高于实际使用 40%",
                "potential_saving": 180,
                "effort": "low",
                "impact": "high",
                "actions": [
                    "分析过去 30 天的 CPU 使用率",
                    "将请求值调整为 70% 的使用率平均值",
                    "监控优化效果"
                ]
            },
            {
                "category": "storage",
                "priority": "medium",
                "title": "存储类型优化",
                "description": "部分非关键数据使用了昂贵的 SSD 存储",
                "potential_saving": 60,
                "effort": "medium",
                "impact": "medium",
                "actions": [
                    "识别非关键数据",
                    "迁移到标准或冷存储",
                    "更新 PVC 配置"
                ]
            },
            {
                "category": "compute",
                "priority": "medium",
                "title": "副本数优化",
                "description": "某些服务的副本数可以减少 1-2 个",
                "potential_saving": 120,
                "effort": "low",
                "impact": "medium",
                "actions": [
                    "分析过去 30 天的流量模式",
                    "调整最小副本数",
                    "优化 HPA 阈值"
                ]
            }
        ]

        return suggestions

    def generate_html_report(self, cost_data: Dict, trends: Dict, anomalies: List, suggestions: List) -> str:
        """生成 HTML 报告"""
        logger.info("正在生成 HTML 报告...")

        html_template = """
<!DOCTYPE html>
<html>
<head>
    <title>BaseBackend 成本分析报告 - {{ report_type | title }} {{ start_date.strftime('%Y-%m') }}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f7fa; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; border-radius: 10px; margin-bottom: 30px; }
        .header h1 { font-size: 36px; margin-bottom: 10px; }
        .header p { font-size: 16px; opacity: 0.9; }
        .card { background: white; border-radius: 10px; padding: 30px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .card h2 { color: #2c3e50; margin-bottom: 20px; border-bottom: 3px solid #667eea; padding-bottom: 10px; }
        .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; }
        .metric-card { background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); padding: 20px; border-radius: 8px; text-align: center; }
        .metric-value { font-size: 36px; font-weight: bold; color: #2c3e50; margin: 10px 0; }
        .metric-label { font-size: 14px; color: #7f8c8d; text-transform: uppercase; }
        .positive { color: #27ae60 !important; }
        .negative { color: #e74c3c !important; }
        .table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        .table th { background: #667eea; color: white; padding: 12px; text-align: left; }
        .table td { padding: 12px; border-bottom: 1px solid #ecf0f1; }
        .table tr:hover { background: #f8f9fa; }
        .priority-high { color: #e74c3c; font-weight: bold; }
        .priority-medium { color: #f39c12; font-weight: bold; }
        .priority-low { color: #27ae60; font-weight: bold; }
        .anomaly { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 10px 0; border-radius: 4px; }
        .suggestion { background: #d4edda; border-left: 4px solid #28a745; padding: 15px; margin: 10px 0; border-radius: 4px; }
        .footer { text-align: center; color: #7f8c8d; margin-top: 30px; padding: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>BaseBackend 成本分析报告</h1>
            <p>{{ report_type | title }} 报告 ({{ start_date.strftime('%Y-%m-%d') }} - {{ end_date.strftime('%Y-%m-%d') }})</p>
        </div>

        <div class="card">
            <h2>成本概览</h2>
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-label">当前周期总成本</div>
                    <div class="metric-value">${{ cost_data.total_cost.current_period }}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">上期成本</div>
                    <div class="metric-value">${{ cost_data.total_cost.previous_period }}</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">成本变化</div>
                    <div class="metric-value {% if cost_data.total_cost.change < 0 %}positive{% else %}negative{% endif %}">
                        ${{ cost_data.total_cost.change }}
                    </div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">变化百分比</div>
                    <div class="metric-value {% if cost_data.total_cost.change_percentage < 0 %}positive{% else %}negative{% endif %}">
                        {{ "%.2f"|format(cost_data.total_cost.change_percentage) }}%
                    </div>
                </div>
            </div>
        </div>

        <div class="card">
            <h2>成本构成</h2>
            <table class="table">
                <thead>
                    <tr>
                        <th>类别</th>
                        <th>金额</th>
                        <th>占比</th>
                        <th>详细信息</th>
                    </tr>
                </thead>
                <tbody>
                    {% for category, data in cost_data.cost_breakdown.items() %}
                    <tr>
                        <td>{{ category | title }}</td>
                        <td>${{ data.amount }}</td>
                        <td>{{ "%.2f"|format(data.percentage) }}%</td>
                        <td>
                            {% for detail, amount in data.details.items() %}
                                {{ detail | title }}: ${{ amount }}<br>
                            {% endfor %}
                        </td>
                    </tr>
                    {% endfor %}
                </tbody>
            </table>
        </div>

        <div class="card">
            <h2>服务成本排行</h2>
            <table class="table">
                <thead>
                    <tr>
                        <th>服务</th>
                        <th>成本</th>
                        <th>占比</th>
                    </tr>
                </thead>
                <tbody>
                    {% for service in cost_data.service_costs %}
                    <tr>
                        <td>{{ service.service }}</td>
                        <td>${{ service.cost }}</td>
                        <td>{{ "%.2f"|format(service.percentage) }}%</td>
                    </tr>
                    {% endfor %}
                </tbody>
            </table>
        </div>

        <div class="card">
            <h2>成本趋势分析</h2>
            <p><strong>趋势方向:</strong>
                <span class="{% if trends.trend_analysis.direction == 'decreasing' %}positive{% else %}negative{% endif %}">
                    {{ trends.trend_analysis.direction | title }}
                </span>
            </p>
            <p><strong>变化百分比:</strong> {{ "%.2f"|format(trends.trend_analysis.percentage) }}%</p>
            <p><strong>高峰日期:</strong> {{ ", ".join(trends.seasonal_patterns.peak_days) }}</p>
            <p><strong>高峰时段:</strong> {{ ", ".join(trends.seasonal_patterns.peak_hours) }}</p>
        </div>

        <div class="card">
            <h2>成本异常</h2>
            {% if anomalies %}
                {% for anomaly in anomalies %}
                <div class="anomaly">
                    <strong>{{ anomaly.date }}</strong> - {{ anomaly.type | title }} ({{ anomaly.severity | title }})<br>
                    {{ anomaly.description }}<br>
                    <em>原因:</em> {{ anomaly.reason }}<br>
                    <em>建议:</em> {{ anomaly.recommendation }}
                </div>
                {% endfor %}
            {% else %}
                <p>未发现成本异常。</p>
            {% endif %}
        </div>

        <div class="card">
            <h2>优化建议</h2>
            {% for suggestion in suggestions %}
            <div class="suggestion">
                <h3>{{ suggestion.title }}</h3>
                <p><strong>优先级:</strong>
                    <span class="priority-{{ suggestion.priority }}">
                        {{ suggestion.priority | title }}
                    </span>
                </p>
                <p>{{ suggestion.description }}</p>
                <p><strong>预计节省:</strong> ${{ suggestion.potential_saving }}/月</p>
                <p><strong>实施难度:</strong> {{ suggestion.effort | title }}</p>
                <p><strong>行动步骤:</strong></p>
                <ul>
                    {% for action in suggestion.actions %}
                    <li>{{ action }}</li>
                    {% endfor %}
                </ul>
            </div>
            {% endfor %}
        </div>

        <div class="footer">
            <p>报告生成时间: {{ generated_at.strftime('%Y-%m-%d %H:%M:%S') }}</p>
            <p>BaseBackend FinOps Team</p>
        </div>
    </div>
</body>
</html>
        """

        template = Template(html_template)
        report_html = template.render(
            report_type=self.report_type,
            start_date=self.start_date,
            end_date=self.end_date,
            cost_data=cost_data,
            trends=trends,
            anomalies=anomalies,
            suggestions=suggestions,
            generated_at=datetime.now()
        )

        # 保存报告
        report_path = f"/tmp/cost-report-{self.report_type}-{self.start_date.strftime('%Y%m%d')}.html"
        with open(report_path, "w", encoding="utf-8") as f:
            f.write(report_html)

        return report_path

    def generate_json_report(self, cost_data: Dict, trends: Dict, anomalies: List, suggestions: List) -> str:
        """生成 JSON 报告"""
        report = {
            "report_metadata": {
                "type": self.report_type,
                "start_date": self.start_date.isoformat(),
                "end_date": self.end_date.isoformat(),
                "generated_at": datetime.now().isoformat(),
                "namespace": self.namespace
            },
            "cost_data": cost_data,
            "trends": trends,
            "anomalies": anomalies,
            "suggestions": suggestions,
            "summary": {
                "total_cost": cost_data["total_cost"]["current_period"],
                "cost_change": cost_data["total_cost"]["change_percentage"],
                "optimization_potential": sum(s["potential_saving"] for s in suggestions),
                "top_optimization": suggestions[0]["title"] if suggestions else None
            }
        }

        report_path = f"/tmp/cost-report-{self.report_type}-{self.start_date.strftime('%Y%m%d')}.json"
        with open(report_path, "w") as f:
            json.dump(report, f, indent=2)

        return report_path

    def send_email_notification(self, report_path: str) -> bool:
        """发送邮件通知"""
        try:
            # 注意: 这里只是示例，实际使用时需要配置 SMTP 服务器
            logger.info("准备发送邮件通知...")

            # 模拟邮件发送
            logger.info(f"报告已生成: {report_path}")
            logger.info("邮件通知功能需要配置 SMTP 服务器")

            return True
        except Exception as e:
            logger.error(f"发送邮件失败: {e}")
            return False

    def generate_report(self) -> Dict[str, str]:
        """生成完整报告"""
        logger.info(f"开始生成 {self.report_type} 成本报告...")

        # 1. 收集数据
        cost_data = self.collect_cost_data()

        # 2. 分析趋势
        trends = self.analyze_cost_trends(cost_data)

        # 3. 识别异常
        anomalies = self.identify_cost_anomalies(cost_data)

        # 4. 生成建议
        suggestions = self.generate_optimization_suggestions(cost_data)

        # 5. 生成报告
        html_report_path = self.generate_html_report(cost_data, trends, anomalies, suggestions)
        json_report_path = self.generate_json_report(cost_data, trends, anomalies, suggestions)

        # 6. 发送通知
        self.send_email_notification(html_report_path)

        return {
            "html_report": html_report_path,
            "json_report": json_report_path,
            "summary": {
                "total_cost": cost_data["total_cost"]["current_period"],
                "cost_change": cost_data["total_cost"]["change_percentage"],
                "optimization_potential": sum(s["potential_saving"] for s in suggestions),
                "recommendations_count": len(suggestions),
                "anomalies_count": len(anomalies)
            }
        }


def main():
    parser = argparse.ArgumentParser(description="成本报告生成器")
    parser.add_argument("--type", choices=["weekly", "monthly", "quarterly"], default="monthly",
                       help="报告类型 (默认: monthly)")
    parser.add_argument("--namespace", default="basebackend", help="Kubernetes 命名空间")
    parser.add_argument("--output", help="输出目录 (默认: /tmp)")

    args = parser.parse_args()

    reporter = CostReporter(report_type=args.type, namespace=args.namespace)
    report_paths = reporter.generate_report()

    # 输出结果
    print("\n" + "="*50)
    print(f"BaseBackend 成本报告已生成 ({args.type})")
    print("="*50)
    print(f"HTML 报告: {report_paths['html_report']}")
    print(f"JSON 报告: {report_paths['json_report']}")
    print("\n报告摘要:")
    print(f"  总成本: ${report_paths['summary']['total_cost']}")
    print(f"  成本变化: {report_paths['summary']['cost_change']:+.2f}%")
    print(f"  优化潜力: ${report_paths['summary']['optimization_potential']}/月")
    print(f"  建议数量: {report_paths['summary']['recommendations_count']}")
    print(f"  异常数量: {report_paths['summary']['anomalies_count']}")
    print("="*50 + "\n")

    return 0


if __name__ == "__main__":
    sys.exit(main())
