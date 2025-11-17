# Profile Service 数据库手动初始化操作手册

## 📋 准备工作

### 1. 确认 MySQL 服务运行

在终端或命令提示符中执行：

```bash
# Windows
net start mysql

# Linux/Mac
sudo systemctl start mysql
# 或
sudo service mysql start
```

### 2. 验证 MySQL 连接

```bash
mysql -u root -p
```

输入密码后，如果能进入 MySQL 命令行，则连接成功。

---

## 🚀 快速初始化（推荐）

### 方式一：使用自动化脚本

```bash
# 给脚本添加执行权限
chmod +x scripts/init-profile-database.sh

# 运行脚本
bash scripts/init-profile-database.sh
```

脚本会自动：
1. 检查 MySQL 连接
2. 创建数据库
3. 创建表
4. 验证结果
5. 可选择插入测试数据

---

### 方式二：手动执行 SQL

#### 步骤 1: 创建数据库和表

```bash
# 直接执行 SQL 文件
mysql -u root -p < basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql
```

#### 步骤 2: 验证创建结果

```sql
-- 登录 MySQL
mysql -u root -p

-- 查看数据库
SHOW DATABASES;
-- 应该看到：basebackend_profile

-- 使用数据库
USE basebackend_profile;

-- 查看表
SHOW TABLES;
-- 应该看到：user_preference

-- 查看表结构
DESCRIBE user_preference;

-- 查看索引
SHOW INDEX FROM user_preference;
```

---

## 📊 详细操作步骤

### 第一步：登录 MySQL

```bash
mysql -u root -p
```

输入密码后，进入 MySQL 命令行。

### 第二步：执行初始化 SQL

在 MySQL 命令行中执行：

```sql
SOURCE /path/to/basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql;
```

**或直接执行 SQL 命令：**

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_profile`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 使用数据库
USE `basebackend_profile`;

-- 创建表
DROP TABLE IF EXISTS `user_preference`;

CREATE TABLE `user_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',

    -- 界面设置
    `theme` VARCHAR(20) DEFAULT 'light' COMMENT '主题: light-浅色, dark-深色, auto-自动',
    `primary_color` VARCHAR(20) DEFAULT NULL COMMENT '主题色（可选，如 #1890ff）',
    `layout` VARCHAR(20) DEFAULT 'side' COMMENT '布局: side-侧边, top-顶部',
    `menu_collapse` TINYINT DEFAULT 0 COMMENT '菜单收起状态: 0-展开, 1-收起',

    -- 语言与地区
    `language` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言: zh-CN-简体中文, en-US-English',
    `timezone` VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '时区（如 Asia/Shanghai, UTC）',
    `date_format` VARCHAR(20) DEFAULT 'YYYY-MM-DD' COMMENT '日期格式',
    `time_format` VARCHAR(20) DEFAULT 'HH:mm:ss' COMMENT '时间格式',

    -- 通知偏好
    `email_notification` TINYINT DEFAULT 1 COMMENT '邮件通知: 0-关闭, 1-开启',
    `sms_notification` TINYINT DEFAULT 0 COMMENT '短信通知: 0-关闭, 1-开启',
    `system_notification` TINYINT DEFAULT 1 COMMENT '系统通知: 0-关闭, 1-开启',

    -- 其他偏好
    `page_size` INT DEFAULT 10 COMMENT '分页大小（每页显示条数）',
    `dashboard_layout` TEXT DEFAULT NULL COMMENT '仪表板布局配置（JSON格式，可扩展）',
    `auto_save` TINYINT DEFAULT 1 COMMENT '自动保存: 0-关闭, 1-开启',

    -- 基础字段
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 主键和索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`) COMMENT '用户ID唯一索引',
    KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引',
    KEY `idx_update_time` (`update_time`) COMMENT '更新时间索引'
) ENGINE=InnoDB
  AUTO_INCREMENT=1000
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='用户偏好设置表 - 存储用户个性化配置';
```

### 第三步：验证创建结果

```sql
-- 查看数据库列表
SHOW DATABASES;

-- 切换到目标数据库
USE basebackend_profile;

-- 查看表列表
SHOW TABLES;

-- 查看表结构
DESCRIBE user_preference;

-- 查看索引
SHOW INDEX FROM user_preference;
```

**预期结果：**
```
+--------------------+
| Tables_in_basebackend_profile |
+--------------------+
| user_preference    |
+--------------------+

+------------------+--------------+------+-----+-------------------+-------------------+
| Field            | Type         | Null | Key | Default           | Extra             |
+------------------+--------------+------+-----+-------------------+-------------------+
| id               | bigint       | NO   | PRI | NULL              | auto_increment    |
| user_id          | bigint       | NO   | UNI | NULL              |                   |
| theme            | varchar(20)  | YES  |     | light             |                   |
| primary_color    | varchar(20)  | YES  |     | NULL              |                   |
| layout           | varchar(20)  | YES  |     | side              |                   |
| menu_collapse    | tinyint      | YES  |     | 0                 |                   |
| language         | varchar(10)  | YES  |     | zh-CN             |                   |
| timezone         | varchar(50)  | YES  |     | Asia/Shanghai     |                   |
| date_format      | varchar(20)  | YES  |     | YYYY-MM-DD        |                   |
| time_format      | varchar(20)  | YES  |     | HH:mm:ss          |                   |
| email_notification | tinyint    | YES  |     | 1                 |                   |
| sms_notification | tinyint      | YES  |     | 0                 |                   |
| system_notification | tinyint   | YES  |     | 1                 |                   |
| page_size        | int          | YES  |     | 10                |                   |
| dashboard_layout | text         | YES  |     | NULL              |                   |
| auto_save        | tinyint      | YES  |     | 1                 |                   |
| create_time      | datetime     | NO   |     | CURRENT_TIMESTAMP |                   |
| update_time      | datetime     | NO   |     | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
+------------------+--------------+------+-----+-------------------+-------------------+
```

### 第四步：插入测试数据（可选）

```sql
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

**预期结果：**
```
+----+---------+-------+--------------+--------+---------------+-----------+---------------+------------+-------------+------------+-------------+------------------+--------------+-------------------+-----------+------------------+---------------------+---------------------+
| id | user_id | theme | primary_color | layout | menu_collapse | language  | timezone       | date_format| time_format | email_notification | sms_notification | system_notification | page_size | dashboard_layout | auto_save | create_time      | update_time      |
+----+---------+-------+--------------+--------+---------------+-----------+---------------+------------+-------------+------------------+------------------+-------------------+-----------+------------------+-----------+------------------+------------------+
|1000|       1 | light | NULL         | side   |             0 | zh-CN     | Asia/Shanghai | YYYY-MM-DD | HH:mm:ss    |                1 |                0 |                 1 |        10 | NULL             |         1 | 2025-11-14 10:30:00 | 2025-11-14 10:30:00 |
+----+---------+-------+--------------+--------+---------------+-----------+---------------+------------+-------------+------------------+------------------+-------------------+-----------+------------------+-----------+------------------+------------------+
```

### 第五步：测试 UPSERT 操作

```sql
-- 使用 REPLACE 实现 UPSERT（存在则更新，不存在则插入）
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
    'dark',
    'en-US',
    'UTC',
    0,
    1,
    20
);

-- 查看更新后的数据
SELECT * FROM user_preference WHERE user_id = 1;
```

---

## 🔧 创建专用数据库用户（可选但推荐）

### 步骤 1: 创建用户

```sql
-- 创建专用用户
CREATE USER 'profile_user'@'%' IDENTIFIED BY 'profile_password_123';

-- 授权
GRANT SELECT, INSERT, UPDATE, DELETE ON basebackend_profile.* TO 'profile_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

### 步骤 2: 验证用户权限

```sql
-- 查看用户权限
SHOW GRANTS FOR 'profile_user'@'%';
```

### 步骤 3: 使用新用户连接

```bash
mysql -u profile_user -p profile_password_123 basebackend_profile
```

---

## 🔐 更新应用配置

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

完成初始化后，请确认以下项目：

- [ ] MySQL 服务正常运行
- [ ] 能够以 root 用户登录
- [ ] 创建了 `basebackend_profile` 数据库
- [ ] 创建了 `user_preference` 表
- [ ] 表结构包含所有字段（17 个字段）
- [ ] 创建了所有索引（4 个索引：主键、唯一索引、时间索引）
- [ ] 能够插入测试数据
- [ ] 能够查询测试数据
- [ ] UPSERT 功能正常（使用 REPLACE）
- [ ] 可选：创建了专用数据库用户
- [ ] 可选：应用配置已更新

---

## ❌ 常见问题及解决方案

### 问题 1: 找不到 MySQL 命令

**现象：** `'mysql' 不是内部或外部命令`

**解决方案：**
- Windows：将 MySQL 安装目录的 `bin` 文件夹添加到 PATH 环境变量
- Linux/Mac：安装 MySQL 或使用包管理器安装

### 问题 2: Access denied 错误

**现象：**
```
ERROR 1045 (28000): Access denied for user 'root'@'localhost'
```

**解决方案：**
```bash
# Windows（MySQL Installer）
mysql_secure_installation

# Linux/Mac
sudo mysql_secure_installation

# 或重置密码
sudo mysql
ALTER USER 'root'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

### 问题 3: 数据库已存在

**现象：**
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

### 问题 4: 表已存在

**现象：**
```
ERROR 1050 (42S01): Table 'user_preference' already exists
```

**解决方案：**
```sql
USE basebackend_profile;

-- 删除现有表（谨慎操作！）
DROP TABLE IF EXISTS user_preference;

-- 重新创建表
SOURCE basebackend-profile-service/src/main/resources/db/migration/V1__init_profile_service.sql;
```

### 问题 5: 字符集问题

**现象：** 中文显示乱码

**解决方案：**
```sql
-- 检查字符集
SHOW VARIABLES LIKE 'character%';
SHOW VARIABLES LIKE 'collation%';

-- 设置字符集
SET NAMES utf8mb4;
```

确保数据库和表的字符集都是 `utf8mb4`。

---

## 📈 性能优化建议

### 1. 索引优化

当前已创建的索引：
- PRIMARY KEY (`id`) - 主键索引
- UNIQUE KEY `uk_user_id` (`user_id`) - 用户ID唯一索引
- KEY `idx_create_time` (`create_time`) - 创建时间索引
- KEY `idx_update_time` (`update_time`) - 更新时间索引

这些索引已经足够满足基本查询需求。

### 2. 分区表（可选）

如果数据量很大（>100万条记录），可以考虑按时间分区：

```sql
-- 按月分区
ALTER TABLE user_preference
PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

### 3. 连接池配置

在 `application.yml` 中配置 Druid 连接池：

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
```

---

## 📞 需要帮助？

如果遇到问题，请参考：
1. MySQL 官方文档：https://dev.mysql.com/doc/
2. 项目文档：PROFILE_SERVICE_DB_INIT_GUIDE.md
3. 初始化脚本日志

---

**操作完成日期：** _______________
**执行人员：** _______________
**验证结果：** _______________

---

## 🎯 下一步行动

数据库初始化完成后：

1. **启动 profile-service**
   ```bash
   cd basebackend-profile-service
   mvn spring-boot:run
   ```

2. **运行集成测试**
   ```bash
   python scripts/integration_test.py
   ```

3. **验证 API 调用**
   ```bash
   curl http://localhost:8180/api/profile/preference
   ```

4. **进行性能测试**

---

**加油喵～ ฅ'ω'ฅ**
