[根目录](../../CLAUDE.md) > **basebackend-backup**

# basebackend-backup

## 模块职责

数据库备份与恢复库。支持MySQL/PostgreSQL全量备份、增量备份(Binlog/WAL)、备份链管理、多副本存储(本地+S3)、校验和验证、分布式锁防并发、自动调度。

## 对外接口

- `MySQLBackupService`: MySQL备份服务接口
- `DataSourceBackupExecutor`: 通用备份执行器
- `RestoreService`: 恢复服务
- `IncrementalChainManager`: 增量备份链管理
- `StorageStrategyExecutor`: 多副本存储策略
- `AutoBackupScheduler`: 自动备份调度器
- `MetricsController`: 备份指标端点

## 关键依赖

- Redisson (分布式锁)
- AWS S3 SDK (对象存储)
- Spring Boot自动配置

## 数据模型

- BackupTask: 备份任务
- BackupHistory: 备份历史
- RestoreRecord: 恢复记录

## 测试与质量

4个测试: ChecksumServiceTest, RedissonLockManagerTest, RetryTemplateTest, MySQLBackupServiceTest, BackupIntegrationTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
