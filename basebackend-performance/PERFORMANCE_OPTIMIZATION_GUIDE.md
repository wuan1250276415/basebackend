# 微服务性能优化指南

## 📋 概述

本文档详细介绍了 BaseBackend 微服务架构的性能优化策略和实施方法，包括 JVM 调优、数据库优化、缓存优化、性能测试等内容。

---

## 🎯 优化目标

### 性能指标
- **响应时间**: P95 < 200ms, P99 < 500ms
- **吞吐量**: 单服务 QPS > 1000
- **可用性**: > 99.9%
- **资源利用**: CPU < 80%, 内存 < 80%

---

## 🔧 JVM 调优

### 1. 内存配置

#### 堆内存设置
```bash
# 根据系统内存分配
-Xms2048m      # 初始堆大小
-Xmx2048m      # 最大堆大小
```

#### 新生代配置
```bash
-XX:NewRatio=8         # 新生代:老年代 = 1:8
-XX:SurvivorRatio=8    # Eden:Survivor = 8:1
```

#### Metaspace 配置
```bash
-XX:MetaspaceSize=256m    # 初始 Metaspace
-XX:MaxMetaspaceSize=256m # 最大 Metaspace
```

### 2. GC 优化

#### G1GC 配置
```bash
-XX:+UseG1GC                # 使用 G1 垃圾收集器
-XX:MaxGCPauseMillis=200    # 最大 GC 暂停时间
-XX:G1HeapRegionSize=16m    # 堆区域大小
-XX:+UseStringDeduplication # 字符串去重
```

#### GC 日志配置
```bash
-Xloggc:/logs/gc-%t.log
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCDateStamps
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=10
-XX:GCLogFileSize=10M
```

### 3. JIT 编译优化

```bash
-XX:+TieredCompilation         # 启用分层编译
-XX:TieredStopAtLevel=4        # 编译级别
-XX:CompileThreshold=10000     # 编译阈值
```

---

## 📊 数据库优化

### 1. 索引优化

#### 常用索引
```sql
-- 用户表索引
ALTER TABLE sys_user
ADD INDEX idx_username (username),
ADD INDEX idx_email (email),
ADD INDEX idx_status (status);

-- 复合索引
ALTER TABLE sys_user
ADD INDEX idx_status_dept (status, dept_id);
```

#### 索引策略
- **主键索引**: 自动创建
- **唯一索引**: 保障数据唯一性
- **复合索引**: 优化多条件查询
- **前缀索引**: 优化长字符串查询

### 2. 查询优化

#### 使用 EXPLAIN 分析
```sql
EXPLAIN SELECT * FROM sys_user WHERE username = 'admin';
```

#### 优化建议
- 避免 SELECT *，只查询必要字段
- 使用 LIMIT 分页
- 避免在 WHERE 子句中使用函数
- 使用合适的比较操作符

### 3. 表结构优化

#### 分区表 (日志表)
```sql
ALTER TABLE sys_log
PARTITION BY RANGE (YEAR(create_time)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

#### 数据归档
- 定期清理历史数据
- 将冷数据迁移到归档表
- 使用数据分区提高查询性能

---

## 🚀 缓存优化

### 1. Redis 缓存配置

#### 内存管理
```conf
maxmemory 2gb
maxmemory-policy allkeys-lru
```

#### 连接池优化
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: -1
```

### 2. 缓存策略

#### 多级缓存
```java
@Cacheable(
    value = "user",
    key = "#id",
    cacheManager = "multiLevelCacheManager"
)
public User getUserById(Long id) {
    return userMapper.selectById(id);
}
```

#### 缓存更新策略
- **Cache Aside**: 应用负责缓存更新
- **Read Through**: 缓存负责数据加载
- **Write Through**: 同步更新缓存和数据库

### 3. 缓存穿透防护

```java
// 使用布隆过滤器
public User getUser(Long id) {
    if (!bloomFilter.exists(id)) {
        return null;
    }
    return cache.get(id);
}
```

---

## 📈 性能测试

### 1. 压力测试

#### 使用 Apache Bench
```bash
# 基础压测
ab -n 10000 -c 100 http://localhost:8081/api/users

# 带时间限制的压测
ab -t 60 -c 100 http://localhost:8081/api/users
```

#### 使用 JMeter
- 创建测试计划
- 配置线程组和HTTP请求
- 添加监听器和报告

### 2. 性能监控

#### 关键指标
- **响应时间**: 平均值、P95、P99
- **吞吐量**: QPS、TPS
- **错误率**: HTTP 错误码统计
- **资源利用率**: CPU、内存、磁盘

#### 监控工具
- **Prometheus + Grafana**: 指标收集和可视化
- **Micrometer**: 应用指标埋点
- **SkyWalking**: 分布式链路追踪

---

## 🛠️ 优化工具

### 1. 分析工具

#### JVM 分析
```bash
# 内存分析
jmap -histo <pid> > memory.txt
jmap -dump:format=b,file=heap.bin <pid>

# GC 分析
jstat -gc <pid> 5s
```

#### 数据库分析
```sql
-- 慢查询分析
SELECT * FROM mysql.slow_log
ORDER BY start_time DESC
LIMIT 10;

-- 索引使用分析
EXPLAIN FORMAT=JSON SELECT * FROM sys_user WHERE username='admin';
```

### 2. 性能测试脚本

#### 自动化测试
```bash
#!/bin/bash
# 执行压力测试
./performance-test.sh

# 执行稳定性测试
./stability-test.sh

# 执行监控
./monitor-performance.sh
```

---

## 📋 优化检查清单

### JVM 优化
- [ ] 设置合适的堆内存大小
- [ ] 选择合适的垃圾收集器
- [ ] 配置 GC 日志
- [ ] 开启 JVM 监控
- [ ] 设置内存溢出处理

### 数据库优化
- [ ] 创建必要的索引
- [ ] 分析慢查询
- [ ] 优化 SQL 语句
- [ ] 配置连接池
- [ ] 启用查询缓存

### 缓存优化
- [ ] 配置 Redis 参数
- [ ] 设计合适的缓存策略
- [ ] 实现缓存穿透防护
- [ ] 监控缓存命中率
- [ ] 配置缓存失效策略

### 网络优化
- [ ] 配置连接池
- [ ] 启用 HTTP/2
- [ ] 优化 TCP 参数
- [ ] 配置负载均衡
- [ ] 启用压缩

### 应用优化
- [ ] 异步处理
- [ ] 批量操作
- [ ] 数据预加载
- [ ] 连接复用
- [ ] 减少序列化开销

---

## 🎯 优化建议

### 短期优化 (1-2周)
1. **数据库索引优化**: 为常用查询添加索引
2. **JVM 参数调优**: 调整堆内存和 GC 参数
3. **缓存配置**: 配置 Redis 缓存和连接池
4. **代码优化**: 减少 N+1 查询，优化循环

### 中期优化 (1-2个月)
1. **数据库分库分表**: 对大表进行水平拆分
2. **CDN 加速**: 使用 CDN 加速静态资源
3. **异步处理**: 将耗时操作改为异步
4. **链路追踪**: 引入分布式链路追踪

### 长期优化 (3-6个月)
1. **服务拆分**: 进一步细化服务拆分
2. **数据库中间件**: 引入 ShardingSphere
3. **消息队列**: 使用 MQ 解耦系统
4. **容器化部署**: 使用 Docker/K8s 部署

---

## 📊 性能基准

### 服务性能目标

| 服务类型 | 响应时间 (P95) | 吞吐量 (QPS) | 可用性 |
|----------|----------------|-------------|--------|
| 用户服务 | < 100ms | 1000+ | > 99.9% |
| 认证服务 | < 50ms | 2000+ | > 99.95% |
| 业务服务 | < 200ms | 500+ | > 99.9% |
| 监控服务 | < 500ms | 200+ | > 99.5% |

### 资源利用目标

| 资源类型 | 目标使用率 | 说明 |
|----------|-----------|------|
| CPU | < 80% | 保证系统稳定性 |
| 内存 | < 80% | 避免 OOM |
| 磁盘 I/O | < 70% | 保证数据读写性能 |
| 网络 | < 70% | 避免网络瓶颈 |

---

## 🎓 最佳实践

### 1. 性能测试
- **测试环境**: 使用与生产环境一致的配置
- **数据量**: 使用与生产环境相当的数据量
- **测试场景**: 覆盖正常、峰值、异常场景
- **持续测试**: 定期进行性能回归测试

### 2. 监控告警
- **指标采集**: 全方位采集性能指标
- **告警规则**: 设置合理的告警阈值
- **日志分析**: 分析错误日志和慢日志
- **容量规划**: 根据业务增长规划容量

### 3. 持续优化
- **定期评审**: 每月进行性能评审
- **优化迭代**: 持续优化性能瓶颈
- **技术升级**: 及时升级技术组件
- **知识分享**: 分享性能优化经验

---

## 📚 参考资料

- [JVM 调优指南](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/)
- [MySQL 优化手册](https://dev.mysql.com/doc/)
- [Redis 调优指南](https://redis.io/docs/manual/optimization/)
- [Spring Boot 性能优化](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
