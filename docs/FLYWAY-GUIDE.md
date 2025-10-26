# Flyway数据库迁移使用指南

## 📚 目录

- [简介](#简介)
- [快速开始](#快速开始)
- [迁移脚本编写](#迁移脚本编写)
- [执行策略](#执行策略)
- [多环境配置](#多环境配置)
- [最佳实践](#最佳实践)
- [故障排查](#故障排查)
- [FAQ](#faq)

## 简介

### 什么是Flyway？

Flyway是一个开源的数据库迁移工具，采用版本化管理方式，类似于Git对代码的版本控制。

**核心优势**:
- 📝 **版本化**: 每个数据库变更都有明确的版本号
- 🔄 **可重复**: 在任何环境都能重现相同的数据库状态
- 🔒 **安全性**: 防止并发迁移冲突，提供校验机制
- 📊 **可追溯**: 完整的迁移历史记录

### 为什么使用Flyway？

**传统方式的问题**:
```
开发环境 → schema.sql + data.sql
          ↓
测试环境 → 手动执行SQL
          ↓
生产环境 → 😱 不知道执行过哪些SQL，版本混乱
```

**Flyway方式**:
```
V1.0__baseline.sql
V1.1__create_core_tables.sql
V1.2__init_data.sql
V1.3__add_user_avatar.sql
...
所有环境自动执行相同的迁移序列 ✅
```

## 快速开始

### 1. 项目结构

```
basebackend-admin-api/
└── src/main/resources/
    └── db/migration/
        ├── V1.0__init_database.sql          # 基线版本（空文件）
        ├── V1.1__create_core_tables.sql     # 核心表结构
        ├── V1.2__init_data.sql              # 初始数据
        ├── V1.3__create_message_tables.sql  # 消息表
        ├── V1.4__create_nacos_tables.sql    # Nacos配置表
        ├── V1.5__create_scheduler_tables.sql # 调度任务表
        └── V1.6__create_storage_tables.sql  # 存储表
```

### 2. 本地开发环境

#### 方式1: Spring Boot自动迁移（推荐开发使用）

启动应用时自动执行迁移：

```bash
# application-dev.yml已配置自动迁移
mvn spring-boot:run -pl basebackend-admin-api -Dspring-boot.run.profiles=dev
```

日志输出：
```
Flyway Community Edition 9.22.3
Database: jdbc:mysql://localhost:3306/basebackend_admin
Successfully validated 6 migrations (execution time 00:00.028s)
Creating Schema History table `basebackend_admin`.`flyway_schema_history` ...
Current version of schema `basebackend_admin`: << Empty Schema >>
Migrating schema `basebackend_admin` to version "1.0 - init database"
Migrating schema `basebackend_admin` to version "1.1 - create core tables"
...
Successfully applied 6 migrations to schema `basebackend_admin` (execution time 00:02.156s)
```

#### 方式2: Docker Compose一键启动

```bash
# 启动MySQL + Redis + Flyway
./scripts/start-dev-env.sh

# 查看日志
docker-compose -f docker-compose-flyway.yml logs -f flyway-admin

# 停止环境
docker-compose -f docker-compose-flyway.yml down
```

#### 方式3: Maven手动执行

```bash
# 查看迁移历史
./scripts/flyway/info.sh \
  -u jdbc:mysql://localhost:3306/basebackend_admin \
  -U root -p root

# 验证迁移脚本
./scripts/flyway/validate.sh \
  -u jdbc:mysql://localhost:3306/basebackend_admin \
  -U root -p root

# 执行迁移
./scripts/flyway/migrate.sh \
  -u jdbc:mysql://localhost:3306/basebackend_admin \
  -U root -p root
```

### 3. 测试环境

在测试环境启用自动迁移：

```yaml
# application-test.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
```

### 4. 生产环境

**重要**: 生产环境禁用自动迁移，使用脚本手动执行。

```yaml
# application-prod.yml
spring:
  flyway:
    enabled: false  # 禁用自动迁移
```

执行迁移：

```bash
# 1. 备份数据库（必须！）
mysqldump -u root -p basebackend_admin > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 预览待执行的迁移
./scripts/flyway/info.sh \
  -u jdbc:mysql://prod-mysql:3306/basebackend_admin \
  -U admin -p prod_password

# 3. 验证迁移脚本
./scripts/flyway/validate.sh \
  -u jdbc:mysql://prod-mysql:3306/basebackend_admin \
  -U admin -p prod_password

# 4. 执行迁移（需要确认）
./scripts/flyway/migrate.sh \
  -u jdbc:mysql://prod-mysql:3306/basebackend_admin \
  -U admin -p prod_password

# 5. 验证迁移结果
./scripts/flyway/info.sh \
  -u jdbc:mysql://prod-mysql:3306/basebackend_admin \
  -U admin -p prod_password
```

## 迁移脚本编写

### 命名规范

Flyway严格要求脚本命名格式：

```
V<版本号>__<描述>.sql

V    - 版本化迁移（Versioned Migration）前缀
1.7  - 版本号（可以是数字、点、下划线组合）
__   - 两个下划线分隔符
描述 - 简短描述（小写字母+下划线）
.sql - 文件扩展名
```

**示例**:
```
✅ V1.7__add_user_avatar.sql
✅ V1.8__create_order_table.sql
✅ V2.0__refactor_permissions.sql
✅ V2.0.1__fix_user_index.sql

❌ v1.7_add_user_avatar.sql     (小写v)
❌ V1.7_add_user_avatar.sql     (单下划线)
❌ V1.7__Add_User_Avatar.sql    (包含大写字母)
❌ V1.7 add user avatar.sql     (包含空格)
```

### 脚本模板

#### 1. 创建表

```sql
-- V1.7__add_user_avatar.sql
-- 描述: 为用户表添加头像字段

-- 修改已存在的表，使用ALTER TABLE
ALTER TABLE sys_user
    ADD COLUMN avatar VARCHAR(500) COMMENT '用户头像URL' AFTER email;

-- 添加索引
CREATE INDEX idx_avatar ON sys_user(avatar);

-- 创建新表，使用IF NOT EXISTS确保幂等性
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    bio TEXT COMMENT '个人简介',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id),
    FOREIGN KEY fk_user (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户详情表';
```

#### 2. 修改表结构

```sql
-- V1.8__modify_user_table.sql
-- 描述: 修改用户表字段类型

-- 修改字段类型
ALTER TABLE sys_user
    MODIFY COLUMN phone VARCHAR(20) COMMENT '手机号（扩展长度）';

-- 修改字段默认值
ALTER TABLE sys_user
    ALTER COLUMN status SET DEFAULT 1;

-- 重命名字段（MySQL 8.0+推荐使用RENAME COLUMN）
ALTER TABLE sys_user
    RENAME COLUMN old_name TO new_name;

-- 删除字段（谨慎使用！）
-- 建议先在应用代码中停止使用该字段，观察一段时间后再删除
ALTER TABLE sys_user
    DROP COLUMN deprecated_field;
```

#### 3. 数据迁移

```sql
-- V1.9__migrate_user_data.sql
-- 描述: 迁移用户数据到新表

-- 插入数据（使用INSERT IGNORE避免主键冲突）
INSERT IGNORE INTO user_profile (user_id, bio, created_at)
SELECT id, CONCAT('User: ', username), created_at
FROM sys_user
WHERE id NOT IN (SELECT user_id FROM user_profile);

-- 更新数据
UPDATE sys_user
SET avatar = CONCAT('https://avatar.example.com/', username, '.png')
WHERE avatar IS NULL;

-- 数据清理（谨慎使用DELETE！）
DELETE FROM sys_user
WHERE status = 0 AND deleted_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

#### 4. 索引优化

```sql
-- V2.0__optimize_indexes.sql
-- 描述: 优化用户表索引

-- 删除未使用的索引
DROP INDEX idx_old_field ON sys_user;

-- 创建复合索引
CREATE INDEX idx_username_status ON sys_user(username, status);

-- 创建全文索引（MySQL 5.6+）
CREATE FULLTEXT INDEX idx_bio_fulltext ON user_profile(bio);

-- 查看索引使用情况（仅用于分析，不应放在迁移脚本中）
-- SHOW INDEX FROM sys_user;
-- EXPLAIN SELECT * FROM sys_user WHERE username = 'test';
```

### 禁止使用的SQL语句

```sql
-- ❌ 不要使用USE语句（Flyway已连接到正确的数据库）
USE basebackend_admin;

-- ❌ 避免使用DROP TABLE（除非确定要删除）
DROP TABLE IF EXISTS old_table;

-- ⚠️ 谨慎使用TRUNCATE（会删除所有数据）
TRUNCATE TABLE cache_table;

-- ⚠️ 避免使用动态SQL或存储过程（难以版本控制）
DELIMITER $$
CREATE PROCEDURE migrate_data() ...
$$
DELIMITER ;
```

### 脚本编写最佳实践

1. **幂等性**: 脚本可以安全地多次执行
   ```sql
   -- ✅ 好的做法
   CREATE TABLE IF NOT EXISTS ...
   ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...

   -- ❌ 不好的做法
   CREATE TABLE ...  -- 第二次执行会报错
   ```

2. **原子性**: 一个脚本完成一个逻辑变更
   ```
   ✅ V1.7__add_user_avatar.sql        (单一功能)
   ❌ V1.7__update_everything.sql     (太宽泛)
   ```

3. **向后兼容**: 尽量不破坏现有功能
   ```sql
   -- ✅ 添加新字段，设置默认值
   ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(500) DEFAULT '';

   -- ❌ 直接删除字段（可能导致应用报错）
   ALTER TABLE sys_user DROP COLUMN email;

   -- ✅ 删除字段的正确流程：
   -- 1. 应用代码停止使用该字段（发版）
   -- 2. 观察一段时间（1-2周）
   -- 3. 创建迁移脚本删除字段
   ```

4. **性能考虑**: 大表操作要小心
   ```sql
   -- ⚠️ 大表添加索引可能锁表很久
   CREATE INDEX idx_created_at ON sys_user(created_at);
   -- 建议使用ONLINE DDL（MySQL 5.6+）
   ALTER TABLE sys_user ADD INDEX idx_created_at (created_at) ALGORITHM=INPLACE, LOCK=NONE;

   -- ⚠️ 大表数据更新，分批执行
   -- 不好的做法（一次更新百万行）
   UPDATE large_table SET new_field = old_field;

   -- 好的做法（分批更新）
   UPDATE large_table SET new_field = old_field WHERE id BETWEEN 1 AND 10000;
   -- 在应用代码中循环执行，或使用脚本
   ```

## 执行策略

### 开发环境 (dev/local)

**策略**: 自动执行，快速迭代

```yaml
# application-dev.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    clean-disabled: false  # 允许clean（仅开发环境）
```

**特点**:
- 启动应用自动执行迁移
- 可以使用 `mvn flyway:clean` 清空数据库重建
- 快速验证迁移脚本

### 测试环境 (test/staging)

**策略**: 自动执行，接近生产

```yaml
# application-test.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    clean-disabled: true  # 禁止clean
    validate-on-migrate: true
```

**特点**:
- 模拟生产环境
- CI/CD自动验证迁移脚本
- 禁止危险操作

### 生产环境 (production)

**策略**: 手动执行，严格控制

```yaml
# application-prod.yml
spring:
  flyway:
    enabled: false  # 禁用自动迁移
```

**执行流程**:

```bash
# 0. 提前准备
# - 在测试环境验证迁移
# - 准备回滚方案
# - 通知相关人员

# 1. 数据库备份（必须！）
mysqldump -u root -p basebackend_admin > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 预览待执行的迁移
./scripts/flyway/info.sh -u $DB_URL -U $DB_USER -p $DB_PASSWORD

# 3. 验证脚本
./scripts/flyway/validate.sh -u $DB_URL -U $DB_USER -p $DB_PASSWORD

# 4. 执行迁移（脚本会要求确认）
./scripts/flyway/migrate.sh -u $DB_URL -U $DB_USER -p $DB_PASSWORD

# 5. 验证结果
./scripts/flyway/info.sh -u $DB_URL -U $DB_USER -p $DB_PASSWORD

# 6. 应用发版
kubectl apply -k k8s/overlays/prod
```

## 多环境配置

### Spring Boot配置

| 配置项 | Dev | Test | Prod | 说明 |
|-------|-----|------|------|------|
| `enabled` | true | true | false | 是否启用Flyway |
| `baseline-on-migrate` | true | true | true | 对已有数据库启用基线 |
| `clean-disabled` | false | true | true | 禁止clean操作 |
| `validate-on-migrate` | true | true | true | 迁移前验证 |
| `out-of-order` | false | false | false | 禁止乱序迁移 |

### Docker Compose

适用于本地开发环境：

```bash
# 启动环境（自动执行Flyway）
docker-compose -f docker-compose-flyway.yml up -d

# 查看迁移日志
docker-compose -f docker-compose-flyway.yml logs flyway-admin

# 停止环境
docker-compose -f docker-compose-flyway.yml down
```

### Kubernetes

使用InitContainer模式，详见 [Kubernetes Flyway部署指南](../k8s/FLYWAY-K8S-GUIDE.md)。

**快速开始**:

```bash
# 1. 创建Secret
kubectl create secret generic admin-api-secrets \
  --from-literal=database.url="jdbc:mysql://..." \
  --from-literal=database.username="admin" \
  --from-literal=database.password="your-password" \
  -n basebackend

# 2. 创建迁移脚本ConfigMap
./k8s/scripts/create-flyway-migration-configmap.sh -n basebackend

# 3. 部署应用
kubectl apply -k k8s/overlays/dev

# 4. 查看InitContainer日志
kubectl logs <pod-name> -c flyway-migration -n basebackend
```

## 最佳实践

### ✅ DO - 应该做的

1. **版本号递增**
   ```
   V1.0__baseline.sql
   V1.1__create_tables.sql
   V1.2__add_indexes.sql
   V2.0__major_refactor.sql  ← 大版本升级
   ```

2. **使用IF NOT EXISTS**
   ```sql
   CREATE TABLE IF NOT EXISTS users (...);
   ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar VARCHAR(500);
   ```

3. **提供详细注释**
   ```sql
   -- V1.7__add_user_avatar.sql
   -- 描述: 为用户表添加头像功能
   -- 影响: sys_user表
   -- 负责人: zhangsan
   -- 关联需求: JIRA-1234

   ALTER TABLE sys_user
       ADD COLUMN avatar VARCHAR(500) COMMENT '用户头像URL' AFTER email;
   ```

4. **测试环境先验证**
   ```bash
   # 先在测试环境验证
   ./scripts/flyway/migrate.sh -u jdbc:mysql://test-db:3306/basebackend_admin ...

   # 验证通过后再生产执行
   ./scripts/flyway/migrate.sh -u jdbc:mysql://prod-db:3306/basebackend_admin ...
   ```

5. **生产迁移前备份**
   ```bash
   mysqldump -u root -p basebackend_admin > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

6. **使用validate验证**
   ```bash
   # 提交代码前验证脚本
   ./scripts/flyway/validate.sh

   # CI/CD自动验证（已配置在GitHub Actions中）
   ```

### ❌ DON'T - 不应该做的

1. **不要修改已应用的脚本**
   ```
   ❌ 修改 V1.1__create_tables.sql（已在生产执行过）
   ✅ 创建 V1.7__modify_tables.sql（新的迁移）
   ```

2. **不要使用DROP TABLE**
   ```sql
   ❌ DROP TABLE IF EXISTS old_table;
   ✅ -- 暂时保留表，观察一段时间后再决定
      -- 或在应用代码中标记为deprecated
   ```

3. **不要在脚本中使用USE**
   ```sql
   ❌ USE basebackend_admin;  -- Flyway已连接到正确的数据库
   ✅ CREATE TABLE IF NOT EXISTS sys_user (...);
   ```

4. **不要跳跃版本号**
   ```
   V1.0, V1.1, V1.3  ❌ （缺少V1.2）
   V1.0, V1.1, V1.2  ✅
   ```

5. **不要在生产环境启用auto-migrate**
   ```yaml
   # application-prod.yml
   ❌ spring.flyway.enabled: true
   ✅ spring.flyway.enabled: false
   ```

## 故障排查

### 问题1: Checksum mismatch（校验和不匹配）

**现象**:
```
ERROR: Validate failed:
Migration checksum mismatch for migration version 1.1
-> Applied to database : 1234567890
-> Resolved locally    : 9876543210
```

**原因**: 已应用的迁移脚本被修改

**解决方案**:

```bash
# 方案1: 恢复原始脚本（推荐）
git checkout V1.1__create_core_tables.sql

# 方案2: 修复checksum（仅限测试环境）
mvn flyway:repair \
  -Dflyway.url=... \
  -Dflyway.user=... \
  -Dflyway.password=...

# 方案3: 创建新的迁移脚本（生产环境）
# 创建 V1.7__fix_v1.1_issue.sql 修复问题
```

### 问题2: Migration failed（迁移失败）

**现象**:
```
ERROR: Migration V1.7__add_user_avatar.sql failed
SQL State  : 42S21
Error Code : 1060
Message    : Duplicate column name 'avatar'
```

**原因**: SQL语句执行错误（如重复列名、语法错误等）

**解决方案**:

```bash
# 1. 查看迁移历史，确认失败的版本
./scripts/flyway/info.sh -u $DB_URL -U $DB_USER -p $DB_PASSWORD

# 2. 修复失败的脚本
vim basebackend-admin-api/src/main/resources/db/migration/V1.7__add_user_avatar.sql
# 改为: ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS avatar ...

# 3. 手动修复数据库（如果已部分执行）
mysql -u root -p basebackend_admin
# 手动执行必要的清理或修复SQL

# 4. 使用repair标记为已修复
mvn flyway:repair \
  -Dflyway.url=$DB_URL \
  -Dflyway.user=$DB_USER \
  -Dflyway.password=$DB_PASSWORD

# 5. 重新执行迁移
./scripts/flyway/migrate.sh -u $DB_URL -U $DB_USER -p $DB_PASSWORD
```

### 问题3: Baseline version不匹配

**现象**:
```
ERROR: Found non-empty schema(s) `basebackend_admin` but no schema history table.
Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
```

**原因**: 数据库已有表，但没有Flyway历史表

**解决方案**:

已在配置中启用 `baseline-on-migrate: true`，正常情况下不会出现此问题。如果出现：

```bash
# 手动执行baseline
mvn flyway:baseline \
  -Dflyway.url=$DB_URL \
  -Dflyway.user=$DB_USER \
  -Dflyway.password=$DB_PASSWORD \
  -Dflyway.baselineVersion=1.0 \
  -Dflyway.baselineDescription="Baseline existing database"
```

### 问题4: 数据库连接失败

**现象**:
```
ERROR: Unable to obtain connection from database
Communications link failure
```

**原因**: 数据库连接配置错误或数据库未启动

**解决方案**:

```bash
# 1. 检查数据库是否运行
docker ps | grep mysql
# 或
systemctl status mysql

# 2. 测试数据库连接
mysql -h localhost -P 3306 -u root -p

# 3. 检查连接URL配置
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/basebackend_admin?useUnicode=true&characterEncoding=utf8

# 4. 检查防火墙/网络
telnet localhost 3306
```

### 问题5: Out of order migration（乱序迁移）

**现象**:
```
ERROR: Detected resolved migration not applied to database: 1.5
```

**原因**: 团队协作时，其他人提交了更早版本的迁移脚本

**解决方案**:

```bash
# 方案1: 重新编号新脚本（推荐）
git mv V1.5__my_feature.sql V1.7__my_feature.sql

# 方案2: 启用out-of-order（不推荐生产环境）
# application-dev.yml
spring:
  flyway:
    out-of-order: true

# 方案3: 团队约定版本号规则
# - 每个功能分支使用不同的主版本号
# - 合并前协调版本号
```

## FAQ

### Q1: 如何回滚数据库迁移？

**A**: Flyway不支持自动回滚。建议：

1. **备份恢复** （推荐生产环境）
   ```bash
   # 恢复到迁移前的备份
   mysql -u root -p basebackend_admin < backup_20250123_100000.sql
   ```

2. **创建撤销脚本** （适用于简单变更）
   ```sql
   -- V1.8__rollback_v1.7.sql
   ALTER TABLE sys_user DROP COLUMN avatar;
   ```

3. **Flyway Undo** （需要Flyway Teams版本，付费）

### Q2: 多个服务共享数据库，如何管理迁移？

**A**:
- **方案1**: 每个服务独立数据库（推荐微服务架构）
- **方案2**: 一个服务负责schema管理，其他服务只读/写数据
- **方案3**: 使用独立的db-migration项目管理所有迁移脚本

当前项目：admin-api负责管理basebackend_admin数据库

### Q3: 如何处理大表的schema变更？

**A**:

1. **在线DDL** （MySQL 5.6+）
   ```sql
   ALTER TABLE large_table
   ADD INDEX idx_created_at (created_at)
   ALGORITHM=INPLACE, LOCK=NONE;
   ```

2. **分批数据迁移**
   ```sql
   -- 不要一次更新百万行
   UPDATE large_table SET new_field = old_field WHERE id BETWEEN 1 AND 10000;
   -- 在应用代码中循环执行
   ```

3. **影子表方案**
   ```sql
   -- 1. 创建新表结构
   CREATE TABLE large_table_new LIKE large_table;
   ALTER TABLE large_table_new ADD COLUMN new_field ...;

   -- 2. 逐步复制数据
   INSERT INTO large_table_new SELECT * FROM large_table WHERE ...;

   -- 3. 切换表名（需要停机）
   RENAME TABLE large_table TO large_table_old, large_table_new TO large_table;
   ```

### Q4: 如何在团队中协作使用Flyway？

**A**:

1. **版本号规范**
   - 主干使用 V1.x, V2.x
   - 功能分支使用 V100.x, V200.x（合并时重新编号）

2. **Code Review**
   - 迁移脚本必须经过审查
   - 关注向后兼容性、性能影响

3. **CI/CD验证**
   - GitHub Actions自动运行 `flyway:validate`
   - 自动化测试数据库迁移

4. **沟通机制**
   - 大的schema变更提前通知团队
   - 生产迁移制定详细计划

### Q5: Flyway vs Liquibase，如何选择？

| 特性 | Flyway | Liquibase |
|-----|--------|-----------|
| 学习曲线 | 简单（纯SQL） | 复杂（XML/YAML/JSON） |
| 数据库支持 | 主流数据库 | 更多数据库 |
| 回滚支持 | 需付费版 | 免费支持 |
| 社区 | 活跃 | 活跃 |
| 适用场景 | 中小型项目 | 企业级项目 |

**当前项目选择Flyway原因**:
- 团队熟悉SQL语法
- 不需要复杂的回滚功能（依赖备份恢复）
- 社区版足够使用

### Q6: 如何清理Flyway历史表？

**A**: 不建议清理 `flyway_schema_history` 表，它是迁移记录的唯一来源。

如果必须清理（仅限开发环境）：

```sql
-- 开发环境重置
TRUNCATE TABLE flyway_schema_history;
-- 或
DROP TABLE flyway_schema_history;
```

然后重新执行 `flyway:migrate` 将重建历史表。

**生产环境**: 永远不要清理历史表！

## 相关链接

- [Flyway官方文档](https://documentation.red-gate.com/fd)
- [Flyway命令行工具](https://documentation.red-gate.com/fd/command-line-184127404.html)
- [Flyway配置参数](https://documentation.red-gate.com/fd/parameters-184127474.html)
- [Kubernetes Flyway部署指南](../k8s/FLYWAY-K8S-GUIDE.md)
- [项目CI/CD文档](../README.md#cicd)

## 更新日志

- 2025-01-23: 创建Flyway使用指南
- 2025-01-23: 添加Kubernetes InitContainer集成
- 2025-01-23: 添加CI/CD GitHub Actions集成
