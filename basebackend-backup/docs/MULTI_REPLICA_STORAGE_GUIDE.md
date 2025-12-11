# 多副本存储策略使用指南

## 概述

多副本存储策略允许备份数据同时存储到多个位置（如本地存储 + S3云存储），显著提高数据安全性和可靠性。即使某个存储位置发生故障，其他副本仍然可以正常工作。

### 核心特性

✅ **同时多副本** - 并行写入多个存储位置
✅ **自动故障转移** - 副本失败不影响其他副本
✅ **可配置副本** - 灵活配置存储类型和优先级
✅ **异步执行** - 高效的并发处理
✅ **智能重试** - 失败副本自动重试
✅ **统计监控** - 详细的副本状态报告

## 快速开始

### 1. 启用多副本存储

在 `application-backup.yml` 中启用：

```yaml
backup:
  storage:
    # 启用多副本策略
    multi-replica:
      enabled: true
```

### 2. 配置副本存储

```yaml
backup:
  storage:
    # 本地存储
    local:
      enabled: true
      base-path: /data/backups
      retention-days: 7

    # S3云存储
    s3:
      enabled: true
      endpoint: https://s3.amazonaws.com
      bucket: my-backup-bucket
      access-key: ${S3_ACCESS_KEY}
      secret-key: ${S3_SECRET_KEY}
      region: us-east-1
      retention-days: 30

    # 多副本策略配置
    multi-replica:
      enabled: true
      replicas:
        # 副本1: 本地存储（优先级1）
        - type: local
          priority: 1
          enabled: true

        # 副本2: S3存储（优先级2）
        - type: s3
          priority: 2
          enabled: true
```

## 配置详解

### 副本配置项

每个副本包含以下配置项：

| 配置项 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| type | String | 存储类型 | local, s3, oss |
| priority | Integer | 优先级（1最高） | 1, 2, 3 |
| enabled | Boolean | 是否启用 | true, false |

### 存储类型说明

#### 1. 本地存储副本

```yaml
backup:
  storage:
    multi-replica:
      replicas:
        - type: local
          priority: 1
          enabled: true
```

**特点：**
- 快速访问，适合热数据
- 成本低，无网络传输
- 适合小规模备份

**适用场景：**
- 快速恢复场景
- 网络受限环境
- 临时存储需求

#### 2. S3云存储副本

```yaml
backup:
  storage:
    s3:
      enabled: true
      bucket: backup-bucket
      region: us-east-1

    multi-replica:
      replicas:
        - type: s3
          priority: 2
          enabled: true
```

**特点：**
- 高可靠性，99.999999999% (11 9's)
- 跨地域复制能力
- 生命周期管理

**适用场景：**
- 长期归档存储
- 灾备恢复
- 跨地域访问

#### 3. 混合副本策略

```yaml
backup:
  storage:
    local:
      enabled: true
      base-path: /data/backups
      retention-days: 3  # 短期保留

    s3:
      enabled: true
      bucket: backup-archive
      region: us-east-1
      retention-days: 365  # 长期保留

    multi-replica:
      enabled: true
      replicas:
        # 快速访问副本（3天）
        - type: local
          priority: 1
          enabled: true

        # 归档副本（1年）
        - type: s3
          priority: 2
          enabled: true
```

## 高级配置

### 1. 自定义副本优先级

```yaml
backup:
  storage:
    multi-replica:
      enabled: true
      replicas:
        # 最高优先级：本地SSD存储
        - type: local
          priority: 1
          enabled: true

        # 次优先级：同城S3
        - type: s3
          priority: 2
          enabled: true

        # 最低优先级：异地S3
        - type: s3
          priority: 3
          enabled: true
```

### 2. 条件启用副本

```yaml
backup:
  storage:
    s3:
      enabled: ${BACKUP_S3_ENABLED:false}

    multi-replica:
      enabled: true
      replicas:
        # 本地副本（始终启用）
        - type: local
          priority: 1
          enabled: true

        # S3副本（根据环境变量控制）
        - type: s3
          priority: 2
          enabled: ${BACKUP_S3_ENABLED:false}
```

### 3. 多地域副本

```yaml
backup:
  storage:
    # 亚洲副本
    s3-asia:
      enabled: true
      endpoint: https://s3.ap-southeast-1.amazonaws.com
      bucket: backup-asia
      region: ap-southeast-1

    # 欧洲副本
    s3-eu:
      enabled: true
      endpoint: https://s3.eu-west-1.amazonaws.com
      bucket: backup-eu
      region: eu-west-1

    # 美洲副本
    s3-us:
      enabled: true
      endpoint: https://s3.us-east-1.amazonaws.com
      bucket: backup-us
      region: us-east-1

    multi-replica:
      enabled: true
      replicas:
        - type: s3-asia
          priority: 1
          enabled: true
        - type: s3-eu
          priority: 2
          enabled: true
        - type: s3-us
          priority: 3
          enabled: true
```

## 工作原理

### 1. 存储流程

```
开始备份
    ↓
获取文件
    ↓
启动多副本任务
    ↓
创建任务线程池
    ↓
并行执行各副本上传
    ↓
等待所有副本完成
    ↓
验证结果
    ↓
返回成功/失败
```

### 2. 失败处理

```
副本上传失败
    ↓
记录错误日志
    ↓
继续其他副本
    ↓
检查成功率
    ↓
至少50%成功 → 成功
    ↓
小于50%成功 → 失败
```

### 3. 重试机制

每个副本失败后会自动重试，重试次数由 `backup.retry.max-attempts` 配置：

```yaml
backup:
  retry:
    max-attempts: 5
    backoff:
      initial: 2s
      multiplier: 2.0
      max: 1m
```

## 编程接口

### 1. 基本使用

```java
@Autowired
private StorageStrategyExecutor executor;

// 执行多副本存储
List<StorageResult> results = executor.execute(
    backupFile,     // 备份文件
    "backup-bucket", // 存储桶
    "mysql/backup-2024-01-01.sql"  // 对象键名
);

log.info("多副本存储完成: {} 个副本成功", results.size());
```

### 2. 验证副本结果

```java
// 验证至少50%副本成功
boolean isValid = executor.validateMultiReplica(results);

if (!isValid) {
    log.error("多副本存储失败：成功率低于50%");
    return;
}

// 获取统计信息
StorageStrategyExecutor.MultiReplicaStats stats = executor.getStats(results);

log.info("存储统计: 总数={}, 成功={}, 失败={}, 成功率={:.1f}%",
    stats.getTotalReplicas(),
    stats.getSuccessReplicas(),
    stats.getFailedReplicas(),
    stats.getSuccessRate() * 100);
```

### 3. 清理过期副本

```java
// 清理30天前的副本
executor.cleanupExpiredReplicas(results, 30);
```

## 性能优化

### 1. 并行度调优

系统默认使用10个线程的线程池：

```yaml
# 可以在代码中自定义线程池大小
StorageStrategyExecutor executor = new StorageStrategyExecutor();
executor.setThreadPoolSize(20);  // 增加并行度
```

### 2. 网络优化

```yaml
backup:
  storage:
    s3:
      # 连接池配置
      connection-pool-size: 50
      # 超时配置
      connection-timeout: 60s
      read-timeout: 300s
      # 分块大小（提高大文件上传效率）
      multipart-chunk-size: 32MB
```

### 3. 批量上传优化

```yaml
backup:
  storage:
    # 启用批量上传模式
    batch:
      enabled: true
      batch-size: 10  # 每批处理10个文件
      parallel-batches: 3  # 并行处理3个批次
```

## 监控告警

### 1. 副本成功率监控

```yaml
# Prometheus指标示例
backup_multi_replica_success_rate{
    task_id="123",
    backup_type="full"
}
```

### 2. 告警规则

```yaml
# Alertmanager告警规则
groups:
  - name: backup_multi_replica
    rules:
      - alert: MultiReplicaLowSuccessRate
        expr: backup_multi_replica_success_rate < 0.5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "备份多副本成功率过低"
          description: "多副本存储成功率低于50%，请检查存储服务状态"
```

## 故障排查

### 1. 副本失败

**症状：** 部分副本上传失败

**排查步骤：**
```bash
# 检查存储服务状态
curl -I https://s3.amazonaws.com/bucket

# 检查网络连通性
ping s3.amazonaws.com

# 查看详细日志
grep "StorageStrategyExecutor" /var/log/backup/backup.log
```

**可能原因：**
- 网络中断
- 存储服务不可用
- 认证失败
- 磁盘空间不足

**解决方案：**
```yaml
# 增加重试次数
backup:
  retry:
    max-attempts: 10  # 增加到10次

# 降低并发度
backup:
  storage:
    multi-replica:
      parallel-threads: 5  # 减少到5个线程
```

### 2. 所有副本失败

**症状：** 所有副本都无法上传

**排查步骤：**
```bash
# 检查磁盘空间
df -h

# 检查网络
ping 8.8.8.8

# 查看错误日志
tail -f /var/log/backup/backup.log | grep ERROR
```

**解决方案：**
1. 清理磁盘空间
2. 检查网络配置
3. 验证存储凭证
4. 降低文件大小（分批备份）

## 最佳实践

### 1. 副本策略建议

| 数据类型 | 副本配置 | 保留策略 |
|---------|----------|----------|
| 核心业务数据 | 本地 + S3 | 本地3天，S3长期 |
| 日志文件 | 本地 + S3 | 本地7天，S3 30天 |
| 归档数据 | 仅S3 | S3 1年+ |
| 开发环境 | 本地 | 本地7天 |

### 2. 成本控制

```yaml
# 合理设置保留期限
backup:
  storage:
    local:
      retention-days: 7  # 本地短期保留
    s3:
      retention-days: 30  # S3中期保留
      # 启用生命周期策略（自动转低频存储）
      lifecycle-enabled: true
```

### 3. 性能调优

```yaml
# 根据硬件性能调整
backup:
  storage:
    # 增加文件分块大小（适合大文件）
    s3:
      multipart-chunk-size: 64MB  # 提高大文件上传效率

    # 减少并发数（适合小文件）
    multi-replica:
      parallel-threads: 5  # 控制并发数
```

### 4. 安全建议

```yaml
# 启用加密
backup:
  storage:
    s3:
      # 服务端加密
      encryption: AES256
      # KMS密钥
      kms-key-id: arn:aws:kms:...

    # 传输加密
    ssl:
      enabled: true
      verify: true
```

## 示例配置

### 完整配置示例

```yaml
backup:
  # 启用备份系统
  enabled: true

  # 存储配置
  storage:
    # 本地存储
    local:
      enabled: true
      base-path: /data/backups
      retention-days: 3

    # S3存储
    s3:
      enabled: true
      endpoint: https://s3.amazonaws.com
      bucket: ${BACKUP_S3_BUCKET}
      access-key: ${S3_ACCESS_KEY}
      secret-key: ${S3_SECRET_KEY}
      region: us-east-1
      retention-days: 90
      multipart-chunk-size: 32MB
      connection-timeout: 60s
      read-timeout: 300s

    # 多副本策略
    multi-replica:
      enabled: true
      replicas:
        - type: local
          priority: 1
          enabled: true
        - type: s3
          priority: 2
          enabled: true

  # 重试配置
  retry:
    max-attempts: 5
    backoff:
      initial: 2s
      multiplier: 2.0
      max: 1m

  # 监控配置
  metrics:
    enabled: true
    prefix: backup

  # 通知配置
  notify:
    email:
      enabled: true
      smtp-host: ${SMTP_HOST}
      smtp-port: 587
      username: ${SMTP_USERNAME}
      password: ${SMTP_PASSWORD}
      recipients: admin@example.com
```

## 参考资源

- [AWS S3 最佳实践](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Welcome.html)
- [阿里云OSS 多副本存储](https://help.aliyun.com/document_detail/33554.html)
- [腾讯云COS 存储桶复制](https://cloud.tencent.com/document/product/436/19235)
- [MinIO 分布式存储](https://docs.min.io/)

---

**注意事项：**
- 多副本存储会增加存储成本，但显著提高数据安全性
- 建议监控各副本的成功率，及时发现存储服务异常
- 合理设置副本优先级，平衡性能与成本
