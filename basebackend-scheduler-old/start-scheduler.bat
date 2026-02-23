@echo off
REM ========================================
REM BaseBackend-Scheduler 启动脚本
REM ========================================
REM 此脚本使用本地配置启动调度器服务
REM 不依赖 Nacos 配置中心，适合开发测试
REM ========================================

setlocal EnableDelayedExpansion

echo.
echo ========================================
echo BaseBackend-Scheduler 服务启动脚本
echo ========================================
echo.

REM 检查 Java 是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Java，请先安装 JDK 8 或更高版本
    pause
    exit /b 1
)

echo [信息] 检测到 Java 版本：
java -version | findstr "version"

REM 检查 JAR 文件是否存在
set JAR_FILE=target\basebackend-scheduler-1.0.0-SNAPSHOT.jar
if not exist "%JAR_FILE%" (
    echo [错误] 未找到 JAR 文件: %JAR_FILE%
    echo 请先运行: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo [信息] 找到 JAR 文件: %JAR_FILE%
echo.

REM 设置 JVM 参数
set JVM_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

REM 设置启动参数
set SPRING_OPTS=--spring.profiles.active=local --server.port=8085

REM 设置日志级别
set LOG_OPTS=--logging.level.root=INFO --logging.level.com.basebackend.scheduler=DEBUG

echo [启动参数]
echo   JVM: %JVM_OPTS%
echo   Profile: local
echo   Port: 8085
echo.

echo ========================================
echo 启动中，请稍候...
echo 访问地址: http://localhost:8085
echo API 文档: http://localhost:8085/doc.html
echo 健康检查: http://localhost:8085/actuator/health
echo 按 Ctrl+C 停止服务
echo ========================================
echo.

REM 启动应用
java %JVM_OPTS% -jar "%JAR_FILE%" %SPRING_OPTS% %LOG_OPTS%

REM 如果启动失败，显示错误信息
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo [错误] 服务启动失败
    echo ========================================
    echo 请检查:
    echo   1. 端口 8085 是否被占用
    echo   2. 数据库连接是否正常
    echo   3. 网络连接是否可达
    echo.
    echo 详细日志请查看控制台输出
    echo 或配置日志文件输出到 logs\scheduler.log
    echo.
    pause
)

endlocal
