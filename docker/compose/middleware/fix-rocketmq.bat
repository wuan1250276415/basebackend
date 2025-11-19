@echo off
REM RocketMQ快速修复脚本 (Windows)

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ==========================================
echo RocketMQ快速修复
echo ==========================================

REM 停止服务
echo 1. 停止RocketMQ服务...
docker-compose down rocketmq-broker rocketmq-namesrv 2>nul

REM 询问是否清理数据
set /p CLEAN="是否清理旧数据？这将删除所有消息 (y/N): "
if /i "%CLEAN%"=="y" (
    echo 2. 清理旧数据...
    docker volume rm middleware_rocketmq-broker-data 2>nul
    docker volume rm middleware_rocketmq-namesrv-data 2>nul
) else (
    echo 2. 保留旧数据
)

REM 重新启动
echo 3. 启动NameServer...
docker-compose up -d rocketmq-namesrv

echo 4. 等待NameServer启动...
timeout /t 10 /nobreak >nul

echo 5. 启动Broker...
docker-compose up -d rocketmq-broker

echo 6. 等待Broker启动...
timeout /t 10 /nobreak >nul

REM 检查状态
echo.
echo 7. 检查服务状态...

docker exec basebackend-rocketmq-namesrv sh -c "netstat -an | grep 9876" >nul 2>&1
if errorlevel 1 (
    echo X NameServer启动失败
    echo 查看日志：
    docker logs basebackend-rocketmq-namesrv
) else (
    echo √ NameServer运行正常
)

docker exec basebackend-rocketmq-broker sh -c "netstat -an | grep 10911" >nul 2>&1
if errorlevel 1 (
    echo X Broker启动失败
    echo 查看日志：
    docker logs basebackend-rocketmq-broker
    pause
    exit /b 1
) else (
    echo √ Broker运行正常
)

REM 启动Console
echo.
echo 8. 启动Console...
docker-compose up -d rocketmq-console

echo.
echo ==========================================
echo RocketMQ修复完成
echo ==========================================
echo.
echo 服务地址：
echo   NameServer: localhost:9876
echo   Broker: localhost:10911
echo   Console: http://localhost:8180
echo.
echo 运行测试：
echo   test-rocketmq.bat
echo.

pause
