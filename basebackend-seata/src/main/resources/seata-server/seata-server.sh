#!/bin/bash
# =====================================================================
# Seata 分布式事务服务器启动脚本
# 创建时间: 2025-11-15
# 描述: 启动 Seata Server 作为分布式事务协调器
# =====================================================================

set -e

echo "======================================="
echo "Seata 分布式事务服务器启动"
echo "======================================="

# 配置变量
SEATA_PORT=7091
SEATA_DATA_DIR="/opt/basebackend/data/seata"
SEATA_LOG_DIR="/opt/basebackend/logs/seata"
SEATA_BIN_DIR="/opt/basebackend/seata/bin"

# 创建目录
mkdir -p $SEATA_DATA_DIR
mkdir -p $SEATA_LOG_DIR

echo "检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 JDK 17+"
    exit 1
fi

echo "检查 Seata 安装..."
if [ ! -f "$SEATA_BIN_DIR/seata-server.sh" ]; then
    echo "下载 Seata Server..."
    cd /opt/basebackend/seata
    wget https://github.com/seata/seata/releases/download/v1.7.0/seata-server-1.7.0.tar.gz
    tar -xzf seata-server-1.7.0.tar.gz
    rm seata-server-1.7.0.tar.gz
fi

# 检查端口
echo "检查端口 $SEATA_PORT 是否被占用..."
if lsof -i :$SEATA_PORT > /dev/null 2>&1; then
    echo "端口 $SEATA_PORT 已被占用，停止现有进程..."
    lsof -ti :$SEATA_PORT | xargs kill -9
    sleep 2
fi

# 配置 Seata
echo "配置 Seata Server..."

# 创建 registry.conf
cat > $SEATA_BIN_DIR/registry.conf << 'EOF'
registry {
  type = "nacos"
  nacos {
    serverAddr = "localhost:8848"
    namespace = "basebackend"
    group = "SEATA_GROUP"
    cluster = "default"
    username = "nacos"
    password = "nacos"
  }
}

config {
  type = "nacos"
  nacos {
    serverAddr = "localhost:8848"
    namespace = "basebackend"
    group = "SEATA_GROUP"
    username = "nacos"
    password = "nacos"
  }
}
EOF

# 创建 logback.xml
cat > $SEATA_BIN_DIR/logback.xml << 'EOF'
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/seata-server.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/seata-server.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
EOF

# 启动 Seata Server
echo "启动 Seata Server..."
cd $SEATA_BIN_DIR

nohup sh seata-server.sh -p $SEATA_PORT \
    -m db \
    -n 1 \
    > $SEATA_LOG_DIR/seata-server.log 2>&1 &

SEATA_PID=$!
echo "Seata Server 已启动，PID: $SEATA_PID"

# 等待启动
echo "等待 Seata Server 启动..."
sleep 10

# 检查启动状态
if curl -f http://localhost:$SEATA_PORT > /dev/null 2>&1; then
    echo "======================================="
    echo "✅ Seata Server 启动成功!"
    echo "======================================="
    echo "📊 管理后台: http://localhost:$SEATA_PORT"
    echo "📋 日志文件: $SEATA_LOG_DIR/seata-server.log"
    echo "🔧 配置文件: $SEATA_BIN_DIR/registry.conf"
    echo "======================================="
else
    echo "❌ Seata Server 启动失败"
    echo "📋 查看日志: tail -f $SEATA_LOG_DIR/seata-server.log"
    echo "======================================="
    exit 1
fi
