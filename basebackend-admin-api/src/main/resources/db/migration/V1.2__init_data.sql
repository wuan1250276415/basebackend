-- ============================
-- Flyway Migration: V1.2
-- Description: 初始化系统数据
-- Author: BaseBackend Team
-- Date: 2025-10-23
-- ============================


-- ============================
-- 插入系统用户
-- ============================
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `gender`, `dept_id`, `user_type`, `status`, `create_by`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSOfvVMe6leNeP1/1y2U3wY97xVkG3V0/4qG9VkG3V0', '超级管理员', 'admin@basebackend.com', '13800138000', 1, 1, 1, 1, 1),
(2, 'test', '$2a$10$7JB720yubVSOfvVMe6leNeP1/1y2U3wY97xVkG3V0/4qG9VkG3V0', '测试用户', 'test@basebackend.com', '13800138001', 1, 2, 1, 1, 1);

-- ============================
-- 插入系统角色
-- ============================
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `remark`, `create_by`) VALUES
(1, '超级管理员', 'admin', 1, 1, 1, '超级管理员，拥有所有权限', 1),
(2, '普通管理员', 'manager', 2, 2, 1, '普通管理员，拥有部门数据权限', 1),
(3, '普通用户', 'user', 3, 4, 1, '普通用户，仅本人数据权限', 1);

-- ============================
-- 插入系统菜单
-- ============================
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`) VALUES
-- 一级菜单
(1, '仪表盘', 0, 0, '/dashboard', 'dashboard/index', 1, 0, 'C', 1, 1, NULL, 'dashboard', '仪表盘', 1),
(2, '系统管理', 0, 1, 'system', NULL, 1, 0, 'M', 1, 1, NULL, 'system', '系统管理目录', 1),
(3, '系统监控', 0, 2, 'monitor', NULL, 1, 0, 'M', 1, 1, NULL, 'monitor', '系统监控目录', 1),
(4, '个人中心', 0, 3, 'user', NULL, 1, 0, 'M', 1, 1, NULL, 'user', '个人中心目录', 1),

-- 系统管理子菜单
(100, '用户管理', 2, 1, '/system/user', 'system/user/index', 1, 0, 'C', 1, 1, 'system:user:list', 'user', '用户管理菜单', 1),
(101, '角色管理', 2, 2, '/system/role', 'system/role/index', 1, 0, 'C', 1, 1, 'system:role:list', 'peoples', '角色管理菜单', 1),
(102, '菜单管理', 2, 3, '/system/menu', 'system/menu/index', 1, 0, 'C', 1, 1, 'system:menu:list', 'tree-table', '菜单管理菜单', 1),
(103, '部门管理', 2, 4, '/system/dept', 'system/dept/index', 1, 0, 'C', 1, 1, 'system:dept:list', 'tree', '部门管理菜单', 1),
(104, '字典管理', 2, 5, '/system/dict', 'system/dict/index', 1, 0, 'C', 1, 1, 'system:dict:list', 'dict', '字典管理菜单', 1),
(105, '操作日志', 2, 6, '/system/operlog', 'system/operlog/index', 1, 0, 'C', 1, 1, 'system:operlog:list', 'form', '操作日志菜单', 1),
(106, '登录日志', 2, 7, '/system/logininfor', 'system/logininfor/index', 1, 0, 'C', 1, 1, 'system:logininfor:list', 'logininfor', '登录日志菜单', 1),
(107, 'API文档', 2, 8, '/developer/api-docs', 'developer/api-docs/index', 1, 0, 'C', 1, 1, 'system:doc:view', 'docs', 'OpenAPI文档与SDK', 1),

-- 个人中心子菜单
(200, '个人信息', 4, 1, '/user/profile', 'user/profile/index', 1, 0, 'C', 1, 1, NULL, 'user', '个人信息菜单', 1),

-- 系统监控子菜单
(300, '在线用户', 3, 1, '/monitor/online', 'monitor/online/index', 1, 0, 'C', 1, 1, 'monitor:online:list', 'online', '在线用户菜单', 1),
(301, '服务器监控', 3, 2, '/monitor/server', 'monitor/server/index', 1, 0, 'C', 1, 1, 'monitor:server:list', 'server', '服务器监控菜单', 1),
(302, '登录日志', 3, 3, '/monitor/loginlog', 'monitor/loginlog/index', 1, 0, 'C', 1, 1, 'monitor:loginlog:list', 'logininfor', '登录日志菜单', 1),
(303, '操作日志', 3, 4, '/monitor/operlog', 'monitor/operlog/index', 1, 0, 'C', 1, 1, 'monitor:operlog:list', 'form', '操作日志菜单', 1),

-- 用户管理按钮
(1000, '用户查询', 100, 1, '', '', 1, 0, 'F', 1, 1, 'system:user:query', '#', '', 1),
(1001, '用户新增', 100, 2, '', '', 1, 0, 'F', 1, 1, 'system:user:add', '#', '', 1),
(1002, '用户修改', 100, 3, '', '', 1, 0, 'F', 1, 1, 'system:user:edit', '#', '', 1),
(1003, '用户删除', 100, 4, '', '', 1, 0, 'F', 1, 1, 'system:user:remove', '#', '', 1),
(1004, '用户导出', 100, 5, '', '', 1, 0, 'F', 1, 1, 'system:user:export', '#', '', 1),
(1005, '用户导入', 100, 6, '', '', 1, 0, 'F', 1, 1, 'system:user:import', '#', '', 1),
(1006, '重置密码', 100, 7, '', '', 1, 0, 'F', 1, 1, 'system:user:resetPwd', '#', '', 1),

-- 角色管理按钮
(1010, '角色查询', 101, 1, '', '', 1, 0, 'F', 1, 1, 'system:role:query', '#', '', 1),
(1011, '角色新增', 101, 2, '', '', 1, 0, 'F', 1, 1, 'system:role:add', '#', '', 1),
(1012, '角色修改', 101, 3, '', '', 1, 0, 'F', 1, 1, 'system:role:edit', '#', '', 1),
(1013, '角色删除', 101, 4, '', '', 1, 0, 'F', 1, 1, 'system:role:remove', '#', '', 1),
(1014, '角色导出', 101, 5, '', '', 1, 0, 'F', 1, 1, 'system:role:export', '#', '', 1),

-- 菜单管理按钮
(1020, '菜单查询', 102, 1, '', '', 1, 0, 'F', 1, 1, 'system:menu:query', '#', '', 1),
(1021, '菜单新增', 102, 2, '', '', 1, 0, 'F', 1, 1, 'system:menu:add', '#', '', 1),
(1022, '菜单修改', 102, 3, '', '', 1, 0, 'F', 1, 1, 'system:menu:edit', '#', '', 1),
(1023, '菜单删除', 102, 4, '', '', 1, 0, 'F', 1, 1, 'system:menu:remove', '#', '', 1);

-- ============================
-- 插入系统权限
-- ============================
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_key`, `api_path`, `http_method`, `permission_type`, `status`, `remark`, `create_by`) VALUES
(1, '用户查询', 'system:user:query', '/api/admin/users', 'GET', 3, 1, '查询用户列表', 1),
(2, '用户新增', 'system:user:add', '/api/admin/users', 'POST', 3, 1, '新增用户', 1),
(3, '用户修改', 'system:user:edit', '/api/admin/users/{id}', 'PUT', 3, 1, '修改用户', 1),
(4, '用户删除', 'system:user:remove', '/api/admin/users/{id}', 'DELETE', 3, 1, '删除用户', 1),
(5, '用户导出', 'system:user:export', '/api/admin/users/export', 'GET', 3, 1, '导出用户', 1),
(6, '用户导入', 'system:user:import', '/api/admin/users/import', 'POST', 3, 1, '导入用户', 1),
(7, '重置密码', 'system:user:resetPwd', '/api/admin/users/{id}/reset-password', 'PUT', 3, 1, '重置用户密码', 1),
(8, '角色查询', 'system:role:query', '/api/admin/roles', 'GET', 3, 1, '查询角色列表', 1),
(9, '角色新增', 'system:role:add', '/api/admin/roles', 'POST', 3, 1, '新增角色', 1),
(10, '角色修改', 'system:role:edit', '/api/admin/roles/{id}', 'PUT', 3, 1, '修改角色', 1),
(11, '角色删除', 'system:role:remove', '/api/admin/roles/{id}', 'DELETE', 3, 1, '删除角色', 1),
(12, '角色导出', 'system:role:export', '/api/admin/roles/export', 'GET', 3, 1, '导出角色', 1),
(13, '菜单查询', 'system:menu:query', '/api/admin/menus', 'GET', 3, 1, '查询菜单列表', 1),
(14, '菜单新增', 'system:menu:add', '/api/admin/menus', 'POST', 3, 1, '新增菜单', 1),
(15, '菜单修改', 'system:menu:edit', '/api/admin/menus/{id}', 'PUT', 3, 1, '修改菜单', 1),
(16, '菜单删除', 'system:menu:remove', '/api/admin/menus/{id}', 'DELETE', 3, 1, '删除菜单', 1),
(17, '查看OpenAPI文档', 'system:doc:view', '/api/admin/openapi/spec.json', 'GET', 3, 1, '查看 OpenAPI 文档', 1),
(18, '下载OpenAPI TypeScript SDK', 'system:sdk:download', '/api/admin/openapi/sdk/typescript', 'GET', 3, 1, '下载 TypeScript SDK', 1);

-- ============================
-- 插入用户角色关联
-- ============================
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_by`) VALUES
(1, 1, 1, 1),
(2, 2, 3, 1);

-- ============================
-- 插入角色菜单关联
-- ============================
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`, `create_by`) VALUES
-- 超级管理员拥有所有菜单
(1, 1, 1, 1), (2, 1, 2, 1), (3, 1, 3, 1),
(4, 1, 100, 1), (5, 1, 101, 1), (6, 1, 102, 1), (7, 1, 103, 1), (8, 1, 104, 1), (9, 1, 105, 1), (10, 1, 106, 1),
(11, 1, 200, 1), (12, 1, 300, 1), (13, 1, 301, 1),
(14, 1, 1000, 1), (15, 1, 1001, 1), (16, 1, 1002, 1), (17, 1, 1003, 1), (18, 1, 1004, 1), (19, 1, 1005, 1), (20, 1, 1006, 1),
(21, 1, 1010, 1), (22, 1, 1011, 1), (23, 1, 1012, 1), (24, 1, 1013, 1), (25, 1, 1014, 1),
(26, 1, 1020, 1), (27, 1, 1021, 1), (28, 1, 1022, 1), (29, 1, 1023, 1), (30, 1, 107, 1),

-- 普通管理员拥有部分菜单
(31, 2, 1, 1), (32, 2, 100, 1), (33, 2, 200, 1),
(34, 2, 1000, 1), (35, 2, 1001, 1), (36, 2, 1002, 1),

-- 普通用户只有用户信息
(37, 3, 2, 1), (38, 3, 200, 1);

-- ============================
-- 插入角色权限关联
-- ============================
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `create_by`) VALUES
-- 超级管理员拥有所有权限
(1, 1, 1, 1), (2, 1, 2, 1), (3, 1, 3, 1), (4, 1, 4, 1), (5, 1, 5, 1), (6, 1, 6, 1), (7, 1, 7, 1),
(8, 1, 8, 1), (9, 1, 9, 1), (10, 1, 10, 1), (11, 1, 11, 1), (12, 1, 12, 1),
(13, 1, 13, 1), (14, 1, 14, 1), (15, 1, 15, 1), (16, 1, 16, 1), (17, 1, 17, 1), (18, 1, 18, 1),

-- 普通管理员拥有部分权限
(19, 2, 1, 1), (20, 2, 2, 1), (21, 2, 3, 1),

-- 普通用户无特殊权限
(22, 3, 1, 1);

-- ============================
-- 插入系统部门
-- ============================
INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `leader`, `phone`, `email`, `status`, `remark`, `create_by`) VALUES
(1, '总公司', 0, 0, 'admin', '15888888888', 'admin@basebackend.com', 1, '总公司', 1),
(2, '技术部', 1, 1, 'admin', '15888888888', 'tech@basebackend.com', 1, '技术部门', 1),
(3, '市场部', 1, 2, 'admin', '15888888888', 'market@basebackend.com', 1, '市场部门', 1),
(4, '财务部', 1, 3, 'admin', '15888888888', 'finance@basebackend.com', 1, '财务部门', 1);

-- ============================
-- 插入数据字典
-- ============================
INSERT INTO `sys_dict` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`) VALUES
(1, '用户性别', 'sys_user_sex', 1, '用户性别列表', 1),
(2, '菜单状态', 'sys_show_hide', 1, '菜单状态列表', 1),
(3, '系统开关', 'sys_normal_disable', 1, '系统开关列表', 1),
(4, '任务状态', 'sys_job_status', 1, '任务状态列表', 1),
(5, '任务分组', 'sys_job_group', 1, '任务分组列表', 1),
(6, '系统是否', 'sys_yes_no', 1, '系统是否列表', 1),
(7, '通知类型', 'sys_notice_type', 1, '通知类型列表', 1),
(8, '通知状态', 'sys_notice_status', 1, '通知状态列表', 1),
(9, '操作类型', 'sys_oper_type', 1, '操作类型列表', 1),
(10, '系统状态', 'sys_common_status', 1, '登录状态列表', 1);

-- ============================
-- 插入字典数据
-- ============================
INSERT INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`) VALUES
(1, 1, '男', '1', 'sys_user_sex', '', '', '1', 1, '性别男', 1),
(2, 2, '女', '2', 'sys_user_sex', '', '', '0', 1, '性别女', 1),
(3, 3, '未知', '0', 'sys_user_sex', '', '', '0', 1, '性别未知', 1),
(4, 1, '显示', '1', 'sys_show_hide', '', 'primary', '1', 1, '显示菜单', 1),
(5, 2, '隐藏', '0', 'sys_show_hide', '', 'danger', '0', 1, '隐藏菜单', 1),
(6, 1, '正常', '1', 'sys_normal_disable', '', 'primary', '1', 1, '正常状态', 1),
(7, 2, '停用', '0', 'sys_normal_disable', '', 'danger', '0', 1, '停用状态', 1),
(8, 1, '正常', '1', 'sys_job_status', '', 'primary', '1', 1, '正常状态', 1),
(9, 2, '暂停', '0', 'sys_job_status', '', 'danger', '0', 1, '停用状态', 1),
(10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', '1', 1, '默认分组', 1),
(11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', '0', 1, '系统分组', 1),
(12, 1, '是', '1', 'sys_yes_no', '', 'primary', '1', 1, '系统默认是', 1),
(13, 2, '否', '0', 'sys_yes_no', '', 'danger', '0', 1, '系统默认否', 1),
(14, 1, '通知', '1', 'sys_notice_type', '', 'warning', '1', 1, '通知', 1),
(15, 2, '公告', '2', 'sys_notice_type', '', 'success', '0', 1, '公告', 1),
(16, 1, '正常', '1', 'sys_notice_status', '', 'primary', '1', 1, '正常状态', 1),
(17, 2, '关闭', '0', 'sys_notice_status', '', 'danger', '0', 1, '关闭状态', 1),
(18, 1, '新增', '1', 'sys_oper_type', '', 'info', '0', 1, '新增操作', 1),
(19, 2, '修改', '2', 'sys_oper_type', '', 'info', '0', 1, '修改操作', 1),
(20, 3, '删除', '3', 'sys_oper_type', '', 'danger', '0', 1, '删除操作', 1),
(21, 4, '授权', '4', 'sys_oper_type', '', 'primary', '0', 1, '授权操作', 1),
(22, 5, '导出', '5', 'sys_oper_type', '', 'warning', '0', 1, '导出操作', 1),
(23, 6, '导入', '6', 'sys_oper_type', '', 'warning', '0', 1, '导入操作', 1),
(24, 7, '强退', '7', 'sys_oper_type', '', 'danger', '0', 1, '强退操作', 1),
(25, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', '0', 1, '生成操作', 1),
(26, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', '0', 1, '清空操作', 1),
(27, 1, '成功', '1', 'sys_common_status', '', 'primary', '0', 1, '正常状态', 1),
(28, 2, '失败', '0', 'sys_common_status', '', 'info', '0', 1, '停用状态', 1);

-- ============================
-- 查看插入结果
-- ============================
SELECT '用户数量' AS type, COUNT(*) AS count FROM sys_user WHERE deleted = 0
UNION ALL
SELECT '角色数量' AS type, COUNT(*) AS count FROM sys_role WHERE deleted = 0
UNION ALL
SELECT '菜单数量' AS type, COUNT(*) AS count FROM sys_menu WHERE deleted = 0
UNION ALL
SELECT '权限数量' AS type, COUNT(*) AS count FROM sys_permission WHERE deleted = 0
UNION ALL
SELECT '部门数量' AS type, COUNT(*) AS count FROM sys_dept WHERE deleted = 0
UNION ALL
SELECT '字典类型数量' AS type, COUNT(*) AS count FROM sys_dict WHERE deleted = 0
UNION ALL
SELECT '字典数据数量' AS type, COUNT(*) AS count FROM sys_dict_data WHERE deleted = 0;
