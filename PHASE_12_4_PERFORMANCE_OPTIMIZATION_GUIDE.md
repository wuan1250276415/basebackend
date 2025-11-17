# Phase 12.4: 性能优化实施指南

## 📋 概述

本指南介绍如何全面优化系统性能，包括 JVM 调优、数据库优化、代码层面优化等核心能力，构建高性能、高可用的微服务系统。

---

## 🏗️ 性能优化架构

### 优化层次架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      性能优化架构                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │  JVM 层优化   │  │  应用层优化   │  │  数据库层优化 │           │
│  │              │  │              │  │              │           │
│  │ • 堆内存调优  │  │ • 代码优化    │  │ • SQL 优化   │           │
│  │ • GC 调优     │  │ • 算法优化    │  │ • 索引优化   │           │
│  │ • JIT 编译器  │  │ • 缓存策略    │  │ • 连接池配置 │           │
│  │ • 线程池调优  │  │ • 异步处理    │  │ • 分库分表   │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   操作系统优化  │  │   网络层优化  │  │   存储层优化  │           │
│  │              │  │              │  │              │           │
│  │ • 内核参数     │  │ • 零拷贝     │  │ • IO 优化    │           │
│  │ • 文件系统     │  │ • TCP 调优   │  │ • 缓存优化   │           │
│  │ • CPU 亲和     │  │ • 负载均衡   │  │ • 压缩算法   │           │
│  │ • NUMA 调优    │  │ • CDN 加速   │  │ • RAID 配置 │           │
│  └──────────────┘  └─────────────┘  └─────────────┘           │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    监控与分析                                 │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • JProfiler / VisualVM (JVM 监控)                           │ │
│  │ • Arthas (应用诊断)                                         │ │
│  │ • MySQL Performance Schema (数据库监控)                      │ │
│  │ • APM 工具 (应用性能监控)                                    │ │
│  │ • Flame Graph (火焰图分析)                                  │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 性能优化指标

| 层次 | 关键指标 | 优化目标 |
|------|----------|----------|
| **JVM** | GC 暂停时间、内存利用率 | < 100ms / < 70% |
| **应用** | 响应时间、吞吐量 | < 100ms / > 1000 TPS |
| **数据库** | 查询延迟、连接数 | < 10ms / < 80% |
| **网络** | RTT、带宽利用率 | < 1ms / > 80% |
| **存储** | IOPS、延迟 | > 10000 / < 5ms |

---

## ☕ JVM 调优

### 1. JVM 参数配置

#### 通用 JVM 参数

```bash
# ===================================================================
# BaseBackend JVM 优化参数
# ===================================================================

# 堆内存配置
-Xms4g                           # 初始堆大小 4GB
-Xmx4g                           # 最大堆大小 4GB
-XX:NewRatio=3                   # 新生代:老年代 = 1:3
-XX:SurvivorRatio=8              # Eden:Survivor = 8:1

# GC 调优
-XX:+UseG1GC                     # 使用 G1 垃圾收集器
-XX:MaxGCPauseMillis=100         # 最大 GC 暂停时间 100ms
-XX:G1HeapRegionSize=16m         # G1 区域大小 16MB
-XX:+G1UseAdaptiveIHOP           # 自适应初始化堆占用阈值
-XX:InitiatingHeapOccupancyPercent=45  # 初始堆占用阈值 45%
-XX:G1HeapWastePercent=5         # 堆浪费阈值 5%

# 元空间配置
-XX:MetaspaceSize=512m           # 元空间初始大小 512MB
-XX:MaxMetaspaceSize=1024m       # 元空间最大大小 1GB
-XX:+UseCompressedClassPointers  # 使用压缩类指针
-XX:CompressedClassSpaceSize=1g  # 压缩类空间大小 1GB

# 编译优化
-XX:+UseStringDeduplication      # 字符串去重
-XX:+UseCompressedOops           # 使用压缩 OOP
-XX:CompileThreshold=10000       # 编译阈值
-XX:+TieredCompilation           # 开启分层编译
-XX:TieredStopAtLevel=4          # 编译层级

# 性能监控
-XX:+PrintGCDetails              # 打印 GC 详情
-XX:+PrintGCTimeStamps           # 打印 GC 时间戳
-XX:+PrintGCDateStamps           # 打印 GC 日期
-XX:+PrintHeapAtGC               # 打印 GC 时堆信息
-XX:+PrintReferenceGC            # 打印引用对象 GC
-Xloggc:/app/logs/gc-%t.log      # GC 日志文件
-XX:+UseGCLogFileRotation        # 使用 GC 日志轮转
-XX:NumberOfGCLogFiles=5         # GC 日志文件数量
-XX:GCLogFileSize=10m            # GC 日志文件大小

# 诊断和调试
-XX:+HeapDumpOnOutOfMemoryError  # OOM 时生成堆转储
-XX:HeapDumpPath=/app/dumps/     # 堆转储文件路径
-XX:+PrintCommandLineFlags       # 打印命令行参数
-XX:+UnlockDiagnosticVMOptions   # 解锁诊断 VM 选项
-XX:+PrintFlagsFinal             # 打印最终参数

# 软引用优化
-XX:SoftRefLRUPolicyMSPerMB=50   # 软引用回收策略

# 生物特征选项
-XX:+UseBiasedLocking            # 使用偏向锁
-XX:+UseFastAccessorMethods      # 使用快速访问方法

# 错误处理
-XX:OnOutOfMemoryError="kill -9 %p"  # OOM 时执行命令
```

#### G1 GC 调优示例

```bash
# ===================================================================
# G1 垃圾收集器优化配置
# ===================================================================

# 基础配置
-Xms8g
-Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# G1 专用参数
-XX:G1HeapRegionSize=32m         # 大对象区域 32MB
-XX:+UseStringDeduplication      # 字符串去重
-XX:G1MixedGCCountTarget=8       # 混合 GC 目标次数
-XX:InitiatingHeapOccupancyPercent=60  # 触发 Mixed GC 的堆占用阈值
-XX:G1HeapWastePercent=5         # 可接受的堆浪费百分比

# 区域管理
-XX:G1NewSizePercent=5           # 新生代最小占比
-XX:G1MaxNewSizePercent=60       # 新生代最大占比
-XX:ParallelGCThreads=8          # 并行 GC 线程数
-XX:ConcGCThreads=4              # 并发 GC 线程数

# 混合 GC 调优
-XX:G1OldCSetRegionThresholdPercent=10  # 老年代 CSet 区域阈值
-XX:G1MixedGCCountTarget=8       # 混合 GC 目标次数
-XX:G1MixedGCLiveThresholdPercent=85    # 混合 GC 存活阈值

# 预热阶段
-XX:+AlwaysPreTouch             # 预分配和触摸内存

# 调试选项
-XX:+PrintSafepointStatistics   # 打印安全点统计
-XX:PrintSafepointStatisticsCount=1
-XX:+VerboseSafepointStatistics
```

### 2. JVM 监控脚本

```bash
#!/bin/bash
# ===================================================================
# JVM 性能监控脚本
# ===================================================================

APP_NAME="basebackend"
PID=$(pgf -f $APP_NAME | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "应用未运行"
    exit 1
fi

log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 获取堆内存使用情况
get_heap_usage() {
    jstat -gc $PID | tail -1
}

# 获取 GC 统计
get_gc_stats() {
    jstat -gc $PID | grep -E "YGCT|FGCT|GCT"
}

# 获取类加载统计
get_class_stats() {
    jstat -class $PID | tail -1
}

# 生成堆转储
generate_heap_dump() {
    local dump_path="/app/dumps/heap-$PID-$(date +%s).hprof"
    jmap -dump:format=b,file=$dump_path $PID
    log_info "堆转储文件生成: $dump_path"
}

# 分析 GC 日志
analyze_gc_log() {
    local gc_log="/app/logs/gc-$(date +%Y%m%d)*.log"
    if [ -f "$gc_log" ]; then
        log_info "分析 GC 日志..."
        jstat -gc $PID
    fi
}

# 监控线程
monitor_threads() {
    jstack $PID > /tmp/thread-dump-$PID.txt
    log_info "线程转储已生成"
}

# 检查死锁
check_deadlock() {
    jstack -l $PID | grep -A 10 "deadlock" && \
        log_warn "检测到死锁" || \
        log_info "未检测到死锁"
}

# 显示内存映射
show_memory_map() {
    pmap -x $PID | head -20
}

# 生成报告
generate_report() {
    local report_file="/tmp/jvm-report-$(date +%Y%m%d-%H%M%S).txt"

    {
        echo "========================================"
        echo "JVM 性能报告 - $(date)"
        echo "========================================"
        echo ""
        echo "PID: $PID"
        echo "应用: $APP_NAME"
        echo ""
        echo "【堆内存使用情况】"
        get_heap_usage
        echo ""
        echo "【GC 统计】"
        get_gc_stats
        echo ""
        echo "【类加载统计】"
        get_class_stats
        echo ""
        echo "【内存映射】"
        show_memory_map
        echo ""
    } > $report_file

    log_info "性能报告已生成: $report_file"
}

# 实时监控
monitor_realtime() {
    while true; do
        clear
        echo "========================================"
        echo "JVM 实时监控 - PID: $PID"
        echo "========================================"
        echo ""

        echo "【堆内存】"
        jstat -gc $PID | tail -1
        echo ""

        echo "【GC 时间】"
        jstat -gc $PID | grep -E "YGCT|FGCT|GCT"
        echo ""

        echo "【线程统计】"
        jstack $PID | grep -E ".*Thread.*java.lang.Thread.State" | sort | uniq -c | sort -rn
        echo ""

        sleep 5
    done
}

# 主函数
case "${1:-}" in
    heap)
        get_heap_usage
        ;;
    gc)
        get_gc_stats
        ;;
    class)
        get_class_stats
        ;;
    dump)
        generate_heap_dump
        ;;
    thread)
        monitor_threads
        ;;
    deadlock)
        check_deadlock
        ;;
    report)
        generate_report
        ;;
    monitor)
        monitor_realtime
        ;;
    *)
        echo "用法: $0 {heap|gc|class|dump|thread|deadlock|report|monitor}"
        exit 1
        ;;
esac
```

### 3. Arthas 诊断工具

```java
/**
 * Arthas 诊断示例
 *
 * 使用方式:
 * 1. 下载 Arthas: wget https://alibaba.github.io/arthas/arthas-boot.jar
 * 2. 启动 Arthas: java -jar arthas-boot.jar
 * 3. 在 Arthas 控制台中执行以下命令
 */

// 1. 查看应用基本信息
// dashboard

// 2. 实时监控应用
// monitor basebackend.service.UserService getUserInfo 5

// 3. 查看方法调用栈
// stack basebackend.service.UserService getUserInfo

// 4. 查看方法入参和返回值
// watch basebackend.service.UserService getUserInfo params[0] returnObj

// 5. 查看方法执行时间
// trace basebackend.service.UserService getUserInfo

// 6. 查看类的源码
// jad basebackend.service.UserService

// 7. 重新定义类（热更新）
// redefine /path/to/UserService.class

// 8. 查看 JVM 信息
// vmoption
// vmoption PrintGCDetails true

// 9. 查看内存对象
// heapdump /tmp/dump.hprof

// 10. 动态修改日志级别
// logger --name ROOT --level debug
```

---

## 🗄️ 数据库优化

### 1. MySQL 优化配置

```ini
# /etc/mysql/mysql.conf.d/basebackend.cnf

[mysqld]
# 基础配置
server-id = 1
port = 3306
basedir = /usr
datadir = /var/lib/mysql
tmpdir = /tmp
pid-file = /var/run/mysqld/mysqld.pid
socket = /var/run/mysqld/mysqld.sock

# 字符集配置
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
init_connect = 'SET NAMES utf8mb4'

# 连接配置
max_connections = 2000
max_connect_errors = 6000
wait_timeout = 28800
interactive_timeout = 28800

# 缓冲区配置
key_buffer_size = 256M
max_allowed_packet = 16M
table_open_cache = 256
sort_buffer_size = 1M
read_buffer_size = 1M
read_rnd_buffer_size = 8M
myisam_sort_buffer_size = 64M
thread_cache_size = 8
query_cache_size = 32M
query_cache_type = 1

# InnoDB 配置
default-storage-engine = INNODB
innodb_buffer_pool_size = 4G
innodb_log_file_size = 512M
innodb_log_buffer_size = 16M
innodb_flush_log_at_trx_commit = 1
innodb_lock_wait_timeout = 50
innodb_file_per_table = 1
innodb_open_files = 500
innodb_io_capacity = 1000
innodb_read_io_threads = 8
innodb_write_io_threads = 8
innodb_flush_method = O_DIRECT
innodb_buffer_pool_instances = 4

# 慢查询日志
slow_query_log = 1
slow_query_log_file = /var/log/mysql/mysql-slow.log
long_query_time = 2
log_queries_not_using_indexes = 1

# 二进制日志
log-bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M

# 性能 Schema
performance_schema = ON
performance_schema_max_table_instances = 500
performance_schema_max_table_handles = 2000

# 安全配置
sql_mode = STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO
```

### 2. 数据库连接池配置

```java
/**
 * HikariCP 连接池配置
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // 基本配置
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池大小
        config.setMaximumPoolSize(50);           // 最大连接数
        config.setMinimumIdle(10);               // 最小空闲连接
        config.setIdleTimeout(600000);           // 空闲连接超时 10 分钟
        config.setMaxLifetime(1800000);          // 连接最大生命周期 30 分钟
        config.setConnectionTimeout(30000);      // 连接超时 30 秒

        // 连接泄露检测
        config.setLeakDetectionThreshold(60000); // 连接泄露检测阈值 60 秒

        // 连接验证
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // 自动提交
        config.setAutoCommit(false);

        // 缓存
        config.setCachePrepStmts(true);
        config.setPrepStmtCacheSize(250);
        config.setPrepStmtCacheSqlLimit(2048);
        config.setUseServerPrepStmts(true);

        // 连接初始化 SQL
        config.setConnectionInitSql("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");

        // 监控
        config.setMetricRegistry(metricRegistry());

        return new HikariDataSource(config);
    }

    @Bean
    public MeterRegistry metricRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * 多数据源配置（主从分离）
     */
    @Bean
    @ConfigurationProperties("spring.datasource.write")
    public DataSource writeDataSource() {
        HikariConfig config = new HikariConfig();
        // 主库配置
        config.setJdbcUrl("jdbc:mysql://mysql-master:3306/basebackend");
        config.setUsername("basebackend");
        config.setPassword("password");
        // 读写分离配置
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        return new HikariDataSource(config);
    }

    @Bean
    @ConfigurationProperties("spring.datasource.read")
    public DataSource readDataSource() {
        HikariConfig config = new HikariConfig();
        // 从库配置
        config.setJdbcUrl("jdbc:mysql://mysql-slave:3306/basebackend");
        config.setUsername("basebackend");
        config.setPassword("password");
        // 读写分离配置
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        return new HikariDataSource(config);
    }
}

/**
 * 读写分离路由
 */
@Component
public class DataSourceRouter extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return getDataSource();
    }

    public static void setDataSource(String dataSourceType) {
        CONTEXT_HOLDER.set(dataSourceType);
    }

    public static String getDataSource() {
        return CONTEXT_HOLDER.get();
    }

    public static void clearDataSource() {
        CONTEXT_HOLDER.remove();
    }

    @Around("@annotation(ReadOnly)")
    public Object routeReadOnly(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            setDataSource("read");
            return joinPoint.proceed();
        } finally {
            clearDataSource();
        }
    }
}

/**
 * 强制使用主库注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WriteOnly {
}

/**
 * 强制使用从库注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {
}
```

### 3. SQL 优化最佳实践

```sql
-- ===================================================================
-- SQL 优化示例
-- ===================================================================

-- 1. 索引优化

-- 创建复合索引
CREATE INDEX idx_user_email_status ON users(email, status);

-- 创建覆盖索引
CREATE INDEX idx_order_user_id_status_created ON orders(user_id, status, created_at);

-- 创建部分索引（PostgreSQL）
CREATE INDEX idx_active_orders ON orders(user_id) WHERE status = 'active';

-- 2. 查询优化

-- 优化前：全表扫描
SELECT * FROM users WHERE email LIKE '%@example.com';

-- 优化后：使用索引 + 子查询
SELECT * FROM users
WHERE id IN (
    SELECT user_id FROM user_profiles
    WHERE email LIKE '%@example.com'
);

-- 3. 分页优化

-- 优化前：LIMIT 偏移量大时性能差
SELECT * FROM orders ORDER BY created_at DESC LIMIT 1000000, 20;

-- 优化后：使用子查询 + 索引
SELECT o.* FROM orders o
INNER JOIN (
    SELECT id FROM orders
    ORDER BY created_at DESC
    LIMIT 1000000, 20
) t ON o.id = t.id
ORDER BY o.created_at DESC;

-- 4. 连接优化

-- 优化前：多表连接
SELECT u.*, o.*, p.*
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
LEFT JOIN payments p ON o.id = p.order_id
WHERE u.status = 'active';

-- 优化后：先过滤再连接
WITH active_users AS (
    SELECT id FROM users WHERE status = 'active'
),
recent_orders AS (
    SELECT user_id, COUNT(*) as order_count
    FROM orders
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    GROUP BY user_id
)
SELECT u.*, ro.order_count
FROM active_users au
JOIN users u ON au.id = u.id
LEFT JOIN recent_orders ro ON u.id = ro.user_id;

-- 5. 聚合查询优化

-- 优化前：直接聚合
SELECT DATE(created_at) as order_date,
       COUNT(*) as order_count,
       SUM(amount) as total_amount
FROM orders
GROUP BY DATE(created_at);

-- 优化后：使用物化视图
CREATE MATERIALIZED VIEW daily_order_stats AS
SELECT DATE(created_at) as order_date,
       COUNT(*) as order_count,
       SUM(amount) as total_amount
FROM orders
GROUP BY DATE(created_at);

-- 6. 子查询优化

-- 优化前：相关子查询
SELECT u.*, (
    SELECT COUNT(*) FROM orders
    WHERE user_id = u.id
) as order_count
FROM users u;

-- 优化后：连接查询
SELECT u.id, u.name, COUNT(o.id) as order_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id;

-- 7. 分区表优化（MySQL）

-- 创建按日期分区
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- 8. 监控慢查询

-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看慢查询
SELECT * FROM mysql.slow_log
ORDER BY start_time DESC
LIMIT 10;

-- 9. 分析查询性能

-- 使用 EXPLAIN 分析
EXPLAIN SELECT u.*, o.*
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE u.status = 'active'
  AND o.created_at >= '2024-01-01';

-- 10. 索引使用统计

-- 查看索引使用情况
SELECT OBJECT_NAME, INDEX_NAME, COUNT_FETCH, COUNT_INSERT, COUNT_UPDATE, COUNT_DELETE
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'basebackend';

-- 11. 锁等待分析

-- 查看锁等待
SELECT * FROM information_schema.innodb_locks;

-- 12. 表统计信息更新

-- 更新表统计信息
ANALYZE TABLE users, orders, payments;

-- 重建索引
OPTIMIZE TABLE users;
```

### 4. 数据库监控脚本

```bash
#!/bin/bash
# ===================================================================
# MySQL 性能监控脚本
# ===================================================================

DB_USER="root"
DB_PASS="password"
DB_HOST="localhost"
DB_NAME="basebackend"

log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 获取数据库状态
get_db_status() {
    mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW STATUS LIKE 'Threads_connected';" 2>/dev/null
}

# 获取连接数
get_connections() {
    local connections=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW STATUS LIKE 'Threads_connected';" 2>/dev/null | grep Threads_connected | awk '{print $2}')
    echo "当前连接数: $connections"
}

# 获取慢查询
get_slow_queries() {
    local slow_queries=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW STATUS LIKE 'Slow_queries';" 2>/dev/null | grep Slow_queries | awk '{print $2}')
    echo "慢查询数量: $slow_queries"
}

# 获取缓存命中率
get_cache_hit_ratio() {
    local hits=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW STATUS LIKE 'Qcache_hits';" 2>/dev/null | grep Qcache_hits | awk '{print $2}')
    local inserts=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW STATUS LIKE 'Qcache_inserts';" 2>/dev/null | grep Qcache_inserts | awk '{print $2}')

    if [ "$hits" -gt 0 ] && [ "$inserts" -gt 0 ]; then
        local hit_ratio=$(echo "scale=2; $hits * 100 / ($hits + $inserts)" | bc)
        echo "查询缓存命中率: ${hit_ratio}%"
    fi
}

# 获取 InnoDB 缓冲池使用率
get_innodb_buffer_pool_usage() {
    local pool_size=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW VARIABLES LIKE 'innodb_buffer_pool_size';" 2>/dev/null | grep innodb_buffer_pool_size | awk '{print $2}')
    local pages_data=$(mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW STATUS LIKE 'Innodb_buffer_pool_pages_data';" 2>/dev/null | grep Innodb_buffer_pool_pages_data | awk '{print $2}')

    if [ "$pool_size" -gt 0 ] && [ "$pages_data" -gt 0 ]; then
        local page_size=16384
        local used=$(echo "$pages_data * $page_size" | bc)
        local usage=$(echo "scale=2; $used * 100 / $pool_size" | bc)
        echo "InnoDB 缓冲池使用率: ${usage}%"
    fi
}

# 获取锁等待
get_lock_waits() {
    mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SELECT * FROM information_schema.innodb_locks;" 2>/dev/null
}

# 获取进程列表
get_process_list() {
    mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "SHOW FULL PROCESSLIST;" 2>/dev/null | head -20
}

# 分析表大小
analyze_table_sizes() {
    mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "
        SELECT
            table_name,
            ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'size_mb'
        FROM information_schema.TABLES
        WHERE table_schema = '$DB_NAME'
        ORDER BY (data_length + index_length) DESC
        LIMIT 10;
    " 2>/dev/null
}

# 检查索引使用
check_index_usage() {
    mysql -h$DB_HOST -u$DB_USER -p$DB_PASS -e "
        SELECT
            table_name,
            index_name,
            count_read,
            count_fetch,
            count_insert,
            count_update,
            count_delete
        FROM performance_schema.table_io_waits_summary_by_index_usage
        WHERE object_schema = '$DB_NAME'
        ORDER BY count_read DESC
        LIMIT 20;
    " 2>/dev/null
}

# 生成性能报告
generate_report() {
    local report_file="/tmp/mysql-performance-report-$(date +%Y%m%d-%H%M%S).txt"

    {
        echo "========================================"
        echo "MySQL 性能报告 - $(date)"
        echo "========================================"
        echo ""
        echo "【连接数】"
        get_connections
        echo ""
        echo "【慢查询】"
        get_slow_queries
        echo ""
        echo "【缓存命中率】"
        get_cache_hit_ratio
        echo ""
        echo "【InnoDB 缓冲池】"
        get_innodb_buffer_pool_usage
        echo ""
        echo "【表大小】"
        analyze_table_sizes
        echo ""
    } > $report_file

    log_info "性能报告已生成: $report_file"
}

# 实时监控
monitor_realtime() {
    while true; do
        clear
        echo "========================================"
        echo "MySQL 实时监控"
        echo "========================================"
        echo ""

        echo "【连接数】"
        get_connections
        echo ""

        echo "【慢查询】"
        get_slow_queries
        echo ""

        echo "【进程列表】"
        get_process_list
        echo ""

        sleep 5
    done
}

# 主函数
case "${1:-}" in
    status)
        get_db_status
        ;;
    connections)
        get_connections
        ;;
    slow)
        get_slow_queries
        ;;
    cache)
        get_cache_hit_ratio
        ;;
    buffer)
        get_innodb_buffer_pool_usage
        ;;
    locks)
        get_lock_waits
        ;;
    process)
        get_process_list
        ;;
    tables)
        analyze_table_sizes
        ;;
    index)
        check_index_usage
        ;;
    report)
        generate_report
        ;;
    monitor)
        monitor_realtime
        ;;
    *)
        echo "用法: $0 {status|connections|slow|cache|buffer|locks|process|tables|index|report|monitor}"
        exit 1
        ;;
esac
```

---

## 💻 代码层面优化

### 1. 代码优化最佳实践

#### 并发优化

```java
/**
 * 线程池优化
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 业务线程池
     */
    @Bean("businessExecutor")
    public TaskExecutor businessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数 = CPU 核心数
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());

        // 最大线程数 = CPU 核心数 * 2
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);

        // 队列容量
        executor.setQueueCapacity(1000);

        // 线程空闲时间
        executor.setKeepAliveSeconds(60);

        // 线程名前缀
        executor.setThreadNamePrefix("business-");

        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        return executor;
    }

    /**
     * IO 密集型线程池
     */
    @Bean("ioExecutor")
    public TaskExecutor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("io-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        return executor;
    }

    /**
     * 定时任务线程池
     */
    @Bean("scheduledExecutor")
    public ThreadPoolTaskScheduler scheduledExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(20);
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);

        return scheduler;
    }
}

/**
 * CompletableFuture 优化
 */
@Service
public class CompletableFutureService {

    @Async("businessExecutor")
    public CompletableFuture<User> findUserAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            // 模拟数据库查询
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return User.builder().id(userId).name("User-" + userId).build();
        });
    }

    /**
     * 并行处理多个任务
     */
    public List<User> findUsersInParallel(List<Long> userIds) {
        // 方法 1: 使用 parallelStream
        return userIds.parallelStream()
            .map(this::findUser)
            .collect(Collectors.toList());

        // 方法 2: 使用 CompletableFuture
        List<CompletableFuture<User>> futures = userIds.stream()
            .map(userId -> CompletableFuture.supplyAsync(() -> findUser(userId)))
            .collect(Collectors.toList());

        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }

    /**
     * 异步组合操作
     */
    public CompletableFuture<OrderDetail> getOrderDetailAsync(Long orderId) {
        // 并行获取订单和用户信息
        CompletableFuture<Order> orderFuture = getOrderAsync(orderId);
        CompletableFuture<User> userFuture = findUserAsync(getUserIdByOrderId(orderId));

        return orderFuture.thenCombine(userFuture, (order, user) -> {
            return OrderDetail.builder()
                .order(order)
                .user(user)
                .build();
        });
    }

    private User findUser(Long userId) {
        // 实际实现
        return null;
    }

    private Order getOrderAsync(Long orderId) {
        // 实际实现
        return null;
    }

    private Long getUserIdByOrderId(Long orderId) {
        // 实际实现
        return null;
    }
}

/**
 * 无锁数据结构
 */
@Component
public class LockFreeDataStructure {

    /**
     * 使用 ConcurrentHashMap 优化缓存
     */
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    /**
     * 缓存获取
     */
    public Object getCachedValue(String key) {
        return cache.computeIfAbsent(key, k -> {
            // 懒加载逻辑
            return loadValue(k);
        });
    }

    /**
     * 使用 LongAdder 优化计数器
     */
    private final LongAdder requestCount = new LongAdder();

    public void incrementRequestCount() {
        requestCount.increment();
    }

    public long getRequestCount() {
        return requestCount.sum();
    }

    /**
     * 使用 Striped 减少锁竞争
     */
    private final Striped<Lock> stripedLock = Striped.lazyWeakLock(100);

    public void processWithLock(String resourceId, Runnable task) {
        Lock lock = stripedLock.get(resourceId);
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }

    private Object loadValue(String key) {
        // 模拟加载
        return new Object();
    }
}
```

#### 内存优化

```java
/**
 * 内存优化工具
 */
@Component
public class MemoryOptimizationUtil {

    /**
     * 使用对象池减少 GC
     */
    private final GenericObjectPool<ExpensiveObject> objectPool =
        new GenericObjectPool<>(new ExpensiveObjectFactory());

    /**
     * 对象池获取对象
     */
    public ExpensiveObject acquireObject() {
        try {
            return objectPool.borrowObject();
        } catch (Exception e) {
            return new ExpensiveObject();
        }
    }

    /**
     * 对象池归还对象
     */
    public void releaseObject(ExpensiveObject obj) {
        if (obj != null) {
            obj.reset();
            objectPool.returnObject(obj);
        }
    }

    /**
     * 使用 ThreadLocal 缓存对象
     */
    private static final ThreadLocal<StringBuilder> STRING_BUILDER_CACHE =
        ThreadLocal.withInitial(() -> new StringBuilder(1024));

    public String buildString(List<String> parts) {
        StringBuilder sb = STRING_BUILDER_CACHE.get();
        sb.setLength(0); // 重用 StringBuilder

        for (String part : parts) {
            sb.append(part);
        }

        return sb.toString();
    }

    /**
     * 字符串优化
     */
    public String optimizeStrings(String... parts) {
        // 使用 String.join 优化
        return String.join("-", parts);
    }

    /**
     * 集合初始化优化
     */
    public Map<String, Object> createOptimizedMap(int size) {
        // 根据预期大小初始化，避免扩容
        return new HashMap<>(size * 4 / 3 + 1);
    }

    public List<String> createOptimizedList(int size) {
        return new ArrayList<>(size);
    }

    /**
     * 避免内存泄漏
     */
    @PreDestroy
    public void cleanup() {
        // 清理 ThreadLocal
        STRING_BUILDER_CACHE.remove();

        // 关闭对象池
        objectPool.close();
    }

    static class ExpensiveObject {
        private byte[] data = new byte[1024];

        public void reset() {
            // 重置对象状态
        }
    }

    static class ExpensiveObjectFactory extends BasePooledObjectFactory<ExpensiveObject> {
        @Override
        public ExpensiveObject create() {
            return new ExpensiveObject();
        }

        @Override
        public PooledObject<ExpensiveObject> wrap(ExpensiveObject obj) {
            obj.reset();
            return super.wrap(obj);
        }
    }
}
```

#### 算法优化

```java
/**
 * 算法优化
 */
@Component
public class AlgorithmOptimization {

    /**
     * 缓存化优化 - 斐波那契数列
     */
    private final Map<Integer, Long> fibonacciCache = new HashMap<>();

    {
        fibonacciCache.put(0, 0L);
        fibonacciCache.put(1, 1L);
    }

    public long fibonacci(int n) {
        if (fibonacciCache.containsKey(n)) {
            return fibonacciCache.get(n);
        }

        long result = fibonacci(n - 1) + fibonacci(n - 2);
        fibonacciCache.put(n, result);
        return result;
    }

    /**
     * 批量处理优化
     */
    public void processBatch(List<Item> items) {
        // 分批处理，避免内存溢出
        int batchSize = 1000;

        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            List<Item> batch = items.subList(i, end);
            processBatchInternal(batch);
        }
    }

    private void processBatchInternal(List<Item> batch) {
        // 实际处理逻辑
    }

    /**
     * 提前终止优化 - 查找第一个匹配项
     */
    public Optional<Item> findFirstMatch(List<Item> items, Predicate<Item> predicate) {
        // 使用 Stream 的短路求值
        return items.stream()
            .filter(predicate)
            .findFirst();
    }

    /**
     * 懒加载优化
     */
    public class LazyInitializer<T> {
        private volatile T value;
        private final Supplier<T> initializer;

        public LazyInitializer(Supplier<T> initializer) {
            this.initializer = initializer;
        }

        public T get() {
            if (value == null) {
                synchronized (this) {
                    if (value == null) {
                        value = initializer.get();
                    }
                }
            }
            return value;
        }
    }

    /**
     * 分治算法优化 - 快速排序
     */
    public void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }

    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = (low - 1);

        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                swap(arr, i, j);
            }
        }

        swap(arr, i + 1, high);
        return i + 1;
    }

    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * 动态规划优化 - 最长公共子序列
     */
    public int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
    }
}
```

#### IO 优化

```java
/**
 * IO 优化工具
 */
@Component
public class IOOptimizationUtil {

    /**
     * 缓冲 IO 优化
     */
    public void bufferedCopy(String source, String target) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(source));
             BufferedWriter writer = new BufferedWriter(new FileWriter(target))) {

            char[] buffer = new char[8192];
            int bytesRead;

            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * NIO 优化 - 文件复制
     */
    public void nioCopy(String source, String target) throws IOException {
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);

        try (FileChannel sourceChannel = FileChannel.open(sourcePath, StandardOpenOption.READ);
             FileChannel targetChannel = FileChannel.open(targetPath,
                 StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    /**
     * 异步 IO 优化
     */
    @Async("ioExecutor")
    public CompletableFuture<Void> asyncWrite(String filePath, String content) {
        return CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(content);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * 批量写入优化
     */
    public void batchWrite(String filePath, List<String> lines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // 批量写入
            for (String line : lines) {
                writer.write(line);
                writer.newLine();

                // 每 1000 行 flush 一次
                if (lines.indexOf(line) % 1000 == 0) {
                    writer.flush();
                }
            }
            writer.flush();
        }
    }

    /**
     * 压缩优化
     */
    public byte[] compressData(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {

            gzip.write(data);
            gzip.finish();

            return baos.toByteArray();
        }
    }

    public byte[] decompressData(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzip = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = gzip.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            return baos.toByteArray();
        }
    }
}
```

### 2. 性能监控注解

```java
/**
 * 性能监控切面
 */
@Aspect
@Component
public class PerformanceMonitorAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorAspect.class);

    @Around("@annotation(monitored)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            long threshold = monitored.threshold();

            // 记录性能指标
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("method.execution.time")
                .description("Method execution time")
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry));

            // 如果超过阈值，记录警告
            if (duration > threshold) {
                logger.warn("Performance warning: {}.{}() took {}ms (threshold: {}ms)",
                    className, methodName, duration, threshold);
            } else {
                logger.debug("Performance: {}.{}() took {}ms",
                    className, methodName, duration);
            }

            return result;

        } catch (Throwable throwable) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Method {}.{}() failed after {}ms",
                className, methodName, duration, throwable);
            throw throwable;
        }
    }

    @Autowired
    private MeterRegistry meterRegistry;

    @Data
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Monitored {
        /**
         * 阈值（毫秒）
         */
        long threshold() default 1000;
    }
}

/**
 * 缓存优化切面
 */
@Aspect
@Component
public class CacheableAspect {

    @Around("@annotation(cacheable)")
    public Object cacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        String cacheName = cacheable.name();
        String key = generateKey(joinPoint, cacheable.key());

        // 先查缓存
        Object cachedValue = cacheManager.getCache(cacheName).get(key);
        if (cachedValue != null) {
            logger.debug("Cache hit: {}", key);
            return cachedValue;
        }

        // 缓存未命中，执行方法
        logger.debug("Cache miss: {}", key);
        Object result = joinPoint.proceed();

        // 放入缓存
        cacheManager.getCache(cacheName).put(key, result);

        return result;
    }

    @Around("@annotation(cacheEvict)")
    public Object cacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        String cacheName = cacheEvict.name();

        // 先执行方法
        Object result = joinPoint.proceed();

        // 清除缓存
        String key = generateKey(joinPoint, cacheEvict.key());
        cacheManager.getCache(cacheName).evict(key);

        logger.debug("Cache evicted: {}", key);

        return result;
    }

    private String generateKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (StringUtils.hasText(keyExpression)) {
            // 使用 SpEL 表达式生成 key
            EvaluationContext context = new StandardEvaluationContext(joinPoint.getArgs()[0]);
            return (String) parser.parseExpression(keyExpression).getValue(context);
        }

        // 使用方法名和参数生成 key
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        return signature.getDeclaringType().getSimpleName() + "." +
               signature.getName() +
               Arrays.toString(args);
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SpelExpressionParser parser;

    @Data
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Cacheable {
        String name();
        String key() default "";
    }

    @Data
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface CacheEvict {
        String name();
        String key() default "";
    }
}
```

---

## 📊 性能测试

### 1. JMH 基准测试

```java
/**
 * JMH 基准测试
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 2, jvmArgs = {"-Xmx2G"})
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 10)
public class PerformanceBenchmark {

    @Param({"1000", "10000", "100000"})
    private int size;

    private List<String> data;

    @Setup
    public void setup() {
        data = IntStream.range(0, size)
            .mapToObj(i -> "item-" + i)
            .collect(Collectors.toList());
    }

    @Benchmark
    public List<String> testForLoop() {
        List<String> result = new ArrayList<>();
        for (String item : data) {
            result.add(item.toUpperCase());
        }
        return result;
    }

    @Benchmark
    public List<String> testStream() {
        return data.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());
    }

    @Benchmark
    public List<String> testParallelStream() {
        return data.parallelStream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());
    }

    @Benchmark
    public String testStringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (String item : data) {
            sb.append(item).append(",");
        }
        return sb.toString();
    }

    @Benchmark
    public String testStringJoiner() {
        return data.stream()
            .collect(Collectors.joining(","));
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(PerformanceBenchmark.class.getSimpleName())
            .build();

        new Runner(opt).run();
    }
}
```

### 2. JMeter 性能测试脚本

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="BaseBackend 性能测试" enabled="true">
      <stringProp name="TestPlan.comments">BaseBackend 微服务平台性能测试</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="用户定义变量" enabled="true">
        <collectionProp name="Arguments.arguments">
          <elementProp name="server" elementType="Argument">
            <stringProp name="Argument.name">server</stringProp>
            <stringProp name="Argument.value">api.basebackend.com</stringProp>
          </elementProp>
          <elementProp name="port" elementType="Argument">
            <stringProp name="Argument.name">port</stringProp>
            <stringProp name="Argument.value">443</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>

    <hashTree>
      <!-- 用户组 -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="用户组" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControllerGui" testclass="LoopController" testname="循环控制器" enabled="true">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">-1</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <longProp name="ThreadGroup.duration">300</longProp>
        <longProp name="ThreadGroup.delay">0</longProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>

      <hashTree>
        <!-- HTTP 请求默认值 -->
        <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP 请求默认值" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="用户定义变量" enabled="true">
            <collectionProp name="Arguments.arguments"/>
          </elementProp>
          <stringProp name="HTTPSampler.domain">${server}</stringProp>
          <stringProp name="HTTPSampler.port">${port}</stringProp>
          <stringProp name="HTTPSampler.protocol">https</stringProp>
          <stringProp name="HTTPSampler.contentEncoding"></stringProp>
          <stringProp name="HTTPSampler.path"></stringProp>
          <stringProp name="HTTPSampler.implementation">HttpClient4</stringProp>
          <stringProp name="HTTPSampler.concurrentPool">6</stringProp>
          <stringProp name="HTTPSampler.connect_timeout">10000</stringProp>
          <stringProp name="HTTPSampler.response_timeout">30000</stringProp>
        </ConfigTestElement>

        <hashTree>
          <!-- 用户登录请求 -->
          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="用户登录" enabled="true">
            <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="用户定义变量" enabled="true">
              <collectionProp name="Arguments.arguments">
                <elementProp name="username" elementType="Argument">
                  <stringProp name="Argument.name">username</stringProp>
                  <stringProp name="Argument.value">testuser</stringProp>
                </elementProp>
                <elementProp name="password" elementType="Argument">
                  <stringProp name="Argument.name">password</stringProp>
                  <stringProp name="Argument.value">password123</stringProp>
                </elementProp>
              </collectionProp>
            </elementProp>
            <stringProp name="HTTPSampler.domain"></stringProp>
            <stringProp name="HTTPSampler.port"></stringProp>
            <stringProp name="HTTPSampler.protocol"></stringProp>
            <stringProp name="HTTPSampler.contentEncoding"></stringProp>
            <stringProp name="HTTPSampler.path">/api/auth/login</stringProp>
            <stringProp name="HTTPSampler.method">POST</stringProp>
            <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
            <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
            <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
            <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
            <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
            <stringProp name="HTTPSampler.implementation">HttpClient4</stringProp>
            <stringProp name="HTTPSampler.connect_timeout"></stringProp>
            <stringProp name="HTTPSampler.response_timeout"></stringProp>
          </HTTPSamplerProxy>

          <hashTree>
            <!-- 响应断言 -->
            <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="响应状态码断言" enabled="true">
              <collectionProp name="Asserion.test_strings">
                <stringProp name="49586">200</stringProp>
              </collectionProp>
              <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
              <boolProp name="Assertion.assume_success">false</boolProp>
              <intProp name="Assertion.test_type">1</intProp>
            </ResponseAssertion>

            <!-- 提取 Token -->
            <RegexExtractor guiclass="RegexExtractorGui" testclass="RegexExtractor" testname="提取 Token" enabled="true">
              <stringProp name="RegexExtractor.referenceNames">token</stringProp>
              <stringProp name="RegexExtractor.regex">"token":"([^"]+)"</stringProp>
              <stringProp name="RegexExtractor.template">$1$</stringProp>
              <stringProp name="RegexExtractor.defaultValue"></stringProp>
              <boolProp name="RegexExtractor.match_number">1</boolProp>
              <boolProp name="RegexExtractor.match_zero">false</boolProp>
              <boolProp name="RegexExtractor.regex_type">CHAR</boolProp>
            </RegexExtractor>
          </hashTree>

          <!-- 获取用户信息 -->
          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="获取用户信息" enabled="true">
            <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="用户定义变量" enabled="true">
              <collectionProp name="Arguments.arguments"/>
            </elementProp>
            <stringProp name="HTTPSampler.domain"></stringProp>
            <stringProp name="HTTPSampler.port"></stringProp>
            <stringProp name="HTTPSampler.protocol"></stringProp>
            <stringProp name="HTTPSampler.contentEncoding"></stringProp>
            <stringProp name="HTTPSampler.path">/api/user/profile</stringProp>
            <stringProp name="HTTPSampler.method">GET</stringProp>
            <boolProp name="HTTPSampler.follow_redirects">false</boolProp>
            <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
            <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
            <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
            <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
            <stringProp name="HTTPSampler.implementation">HttpClient4</stringProp>
            <stringProp name="HTTPSampler.connect_timeout"></stringProp>
            <stringProp name="HTTPSampler.response_timeout"></stringProp>
          </HTTPSamplerProxy>

          <hashTree>
            <!-- 添加 HTTP Header -->
            <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header 管理器" enabled="true">
              <collectionProp name="HeaderManager.headers">
                <elementProp name="Authorization" elementType="Header">
                  <stringProp name="Header.name">Authorization</stringProp>
                  <stringProp name="Header.value">Bearer ${token}</stringProp>
                </elementProp>
              </collectionProp>
            </HeaderManager>
          </hashTree>

          <!-- 聚合报告 -->
          <ResultCollector guiclass="StatVisualizer" testclass="ResultCollector" testname="聚合报告" enabled="true">
            <boolProp name="ResultCollector.error_logging">false</boolProp>
            <objProp>
              <name>saveConfig</name>
              <value class="SampleSaveConfiguration">
                <time>true</time>
                <latency>true</latency>
                <timestamp>true</timestamp>
                <success>true</success>
                <label>true</label>
                <code>true</code>
                <message>true</message>
                <threadName>true</threadName>
                <dataType>true</dataType>
                <encoding>false</encoding>
                <assertions>true</assertions>
                <subresults>true</subresults>
                <responseData>false</responseData>
                <samplerData>false</samplerData>
                <xml>false</xml>
                <fieldNames>true</fieldNames>
                <responseHeaders>false</responseHeaders>
                <requestHeaders>false</requestHeaders>
                <responseDataOnError>false</responseDataOnError>
                <saveAssertionResultsFailureMessage>true</saveAssertionResultsFailureMessage>
                <assertionsResultsToSave>0</assertionsResultsToSave>
                <bytes>true</bytes>
                <sentBytes>true</sentBytes>
                <url>true</url>
                <threadCounts>true</threadCounts>
                <idleTime>true</idleTime>
                <connectTime>true</connectTime>
              </value>
            </objProp>
            <stringProp name="filename"></stringProp>
          </ResultCollector>

        </hashTree>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

---

## 📚 参考资料

1. [JVM 调优指南](https://www.oracle.com/technetwork/java/gc-tuning-5-138395.html)
2. [MySQL 性能优化](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
3. [Spring Boot 性能优化](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html)
4. [Java 性能优化最佳实践](https://www.baeldung.com/java-performance)

---

## 📋 性能优化检查清单

### JVM 调优
- [ ] 堆内存配置合理
- [ ] GC 收集器选择适当
- [ ] GC 日志分析
- [ ] 堆转储分析
- [ ] 线程监控
- [ ] 死锁检测

### 数据库优化
- [ ] 索引设计合理
- [ ] 查询优化
- [ ] 连接池配置
- [ ] 慢查询分析
- [ ] 读写分离
- [ ] 分库分表

### 代码优化
- [ ] 并发处理优化
- [ ] 内存使用优化
- [ ] 算法优化
- [ ] IO 优化
- [ ] 缓存策略
- [ ] 异步处理

### 性能测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 压力测试
- [ ] 基准测试
- [ ] 监控告警

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-14
**状态：** 📋 指南完成，准备实施

**加油喵～ 性能优化即将完成！** ฅ'ω'ฅ
