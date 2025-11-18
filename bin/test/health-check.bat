@echo off
REM ============================================================================
REM BaseBackend 服务健康检查脚本 (Windows)
REM ============================================================================
REM 功能：检查所有微服务的健康状态
REM 使用：health-check.bat
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================
echo   BaseBackend 服务健康检查
echo ========================================
echo.

set total=0
set healthy=0

REM 检查网关
set /a total+=1
call :check_service "API网关" 8080
if !errorlevel! equ 0 set /a healthy+=1

REM 检查用户服务
set /a total+=1
call :check_service "用户服务" 8081
if !errorlevel! equ 0 set /a healthy+=1

REM 检查系统服务
set /a total+=1
call :check_service "系统服务" 8082
if !errorlevel! equ 0 set /a healthy+=1

REM 检查认证服务
set /a total+=1
call :check_service "认证服务" 8083
if !errorlevel! equ 0 set /a healthy+=1

REM 检查通知服务
set /a total+=1
call :check_service "通知服务" 8086
if !errorlevel! equ 0 set /a healthy+=1

REM 检查可观测性服务
set /a total+=1
call :check_service "可观测性服务" 8087
if !errorlevel! equ 0 set /a healthy+=1

echo.
echo ========================================
echo   健康检查结果: !healthy!/!total! 服务正常
echo ========================================
echo.

if !healthy! equ !total! (
    echo [✓] 所有服务运行正常
    exit /b 0
) else (
    echo [✗] 部分服务异常，请检查日志
    exit /b 1
)

REM ============================================================================
REM 检查服务函数
REM ============================================================================
:check_service
set service_name=%~1
set port=%~2

curl -s http://localhost:%port%/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [✗] %service_name% (:%port%) - 不健康
    exit /b 1
) else (
    echo [✓] %service_name% (:%port%) - 健康
    exit /b 0
)
