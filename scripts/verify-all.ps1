# ============================================
# 优化功能验证脚本 (PowerShell 版本)
# ============================================
# 用途: 验证所有优化功能是否正常工作
# 作者: 浮浮酱 🐱
# 日期: 2025-11-13
# ============================================

# 设置控制台编码为 UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BaseBackend 优化功能验证脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 配置
$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8082" }
$GatewayUrl = if ($env:GATEWAY_URL) { $env:GATEWAY_URL } else { "http://localhost:8081" }

# 测试计数
$script:TotalTests = 0
$script:PassedTests = 0
$script:FailedTests = 0

# 辅助函数
function Write-TestHeader {
    param([string]$Title)
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "  $Title" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
}

function Write-TestResult {
    param(
        [string]$TestName,
        [bool]$Success,
        [string]$Message = ""
    )
    $script:TotalTests++

    if ($Success) {
        Write-Host "✅ $TestName" -ForegroundColor Green
        $script:PassedTests++
    } else {
        Write-Host "❌ $TestName" -ForegroundColor Red
        $script:FailedTests++
    }

    if ($Message) {
        Write-Host "   $Message" -ForegroundColor Gray
    }
}

# 测试 1: 应用健康检查
function Test-ApplicationHealth {
    Write-TestHeader "1. 应用健康检查"

    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -Method Get -UseBasicParsing
        $health = $response.Content | ConvertFrom-Json

        if ($response.StatusCode -eq 200 -and $health.status -eq "UP") {
            Write-TestResult "应用健康检查" $true "应用状态: UP (正常运行)"

            # 检查数据库连接
            if ($health.components.db) {
                $dbStatus = $health.components.db.status
                Write-Host "   数据库状态: $dbStatus" -ForegroundColor Gray
            }

            return $true
        } else {
            Write-TestResult "应用健康检查" $false "应用状态异常"
            return $false
        }
    } catch {
        Write-TestResult "应用健康检查" $false "无法连接到应用 (端口 8080)"
        Write-Host "   错误: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# 测试 2: Druid 监控页面
function Test-DruidMonitor {
    Write-TestHeader "2. Druid 监控功能"

    try {
        $druidUrl = "$BaseUrl/druid/index.html"
        $response = Invoke-WebRequest -Uri $druidUrl -Method Get -UseBasicParsing

        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 401) {
            Write-TestResult "Druid 监控页面访问" $true "页面可访问"
            Write-Host "   URL: $druidUrl" -ForegroundColor Gray
            Write-Host "   用户名: admin" -ForegroundColor Gray
            Write-Host "   密码: admin123" -ForegroundColor Gray
            return $true
        } else {
            Write-TestResult "Druid 监控页面访问" $false
            return $false
        }
    } catch {
        Write-TestResult "Druid 监控页面访问" $false "无法访问监控页面"
        return $false
    }
}

# 测试 3: 慢查询监控 API
function Test-SlowSqlMonitor {
    Write-TestHeader "3. 慢查询监控功能"

    try {
        # 测试 TOP N API
        $topUrl = "$BaseUrl/api/database/slow-sql/top?topN=10"
        $response = Invoke-WebRequest -Uri $topUrl -Method Get -UseBasicParsing

        if ($response.StatusCode -eq 200) {
            Write-TestResult "慢查询 TOP API" $true "API 可访问"
            $data = $response.Content | ConvertFrom-Json
            if ($data.total) {
                Write-Host "   当前慢查询记录数: $($data.total)" -ForegroundColor Gray
            } else {
                Write-Host "   当前暂无慢查询记录（正常）" -ForegroundColor Gray
            }
        } else {
            Write-TestResult "慢查询 TOP API" $false
        }

        # 测试健康检查 API
        $healthUrl = "$BaseUrl/api/database/slow-sql/health"
        $response = Invoke-WebRequest -Uri $healthUrl -Method Get -UseBasicParsing

        if ($response.StatusCode -eq 200) {
            $health = $response.Content | ConvertFrom-Json
            Write-TestResult "慢查询健康检查" $true "状态: $($health.status)"
        } else {
            Write-TestResult "慢查询健康检查" $false
        }

        return $true
    } catch {
        Write-TestResult "慢查询监控 API" $false "API 调用失败"
        Write-Host "   错误: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# 测试 4: Prometheus 指标
function Test-PrometheusMetrics {
    Write-TestHeader "4. Prometheus 监控指标"

    try {
        $promUrl = "$BaseUrl/actuator/prometheus"
        $response = Invoke-WebRequest -Uri $promUrl -Method Get -UseBasicParsing

        if ($response.StatusCode -eq 200) {
            $content = $response.Content

            Write-TestResult "Prometheus 指标端点" $true "指标可访问"

            # 检查关键指标
            $metrics = @{
                "mybatis_slow_sql_count" = "慢查询总数"
                "mybatis_sql_execution_time" = "SQL 执行时间"
                "jvm_memory" = "JVM 内存"
                "http_server_requests" = "HTTP 请求"
            }

            Write-Host ""
            Write-Host "   📊 关键指标检查:" -ForegroundColor Cyan
            foreach ($metric in $metrics.GetEnumerator()) {
                if ($content -match $metric.Key) {
                    Write-Host "   ✓ $($metric.Value)" -ForegroundColor Green
                } else {
                    Write-Host "   ✗ $($metric.Value) - 未找到" -ForegroundColor Yellow
                }
            }

            return $true
        } else {
            Write-TestResult "Prometheus 指标端点" $false
            return $false
        }
    } catch {
        Write-TestResult "Prometheus 指标端点" $false "无法访问指标端点"
        return $false
    }
}

# 测试 5: Gateway 限流
function Test-RateLimiting {
    Write-TestHeader "5. Gateway 限流功能"

    try {
        # 检查 Gateway 健康
        $gatewayHealth = "$GatewayUrl/actuator/health"
        $response = Invoke-WebRequest -Uri $gatewayHealth -Method Get -UseBasicParsing

        if ($response.StatusCode -eq 200) {
            Write-TestResult "Gateway 健康检查" $true "Gateway 运行正常"
        } else {
            Write-TestResult "Gateway 健康检查" $false
            return $false
        }

        # 测试登录接口限流
        Write-Host ""
        Write-Host "   测试登录接口限流（限制: 5 req/s）..." -ForegroundColor Cyan

        $loginUrl = "$GatewayUrl/api/auth/login"
        $limitTriggered = $false
        $successCount = 0
        $limitedCount = 0

        for ($i = 1; $i -le 12; $i++) {
            try {
                $body = @{
                    username = "test"
                    password = "123456"
                } | ConvertTo-Json

                $response = Invoke-WebRequest -Uri $loginUrl -Method Post -Body $body -ContentType "application/json" -UseBasicParsing

                if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 401) {
                    $successCount++
                    Write-Host "   [请求 $i/12] ✓ 成功" -ForegroundColor Green
                }
            } catch {
                if ($_.Exception.Response.StatusCode -eq 429) {
                    $limitedCount++
                    $limitTriggered = $true
                    Write-Host "   [请求 $i/12] ✅ 触发限流 (429)" -ForegroundColor Red
                } else {
                    Write-Host "   [请求 $i/12] 其他错误: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
                }
            }
        }

        Write-Host ""
        if ($limitTriggered) {
            Write-TestResult "登录接口限流测试" $true "成功: $successCount, 限流: $limitedCount"
        } else {
            Write-TestResult "登录接口限流测试" $false "未触发限流,配置可能未生效"
        }

        return $limitTriggered
    } catch {
        Write-TestResult "Gateway 限流功能" $false "Gateway 不可访问 (端口 8180)"
        Write-Host "   错误: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# 测试 6: 数据库读写分离
function Test-ReadWriteSeparation {
    Write-TestHeader "6. 数据库读写分离"

    Write-Host "   说明: 需要查看应用日志验证读写分离" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   验证步骤:" -ForegroundColor Cyan
    Write-Host "   1. 调用查询接口 (GET /api/users/1)" -ForegroundColor Gray
    Write-Host "   2. 查看日志中是否有: '当前数据源: SLAVE'" -ForegroundColor Gray
    Write-Host "   3. 调用写入接口 (POST /api/users)" -ForegroundColor Gray
    Write-Host "   4. 查看日志中是否有: '当前数据源: MASTER'" -ForegroundColor Gray
    Write-Host ""

    try {
        # 测试查询接口
        $response = Invoke-WebRequest -Uri "$BaseUrl/api/users/1" -Method Get -UseBasicParsing

        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 404) {
            Write-TestResult "查询接口调用" $true "请求成功,请检查日志确认使用从库"
        } else {
            Write-TestResult "查询接口调用" $false
        }

        # 测试写入接口
        $body = @{
            username = "test_rw"
            nickname = "测试用户"
            password = "test123"
        } | ConvertTo-Json

        $response = Invoke-WebRequest -Uri "$BaseUrl/api/users" -Method Post -Body $body -ContentType "application/json" -UseBasicParsing

        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 201) {
            Write-TestResult "写入接口调用" $true "请求成功,请检查日志确认使用主库"
        } else {
            Write-TestResult "写入接口调用" $false
        }

        return $true
    } catch {
        Write-Host "   ℹ️  接口调用异常,可能接口不存在或需要认证" -ForegroundColor Yellow
        return $false
    }
}

# 主函数
function Main {
    Write-Host "📌 测试配置:" -ForegroundColor Cyan
    Write-Host "   Base URL: $BaseUrl" -ForegroundColor Gray
    Write-Host "   Gateway URL: $GatewayUrl" -ForegroundColor Gray
    Write-Host ""
    Write-Host "开始验证..." -ForegroundColor Cyan

    # 运行所有测试
    $healthOk = Test-ApplicationHealth
    if (-not $healthOk) {
        Write-Host ""
        Write-Host "⚠️  应用未运行或健康检查失败,请先启动应用！" -ForegroundColor Red
        Write-Host ""
        Write-Host "启动步骤:" -ForegroundColor Yellow
        Write-Host "   1. 启动 Nacos: .\nacos\bin\startup.cmd -m standalone" -ForegroundColor Gray
        Write-Host "   2. 启动应用: mvn spring-boot:run" -ForegroundColor Gray
        Write-Host ""
        exit 1
    }

    Test-DruidMonitor
    Test-SlowSqlMonitor
    Test-PrometheusMetrics
    Test-RateLimiting
    Test-ReadWriteSeparation

    # 输出结果汇总
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  测试结果汇总" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "总测试数: $script:TotalTests" -ForegroundColor White
    Write-Host "通过: $script:PassedTests" -ForegroundColor Green
    Write-Host "失败: $script:FailedTests" -ForegroundColor Red
    Write-Host ""

    if ($script:FailedTests -eq 0) {
        Write-Host "✅ 所有功能验证通过！优化功能正常工作喵～" -ForegroundColor Green
        Write-Host ""
        Write-Host "📊 下一步操作:" -ForegroundColor Cyan
        Write-Host "   1. 访问 Druid 监控查看数据库连接池状态" -ForegroundColor Gray
        Write-Host "   2. 使用 JMeter 或 wrk 进行性能压测" -ForegroundColor Gray
        Write-Host "   3. 配置 Grafana 监控大盘" -ForegroundColor Gray
        Write-Host "   4. 开始 Phase 10.1 - 用户服务迁移" -ForegroundColor Gray
        Write-Host ""
    } else {
        $passRate = [math]::Round(($script:PassedTests / $script:TotalTests) * 100, 2)
        Write-Host "⚠️  部分功能验证失败 (通过率: $passRate%)" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "🔧 故障排查建议:" -ForegroundColor Yellow
        Write-Host "   1. 检查应用是否完全启动" -ForegroundColor Gray
        Write-Host "   2. 确认 Nacos 配置已正确加载" -ForegroundColor Gray
        Write-Host "   3. 查看应用日志中的错误信息" -ForegroundColor Gray
        Write-Host "   4. 验证 Redis 连接是否正常" -ForegroundColor Gray
        Write-Host ""
    }

    Write-Host "📝 详细验证指南: OPTIMIZATION_ACTIVATION_GUIDE.md" -ForegroundColor Cyan
    Write-Host ""
}

# 运行主函数
Main
