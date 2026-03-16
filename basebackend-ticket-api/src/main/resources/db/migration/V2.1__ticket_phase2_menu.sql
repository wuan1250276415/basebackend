-- Phase 2: 工单系统菜单扩展（Dashboard/Kanban/Search）
-- 注意: 实际菜单ID需根据现有最大ID调整

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, type, sort_order, visible, status, perms, tenant_id, deleted, create_time, update_time)
VALUES
-- 工单仪表盘
(3010, 3000, '工单仪表盘', 'dashboard', 'ticket/dashboard/index', 'DashboardOutlined', 1, 1, 1, 1, 'ticket:statistics:query', 0, 0, NOW(), NOW()),
-- 工单看板
(3011, 3000, '工单看板', 'kanban', 'ticket/kanban/index', 'AppstoreOutlined', 1, 2, 1, 1, 'ticket:list', 0, 0, NOW(), NOW()),
-- 工单搜索
(3012, 3000, '工单搜索', 'search', 'ticket/search/index', 'SearchOutlined', 1, 3, 1, 1, 'ticket:list', 0, 0, NOW(), NOW());

-- 工单AI权限
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, type, sort_order, visible, status, perms, tenant_id, deleted, create_time, update_time)
VALUES
(3020, 3000, 'AI智能', '', '', '', 2, 10, 0, 1, 'ticket:ai', 0, 0, NOW(), NOW()),
(3021, 3000, '工单导出', '', '', '', 2, 11, 0, 1, 'ticket:export', 0, 0, NOW(), NOW()),
(3022, 3000, '工单管理', '', '', '', 2, 12, 0, 1, 'ticket:admin', 0, 0, NOW(), NOW());
