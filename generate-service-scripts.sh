#!/bin/bash
# 生成业务服务脚本
set -e

services=("dict" "log" "menu" "monitor" "notification" "profile")
ports=(8083 8085 8088 8089 8090 8091)

for i in "${!services[@]}"; do
    service=${services[$i]}
    port=${ports[$i]}
    
    echo "Generating scripts for basebackend-${service}-service (port: $port)..."
    
    # 创建启动脚本
    cat > "basebackend-${service}-service/scripts/start-${service}-service.sh" << EOF
#!/bin/bash
set -e

echo "======================================="
echo "${service^}服务启动脚本"
echo "======================================="

SERVICE_PORT=$port
SERVICE_NAME="basebackend-${service}-service"
SERVICE_LOG="logs/${service}-service.log"

mkdir -p logs

# 检查依赖服务
echo "检查依赖服务..."
for svc in mysql redis nacos; do
    if ! nc -z localhost \$((\$svc == 'mysql' && echo '3306' || \$svc == 'redis' && echo '6379' || echo '8848')) 2>/dev/null; then
        echo "警告: \$svc 服务不可用"
        read -p "是否继续? (y/n): " -n 1 -r
        echo
        [[ ! \$REPLY =~ ^[Yy]$ ]] && exit 1
    fi
done

# 检查端口
if lsof -i :\$SERVICE_PORT > /dev/null 2>&1; then
    echo "端口 \$SERVICE_PORT 已被占用，正在停止进程..."
    lsof -ti :\$SERVICE_PORT | xargs kill -9
    sleep 2
fi

# 编译和启动
echo "编译${service^}服务..."
mvn clean compile -DskipTests

echo "启动${service^}服务..."
nohup mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" > \$SERVICE_LOG 2>&1 &

echo "${service^}服务已启动，PID: \$!"
sleep 10

if curl -f http://localhost:\$SERVICE_PORT/actuator/health > /dev/null 2>&1; then
    echo "✅ ${service^}服务启动成功!"
    echo "📖 API文档: http://localhost:\$SERVICE_PORT/swagger-ui.html"
else
    echo "❌ ${service^}服务启动失败"
    exit 1
fi
