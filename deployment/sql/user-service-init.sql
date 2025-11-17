-- ============================================
-- BaseBackend 用户服务数据库初始化脚本
-- ============================================
-- 用途: 创建用户服务独立数据库和表结构
-- 作者: 浮浮酱 🐱
-- 日期: 2025-11-13
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_user`;

-- ============================================
-- 1. 系统用户表
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(32) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（加密）',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(64) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT(1) DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `dept_id` BIGINT(20) DEFAULT NULL COMMENT '部门ID',
    `user_type` TINYINT(1) DEFAULT 1 COMMENT '用户类型：1-系统用户，2-普通用户',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    `login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `deleted`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================
-- 2. 初始化管理员账户
-- ============================================
-- 默认密码: admin123 (BCrypt 加密后的值)
INSERT INTO `sys_user` (
    `id`,
    `username`,
    `password`,
    `nickname`,
    `email`,
    `phone`,
    `gender`,
    `user_type`,
    `status`,
    `create_by`,
    `update_by`,
    `remark`
) VALUES (
    1,
    'admin',
    '$2a$10$N.Mf3eXQg9TqQJZ6p6yPLeF7Q1zLY/KBQkG7X.vXh3O3q3xT3Sf.O',  -- admin123
    '系统管理员',
    'admin@basebackend.com',
    '13800138000',
    1,
    1,
    1,
    1,
    1,
    '系统初始管理员账户'
);

-- ============================================
-- 3. 示例测试用户
-- ============================================
INSERT INTO `sys_user` (
    `username`,
    `password`,
    `nickname`,
    `email`,
    `phone`,
    `gender`,
    `user_type`,
    `status`,
    `create_by`,
    `update_by`
) VALUES
    ('test_user1', '$2a$10$N.Mf3eXQg9TqQJZ6p6yPLeF7Q1zLY/KBQkG7X.vXh3O3q3xT3Sf.O', '测试用户1', 'test1@example.com', '13800138001', 1, 2, 1, 1, 1),
    ('test_user2', '$2a$10$N.Mf3eXQg9TqQJZ6p6yPLeF7Q1zLY/KBQkG7X.vXh3O3q3xT3Sf.O', '测试用户2', 'test2@example.com', '13800138002', 2, 2, 1, 1, 1),
    ('test_user3', '$2a$10$N.Mf3eXQg9TqQJZ6p6yPLeF7Q1zLY/KBQkG7X.vXh3O3q3xT3Sf.O', '测试用户3', 'test3@example.com', '13800138003', 1, 2, 1, 1, 1);

-- ============================================
-- 4. 统计信息
-- ============================================
SELECT
    'sys_user' AS table_name,
    COUNT(*) AS record_count,
    '用户表' AS description
FROM sys_user
UNION ALL
SELECT
    '数据库初始化' AS table_name,
    1 AS record_count,
    '完成' AS description;

-- ============================================
-- 完成提示
-- ============================================
SELECT
    '✅ 用户服务数据库初始化完成！' AS status,
    '数据库: basebackend_user' AS database_name,
    '初始管理员: admin / admin123' AS admin_account;
