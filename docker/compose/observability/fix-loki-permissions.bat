@echo off
REM 修复Loki权限问题

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==========================================
echo 修复Loki权限问题
echo ==========================================

REM 停止Loki容器
echo 停止Loki容器...
docker-compose down loki 2>nul

REM 删除旧的volume
echo 删除旧的Loki数据卷...
docker volume rm observability_loki-data 2>nul

REM 重新启动
echo 重新启动Loki...
docker-compose up -d loki

REM 等待启动
echo 等待Loki启动...
timeout /t 5 /nobreak >nul

REM 检查状态
curl -s http://localhost:3100/ready >nul 2>&1
if errorlevel 1 (
    echo X Loki启动失败，查看日志：
    docker logs basebackend-loki
    exit /b 1
) else (
    echo √ Loki启动成功
)

echo.
echo ==========================================
echo 权限问题已修复
echo ==========================================

pause
