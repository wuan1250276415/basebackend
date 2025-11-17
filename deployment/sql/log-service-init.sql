-- ========================================
-- BaseBackend Log Service 数据库初始化脚本
-- ========================================
-- 功能：创建日志服务独立数据库
-- 包含：登录日志表、操作日志表及示例数据
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_log`
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_log`;

-- ========================================
-- sys_login_log - 登录日志表
-- ========================================
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
    `id` BIGINT(20) NOT NULL COMMENT '主键ID',
    `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `login_location` VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器类型',
    `os` VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
    `status` TINYINT(1) DEFAULT 1 COMMENT '登录状态：0-失败，1-成功',
    `msg` VARCHAR(255) DEFAULT NULL COMMENT '提示消息',
    `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_ip_address` (`ip_address`),
    KEY `idx_status` (`status`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ========================================
-- sys_operation_log - 操作日志表
-- ========================================
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id` BIGINT(20) NOT NULL COMMENT '主键ID',
    `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作',
    `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    `params` TEXT DEFAULT NULL COMMENT '请求参数',
    `time` BIGINT(20) DEFAULT NULL COMMENT '执行时长(毫秒)',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '操作地点',
    `status` TINYINT(1) DEFAULT 1 COMMENT '操作状态：0-失败，1-成功',
    `error_msg` TEXT DEFAULT NULL COMMENT '错误消息',
    `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_operation` (`operation`),
    KEY `idx_status` (`status`),
    KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ========================================
-- 初始化示例数据 - 登录日志
-- ========================================

-- 插入成功登录记录
INSERT INTO `sys_login_log` (`id`, `user_id`, `username`, `ip_address`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES
(1, 1, 'admin', '127.0.0.1', '本地', 'Chrome 120.0', 'Windows 10', 1, '登录成功', '2025-01-13 09:00:00'),
(2, 2, 'user01', '192.168.1.100', '上海', 'Firefox 121.0', 'Windows 11', 1, '登录成功', '2025-01-13 09:15:00'),
(3, 3, 'user02', '192.168.1.101', '北京', 'Chrome 120.0', 'macOS 14.0', 1, '登录成功', '2025-01-13 09:30:00'),
(4, 1, 'admin', '127.0.0.1', '本地', 'Chrome 120.0', 'Windows 10', 1, '登录成功', '2025-01-13 14:00:00'),
(5, 4, 'user03', '192.168.1.102', '深圳', 'Safari 17.0', 'macOS 14.0', 1, '登录成功', '2025-01-13 14:30:00');

-- 插入失败登录记录
INSERT INTO `sys_login_log` (`id`, `user_id`, `username`, `ip_address`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES
(6, NULL, 'hacker', '203.0.113.1', '未知', 'Unknown', 'Unknown', 0, '用户名或密码错误', '2025-01-13 10:00:00'),
(7, NULL, 'admin', '203.0.113.2', '未知', 'Chrome 120.0', 'Windows 10', 0, '用户名或密码错误', '2025-01-13 10:05:00'),
(8, NULL, 'test', '203.0.113.3', '未知', 'Firefox 121.0', 'Linux', 0, '账号已被锁定', '2025-01-13 11:00:00');

-- ========================================
-- 初始化示例数据 - 操作日志
-- ========================================

-- 插入操作成功记录
INSERT INTO `sys_operation_log` (`id`, `user_id`, `username`, `operation`, `method`, `params`, `time`, `ip_address`, `location`, `status`, `error_msg`, `operation_time`) VALUES
(1, 1, 'admin', '创建用户', 'POST /api/users', '{"username":"newuser","email":"newuser@example.com"}', 150, '127.0.0.1', '本地', 1, NULL, '2025-01-13 09:10:00'),
(2, 1, 'admin', '更新用户', 'PUT /api/users/10', '{"username":"updateduser","status":1}', 120, '127.0.0.1', '本地', 1, NULL, '2025-01-13 09:20:00'),
(3, 2, 'user01', '查询部门列表', 'GET /api/depts', '{}', 80, '192.168.1.100', '上海', 1, NULL, '2025-01-13 09:25:00'),
(4, 1, 'admin', '删除角色', 'DELETE /api/roles/5', '{}', 100, '127.0.0.1', '本地', 1, NULL, '2025-01-13 10:00:00'),
(5, 3, 'user02', '导出用户数据', 'GET /api/users/export', '{"format":"excel"}', 2500, '192.168.1.101', '北京', 1, NULL, '2025-01-13 10:30:00'),
(6, 1, 'admin', '创建角色', 'POST /api/roles', '{"roleName":"审计员","roleKey":"auditor"}', 180, '127.0.0.1', '本地', 1, NULL, '2025-01-13 11:00:00'),
(7, 4, 'user03', '更新个人信息', 'PUT /api/users/profile', '{"email":"newemail@example.com"}', 95, '192.168.1.102', '深圳', 1, NULL, '2025-01-13 14:35:00');

-- 插入操作失败记录
INSERT INTO `sys_operation_log` (`id`, `user_id`, `username`, `operation`, `method`, `params`, `time`, `ip_address`, `location`, `status`, `error_msg`, `operation_time`) VALUES
(8, 2, 'user01', '删除用户', 'DELETE /api/users/1', '{}', 50, '192.168.1.100', '上海', 0, '权限不足：无法删除管理员账号', '2025-01-13 10:15:00'),
(9, 3, 'user02', '修改系统配置', 'PUT /api/config/system', '{"key":"value"}', 30, '192.168.1.101', '北京', 0, '权限不足：需要管理员权限', '2025-01-13 11:30:00'),
(10, 1, 'admin', '批量导入用户', 'POST /api/users/import', '{"file":"users.csv"}', 5000, '127.0.0.1', '本地', 0, '数据格式错误：第10行缺少必填字段', '2025-01-13 15:00:00');

-- ========================================
-- 数据统计
-- ========================================

-- 完成
SELECT '✓ Log Service 数据库初始化完成' AS 'Status';
SELECT '✓ 已创建 2 张表：sys_login_log, sys_operation_log' AS 'Tables';
SELECT '✓ 已初始化登录日志：8 条记录（5 条成功，3 条失败）' AS 'LoginLog';
SELECT '✓ 已初始化操作日志：10 条记录（7 条成功，3 条失败）' AS 'OperationLog';
SELECT CONCAT('✓ 登录日志总数：', COUNT(*), ' 条') AS 'LoginLogCount' FROM sys_login_log;
SELECT CONCAT('✓ 操作日志总数：', COUNT(*), ' 条') AS 'OperationLogCount' FROM sys_operation_log;

-- 日志统计概览
SELECT '━━━━━━━━━━ 登录日志统计 ━━━━━━━━━━' AS '';
SELECT
    CASE status
        WHEN 1 THEN '✓ 成功登录'
        WHEN 0 THEN '✗ 失败登录'
    END AS '状态',
    COUNT(*) AS '数量'
FROM sys_login_log
GROUP BY status
ORDER BY status DESC;

SELECT '━━━━━━━━━━ 操作日志统计 ━━━━━━━━━━' AS '';
SELECT
    CASE status
        WHEN 1 THEN '✓ 成功操作'
        WHEN 0 THEN '✗ 失败操作'
    END AS '状态',
    COUNT(*) AS '数量'
FROM sys_operation_log
GROUP BY status
ORDER BY status DESC;

-- 最近登录记录
SELECT '━━━━━━━━━━ 最近登录记录（Top 5）━━━━━━━━━━' AS '';
SELECT
    username AS '用户名',
    ip_address AS 'IP地址',
    login_location AS '登录地点',
    CASE status WHEN 1 THEN '✓ 成功' ELSE '✗ 失败' END AS '状态',
    DATE_FORMAT(login_time, '%Y-%m-%d %H:%i:%s') AS '登录时间'
FROM sys_login_log
ORDER BY login_time DESC
LIMIT 5;

-- 最近操作记录
SELECT '━━━━━━━━━━ 最近操作记录（Top 5）━━━━━━━━━━' AS '';
SELECT
    username AS '用户名',
    operation AS '操作',
    method AS '请求方法',
    CASE status WHEN 1 THEN '✓ 成功' ELSE '✗ 失败' END AS '状态',
    DATE_FORMAT(operation_time, '%Y-%m-%d %H:%i:%s') AS '操作时间'
FROM sys_operation_log
ORDER BY operation_time DESC
LIMIT 5;
