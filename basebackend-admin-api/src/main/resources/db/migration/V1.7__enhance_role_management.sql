-- ============================
-- Flyway Migration: V1.7
-- Description: 增强角色管理功能（支持树形结构、细粒度数据权限、列表操作权限）
-- Author: BaseBackend Team
-- Date: 2025-10-28
-- ============================

-- ============================
-- 1. 修改sys_role表，添加parent_id字段支持树形结构
-- ============================
ALTER TABLE `sys_role`
  ADD COLUMN `parent_id` BIGINT DEFAULT 0 COMMENT '父角色ID（0表示顶级角色）' AFTER `id`,
  ADD KEY `idx_parent_id` (`parent_id`);

-- ============================
-- 2. 确保sys_role表有app_id字段（支持多应用隔离）
-- 如果之前已执行init-application-management.sql，此字段已存在，需手动跳过此步骤
-- ============================
-- ALTER TABLE `sys_role`
--   ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT '所属应用ID（NULL表示系统角色）' AFTER `parent_id`,
--   ADD KEY `idx_app_id` (`app_id`);

-- ============================
-- 3. 创建细粒度数据权限配置表
-- ============================
CREATE TABLE IF NOT EXISTS `sys_role_data_permission` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型（如：user, dept, order等）',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `filter_type` VARCHAR(20) NOT NULL COMMENT '过滤类型：dept-部门，field-字段，custom-自定义',
  `filter_rule` TEXT DEFAULT NULL COMMENT '过滤规则（JSON格式）',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色数据权限配置表';

-- ============================
-- 4. 创建列表操作定义表
-- ============================
CREATE TABLE IF NOT EXISTS `sys_list_operation` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `operation_code` VARCHAR(50) NOT NULL COMMENT '操作编码（唯一标识）',
  `operation_name` VARCHAR(100) NOT NULL COMMENT '操作名称',
  `operation_type` VARCHAR(20) NOT NULL COMMENT '操作类型：view-查看，add-新增，edit-编辑，delete-删除，export-导出，import-导入',
  `resource_type` VARCHAR(50) DEFAULT NULL COMMENT '适用资源类型（NULL表示通用）',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '操作图标',
  `order_num` INT DEFAULT 0 COMMENT '显示顺序',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_operation_code` (`operation_code`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='列表操作定义表';

-- ============================
-- 5. 创建角色列表操作关联表
-- ============================
CREATE TABLE IF NOT EXISTS `sys_role_list_operation` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `operation_id` BIGINT NOT NULL COMMENT '操作ID',
  `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_operation_resource` (`role_id`, `operation_id`, `resource_type`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_operation_id` (`operation_id`),
  KEY `idx_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色列表操作关联表';

-- ============================
-- 6. 插入默认列表操作数据
-- ============================
INSERT INTO `sys_list_operation` (`id`, `operation_code`, `operation_name`, `operation_type`, `icon`, `order_num`, `status`, `remark`, `create_by`) VALUES
(1, 'list_view', '查看', 'view', 'EyeOutlined', 1, 1, '查看详情操作', NULL),
(2, 'list_add', '新增', 'add', 'PlusOutlined', 2, 1, '新增记录操作', NULL),
(3, 'list_edit', '编辑', 'edit', 'EditOutlined', 3, 1, '编辑记录操作', NULL),
(4, 'list_delete', '删除', 'delete', 'DeleteOutlined', 4, 1, '删除记录操作', NULL),
(5, 'list_export', '导出', 'export', 'ExportOutlined', 5, 1, '导出数据操作', NULL),
(6, 'list_import', '导入', 'import', 'ImportOutlined', 6, 1, '导入数据操作', NULL),
(7, 'list_batch_delete', '批量删除', 'delete', 'DeleteOutlined', 7, 1, '批量删除操作', NULL);

-- ============================
-- 完成
-- ============================
