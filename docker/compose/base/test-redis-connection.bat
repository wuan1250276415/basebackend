@echo off
REM Redis连接测试脚本 (Windows)

setlocal enabledelayedexpansion

set REDIS_PASSWORD=redis2025
set REDIS_HOST=localhost
set REDIS_PORT=6379

echo ==========================================
echo Redis连接测试
echo ==========================================
echo Host: %REDIS_HOST%
echo Port: %REDIS_PORT%
echo Password: %REDIS_PASSWORD%
echo.

REM 测试1: 使用docker exec
echo 测试1: 使用docker exec连接...
docker exec basebackend-redis redis-cli -a "%REDIS_PASSWORD%" ping >nul 2>&1
if errorlevel 1 (
    echo X Docker exec连接失败
) else (
    echo √ Docker exec连接成功
)

REM 测试2: 测试读写操作
echo.
echo 测试2: 测试读写操作...
docker exec basebackend-redis redis-cli -a "%REDIS_PASSWORD%" SET test_key "test_value" >nul 2>&1
for /f "delims=" %%i in ('docker exec basebackend-redis redis-cli -a "%REDIS_PASSWORD%" GET test_key 2^>nul') do set RESULT=%%i
if "%RESULT%"=="test_value" (
    echo √ 读写操作成功
    docker exec basebackend-redis redis-cli -a "%REDIS_PASSWORD%" DEL test_key >nul 2>&1
) else (
    echo X 读写操作失败
)

REM 测试3: 查看Redis信息
echo.
echo 测试3: 查看Redis信息...
docker exec basebackend-redis redis-cli -a "%REDIS_PASSWORD%" INFO server 2>nul | findstr /C:"redis_version" /C:"os" /C:"tcp_port"

echo.
echo ==========================================
echo 连接信息
echo ==========================================
echo 使用redis-cli连接命令：
echo   redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD%
echo.
echo 使用Docker exec连接命令：
echo   docker exec -it basebackend-redis redis-cli -a %REDIS_PASSWORD%
echo.
echo Spring Boot配置：
echo   spring.redis.host=%REDIS_HOST%
echo   spring.redis.port=%REDIS_PORT%
echo   spring.redis.password=%REDIS_PASSWORD%
echo.

pause
