#!/bin/bash
# =====================================================================
# JVM 参数优化脚本
# 创建时间: 2025-11-15
# 描述: 自动优化 JVM 参数以提升微服务性能
# =====================================================================

set -e

echo "======================================="
echo "JVM 参数优化"
echo "======================================="

# 检测系统资源
TOTAL_MEMORY=$(free -m | awk 'NR==2{printf "%.0f", $2}')
AVAILABLE_MEMORY=$(free -m | awk 'NR==2{printf "%.0f", $7}')
CPU_CORES=$(nproc)

echo "系统信息:"
echo "  总内存: ${TOTAL_MEMORY}MB"
echo "  可用内存: ${AVAILABLE_MEMORY}MB"
echo "  CPU 核心数: $CPU_CORES"
echo ""

# 根据系统资源计算 JVM 参数
if [ $AVAILABLE_MEMORY -gt 8192 ]; then
    # 高内存系统 (>= 8GB)
    HEAP_SIZE=4096
    METASPACE_SIZE=512
    NEW_RATIO=8
elif [ $AVAILABLE_MEMORY -gt 4096 ]; then
    # 中等内存系统 (4-8GB)
    HEAP_SIZE=2048
    METASPACE_SIZE=256
    NEW_RATIO=8
else
    # 低内存系统 (< 4GB)
    HEAP_SIZE=1024
    METASPACE_SIZE=128
    NEW_RATIO=8
fi

echo "计算 JVM 参数:"
echo "  堆内存大小: ${HEAP_SIZE}MB"
echo "  Metaspace 大小: ${METASPACE_SIZE}MB"
echo "  新生代比例: ${NEW_RATIO}"
echo ""

# 生成 JVM 优化配置
cat > jvm-optimization.properties << EOF
# JVM 优化参数配置
# 创建时间: $(date)

# ============================================
# 堆内存配置
# ============================================
-Xms${HEAP_SIZE}m
-Xmx${HEAP_SIZE}m

# ============================================
# 新生代配置
# ============================================
-XX:NewRatio=${NEW_RATIO}

# ============================================
# Metaspace 配置
# ============================================
-XX:MetaspaceSize=${METASPACE_SIZE}m
-XX:MaxMetaspaceSize=${METASPACE_SIZE}m

# ============================================
# GC 配置 (G1GC)
# ============================================
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:+UseStringDeduplication
-XX:+ParallelRefProcEnabled
-XX:G1HeapWastePercent=5

# ============================================
# 性能优化
# ============================================
-XX:+UseStringCache
-XX:+OptimizeStringConcat
-XX:+UseFastAccessorMethods
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers

# ============================================
# 调试和监控
# ============================================
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCDateStamps
-Xloggc:/logs/gc-%t.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=10
-XX:GCLogFileSize=10M

# ============================================
# 崩溃处理
# ============================================
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/heapdump.hprof
-XX:+ExitOnOutOfMemoryError

# ============================================
# JIT 编译优化
# ============================================
-XX:+TieredCompilation
-XX:TieredStopAtLevel=4
-XX:CompileThreshold=10000

# ============================================
# AOT 配置 (Java 9+)
# ============================================
-XX:+EnableJVMCI
-XX:+UseJVMCICompiler
EOF

# 创建服务启动脚本模板
cat > start-service-optimized.sh << 'EOF'
#!/bin/bash
# =====================================================================
# 优化后的服务启动脚本
# =====================================================================

SERVICE_NAME=$1
SERVICE_PORT=$2
SERVICE_LOG="logs/${SERVICE_NAME}.log"

if [ -z "$SERVICE_NAME" ] || [ -z "$SERVICE_PORT" ]; then
    echo "用法: $0 <服务名> <端口>"
    echo "示例: $0 user-service 8081"
    exit 1
fi

# 加载 JVM 优化参数
source jvm-optimization.properties

echo "======================================="
echo "启动服务: $SERVICE_NAME"
echo "端口: $SERVICE_PORT"
echo "JVM 参数: $@"
echo "======================================="

# 检查端口
if lsof -i :$SERVICE_PORT > /dev/null 2>&1; then
    echo "端口 $SERVICE_PORT 已被占用"
    exit 1
fi

# 编译项目
mvn clean compile -DskipTests

# 启动服务
nohup mvn spring-boot:run \
    $@ \
    -Dspring-boot.run.jvmArguments="$*" \
    > $SERVICE_LOG 2>&1 &

SERVICE_PID=$!
echo "服务已启动，PID: $SERVICE_PID"
echo "日志文件: $SERVICE_LOG"
echo "======================================="
EOF

chmod +x start-service-optimized.sh

echo "JVM 优化配置已生成: jvm-optimization.properties"
echo "优化启动脚本已生成: start-service-optimized.sh"
echo ""

# 为不同服务生成启动脚本
declare -A SERVICES
SERVICES=(
    ["user-service"]="8081"
    ["auth-service"]="8082"
    ["dict-service"]="8083"
    ["dept-service"]="8084"
)

for service in "${!SERVICES[@]}"; do
    port=${SERVICES[$service]}
    script_name="start-${service}-optimized.sh"

    cat > "$script_name" << EOF
#!/bin/bash
source jvm-optimization.properties

SERVICE_LOG="logs/${service}.log"
mkdir -p logs

mvn clean compile -DskipTests

nohup mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="\$*" \
    > \$SERVICE_LOG 2>&1 &

echo "${service} 已启动，PID: \$!"
echo "日志: \$SERVICE_LOG"
EOF

    chmod +x "$script_name"
done

echo "已为所有服务生成优化启动脚本"
echo "======================================="
