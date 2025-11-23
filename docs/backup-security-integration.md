# Backup & Security 集成说明（user-api 与 system-api）

## 1. 依赖继承
- `basebackend-user-api` 与 `basebackend-system-api` 已引入 `basebackend-backup`（安全模块原有依赖保留），默认启用自动配置。

## 2. 配置获取
- 两个模块的 `bootstrap.yml` 已新增共享配置 `backup-config.yml`（同时保留 `security-config.yml`）。
- 模板文件：
  - `config/backup-config.yml`：备份与存储、增量备份、Redis/MySQL/监控等默认值。
  - `config/security-config.yml`：Web 安全基线的允许来源与 Referer 校验。
- 推送到 Nacos 的建议：
  1) `Data ID`: `backup-config.yml`，`Group`: `DEFAULT_GROUP`，类型 `yaml`，内容取自模板并按环境修改。
  2) `Data ID`: `security-config.yml`，`Group`: `DEFAULT_GROUP`，类型 `yaml`，内容取自模板并按前端域名调整。
  3) 确认 `bootstrap.yml` 中的 `shared-configs` 已包含以上 Data ID（已就绪）。

## 3. 关键配置项速览
- 备份：`backup.enabled` 开关、`backup.backup-path`、`backup.storage.local.base-path`、`backup.storage.s3.*`、`backup.retention-days`、`backup.incremental.mysql/postgres.*`、`backup.metrics.enabled`。
- 分布式锁/重试：`backup.distributed-lock.*`，`backup.retry.*`。
- 安全基线：`security.baseline.allowed-origins`、`security.baseline.enforce-referer`。

## 4. 验证建议
- 启动 user-api / system-api 后检查日志是否打印 “备份模块已启用”，并确认 Nacos 拉取 `backup-config.yml` / `security-config.yml` 成功。
- 访问 `/actuator/health` 与 `/actuator/metrics` 验证监控指标加载，必要时在 `backup.metrics.enabled` 置为 `true`。
