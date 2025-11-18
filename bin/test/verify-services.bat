@echo off
REM 微服务验证脚本 - Windows版本
REM 用于验证三个核心微服务是否正常运行

setlocal enabledelayedexpansion

echo ==========================================
echo 微服务验证脚本
echo ==========================================
echo.

REM 服务配置
set USER_API_URL=http://localhost:8081
set SYSTEM_API_URL=http://localhost:8082
set AUTH_API_URL=http://localhost:8083
set NACOS_URL=http://localhost:8848

REM 检查curl是否可用
where curl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未找到curl命令，请安装curl或使用Git Bash运行.sh脚本
    exit /b 1
)

echo 1. 检查基础设施
echo ----------------------------------------

REM 检查Nacos
echo 检查Nacos...
curl -s -f "%NACOS_URL%/nacos/v1/console/health/readiness" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Nacos运行中
) else (
    echo [错误] Nacos未运行
    echo 请先启动Nacos: docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d nacos
    exit /b 1
)

echo.
echo 2. 检查微服务健康状态
echo ----------------------------------------

REM 检查User API
echo 检查User API健康状态...
curl -s -f "%USER_API_URL%/actuator/health" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] User API正常
) else (
    echo [错误] User API异常
    set HEALTH_FAILED=1
)

REM 检查System API
echo 检查System API健康状态...
curl -s -f "%SYSTEM_API_URL%/actuator/health" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] System API正常
) else (
    echo [错误] System API异常
    set HEALTH_FAILED=1
)

REM 检查Auth API
echo 检查Auth API健康状态...
curl -s -f "%AUTH_API_URL%/actuator/health" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Auth API正常
) else (
    echo [错误] Auth API异常
    set HEALTH_FAILED=1
)

if defined HEALTH_FAILED (
    echo.
    echo [警告] 部分服务健康检查失败，请检查服务是否启动
    echo 启动命令: bin\start\start-microservices.bat
    exit /b 1
)

echo.
echo 3. 检查Nacos服务注册
echo ----------------------------------------

REM 检查User API注册
echo 检查basebackend-user-api在Nacos的注册状态...
curl -s "%NACOS_URL%/nacos/v1/ns/instance/list?serviceName=basebackend-user-api" | findstr /C:"\"count\":[1-9]" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] basebackend-user-api已注册
) else (
    echo [警告] basebackend-user-api未注册
)

REM 检查System API注册
echo 检查basebackend-system-api在Nacos的注册状态...
curl -s "%NACOS_URL%/nacos/v1/ns/instance/list?serviceName=basebackend-system-api" | findstr /C:"\"count\":[1-9]" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] basebackend-system-api已注册
) else (
    echo [警告] basebackend-system-api未注册
)

REM 检查Auth API注册
echo 检查basebackend-auth-api在Nacos的注册状态...
curl -s "%NACOS_URL%/nacos/v1/ns/instance/list?serviceName=basebackend-auth-api" | findstr /C:"\"count\":[1-9]" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] basebackend-auth-api已注册
) else (
    echo [警告] basebackend-auth-api未注册
)

echo.
echo 4. 测试API接口
echo ----------------------------------------

echo 测试登录接口...
curl -s -X POST "%AUTH_API_URL%/api/auth/login" ^
    -H "Content-Type: application/json" ^
    -d "{\"username\":\"admin\",\"password\":\"admin123\"}" > temp_response.json 2>&1

findstr /C:"accessToken" temp_response.json >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] 登录接口测试成功
    type temp_response.json
) else (
    echo [错误] 登录接口测试失败
    type temp_response.json
)

del temp_response.json >nul 2>&1

echo.
echo ==========================================
echo 验证完成
echo ==========================================
echo.
echo 服务访问地址:
echo   - User API:   %USER_API_URL%
echo   - System API: %SYSTEM_API_URL%
echo   - Auth API:   %AUTH_API_URL%
echo.
echo API文档:
echo   - User API:   %USER_API_URL%/doc.html
echo   - System API: %SYSTEM_API_URL%/doc.html
echo   - Auth API:   %AUTH_API_URL%/doc.html
echo.
echo Nacos控制台:
echo   - URL: %NACOS_URL%/nacos
echo   - 账号: nacos / nacos
echo.

endlocal
