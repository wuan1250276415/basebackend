#!/bin/bash
# BaseBackend 监控系统启动脚本

set -e

echo "================================"
echo "BaseBackend 监控系统启动脚本"
echo "================================"

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

# 创建必要的目录
echo "📁 创建监控目录结构..."
mkdir -p prometheus grafana/provisioning/datasources grafana/provisioning/dashboards grafana/dashboards grafana/dashboards-json alertmanager

# 设置文件权限
chmod +x ./scripts/*.sh

echo ""
echo "启动顺序："
echo "1. Prometheus (端口 9090) - 监控数据收集"
echo "2. Grafana (端口 3000) - 可视化仪表板"
echo "3. AlertManager (端口 9093) - 告警管理"
echo ""

# 启动监控系统
echo "🚀 启动监控系统..."
docker-compose -f docker-compose.monitoring.yml up -d

echo ""
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "📊 检查服务状态："
echo "----------------------------------------"

if curl -s http://localhost:9090 > /dev/null; then
    echo "✅ Prometheus: http://localhost:9090"
else
    echo "❌ Prometheus 启动失败"
fi

if curl -s http://localhost:3000 > /dev/null; then
    echo "✅ Grafana: http://localhost:3000"
    echo "   默认用户名: admin"
    echo "   默认密码: admin123"
else
    echo "❌ Grafana 启动失败"
fi

if curl -s http://localhost:9093 > /dev/null; then
    echo "✅ AlertManager: http://localhost:9093"
else
    echo "❌ AlertManager 启动失败"
fi

echo ""
echo "================================"
echo "🎉 监控系统启动完成！"
echo "================================"
echo ""
echo "访问地址："
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000"
echo "  AlertManager: http://localhost:9093"
echo ""
echo "使用说明："
echo "  1. 访问 Grafana 仪表板查看监控数据"
echo "  2. 在 Prometheus 中查看告警规则"
echo "  3. 在 AlertManager 中管理告警通知"
echo ""
echo "常用命令："
echo "  停止监控: docker-compose -f docker-compose.monitoring.yml down"
echo "  查看日志: docker-compose -f docker-compose.monitoring.yml logs -f"
echo "  重启服务: docker-compose -f docker-compose.monitoring.yml restart"
echo ""
