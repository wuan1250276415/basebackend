#!/bin/bash
# =====================================================================
# RocketMQ NameServer 启动脚本
# 创建时间: 2025-11-15
# 描述: 启动 RocketMQ NameServer 集群
# =====================================================================

set -e

echo "======================================="
echo "RocketMQ NameServer 集群启动"
echo "======================================="

# 配置变量
ROCKETMQ_VERSION=5.1.4
ROCKETMQ_HOME="/opt/basebackend/rocketmq"
ROCKETMQ_DATA_DIR="/opt/basebackend/data/rocketmq"
ROCKETMQ_LOG_DIR="/opt/basebackend/logs/rocketmq"
NAMESERVER_PORT=9876

# 下载并安装 RocketMQ
echo "下载 RocketMQ..."
cd /tmp
if [ ! -f "rocketmq-all-${ROCKETMQ_VERSION}-bin-release.zip" ]; then
    wget https://archive.apache.org/dist/rocketmq/rocketmq-all/${ROCKETMQ_VERSION}/rocketmq-all-${ROCKETMQ_VERSION}-bin-release.zip
fi

# 解压并安装
unzip -q rocketmq-all-${ROCKETMQ_VERSION}-bin-release.zip
mv rocketmq-all-${ROCKETMQ_VERSION}-bin-release $ROCKETMQ_HOME
rm -f rocketmq-all-${ROCKETMQ_VERSION}-bin-release.zip

# 创建数据目录
mkdir -p $ROCKETMQ_DATA_DIR/{nameserver,broker}
mkdir -p $ROCKETMQ_LOG_DIR/{nameserver,broker}
mkdir -p $ROCKETMQ_HOME/logs

# 配置 NameServer
echo "配置 NameServer..."
cat > $ROCKETMQ_HOME/conf/logback_nameserver.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="Default" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="RocketmqHome" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/nameserver.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/nameserver.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="RocketmqCommon" additivity="false">
        <level value="DEBUG" />
        <appender-ref ref="RocketmqHome" />
    </logger>

    <logger name="RocketmqRemoting" additivity="false">
        <level value="DEBUG" />
        <appender-ref ref="RocketmqHome" />
    </logger>

    <logger name="RocketmqStore" additivity="false">
        <level value="DEBUG" />
        <appender-ref ref="RocketmqHome" />
    </logger>

    <logger name="RocketmqFilter" additivity="false">
        <level value="DEBUG" />
        <appender-ref ref="RocketmqHome" />
    </logger>

    <logger name="RocketmqREST" additivity="false">
        <level value="INFO" />
        <appender-ref ref="RocketmqHome" />
    </logger>

    <logger name="RocketmqTEST" additivity="false">
        <level value="DEBUG" />
        <appender-ref ref="RocketmqHome" />
    </logger>

    <root level="INFO">
        <appender-ref ref="Default"/>
    </root>
</configuration>
EOF

# 启动 NameServer
echo "启动 NameServer..."
cd $ROCKETMQ_HOME

# 检查端口
if lsof -i :$NAMESERVER_PORT > /dev/null 2>&1; then
    echo "端口 $NAMESERVER_PORT 已被占用，停止现有进程..."
    lsof -ti :$NAMESERVER_PORT | xargs kill -9
    sleep 2
fi

# 启动 NameServer
nohup sh tools.sh org.apache.rocketmq.namesrv.NamesrvStartup \
    -c conf/nameserver.properties \
    > $ROCKETMQ_LOG_DIR/nameserver.log 2>&1 &

NAMESERVER_PID=$!
echo "NameServer 已启动，PID: $NAMESERVER_PID"

# 等待启动
echo "等待 NameServer 启动..."
sleep 10

# 检查启动状态
if curl -f http://localhost:${NAMESERVER_PORT}/rocketmq/ > /dev/null 2>&1 || \
   netstat -tlnp | grep -q ":${NAMESERVER_PORT}"; then
    echo "======================================="
    echo "✅ NameServer 启动成功!"
    echo "======================================="
    echo "📊 NameServer 地址: localhost:${NAMESERVER_PORT}"
    echo "📋 日志文件: $ROCKETMQ_LOG_DIR/nameserver.log"
    echo "📦 集群管理命令:"
    echo "  查看集群状态: sh tools.sh mqadmin clusterList -t 'cluster-demo'"
    echo "======================================="
else
    echo "❌ NameServer 启动失败"
    echo "📋 查看日志: tail -f $ROCKETMQ_LOG_DIR/nameserver.log"
    echo "======================================="
    exit 1
fi
