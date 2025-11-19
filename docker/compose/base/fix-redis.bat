@echo off
REM Redis快速修复脚本 (Windows)

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==========================================
echo Redis快速修复
echo ==========================================

REM 停止Redis
echo 1. 停止Redis容器...
docker-compose down redis 2>nul

REM 重新启动
echo 2. 重新启动Redis...
docker-compose up -d redis

REM 等待启动
echo 3. 等待Redis启动...
timeout /t 3 /nobreak >nul

REM 测试连接
echo 4. 测试连接...
docker exec basebackend-redis redis-cli -a redis2025 ping >nul 2>&1
if errorlevel 1 (
    echo X Redis启动失败
    echo.
    echo 查看日志：
    docker logs basebackend-redis
    exit /b 1
) else (
    echo √ Redis启动成功
)

REM 运行完整测试
echo.
echo 5. 运行完整测试...
call test-redis-connection.bat

echo.
echo ==========================================
echo Redis修复完成
echo ==========================================
echo.
echo 连接信息：
echo   Host: localhost
echo   Port: 6379
echo   Password: redis2025
echo.
echo 连接命令：
echo   redis-cli -h localhost -p 6379 -a redis2025
echo.

pause
