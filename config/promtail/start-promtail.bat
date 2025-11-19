@echo off
REM Promtail启动脚本 (Windows)

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==========================================
echo 启动Promtail日志收集服务
echo ==========================================

REM 检查Docker是否运行
docker info >nul 2>&1
if errorlevel 1 (
    echo 错误: Docker未运行，请先启动Docker
    exit /b 1
)

REM 创建网络（如果不存在）
docker network inspect basebackend-network >nul 2>&1
if errorlevel 1 (
    echo 创建Docker网络: basebackend-network
    docker network create basebackend-network
)

REM 创建日志目录
echo 创建日志目录...
if not exist "..\..\logs\gateway" mkdir "..\..\logs\gateway"
if not exist "..\..\logs\auth-api" mkdir "..\..\logs\auth-api"
if not exist "..\..\logs\user-api" mkdir "..\..\logs\user-api"
if not exist "..\..\logs\system-api" mkdir "..\..\logs\system-api"
if not exist "..\..\logs\notification-service" mkdir "..\..\logs\notification-service"
if not exist "..\..\logs\observability-service" mkdir "..\..\logs\observability-service"

REM 启动服务
echo 启动Loki和Promtail...
docker-compose -f docker-compose.promtail.yml up -d

REM 等待服务启动
echo 等待服务启动...
timeout /t 5 /nobreak >nul

REM 检查服务状态
echo.
echo 检查服务状态...

curl -s http://localhost:3100/ready >nul 2>&1
if errorlevel 1 (
    echo X Loki启动失败
) else (
    echo √ Loki运行正常 (http://localhost:3100)
)

curl -s http://localhost:9080/ready >nul 2>&1
if errorlevel 1 (
    echo X Promtail启动失败
) else (
    echo √ Promtail运行正常 (http://localhost:9080)
)

echo.
echo ==========================================
echo Promtail启动完成
echo ==========================================
echo.
echo 服务地址:
echo   - Loki: http://localhost:3100
echo   - Promtail: http://localhost:9080
echo.
echo 查看日志:
echo   docker logs -f promtail
echo   docker logs -f loki
echo.
echo 查看Targets:
echo   curl http://localhost:9080/targets
echo.
echo 停止服务:
echo   docker-compose -f docker-compose.promtail.yml down
echo.

pause
