# Flyway 数据库迁移指南

## 概述

本模块集成了 Flyway 数据库迁移工具，提供版本化的数据库管理能力。

## 功能特性

- ✅ 自动执行数据库迁移脚本
- ✅ 迁移历史记录和查询
- ✅ 迁移脚本验证
- ✅ 失败迁移修复
- ✅ 数据库基线化
- ✅ RESTful API 管理接口

## 配置说明

### 1. 启用 Flyway

在 `application.yml` 或 `application-database-enhanced.yml` 中配置：

```yaml
spring:
  flyway:
    enabled: true                          # 启用 Flyway
    locations: classpath:db/migration      # 迁移脚本位置
    baseline-version: 1.0.0                # 基线版本
    baseline-on-migrate: true              # 首次迁移时创建基线
    validate-on-migrate: true              # 迁移前验证
    out-of-order: false                    # 不允许乱序执行
    encoding: UTF-8                        # 脚本编码
    placeholder-replacement: true          # 启用占位符替换
```

### 2. 迁移脚本命名规范

迁移脚本必须遵循以下命名规范：

```
V<版本号>__<描述>.sql
```

示例：
- `V1.0.1__Create_Audit_Log_Table.sql`
- `V1.0.2__Add_User_Email_Column.sql`
- `V2.0.0__Refactor_Permission_System.sql`

**命名规则：**
- 版本号使用点分隔（如 1.0.1, 2.0.0）
- 版本号和描述之间使用双下划线 `__` 分隔
- 描述使用下划线分隔单词
- 文件扩展名为 `.sql`

### 3. 迁移脚本位置

默认位置：`src/main/resources/db/migration/`

```
db/
└── migration/
    ├── V1.0.1__Create_Audit_Log_Table.sql
    ├── V1.0.2__Create_Audit_Log_Archive_Table.sql
    ├── V1.0.3__Create_Tenant_Config_Table.sql
    └── V1.0.4__Create_Migration_Backup_Table.sql
```

## 使用方式

### 1. 编程方式

```java
@Autowired
private MigrationService migrationService;

// 执行迁移
String result = migrationService.migrate();

// 获取迁移历史
List<MigrationInfo> history = migrationService.getMigrationHistory();

// 获取待执行的迁移
List<MigrationInfo> pending = migrationService.getPendingMigrations();

// 获取当前版本
String version = migrationService.getCurrentVersion();

// 验证迁移脚本
String validateResult = migrationService.validate();

// 修复失败的迁移
String repairResult = migrationService.repair();

// 基线化数据库
String baselineResult = migrationService.baseline("1.0.0");
```

### 2. REST API 方式

#### 获取迁移信息

```bash
GET /api/database/migration/info
```

响应：
```json
{
  "info": "数据库迁移信息 - 当前版本: 1.0.4, 总迁移数: 4, 已应用: 4, 待执行: 0, 失败: 0",
  "currentVersion": "1.0.4"
}
```

#### 获取迁移历史（分页）

```bash
GET /api/database/migration/history?page=0&size=20
```

响应：
```json
{
  "content": [
    {
      "version": "1.0.4",
      "description": "Create Migration Backup Table",
      "type": "SQL",
      "script": "V1.0.4__Create_Migration_Backup_Table.sql",
      "checksum": 123456789,
      "installedRank": 4,
      "installedOn": "2025-11-20T10:30:00",
      "installedBy": "root",
      "executionTime": 150,
      "state": "Success",
      "success": true
    }
  ],
  "totalElements": 4,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

#### 获取待执行的迁移

```bash
GET /api/database/migration/pending
```

#### 执行迁移

```bash
POST /api/database/migration/migrate
```

#### 验证迁移脚本

```bash
POST /api/database/migration/validate
```

#### 修复迁移记录

```bash
POST /api/database/migration/repair
```

#### 基线化数据库

```bash
POST /api/database/migration/baseline?version=1.0.0
```

## 迁移脚本编写指南

### 1. 基本结构

```sql
-- 描述迁移的目的
-- 作者：xxx
-- 日期：2025-11-20

-- 创建表
CREATE TABLE IF NOT EXISTS table_name (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表说明';

-- 插入初始数据（如果需要）
INSERT INTO table_name (name) VALUES ('初始数据');
```

### 2. 修改表结构

```sql
-- 添加列
ALTER TABLE table_name ADD COLUMN new_column VARCHAR(50) COMMENT '新列';

-- 修改列
ALTER TABLE table_name MODIFY COLUMN existing_column VARCHAR(100) NOT NULL;

-- 删除列
ALTER TABLE table_name DROP COLUMN old_column;

-- 添加索引
CREATE INDEX idx_column_name ON table_name(column_name);

-- 删除索引
DROP INDEX idx_old_column ON table_name;
```

### 3. 数据迁移

```sql
-- 数据转换
UPDATE table_name SET new_column = CONCAT('prefix_', old_column);

-- 数据清理
DELETE FROM table_name WHERE status = 'DELETED';
```

### 4. 最佳实践

1. **使用事务**：Flyway 默认在事务中执行迁移脚本
2. **幂等性**：使用 `IF NOT EXISTS` 等语句确保脚本可重复执行
3. **向后兼容**：避免删除或重命名列，考虑使用新列
4. **数据备份**：重要数据迁移前先备份
5. **测试验证**：在测试环境充分测试后再应用到生产环境

## 故障处理

### 1. 迁移失败

如果迁移失败，Flyway 会记录失败状态。需要：

1. 修复失败的迁移脚本
2. 执行修复命令：
   ```java
   migrationService.repair();
   ```
3. 重新执行迁移：
   ```java
   migrationService.migrate();
   ```

### 2. 校验和不匹配

如果修改了已执行的迁移脚本，会导致校验和不匹配。解决方法：

1. 不要修改已执行的脚本，创建新的迁移脚本
2. 如果必须修改，执行修复命令：
   ```java
   migrationService.repair();
   ```

### 3. 基线化现有数据库

对于已有数据的数据库，首次使用 Flyway 时需要基线化：

```java
migrationService.baseline("1.0.0");
```

## 生产环境注意事项

### 1. 权限控制

迁移 API 应该有严格的权限控制，建议：
- 只允许管理员访问
- 使用 IP 白名单
- 记录所有迁移操作的审计日志

### 2. 确认机制

生产环境迁移应该：
- 需要额外的确认步骤
- 在维护窗口期执行
- 提前通知相关人员

### 3. 备份策略

执行迁移前：
- 备份数据库
- 备份迁移脚本
- 准备回滚方案

### 4. 监控告警

- 监控迁移执行时间
- 监控迁移失败情况
- 设置告警通知

## 示例场景

### 场景 1：新增表

```sql
-- V1.0.5__Create_User_Profile_Table.sql
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料表';
```

### 场景 2：添加列

```sql
-- V1.0.6__Add_User_Status_Column.sql
ALTER TABLE sys_user 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态' AFTER email;

CREATE INDEX idx_status ON sys_user(status);
```

### 场景 3：数据迁移

```sql
-- V1.0.7__Migrate_User_Data.sql
-- 备份旧数据
INSERT INTO sys_migration_backup (migration_version, table_name, backup_data)
SELECT '1.0.7', 'sys_user', JSON_OBJECT('id', id, 'old_field', old_field)
FROM sys_user WHERE old_field IS NOT NULL;

-- 迁移数据
UPDATE sys_user 
SET new_field = CONCAT('migrated_', old_field)
WHERE old_field IS NOT NULL;
```

## 常见问题

### Q1: 如何跳过某个迁移？

A: Flyway 不支持跳过迁移。如果需要，可以：
1. 创建一个空的迁移脚本占位
2. 或者使用 `repair()` 删除失败的迁移记录

### Q2: 如何回滚迁移？

A: Flyway 社区版不支持自动回滚。需要：
1. 手动编写回滚脚本
2. 或者从备份恢复数据库

### Q3: 迁移脚本可以使用占位符吗？

A: 可以。在配置中启用 `placeholder-replacement: true`，然后在脚本中使用 `${placeholder}`。

## 相关文档

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [数据库增强功能文档](DATABASE_ENHANCEMENT_README.md)
- [审计系统文档](AUDIT_ARCHIVE_IMPLEMENTATION.md)
