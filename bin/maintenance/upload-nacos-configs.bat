@echo off
REM ============================================================================
REM Nacos配置上传脚本 (Windows)
REM ============================================================================
REM 功能：将config/nacos目录下的配置文件上传到Nacos配置中心
REM 使用：upload-nacos-configs.bat [nacos-server-url]
REM ============================================================================

setlocal enabledelayedexpansion

REM 设置Nacos服务器地址
if "%~1"=="" (
    set NACOS_SERVER=http://localhost:8848
) else (
    set NACOS_SERVER=%~1
)

set NACOS_USERNAME=nacos
set NACOS_PASSWORD=nacos
set GROUP=DEFAULT_GROUP
set CONFIG_DIR=config\nacos

echo.
echo ========================================
echo   Nacos配置上传脚本
echo ========================================
echo.
echo Nacos服务器: %NACOS_SERVER%
echo 配置目录: %CONFIG_DIR%
echo.

REM 检查curl是否可用
curl --version >nul 2>&1
if errorlevel 1 (
    echo [错误] curl未安装或未配置到PATH
    echo 请安装curl或使用Git Bash执行.sh脚本
    pause
    exit /b 1
)

REM 检查配置目录
if not exist "%CONFIG_DIR%" (
    echo [错误] 配置目录不存在: %CONFIG_DIR%
    pause
    exit /b 1
)

REM 上传配置文件
set count=0
for %%f in (%CONFIG_DIR%\*.yml) do (
    set /a count+=1
    echo [!count!] 上传配置: %%~nxf
    
    REM 读取文件内容（使用PowerShell）
    for /f "delims=" %%i in ('powershell -Command "Get-Content '%%f' -Raw"') do set content=%%i
    
    REM 上传到Nacos
    curl -X POST "%NACOS_SERVER%/nacos/v1/cs/configs" ^
        -d "dataId=%%~nxf" ^
        -d "group=%GROUP%" ^
        -d "content=!content!" ^
        -d "type=yaml" ^
        -d "username=%NACOS_USERNAME%" ^
        -d "password=%NACOS_PASSWORD%" ^
        >nul 2>&1
    
    if errorlevel 1 (
        echo    [✗] 上传失败
    ) else (
        echo    [✓] 上传成功
    )
)

echo.
echo ========================================
echo   配置上传完成！共上传 !count! 个文件
echo ========================================
echo.
echo 访问Nacos控制台查看配置：
echo %NACOS_SERVER%/nacos
echo.
pause
