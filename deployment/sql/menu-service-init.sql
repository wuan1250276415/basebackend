-- ========================================
-- Phase 10.8: Menu Service 数据库初始化脚本
-- 菜单资源管理服务独立数据库
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_menu` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_menu`;

-- ========================================
-- 表结构: sys_application_resource (应用资源表)
-- ========================================
DROP TABLE IF EXISTS `sys_application_resource`;

CREATE TABLE `sys_application_resource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源ID',
    `app_id` BIGINT DEFAULT NULL COMMENT '所属应用ID',
    `resource_name` VARCHAR(200) NOT NULL COMMENT '资源名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父资源ID(0表示顶级)',
    `resource_type` VARCHAR(10) NOT NULL DEFAULT 'M' COMMENT '资源类型(M-目录 C-菜单 F-按钮)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '路由地址',
    `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    `perms` VARCHAR(200) DEFAULT NULL COMMENT '权限标识',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示(0-隐藏 1-显示)',
    `open_type` VARCHAR(20) DEFAULT 'current' COMMENT '打开方式(current-当前页 blank-新窗口)',
    `order_num` INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0-禁用 1-启用)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_resource_type` (`resource_type`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_order_num` (`order_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用资源(菜单)表';

-- ========================================
-- 表结构: sys_role_resource (角色资源关联表)
-- ========================================
DROP TABLE IF EXISTS `sys_role_resource`;

CREATE TABLE `sys_role_resource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `resource_id` BIGINT NOT NULL COMMENT '资源ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_resource` (`role_id`, `resource_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色资源关联表';

-- ========================================
-- 表结构: sys_role_menu (角色菜单关联表)
-- ========================================
DROP TABLE IF EXISTS `sys_role_menu`;

CREATE TABLE `sys_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ========================================
-- 初始化数据: 示例菜单资源
-- ========================================

-- 插入顶级目录（系统管理）
INSERT INTO `sys_application_resource` (`id`, `app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`) VALUES
(1, 1, '系统管理', 0, 'M', '/system', NULL, NULL, 'system', 1, 1, 1, '系统管理目录'),
(2, 1, '权限管理', 0, 'M', '/auth', NULL, NULL, 'lock', 1, 2, 1, '权限管理目录'),
(3, 1, '业务管理', 0, 'M', '/business', NULL, NULL, 'shopping', 1, 3, 1, '业务管理目录');

-- 系统管理子菜单
INSERT INTO `sys_application_resource` (`id`, `app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`) VALUES
(11, 1, '用户管理', 1, 'C', '/system/user', '/system/user/index', 'system:user:list', 'user', 1, 1, 1, '用户管理菜单'),
(12, 1, '部门管理', 1, 'C', '/system/dept', '/system/dept/index', 'system:dept:list', 'tree', 1, 2, 1, '部门管理菜单'),
(13, 1, '字典管理', 1, 'C', '/system/dict', '/system/dict/index', 'system:dict:list', 'dict', 1, 3, 1, '字典管理菜单'),
(14, 1, '日志管理', 1, 'C', '/system/log', '/system/log/index', 'system:log:list', 'log', 1, 4, 1, '日志管理菜单'),
(15, 1, '通知管理', 1, 'C', '/system/notification', '/system/notification/index', 'system:notification:list', 'message', 1, 5, 1, '通知管理菜单');

-- 权限管理子菜单
INSERT INTO `sys_application_resource` (`id`, `app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`) VALUES
(21, 1, '角色管理', 2, 'C', '/auth/role', '/auth/role/index', 'auth:role:list', 'peoples', 1, 1, 1, '角色管理菜单'),
(22, 1, '权限管理', 2, 'C', '/auth/permission', '/auth/permission/index', 'auth:permission:list', 'tree-table', 1, 2, 1, '权限管理菜单'),
(23, 1, '菜单管理', 2, 'C', '/auth/menu', '/auth/menu/index', 'auth:menu:list', 'tree', 1, 3, 1, '菜单管理菜单');

-- 业务管理子菜单
INSERT INTO `sys_application_resource` (`id`, `app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`) VALUES
(31, 1, '应用管理', 3, 'C', '/business/application', '/business/application/index', 'business:application:list', 'component', 1, 1, 1, '应用管理菜单');

-- 用户管理按钮权限
INSERT INTO `sys_application_resource` (`id`, `app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`) VALUES
(111, 1, '用户查询', 11, 'F', NULL, NULL, 'system:user:query', NULL, 1, 1, 1, '用户查询按钮'),
(112, 1, '用户新增', 11, 'F', NULL, NULL, 'system:user:add', NULL, 1, 2, 1, '用户新增按钮'),
(113, 1, '用户修改', 11, 'F', NULL, NULL, 'system:user:edit', NULL, 1, 3, 1, '用户修改按钮'),
(114, 1, '用户删除', 11, 'F', NULL, NULL, 'system:user:remove', NULL, 1, 4, 1, '用户删除按钮'),
(115, 1, '重置密码', 11, 'F', NULL, NULL, 'system:user:resetPwd', NULL, 1, 5, 1, '重置密码按钮');

-- 角色管理按钮权限
INSERT INTO `sys_application_resource` (`id`, `app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`) VALUES
(211, 1, '角色查询', 21, 'F', NULL, NULL, 'auth:role:query', NULL, 1, 1, 1, '角色查询按钮'),
(212, 1, '角色新增', 21, 'F', NULL, NULL, 'auth:role:add', NULL, 1, 2, 1, '角色新增按钮'),
(213, 1, '角色修改', 21, 'F', NULL, NULL, 'auth:role:edit', NULL, 1, 3, 1, '角色修改按钮'),
(214, 1, '角色删除', 21, 'F', NULL, NULL, 'auth:role:remove', NULL, 1, 4, 1, '角色删除按钮'),
(215, 1, '分配权限', 21, 'F', NULL, NULL, 'auth:role:authorize', NULL, 1, 5, 1, '分配权限按钮');

-- 菜单管理按钮权限
INSERT INTO `sys_application_resource` (`id`, `app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`) VALUES
(231, 1, '菜单查询', 23, 'F', NULL, NULL, 'auth:menu:query', NULL, 1, 1, 1, '菜单查询按钮'),
(232, 1, '菜单新增', 23, 'F', NULL, NULL, 'auth:menu:add', NULL, 1, 2, 1, '菜单新增按钮'),
(233, 1, '菜单修改', 23, 'F', NULL, NULL, 'auth:menu:edit', NULL, 1, 3, 1, '菜单修改按钮'),
(234, 1, '菜单删除', 23, 'F', NULL, NULL, 'auth:menu:remove', NULL, 1, 4, 1, '菜单删除按钮');

-- ========================================
-- 数据统计查询
-- ========================================

-- 统计各类型资源数量
SELECT
    resource_type AS '资源类型',
    CASE
        WHEN resource_type = 'M' THEN '目录'
        WHEN resource_type = 'C' THEN '菜单'
        WHEN resource_type = 'F' THEN '按钮'
        ELSE '未知'
    END AS '类型名称',
    COUNT(*) AS '总数'
FROM sys_application_resource
WHERE deleted = 0
GROUP BY resource_type
ORDER BY resource_type;

-- 统计各应用资源数量
SELECT
    COALESCE(app_id, 0) AS '应用ID',
    COUNT(*) AS '资源总数',
    SUM(CASE WHEN resource_type = 'M' THEN 1 ELSE 0 END) AS '目录数',
    SUM(CASE WHEN resource_type = 'C' THEN 1 ELSE 0 END) AS '菜单数',
    SUM(CASE WHEN resource_type = 'F' THEN 1 ELSE 0 END) AS '按钮数'
FROM sys_application_resource
WHERE deleted = 0
GROUP BY app_id
ORDER BY app_id;

-- 查看菜单树结构（顶级目录）
SELECT
    id AS 'ID',
    resource_name AS '资源名称',
    resource_type AS '类型',
    path AS '路径',
    icon AS '图标',
    order_num AS '排序',
    CASE status WHEN 1 THEN '启用' ELSE '禁用' END AS '状态'
FROM sys_application_resource
WHERE deleted = 0 AND parent_id = 0
ORDER BY order_num;

-- ========================================
-- 初始化完成提示
-- ========================================
SELECT '✅ Menu Service 数据库初始化完成！' AS '状态',
       '已创建 3 张表并插入 26 条示例菜单资源数据' AS '说明';
