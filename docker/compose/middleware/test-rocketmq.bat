@echo off
REM RocketMQ连接测试脚本 (Windows)

setlocal enabledelayedexpansion

set NAMESRV_ADDR=localhost:9876

echo ==========================================
echo RocketMQ连接测试
echo ==========================================
echo NameServer: %NAMESRV_ADDR%
echo.

REM 测试1: 检查NameServer
echo 测试1: 检查NameServer状态...
docker exec basebackend-rocketmq-namesrv sh -c "netstat -an | grep 9876" >nul 2>&1
if errorlevel 1 (
    echo X NameServer未运行
) else (
    echo √ NameServer运行正常
)

REM 测试2: 检查Broker
echo.
echo 测试2: 检查Broker状态...
docker exec basebackend-rocketmq-broker sh -c "netstat -an | grep 10911" >nul 2>&1
if errorlevel 1 (
    echo X Broker未运行
) else (
    echo √ Broker运行正常
)

REM 测试3: 查看集群信息
echo.
echo 测试3: 查看集群信息...
docker exec basebackend-rocketmq-namesrv sh mqadmin clusterList -n localhost:9876 2>nul

REM 测试4: 创建测试主题
echo.
echo 测试4: 创建测试主题...
docker exec basebackend-rocketmq-broker sh mqadmin updateTopic -n localhost:9876 -t TestTopic -c DefaultCluster 2>nul
if errorlevel 1 (
    echo ⚠ 主题创建失败
) else (
    echo √ 主题创建成功
)

echo.
echo ==========================================
echo 连接信息
echo ==========================================
echo NameServer地址: %NAMESRV_ADDR%
echo Broker地址: localhost:10911
echo Console地址: http://localhost:8180
echo.
echo Spring Boot配置：
echo   rocketmq.name-server=%NAMESRV_ADDR%
echo.
echo 查看日志：
echo   docker logs -f basebackend-rocketmq-namesrv
echo   docker logs -f basebackend-rocketmq-broker
echo.

pause
