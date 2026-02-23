# 迁移失败处理和数据备份

本文档描述了数据库迁移的失败处理、数据备份和生产环境确认机制。

## 功能概述

### 1. 迁移失败自动回滚

当迁移失败时，系统会自动调用 `repair()` 方法清理失败的迁移记录，确保数据库状态一致。

### 2. 迁移前数据备份

在执行迁移前，可以创建数据库备份，以便在迁移失败时恢复数据。

### 3. 生产环境确认机制

在生产环境执行迁移时，需要先生成确认令牌，然后使用令牌确认后才能执行迁移。

## API 接口

### 迁移相关接口

#### 1. 执行迁移（带备份）

```http
POST /api/database/migration/migrate-with-backup?createBackup=true
```

**参数：**
- `createBackup`: 是否创建备份（默认 true）

**响应：**
```json
{
  "message": "数据库迁移完成。目标版本: 2.0.0, 执行的迁移数: 3 (备份ID: backup_2_0_0_20250120_143022)"
}
```

#### 2. 执行迁移（生产环境确认）

```http
POST /api/database/migration/migrate-with-confirmation
Content-Type: application/json

{
  "confirmationToken": "550e8400-e29b-41d4-a716-446655440000",
  "confirmedBy": "admin",
  "reason": "部署新版本",
  "createBackup": true
}
```

**响应：**
```json
{
  "message": "数据库迁移完成。目标版本: 2.0.0, 执行的迁移数: 3 (备份ID: backup_2_0_0_20250120_143022)"
}
```

#### 3. 回滚到指定版本

```http
POST /api/database/migration/rollback?targetVersion=1.0.0
```

**响应：**
```json
{
  "message": "回滚完成。从版本 2.0.0 回滚到 1.0.0。注意：这只是标记了版本，实际数据需要从备份恢复。"
}
```

#### 4. 生成确认令牌

```http
POST /api/database/migration/generate-token
```

**响应：**
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "validityMinutes": "30"
}
```

#### 5. 验证确认令牌

```http
GET /api/database/migration/validate-token?token=550e8400-e29b-41d4-a716-446655440000
```

**响应：**
```json
{
  "valid": true
}
```

### 备份相关接口

#### 1. 创建备份

```http
POST /api/database/migration/backup/create?migrationVersion=2.0.0
```

**参数：**
- `migrationVersion`: 迁移版本号
- `tables`: 需要备份的表列表（可选，为空则备份所有表）

**响应：**
```json
{
  "backupId": "backup_2_0_0_20250120_143022",
  "migrationVersion": "2.0.0",
  "backupPath": "./backups/backup_2_0_0_20250120_143022.sql",
  "backupTime": "2025-01-20T14:30:22",
  "backupSize": 1048576,
  "status": "SUCCESS",
  "description": "备份 15 个表",
  "restored": false
}
```

#### 2. 获取备份列表

```http
GET /api/database/migration/backup/list
```

**响应：**
```json
[
  {
    "backupId": "backup_2_0_0_20250120_143022",
    "migrationVersion": "2.0.0",
    "backupPath": "./backups/backup_2_0_0_20250120_143022.sql",
    "backupTime": "2025-01-20T14:30:22",
    "backupSize": 1048576,
    "status": "SUCCESS",
    "restored": false
  }
]
```

#### 3. 获取备份详情

```http
GET /api/database/migration/backup/{backupId}
```

#### 4. 恢复备份

```http
POST /api/database/migration/backup/{backupId}/restore
```

**响应：**
```json
{
  "message": "备份恢复成功"
}
```

#### 5. 删除备份

```http
DELETE /api/database/migration/backup/{backupId}
```

**响应：**
```json
{
  "message": "备份删除成功"
}
```

## 使用场景

### 场景 1：开发/测试环境迁移

在开发或测试环境，可以直接执行迁移，不需要确认：

```bash
curl -X POST http://localhost:8080/api/database/migration/migrate
```

### 场景 2：生产环境迁移（带备份）

在生产环境，建议先创建备份再执行迁移：

```bash
# 执行迁移并自动创建备份
curl -X POST http://localhost:8080/api/database/migration/migrate-with-backup?createBackup=true
```

### 场景 3：生产环境迁移（需要确认）

在生产环境，如果配置了需要确认，则需要先生成令牌：

```bash
# 1. 生成确认令牌
TOKEN=$(curl -X POST http://localhost:8080/api/database/migration/generate-token | jq -r '.token')

# 2. 使用令牌执行迁移
curl -X POST http://localhost:8080/api/database/migration/migrate-with-confirmation \
  -H "Content-Type: application/json" \
  -d "{
    \"confirmationToken\": \"$TOKEN\",
    \"confirmedBy\": \"admin\",
    \"reason\": \"部署新版本\",
    \"createBackup\": true
  }"
```

### 场景 4：迁移失败后恢复

如果迁移失败，可以从备份恢复：

```bash
# 1. 查看备份列表
curl http://localhost:8080/api/database/migration/backup/list

# 2. 恢复指定备份
curl -X POST http://localhost:8080/api/database/migration/backup/{backupId}/restore
```

### 场景 5：回滚到旧版本

如果需要回滚到旧版本：

```bash
# 1. 回滚版本标记
curl -X POST http://localhost:8080/api/database/migration/rollback?targetVersion=1.0.0

# 2. 从备份恢复数据
curl -X POST http://localhost:8080/api/database/migration/backup/{backupId}/restore
```

## 配置

在 `application.yml` 中配置迁移相关参数：

```yaml
database:
  enhanced:
    migration:
      # 备份目录
      backup-dir: ./backups
      # 是否在迁移前自动创建备份
      auto-backup: true
      # 生产环境是否需要确认
      require-confirmation: true
      # 确认令牌有效期（分钟）
      token-validity-minutes: 30
      # 迁移失败时是否自动回滚
      auto-rollback: true

spring:
  profiles:
    active: prod  # 设置为 prod 或 production 时会启用确认机制
```

## 注意事项

1. **备份文件管理**：备份文件存储在本地文件系统，建议定期清理旧备份或将备份文件转移到其他存储。

2. **权限控制**：所有迁移和备份相关的接口都应该有严格的权限控制，建议只允许管理员访问。

3. **生产环境确认**：确认令牌有 30 分钟有效期，过期后需要重新生成。

4. **回滚限制**：Flyway 不直接支持回滚，`rollbackToVersion` 方法只是标记版本，实际数据需要从备份恢复。

5. **备份性能**：备份大型数据库可能需要较长时间，建议在低峰期执行。

6. **迁移失败处理**：迁移失败时会自动调用 `repair()` 清理失败记录，但不会自动恢复数据，需要手动从备份恢复。

## 最佳实践

1. **始终创建备份**：在生产环境执行迁移前，始终创建备份。

2. **测试迁移脚本**：在生产环境执行前，先在测试环境验证迁移脚本。

3. **保留备份**：至少保留最近 3 次迁移的备份。

4. **监控迁移**：监控迁移执行时间和结果，及时发现问题。

5. **文档记录**：记录每次迁移的原因、执行人和结果。

6. **回滚计划**：在执行迁移前准备好回滚计划。
