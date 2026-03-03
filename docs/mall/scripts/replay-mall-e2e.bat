@echo off
REM 商城事件回放脚本（Windows）
REM 场景：success / fail / timeout / all

setlocal enabledelayedexpansion

set "BASE_URL=http://127.0.0.1:8080"
set "SCENARIO=all"
set "FAIL_REASON=MOCK_PAY_FAILED"
set "WAIT_SECONDS=1"
set "CURL_TIMEOUT=20"

if "%~1"=="" goto after_parse_args

:parse_args
if "%~1"=="" goto after_parse_args

if /i "%~1"=="--base-url" (
  if "%~2"=="" (
    echo [ERROR] --base-url 缺少参数
    goto usage_fail
  )
  set "BASE_URL=%~2"
  shift
  shift
  goto parse_args
)

if /i "%~1"=="--scenario" (
  if "%~2"=="" (
    echo [ERROR] --scenario 缺少参数
    goto usage_fail
  )
  set "SCENARIO=%~2"
  shift
  shift
  goto parse_args
)

if /i "%~1"=="--reason" (
  if "%~2"=="" (
    echo [ERROR] --reason 缺少参数
    goto usage_fail
  )
  set "FAIL_REASON=%~2"
  shift
  shift
  goto parse_args
)

if /i "%~1"=="--wait-seconds" (
  if "%~2"=="" (
    echo [ERROR] --wait-seconds 缺少参数
    goto usage_fail
  )
  set "WAIT_SECONDS=%~2"
  shift
  shift
  goto parse_args
)

if /i "%~1"=="-h" goto usage_ok
if /i "%~1"=="--help" goto usage_ok

echo [ERROR] 未知参数：%~1
goto usage_fail

:after_parse_args
if "%BASE_URL:~-1%"=="/" set "BASE_URL=%BASE_URL:~0,-1%"

echo %WAIT_SECONDS%| findstr /r "^[0-9][0-9]*$" >nul
if errorlevel 1 (
  echo [ERROR] --wait-seconds 必须是非负整数
  exit /b 1
)

if /i "%SCENARIO%"=="success" goto scenario_ok
if /i "%SCENARIO%"=="fail" goto scenario_ok
if /i "%SCENARIO%"=="timeout" goto scenario_ok
if /i "%SCENARIO%"=="all" goto scenario_ok
echo [ERROR] --scenario 仅支持 success ^| fail ^| timeout ^| all
exit /b 1

:scenario_ok
echo [INFO] 网关地址：%BASE_URL%
echo [INFO] 回放场景：%SCENARIO%

if /i "%SCENARIO%"=="success" (
  call :replay_success_flow
  if errorlevel 1 exit /b 1
)
if /i "%SCENARIO%"=="fail" (
  call :replay_fail_flow
  if errorlevel 1 exit /b 1
)
if /i "%SCENARIO%"=="timeout" (
  call :replay_timeout_flow
  if errorlevel 1 exit /b 1
)
if /i "%SCENARIO%"=="all" (
  call :replay_success_flow
  if errorlevel 1 exit /b 1
  call :replay_fail_flow
  if errorlevel 1 exit /b 1
  call :replay_timeout_flow
  if errorlevel 1 exit /b 1
)
echo [WARN] 回放结束。建议执行 docs/mall/mall-e2e-debug-runbook.md 中 SQL 进行落库核对。
exit /b 0

:replay_success_flow
set "ITEMS_SUCCESS=[{\"skuId\":10001,\"quantity\":1},{\"skuId\":10002,\"quantity\":1}]"
set "ORDER_NO="
echo [INFO] 开始回放：成功链路
call :submit_order 10001 198.00 "%ITEMS_SUCCESS%" ORDER_NO
if errorlevel 1 exit /b 1
echo [INFO] 成功链路下单完成，orderNo=!ORDER_NO!
call :sleep_if_needed

set "RESP_FILE=%TEMP%\mall_replay_success_%RANDOM%%RANDOM%.json"
call :http_post_no_body "%BASE_URL%/api/mall/payments/mock-success/!ORDER_NO!" "%RESP_FILE%"
if errorlevel 1 exit /b 1
call :assert_success_result "模拟支付成功" "%RESP_FILE%"
if errorlevel 1 exit /b 1
del /q "%RESP_FILE%" >nul 2>&1
echo [INFO] 成功链路回放完成，orderNo=!ORDER_NO!
exit /b 0

:replay_fail_flow
set "ITEMS_FAIL=[{\"skuId\":10001,\"quantity\":1}]"
set "ORDER_NO="
set "ENCODED_REASON="
echo [INFO] 开始回放：失败链路
call :submit_order 10002 99.00 "%ITEMS_FAIL%" ORDER_NO
if errorlevel 1 exit /b 1
echo [INFO] 失败链路下单完成，orderNo=!ORDER_NO!
call :url_encode "%FAIL_REASON%" ENCODED_REASON
if errorlevel 1 exit /b 1
call :sleep_if_needed

set "RESP_FILE=%TEMP%\mall_replay_fail_%RANDOM%%RANDOM%.json"
call :http_post_no_body "%BASE_URL%/api/mall/payments/mock-fail/!ORDER_NO!?reason=!ENCODED_REASON!" "%RESP_FILE%"
if errorlevel 1 exit /b 1
call :assert_success_result "模拟支付失败" "%RESP_FILE%"
if errorlevel 1 exit /b 1
del /q "%RESP_FILE%" >nul 2>&1
echo [INFO] 失败链路回放完成，orderNo=!ORDER_NO!, reason=%FAIL_REASON%
exit /b 0

:replay_timeout_flow
set "ITEMS_TIMEOUT=[{\"skuId\":10002,\"quantity\":1}]"
set "ORDER_NO="
echo [INFO] 开始回放：超时链路
call :submit_order 10003 129.00 "%ITEMS_TIMEOUT%" ORDER_NO
if errorlevel 1 exit /b 1
echo [INFO] 超时链路下单完成，orderNo=!ORDER_NO!
call :sleep_if_needed

set "RESP_FILE=%TEMP%\mall_replay_timeout_%RANDOM%%RANDOM%.json"
call :http_post_no_body "%BASE_URL%/api/mall/trades/orders/!ORDER_NO!/timeout-close" "%RESP_FILE%"
if errorlevel 1 exit /b 1
call :assert_success_result "手工触发超时关单" "%RESP_FILE%"
if errorlevel 1 exit /b 1
del /q "%RESP_FILE%" >nul 2>&1
echo [INFO] 超时链路回放完成，orderNo=!ORDER_NO!
exit /b 0

:submit_order
set "USER_ID=%~1"
set "PAY_AMOUNT=%~2"
set "ITEMS_JSON=%~3"
set "OUT_VAR=%~4"
set "ORDER_NO="

set "REQ_FILE=%TEMP%\mall_submit_req_%RANDOM%%RANDOM%.json"
set "RESP_FILE=%TEMP%\mall_submit_resp_%RANDOM%%RANDOM%.json"

(
  echo {
  echo   "userId": %USER_ID%,
  echo   "payAmount": %PAY_AMOUNT%,
  echo   "items": %ITEMS_JSON%
  echo }
) > "%REQ_FILE%"

call :http_post_json "%BASE_URL%/api/mall/trades/orders/submit" "%REQ_FILE%" "%RESP_FILE%"
if errorlevel 1 (
  del /q "%REQ_FILE%" "%RESP_FILE%" >nul 2>&1
  exit /b 1
)

call :assert_success_result "提交订单" "%RESP_FILE%"
if errorlevel 1 (
  del /q "%REQ_FILE%" "%RESP_FILE%" >nul 2>&1
  exit /b 1
)

set "RESP_FILE_PS=%RESP_FILE%"
for /f "usebackq delims=" %%i in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $j = Get-Content -Raw -LiteralPath $env:RESP_FILE_PS | ConvertFrom-Json; if ($null -ne $j.data.orderNo) { $j.data.orderNo } } catch { '' }"`) do set "ORDER_NO=%%i"
if "!ORDER_NO!"=="" (
  echo [ERROR] 提交订单成功但未解析到 orderNo
  type "%RESP_FILE%"
  del /q "%REQ_FILE%" "%RESP_FILE%" >nul 2>&1
  exit /b 1
)

set "%OUT_VAR%=!ORDER_NO!"
del /q "%REQ_FILE%" "%RESP_FILE%" >nul 2>&1
exit /b 0

:http_post_json
set "URL=%~1"
set "REQ_FILE=%~2"
set "RESP_FILE=%~3"

curl -sS --connect-timeout 5 --max-time %CURL_TIMEOUT% -X POST "%URL%" -H "Content-Type: application/json" --data-binary "@%REQ_FILE%" > "%RESP_FILE%"
if errorlevel 1 (
  echo [ERROR] 请求失败：POST %URL%
  exit /b 1
)
exit /b 0

:http_post_no_body
set "URL=%~1"
set "RESP_FILE=%~2"

curl -sS --connect-timeout 5 --max-time %CURL_TIMEOUT% -X POST "%URL%" > "%RESP_FILE%"
if errorlevel 1 (
  echo [ERROR] 请求失败：POST %URL%
  exit /b 1
)
exit /b 0

:assert_success_result
set "ACTION_NAME=%~1"
set "RESP_FILE=%~2"
set "RESULT_CODE="

set "RESP_FILE_PS=%RESP_FILE%"
for /f "usebackq delims=" %%i in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "try { $j = Get-Content -Raw -LiteralPath $env:RESP_FILE_PS | ConvertFrom-Json; if ($null -ne $j.code) { $j.code } } catch { '' }"`) do set "RESULT_CODE=%%i"
if not "%RESULT_CODE%"=="200" (
  echo [ERROR] %ACTION_NAME%失败，返回码=%RESULT_CODE%
  type "%RESP_FILE%"
  exit /b 1
)
exit /b 0

:url_encode
set "RAW_TEXT=%~1"
set "OUT_VAR=%~2"
set "ENCODED_TEXT="
set "RAW_TEXT_PS=%RAW_TEXT%"

for /f "usebackq delims=" %%i in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "[uri]::EscapeDataString($env:RAW_TEXT_PS)"`) do set "ENCODED_TEXT=%%i"
if "%ENCODED_TEXT%"=="" (
  echo [ERROR] reason URL 编码失败
  exit /b 1
)
set "%OUT_VAR%=%ENCODED_TEXT%"
exit /b 0

:sleep_if_needed
if "%WAIT_SECONDS%"=="0" exit /b 0
timeout /t %WAIT_SECONDS% /nobreak >nul
exit /b 0

:usage_ok
call :usage
exit /b 0

:usage_fail
call :usage
exit /b 1

:usage
echo.
echo 商城事件回放脚本（Windows）
echo.
echo 用法：
echo   replay-mall-e2e.bat [选项]
echo.
echo 选项：
echo   --base-url ^<url^>       网关地址，默认 http://127.0.0.1:8080
echo   --scenario ^<name^>      回放场景：success ^| fail ^| timeout ^| all（默认 all）
echo   --reason ^<text^>        失败场景 reason 参数，默认 MOCK_PAY_FAILED
echo   --wait-seconds ^<num^>   每步请求间隔秒数，默认 1
echo   -h, --help               查看帮助
echo.
echo 示例：
echo   replay-mall-e2e.bat --scenario success
echo   replay-mall-e2e.bat --scenario fail --reason MANUAL_FAIL
echo   replay-mall-e2e.bat --scenario all --base-url http://127.0.0.1:8080
echo.
exit /b 0
