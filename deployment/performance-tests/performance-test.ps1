# ================================================================================================
# BaseBackend 性能测试脚本 (PowerShell)
# ================================================================================================
#
# 用途：对 BaseBackend API 进行基础性能压测
#
# 使用方法：
# .\performance-test.ps1 [-TargetUrl "http://localhost:8080"] [-Concurrency 10] [-TotalRequests 1000]
#
# 示例：
# .\performance-test.ps1
# .\performance-test.ps1 -TargetUrl "http://localhost:8080" -Concurrency 20 -TotalRequests 2000
#
# ================================================================================================

param(
    [string]$TargetUrl = "http://localhost:8080",
    [int]$Concurrency = 10,
    [int]$TotalRequests = 1000
)

Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host " BaseBackend 性能测试" -ForegroundColor Cyan
Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "目标 URL: $TargetUrl"
Write-Host "并发数: $Concurrency"
Write-Host "总请求数: $TotalRequests"
Write-Host ""

# ========================================
# 1. 健康检查端点测试
# ========================================
Write-Host "【1/5】健康检查端点性能测试" -ForegroundColor Green
Write-Host "测试端点: $TargetUrl/actuator/health"
Write-Host ""

$startTime = Get-Date
$successCount = 0
$totalTime = 0

for ($i = 1; $i -le 100; $i++) {
    try {
        $response = Measure-Command {
            $result = Invoke-WebRequest -Uri "$TargetUrl/actuator/health" -UseBasicParsing -TimeoutSec 5
        }
        $successCount++
        $totalTime += $response.TotalMilliseconds
    }
    catch {
        # 忽略错误
    }
}

$endTime = Get-Date
$duration = ($endTime - $startTime).TotalMilliseconds
$avgTime = if ($successCount -gt 0) { $totalTime / $successCount } else { 0 }

Write-Host "完成请求: $successCount/100"
Write-Host "总耗时: $([math]::Round($duration, 2))ms"
Write-Host "平均响应时间: $([math]::Round($avgTime, 2))ms"
Write-Host "QPS: $([math]::Round(($successCount / ($duration / 1000)), 2))"

Write-Host ""
Write-Host "------------------------------------------------------------------------------------------------"
Write-Host ""

# ========================================
# 2. Metrics 端点测试
# ========================================
Write-Host "【2/5】Metrics 端点性能测试" -ForegroundColor Green
Write-Host "测试端点: $TargetUrl/actuator/prometheus"
Write-Host ""

$startTime = Get-Date
$successCount = 0
$totalTime = 0

for ($i = 1; $i -le 50; $i++) {
    try {
        $response = Measure-Command {
            $result = Invoke-WebRequest -Uri "$TargetUrl/actuator/prometheus" -UseBasicParsing -TimeoutSec 10
        }
        $successCount++
        $totalTime += $response.TotalMilliseconds
    }
    catch {
        # 忽略错误
    }
}

$endTime = Get-Date
$duration = ($endTime - $startTime).TotalMilliseconds
$avgTime = if ($successCount -gt 0) { $totalTime / $successCount } else { 0 }

Write-Host "完成请求: $successCount/50"
Write-Host "总耗时: $([math]::Round($duration, 2))ms"
Write-Host "平均响应时间: $([math]::Round($avgTime, 2))ms"

Write-Host ""
Write-Host "------------------------------------------------------------------------------------------------"
Write-Host ""

# ========================================
# 3. API 端点响应时间测试
# ========================================
Write-Host "【3/5】API 响应时间测试" -ForegroundColor Green
Write-Host "测试 API 端点的平均响应时间..."
Write-Host ""

$endpoints = @(
    "/actuator/health",
    "/actuator/health/liveness",
    "/actuator/health/readiness",
    "/actuator/metrics"
)

foreach ($endpoint in $endpoints) {
    Write-Host -NoNewline "测试: $endpoint ... "

    $totalTime = 0
    $successCount = 0

    for ($i = 1; $i -le 10; $i++) {
        try {
            $response = Measure-Command {
                $result = Invoke-WebRequest -Uri "$TargetUrl$endpoint" -UseBasicParsing -TimeoutSec 5
            }
            $totalTime += $response.TotalMilliseconds
            $successCount++
        }
        catch {
            # 忽略错误
        }
    }

    if ($successCount -gt 0) {
        $avgTime = $totalTime / $successCount
        Write-Host "平均响应时间: $([math]::Round($avgTime, 2))ms" -ForegroundColor Green
    }
    else {
        Write-Host "失败" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "------------------------------------------------------------------------------------------------"
Write-Host ""

# ========================================
# 4. 并发压力测试
# ========================================
Write-Host "【4/5】并发压力测试" -ForegroundColor Green
Write-Host "模拟 $Concurrency 个并发请求..."
Write-Host ""

$jobs = @()
$startTime = Get-Date

for ($i = 1; $i -le $Concurrency; $i++) {
    $job = Start-Job -ScriptBlock {
        param($url, $count)
        $success = 0
        $failed = 0

        for ($j = 1; $j -le $count; $j++) {
            try {
                $result = Invoke-WebRequest -Uri "$url/actuator/health" -UseBasicParsing -TimeoutSec 5
                if ($result.StatusCode -eq 200) {
                    $success++
                }
                else {
                    $failed++
                }
            }
            catch {
                $failed++
            }
        }

        return @{
            Success = $success
            Failed  = $failed
        }
    } -ArgumentList $TargetUrl, ($TotalRequests / $Concurrency)

    $jobs += $job
}

# 等待所有任务完成
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

$endTime = Get-Date
$duration = ($endTime - $startTime).TotalSeconds

$totalSuccess = ($results | Measure-Object -Property Success -Sum).Sum
$totalFailed = ($results | Measure-Object -Property Failed -Sum).Sum

Write-Host "【测试结果汇总】"
Write-Host "完成请求: $totalSuccess"
Write-Host "失败请求: $totalFailed"
Write-Host "总耗时: $([math]::Round($duration, 2))s"
Write-Host "QPS: $([math]::Round($totalSuccess / $duration, 2))"
Write-Host "平均响应时间: $([math]::Round(($duration * 1000) / $totalSuccess, 2))ms"

Write-Host ""
Write-Host "------------------------------------------------------------------------------------------------"
Write-Host ""

# ========================================
# 5. Metrics 验证
# ========================================
Write-Host "【5/5】验证 Metrics 数据" -ForegroundColor Green
Write-Host "检查 Prometheus metrics 是否正常暴露..."
Write-Host ""

try {
    $metricsResponse = Invoke-WebRequest -Uri "$TargetUrl/actuator/prometheus" -UseBasicParsing -TimeoutSec 10
    $metricsContent = $metricsResponse.Content

    if ($metricsContent -match "jvm_memory_used_bytes") {
        Write-Host "✓ JVM 内存指标存在" -ForegroundColor Green
    }
    else {
        Write-Host "✗ JVM 内存指标缺失" -ForegroundColor Red
    }

    if ($metricsContent -match "api_calls_total") {
        Write-Host "✓ API 调用指标存在" -ForegroundColor Green
    }
    else {
        Write-Host "⚠ API 调用指标缺失（可能未调用过 API）" -ForegroundColor Yellow
    }

    if ($metricsContent -match "business_users_total") {
        Write-Host "✓ 业务指标存在" -ForegroundColor Green
    }
    else {
        Write-Host "⚠ 业务指标缺失（可能未初始化）" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "✗ 无法获取 Metrics 数据" -ForegroundColor Red
    Write-Host "  错误: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host "性能测试完成！" -ForegroundColor Green
Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步："
Write-Host "1. 查看 Grafana 仪表板: http://localhost:3000 (admin/admin123)"
Write-Host "2. 查看 Prometheus: http://localhost:9090"
Write-Host "3. 检查应用日志中的慢接口告警"
Write-Host ""
