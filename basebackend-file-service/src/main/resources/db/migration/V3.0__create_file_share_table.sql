-- 创建文件分享表
-- Version: 3.0
-- Author: Claude Code (浮浮酱)
-- Date: 2025-11-28
-- Description: 创建文件分享功能相关表

CREATE TABLE IF NOT EXISTS `sys_file_share` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `share_code` VARCHAR(20) NOT NULL COMMENT '分享码（唯一标识）',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` VARCHAR(50) DEFAULT '' COMMENT '文件类型',
  `share_password` VARCHAR(128) DEFAULT NULL COMMENT '分享密码（SHA256加密）',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `download_limit` INT DEFAULT 100 COMMENT '下载限制次数',
  `download_count` INT DEFAULT 0 COMMENT '已下载次数',
  `allow_download` TINYINT(1) DEFAULT 1 COMMENT '是否允许下载',
  `allow_preview` TINYINT(1) DEFAULT 1 COMMENT '是否允许预览',
  `created_by` VARCHAR(50) NOT NULL COMMENT '创建者',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(50) NOT NULL COMMENT '更新者',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0:未删除 1:已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_code` (`share_code`),
  KEY `idx_file_path` (`file_path`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享表';

-- 创建分享访问日志表
CREATE TABLE IF NOT EXISTS `sys_file_share_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `share_code` VARCHAR(20) NOT NULL COMMENT '分享码',
  `access_ip` VARCHAR(50) NOT NULL COMMENT '访问IP',
  `access_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `access_type` VARCHAR(20) NOT NULL COMMENT '访问类型（preview/download）',
  `user_agent` TEXT COMMENT '用户代理',
  PRIMARY KEY (`id`),
  KEY `idx_share_code` (`share_code`),
  KEY `idx_access_time` (`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享访问日志表';

-- 插入初始数据（可选）
-- INSERT INTO `sys_file_share` (...) VALUES (...);
