# Flyway 数据库迁移快速开始

## 快速配置

### 1. 启用 Flyway

在 `application.yml` 中添加：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### 2. 创建迁移脚本

在 `src/main/resources/db/migration/` 目录下创建迁移脚本：

```sql
-- V1.0.1__Create_User_Table.sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 3. 启动应用

应用启动时，Flyway 会自动执行待执行的迁移脚本。

## 使用 API

### 编程方式

```java
@Autowired
private MigrationService migrationService;

// 获取迁移信息
String info = migrationService.info();
System.out.println(info);

// 获取迁移历史
List<MigrationInfo> history = migrationService.getMigrationHistory();

// 获取当前版本
String version = migrationService.getCurrentVersion();
```

### REST API

```bash
# 获取迁移信息
curl http://localhost:8080/api/database/migration/info

# 获取迁移历史
curl http://localhost:8080/api/database/migration/history

# 获取当前版本
curl http://localhost:8080/api/database/migration/version

# 执行迁移（需要权限）
curl -X POST http://localhost:8080/api/database/migration/migrate

# 验证迁移脚本
curl -X POST http://localhost:8080/api/database/migration/validate
```

## 迁移脚本命名规范

```
V<版本号>__<描述>.sql
```

示例：
- `V1.0.1__Create_User_Table.sql`
- `V1.0.2__Add_User_Status_Column.sql`
- `V2.0.0__Refactor_Permission_System.sql`

## 常见操作

### 添加新列

```sql
-- V1.0.3__Add_User_Phone_Column.sql
ALTER TABLE sys_user 
ADD COLUMN phone VARCHAR(20) COMMENT '手机号' AFTER email;

CREATE INDEX idx_phone ON sys_user(phone);
```

### 数据迁移

```sql
-- V1.0.4__Migrate_User_Status.sql
UPDATE sys_user 
SET status = 'ACTIVE' 
WHERE status IS NULL;
```

### 创建索引

```sql
-- V1.0.5__Add_User_Indexes.sql
CREATE INDEX idx_created_time ON sys_user(created_time);
CREATE INDEX idx_email ON sys_user(email);
```

## 故障处理

### 迁移失败

如果迁移失败：

1. 查看错误日志
2. 修复迁移脚本
3. 执行修复：
   ```java
   migrationService.repair();
   ```
4. 重新迁移：
   ```java
   migrationService.migrate();
   ```

### 基线化现有数据库

对于已有数据的数据库：

```java
migrationService.baseline("1.0.0");
```

或通过 API：

```bash
curl -X POST "http://localhost:8080/api/database/migration/baseline?version=1.0.0"
```

## 最佳实践

1. **版本号递增**：确保新的迁移脚本版本号大于已有的
2. **幂等性**：使用 `IF NOT EXISTS` 等语句
3. **向后兼容**：避免删除列，考虑添加新列
4. **测试验证**：在测试环境充分测试
5. **备份数据**：重要操作前先备份

## 更多信息

详细文档请参考：[FLYWAY_MIGRATION_GUIDE.md](FLYWAY_MIGRATION_GUIDE.md)
