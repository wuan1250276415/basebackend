-- V1.6: 创建存储管理相关表（备份记录、文件信息）
-- Author: BaseBackend
-- Date: 2025-10-21

-- ========================================
-- 备份记录表
-- ========================================
CREATE TABLE IF NOT EXISTS `sys_backup_record` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '备份ID',
    `backup_code` VARCHAR(64) NOT NULL COMMENT '备份编号',
    `backup_type` VARCHAR(20) NOT NULL COMMENT '备份类型(full/incremental)',
    `status` VARCHAR(20) NOT NULL COMMENT '备份状态(running/success/failed)',
    `database_name` VARCHAR(64) NOT NULL COMMENT '数据库名称',
    `file_path` VARCHAR(512) DEFAULT NULL COMMENT '备份文件路径',
    `file_size` BIGINT(20) DEFAULT NULL COMMENT '备份文件大小(字节)',
    `binlog_file` VARCHAR(128) DEFAULT NULL COMMENT 'Binlog文件名',
    `binlog_position` BIGINT(20) DEFAULT NULL COMMENT 'Binlog位置',
    `start_time` DATETIME NOT NULL COMMENT '备份开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '备份结束时间',
    `duration` BIGINT(20) DEFAULT NULL COMMENT '耗时(秒)',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_backup_code` (`backup_code`),
    KEY `idx_backup_type` (`backup_type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备份记录表';

-- ========================================
-- 文件信息表
-- ========================================
CREATE TABLE IF NOT EXISTS `sys_file_info` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `file_code` VARCHAR(64) NOT NULL COMMENT '文件编号',
    `original_filename` VARCHAR(256) NOT NULL COMMENT '原始文件名',
    `stored_filename` VARCHAR(256) NOT NULL COMMENT '存储文件名',
    `file_path` VARCHAR(512) NOT NULL COMMENT '文件路径',
    `file_url` VARCHAR(1024) DEFAULT NULL COMMENT '文件URL',
    `file_size` BIGINT(20) NOT NULL COMMENT '文件大小(字节)',
    `content_type` VARCHAR(128) DEFAULT NULL COMMENT '文件类型(MIME)',
    `file_category` VARCHAR(32) DEFAULT 'file' COMMENT '文件分类(file/image/large)',
    `thumbnail_url` VARCHAR(1024) DEFAULT NULL COMMENT '缩略图URL',
    `bucket_name` VARCHAR(64) NOT NULL COMMENT '存储桶名称',
    `upload_time` DATETIME NOT NULL COMMENT '上传时间',
    `upload_user_id` BIGINT(20) DEFAULT NULL COMMENT '上传人ID',
    `upload_username` VARCHAR(64) DEFAULT NULL COMMENT '上传人姓名',
    `etag` VARCHAR(128) DEFAULT NULL COMMENT 'ETag',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记(0-未删除,1-已删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_code` (`file_code`),
    KEY `idx_file_category` (`file_category`),
    KEY `idx_upload_user_id` (`upload_user_id`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';
