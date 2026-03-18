# ================================================================================================
# Nacos 配置导入脚本 (PowerShell)
# ================================================================================================
#
# 使用说明：
# 1. 确保 Nacos 服务已启动（默认地址：http://localhost:8848/nacos）
# 2. 在 PowerShell 中执行此脚本：.\import-nacos-configs.ps1
# 3. 可选参数：
#    .\import-nacos-configs.ps1 -NacosServer "localhost:8848" -Namespace "public"
#
# ================================================================================================

param(
    [string]$NacosServer = "localhost:8848",
    [string]$Namespace = "public",
    [string]$Username = "nacos",
    [string]$Password = "nacos",
    [string]$Group = "DEFAULT_GROUP",
    [string]$ConfigDir = (Join-Path $PSScriptRoot "dev"),
    [int]$NacosWaitTimeoutSeconds = 90,
    [int]$NacosWaitIntervalSeconds = 3
)

$AccessToken = $null

Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host " Nacos 配置导入脚本" -ForegroundColor Cyan
Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Nacos 服务器: http://$NacosServer"
Write-Host "命名空间: $Namespace"
Write-Host "分组: $Group"
Write-Host "配置目录: $ConfigDir"
Write-Host ""

# 检查配置目录是否存在
if (-not (Test-Path $ConfigDir)) {
    Write-Host "错误: 配置目录不存在: $ConfigDir" -ForegroundColor Red
    exit 1
}

function Get-Message {
    param(
        [object]$Response
    )

    if ($null -ne $Response -and $null -ne $Response.message) {
        return $Response.message
    }

    return ($Response | Out-String).Trim()
}

function Wait-ForNacos {
    $Deadline = (Get-Date).AddSeconds($NacosWaitTimeoutSeconds)
    $HealthUrl = "http://$NacosServer/nacos/"

    while ((Get-Date) -lt $Deadline) {
        try {
            $Response = Invoke-WebRequest -Uri $HealthUrl -Method Get -UseBasicParsing -TimeoutSec 5
            if ($Response.StatusCode -ge 200 -and $Response.StatusCode -lt 400) {
                return $true
            }
        }
        catch {
        }

        Start-Sleep -Seconds $NacosWaitIntervalSeconds
    }

    Write-Host "错误: 等待 Nacos 就绪超时（$NacosWaitTimeoutSeconds 秒）" -ForegroundColor Red
    return $false
}

function Ensure-AccessToken {
    $LoginUrl = "http://$NacosServer/nacos/v3/auth/user/login"
    $AdminInitUrl = "http://$NacosServer/nacos/v3/auth/user/admin"

    try {
        $LoginResponse = Invoke-RestMethod -Uri $LoginUrl -Method Post -Body @{
            username = $Username
            password = $Password
        } -ContentType "application/x-www-form-urlencoded; charset=UTF-8"

        if ($null -ne $LoginResponse.accessToken) {
            $script:AccessToken = $LoginResponse.accessToken
            return $true
        }
    }
    catch {
    }

    Write-Host "首次登录未获取到 accessToken，尝试初始化管理员密码..." -ForegroundColor Yellow

    try {
        [void](Invoke-RestMethod -Uri $AdminInitUrl -Method Post -Body @{
            password = $Password
        } -ContentType "application/x-www-form-urlencoded; charset=UTF-8")
    }
    catch {
        Write-Host "管理员初始化未成功，继续尝试登录..." -ForegroundColor Yellow
    }

    try {
        $LoginResponse = Invoke-RestMethod -Uri $LoginUrl -Method Post -Body @{
            username = $Username
            password = $Password
        } -ContentType "application/x-www-form-urlencoded; charset=UTF-8"

        if ($null -ne $LoginResponse.accessToken) {
            $script:AccessToken = $LoginResponse.accessToken
            return $true
        }
    }
    catch {
        Write-Host "错误: 无法获取 Nacos accessToken" -ForegroundColor Red
        Write-Host "  错误: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }

    Write-Host "错误: 无法获取 Nacos accessToken" -ForegroundColor Red
    return $false
}

# 导入配置文件的函数
function Import-Config {
    param(
        [string]$DataId
    )

    $ConfigFile = Join-Path $ConfigDir $DataId

    if (-not (Test-Path $ConfigFile)) {
        Write-Host "✗ 跳过（文件不存在）: $DataId" -ForegroundColor Red
        return $false
    }

    Write-Host -NoNewline "导入: $DataId ... "

    try {
        # 读取配置文件内容
        $Content = Get-Content -Path $ConfigFile -Raw -Encoding UTF8

        # 构建请求体
        $Body = @{
            dataId      = $DataId
            groupName   = $Group
            namespaceId = $Namespace
            content     = $Content
            type        = "yaml"
        }

        # 发送请求到 Nacos
        $Url = "http://$NacosServer/nacos/v3/admin/cs/config"
        $Headers = @{
            accessToken = $script:AccessToken
        }
        $Response = Invoke-RestMethod -Uri $Url -Method Post -Headers $Headers -Body $Body -ContentType "application/x-www-form-urlencoded; charset=UTF-8"

        if ($Response.code -eq 0 -and $Response.data -eq $true) {
            Write-Host "✓ 成功" -ForegroundColor Green
            return $true
        }
        else {
            Write-Host "✗ 失败" -ForegroundColor Red
            Write-Host "  响应: $(Get-Message -Response $Response)" -ForegroundColor Yellow
            return $false
        }
    }
    catch {
        Write-Host "✗ 失败" -ForegroundColor Red
        Write-Host "  错误: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

# 导入所有配置文件
Write-Host "等待 Nacos 就绪..." -ForegroundColor Cyan
if (-not (Wait-ForNacos)) {
    exit 1
}

Write-Host "✓ Nacos 已就绪" -ForegroundColor Green
Write-Host ""

Write-Host "检查 Nacos 认证状态..." -ForegroundColor Cyan
if (-not (Ensure-AccessToken)) {
    exit 1
}

Write-Host "✓ 已获取 accessToken" -ForegroundColor Green
Write-Host ""
Write-Host "开始导入配置..." -ForegroundColor Cyan
Write-Host ""

$Total = 0
$Success = 0
$Failed = 0

$Configs = @(
    "common-config.yml",
    "mysql-config.yml",
    "redis-config.yml",
    "rocketmq-config.yml",
    "observability-config.yml",
    "security-config.yml",
    "seata-config.yml"
)

foreach ($Config in $Configs) {
    $Total++
    if (Import-Config -DataId $Config) {
        $Success++
    }
    else {
        $Failed++
    }
}

Write-Host ""
Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host " 导入完成" -ForegroundColor Cyan
Write-Host "================================================================================================" -ForegroundColor Cyan
Write-Host "总计: $Total | " -NoNewline
Write-Host "成功: $Success" -ForegroundColor Green -NoNewline
Write-Host " | " -NoNewline
Write-Host "失败: $Failed" -ForegroundColor Red
Write-Host ""

if ($Failed -eq 0) {
    Write-Host "✓ 所有配置导入成功！" -ForegroundColor Green
    Write-Host ""
    Write-Host "下一步："
    Write-Host "1. 访问 Nacos 控制台验证配置: http://$NacosServer/nacos"
    Write-Host "2. 重启应用程序以加载 Nacos 配置"
    exit 0
}
else {
    Write-Host "⚠ 部分配置导入失败，请检查错误信息" -ForegroundColor Yellow
    exit 1
}
