-- 权限服务数据库迁移脚本
-- 创建时间: 2025-11-15
-- 描述: 创建权限管理相关表

-- 创建角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` varchar(30) NOT NULL COMMENT '角色名称',
    `role_code` varchar(100) NOT NULL COMMENT '角色权限字符串',
    `role_sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `data_scope` char(1) DEFAULT NULL COMMENT '数据范围（1：全部数据权限 2：自定数据权限）',
    `menu_check_strictly` tinyint NOT NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
    `dept_check_strictly` tinyint NOT NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
    `status` char(1) NOT NULL DEFAULT '0' COMMENT '角色状态（0正常 1停用)',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_name` (`role_name`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_role_status` (`status`),
    KEY `idx_role_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

-- 创建权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
    `permission_code` varchar(100) NOT NULL COMMENT '权限字符串',
    `resource_type` varchar(20) NOT NULL DEFAULT 'menu' COMMENT '资源类型（menu：菜单，button：按钮)',
    `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父权限ID',
    `permission_url` varchar(200) DEFAULT NULL COMMENT '权限URL',
    `permission_icon` varchar(100) DEFAULT NULL COMMENT '权限图标',
    `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
    `is_frame` tinyint NOT NULL DEFAULT 0 COMMENT '是否为外链（0是 1否)',
    `is_cache` tinyint NOT NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存)',
    `visible` tinyint NOT NULL DEFAULT 1 COMMENT '是否显示（0显示 1隐藏)',
    `status` char(1) NOT NULL DEFAULT '0' COMMENT '权限状态（0正常 1停用)',
    `perms` varchar(100) DEFAULT NULL COMMENT '权限标识字符串',
    `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
    `order_num` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `path` varchar(200) DEFAULT NULL COMMENT '路由地址',
    `component_name` varchar(100) DEFAULT NULL COMMENT '组件名称',
    `query` varchar(255) DEFAULT NULL COMMENT '路由参数',
    `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除 1已删除）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_permission_parent` (`parent_id`),
    KEY `idx_permission_status` (`status`),
    KEY `idx_permission_deleted` (`is_deleted`),
    KEY `idx_permission_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限信息表';

-- 创建角色和权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `permission_id` bigint NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY `idx_role_permission_role` (`role_id`),
    KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和权限关联表';

-- 创建用户和角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_role_user` (`user_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

-- 插入初始角色数据
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_sort`, `data_scope`, `menu_check_strictly`, `status`, `del_flag`, `remark`) VALUES
(1, '超级管理员', 'ROLE_ADMIN', 1, '1', 1, '0', '0', '拥有系统所有权限'),
(2, '普通用户', 'ROLE_USER', 2, '2', 1, '0', '0', '普通用户角色'),
(3, '部门管理员', 'ROLE_MANAGER', 3, '2', 1, '0', '0', '部门管理员角色');

-- 插入初始权限数据
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `resource_type`, `parent_id`, `permission_url`, `is_frame`, `visible`, `status`, `order_num`, `remark`) VALUES
(1, '系统管理', 'system', 'menu', 0, NULL, 0, 1, '0', 1, '系统管理菜单'),
(2, '用户管理', 'system:user', 'menu', 1, '/system/user', 0, 1, '0', 1, '用户管理菜单'),
(3, '角色管理', 'system:role', 'menu', 1, '/system/role', 0, 1, '0', 2, '角色管理菜单'),
(4, '权限管理', 'system:permission', 'menu', 1, '/system/permission', 0, 1, '0', 3, '权限管理菜单'),
(5, '部门管理', 'system:dept', 'menu', 1, '/system/dept', 0, 1, '0', 4, '部门管理菜单'),
(6, '字典管理', 'system:dict', 'menu', 1, '/system/dict', 0, 1, '0', 5, '字典管理菜单'),
(7, '日志管理', 'system:log', 'menu', 1, '/system/log', 0, 1, '0', 6, '日志管理菜单');

-- 插入按钮权限数据
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `resource_type`, `parent_id`, `permission_url`, `is_frame`, `visible`, `status`, `order_num`, `remark`) VALUES
(100, '用户查询', 'system:user:query', 'button', 2, NULL, 0, 1, '0', 1, '用户查询按钮'),
(101, '用户新增', 'system:user:add', 'button', 2, NULL, 0, 1, '0', 2, '用户新增按钮'),
(102, '用户修改', 'system:user:edit', 'button', 2, NULL, 0, 1, '0', 3, '用户修改按钮'),
(103, '用户删除', 'system:user:remove', 'button', 2, NULL, 0, 1, '0', 4, '用户删除按钮'),
(104, '用户导出', 'system:user:export', 'button', 2, NULL, 0, 1, '0', 5, '用户导出按钮'),
(200, '角色查询', 'system:role:query', 'button', 3, NULL, 0, 1, '0', 1, '角色查询按钮'),
(201, '角色新增', 'system:role:add', 'button', 3, NULL, 0, 1, '0', 2, '角色新增按钮'),
(202, '角色修改', 'system:role:edit', 'button', 3, NULL, 0, 1, '0', 3, '角色修改按钮'),
(203, '角色删除', 'system:role:remove', 'button', 3, NULL, 0, 1, '0', 4, '角色删除按钮'),
(204, '角色导出', 'system:role:export', 'button', 3, NULL, 0, 1, '0', 5, '角色导出按钮');

-- 关联角色和权限（超级管理员拥有所有权限）
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission`;

-- 关联用户和角色（admin用户分配超级管理员角色）
-- 注意：这里假设admin用户的ID是1，实际使用时需要根据实际情况调整
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1);
