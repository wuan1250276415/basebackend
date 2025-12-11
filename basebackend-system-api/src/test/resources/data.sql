-- 测试基础数据

-- 插入权限测试数据
INSERT INTO sys_permission (id, permission_name, permission_key, api_path, http_method, permission_type, status, remark, create_time, update_time, create_by, update_by, deleted)
VALUES
    (1, '系统管理', 'system:manage', '/api/system', 'GET', 1, 1, '系统管理菜单权限', NOW(), NOW(), 1, 1, 0),
    (2, '权限查询', 'permission:query', '/api/permissions', 'GET', 3, 1, '权限查询API权限', NOW(), NOW(), 1, 1, 0),
    (3, '权限新增', 'permission:create', '/api/permissions', 'POST', 2, 1, '权限新增按钮权限', NOW(), NOW(), 1, 1, 0),
    (4, '权限修改', 'permission:update', '/api/permissions', 'PUT', 2, 1, '权限修改按钮权限', NOW(), NOW(), 1, 1, 0),
    (5, '权限删除', 'permission:delete', '/api/permissions', 'DELETE', 2, 0, '权限删除按钮权限（已禁用）', NOW(), NOW(), 1, 1, 0);

-- 插入角色权限关联数据
INSERT INTO sys_role_permission (id, role_id, permission_id, create_time, create_by)
VALUES
    (1, 1, 1, NOW(), 1), -- 管理员角色拥有系统管理权限
    (2, 1, 2, NOW(), 1), -- 管理员角色拥有权限查询权限
    (3, 1, 3, NOW(), 1), -- 管理员角色拥有权限新增权限
    (4, 1, 4, NOW(), 1); -- 管理员角色拥有权限修改权限
