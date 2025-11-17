-- Seata 分布式事务数据库初始化脚本
-- 版本: Seata 2.0.0
-- 日期: 2025-11-14

-- ============================================
-- 全局事务表 (global_table)
-- ============================================
CREATE TABLE IF NOT EXISTS `global_table` (
  `xid` VARCHAR(128) NOT NULL,
  `transaction_id` BIGINT,
  `status` TINYINT NOT NULL,
  `application_id` VARCHAR(64),
  `transaction_service_group` VARCHAR(64),
  `transaction_name` VARCHAR(64),
  `timeout` INT,
  `begin_time` BIGINT,
  `application_data` VARCHAR(2000),
  `gmt_create` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`xid`),
  KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 分支事务表 (branch_table)
-- ============================================
CREATE TABLE IF NOT EXISTS `branch_table` (
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(128) NOT NULL,
  `transaction_id` BIGINT,
  `resource_group_id` VARCHAR(32),
  `resource_id` VARCHAR(256),
  `lock_key` VARCHAR(128),
  `lock_type` VARCHAR(16),
  `status` TINYINT NOT NULL,
  `client_id` VARCHAR(64),
  `application_data` VARCHAR(2000),
  `gmt_create` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 分布式锁表 (lock_table)
-- ============================================
CREATE TABLE IF NOT EXISTS `lock_table` (
  `row_key` VARCHAR(128) NOT NULL,
  `xid` VARCHAR(128),
  `transaction_id` BIGINT,
  `branch_id` BIGINT,
  `resource_id` VARCHAR(256),
  `table_name` VARCHAR(32),
  `pk` VARCHAR(36),
  `status` TINYINT NOT NULL DEFAULT '0',
  `gmt_create` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`row_key`),
  KEY `idx_status` (`status`),
  KEY `idx_xid_and_branch_id` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 分布式会话锁表 (distributed_lock)
-- ============================================
CREATE TABLE IF NOT EXISTS `distributed_lock` (
  `lock_key` VARCHAR(128) NOT NULL,
  `locked` TINYINT NOT NULL DEFAULT 0,
  `locker` VARCHAR(64),
  `gmt_create` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`lock_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 初始化数据
-- ============================================
-- 插入分布式锁初始数据
INSERT INTO distributed_lock (lock_key, locked) VALUES ('seata_flush_lock', 0) ON DUPLICATE KEY UPDATE locked=locked;

-- ============================================
-- 创建索引 (提升查询性能)
-- ============================================
-- 全局事务表索引
ALTER TABLE `global_table` ADD INDEX IF NOT EXISTS `idx_gmt_modified_status` (`gmt_modified`, `status`);
ALTER TABLE `global_table` ADD INDEX IF NOT EXISTS `idx_transaction_id` (`transaction_id`);
ALTER TABLE `global_table` ADD INDEX IF NOT EXISTS `idx_transaction_service_group` (`transaction_service_group`);

-- 分支事务表索引
ALTER TABLE `branch_table` ADD INDEX IF NOT EXISTS `idx_xid` (`xid`);
ALTER TABLE `branch_table` ADD INDEX IF NOT EXISTS `idx_xid_and_branch_id` (`xid`, `branch_id`);

-- 锁表索引
ALTER TABLE `lock_table` ADD INDEX IF NOT EXISTS `idx_status` (`status`);
ALTER TABLE `lock_table` ADD INDEX IF NOT EXISTS `idx_xid_and_branch_id` (`xid`, `branch_id`);
ALTER TABLE `lock_table` ADD INDEX IF NOT EXISTS `idx_lock_key` (`lock_key`);

-- ============================================
-- Nacos 配置表 (在 Seata 数据库中)
-- ============================================
CREATE TABLE IF NOT EXISTS `config_info` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `tenant_id` VARCHAR(128) DEFAULT '',
  `group_id` VARCHAR(32) NOT NULL,
  `data_id` VARCHAR(255) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `md5` VARCHAR(32) DEFAULT '',
  `gmt_create` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `src_user` TEXT,
  `src_ip` VARCHAR(50) DEFAULT '',
  `app_name` VARCHAR(128) DEFAULT '',
  `c_desc` VARCHAR(256) DEFAULT '',
  `c_use` VARCHAR(64) DEFAULT '',
  `effect` VARCHAR(64) DEFAULT '',
  `c_schema` TEXT,
  `type` VARCHAR(64) DEFAULT 'text',
  `encrypted_data_key` TEXT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `config_info_aggr` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `tenant_id` VARCHAR(128) DEFAULT '',
  `group_id` VARCHAR(32) NOT NULL,
  `data_id` VARCHAR(255) NOT NULL,
  `datum_id` VARCHAR(255) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `gmt_modified` DATETIME(3) NOT NULL,
  `gmt_create` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfoaggr_datumid` (`data_id`,`group_id`,`tenant_id`,`datum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `config_info_beta` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `tenant_id` VARCHAR(128) DEFAULT '',
  `group_id` VARCHAR(32) NOT NULL,
  `data_id` VARCHAR(255) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `beta_ips` VARCHAR(1024) DEFAULT NULL,
  `md5` VARCHAR(32) DEFAULT '',
  `gmt_create` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfobeta_dagroupid` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `config_info_tag` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `tenant_id` VARCHAR(128) DEFAULT '',
  `group_id` VARCHAR(32) NOT NULL,
  `data_id` VARCHAR(255) NOT NULL,
  `tag_id` VARCHAR(128) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `md5` VARCHAR(32) DEFAULT '',
  `gmt_create` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `gmt_modified` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_configinfotag_dagrouptag` (`data_id`,`group_id`,`tenant_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `his_config_info` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `nid` BIGINT,
  `tenant_id` VARCHAR(128) DEFAULT '',
  `group_id` VARCHAR(32) NOT NULL,
  `data_id` VARCHAR(255) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `md5` VARCHAR(32) DEFAULT '',
  `gmt_create` DATETIME(3) NOT NULL,
  `gmt_modified` DATETIME(3) NOT NULL,
  `src_user` TEXT,
  `src_ip` VARCHAR(50) DEFAULT '',
  `op_type` VARCHAR(64) DEFAULT '',
  `operation_name` VARCHAR(128) DEFAULT '',
  `app_name` VARCHAR(128) DEFAULT '',
  `c_desc` VARCHAR(256) DEFAULT '',
  `c_use` VARCHAR(64) DEFAULT '',
  `effect` VARCHAR(64) DEFAULT '',
  `c_schema` TEXT,
  `type` VARCHAR(64) DEFAULT 'text',
  `encrypted_data_key` TEXT,
  PRIMARY KEY (`id`),
  KEY `idx_gmt_create` (`gmt_create`),
  KEY `idx_gmt_modified` (`gmt_modified`),
  KEY `idx_did` (`data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tenant_capacity` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `tenant_id` VARCHAR(128) DEFAULT '',
  `quota` INT NOT NULL DEFAULT '0',
  `usage` INT NOT NULL DEFAULT '0',
  `max_size` INT NOT NULL DEFAULT '0',
  `max_aggr_count` INT NOT NULL DEFAULT '0',
  `max_aggr_size` INT NOT NULL DEFAULT '0',
  `max_history_count` INT NOT NULL DEFAULT '0',
  `gmt_create` DATETIME(3) NOT NULL,
  `gmt_modified` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tenant_info` (
  `id` BIGINT AUTO_INCREMENT NOT NULL,
  `kp` VARCHAR(128) NOT NULL,
  `tenant_id` VARCHAR(128) DEFAULT '',
  `tenant_name` VARCHAR(64) DEFAULT '',
  `tenant_desc` VARCHAR(256) DEFAULT NULL,
  `create_time` DATETIME(3) NOT NULL,
  `create_source` VARCHAR(32) DEFAULT NULL,
  `impower` VARCHAR(256) DEFAULT NULL,
  `gmt_create` DATETIME(3) NOT NULL,
  `gmt_modified` DATETIME(3) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`),
  KEY `idx_tenant_name` (`tenant_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `users` (
  `username` VARCHAR(50) NOT NULL PRIMARY KEY,
  `password` VARCHAR(500) NOT NULL,
  `enabled` TINYINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `roles` (
  `role_name` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `permissions` (
  `role_name` VARCHAR(50) NOT NULL,
  `resource` VARCHAR(255) NOT NULL,
  `action` VARCHAR(8) NOT NULL,
  PRIMARY KEY (`role_name`,`resource`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `roles_permissions` (
  `role_name` VARCHAR(50) NOT NULL,
  `permission` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`role_name`,`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `users_roles` (
  `username` VARCHAR(50) NOT NULL,
  `role_name` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`username`,`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化 Nacos 用户
INSERT INTO users (username, password, enabled) VALUES ('nacos', '$2a$10$EuWPZ3cS2O6ouakp6uOaEO5Y9A3wQ3Q5T5G5X5X5X5X5X5X5X5X5X', 1) ON DUPLICATE KEY UPDATE enabled=1;

-- 初始化角色
INSERT INTO roles (role_name) VALUES ('ROLE_ADMIN') ON DUPLICATE KEY UPDATE role_name=ROLE_ADMIN;

-- 初始化权限
INSERT INTO permissions (role_name, resource, action) VALUES ('ROLE_ADMIN', '*', 'rw') ON DUPLICATE KEY UPDATE role_name=ROLE_ADMIN;

-- 关联用户角色
INSERT INTO users_roles (username, role_name) VALUES ('nacos', 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE username=nacos;

-- ============================================
-- 创建视图 (便于监控)
-- ============================================
-- 事务统计视图
CREATE OR REPLACE VIEW v_global_transaction_stats AS
SELECT
  transaction_service_group,
  status,
  COUNT(*) AS transaction_count,
  AVG(TIMESTAMPDIFF(SECOND, gmt_create, gmt_modified)) AS avg_duration_seconds,
  MIN(gmt_create) AS first_transaction_time,
  MAX(gmt_create) AS last_transaction_time
FROM global_table
GROUP BY transaction_service_group, status;

-- 分支事务统计视图
CREATE OR REPLACE VIEW v_branch_transaction_stats AS
SELECT
  xid,
  COUNT(*) AS branch_count,
  MAX(gmt_modified) AS last_update_time
FROM branch_table
GROUP BY xid;

-- ============================================
-- 存储过程 (清理过期数据)
-- ============================================
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS `clean_expired_global_transactions`
(
  IN days_before INT
)
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_xid VARCHAR(128);

  DECLARE cursor_expired CURSOR FOR
    SELECT DISTINCT xid
    FROM global_table
    WHERE gmt_create < DATE_SUB(NOW(), INTERVAL days_before DAY)
      AND status IN (2, 4, 5); -- 结束的事务状态

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  START TRANSACTION;

  -- 清理全局事务表
  DELETE FROM global_table
  WHERE gmt_create < DATE_SUB(NOW(), INTERVAL days_before DAY)
    AND status IN (2, 4, 5);

  -- 清理分支事务表
  DELETE FROM branch_table
  WHERE xid NOT IN (SELECT xid FROM global_table);

  -- 清理锁表
  DELETE FROM lock_table
  WHERE xid NOT IN (SELECT xid FROM global_table);

  COMMIT;

  SELECT CONCAT('Cleaned expired transactions older than ', days_before, ' days') AS result;
END$$

DELIMITER ;

-- 设置清理任务的存储过程（每天凌晨2点执行）
CREATE EVENT IF NOT EXISTS `e_cleanup_expired_transactions`
ON SCHEDULE EVERY 1 DAY STARTS '2025-01-01 02:00:00'
DO
CALL clean_expired_global_transactions(30);
