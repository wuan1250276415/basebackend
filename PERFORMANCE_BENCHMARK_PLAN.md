# BaseBackend 性能基准测试方案

> 用于验证优化效果的性能测试指南 🚀

**创建日期:** 2025-11-13
**创建人:** 浮浮酱（猫娘工程师）
**状态:** 📋 待执行

---

## 📊 测试目标

### 1. 验证优化效果

**优化前 vs 优化后对比:**

| 指标 | 优化前 | 优化后目标 | 提升幅度 |
|------|--------|-----------|----------|
| **读操作 QPS** | 100 | 300+ | +200% |
| **主库 CPU 使用率** | 80% | <35% | -60% |
| **查询响应时间 (P95)** | 150ms | <80ms | -47% |
| **Gateway 并发连接** | 100 | 500+ | +400% |
| **连接获取时间** | ~100ms | <50ms | -50% |

### 2. 压力测试目标

- **并发用户数:** 100 → 500 → 1000
- **持续时间:** 10 分钟
- **成功率:** >99%
- **错误率:** <1%

---

## 🔧 测试工具选择

### 推荐工具组合

#### 1. JMeter (推荐用于功能测试)

**优点:**
- ✅ 图形化界面,易于上手
- ✅ 丰富的插件生态
- ✅ 支持分布式测试
- ✅ 详细的性能报告

**安装:**
```bash
# Windows
choco install jmeter

# 或手动下载
# https://jmeter.apache.org/download_jmeter.cgi
```

#### 2. wrk (推荐用于压力测试)

**优点:**
- ✅ 轻量级,性能极高
- ✅ 支持 Lua 脚本
- ✅ 结果直观

**安装:**
```bash
# Windows (使用 WSL)
sudo apt install wrk

# macOS
brew install wrk
```

#### 3. Apache Bench (ab)

**优点:**
- ✅ 简单快速
- ✅ 适合快速验证

**安装:**
```bash
# 通常系统自带
ab -V
```

---

## 📋 测试场景设计

### 场景 1: 数据库读写分离性能测试

**测试目的:** 验证读写分离是否有效提升查询性能

#### 测试 1.1: 纯查询压力测试

**使用 wrk:**
```bash
# 测试查询接口 (GET /api/users)
wrk -t4 -c100 -d30s --latency http://localhost:8080/api/users

# 参数说明:
# -t4: 4 个线程
# -c100: 100 个并发连接
# -d30s: 持续 30 秒
# --latency: 显示延迟分布
```

**预期结果:**
```
Running 30s test @ http://localhost:8080/api/users
  4 threads and 100 connections

Requests/sec:   3000+        # QPS 应该 >3000
Latency        50.00ms       # P50 延迟 <50ms
  50%    30ms
  75%    50ms
  90%    80ms
  99%   150ms

✅ 成功标准:
   - QPS >3000
   - P95 延迟 <80ms
   - 错误率 <1%
```

**查看日志验证:**
- 日志中应该看到大量 "当前数据源: SLAVE"
- 主库连接数应保持较低水平
- 从库连接数应增加

#### 测试 1.2: 读写混合测试

**使用 JMeter:**

创建测试计划:
```
线程组 (1000 users, Ramp-up: 60s)
├── HTTP Request 1: GET /api/users (70% 权重)
├── HTTP Request 2: POST /api/users (20% 权重)
└── HTTP Request 3: PUT /api/users/{id} (10% 权重)
```

**配置步骤:**
1. 添加线程组
   - 线程数: 1000
   - Ramp-up 时间: 60 秒
   - 循环次数: 永久 (持续 10 分钟)

2. 添加 Throughput Controller
   - 查询请求: 70% 权重
   - 创建请求: 20% 权重
   - 更新请求: 10% 权重

3. 添加监听器
   - 聚合报告
   - 图形结果
   - 查看结果树

**预期结果:**
```
查询接口:
  - 平均响应时间: <50ms
  - 90% Line: <80ms
  - 吞吐量: >2000 req/s
  - 错误率: <0.5%

写入接口:
  - 平均响应时间: <100ms
  - 90% Line: <150ms
  - 吞吐量: >500 req/s
  - 错误率: <1%
```

---

### 场景 2: Gateway 限流功能测试

**测试目的:** 验证限流配置是否生效

#### 测试 2.1: 登录接口限流 (5 req/s)

**使用 wrk:**
```bash
# 快速发送大量登录请求
wrk -t2 -c10 -d10s --script=login.lua http://localhost:8180/api/auth/login
```

**login.lua 脚本:**
```lua
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"username":"test","password":"123456"}'
```

**预期结果:**
```
Requests/sec:   10-15        # 大部分请求被限流
Non-2xx responses: 50-70%    # 50-70% 返回 429

✅ 成功标准:
   - 大量 429 Too Many Requests 响应
   - 实际通过率接近配置的限流值 (5 req/s)
```

#### 测试 2.2: 不同 API 限流独立性

**并发测试多个接口:**
```bash
# 终端 1: 登录接口 (5 req/s)
wrk -t2 -c10 -d30s --script=login.lua http://localhost:8180/api/auth/login

# 终端 2: 注册接口 (3 req/s)
wrk -t2 -c10 -d30s --script=register.lua http://localhost:8180/api/auth/register

# 终端 3: 普通接口 (100 req/s)
wrk -t4 -c50 -d30s http://localhost:8180/api/users
```

**验证:**
- 各接口限流独立生效
- 互不影响
- Redis 中有对应的限流 Key

---

### 场景 3: 连接池性能测试

**测试目的:** 验证 Druid 连接池优化效果

#### 测试 3.1: 连接获取时间测试

**使用 JMeter:**

创建高并发测试:
```
线程组 (500 users, Ramp-up: 10s)
└── HTTP Request: GET /api/users/1
```

**监控指标:**
1. 访问 Druid 监控: `http://localhost:8080/druid`
2. 查看 "数据源" 页面
   - 活跃连接峰值: 应 <200
   - 等待线程数: 应 =0
   - 平均获取连接时间: 应 <50ms

**预期结果:**
```
连接池配置 (优化后):
  - initial-size: 20
  - min-idle: 15
  - max-active: 200

压测结果:
  - 活跃连接峰值: ~150
  - 等待线程数: 0
  - 平均获取连接时间: 30-40ms

✅ 成功标准:
   - 无连接等待
   - 获取连接时间 <50ms
   - 无连接泄漏
```

---

### 场景 4: 慢查询监控测试

**测试目的:** 验证慢查询监控功能

#### 测试 4.1: 触发慢查询

**手动执行慢查询:**
```sql
-- 连接数据库
mysql -h 1.117.67.222 -P 3306 -u basebackend_admin -p

-- 执行慢查询 (模拟)
SELECT SLEEP(2);  -- 睡眠 2 秒

-- 或执行复杂查询
SELECT * FROM sys_user
WHERE username LIKE '%test%'
  AND create_time > DATE_SUB(NOW(), INTERVAL 1 YEAR)
ORDER BY create_time DESC;
```

**查看监控:**
```bash
# 1. 调用慢查询 API
curl http://localhost:8080/api/database/slow-sql/top?topN=10

# 2. 查看 Druid 监控
# 访问: http://localhost:8080/druid
# 点击 "SQL监控" -> 查看慢 SQL (红色标记)
```

**预期结果:**
```json
{
  "total": 1,
  "topN": 10,
  "data": [
    {
      "sqlTemplate": "SELECT SLEEP(?)",
      "executionCount": 1,
      "totalTime": 2000,
      "averageTime": 2000,
      "maxTime": 2000,
      "minTime": 2000
    }
  ]
}
```

---

### 场景 5: 稳定性测试 (Soak Test)

**测试目的:** 验证长时间运行稳定性

#### 测试 5.1: 7x24 小时压力测试

**使用 JMeter:**

```xml
<jmeterTestPlan>
  <hashTree>
    <ThreadGroup>
      <stringProp name="ThreadGroup.num_threads">200</stringProp>
      <stringProp name="ThreadGroup.ramp_time">120</stringProp>
      <boolProp name="ThreadGroup.scheduler">true</boolProp>
      <stringProp name="ThreadGroup.duration">86400</stringProp>  <!-- 24小时 -->
      <stringProp name="ThreadGroup.delay">0</stringProp>
    </ThreadGroup>
  </hashTree>
</jmeterTestPlan>
```

**监控项:**
1. **应用监控** (每小时检查)
   - JVM Heap 内存使用率
   - GC 频率和耗时
   - 线程数变化

2. **数据库监控**
   - 连接数变化
   - 慢查询数量
   - 连接泄漏检测

3. **Gateway 监控**
   - 请求成功率
   - 平均响应时间
   - 限流触发次数

**成功标准:**
```
✅ 24 小时内:
   - 无内存泄漏 (Heap 稳定)
   - 无连接泄漏 (连接数稳定)
   - 成功率 >99.9%
   - 响应时间稳定 (波动 <10%)
   - 无系统崩溃或重启
```

---

## 📊 性能基准测试报告模板

### 测试环境

```yaml
服务器配置:
  CPU: Intel Core i7 / 4 核心 8 线程
  内存: 16GB
  磁盘: SSD 256GB
  操作系统: Windows 11 / Ubuntu 20.04

数据库:
  主库: MySQL 8.0 (1.117.67.222:3306)
  从库: MySQL 8.0 (192.168.66.126:3307)

应用配置:
  JVM Heap: -Xms2g -Xmx4g
  连接池最大连接: 200
  Gateway 限流: 100 req/s (默认)
```

### 测试结果记录表

#### 1. 读操作性能测试

| 并发数 | QPS | 平均响应时间 | P95 | P99 | 错误率 | 主库连接 | 从库连接 |
|--------|-----|-------------|-----|-----|--------|----------|----------|
| 100    |     |             |     |     |        |          |          |
| 300    |     |             |     |     |        |          |          |
| 500    |     |             |     |     |        |          |          |
| 1000   |     |             |     |     |        |          |          |

#### 2. 写操作性能测试

| 并发数 | QPS | 平均响应时间 | P95 | P99 | 错误率 | 主库连接 |
|--------|-----|-------------|-----|-----|--------|----------|
| 50     |     |             |     |     |        |          |
| 100    |     |             |     |     |        |          |
| 200    |     |             |     |     |        |          |

#### 3. 读写混合测试

| 读写比例 | 总 QPS | 读 QPS | 写 QPS | 平均响应时间 | 错误率 |
|---------|--------|--------|--------|-------------|--------|
| 7:3     |        |        |        |             |        |
| 5:5     |        |        |        |             |        |
| 3:7     |        |        |        |             |        |

---

## 🔍 性能分析工具

### 1. JVM 性能分析

**使用 Arthas:**
```bash
# 下载 Arthas
wget https://arthas.aliyun.com/arthas-boot.jar

# 启动
java -jar arthas-boot.jar

# 常用命令
dashboard          # 实时仪表盘
thread             # 线程分析
jvm                # JVM 信息
gc                 # GC 统计
heapdump           # Heap dump
```

### 2. 数据库性能分析

**慢查询分析:**
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

-- 查看慢查询
SELECT * FROM mysql.slow_log
ORDER BY start_time DESC
LIMIT 10;
```

**连接分析:**
```sql
-- 查看当前连接
SHOW PROCESSLIST;

-- 查看连接统计
SHOW STATUS LIKE 'Threads%';
SHOW STATUS LIKE 'Connections';
```

### 3. 实时监控

**使用 Prometheus + Grafana:**

**启动 Prometheus:**
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'basebackend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']

  - job_name: 'gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8180']
```

**关键监控指标:**
- `mybatis_slow_sql_count_total` - 慢查询总数
- `mybatis_sql_execution_time_seconds` - SQL 执行时间
- `gateway_request_duration_seconds` - Gateway 响应时间
- `jvm_memory_used_bytes` - JVM 内存使用
- `http_server_requests_seconds` - HTTP 请求响应时间

---

## 📝 测试检查清单

### 测试前准备

- [ ] 应用已启动并通过健康检查
- [ ] Nacos 配置已加载
- [ ] 主从数据库连接正常
- [ ] Redis 连接正常
- [ ] 监控工具配置完成 (Prometheus/Grafana)
- [ ] 测试数据已准备
- [ ] 测试脚本已验证
- [ ] 备份重要数据

### 测试执行

- [ ] 场景 1: 数据库读写分离测试
- [ ] 场景 2: Gateway 限流测试
- [ ] 场景 3: 连接池性能测试
- [ ] 场景 4: 慢查询监控测试
- [ ] 场景 5: 稳定性测试 (可选)

### 测试后验证

- [ ] 查看应用日志,无ERROR
- [ ] 查看 Druid 监控,连接池正常
- [ ] 查看慢查询统计
- [ ] 检查数据一致性
- [ ] 分析性能报告
- [ ] 记录测试结果
- [ ] 对比优化目标

---

## 🚀 快速开始

### 步骤 1: 运行自动化验证脚本

```powershell
# Windows PowerShell
.\scripts\verify-all.ps1
```

### 步骤 2: 使用 wrk 快速压测

```bash
# 查询接口压测
wrk -t4 -c100 -d30s --latency http://localhost:8080/api/users

# 登录接口限流测试
wrk -t2 -c10 -d10s --script=scripts/login.lua http://localhost:8180/api/auth/login
```

### 步骤 3: 查看监控数据

```bash
# Druid 监控
open http://localhost:8080/druid

# 慢查询统计
curl http://localhost:8080/api/database/slow-sql/top?topN=10

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

---

**创建人:** 浮浮酱 🐱
**更新时间:** 2025-11-13

---

主人,这就是详细的性能基准测试方案喵～ φ(≧ω≦*)♪

按照这个方案执行测试,可以全面验证优化效果,并为未来的性能优化提供数据支持呢！✨
