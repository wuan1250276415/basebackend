#!/bin/bash
# =====================================================================
# Redis Cluster 启动脚本
# 创建时间: 2025-11-15
# 描述: 启动 Redis Cluster 集群
# =====================================================================

set -e

echo "======================================="
echo "Redis Cluster 集群启动"
echo "======================================="

# 配置变量
REDIS_VERSION=7.0.0
REDIS_PORT_START=7000
REDIS_CLUSTER_SIZE=6
REDIS_DIR="/opt/basebackend/redis-cluster"

# 安装 Redis
echo "安装 Redis..."
cd /tmp
wget http://download.redis.io/releases/redis-${REDIS_VERSION}.tar.gz
tar -xzf redis-${REDIS_VERSION}.tar.gz
cd redis-${REDIS_VERSION}
make
make install
cp src/redis-* /usr/local/bin/

# 创建集群目录
echo "创建集群目录..."
mkdir -p $REDIS_DIR/{7000,7001,7002,7003,7004,7005}
mkdir -p /opt/basebackend/logs/redis-cluster

# 创建 Redis 配置文件
for i in $(seq 0 5); do
    PORT=$((REDIS_PORT_START + i))
    CONF_FILE="$REDIS_DIR/$PORT/redis.conf"

    cat > $CONF_FILE << EOF
# Redis Cluster 配置文件
port $PORT
bind 0.0.0.0
protected-mode no
dir /opt/basebackend/redis-cluster/$PORT
pidfile /var/run/redis-${PORT}.pid

# 集群配置
cluster-enabled yes
cluster-config-file nodes-${PORT}.conf
cluster-node-timeout 15000
cluster-require-full-coverage no

# 持久化配置
appendonly yes
appendfilename "appendonly-${PORT}.aof"
save 900 1
save 300 10
save 60 10000

# AOF 优化
appendfsync everysec
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# 内存配置
maxmemory 2gb
maxmemory-policy allkeys-lru

# 网络优化
tcp-backlog 511
tcp-keepalive 300
timeout 0

# 慢查询
slowlog-log-slower-than 10000
slowlog-max-len 128

# 日志
loglevel notice
logfile /opt/basebackend/logs/redis-cluster/redis-${PORT}.log

# 安全配置
# requirepass yourpassword

# Lua 脚本优化
lua-time-limit 5000
EOF

    echo "创建配置文件: $CONF_FILE"
done

# 启动 Redis 实例
echo "启动 Redis 实例..."
for i in $(seq 0 5); do
    PORT=$((REDIS_PORT_START + i))
    CONF_FILE="$REDIS_DIR/$PORT/redis.conf"
    PID_FILE="/var/run/redis-${PORT}.pid"

    echo "启动 Redis 实例 $PORT..."

    # 停止现有进程
    if [ -f "$PID_FILE" ]; then
        kill $(cat $PID_FILE) 2>/dev/null || true
        rm -f $PID_FILE
    fi

    # 启动 Redis
    redis-server $CONF_FILE

    sleep 2

    # 检查启动状态
    if redis-cli -p $PORT ping > /dev/null 2>&1; then
        echo "  ✅ Redis $PORT 启动成功"
    else
        echo "  ❌ Redis $PORT 启动失败"
    fi
done

# 创建 Redis Cluster
echo "创建 Redis Cluster..."
sleep 5

# 检查所有实例是否都可用
ALL_OK=true
for i in $(seq 0 5); do
    PORT=$((REDIS_PORT_START + i))
    if ! redis-cli -p $PORT ping > /dev/null 2>&1; then
        echo "❌ Redis $PORT 不可用"
        ALL_OK=false
        break
    fi
done

if [ "$ALL_OK" = true ]; then
    echo "所有 Redis 实例已启动，开始创建集群..."

    # 创建集群
    redis-cli --cluster create \
        127.0.0.1:7000 \
        127.0.0.1:7001 \
        127.0.0.1:7002 \
        127.0.0.1:7003 \
        127.0.0.1:7004 \
        127.0.0.1:7005 \
        --cluster-replicas 1 \
        --cluster-yes

    echo "======================================="
    echo "✅ Redis Cluster 集群创建成功!"
    echo "======================================="
    echo "集群节点:"
    redis-cli -p 7000 cluster nodes

    echo ""
    echo "集群信息:"
    redis-cli -p 7000 cluster info

    echo ""
    echo "集群槽位信息:"
    redis-cli -p 7000 cluster slots

    echo ""
    echo "======================================="
    echo "📊 集群管理命令:"
    echo "查看集群状态: redis-cli -p 7000 cluster info"
    echo "查看集群节点: redis-cli -p 7000 cluster nodes"
    echo "查看集群槽位: redis-cli -p 7000 cluster slots"
    echo "连接集群: redis-cli -c -p 7000"
    echo "======================================="
else
    echo "❌ 部分 Redis 实例启动失败"
    exit 1
fi
