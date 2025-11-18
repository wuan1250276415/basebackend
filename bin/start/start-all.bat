@echo off
REM ============================================================================
REM BaseBackend 微服务启动脚本 (Windows)
REM ============================================================================
REM 功能：按顺序启动所有微服务，并进行健康检查
REM 使用：双击运行或在命令行执行 start-all.bat
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================
echo   BaseBackend 微服务启动脚本
echo ========================================
echo.

REM 检查Java环境
echo [1/7] 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] Java未安装或未配置到PATH
    echo 请安装Java 17或更高版本
    pause
    exit /b 1
)
echo [✓] Java环境检查通过
echo.

REM 检查Maven构建
echo [2/7] 检查项目构建...
if not exist "basebackend-user-api\target\*.jar" (
    echo [警告] 未找到构建产物，开始构建项目...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo [错误] 项目构建失败
        pause
        exit /b 1
    )
)
echo [✓] 项目构建检查通过
echo.

REM 检查基础设施
echo [3/7] 检查基础设施服务...
echo 检查MySQL...
netstat -an | findstr ":3306" >nul 2>&1
if errorlevel 1 (
    echo [警告] MySQL未启动，请先启动MySQL
    echo 可以使用: docker-compose -f docker/compose/base/docker-compose.base.yml up -d mysql
)

echo 检查Redis...
netstat -an | findstr ":6379" >nul 2>&1
if errorlevel 1 (
    echo [警告] Redis未启动，请先启动Redis
    echo 可以使用: docker-compose -f docker/compose/base/docker-compose.base.yml up -d redis
)

echo 检查Nacos...
netstat -an | findstr ":8848" >nul 2>&1
if errorlevel 1 (
    echo [警告] Nacos未启动，请先启动Nacos
    echo 可以使用: docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d nacos
)
echo.

REM 设置JVM参数
set JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=100

REM 启动网关
echo [4/7] 启动API网关...
cd basebackend-gateway
start "BaseBackend Gateway" java %JAVA_OPTS% -jar target\basebackend-gateway-*.jar
cd ..
timeout /t 15 /nobreak >nul
call :check_health "Gateway" 8080
echo.

REM 启动认证服务
echo [5/7] 启动认证服务...
cd basebackend-auth-api
start "BaseBackend Auth API" java %JAVA_OPTS% -jar target\basebackend-auth-api-*.jar
cd ..
timeout /t 15 /nobreak >nul
call :check_health "Auth API" 8083
echo.

REM 启动用户服务
echo [6/7] 启动用户服务...
cd basebackend-user-api
start "BaseBackend User API" java %JAVA_OPTS% -jar target\basebackend-user-api-*.jar
cd ..
timeout /t 15 /nobreak >nul
call :check_health "User API" 8081
echo.

REM 启动系统服务
echo [7/7] 启动系统服务...
cd basebackend-system-api
start "BaseBackend System API" java %JAVA_OPTS% -jar target\basebackend-system-api-*.jar
cd ..
timeout /t 15 /nobreak >nul
call :check_health "System API" 8082
echo.

REM 启动通知服务（可选）
echo [可选] 启动通知服务...
cd basebackend-notification-service
start "BaseBackend Notification Service" java %JAVA_OPTS% -jar target\basebackend-notification-service-*.jar
cd ..
timeout /t 10 /nobreak >nul
echo.

REM 启动可观测性服务（可选）
echo [可选] 启动可观测性服务...
cd basebackend-observability-service
start "BaseBackend Observability Service" java %JAVA_OPTS% -jar target\basebackend-observability-service-*.jar
cd ..
timeout /t 10 /nobreak >nul
echo.

echo ========================================
echo   所有服务启动完成！
echo ========================================
echo.
echo 服务访问地址：
echo   - API网关:        http://localhost:8080
echo   - 用户服务:       http://localhost:8081
echo   - 系统服务:       http://localhost:8082
echo   - 认证服务:       http://localhost:8083
echo   - 通知服务:       http://localhost:8086
echo   - 可观测性服务:   http://localhost:8087
echo.
echo API文档地址：
echo   - 网关文档:       http://localhost:8080/doc.html
echo   - 用户服务文档:   http://localhost:8081/doc.html
echo   - 系统服务文档:   http://localhost:8082/doc.html
echo.
echo 按任意键退出...
pause >nul
exit /b 0

REM ============================================================================
REM 健康检查函数
REM ============================================================================
:check_health
set service_name=%~1
set port=%~2
set max_attempts=30

echo 等待 %service_name% 启动...
for /L %%i in (1,1,%max_attempts%) do (
    curl -s http://localhost:%port%/actuator/health >nul 2>&1
    if not errorlevel 1 (
        echo [✓] %service_name% 启动成功
        goto :eof
    )
    timeout /t 2 /nobreak >nul
)

echo [✗] %service_name% 启动失败或超时
goto :eof
