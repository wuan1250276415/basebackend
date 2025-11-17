-- ========================================
-- BaseBackend Dict Service 数据库初始化脚本
-- ========================================
-- 功能：创建字典服务独立数据库
-- 包含：字典类型表、字典数据表
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_dict`
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_dict`;

-- ========================================
-- 1. sys_dict - 系统字典类型表
-- ========================================
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_id` BIGINT(20) DEFAULT NULL COMMENT '应用ID（NULL表示系统字典）',
    `dict_name` VARCHAR(100) NOT NULL COMMENT '字典名称',
    `dict_type` VARCHAR(100) NOT NULL COMMENT '字典类型',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type` (`dict_type`, `deleted`),
    KEY `idx_app_id` (`app_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型表';

-- ========================================
-- 2. sys_dict_data - 系统字典数据表
-- ========================================
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_id` BIGINT(20) DEFAULT NULL COMMENT '应用ID（NULL表示系统字典）',
    `dict_sort` INT(4) DEFAULT 0 COMMENT '字典排序',
    `dict_label` VARCHAR(100) NOT NULL COMMENT '字典标签',
    `dict_value` VARCHAR(100) NOT NULL COMMENT '字典键值',
    `dict_type` VARCHAR(100) NOT NULL COMMENT '字典类型',
    `css_class` VARCHAR(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
    `list_class` VARCHAR(100) DEFAULT NULL COMMENT '表格回显样式',
    `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否默认：0-否，1-是',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_dict_type` (`dict_type`),
    KEY `idx_dict_label` (`dict_label`),
    KEY `idx_status` (`status`),
    KEY `idx_dict_sort` (`dict_sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典数据表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入系统常用字典类型
INSERT INTO `sys_dict` (`id`, `app_id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, NULL, '用户性别', 'user_gender', 1, '用户性别字典', 1, NOW(), 1, NOW(), 0),
(2, NULL, '用户状态', 'user_status', 1, '用户状态字典', 1, NOW(), 1, NOW(), 0),
(3, NULL, '菜单状态', 'menu_status', 1, '菜单状态字典', 1, NOW(), 1, NOW(), 0),
(4, NULL, '系统开关', 'sys_switch', 1, '系统开关字典', 1, NOW(), 1, NOW(), 0),
(5, NULL, '任务状态', 'task_status', 1, '任务状态字典', 1, NOW(), 1, NOW(), 0),
(6, NULL, '数据范围', 'data_scope', 1, '数据权限范围字典', 1, NOW(), 1, NOW(), 0),
(7, NULL, '通知类型', 'notice_type', 1, '通知类型字典', 1, NOW(), 1, NOW(), 0),
(8, NULL, '通知状态', 'notice_status', 1, '通知状态字典', 1, NOW(), 1, NOW(), 0);

-- 插入用户性别字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '男', '1', 'user_gender', '', 'primary', 0, 1, '男性', 1, NOW(), 1, NOW(), 0),
(2, '女', '2', 'user_gender', '', 'danger', 0, 1, '女性', 1, NOW(), 1, NOW(), 0),
(3, '未知', '0', 'user_gender', '', 'info', 1, 1, '性别未知', 1, NOW(), 1, NOW(), 0);

-- 插入用户状态字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '正常', '1', 'user_status', '', 'success', 1, 1, '用户正常状态', 1, NOW(), 1, NOW(), 0),
(2, '停用', '0', 'user_status', '', 'danger', 0, 1, '用户停用状态', 1, NOW(), 1, NOW(), 0);

-- 插入菜单状态字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '显示', '1', 'menu_status', '', 'success', 1, 1, '菜单显示', 1, NOW(), 1, NOW(), 0),
(2, '隐藏', '0', 'menu_status', '', 'info', 0, 1, '菜单隐藏', 1, NOW(), 1, NOW(), 0);

-- 插入系统开关字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '开启', '1', 'sys_switch', '', 'success', 1, 1, '开启状态', 1, NOW(), 1, NOW(), 0),
(2, '关闭', '0', 'sys_switch', '', 'danger', 0, 1, '关闭状态', 1, NOW(), 1, NOW(), 0);

-- 插入任务状态字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '待执行', '0', 'task_status', '', 'info', 1, 1, '任务待执行', 1, NOW(), 1, NOW(), 0),
(2, '执行中', '1', 'task_status', '', 'primary', 0, 1, '任务执行中', 1, NOW(), 1, NOW(), 0),
(3, '已完成', '2', 'task_status', '', 'success', 0, 1, '任务已完成', 1, NOW(), 1, NOW(), 0),
(4, '已失败', '3', 'task_status', '', 'danger', 0, 1, '任务已失败', 1, NOW(), 1, NOW(), 0);

-- 插入数据范围字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '全部数据权限', '1', 'data_scope', '', '', 0, 1, '全部数据权限', 1, NOW(), 1, NOW(), 0),
(2, '本部门数据权限', '2', 'data_scope', '', '', 0, 1, '本部门数据权限', 1, NOW(), 1, NOW(), 0),
(3, '本部门及以下数据权限', '3', 'data_scope', '', '', 0, 1, '本部门及以下数据权限', 1, NOW(), 1, NOW(), 0),
(4, '仅本人数据权限', '4', 'data_scope', '', '', 1, 1, '仅本人数据权限', 1, NOW(), 1, NOW(), 0);

-- 插入通知类型字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '系统通知', '1', 'notice_type', '', 'primary', 0, 1, '系统通知', 1, NOW(), 1, NOW(), 0),
(2, '公告', '2', 'notice_type', '', 'success', 0, 1, '公告', 1, NOW(), 1, NOW(), 0),
(3, '警告', '3', 'notice_type', '', 'warning', 0, 1, '警告', 1, NOW(), 1, NOW(), 0);

-- 插入通知状态字典数据
INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '未读', '0', 'notice_status', '', 'info', 1, 1, '未读状态', 1, NOW(), 1, NOW(), 0),
(2, '已读', '1', 'notice_status', '', 'success', 0, 1, '已读状态', 1, NOW(), 1, NOW(), 0);

-- 完成
SELECT '✓ Dict Service 数据库初始化完成' AS 'Status';
SELECT '✓ 已创建 2 张表：sys_dict, sys_dict_data' AS 'Tables';
SELECT '✓ 已初始化 8 个字典类型、30+ 条字典数据' AS 'Data';
