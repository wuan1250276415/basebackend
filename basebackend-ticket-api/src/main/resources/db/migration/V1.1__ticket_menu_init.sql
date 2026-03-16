-- =============================================
-- 工单系统菜单初始化
-- 插入 sys_application_resource 记录
-- resource_type: M=目录, C=菜单, F=按钮
-- visible: 0=隐藏, 1=显示
-- =============================================

-- 工单管理主菜单 (M 目录)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3000, 1, '工单管理', 0, 'M', '/ticket', '', 'solution', 1, 6, 1, 0, 0, NOW(), NOW(), 0);

-- 工单列表 (C 菜单)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3001, 1, '工单列表', 3000, 'C', '/ticket', 'ticket:list', 'unordered-list', 1, 1, 1, 0, 0, NOW(), NOW(), 0);

-- 创建工单 (F 按钮权限)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3002, 1, '创建工单', 3001, 'F', '', 'ticket:create', '', 1, 1, 1, 0, 0, NOW(), NOW(), 0);

-- 工单详情 (C 菜单, 隐藏)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3003, 1, '工单详情', 3000, 'C', '/ticket/detail', 'ticket:detail', '', 0, 2, 1, 0, 0, NOW(), NOW(), 0);

-- 创建工单页 (C 菜单, 隐藏)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3008, 1, '创建工单页', 3000, 'C', '/ticket/create', 'ticket:create', '', 0, 5, 1, 0, 0, NOW(), NOW(), 0);

-- 待审批 (C 菜单)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3004, 1, '待审批', 3000, 'C', '/ticket/approval', 'ticket:approval', 'audit', 1, 3, 1, 0, 0, NOW(), NOW(), 0);

-- 分类管理 (C 菜单)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3005, 1, '分类管理', 3000, 'C', '/ticket/category', 'ticket:category', 'appstore', 1, 4, 1, 0, 0, NOW(), NOW(), 0);

-- 审批操作 (F 按钮权限)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3006, 1, '审批操作', 3004, 'F', '', 'ticket:approve', '', 1, 1, 1, 0, 0, NOW(), NOW(), 0);

-- 删除工单 (F 按钮权限)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3007, 1, '删除工单', 3001, 'F', '', 'ticket:delete', '', 1, 2, 1, 0, 0, NOW(), NOW(), 0);

-- 更新工单 (F 按钮权限)
INSERT INTO sys_application_resource (id, app_id, resource_name, parent_id, resource_type, path, perms, icon, visible, order_num, status, create_by, update_by, create_time, update_time, deleted)
VALUES (3009, 1, '更新工单', 3001, 'F', '', 'ticket:update', '', 1, 3, 1, 0, 0, NOW(), NOW(), 0);
