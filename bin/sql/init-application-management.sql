-- =====================================================
-- 应用管理系统数据库初始化脚本
-- =====================================================

-- 1. 创建应用信息表
DROP TABLE IF EXISTS `sys_application`;
CREATE TABLE `sys_application` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '应用ID',
  `app_name` VARCHAR(100) NOT NULL COMMENT '应用名称',
  `app_code` VARCHAR(50) NOT NULL COMMENT '应用编码（唯一标识）',
  `app_type` VARCHAR(20) NOT NULL COMMENT '应用类型（web/mobile/api等）',
  `app_icon` VARCHAR(255) DEFAULT NULL COMMENT '应用图标',
  `app_url` VARCHAR(255) DEFAULT NULL COMMENT '应用地址',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `order_num` INT(11) DEFAULT 0 COMMENT '显示顺序',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_code` (`app_code`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用信息表';

-- 2. 创建应用资源（菜单）表
DROP TABLE IF EXISTS `sys_application_resource`;
CREATE TABLE `sys_application_resource` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `app_id` BIGINT(20) NOT NULL COMMENT '所属应用ID',
  `resource_name` VARCHAR(100) NOT NULL COMMENT '资源名称',
  `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父资源ID（0表示顶级）',
  `resource_type` VARCHAR(20) NOT NULL COMMENT '资源类型：M-目录，C-菜单，F-按钮',
  `path` VARCHAR(255) DEFAULT NULL COMMENT '路由地址',
  `component` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  `perms` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
  `visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否显示：0-隐藏，1-显示',
  `open_type` VARCHAR(20) DEFAULT 'current' COMMENT '打开方式：current-当前页，blank-新窗口',
  `order_num` INT(11) DEFAULT 0 COMMENT '显示顺序',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用资源表';

-- 3. 修改角色表，添加应用ID字段
ALTER TABLE `sys_role` ADD COLUMN `app_id` BIGINT(20) DEFAULT NULL COMMENT '所属应用ID（NULL表示系统角色）' AFTER `id`;
ALTER TABLE `sys_role` ADD KEY `idx_app_id` (`app_id`);

-- 4. 修改字典表，添加应用ID字段
ALTER TABLE `sys_dict` ADD COLUMN `app_id` BIGINT(20) DEFAULT NULL COMMENT '所属应用ID（NULL表示系统字典）' AFTER `id`;
ALTER TABLE `sys_dict` ADD KEY `idx_app_id` (`app_id`);

ALTER TABLE `sys_dict_data` ADD COLUMN `app_id` BIGINT(20) DEFAULT NULL COMMENT '所属应用ID（NULL表示系统字典）' AFTER `id`;
ALTER TABLE `sys_dict_data` ADD KEY `idx_app_id` (`app_id`);

-- 5. 创建角色资源关联表
DROP TABLE IF EXISTS `sys_role_resource`;
CREATE TABLE `sys_role_resource` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
  `resource_id` BIGINT(20) NOT NULL COMMENT '资源ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_resource` (`role_id`, `resource_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色资源关联表';

-- 6. 插入默认应用数据
INSERT INTO `sys_application` (`id`, `app_name`, `app_code`, `app_type`, `app_icon`, `app_url`, `status`, `order_num`, `remark`, `create_by`)
VALUES
(1, '系统管理平台', 'admin', 'web', 'el-icon-setting', '/admin', 1, 1, '后台管理系统', 'system'),
(2, '用户门户', 'portal', 'web', 'el-icon-user', '/portal', 1, 2, '用户门户系统', 'system');

-- 7. 迁移现有菜单数据到应用资源表（将sys_menu的数据迁移到sys_application_resource）
INSERT INTO `sys_application_resource`
(`app_id`, `resource_name`, `parent_id`, `resource_type`, `path`, `component`, `perms`, `icon`, `visible`, `order_num`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT
  1 AS app_id,
  menu_name,
  parent_id,
  menu_type,
  path,
  component,
  perms,
  icon,
  visible,
  order_num,
  status,
  remark,
  create_by,
  create_time,
  update_by,
  update_time
FROM `sys_menu`
WHERE deleted = 0;

-- 8. 迁移角色菜单关联数据到角色资源关联表
INSERT INTO `sys_role_resource` (`role_id`, `resource_id`)
SELECT
  rm.role_id,
  ar.id
FROM `sys_role_menu` rm
INNER JOIN `sys_menu` m ON rm.menu_id = m.id
INNER JOIN `sys_application_resource` ar ON ar.resource_name = m.menu_name AND ar.app_id = 1
WHERE m.deleted = 0;

-- 9. 更新现有角色的应用ID（默认都归属到系统管理平台）
UPDATE `sys_role` SET `app_id` = 1 WHERE `app_id` IS NULL;

-- 完成
SELECT '应用管理系统初始化完成！' AS message;
