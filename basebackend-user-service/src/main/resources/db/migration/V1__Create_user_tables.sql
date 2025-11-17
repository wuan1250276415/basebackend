-- =====================================================================
-- 用户服务数据库迁移脚本 V1
-- 创建时间: 2025-11-15
-- 描述: 创建用户相关表结构
-- =====================================================================

-- 用户表
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_type` tinyint NOT NULL DEFAULT 1 COMMENT '用户类型：1-系统用户，2-普通用户',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `update_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '更新者',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  UNIQUE KEY `uk_email` (`email`) USING BTREE,
  UNIQUE KEY `uk_phone` (`phone`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户角色关联表
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 用户部门关联表
CREATE TABLE `sys_user_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dept` (`user_id`, `dept_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户部门关联表';

-- =====================================================================
-- 插入初始数据
-- =====================================================================

-- 插入默认管理员用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `gender`, `birthday`, `dept_id`, `user_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `remark`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSOfvamj/hzXeG7H/ihz1J4h4vZJz7L8YJzJ4h4vZJz', '系统管理员', 'admin@example.com', '13800138000', NULL, 1, '1990-01-01', NULL, 1, 1, NOW(), NOW(), 'system', 'system', 0, '默认管理员账户');

-- 插入测试用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `gender`, `birthday`, `dept_id`, `user_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `is_deleted`, `remark`) VALUES
(2, 'test', '$2a$10$7JB720yubVSOfvamj/hzXeG7H/ihz1J4h4vZJz7L8YJzJ4h4vZJz', '测试用户', 'test@example.com', '13800138001', NULL, 1, '1995-01-01', NULL, 2, 1, NOW(), NOW(), 'admin', 'admin', 0, '测试用户账户');

-- 插入默认角色
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_time`, `create_by`) VALUES
(1, 1, 1, NOW(), 'system'), -- 管理员分配管理员角色
(2, 2, 2, NOW(), 'admin');  -- 测试用户分配普通用户角色
