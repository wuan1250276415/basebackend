-- ==================================================
-- 统一备份系统数据库迁移脚本 V3.0
-- 创建时间: 2025-11-26
-- 描述: 创建统一的备份任务、备份历史和恢复记录表
-- ==================================================

-- 备份任务表
CREATE TABLE `backup_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `datasource_type` VARCHAR(32) NOT NULL COMMENT '数据源类型：mysql/postgres/redis',
    `backup_type` VARCHAR(16) NOT NULL COMMENT '备份类型：full/incremental',
    `schedule_cron` VARCHAR(64) NULL COMMENT 'CRON表达式',
    `strategy_json` TEXT NULL COMMENT '策略配置JSON',
    `retention_policy_json` TEXT NULL COMMENT '保留策略JSON',
    `storage_policy_json` TEXT NULL COMMENT '存储策略JSON',
    `notify_policy_json` TEXT NULL COMMENT '通知策略JSON',
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_datasource_type` (`datasource_type`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_schedule` (`schedule_cron`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备份任务表';

-- 备份历史表
CREATE TABLE `backup_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `task_id` BIGINT NOT NULL COMMENT '关联任务ID',
    `task_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `status` VARCHAR(16) NOT NULL COMMENT '备份状态：SUCCESS/FAILED/RUNNING',
    `backup_type` VARCHAR(16) NOT NULL COMMENT '备份类型：full/incremental',
    `base_full_id` BIGINT NULL COMMENT '增量链基线全量备份ID',
    `binlog_start` VARCHAR(128) NULL COMMENT 'MySQL binlog起始位置',
    `binlog_end` VARCHAR(128) NULL COMMENT 'MySQL binlog结束位置',
    `wal_start` VARCHAR(128) NULL COMMENT 'PostgreSQL WAL起始位置',
    `wal_end` VARCHAR(128) NULL COMMENT 'PostgreSQL WAL结束位置',
    `file_size` BIGINT NULL COMMENT '备份文件大小（字节）',
    `storage_locations` TEXT NULL COMMENT '存储位置列表（JSON格式）',
    `checksum_md5` VARCHAR(64) NULL COMMENT 'MD5校验和',
    `checksum_sha256` VARCHAR(128) NULL COMMENT 'SHA256校验和',
    `started_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份开始时间',
    `finished_at` TIMESTAMP NULL COMMENT '备份结束时间',
    `duration_seconds` INT NULL COMMENT '备份耗时（秒）',
    `error_message` TEXT NULL COMMENT '错误信息',
    `started_at_ms` BIGINT NULL COMMENT '备份开始时间戳（毫秒）',
    `finished_at_ms` BIGINT NULL COMMENT '备份结束时间戳（毫秒）',
    PRIMARY KEY (`id`),
    INDEX `idx_task_time` (`task_id`, `started_at`),
    INDEX `idx_status` (`status`),
    INDEX `idx_backup_type` (`backup_type`),
    INDEX `idx_base_full` (`base_full_id`),
    FOREIGN KEY (`task_id`) REFERENCES `backup_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备份历史表';

-- 恢复记录表
CREATE TABLE `restore_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `task_id` BIGINT NOT NULL COMMENT '关联任务ID',
    `history_id` BIGINT NULL COMMENT '关联备份历史ID',
    `target_point` VARCHAR(64) NULL COMMENT 'PITR目标时间点',
    `status` VARCHAR(16) NOT NULL COMMENT '恢复状态：SUCCESS/FAILED/RUNNING',
    `started_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '恢复开始时间',
    `finished_at` TIMESTAMP NULL COMMENT '恢复结束时间',
    `duration_seconds` INT NULL COMMENT '恢复耗时（秒）',
    `error_message` TEXT NULL COMMENT '错误信息',
    `operator` VARCHAR(64) NULL COMMENT '恢复操作者',
    `remark` TEXT NULL COMMENT '恢复备注',
    `started_at_ms` BIGINT NULL COMMENT '恢复开始时间戳（毫秒）',
    `finished_at_ms` BIGINT NULL COMMENT '恢复结束时间戳（毫秒）',
    PRIMARY KEY (`id`),
    INDEX `idx_task_time` (`task_id`, `started_at`),
    INDEX `idx_history` (`history_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_target_point` (`target_point`),
    FOREIGN KEY (`task_id`) REFERENCES `backup_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`history_id`) REFERENCES `backup_history` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='恢复记录表';

-- ==================================================
-- 插入示例数据（可选）
-- ==================================================

-- 示例：MySQL全量备份任务
INSERT INTO `backup_task` (
    `name`, `datasource_type`, `backup_type`, `schedule_cron`,
    `strategy_json`, `retention_policy_json`, `storage_policy_json`, `notify_policy_json`, `enabled`
) VALUES (
    'MySQL全量备份任务',
    'mysql',
    'full',
    '0 0 2 * * ?',
    '{"host":"localhost","port":3306,"username":"root","password":"","database":"test","tables":[],"excludeTables":[]}',
    '{"retentionDays":30,"tier1":7,"tier2":30,"tier3":90}',
    '{"replicas":[{"type":"local","enabled":true,"priority":1},{"type":"s3","enabled":true,"priority":2}]}',
    '{"onFailure":true,"onSuccess":false,"email":"admin@example.com"}',
    TRUE
);

-- 示例：MySQL增量备份任务
INSERT INTO `backup_task` (
    `name`, `datasource_type`, `backup_type`, `schedule_cron`,
    `strategy_json`, `retention_policy_json`, `storage_policy_json`, `notify_policy_json`, `enabled`
) VALUES (
    'MySQL增量备份任务',
    'mysql',
    'incremental',
    '0 0 * * * ?',
    '{"host":"localhost","port":3306,"username":"root","password":"","database":"test","binlogDir":"/var/lib/mysql"}',
    '{"retentionDays":7,"tier1":3,"tier2":7}',
    '{"replicas":[{"type":"local","enabled":true,"priority":1}]}',
    '{"onFailure":true,"email":"admin@example.com"}',
    TRUE
);

-- ==================================================
-- 创建视图（便于查询）
-- ==================================================

-- 备份任务统计视图
CREATE VIEW `v_backup_task_stats` AS
SELECT
    bt.id,
    bt.name,
    bt.datasource_type,
    bt.backup_type,
    bt.enabled,
    COUNT(bh.id) as total_backups,
    COUNT(CASE WHEN bh.status = 'SUCCESS' THEN 1 END) as success_backups,
    COUNT(CASE WHEN bh.status = 'FAILED' THEN 1 END) as failed_backups,
    MAX(bh.started_at) as last_backup_time,
    SUM(CASE WHEN bh.status = 'SUCCESS' THEN bh.file_size ELSE 0 END) as total_size
FROM backup_task bt
LEFT JOIN backup_history bh ON bt.id = bh.task_id
GROUP BY bt.id, bt.name, bt.datasource_type, bt.backup_type, bt.enabled;

-- 最近的备份记录视图
CREATE VIEW `v_recent_backups` AS
SELECT
    bh.*,
    bt.datasource_type,
    bt.name as task_name
FROM backup_history bh
INNER JOIN backup_task bt ON bh.task_id = bt.id
WHERE bh.started_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY bh.started_at DESC;

-- ==================================================
-- 存储过程（可选）
-- ==================================================

DELIMITER $$

-- 清理过期备份的存储过程
CREATE PROCEDURE `sp_cleanup_old_backups`(IN taskId BIGINT, IN retentionDays INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE historyId BIGINT;
    DECLARE cur CURSOR FOR
        SELECT id FROM backup_history
        WHERE task_id = taskId
        AND status = 'SUCCESS'
        AND started_at < DATE_SUB(NOW(), INTERVAL retentionDays DAY)
        ORDER BY started_at ASC;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO historyId;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 这里可以添加清理逻辑，如删除物理文件等
        -- CALL sp_delete_backup_files(historyId);

        -- 删除记录
        DELETE FROM backup_history WHERE id = historyId;
    END LOOP;
    CLOSE cur;
END$$

DELIMITER ;

-- ==================================================
-- 结束
-- ==================================================
