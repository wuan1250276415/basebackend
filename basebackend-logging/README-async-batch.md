# AsyncBatchAppender 高性能异步批量日志写入器

## 📖 概述

AsyncBatchAppender 是专为 basebackend-logging 模块设计的高性能异步批量日志写入器，通过批量处理和异步I/O显著提升日志系统吞吐量，同时保证系统稳定性和可观测性。

### 核心特性

- ✅ **双触发策略**：基于批量大小和时间窗口的混合触发机制
- ✅ **有界队列**：防止内存溢出，队列满时自动丢弃最旧日志
- ✅ **动态批量调整**：根据写入延迟和队列压力自动优化批量大小
- ✅ **指数退避重试**：写入失败时使用指数退避算法进行重试
- ✅ **完整监控指标**：提供队列深度、吞吐量、失败率等关键指标
- ✅ **兼容Logback**：完美兼容现有 Logback 配置和 Spring Boot 3.1.5

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **吞吐量提升** | ≥80% | 相比同步逐条写入 |
| **内存占用** | 可控 | 有界队列防止OOM |
| **写入延迟** | <150ms | 95分位数延迟 |
| **失败恢复** | ≤3次 | 自动重试机制 |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────┐
│         AsyncBatchAppender                  │
├─────────────────────────────────────────────┤
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │         Application Logs             │  │
│  └────────────────┬─────────────────────┘  │
│                   │ append(event)           │
│                   ▼                         │
│  ┌──────────────────────────────────────┐  │
│  │     ArrayBlockingQueue (有界队列)      │  │
│  │        默认大小: 16,384                │  │
│  └────────────────┬─────────────────────┘  │
│                   │ poll()                   │
│                   ▼                         │
│  ┌──────────────────────────────────────┐  │
│  │        Worker Thread                 │  │
│  │   ┌──────────────────────────────┐   │  │
│  │   │   批量聚合 (Batch Collector)   │   │  │
│  │   │   - 批量大小触发              │   │  │
│  │   │   - 时间窗口触发              │   │  │
│  │   └────────────┬─────────────────┘   │  │
│  └────────────────┼─────────────────────┘  │
│                   │ flushBatch()            │
│                   ▼                         │
│  ┌──────────────────────────────────────┐  │
│  │      底层 Appender 链                │  │
│  │   ┌──────────┐ ┌──────────┐         │  │
│  │   │  File    │ │ Console  │ ...     │  │
│  │   │Appender  │ │Appender  │         │  │
│  │   └──────────┘ └──────────┘         │  │
│  └──────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 在 logback-spring.xml 中配置

```xml
<configuration>
    <!-- 定义文件输出Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/basebackend/application.log</file>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder"/>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/basebackend/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>7</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 使用 AsyncBatchAppender 包装文件输出 -->
    <appender name="ASYNC_BATCH_FILE" class="com.basebackend.logging.appender.AsyncBatchAppender">
        <!-- 队列配置 -->
        <queueSize>20000</queueSize>

        <!-- 批量配置 -->
        <minBatchSize>50</minBatchSize>
        <maxBatchSize>400</maxBatchSize>

        <!-- 时间窗口配置 -->
        <flushIntervalMillis>150</flushIntervalMillis>

        <!-- 重试配置 -->
        <maxRetries>4</maxRetries>
        <initialBackoffMillis>50</initialBackoffMillis>
        <maxBackoffMillis>3000</maxBackoffMillis>

        <!-- 动态调整配置 -->
        <dynamicBatchSize>true</dynamicBatchSize>
        <targetLatencyMillis>120</targetLatencyMillis>

        <!-- 同步模式（调试用） -->
        <synchronous>false</synchronous>

        <!-- 引用底层Appender -->
        <appender-ref ref="FILE"/>
    </appender>

    <!-- 全局日志级别 -->
    <root level="INFO">
        <appender-ref ref="ASYNC_BATCH_FILE"/>
    </root>

    <!-- 特定包的日志级别 -->
    <logger name="com.basebackend" level="DEBUG"/>
    <logger name="org.springframework" level="INFO"/>
</configuration>
```

### 2. 验证配置

启动应用后，检查日志：

```bash
# 查看应用日志
tail -f /var/log/basebackend/application.log

# 检查 AsyncBatchAppender 状态
# 在日志中查找类似信息：
# AsyncBatchAppender started with queueSize=20000, batch=[50,400], flushIntervalMs=150, synchronous=false, dynamicBatchSize=true
```

## ⚙️ 配置参数详解

### 队列配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `queueSize` | int | 16,384 | 有界队列容量，单位：事件数<br/>建议值：根据日志产生速率调整，通常16K-100K之间 |

### 批量配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `minBatchSize` | int | 32 | 最小批量大小，保证批量处理的最小效率 |
| `maxBatchSize` | int | 512 | 最大批量大小，批量越大吞吐越高但延迟增加 |
| `dynamicBatchSize` | boolean | true | 是否启用动态批量调整 |

### 时间窗口配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `flushIntervalMillis` | long | 200 | 刷新间隔（毫秒），时间窗口触发器<br/>即使批量未满也会强制刷新 |

### 重试配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `maxRetries` | int | 3 | 最大重试次数，写入失败时的重试策略 |
| `initialBackoffMillis` | long | 50 | 初始退避时间（毫秒），指数退避算法的起始值 |
| `maxBackoffMillis` | long | 2000 | 最大退避时间（毫秒），防止退避时间过长 |

### 动态调整配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `targetLatencyMillis` | long | 150 | 目标延迟（毫秒），用于动态调整批量大小的基准指标 |

### 调试配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `synchronous` | boolean | false | 同步模式开关，true时退化为同步直写模式，用于调试和对比测试 |

## 📊 监控指标

AsyncBatchAppender 提供了丰富的监控指标，可以通过以下方式获取：

### 1. 通过 JMX 监控

```java
// 获取AsyncBatchAppender实例
AsyncBatchAppender appender = (AsyncBatchAppender) LoggerFactory.getLogger("ASYNC_BATCH_FILE").getAppender("ASYNC_BATCH_FILE");

// 获取监控指标
long queueDepth = appender.getQueueDepth();              // 当前队列深度
long delivered = appender.getDelivered();                // 成功交付事件数
long dropped = appender.getDropped();                    // 丢弃事件数
long failed = appender.getFailed();                      // 失败事件数
long batches = appender.getBatches();                    // 已处理批量数
double writePerSecond = appender.getWritePerSecond();    // 每秒写入速率
double failureRate = appender.getFailureRate();          // 失败率
double queueUtilization = appender.getQueueUtilization(); // 队列使用率
int currentBatchSize = appender.getCurrentBatchSize();   // 当前批量大小
```

### 2. 通过 Micrometer 集成

```java
@Component
public class LoggingMetrics {

    private final AsyncBatchAppender appender;

    public LoggingMetrics(AsyncBatchAppender appender) {
        this.appender = appender;
        // 注册指标
        MeterRegistry.registry.gauge("logging.async.queue.depth", appender, AsyncBatchAppender::getQueueDepth);
        MeterRegistry.registry.gauge("logging.async.write.per.second", appender, AsyncBatchAppender::getWritePerSecond);
        MeterRegistry.registry.gauge("logging.async.failure.rate", appender, AsyncBatchAppender::getFailureRate);
    }
}
```

### 3. 关键指标说明

| 指标 | 类型 | 说明 | 阈值建议 |
|------|------|------|----------|
| `queueDepth` | Gauge | 当前队列中的事件数 | < 80% queueSize |
| `dropped` | Counter | 队列满时被丢弃的事件数 | ≈ 0 |
| `failed` | Counter | 写入失败的事件数 | < 1% delivered |
| `writePerSecond` | Gauge | 每秒写入的事件数 | 监控基线 |
| `failureRate` | Gauge | 写入失败率 | < 0.1% |
| `queueUtilization` | Gauge | 队列使用率 | < 80% |

## 🔧 性能调优

### 1. 根据业务场景调整配置

#### 高吞吐场景（批量导入、数据迁移）
```xml
<appender name="HIGH_THROUGHPUT" class="com.basebackend.logging.appender.AsyncBatchAppender">
    <queueSize>50000</queueSize>
    <minBatchSize>100</minBatchSize>
    <maxBatchSize>1000</maxBatchSize>
    <flushIntervalMillis>500</flushIntervalMillis>
    <dynamicBatchSize>true</dynamicBatchSize>
    <targetLatencyMillis>300</targetLatencyMillis>
</appender>
```

#### 低延迟场景（实时告警、审计日志）
```xml
<appender name="LOW_LATENCY" class="com.basebackend.logging.appender.AsyncBatchAppender">
    <queueSize>5000</queueSize>
    <minBatchSize>10</minBatchSize>
    <maxBatchSize>100</maxBatchSize>
    <flushIntervalMillis>50</flushIntervalMillis>
    <dynamicBatchSize>true</dynamicBatchSize>
    <targetLatencyMillis>50</targetLatencyMillis>
</appender>
```

#### 平衡场景（默认配置）
```xml
<appender name="BALANCED" class="com.basebackend.logging.appender.AsyncBatchAppender">
    <queueSize>20000</queueSize>
    <minBatchSize>50</minBatchSize>
    <maxBatchSize>400</maxBatchSize>
    <flushIntervalMillis>150</flushIntervalMillis>
    <dynamicBatchSize>true</dynamicBatchSize>
    <targetLatencyMillis>120</targetLatencyMillis>
</appender>
```

### 2. 性能基准测试

#### 同步 vs 异步对比

```bash
# 启动同步模式测试
<appender name="SYNC_TEST" class="com.basebackend.logging.appender.AsyncBatchAppender">
    <synchronous>true</synchronous>
    <appender-ref ref="FILE"/>
</appender>

# 启动异步模式测试
<appender name="ASYNC_TEST" class="com.basebackend.logging.appender.AsyncBatchAppender">
    <synchronous>false</synchronous>
    <appender-ref ref="FILE"/>
</appender>
```

#### 批量大小影响

| 批量大小 | 吞吐量 | 平均延迟 | 99分位延迟 | 推荐场景 |
|----------|--------|----------|------------|----------|
| 32 | 基准 | 低 | 低 | 实时场景 |
| 128 | +40% | 中低 | 中低 | 平衡场景 |
| 256 | +65% | 中 | 中 | 批量场景 |
| 512 | +80% | 中高 | 中高 | 离线场景 |
| 1024 | +85% | 高 | 高 | 吞吐量优先 |

### 3. JVM 参数调优

建议 JVM 参数：

```bash
# 堆内存设置（根据日志量调整）
-Xms2g -Xmx4g

# GC 配置（推荐 G1GC）
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:G1HeapRegionSize=16m

# 线程栈大小（Worker线程）
-Xss1m

# 开启详细 GC 日志（用于分析性能问题）
-Xlog:gc*:file=/var/log/basebackend/gc.log:time,uptime,level,tags
```

## 🛠️ 故障排查

### 常见问题

#### 1. 队列持续满载（`dropped` 持续增长）

**原因分析：**
- 队列容量不足
- 批量处理速度跟不上日志产生速度
- 底层 Appender 阻塞

**解决方案：**
```xml
<!-- 增大队列容量 -->
<queueSize>30000</queueSize>

<!-- 减小批量大小以提高响应性 -->
<maxBatchSize>256</maxBatchSize>
<flushIntervalMillis>100</flushIntervalMillis>

<!-- 启用多线程写入（增加多个AsyncBatchAppender实例）-->
```

#### 2. 写入失败率过高（`failureRate` > 1%）

**原因分析：**
- 底层存储系统不可用
- 磁盘空间不足
- 网络问题

**解决方案：**
```xml
<!-- 增加重试次数 -->
<maxRetries>5</maxRetries>

<!-- 调整退避策略 -->
<initialBackoffMillis>100</initialBackoffMillis>
<maxBackoffMillis>5000</maxBackoffMillis>

<!-- 启用降级策略：写入本地临时文件 -->
```

#### 3. 内存占用过高

**原因分析：**
- 队列容量过大
- 批量大小过大
- 事件对象本身过大

**解决方案：**
```xml
<!-- 减小队列容量 -->
<queueSize>10000</queueSize>

<!-- 减小批量大小 -->
<maxBatchSize>256</maxBatchSize>

<!-- 启用事件压缩 -->
<eventCompressor>true</eventCompressor>
```

#### 4. 延迟过高（`targetLatencyMillis` 持续超过）

**原因分析：**
- 批量设置过大
- 底层存储I/O瓶颈
- 网络延迟

**解决方案：**
```xml
<!-- 减小批量大小 -->
<maxBatchSize>128</maxBatchSize>

<!-- 缩短时间窗口 -->
<flushIntervalMillis>50</flushIntervalMillis>

<!-- 关闭动态调整（使用小批量）-->
<dynamicBatchSize>false</dynamicBatchSize>
```

### 调试模式

启用调试模式查看内部状态：

```xml
<configuration>
    <!-- 启用 AsyncBatchAppender 的 DEBUG 日志 -->
    <logger name="com.basebackend.logging.appender.AsyncBatchAppender" level="DEBUG"/>

    <!-- 监控队列状态 -->
    <appender name="ASYNC_BATCH_FILE" class="com.basebackend.logging.appender.AsyncBatchAppender">
        <queueSize>20000</queueSize>
        <!-- 其他配置... -->
    </appender>
</configuration>
```

## 📝 最佳实践

### 1. 配置建议

- **队列大小**：通常设置为高峰日志产生速率 × 期望的缓冲时间（例如：1000 events/sec × 20 sec = 20,000）
- **批量大小**：平衡吞吐量和延迟，建议从256开始调优
- **时间窗口**：通常设置为100-500ms，根据实时性要求调整
- **重试策略**：生产环境建议3-5次，退避时间50ms-5000ms

### 2. 监控建议

- **必监控指标**：`queueDepth`、`dropped`、`failureRate`
- **可选监控**：`writePerSecond`、`batches`、`currentBatchSize`
- **告警阈值**：队列使用率>80%、失败率>0.1%、dropped>0

### 3. 容量规划

**根据业务量估算配置：**

```java
// 估算公式
int queueSize = (int) (logEventsPerSecond * bufferTimeInSeconds * safetyFactor);
// 例子：1000 events/sec * 30 sec * 1.5 = 45,000

int maxBatchSize = (int) (targetLatencyMillis * logEventsPerSecond / 1000 / throughputImprovementFactor);
// 例子：150ms * 1000 / 1000 / 1.8 = 83
```

### 4. 升级指南

从现有 Logback Appender 升级：

```xml
<!-- 升级前 -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <!-- 配置... -->
</appender>

<!-- 升级后：使用 AsyncBatchAppender 包装 -->
<appender name="ASYNC_FILE" class="com.basebackend.logging.appender.AsyncBatchAppender">
    <appender-ref ref="FILE"/>
</appender>
```

## 🔗 相关资源

- [Logback 官方文档](https://logback.qos.ch/)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)
- [basebackend-logging 主页](./README.md)

## 📄 许可证

本项目遵循 Apache License 2.0 许可证。

---

**更多详细信息和更新，请访问 [basebackend 项目主页](https://github.com/basebackend/basebackend)**
