-- 修复菜单路径和结构的SQL脚本

USE basebackend_admin;

-- 1. 添加仪表盘菜单（如果不存在）
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`) VALUES
(1, '仪表盘', 0, 0, '/dashboard', 'dashboard/index', 1, 0, 'C', 1, 1, NULL, 'dashboard', '仪表盘', 1);

-- 2. 更新系统管理菜单为目录类型
UPDATE `sys_menu` SET 
  `menu_name` = '系统管理',
  `parent_id` = 0,
  `order_num` = 1,
  `path` = 'system',
  `component` = NULL,
  `menu_type` = 'M',
  `icon` = 'system'
WHERE `id` = 2;

-- 3. 更新系统监控菜单为目录类型
UPDATE `sys_menu` SET 
  `menu_name` = '系统监控',
  `parent_id` = 0,
  `order_num` = 2,
  `path` = 'monitor',
  `component` = NULL,
  `menu_type` = 'M',
  `icon` = 'monitor'
WHERE `id` = 3;

-- 4. 添加个人中心目录
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`) VALUES
(4, '个人中心', 0, 3, 'user', NULL, 1, 0, 'M', 1, 1, NULL, 'user', '个人中心目录', 1);

-- 5. 更新系统管理子菜单的父ID和路径
UPDATE `sys_menu` SET 
  `parent_id` = 2,
  `path` = CONCAT('/', `path`)
WHERE `id` IN (100, 101, 102, 103, 104, 105, 106);

-- 6. 更新个人中心子菜单
UPDATE `sys_menu` SET 
  `parent_id` = 4,
  `path` = '/user/profile',
  `menu_name` = '个人信息'
WHERE `id` = 200;

-- 7. 更新系统监控子菜单的父ID和路径
UPDATE `sys_menu` SET 
  `parent_id` = 3,
  `path` = CONCAT('/', `path`)
WHERE `id` IN (300, 301);

-- 8. 添加新的监控菜单
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`) VALUES
(302, '登录日志', 3, 3, '/monitor/loginlog', 'monitor/loginlog/index', 1, 0, 'C', 1, 1, 'monitor:loginlog:list', 'logininfor', '登录日志菜单', 1),
(303, '操作日志', 3, 4, '/monitor/operlog', 'monitor/operlog/index', 1, 0, 'C', 1, 1, 'monitor:operlog:list', 'form', '操作日志菜单', 1);

-- 9. 更新角色菜单关联（为超级管理员分配所有菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1),   -- 仪表盘
(1, 2),   -- 系统管理
(1, 3),   -- 系统监控
(1, 4),   -- 个人中心
(1, 100), -- 用户管理
(1, 101), -- 角色管理
(1, 102), -- 菜单管理
(1, 103), -- 部门管理
(1, 104), -- 字典管理
(1, 200), -- 个人信息
(1, 300), -- 在线用户
(1, 301), -- 服务器监控
(1, 302), -- 登录日志
(1, 303); -- 操作日志

-- 10. 显示更新结果
SELECT 
  id,
  menu_name,
  parent_id,
  order_num,
  path,
  menu_type,
  icon,
  status,
  visible
FROM sys_menu 
ORDER BY order_num, id;
