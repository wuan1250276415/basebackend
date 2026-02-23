#!/bin/bash
# ========================================
# BaseBackend-Scheduler 启动脚本 (Linux/Mac)
# ========================================
# 此脚本使用本地配置启动调度器服务
# 不依赖 Nacos 配置中心，适合开发测试
# ========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[信息]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[警告]${NC} $1"
}

log_error() {
    echo -e "${RED}[错误]${NC} $1"
}

echo ""
echo "========================================"
echo "BaseBackend-Scheduler 服务启动脚本"
echo "========================================"
echo ""

# 检查 Java 是否安装
if ! command -v java &> /dev/null; then
    log_error "未检测到 Java，请先安装 JDK 8 或更高版本"
    exit 1
fi

log_info "检测到 Java 版本："
java -version 2>&1 | grep version
echo ""

# 检查 JAR 文件是否存在
JAR_FILE="target/basebackend-scheduler-1.0.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    log_error "未找到 JAR 文件: $JAR_FILE"
    echo "请先运行: mvn clean package -DskipTests"
    exit 1
fi

log_info "找到 JAR 文件: $JAR_FILE"
echo ""

# 设置 JVM 参数
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 设置启动参数
SPRING_OPTS="--spring.profiles.active=local --server.port=8085"

# 设置日志级别
LOG_OPTS="--logging.level.root=INFO --logging.level.com.basebackend.scheduler=DEBUG"

log_info "启动参数"
echo "  JVM: $JVM_OPTS"
echo "  Profile: local"
echo "  Port: 8085"
echo ""

echo "========================================"
echo "启动中，请稍候..."
echo "访问地址: http://localhost:8085"
echo "API 文档: http://localhost:8085/doc.html"
echo "健康检查: http://localhost:8085/actuator/health"
echo "按 Ctrl+C 停止服务"
echo "========================================"
echo ""

# 启动应用
java $JVM_OPTS -jar "$JAR_FILE" $SPRING_OPTS $LOG_OPTS

# 如果启动失败，显示错误信息
if [ $? -ne 0 ]; then
    echo ""
    echo "========================================"
    log_error "服务启动失败"
    echo "========================================"
    echo "请检查:"
    echo "  1. 端口 8085 是否被占用 (netstat -tlnp | grep 8085)"
    echo "  2. 数据库连接是否正常"
    echo "  3. 网络连接是否可达"
    echo ""
    echo "详细日志请查看控制台输出"
    exit 1
fi
