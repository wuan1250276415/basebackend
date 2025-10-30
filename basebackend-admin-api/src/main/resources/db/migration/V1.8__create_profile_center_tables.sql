-- ================================================================
-- 个人中心模块数据库表
-- Version: 1.8
-- Author: Claude Code
-- Date: 2025-10-29
-- ================================================================

-- ================================================================
-- 表1: 用户偏好设置表
-- ================================================================
CREATE TABLE IF NOT EXISTS `user_preference` (
    `id` BIGINT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',

    -- 界面设置
    `theme` VARCHAR(20) DEFAULT 'light' COMMENT '主题: light-浅色, dark-深色',
    `primary_color` VARCHAR(20) DEFAULT '#1890ff' COMMENT '主题色',
    `layout` VARCHAR(20) DEFAULT 'side' COMMENT '布局: side-侧边, top-顶部',
    `menu_collapse` TINYINT DEFAULT 0 COMMENT '菜单收起状态: 0-展开, 1-收起',

    -- 语言与地区
    `language` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言: zh-CN-简体中文, en-US-English',
    `timezone` VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `date_format` VARCHAR(20) DEFAULT 'YYYY-MM-DD' COMMENT '日期格式',
    `time_format` VARCHAR(20) DEFAULT 'HH:mm:ss' COMMENT '时间格式',

    -- 通知偏好
    `email_notification` TINYINT DEFAULT 1 COMMENT '邮件通知: 0-关闭, 1-开启',
    `sms_notification` TINYINT DEFAULT 0 COMMENT '短信通知: 0-关闭, 1-开启',
    `system_notification` TINYINT DEFAULT 1 COMMENT '系统通知: 0-关闭, 1-开启',

    -- 其他偏好
    `page_size` INT DEFAULT 20 COMMENT '分页大小',
    `auto_save` TINYINT DEFAULT 1 COMMENT '自动保存: 0-关闭, 1-开启',

    -- 基础字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置表';

-- ================================================================
-- 表2: 在线设备会话表
-- ================================================================
CREATE TABLE IF NOT EXISTS `user_device` (
    `id` BIGINT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(255) NOT NULL UNIQUE COMMENT '会话ID (Redis key)',
    `token` VARCHAR(500) NOT NULL COMMENT 'JWT Token',

    -- 设备信息
    `device_type` VARCHAR(50) COMMENT '设备类型: PC, Mobile, Tablet',
    `os_name` VARCHAR(100) COMMENT '操作系统',
    `os_version` VARCHAR(50) COMMENT '系统版本',
    `browser_name` VARCHAR(100) COMMENT '浏览器名称',
    `browser_version` VARCHAR(50) COMMENT '浏览器版本',
    `user_agent` VARCHAR(500) COMMENT 'User-Agent',
    is_trusted INT comment '是否信任设备',
    -- 网络信息
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `location` VARCHAR(255) COMMENT '登录地点',

    -- 会话状态
    `is_current` TINYINT DEFAULT 0 COMMENT '是否当前设备: 0-否, 1-是',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已注销, 1-在线',
    `last_active_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    first_login_time DATETIME DEFAULT CURRENT_TIMESTAMP ,
    `login_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `logout_time` DATETIME COMMENT '注销时间',
    `expired_time` DATETIME COMMENT '过期时间',

    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_status` (`status`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='在线设备会话表';

-- ================================================================
-- 表3: 用户操作日志表
-- ================================================================
CREATE TABLE IF NOT EXISTS `user_operation_log` (
    `id` BIGINT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `username` VARCHAR(50) COMMENT '用户名',

    -- 操作信息
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型: UPDATE_PROFILE-更新资料, CHANGE_PASSWORD-修改密码, ENABLE_2FA-启用2FA, DISABLE_2FA-禁用2FA, etc',
    `operation_module` VARCHAR(50) NOT NULL COMMENT '操作模块: PROFILE-个人资料, SECURITY-安全设置, PREFERENCE-偏好设置',
    `operation_desc` VARCHAR(500) COMMENT '操作描述',
    `operation_detail` TEXT COMMENT '操作详情 (JSON格式)',

    -- 请求信息
    `request_method` VARCHAR(10) COMMENT '请求方法: GET, POST, PUT, DELETE',
    `request_url` VARCHAR(500) COMMENT '请求URL',
    `request_params` TEXT COMMENT '请求参数 (JSON格式)',

    -- 环境信息
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `location` VARCHAR(255) COMMENT '操作地点',
    `browser` VARCHAR(100) COMMENT '浏览器',
    `os` VARCHAR(100) COMMENT '操作系统',
    `user_agent` VARCHAR(500) COMMENT 'User-Agent',

    -- 结果信息
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-失败, 1-成功',
    `error_msg` VARCHAR(500) COMMENT '错误消息',
    `execution_time` INT COMMENT '执行时长(毫秒)',

    -- 基础字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    KEY `idx_user_id` (`user_id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_operation_module` (`operation_module`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户操作日志表';

-- ================================================================
-- 表4: 双因素认证配置表
-- ================================================================
CREATE TABLE IF NOT EXISTS `user_2fa` (
    `id` BIGINT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',

    -- 2FA配置
    `enabled` TINYINT DEFAULT 0 COMMENT '是否启用: 0-未启用, 1-已启用',
    `auth_type` VARCHAR(20) DEFAULT 'TOTP' COMMENT '认证类型: TOTP-基于时间的一次性密码',
    `secret_key` VARCHAR(255) NOT NULL COMMENT '密钥 (加密存储)',
    `qr_code_url` VARCHAR(500) COMMENT '二维码URL',

    -- 备用码 (用于丢失认证器时恢复)
    `backup_codes` TEXT COMMENT '备用恢复码 (加密存储, 逗号分隔)',
    `backup_codes_used` TEXT COMMENT '已使用的备用码 (逗号分隔)',

    -- 状态信息
    `bind_time` DATETIME COMMENT '绑定时间',
    `last_verify_time` DATETIME COMMENT '最后验证时间',
    `verify_fail_count` INT DEFAULT 0 COMMENT '验证失败次数',
    `locked_until` DATETIME COMMENT '锁定至 (防暴力破解)',

    -- 基础字段
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    KEY `idx_user_id` (`user_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='双因素认证配置表';

-- ================================================================
-- 初始化数据
-- ================================================================

-- 为已存在的用户创建默认偏好设置
INSERT INTO `user_preference` (`id`, `user_id`, `theme`, `language`, `timezone`)
SELECT id, id, 'light', 'zh-CN', 'Asia/Shanghai'
FROM `sys_user`
WHERE NOT EXISTS (
    SELECT 1 FROM `user_preference` WHERE `user_preference`.`user_id` = `sys_user`.`id`
);
