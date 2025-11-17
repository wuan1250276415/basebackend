# Profile Service 数据库初始化指南

## 📋 初始化步骤

### 1. 确保 MySQL 服务运行

```bash
# 检查 MySQL 服务状态
systemctl status mysql
# 或者
service mysql status

# 如果未运行，启动 MySQL
systemctl start mysql
# 或者
service mysql start
```

### 2. 登录 MySQL

```bash
mysql -u root -p
```

### 3. 执行初始化脚本

```bash
# 方式一：直接在 MySQL 中执行
mysql -u root -p < basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql

# 方式二：在 MySQL 命令行中执行
SOURCE /path/to/basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql;
```

### 4. 验证数据库创建

```sql
-- 查看数据库
SHOW DATABASES;
-- 应该看到：basebackend_profile

-- 查看表
USE basebackend_profile;
SHOW TABLES;
-- 应该看到：user_preference

-- 查看表结构
DESCRIBE user_preference;
```

---

## 📊 验证检查清单

- [ ] MySQL 服务正常运行
- [ ] 能够以 root 用户登录
- [ ] 创建了 `basebackend_profile` 数据库
- [ ] 创建了 `user_preference` 表
- [ ] 表结构包含所有字段（15 个字段）
- [ ] 索引创建正确（主键、唯一索引、时间索引）

---

## 🔧 常见问题

### 问题 1: Access denied

**错误信息：**
```
ERROR 1045 (28000): Access denied for user 'root'@'localhost'
```

**解决方案：**
```bash
# 重置 MySQL root 密码
sudo mysql_secure_installation
# 或者
sudo service mysql stop
sudo mysqld_safe --skip-grant-tables &
mysql -u root
UPDATE mysql.user SET authentication_string = PASSWORD('new_password') WHERE User = 'root';
FLUSH PRIVILEGES;
```

### 问题 2: 数据库已存在

**错误信息：**
```
ERROR 1007 (HY000): Can't create database 'basebackend_profile'; database exists
```

**解决方案：**
```sql
-- 删除现有数据库（谨慎操作！）
DROP DATABASE IF EXISTS basebackend_profile;

-- 然后重新创建
SOURCE basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql;
```

### 问题 3: 表已存在

**错误信息：**
```
ERROR 1050 (42S01): Table 'user_preference' already exists
```

**解决方案：**
```sql
-- 删除现有表（谨慎操作！）
DROP TABLE IF EXISTS user_preference;

-- 重新创建表
SOURCE basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql;
```

---

## 🧪 测试数据插入

### 1. 插入测试数据

```sql
USE basebackend_profile;

-- 插入用户偏好设置（用户ID: 1）
INSERT INTO user_preference (
    user_id,
    theme,
    language,
    timezone,
    email_notification,
    system_notification,
    page_size
) VALUES (
    1,
    'light',
    'zh-CN',
    'Asia/Shanghai',
    1,
    1,
    10
);

-- 查看插入的数据
SELECT * FROM user_preference WHERE user_id = 1;
```

### 2. 更新测试数据

```sql
-- 更新偏好设置
UPDATE user_preference
SET theme = 'dark',
    language = 'en-US',
    page_size = 20
WHERE user_id = 1;

-- 验证更新
SELECT * FROM user_preference WHERE user_id = 1;
```

### 3. UPSERT 测试

```sql
-- 使用 REPLACE 实现 UPSERT
REPLACE INTO user_preference (
    user_id,
    theme,
    language,
    timezone,
    email_notification,
    system_notification,
    page_size
) VALUES (
    1,
    'auto',
    'zh-CN',
    'Asia/Shanghai',
    0,
    1,
    15
);
```

---

## 📈 性能优化

### 1. 索引优化

已创建的索引：
- PRIMARY KEY (`id`) - 主键索引
- UNIQUE KEY `uk_user_id` (`user_id`) - 用户ID唯一索引
- KEY `idx_create_time` (`create_time`) - 创建时间索引
- KEY `idx_update_time` (`update_time`) - 更新时间索引

**查询优化示例：**
```sql
-- 根据用户ID查询（使用唯一索引，效率最高）
SELECT * FROM user_preference WHERE user_id = 1;

-- 根据创建时间范围查询（使用时间索引）
SELECT * FROM user_preference WHERE create_time BETWEEN '2025-11-01' AND '2025-11-30';
```

### 2. 分区表（可选）

如果数据量很大，可以考虑按时间分区：

```sql
-- 按月分区示例
ALTER TABLE user_preference
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

---

## 🔐 安全配置

### 1. 创建专用数据库用户

```sql
-- 创建专用用户
CREATE USER 'profile_user'@'%' IDENTIFIED BY 'profile_password_123';

-- 授权
GRANT SELECT, INSERT, UPDATE, DELETE ON basebackend_profile.* TO 'profile_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

### 2. 更新应用配置

在 `application.yml` 中更新数据库配置：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/basebackend_profile?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:profile_user}
    password: ${DB_PASSWORD:profile_password_123}
```

---

## ✅ 验证清单

完成初始化后，请检查以下项目：

- [ ] 数据库 `basebackend_profile` 创建成功
- [ ] 表 `user_preference` 创建成功
- [ ] 所有字段存在（15 个字段）
- [ ] 所有索引创建成功（4 个索引）
- [ ] 能够插入测试数据
- [ ] 能够查询测试数据
- [ ] UPSERT 功能正常
- [ ] 数据库用户权限配置正确
- [ ] 应用配置更新正确

---

## 📞 支持信息

**数据库版本：** MySQL 8.0+
**字符集：** utf8mb4
**排序规则：** utf8mb4_general_ci

**初始化脚本位置：**
```
basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql
```

**表结构文档：**
见初始化脚本中的注释（行 18-93）

---

**完成日期：** _______________
**执行人员：** _______________
**验证结果：** _______________
