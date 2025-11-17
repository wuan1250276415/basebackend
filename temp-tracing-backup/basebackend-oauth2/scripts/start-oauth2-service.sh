#!/bin/bash
# =====================================================================
# BaseBackend OAuth2.0服务启动脚本
# 创建时间: 2025-11-15
# 描述: 启动OAuth2.0授权服务器服务
# =====================================================================

set -e

echo "======================================="
echo "BaseBackend OAuth2.0 授权服务器启动"
echo "======================================="

# 配置变量
SERVICE_NAME="basebackend-oauth2"
SERVICE_PORT=8082
SERVICE_DIR="/opt/basebackend/services/oauth2"
LOG_DIR="/opt/basebackend/logs/oauth2"
SPRING_PROFILES="dev"
MAX_MEMORY=2048

# 停止现有进程
echo "停止现有OAuth2.0服务进程..."
lsof -ti:$SERVICE_PORT | xargs kill -9 2>/dev/null || true
sleep 2

# 创建目录
mkdir -p $SERVICE_DIR
mkdir -p $LOG_DIR

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请先安装JDK 17+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "检测到Java版本: $JAVA_VERSION"

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven环境，请先安装Maven 3.6+"
    exit 1
fi

# 构建项目
echo "构建OAuth2.0授权服务器项目..."
cd $SERVICE_DIR/../..
mvn clean package -DskipTests -pl basebackend-oauth2 -am

if [ $? -ne 0 ]; then
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi

echo "✅ 构建成功"

# 创建启动脚本
cat > $SERVICE_DIR/start-oauth2.sh << 'EOF'
#!/bin/bash
SERVICE_PORT=8082
SERVICE_JAR=$(find /opt/basebackend/services/oauth2 -name "basebackend-oauth2-*.jar" | head -1)

if [ -z "$SERVICE_JAR" ]; then
    echo "错误: 未找到OAuth2.0 JAR文件"
    exit 1
fi

echo "启动OAuth2.0授权服务器..."
echo "服务端口: $SERVICE_PORT"
echo "JVM内存: ${MAX_MEMORY}m"

nohup java -Xms${MAX_MEMORY}m -Xmx${MAX_MEMORY}m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/opt/basebackend/logs/oauth2/ \
    -Dspring.profiles.active=${SPRING_PROFILES} \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -jar $SERVICE_JAR \
    > /opt/basebackend/logs/oauth2/oauth2.out 2>&1 &

echo "OAuth2.0授权服务器已启动，PID: $!"
EOF

chmod +x $SERVICE_DIR/start-oauth2.sh

# 启动服务
cd $SERVICE_DIR
./start-oauth2.sh

# 等待启动
echo "等待OAuth2.0授权服务器启动..."
for i in {1..30}; do
    if curl -f http://localhost:$SERVICE_PORT/actuator/health > /dev/null 2>&1; then
        echo "======================================="
        echo "✅ OAuth2.0授权服务器启动成功!"
        echo "======================================="
        echo "📊 服务信息:"
        echo "  服务名称: $SERVICE_NAME"
        echo "  服务端口: $SERVICE_PORT"
        echo "  日志文件: $LOG_DIR/oauth2.out"
        echo ""
        echo "🌐 服务地址:"
        echo "  授权端点: http://localhost:$SERVICE_PORT/oauth2/authorize"
        echo "  令牌端点: http://localhost:$SERVICE_PORT/oauth2/token"
        echo "  用户信息: http://localhost:$SERVICE_PORT/oauth2/userinfo"
        echo "  JWK集: http://localhost:$SERVICE_PORT/oauth2/jwks"
        echo ""
        echo "📖 文档地址: http://localhost:$SERVICE_PORT/swagger-ui.html"
        echo "📊 健康检查: http://localhost:$SERVICE_PORT/actuator/health"
        echo "======================================="
        echo ""
        echo "🔑 OAuth2.0客户端配置:"
        echo "  Web应用: basebackend-web"
        echo "  移动应用: basebackend-mobile"
        echo "  微服务: basebackend-service"
        echo "======================================="
        exit 0
    fi
    sleep 2
    echo -n "."
done

echo ""
echo "❌ 启动超时，检查日志: tail -f $LOG_DIR/oauth2.out"
exit 1
