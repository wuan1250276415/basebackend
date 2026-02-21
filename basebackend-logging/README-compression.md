# AsyncGzipSizeAndTimeRollingPolicy 异步压缩滚动策略

## 📖 概述

AsyncGzipSizeAndTimeRollingPolicy 是专为 basebackend-logging 模块设计的高效日志压缩与存储优化策略，通过异步Gzip压缩、冷热数据分离和智能保留策略，在不影响日志写入性能的前提下，实现显著的成本节约和运维效率提升。

### 核心特性

- ✅ **时间/大小双触发滚动**：支持基于时间和文件大小的双重滚动触发
- ✅ **异步Gzip压缩**：后台线程池异步压缩，不阻塞日志写入线程
- ✅ **冷热数据分离**：7天内热存储，7天后自动归档压缩
- ✅ **智能保留策略**：基于天数和总存储容量的双重约束清理
- ✅ **压缩验证**：自动验证压缩文件完整性，确保数据可靠
- ✅ **性能优化**：流式压缩、CPU使用率控制、小文件跳过压缩
- ✅ **索引优化**：精简索引文件并自动裁剪，防止索引膨胀

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **存储节省** | ≥60% | 通过Gzip压缩实现的存储空间节省 |
| **压缩速度** | 高效 | 不影响日志写入速度 |
| **CPU使用率** | <50% | 压缩期间CPU使用率限制 |
| **压缩比率** | 动态 | 根据压缩级别自动调整（5-9级） |

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│         AsyncGzipSizeAndTimeRollingPolicy                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           RollingFileAppender                        │  │
│  └────────────────┬──────────────────────────────────────┘  │
│                   │                                          │
│                   ▼                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         双触发策略（TriggeringPolicy）                │  │
│  │  ┌──────────────┐  ┌──────────────┐                  │  │
│  │  │ 时间触发     │  │ 大小触发     │                  │  │
│  │  │ (按天滚动)   │  │ (256MB)      │                  │  │
│  │  └──────────────┘  └──────────────┘                  │  │
│  └────────────────┬──────────────────────────────────────┘  │
│                   │                                          │
│                   ▼                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           Rollover 执行                               │  │
│  │  1. 重命名文件 (app.log → app.2025-11-22.0.log)       │  │
│  │  2. 解析归档目标 (冷热分离判断)                        │  │
│  │  3. 提交异步压缩任务                                  │  │
│  └────────────────┬──────────────────────────────────────┘  │
│                   │                                          │
│                   ▼                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      异步压缩线程池 (ThreadPoolExecutor)               │  │
│  │                                                       │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐           │  │
│  │  │ 压缩任务1 │  │ 压缩任务2 │  │ 压缩任务N │           │  │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘           │  │
│  │       │              │              │                │  │
│  │       ▼              ▼              ▼                │  │
│  │  ┌──────────────────────────────────────┐          │  │
│  │  │        Gzip压缩流程                   │          │  │
│  │  │  1. 读取源文件 (流式处理)              │          │  │
│  │  │  2. Gzip压缩 (级别1-9)               │          │  │
│  │  │  3. 写入目标文件                     │          │  │
│  │  │  4. 验证完整性                       │          │  │
│  │  │  5. 更新索引                         │          │  │
│  │  │  6. 删除源文件                       │          │  │
│  │  └──────────────────────────────────────┘          │  │
│  └────────────────┬──────────────────────────────────────┘  │
│                   │                                          │
│                   ▼                                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           存储层管理                                  │  │
│  │                                                       │  │
│  │  热数据区 (7天内)        冷数据区 (7天后)             │  │
│  │  ┌──────────────┐        ┌──────────────┐            │  │
│  │  │ logs/app.*.log │      │ logs/archive/ │            │  │
│  │  │              │        │ app.*.log.gz │            │  │
│  │  └──────────────┘        └──────────────┘            │  │
│  │                                                       │  │
│  │  清理策略：                                               │  │
│  │  - 超过retentionDays的文件删除                           │  │
│  │  - 超过maxTotalSize从最旧开始删除                        │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 在 logback-spring.xml 中配置

```xml
<configuration>
    <!-- 使用 AsyncGzipSizeAndTimeRollingPolicy 的文件输出 -->
    <appender name="COMPRESSED_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/basebackend/application.log</file>

        <!-- 滚动策略配置 -->
        <rollingPolicy class="com.basebackend.logging.rollover.AsyncGzipSizeAndTimeRollingPolicy">
            <!-- 文件名模式（时间滚动） -->
            <fileNamePattern>/var/log/basebackend/application.%d{yyyy-MM-dd}.%i.log</fileNamePattern>

            <!-- 大小触发阈值 -->
            <maxFileSize>256MB</maxFileSize>

            <!-- 压缩配置 -->
            <compressionLevel>6</compressionLevel>    <!-- 1-9，建议6-7 -->
            <compressionThreshold>512KB</compressionThreshold>  <!-- 小文件不压缩 -->

            <!-- 冷热分离配置 -->
            <hotRetentionDays>7</hotRetentionDays>    <!-- 7天内热存储 -->
            <archiveDirectory>/var/log/basebackend/archive</archiveDirectory>

            <!-- 保留策略 -->
            <retentionDays>30</retentionDays>         <!-- 保留30天 -->
            <maxTotalSize>15GB</maxTotalSize>         <!-- 总容量上限 -->

            <!-- 性能配置 -->
            <maxConcurrentCompressions>2</maxConcurrentCompressions>  <!-- 并发线程数 -->
            <verifyCompression>true</verifyCompression>  <!-- 验证压缩完整性 -->
            <eagerCleanup>true</eagerCleanup>           <!-- 立即清理 -->
        </rollingPolicy>

        <!-- 日志格式 -->
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 全局日志级别 -->
    <root level="INFO">
        <appender-ref ref="COMPRESSED_FILE"/>
    </root>

    <!-- 特定包的日志级别 -->
    <logger name="com.basebackend" level="DEBUG"/>
    <logger name="org.springframework" level="INFO"/>
</configuration>
```

### 2. 验证配置

启动应用后，检查目录结构：

```bash
# 查看日志目录
ls -lh /var/log/basebackend/

# 输出示例：
# application.log                    # 当前活动日志
# application.2025-11-22.0.log       # 滚动后的日志（待压缩）
# application.2025-11-21.0.log.gz    # 已压缩日志
# archive/                           # 归档目录
#   application.2025-11-15.0.log.gz  # 超过7天的日志归档到此
#   application.2025-11-14.0.log.gz
# log-index.meta                     # 压缩文件索引
```

### 3. 监控压缩状态

```bash
# 查看压缩索引
cat /var/log/basebackend/log-index.meta

# 输出示例：
# 2025-11-22 10:30:15|application.2025-11-21.0.log.gz|268435456|72345678
# 2025-11-22 09:15:22|application.2025-11-20.0.log.gz|268435456|72123456
```

## ⚙️ 配置参数详解

### 滚动配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `fileNamePattern` | String | **必需** | 文件名模式，用于时间滚动<br/>例如：`logs/app.%d{yyyy-MM-dd}.%i.log` |
| `maxFileSize` | String | 256MB | 大小触发阈值<br/>当文件超过此大小时立即滚动<br/>格式：`10MB`、`512KB`、`1GB` |

### 压缩配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `compressionLevel` | int | 5 | Gzip压缩级别（1-9）<br/>1=最快，压缩率最低；9=最慢，压缩率最高<br/>**推荐**：6-7（平衡速度与压缩率） |
| `compressionThreshold` | String | 0（全部压缩） | 压缩阈值<br/>小于此大小的文件直接移动不压缩<br/>格式：`512KB`、`1MB` |

### 冷热分离配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `hotRetentionDays` | int | 7 | 热数据保留天数<br/>此期间日志保留在原目录，不归档 |
| `archiveDirectory` | String | null | 归档目录（可选）<br/>冷数据（超过hotRetentionDays）将存放至此目录 |

### 保留策略配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `retentionDays` | int | 30 | 压缩文件保留天数<br/>超过此天数的压缩文件将被自动清理 |
| `maxTotalSize` | String | 20GB | 最大总存储容量<br/>当压缩文件总大小超过此值时，从最旧开始清理 |

### 性能配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `maxConcurrentCompressions` | int | `min(2, CPU/2)` | 最大并发压缩线程数<br/>限制CPU使用率，建议不超过2 |
| `verifyCompression` | boolean | true | 是否验证压缩完整性<br/>压缩后读取Gzip头部验证 |
| `eagerCleanup` | boolean | true | 是否启用立即清理<br/>每次滚动后立即执行清理任务 |

## 📊 监控指标

AsyncGzipSizeAndTimeRollingPolicy 提供了丰富的监控指标：

### 1. 通过代码获取指标

```java
// 获取RollingPolicy实例
AsyncGzipSizeAndTimeRollingPolicy policy =
    (AsyncGzipSizeAndTimeRollingPolicy) LoggerFactory.getLogger("COMPRESSED_FILE")
        .getAppender("COMPRESSED_FILE")
        .getRollingPolicy();

// 获取压缩指标
double compressionRatio = policy.getCompressionRatio();          // 压缩比率 (compressed/raw)
double savedPercentage = policy.getSavedPercentage();            // 节省比例 (百分比)
double compressionSpeed = policy.getCompressionSpeedMBps();      // 压缩速度 (MB/s)
long compressedFiles = policy.getCompressedFiles();              // 已压缩文件数
long rawBytes = policy.getRawBytes();                            // 原始字节数
long compressedBytes = policy.getCompressedBytes();              // 压缩后字节数
long savedBytes = policy.getSavedBytes();                        // 节省字节数

System.out.println(String.format("压缩率: %.2f%%, 节省: %.2f%%, 速度: %.2f MB/s",
    (1 - compressionRatio) * 100, savedPercentage, compressionSpeed));
```

### 2. 通过 Micrometer 集成

```java
@Component
public class CompressionMetrics {

    private final AsyncGzipSizeAndTimeRollingPolicy policy;

    public CompressionMetrics(AsyncGzipSizeAndTimeRollingPolicy policy) {
        this.policy = policy;
        // 注册指标
        MeterRegistry.registry.gauge("logging.compression.ratio",
            policy, AsyncGzipSizeAndTimeRollingPolicy::getCompressionRatio);
        MeterRegistry.registry.gauge("logging.compression.speed.mbps",
            policy, AsyncGzipSizeAndTimeRollingPolicy::getCompressionSpeedMBps);
        MeterRegistry.registry.gauge("logging.compression.files",
            policy, AsyncGzipSizeAndTimeRollingPolicy::getCompressedFiles);
        MeterRegistry.registry.gauge("logging.compression.saved.bytes",
            policy, AsyncGzipSizeAndTimeRollingPolicy::getSavedBytes);
    }
}
```

### 3. 关键指标说明

| 指标 | 类型 | 说明 | 正常范围 |
|------|------|------|----------|
| `compressionRatio` | Gauge | 压缩比率（压缩后/原始） | 0.3-0.5（节省50-70%） |
| `savedPercentage` | Gauge | 节省比例（百分比） | 50-70% |
| `compressionSpeed` | Gauge | 压缩速度（MB/s） | 依据压缩级别和CPU |
| `compressedFiles` | Counter | 已压缩文件数 | 持续增长 |
| `rawBytes` | Counter | 原始总字节数 | 持续增长 |
| `compressedBytes` | Counter | 压缩后总字节数 | 持续增长 |

## 🔧 性能调优

### 1. 根据场景调整压缩级别

#### 快速压缩场景（实时性要求高）
```xml
<compressionLevel>3</compressionLevel>  <!-- 快速，压缩率约40-50% -->
```

#### 平衡场景（推荐）
```xml
<compressionLevel>6</compressionLevel>  <!-- 平衡，压缩率约60-70% -->
```

#### 最大压缩场景（存储成本优先）
```xml
<compressionLevel>9</compressionLevel>  <!-- 最慢，压缩率约70-80% -->
```

### 2. 线程池配置优化

#### 低CPU环境（2核心）
```xml
<maxConcurrentCompressions>1</maxConcurrentCompressions>
```

#### 标准CPU环境（4-8核心）
```xml
<maxConcurrentCompressions>2</maxConcurrentCompressions>
```

#### 高性能CPU环境（16+核心）
```xml
<maxConcurrentCompressions>3</maxConcurrentCompressions>
```

### 3. 存储策略优化

#### 高频滚动场景（大量小文件）
```xml
<maxFileSize>100MB</maxFileSize>              <!-- 小文件滚动 -->
<compressionThreshold>256KB</compressionThreshold>  <!-- 小文件跳过压缩 -->
```

#### 低频滚动场景（少量大文件）
```xml
<maxFileSize>512MB</maxFileSize>              <!-- 大文件滚动 -->
<compressionThreshold>0</compressionThreshold>      <!-- 所有文件压缩 -->
```

## 🛠️ 故障排查

### 常见问题

#### 1. 压缩失败（压缩文件为0字节或损坏）

**原因分析：**
- 磁盘空间不足
- 权限问题
- 压缩过程中JVM崩溃

**解决方案：**
```bash
# 检查磁盘空间
df -h /var/log/basebackend

# 检查权限
ls -la /var/log/basebackend/archive

# 启用压缩验证
<verifyCompression>true</verifyCompression>
```

#### 2. CPU使用率过高

**原因分析：**
- 并发压缩线程过多
- 压缩级别过高
- 压缩文件过大

**解决方案：**
```xml
<!-- 减少并发线程 -->
<maxConcurrentCompressions>1</maxConcurrentCompressions>

<!-- 降低压缩级别 -->
<compressionLevel>4</compressionLevel>

<!-- 减小文件大小 -->
<maxFileSize>128MB</maxFileSize>
```

#### 3. 存储空间持续增长

**原因分析：**
- 保留天数过长
- 总容量上限设置过大
- 清理任务未执行

**解决方案：**
```xml
<!-- 缩短保留天数 -->
<retentionDays>7</retentionDays>

<!-- 降低总容量上限 -->
<maxTotalSize>10GB</maxTotalSize>

<!-- 启用立即清理 -->
<eagerCleanup>true</eagerCleanup>
```

#### 4. 压缩速度过慢

**原因分析：**
- I/O性能瓶颈
- 压缩级别过高
- 文件太大

**解决方案：**
```bash
# 使用高性能存储
# - SSD优于机械硬盘
# - 本地存储优于网络存储

# 调整压缩策略
<compressionLevel>5</compressionLevel>  <!-- 降低级别 -->
<maxFileSize>128MB</maxFileSize>      <!-- 减小文件 -->
```

### 调试模式

启用调试模式查看内部状态：

```xml
<configuration>
    <!-- 启用压缩策略的DEBUG日志 -->
    <logger name="com.basebackend.logging.rollover.AsyncGzipSizeAndTimeRollingPolicy"
            level="DEBUG"/>

    <!-- 监控压缩线程状态 -->
    <appender name="COMPRESSED_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="com.basebackend.logging.rollover.AsyncGzipSizeAndTimeRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>256MB</maxFileSize>
            <!-- 其他配置... -->
        </rollingPolicy>
    </appender>
</configuration>
```

## 📝 最佳实践

### 1. 配置建议

- **压缩级别**：推荐6-7（平衡速度和压缩率）
- **文件大小**：根据I/O性能调整，通常100-512MB
- **并发线程**：不超过2，避免影响业务线程
- **保留策略**：生产环境建议7-30天，根据合规要求调整

### 2. 监控建议

- **必监控指标**：`compressionRatio`、`savedPercentage`、`compressedFiles`
- **告警阈值**：
  - 压缩失败率 > 1%
  - 压缩比率 < 30%（压缩效果差）
  - 磁盘使用率 > 80%

### 3. 容量规划

**压缩效果估算：**

| 原始日志量 | 压缩级别 | 压缩后大小 | 节省空间 |
|------------|----------|------------|----------|
| 100GB | 5 (默认) | 35-45GB | 55-65% |
| 100GB | 6 | 30-40GB | 60-70% |
| 100GB | 9 | 20-30GB | 70-80% |

**保留策略计算：**

```java
// 估算公式
long dailyLogVolume = 10GB;  // 每日日志量
int retentionDays = 30;
double compressionRatio = 0.4;  // 压缩后40%

long requiredStorage = (long) (dailyLogVolume * retentionDays * compressionRatio);
// 10GB * 30 * 0.4 = 120GB
```

### 4. 升级指南

从标准 Logback 滚动策略升级：

```xml
<!-- 升级前 -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
    </rollingPolicy>
</appender>

<!-- 升级后：使用异步压缩策略 -->
<appender name="COMPRESSED_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/app.log</file>
    <rollingPolicy class="com.basebackend.logging.rollover.AsyncGzipSizeAndTimeRollingPolicy">
        <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
        <maxFileSize>256MB</maxFileSize>
        <compressionLevel>6</compressionLevel>
        <retentionDays>30</retentionDays>
    </rollingPolicy>
</appender>
```

## 📄 存储结构示例

### 热数据区（7天内）

```
/var/log/basebackend/
├── application.log                     # 当前活动日志
├── application.2025-11-22.0.log        # 刚滚动的日志（等待压缩）
├── application.2025-11-21.0.log.gz     # 已压缩的日志
├── application.2025-11-20.0.log.gz     # 已压缩的日志
└── log-index.meta                      # 压缩文件索引
```

### 冷数据区（7天后）

```
/var/log/basebackend/archive/
├── application.2025-11-15.0.log.gz     # 归档压缩日志
├── application.2025-11-14.0.log.gz
├── application.2025-11-13.0.log.gz
└── log-index.meta                      # 归档区索引
```

### 索引文件格式

```
# log-index.meta
2025-11-22 10:30:15|application.2025-11-21.0.log.gz|268435456|72345678
2025-11-22 09:15:22|application.2025-11-20.0.log.gz|268435456|72123456
2025-11-22 08:00:10|application.2025-11-19.0.log.gz|268435456|71987654

# 格式：时间|文件名|原始大小|压缩后大小
```

## 🔗 相关资源

- [Gzip 压缩算法详解](https://www.zlib.net/manual.html)
- [Logback 滚动策略](https://logback.qos.ch/manual/appenders.html#rolling)
- [basebackend-logging 主页](./README.md)
- [AsyncBatchAppender 文档](./README-async-batch.md)

## 📄 许可证

本项目遵循 Apache License 2.0 许可证。

---

**更多详细信息和更新，请访问 [basebackend 项目主页](https://github.com/basebackend/basebackend)**
