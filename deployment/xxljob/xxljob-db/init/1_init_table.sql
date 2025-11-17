# XXL-Job 初始化 SQL 脚本
# 版本: 2.4.0
# 说明: XXL-Job 调度中心表结构初始化

-- 任务注册表
CREATE TABLE `xxl_job_registry` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `registry_group` VARCHAR(50) NOT NULL,
  `registry_key` VARCHAR(255) NOT NULL,
  `registry_value` VARCHAR(255) NOT NULL,
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_registry_group_key` (`registry_group`,`registry_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务组表
CREATE TABLE `xxl_job_group` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `app_name` VARCHAR(64) NOT NULL,
  `title` VARCHAR(12) NOT NULL,
  `address_type` TINYINT NOT NULL DEFAULT '0',
  `address_list` TEXT,
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appname` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务表
CREATE TABLE `xxl_job_info` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `job_group` INT NOT NULL,
  `job_desc` VARCHAR(255) NOT NULL,
  `add_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `author` VARCHAR(63) DEFAULT NULL,
  `alarm_email` VARCHAR(100) DEFAULT NULL,
  `schedule_type` VARCHAR(50) NOT NULL DEFAULT 'CRON',
  `schedule_conf` VARCHAR(255) DEFAULT NULL,
  `misfire_strategy` VARCHAR(50) NOT NULL DEFAULT 'DO_NOTHING',
  `executor_route_strategy` VARCHAR(50) DEFAULT NULL,
  `executor_handler` VARCHAR(255) DEFAULT NULL,
  `executor_param` TEXT,
  `executor_block_strategy` VARCHAR(50) DEFAULT NULL,
  `executor_fail_strategy` VARCHAR(50) NOT NULL DEFAULT 'FAIL_ALARM',
  `glue_type` VARCHAR(50) NOT NULL,
  `glue_source` MEDIUMTEXT,
  `glue_remark` TEXT,
  `glue_updatetime` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `child_jobid` VARCHAR(255) DEFAULT NULL,
  `trigger_status` TINYINT NOT NULL DEFAULT '0',
  `trigger_last_time` BIGINT DEFAULT '0',
  `trigger_next_time` BIGINT DEFAULT '0',
  `trigger_last_time_millis` BIGINT DEFAULT '0',
  `trigger_next_time_millis` BIGINT DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务日志表
CREATE TABLE `xxl_job_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `job_group` INT NOT NULL,
  `job_id` INT NOT NULL,
  `executor_address` VARCHAR(255) DEFAULT NULL,
  `executor_handler` VARCHAR(255) DEFAULT NULL,
  `executor_param` TEXT,
  `executor_sharding_param` VARCHAR(255) DEFAULT NULL,
  `executor_fail_retry_count` INT NOT NULL DEFAULT '0',
  `trigger_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `trigger_code` INT NOT NULL,
  `trigger_msg` TEXT,
  `handle_time` DATETIME(3) DEFAULT NULL,
  `handle_code` INT NOT NULL,
  `handle_msg` TEXT,
  `alarm_status` TINYINT NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_jobid_trigger_time` (`job_id`,`trigger_time`),
  KEY `idx_trigger_time` (`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务日志报告表
CREATE TABLE `xxl_job_log_report` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `trigger_day` DATE NOT NULL,
  `running_count` INT NOT NULL DEFAULT '0',
  `suc_count` INT NOT NULL DEFAULT '0',
  `fail_count` INT NOT NULL DEFAULT '0',
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trigger_day` (`trigger_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务熔断表
CREATE TABLE `xxl_job_logglue` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `job_id` INT NOT NULL,
  `glue_type` VARCHAR(50) NOT NULL,
  `glue_source` MEDIUMTEXT,
  `glue_remark` TEXT,
  `add_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户表
CREATE TABLE `xxl_job_user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  `permission` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 访问令牌表
CREATE TABLE `xxl_job_token` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(255) NOT NULL,
  `user_id` INT NOT NULL,
  `expire_time` DATETIME(3) NOT NULL,
  `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 调度表
CREATE TABLE `xxl_job_schedule` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `schedule_type` VARCHAR(50) NOT NULL DEFAULT 'CRON',
  `schedule_conf` VARCHAR(255) NOT NULL,
  `job_group` INT NOT NULL,
  `job_id` INT NOT NULL,
  `create_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================
-- 初始化管理员用户
-- ==========================================
INSERT INTO xxl_job_user (`username`, `password`, `role`, `permission`) VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', 'ADMIN', NULL);

-- ==========================================
-- 初始化示例任务组
-- ==========================================
INSERT INTO xxl_job_group (`app_name`, `title`, `address_type`) VALUES ('basebackend-executor', 'BaseBackend 执行器', 0);

-- ==========================================
-- 初始化示例任务
-- ==========================================
INSERT INTO xxl_job_info (`job_group`, `job_desc`, `author`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`, `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_fail_strategy`, `glue_type`, `alarm_email`, `trigger_status`, `trigger_next_time`) VALUES
(1, '用户数据同步任务', 'admin', 'CRON', '0 0 2 * * ?', 'DO_NOTHING', 'FIRST', 'userDataSyncJob', '', 'SERIAL_EXECUTION', 'FAIL_ALARM', 'BEAN', '', 1, 0),
(1, '每日数据统计任务', 'admin', 'CRON', '0 30 3 * * ?', 'DO_NOTHING', 'FIRST', 'dailyStatisticsJob', '', 'SERIAL_EXECUTION', 'FAIL_ALARM', 'BEAN', '', 1, 0),
(1, '缓存预热任务', 'admin', 'CRON', '0 0 1 * * ?', 'DO_NOTHING', 'SHARDING_BROADCAST', 'cacheWarmupJob', '', 'SERIAL_EXECUTION', 'FAIL_ALARM', 'BEAN', '', 1, 0),
(1, '日志清理任务', 'admin', 'CRON', '0 0 4 * * ?', 'DO_NOTHING', 'FIRST', 'logCleanupJob', '', 'DISCARD_LATER', 'FAIL_ALARM', 'BEAN', '', 1, 0);

-- ==========================================
-- 创建索引
-- ==========================================
ALTER TABLE xxl_job_log ADD INDEX IF NOT EXISTS idx_jobid_trigger_time (`job_id`, `trigger_time`);
ALTER TABLE xxl_job_log ADD INDEX IF NOT EXISTS idx_trigger_time (`trigger_time`);
ALTER TABLE xxl_job_log ADD INDEX IF NOT EXISTS idx_trigger_code (`trigger_code`);
ALTER TABLE xxl_job_log_report ADD INDEX IF NOT EXISTS idx_trigger_day (`trigger_day`);

-- ==========================================
-- 创建视图（便于统计）
-- ==========================================
CREATE OR REPLACE VIEW v_job_execution_stats AS
SELECT
  job_group,
  job_id,
  COUNT(*) AS total_executions,
  SUM(CASE WHEN trigger_code = 200 THEN 1 ELSE 0 END) AS success_count,
  SUM(CASE WHEN trigger_code != 200 THEN 1 ELSE 0 END) AS failure_count,
  ROUND(AVG(CASE
    WHEN trigger_time IS NOT NULL AND handle_time IS NOT NULL
    THEN TIMESTAMPDIFF(MICROSECOND, trigger_time, handle_time) / 1000
    ELSE NULL END), 2) AS avg_execution_time_ms
FROM xxl_job_log
GROUP BY job_group, job_id;

-- ==========================================
-- 创建存储过程（清理历史数据）
-- ==========================================
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS `cleanExpiredJobLogs`
(
  IN days_before INT
)
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_job_id INT;
  DECLARE v_count INT;

  DECLARE cursor_old_logs CURSOR FOR
    SELECT DISTINCT job_id
    FROM xxl_job_log
    WHERE trigger_time < DATE_SUB(NOW(), INTERVAL days_before DAY)
      AND handle_code IS NOT NULL;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  START TRANSACTION;

  -- 清理过期日志
  DELETE FROM xxl_job_log
  WHERE trigger_time < DATE_SUB(NOW(), INTERVAL days_before DAY)
    AND handle_code IS NOT NULL;

  -- 清理过期的任务报告
  DELETE FROM xxl_job_log_report
  WHERE trigger_day < DATE_SUB(NOW(), INTERVAL days_before DAY);

  -- 清理过期的 Glue 记录
  DELETE FROM xxl_job_logglue
  WHERE add_time < DATE_SUB(NOW(), INTERVAL days_before DAY);

  COMMIT;

  SELECT CONCAT('清理了 ', ROW_COUNT(), ' 条过期日志') AS result;
END$$

DELIMITER ;

-- 设置自动清理任务（每天凌晨3点执行）
CREATE EVENT IF NOT EXISTS `e_cleanup_expired_job_logs`
ON SCHEDULE EVERY 1 DAY STARTS '2025-01-01 03:00:00'
DO
CALL cleanExpiredJobLogs(30);
