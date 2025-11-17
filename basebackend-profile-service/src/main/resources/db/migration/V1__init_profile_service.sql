-- ===================================================================
-- Phase 10.10 - 用户档案服务数据库初始化脚本
-- ===================================================================
-- 创建时间: 2025-11-14
-- 服务名称: basebackend-profile-service
-- 数据库名: basebackend_profile
-- ===================================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `basebackend_profile`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 使用数据库
USE `basebackend_profile`;

-- ===================================================================
-- 表结构：用户偏好设置表
-- ===================================================================

DROP TABLE IF EXISTS `user_preference`;

CREATE TABLE `user_preference` (
    -- 主键
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- 用户关联
    `user_id` BIGINT NOT NULL COMMENT '用户ID',

    -- ==================== 界面设置 ====================
    `theme` VARCHAR(20) DEFAULT 'light' COMMENT '主题: light-浅色, dark-深色, auto-自动',
    `primary_color` VARCHAR(20) DEFAULT NULL COMMENT '主题色（可选，如 #1890ff）',
    `layout` VARCHAR(20) DEFAULT 'side' COMMENT '布局: side-侧边, top-顶部',
    `menu_collapse` TINYINT DEFAULT 0 COMMENT '菜单收起状态: 0-展开, 1-收起',

    -- ==================== 语言与地区 ====================
    `language` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言: zh-CN-简体中文, en-US-English',
    `timezone` VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '时区（如 Asia/Shanghai, UTC）',
    `date_format` VARCHAR(20) DEFAULT 'YYYY-MM-DD' COMMENT '日期格式',
    `time_format` VARCHAR(20) DEFAULT 'HH:mm:ss' COMMENT '时间格式',

    -- ==================== 通知偏好 ====================
    `email_notification` TINYINT DEFAULT 1 COMMENT '邮件通知: 0-关闭, 1-开启',
    `sms_notification` TINYINT DEFAULT 0 COMMENT '短信通知: 0-关闭, 1-开启',
    `system_notification` TINYINT DEFAULT 1 COMMENT '系统通知: 0-关闭, 1-开启',

    -- ==================== 其他偏好 ====================
    `page_size` INT DEFAULT 10 COMMENT '分页大小（每页显示条数）',
    `dashboard_layout` TEXT DEFAULT NULL COMMENT '仪表板布局配置（JSON格式，可扩展）',
    `auto_save` TINYINT DEFAULT 1 COMMENT '自动保存: 0-关闭, 1-开启',

    -- ==================== 基础字段 ====================
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

-- ===================================================================
-- 默认配置数据（可选）
-- ===================================================================
-- 说明：用户首次访问时，如果没有偏好设置记录，系统会返回代码中定义的默认值
-- 这里不插入默认数据，采用按需创建（UPSERT）的策略

-- ===================================================================
-- 完成提示
-- ===================================================================
-- 数据库初始化完成！
--
-- 表结构说明：
-- 1. user_preference 表用于存储用户的个性化偏好设置
-- 2. 每个用户只有一条记录（通过 uk_user_id 唯一约束保证）
-- 3. 首次访问时返回默认值，更新时采用 UPSERT 逻辑
-- 4. 支持的偏好类型：界面设置、语言地区、通知偏好、其他偏好
--
-- 默认偏好值（在代码中定义）：
-- - theme: light
-- - language: zh-CN
-- - timezone: Asia/Shanghai
-- - email_notification: 1（开启）
-- - sms_notification: 0（关闭）
-- - system_notification: 1（开启）
-- - page_size: 10
-- - auto_save: 1（开启）
-- ===================================================================
