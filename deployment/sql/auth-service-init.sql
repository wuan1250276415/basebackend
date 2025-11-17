-- ========================================
-- BaseBackend Auth Service 数据库初始化脚本
-- ========================================
-- 功能：创建认证授权服务独立数据库
-- 包含：角色、权限、菜单及其关联表
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_auth`
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_auth`;

-- ========================================
-- 1. sys_role - 系统角色表
-- ========================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父角色ID（0表示顶级角色）',
    `app_id` BIGINT(20) DEFAULT NULL COMMENT '所属应用ID（NULL表示系统角色）',
    `role_name` VARCHAR(30) NOT NULL COMMENT '角色名称',
    `role_key` VARCHAR(100) NOT NULL COMMENT '角色标识',
    `role_sort` INT(4) DEFAULT 0 COMMENT '显示顺序',
    `data_scope` TINYINT(1) DEFAULT 1 COMMENT '数据范围：1-全部，2-本部门，3-本部门及以下，4-仅本人',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`, `deleted`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ========================================
-- 2. sys_permission - 系统权限表
-- ========================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `permission_key` VARCHAR(100) NOT NULL COMMENT '权限标识',
    `api_path` VARCHAR(200) DEFAULT NULL COMMENT 'API路径',
    `http_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP方法',
    `permission_type` TINYINT(1) DEFAULT 3 COMMENT '权限类型：1-菜单权限，2-按钮权限，3-API权限',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_key` (`permission_key`, `deleted`),
    KEY `idx_permission_type` (`permission_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- ========================================
-- 3. sys_menu - 系统菜单表
-- ========================================
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_id` BIGINT(20) DEFAULT NULL COMMENT '应用ID（为空表示系统菜单）',
    `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父菜单ID',
    `order_num` INT(4) DEFAULT 0 COMMENT '显示顺序',
    `path` VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
    `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    `query` VARCHAR(255) DEFAULT NULL COMMENT '路由参数',
    `is_frame` TINYINT(1) DEFAULT 1 COMMENT '是否为外链：0-是，1-否',
    `is_cache` TINYINT(1) DEFAULT 0 COMMENT '是否缓存：0-缓存，1-不缓存',
    `menu_type` CHAR(1) NOT NULL COMMENT '菜单类型：M-目录，C-菜单，F-按钮',
    `visible` TINYINT(1) DEFAULT 1 COMMENT '菜单状态：0-隐藏，1-显示',
    `status` TINYINT(1) DEFAULT 1 COMMENT '菜单状态：0-禁用，1-启用',
    `perms` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_menu_type` (`menu_type`),
    KEY `idx_visible` (`visible`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- ========================================
-- 4. sys_user_role - 用户角色关联表
-- ========================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ========================================
-- 5. sys_role_permission - 角色权限关联表
-- ========================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT(20) NOT NULL COMMENT '权限ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ========================================
-- 6. sys_role_menu - 角色菜单关联表
-- ========================================
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT(20) NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入初始角色：超级管理员
INSERT INTO `sys_role` VALUES (
    1, 0, NULL, '超级管理员', 'admin', 1, 1, 1, '系统超级管理员角色，拥有所有权限',
    1, NOW(), 1, NOW(), 0
);

-- 插入初始角色：普通用户
INSERT INTO `sys_role` VALUES (
    2, 0, NULL, '普通用户', 'user', 2, 4, 1, '普通用户角色，只能查看自己的数据',
    1, NOW(), 1, NOW(), 0
);

-- 插入初始权限
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_key`, `api_path`, `http_method`, `permission_type`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '用户查询', 'user:query', '/api/users', 'GET', 3, 1, '查询用户列表权限', 1, NOW(), 1, NOW(), 0),
(2, '用户创建', 'user:create', '/api/users', 'POST', 3, 1, '创建用户权限', 1, NOW(), 1, NOW(), 0),
(3, '用户更新', 'user:update', '/api/users/*', 'PUT', 3, 1, '更新用户权限', 1, NOW(), 1, NOW(), 0),
(4, '用户删除', 'user:delete', '/api/users/*', 'DELETE', 3, 1, '删除用户权限', 1, NOW(), 1, NOW(), 0),
(5, '角色查询', 'role:query', '/api/roles', 'GET', 3, 1, '查询角色列表权限', 1, NOW(), 1, NOW(), 0),
(6, '角色创建', 'role:create', '/api/roles', 'POST', 3, 1, '创建角色权限', 1, NOW(), 1, NOW(), 0),
(7, '角色更新', 'role:update', '/api/roles/*', 'PUT', 3, 1, '更新角色权限', 1, NOW(), 1, NOW(), 0),
(8, '角色删除', 'role:delete', '/api/roles/*', 'DELETE', 3, 1, '删除角色权限', 1, NOW(), 1, NOW(), 0);

-- 插入初始菜单
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '系统管理', 0, 1, '/system', NULL, 'M', 1, 1, NULL, 'system', '系统管理目录', 1, NOW(), 1, NOW(), 0),
(2, '用户管理', 1, 1, '/system/user', 'system/user/index', 'C', 1, 1, 'user:query', 'user', '用户管理菜单', 1, NOW(), 1, NOW(), 0),
(3, '角色管理', 1, 2, '/system/role', 'system/role/index', 'C', 1, 1, 'role:query', 'role', '角色管理菜单', 1, NOW(), 1, NOW(), 0),
(4, '菜单管理', 1, 3, '/system/menu', 'system/menu/index', 'C', 1, 1, 'menu:query', 'menu', '菜单管理菜单', 1, NOW(), 1, NOW(), 0),
(5, '权限管理', 1, 4, '/system/permission', 'system/permission/index', 'C', 1, 1, 'permission:query', 'permission', '权限管理菜单', 1, NOW(), 1, NOW(), 0);

-- 为超级管理员角色分配所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`, `create_by`, `create_time`) VALUES
(1, 1, 1, NOW()), (1, 2, 1, NOW()), (1, 3, 1, NOW()), (1, 4, 1, NOW()),
(1, 5, 1, NOW()), (1, 6, 1, NOW()), (1, 7, 1, NOW()), (1, 8, 1, NOW());

-- 为超级管理员角色分配所有菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_by`, `create_time`) VALUES
(1, 1, 1, NOW()), (1, 2, 1, NOW()), (1, 3, 1, NOW()), (1, 4, 1, NOW()), (1, 5, 1, NOW());

-- 为 admin 用户分配超级管理员角色（用户ID为1，来自user-service）
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_by`, `create_time`) VALUES (1, 1, 1, NOW());

-- 完成
SELECT '✓ Auth Service 数据库初始化完成' AS 'Status';
SELECT '✓ 已创建 6 张表：sys_role, sys_permission, sys_menu, sys_user_role, sys_role_permission, sys_role_menu' AS 'Tables';
SELECT '✓ 已初始化 2 个角色、8 个权限、5 个菜单' AS 'Data';
