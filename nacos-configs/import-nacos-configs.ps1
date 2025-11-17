# ================================================================================================
# Nacos 配置导入脚本 (PowerShell)
# ================================================================================================
#
# 使用说明：
# 1. 确保 Nacos 服务已启动（默认地址：http://localhost:8848/nacos）
# 2. 在 PowerShell 中执行此脚本：.\import-nacos-configs.ps1
# 3. 可选参数：
#    .\import-nacos-configs.ps1 -NacosServer "192.168.66.126:8848" -Namespace "dev"
#
# ================================================================================================

param(
    [string]$NacosServer = "192.168.66.126:8848",
    [string]$Namespace = "public",
    [string]$Username = "nacos",
    [string]$Password = "nacos"
)

$Group = "DEFAULT_GROUP"
$ConfigDir = Join-Path $PSScriptRoot "dev"

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

# 导入配置文件的函数
function Import-Config {
    param(
        [string]$DataId,
        [string]$ConfigType = "yaml",
        [string]$ConfigGroup = $Group
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
            dataId   = $DataId
            group    = $ConfigGroup
            content  = $Content
            type     = $ConfigType
            tenant   = $Namespace
            username = $Username
            password = $Password
        }

        # 发送请求到 Nacos
        $Url = "http://$NacosServer/nacos/v1/cs/configs"
        $Response = Invoke-RestMethod -Uri $Url -Method Post -Body $Body -ContentType "application/x-www-form-urlencoded; charset=UTF-8"

        if ($Response -eq "true" -or $Response -eq $true) {
            Write-Host "✓ 成功" -ForegroundColor Green
            return $true
        }
        else {
            Write-Host "✗ 失败" -ForegroundColor Red
            Write-Host "  响应: $Response" -ForegroundColor Yellow
            return $false
        }
    }
    catch {
        Write-Host "✗ 失败" -ForegroundColor Red
        Write-Host "  错误: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

# 导入 Sentinel 规则的函数
function Import-SentinelRule {
    param(
        [string]$DataId
    )

    $ConfigFile = Join-Path $PSScriptRoot $DataId

    if (-not (Test-Path $ConfigFile)) {
        Write-Host "✗ 跳过（文件不存在）: $DataId" -ForegroundColor Red
        return $false
    }

    Write-Host -NoNewline "导入 Sentinel 规则: $DataId ... "

    try {
        # 读取配置文件内容
        $Content = Get-Content -Path $ConfigFile -Raw -Encoding UTF8

        # 构建请求体
        $Body = @{
            dataId   = $DataId
            group    = "SENTINEL_GROUP"
            content  = $Content
            type     = "json"
            tenant   = $Namespace
            username = $Username
            password = $Password
        }

        # 发送请求到 Nacos
        $Url = "http://$NacosServer/nacos/v1/cs/configs"
        $Response = Invoke-RestMethod -Uri $Url -Method Post -Body $Body -ContentType "application/x-www-form-urlencoded; charset=UTF-8"

        if ($Response -eq "true" -or $Response -eq $true) {
            Write-Host "✓ 成功" -ForegroundColor Green
            return $true
        }
        else {
            Write-Host "✗ 失败" -ForegroundColor Red
            Write-Host "  响应: $Response" -ForegroundColor Yellow
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
Write-Host "------------------------------------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host " 导入 Sentinel 规则" -ForegroundColor Cyan
Write-Host "------------------------------------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host ""

$SentinelRules = @(
    "basebackend-gateway-flow-rules.json",
    "basebackend-gateway-degrade-rules.json",
    "basebackend-gateway-gw-flow-rules.json",
    "admin-api-flow-rules.json",
    "admin-api-degrade-rules.json",
    "admin-api-param-flow-rules.json",
    "admin-api-system-rules.json",
    "admin-api-authority-rules.json"
)

foreach ($Rule in $SentinelRules) {
    $Total++
    if (Import-SentinelRule -DataId $Rule) {
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
