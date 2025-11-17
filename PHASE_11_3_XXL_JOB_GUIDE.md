# Phase 11.3: XXL-Job 分布式任务调度实施指南

## 📋 概述

XXL-Job 是由大众点评开源的轻量级分布式任务调度框架，本指南介绍如何部署和使用 XXL-Job 实现分布式定时任务管理。

---

## 🏗️ XXL-Job 架构

### 架构图

```
┌────────────────────────────────────────────────────────────────┐
│                    XXL-Job 分布式调度架构                       │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌─────────────┐         ┌─────────────┐                      │
│  │  调度中心    │◄────────│  执行器集群  │                      │
│  │(XXL-Job)    │         │(JobHandler) │                      │
│  │ 端口: 8080  │         │ 多个实例     │                      │
│  └──────┬──────┘         └──────┬──────┘                      │
│         │                        │                            │
│  ┌──────▼──────┐         ┌──────▼──────┐                      │
│  │   MySQL     │         │   微服务     │                      │
│  │  任务注册    │         │   业务逻辑   │                      │
│  │  任务执行    │         │   任务执行   │                      │
│  └─────────────┘         └─────────────┘                      │
│         │                        │                            │
│  ┌──────▼──────┐         ┌──────▼──────┐                      │
│  │   Redis     │         │  注册中心    │                      │
│  │  失败重试    │         │ (Nacos)     │                      │
│  │  子任务     │         │             │                      │
│  └─────────────┘         └─────────────┘                      │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │                    任务路由模式                             │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │ • RPC 调用模式 (推荐) - 通过 XXL-RPC 调用 JobHandler        │ │
│  │ • GLUE 模式 - 在调度中心维护 Java/Shell/Python 代码        │ │
│  │ • BEAN 模式 (推荐) - 在微服务中编写 JobHandler            │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### 核心组件

| 组件 | 说明 | 端口 |
|------|------|------|
| **调度中心** | 负责任务调度、监控、管理 | 8080 |
| **执行器** | 负责任务执行，可在微服务中集成 | 8081+ |
| **任务处理器** | 具体的业务逻辑实现 | - |

---

## 🚀 快速开始

### 1. 下载 XXL-Job

```bash
# 下载源码
git clone https://github.com/xuxueli/xxl-job.git

# 或下载发行版
wget https://github.com/xuxueli/xxl-job/releases/download/2.4.0/xxl-job-2.4.0.tar.gz
tar -xzf xxl-job-2.4.0.tar.gz
```

### 2. 部署调度中心

使用 Docker Compose 快速部署：

```yaml
# docker-compose-xxljob.yml
version: '3.8'

services:
  xxl-job-server:
    image: xuxueli/xxl-job-admin:2.4.0
    container_name: xxl-job-server
    ports:
      - "8080:8080"
    environment:
      - PARAMS=--spring.datasource.username=root
      - PARAMS=--spring.datasource.password=123456
      - PARAMS=--spring.datasource.url=jdbc:mysql://mysql-xxljob:3306/xxl_job?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
    volumes:
      - ./xxljob-logs:/data/applogs/xxl-job
    depends_on:
      - mysql-xxljob
    networks:
      - xxljob-network

  mysql-xxljob:
    image: mysql:8.0
    container_name: mysql-xxljob
    ports:
      - "3308:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: xxl_job
    volumes:
      - ./xxljob-db:/docker-entrypoint-initdb.d
    networks:
      - xxljob-network

  xxl-job-executor:
    image: xxl-job-executor-sample-springboot:2.4.0
    container_name: xxl-job-executor
    ports:
      - "8081:8081"
    environment:
      - PARAMS=--xxl.job.admin.addresses=http://xxl-job-server:8080/xxl-job-admin
      - PARAMS=--xxl.job.executor.appname=basebackend-executor
      - PARAMS=--xxl.job.executor.port=8081
    networks:
      - xxljob-network

networks:
  xxljob-network:
    driver: bridge
```

### 3. 启动服务

```bash
# 启动服务
docker-compose -f docker-compose-xxljob.yml up -d

# 查看日志
docker logs -f xxl-job-server

# 访问控制台
# http://localhost:8080/xxl-job-admin
# 用户名: admin
# 密码: 123456
```

---

## 📦 微服务集成 XXL-Job

### 1. 添加依赖

在微服务的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

### 2. 配置 application.yml

```yaml
xxl:
  job:
    # 调度中心配置
    admin:
      addresses: http://localhost:8080/xxl-job-admin

    # 执行器配置
    executor:
      appname: basebackend-user-executor
      port: 8081
      logpath: /data/applogs/xxl-job
      logretentiondays: 30

    # 访问令牌
    accessToken: basebackend_xxl_job_token_2024

    # 任务执行器配置
    triggerpool:
      fast:
        max: 200
      slow:
        max: 100

    # 任务配置
    job:
      # 失败重试次数
      failover: 2
      # 超时时间 (分钟)
      timeout: 30
      # 并发数量
      concurrency: 3
```

### 3. 创建配置类

```java
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        return xxlJobSpringExecutor;
    }
}
```

---

## 📝 任务处理器开发

### 1. Bean 模式（推荐）

在微服务中直接编写任务处理器：

```java
@Component
public class UserDataSyncJob {

    private static final Logger log = LoggerFactory.getLogger(UserDataSyncJob.class);

    /**
     * 1. 简单任务示例（Bean模式）
     */
    @XxlJob("syncUserData")
    public void syncUserData() throws Exception {
        log.info("开始执行用户数据同步任务");

        // 1. 记录任务开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 2. 执行具体的业务逻辑
            syncUserDataFromExternal();

            // 3. 记录任务完成
            log.info("用户数据同步任务执行成功，耗时: {}ms", System.currentTimeMillis() - startTime);

            // 4. 返回成功
            XxlJobHelper.handleSuccess("用户数据同步完成");

        } catch (Exception e) {
            log.error("用户数据同步任务执行失败", e);
            // 返回失败，触发重试
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    /**
     * 2. 周期性任务示例（支持cron表达式）
     */
    @XxlJob("periodicDataCleanup")
    public void periodicDataCleanup() throws Exception {
        log.info("开始执行周期性数据清理任务");

        try {
            // 清理过期数据
            cleanupExpiredData();

            // 清理临时文件
            cleanupTempFiles();

            // 更新统计信息
            updateStatistics();

            XxlJobHelper.handleSuccess("数据清理完成");

        } catch (Exception e) {
            log.error("数据清理任务失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    /**
     * 3. 分片任务示例（分布式处理）
     */
    @XxlJob("shardingDataProcess")
    public void shardingDataProcess() throws Exception {
        // 获取分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        log.info("分片任务执行: 分片索引={}/{}", shardIndex, shardTotal);

        try {
            // 根据分片参数处理对应的数据
            processDataByShard(shardIndex, shardTotal);

            XxlJobHelper.handleSuccess("分片任务执行成功");

        } catch (Exception e) {
            log.error("分片任务失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    /**
     * 4. 子任务示例（任务依赖）
     */
    @XxlJob("mainDataProcess")
    public void mainDataProcess() throws Exception {
        log.info("开始执行主任务");

        try {
            // 主任务逻辑
            processMainData();

            // 触发子任务
            XxlJobHelper.triggerSubJob("subDataProcess1", "param1");
            XxlJobHelper.triggerSubJob("subDataProcess2", "param2");
            XxlJobHelper.triggerSubJob("subDataProcess3", "param3");

            XxlJobHelper.handleSuccess("主任务执行完成，子任务已触发");

        } catch (Exception e) {
            log.error("主任务失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    /**
     * 5. 广播任务示例（所有节点执行）
     */
    @XxlJob("broadcastConfigRefresh")
    public void broadcastConfigRefresh() throws Exception {
        log.info("执行配置刷新任务");

        try {
            // 刷新配置
            refreshConfiguration();

            // 清理缓存
            clearCache();

            // 重新初始化连接
            reinitializeConnections();

            XxlJobHelper.handleSuccess("配置刷新完成");

        } catch (Exception e) {
            log.error("配置刷新失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    // ========================================
    // 业务逻辑方法
    // ========================================

    private void syncUserDataFromExternal() throws InterruptedException {
        log.info("正在同步用户数据...");

        // 模拟数据同步
        for (int i = 0; i < 100; i++) {
            Thread.sleep(10);
            log.debug("同步进度: {}/100", i + 1);
        }

        log.info("用户数据同步完成");
    }

    private void cleanupExpiredData() throws InterruptedException {
        log.info("清理过期数据...");

        // 模拟清理操作
        for (int i = 0; i < 50; i++) {
            Thread.sleep(10);
        }

        log.info("过期数据清理完成");
    }

    private void cleanupTempFiles() {
        log.info("清理临时文件...");
        // 清理临时文件逻辑
        log.info("临时文件清理完成");
    }

    private void updateStatistics() {
        log.info("更新统计信息...");
        // 更新统计信息逻辑
        log.info("统计信息更新完成");
    }

    private void processDataByShard(int shardIndex, int shardTotal) throws InterruptedException {
        log.info("根据分片处理数据: shardIndex={}, shardTotal={}", shardIndex, shardTotal);

        // 模拟分片处理
        for (int i = shardIndex; i < 1000; i += shardTotal) {
            Thread.sleep(5);
            log.debug("处理数据: {}", i);
        }

        log.info("分片数据处理完成");
    }

    private void processMainData() throws InterruptedException {
        log.info("执行主任务逻辑...");

        // 模拟主任务执行
        for (int i = 0; i < 50; i++) {
            Thread.sleep(20);
        }

        log.info("主任务执行完成");
    }

    private void refreshConfiguration() {
        log.info("刷新配置...");
        // 配置刷新逻辑
        log.info("配置刷新完成");
    }

    private void clearCache() {
        log.info("清理缓存...");
        // 缓存清理逻辑
        log.info("缓存清理完成");
    }

    private void reinitializeConnections() {
        log.info("重新初始化连接...");
        // 连接重初始化逻辑
        log.info("连接重新初始化完成");
    }
}
```

### 2. 任务注解说明

```java
/**
 * @XxlJob 注解属性说明
 *
 * value: 任务名称
 * init: 任务初始化方法（可选）
 * destroy: 任务销毁方法（可选）
 * jobMethod: 任务执行方法名（可选）
 */
@XxlJob(value = "syncUserData", init = "initJob", destroy = "destroyJob")
public void syncUserData() {
    // 任务逻辑
}

public void initJob() {
    // 任务初始化时执行
    log.info("任务初始化");
}

public void destroyJob() {
    // 任务销毁时执行
    log.info("任务销毁");
}
```

---

## 📊 业务任务迁移

### 1. 数据统计任务

```java
@Component
public class StatisticsJob {

    /**
     * 每日数据统计任务
     */
    @XxlJob("dailyStatisticsJob")
    public void dailyStatistics() {
        log.info("开始执行每日数据统计");

        try {
            // 统计用户数据
            statUserStatistics();
            // 统计订单数据
            statOrderStatistics();
            // 统计系统指标
            statSystemMetrics();

            XxlJobHelper.handleSuccess("每日统计完成");
        } catch (Exception e) {
            log.error("每日统计失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    /**
     * 每周数据汇总任务
     */
    @XxlJob("weeklySummaryJob")
    public void weeklySummary() {
        log.info("开始执行每周数据汇总");

        try {
            // 汇总周报数据
            generateWeeklyReport();
            // 清理历史数据
            cleanupHistoricalData();
            // 更新趋势分析
            updateTrendAnalysis();

            XxlJobHelper.handleSuccess("每周汇总完成");
        } catch (Exception e) {
            log.error("每周汇总失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    private void statUserStatistics() {
        log.info("统计用户数据...");
        // 统计用户增长、活跃度等
    }

    private void statOrderStatistics() {
        log.info("统计订单数据...");
        // 统计订单量、金额等
    }

    private void statSystemMetrics() {
        log.info("统计系统指标...");
        // 统计 QPS、响应时间等
    }

    private void generateWeeklyReport() {
        log.info("生成周报...");
        // 生成周报
    }

    private void cleanupHistoricalData() {
        log.info("清理历史数据...");
        // 清理过期数据
    }

    private void updateTrendAnalysis() {
        log.info("更新趋势分析...");
        // 更新趋势分析
    }
}
```

### 2. 缓存预热任务

```java
@Component
public class CacheWarmupJob {

    /**
     * 缓存预热任务
     */
    @XxlJob("cacheWarmupJob")
    public void warmupCache() {
        log.info("开始执行缓存预热");

        try {
            // 预热用户缓存
            warmupUserCache();
            // 预热菜单缓存
            warmupMenuCache();
            // 预热权限缓存
            warmupPermissionCache();

            XxlJobHelper.handleSuccess("缓存预热完成");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    /**
     * 分片预热任务
     */
    @XxlJob("shardingCacheWarmupJob")
    public void shardingWarmupCache() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        log.info("分片预热缓存: {}/{}", shardIndex, shardTotal);

        try {
            // 根据分片预热对应的缓存
            warmupCacheByShard(shardIndex, shardTotal);

            XxlJobHelper.handleSuccess("分片预热完成");
        } catch (Exception e) {
            log.error("分片预热失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    private void warmupUserCache() {
        log.info("预热用户缓存...");
        // 预热用户缓存
    }

    private void warmupMenuCache() {
        log.info("预热菜单缓存...");
        // 预热菜单缓存
    }

    private void warmupPermissionCache() {
        log.info("预热权限缓存...");
        // 预热权限缓存
    }

    private void warmupCacheByShard(int shardIndex, int shardTotal) {
        log.info("根据分片预热缓存: {}/{}", shardIndex, shardTotal);
        // 分片预热逻辑
    }
}
```

### 3. 日志清理任务

```java
@Component
public class LogCleanupJob {

    /**
     * 日志清理任务
     */
    @XxlJob("logCleanupJob")
    public void cleanupLogs() {
        log.info("开始执行日志清理");

        try {
            // 清理应用日志
            cleanupApplicationLogs();
            // 清理审计日志
            cleanupAuditLogs();
            // 清理错误日志
            cleanupErrorLogs();

            XxlJobHelper.handleSuccess("日志清理完成");
        } catch (Exception e) {
            log.error("日志清理失败", e);
            XxlJobHelper.handleFail(e.getMessage());
            throw e;
        }
    }

    private void cleanupApplicationLogs() {
        log.info("清理应用日志...");
        // 清理应用日志文件
    }

    private void cleanupAuditLogs() {
        log.info("清理审计日志...");
        // 清理审计日志数据
    }

    private void cleanupErrorLogs() {
        log.info("清理错误日志...");
        // 清理错误日志数据
    }
}
```

---

## 📈 任务监控与告警

### 1. 任务监控

```java
@RestController
@RequestMapping("/api/monitor/xxljob")
public class XxlJobMonitorController {

    @Autowired
    private XxlJobMonitor xxlJobMonitor;

    /**
     * 获取任务执行统计
     */
    @GetMapping("/stats")
    public Result<XxlJobStats> getStats() {
        XxlJobStats stats = xxlJobMonitor.getStatistics();
        return Result.success(stats);
    }

    /**
     * 获取任务执行历史
     */
    @GetMapping("/history")
    public Result<List<XxlJobExecution>> getHistory(
            @RequestParam(required = false) String jobName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<XxlJobExecution> history = xxlJobMonitor.getExecutionHistory(jobName, pageNum, pageSize);
        return Result.success(history);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<XxlJobHealth> checkHealth() {
        XxlJobHealth health = new XxlJobHealth();

        // 检查调度中心连接
        boolean adminConnected = xxlJobMonitor.checkAdminConnection();
        health.setAdminStatus(adminConnected ? "UP" : "DOWN");

        // 检查执行器状态
        boolean executorActive = xxlJobMonitor.isExecutorActive();
        health.setExecutorStatus(executorActive ? "UP" : "DOWN");

        // 检查任务执行情况
        int recentFailures = xxlJobMonitor.getRecentFailures();
        health.setRecentFailures(recentFailures);

        if (adminConnected && executorActive && recentFailures == 0) {
            health.setOverallStatus("UP");
            return Result.success(health);
        } else {
            health.setOverallStatus("DEGRADED");
            return Result.failed("XXL-Job 健康检查异常");
        }
    }
}
```

### 2. 告警配置

在 XXL-Job 控制台配置告警：

1. **调度中心 > 系统设置 > 告警配置**
   - 邮箱告警
   - 钉钉告警
   - 企业微信告警
   - Webhook 告警

2. **任务配置 > 告警设置**
   - 任务失败告警
   - 任务超时告警
   - 任务触发告警

---

## 🧪 测试与验证

### 1. 任务测试脚本

```bash
#!/bin/bash
# test-xxljob.sh

# 测试任务执行
curl -X POST "http://localhost:8080/xxl-job-admin/jobinfo/trigger" \
  -d "id=1" \
  -d "executorParam=syncUserData"

# 查看任务日志
curl "http://localhost:8080/xxl-job-admin/joblog/logDetailPage" \
  -d "logId=123"
```

### 2. 压测脚本

```bash
#!/bin/bash
# stress-test-xxljob.sh

# 并发触发多个任务
for i in {1..10}; do
  (
    curl -X POST "http://localhost:8080/xxl-job-admin/jobinfo/trigger" \
      -d "id=1" \
      -d "executorParam=test_$i"
  ) &
done

wait
echo "所有任务已触发"
```

---

## ⚠️ 常见问题

### 1. 执行器注册失败

**现象**: 控制台显示执行器离线

**解决方案**:
```yaml
# 检查配置
xxl:
  job:
    executor:
      appname: unique_app_name  # 确保应用名唯一
    accessToken: correct_token  # 确保 Token 正确
```

### 2. 任务执行失败

**现象**: 任务一直失败

**解决方案**:
- 检查任务代码是否有异常
- 检查数据库连接是否正常
- 检查 XXL-Job 版本兼容性
- 查看任务日志定位具体错误

### 3. 分片任务不均匀

**现象**: 分片任务执行不均匀

**解决方案**:
- 确保所有执行器实例都正常运行
- 检查分片总数配置是否一致
- 使用固定分片数量避免动态变化

### 4. 任务超时

**现象**: 任务执行超时

**解决方案**:
```yaml
xxl:
  job:
    job:
      timeout: 60  # 增加超时时间
```

---

## 📊 性能优化

### 1. 任务执行器优化

```yaml
xxl:
  job:
    triggerpool:
      fast:
        max: 200  # 快速触发器线程池大小
      slow:
        max: 100  # 慢速触发器线程池大小
```

### 2. 数据库优化

```sql
-- 为 xxl_job_log 表添加索引
ALTER TABLE xxl_job_log ADD INDEX idx_jobid_trigger_time (job_id, trigger_time);
ALTER TABLE xxl_job_log ADD INDEX idx_trigger_code (trigger_code);
```

### 3. 监控优化

```java
// 自定义任务执行监控
@Component
public class XxlJobMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter jobExecutionCounter;
    private final Timer jobExecutionTimer;

    public XxlJobMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.jobExecutionCounter = Counter.builder("xxl_job_execution_total")
            .description("XXL-Job 任务执行总数")
            .register(meterRegistry);
        this.jobExecutionTimer = Timer.builder("xxl_job_execution_duration")
            .description("XXL-Job 任务执行时长")
            .register(meterRegistry);
    }

    public void recordJobExecution(String jobName, Duration duration, boolean success) {
        jobExecutionCounter.increment(
            Tags.of("job", jobName, "status", success ? "success" : "failure")
        );
        jobExecutionTimer.record(duration,
            Tags.of("job", jobName, "status", success ? "success" : "failure")
        );
    }
}
```

---

## 📚 参考资料

1. [XXL-Job 官方文档](https://www.xuxueli.com/xxl-job/)
2. [XXL-Job GitHub](https://github.com/xuxueli/xxl-job)
3. [分布式任务调度框架对比](https://tech.meituan.com/2018/11/22/xxl-job/)

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ 分布式任务调度即将完成！** ฅ'ω'ฅ
