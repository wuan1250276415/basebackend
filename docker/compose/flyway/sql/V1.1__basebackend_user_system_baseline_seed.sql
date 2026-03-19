INSERT INTO sys_dept (
    id, dept_name, parent_id, order_num, leader, phone, email, status, remark,
    create_by, update_by, deleted
) VALUES
    (1, '技术部', 0, 1, '管理员', '13800138000', 'admin@basebackend.com', 1,
     'user-api/system-api baseline 根部门', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    dept_name = VALUES(dept_name),
    leader = VALUES(leader),
    phone = VALUES(phone),
    email = VALUES(email),
    status = VALUES(status),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_role (
    id, parent_id, app_id, role_name, role_key, role_sort, data_scope, status,
    remark, create_by, update_by, deleted
) VALUES
    (1, 0, NULL, '管理员', 'admin', 1, 1, 1,
     'baseline 超级管理员角色', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    role_key = VALUES(role_key),
    role_sort = VALUES(role_sort),
    data_scope = VALUES(data_scope),
    status = VALUES(status),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_user (
    id, username, password, nickname, email, phone, avatar, gender, birthday,
    dept_id, user_type, status, login_ip, login_time, create_by, update_by,
    deleted
) VALUES
    (1, 'admin',
     '$2a$10$HiQoGGff0g7qe2BBPTviZePQVGzml77bsevGbODs6ZXmPOb.Lz9SG',
     '管理员', 'admin@basebackend.com', '13800138000', NULL, 0, NULL,
     1, 0, 1, NULL, NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = VALUES(nickname),
    email = VALUES(email),
    phone = VALUES(phone),
    dept_id = VALUES(dept_id),
    user_type = VALUES(user_type),
    status = VALUES(status),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_user_role (id, user_id, role_id, create_by)
VALUES (1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
    create_by = VALUES(create_by);

INSERT INTO sys_permission (
    id, permission_name, permission_key, api_path, http_method, permission_type,
    status, remark, create_by, update_by, deleted
) VALUES
    (3001, '超级权限', '*:*', '*', '*', 3, 1, 'baseline 通配权限', 1, 1, 0),
    (3002, '创建部门', 'system:dept:create', '/api/system/depts', 'POST', 3, 1, NULL, 1, 1, 0),
    (3003, '更新部门', 'system:dept:update', '/api/system/depts/*', 'PUT', 3, 1, NULL, 1, 1, 0),
    (3004, '删除部门', 'system:dept:delete', '/api/system/depts/*', 'DELETE', 3, 1, NULL, 1, 1, 0),
    (3005, '创建字典', 'system:dict:create', '/api/system/dicts', 'POST', 3, 1, NULL, 1, 1, 0),
    (3006, '更新字典', 'system:dict:update', '/api/system/dicts/*', 'PUT', 3, 1, NULL, 1, 1, 0),
    (3007, '删除字典', 'system:dict:delete', '/api/system/dicts/*', 'DELETE', 3, 1, NULL, 1, 1, 0),
    (3008, '创建应用', 'system:application:create', '/api/system/application', 'POST', 3, 1, NULL, 1, 1, 0),
    (3009, '更新应用', 'system:application:update', '/api/system/application', 'PUT', 3, 1, NULL, 1, 1, 0),
    (3010, '删除应用', 'system:application:delete', '/api/system/application/*', 'DELETE', 3, 1, NULL, 1, 1, 0),
    (3011, '删除日志', 'system:log:delete', '/api/system/logs/**', 'DELETE', 3, 1, NULL, 1, 1, 0),
    (3012, '清空日志', 'system:log:clean', '/api/system/logs/**', 'DELETE', 3, 1, NULL, 1, 1, 0),
    (3013, '更新资源', 'system:resource:update', '/api/system/application/resource/**', 'PUT', 3, 1, NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    api_path = VALUES(api_path),
    http_method = VALUES(http_method),
    permission_type = VALUES(permission_type),
    status = VALUES(status),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_role_permission (id, role_id, permission_id, create_by)
VALUES
    (3101, 1, 3001, 1),
    (3102, 1, 3002, 1),
    (3103, 1, 3003, 1),
    (3104, 1, 3004, 1),
    (3105, 1, 3005, 1),
    (3106, 1, 3006, 1),
    (3107, 1, 3007, 1),
    (3108, 1, 3008, 1),
    (3109, 1, 3009, 1),
    (3110, 1, 3010, 1),
    (3111, 1, 3011, 1),
    (3112, 1, 3012, 1),
    (3113, 1, 3013, 1)
ON DUPLICATE KEY UPDATE
    create_by = VALUES(create_by);

INSERT INTO sys_application (
    id, app_name, app_code, app_type, app_icon, app_url, status, order_num,
    remark, create_by, update_by, deleted
) VALUES
    (1, '管理后台', 'admin-web', 'web', 'setting', '/', 1, 1,
     'user-api/system-api baseline 默认应用', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    app_name = VALUES(app_name),
    app_type = VALUES(app_type),
    app_icon = VALUES(app_icon),
    app_url = VALUES(app_url),
    status = VALUES(status),
    order_num = VALUES(order_num),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_application_resource (
    id, app_id, resource_name, parent_id, resource_type, path, component, perms,
    icon, visible, open_type, order_num, status, remark, create_by, update_by,
    deleted
) VALUES
    (1001, 1, '首页', 0, 'C', 'dashboard', 'Dashboard/Overview', 'dashboard:view',
     'dashboard', 1, 'current', 1, 1, 'baseline 首页', 1, 1, 0),
    (1100, 1, '系统管理', 0, 'M', 'system', 'LAYOUT', NULL,
     'setting', 1, 'current', 10, 1, 'baseline 系统管理目录', 1, 1, 0),
    (1101, 1, '用户管理', 1100, 'C', 'user', 'User/List', 'system:user:list',
     'user', 1, 'current', 1, 1, NULL, 1, 1, 0),
    (1102, 1, '角色管理', 1100, 'C', 'role', 'System/RoleManagement', 'system:role:list',
     'team', 1, 'current', 2, 1, NULL, 1, 1, 0),
    (1103, 1, '部门管理', 1100, 'C', 'dept', 'System/DeptManagement', 'system:dept:list',
     'apartment', 1, 'current', 3, 1, NULL, 1, 1, 0),
    (1104, 1, '字典管理', 1100, 'C', 'dict', 'System/Dictionary', 'system:dict:list',
     'book', 1, 'current', 4, 1, NULL, 1, 1, 0),
    (1105, 1, '应用管理', 1100, 'C', 'application', 'System/Application', 'system:application:list',
     'appstore', 1, 'current', 5, 1, NULL, 1, 1, 0),
    (1106, 1, '资源管理', 1100, 'C', 'application-resource', 'System/ApplicationResource', 'system:resource:list',
     'bars', 1, 'current', 6, 1, NULL, 1, 1, 0),
    (1107, 1, '日志管理', 1100, 'C', 'logs', 'Monitor/OperationLog', 'system:log:list',
     'file-text', 1, 'current', 7, 1, NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    resource_name = VALUES(resource_name),
    parent_id = VALUES(parent_id),
    resource_type = VALUES(resource_type),
    path = VALUES(path),
    component = VALUES(component),
    perms = VALUES(perms),
    icon = VALUES(icon),
    visible = VALUES(visible),
    open_type = VALUES(open_type),
    order_num = VALUES(order_num),
    status = VALUES(status),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_role_resource (
    id, role_id, resource_id, create_by, update_by, deleted
) VALUES
    (2001, 1, 1001, 1, 1, 0),
    (2002, 1, 1100, 1, 1, 0),
    (2003, 1, 1101, 1, 1, 0),
    (2004, 1, 1102, 1, 1, 0),
    (2005, 1, 1103, 1, 1, 0),
    (2006, 1, 1104, 1, 1, 0),
    (2007, 1, 1105, 1, 1, 0),
    (2008, 1, 1106, 1, 1, 0),
    (2009, 1, 1107, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_menu (
    id, parent_id, menu_name, menu_key, order_num, path, component, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon, remark,
    create_by, update_by, deleted
) VALUES
    (4001, 0, '首页', 'dashboard:view', 1, 'dashboard', 'Dashboard/Overview', NULL,
     1, 0, 'C', 1, 1, 'dashboard:view', 'dashboard', '兼容旧菜单链', 1, 1, 0),
    (4100, 0, '系统管理', 'system', 10, 'system', 'LAYOUT', NULL,
     1, 0, 'M', 1, 1, NULL, 'setting', '兼容旧菜单链', 1, 1, 0),
    (4101, 4100, '用户管理', 'system:user', 1, 'user', 'User/List', NULL,
     1, 0, 'C', 1, 1, 'system:user:list', 'user', NULL, 1, 1, 0),
    (4102, 4100, '角色管理', 'system:role', 2, 'role', 'System/RoleManagement', NULL,
     1, 0, 'C', 1, 1, 'system:role:list', 'team', NULL, 1, 1, 0),
    (4103, 4100, '部门管理', 'system:dept', 3, 'dept', 'System/DeptManagement', NULL,
     1, 0, 'C', 1, 1, 'system:dept:list', 'apartment', NULL, 1, 1, 0),
    (4104, 4100, '字典管理', 'system:dict', 4, 'dict', 'System/Dictionary', NULL,
     1, 0, 'C', 1, 1, 'system:dict:list', 'book', NULL, 1, 1, 0),
    (4105, 4100, '应用管理', 'system:application', 5, 'application', 'System/Application', NULL,
     1, 0, 'C', 1, 1, 'system:application:list', 'appstore', NULL, 1, 1, 0),
    (4106, 4100, '资源管理', 'system:resource', 6, 'application-resource', 'System/ApplicationResource', NULL,
     1, 0, 'C', 1, 1, 'system:resource:list', 'bars', NULL, 1, 1, 0),
    (4107, 4100, '日志管理', 'system:log', 7, 'logs', 'Monitor/OperationLog', NULL,
     1, 0, 'C', 1, 1, 'system:log:list', 'file-text', NULL, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    parent_id = VALUES(parent_id),
    order_num = VALUES(order_num),
    path = VALUES(path),
    component = VALUES(component),
    query_param = VALUES(query_param),
    is_frame = VALUES(is_frame),
    is_cache = VALUES(is_cache),
    menu_type = VALUES(menu_type),
    visible = VALUES(visible),
    status = VALUES(status),
    perms = VALUES(perms),
    icon = VALUES(icon),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_role_menu (id, role_id, menu_id, create_by)
VALUES
    (5001, 1, 4001, 1),
    (5002, 1, 4100, 1),
    (5003, 1, 4101, 1),
    (5004, 1, 4102, 1),
    (5005, 1, 4103, 1),
    (5006, 1, 4104, 1),
    (5007, 1, 4105, 1),
    (5008, 1, 4106, 1),
    (5009, 1, 4107, 1)
ON DUPLICATE KEY UPDATE
    create_by = VALUES(create_by);

INSERT INTO sys_dict (
    id, dict_name, dict_type, status, remark, create_by, update_by, deleted
) VALUES
    (6001, '通用状态', 'sys_common_status', 1, 'baseline 通用状态字典', 1, 1, 0),
    (6002, '用户性别', 'sys_user_gender', 1, 'baseline 用户性别字典', 1, 1, 0),
    (6003, '用户类型', 'sys_user_type', 1, 'baseline 用户类型字典', 1, 1, 0),
    (6004, '用户类型兼容字典', 'user_type', 1, '兼容 system-api 测试中的 user_type', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    dict_name = VALUES(dict_name),
    status = VALUES(status),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);

INSERT INTO sys_dict_data (
    id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
    is_default, status, remark, create_by, update_by, deleted
) VALUES
    (6101, 1, '停用', '0', 'sys_common_status', NULL, 'default', 0, 1, NULL, 1, 1, 0),
    (6102, 2, '正常', '1', 'sys_common_status', NULL, 'success', 1, 1, NULL, 1, 1, 0),
    (6201, 1, '未知', '0', 'sys_user_gender', NULL, 'default', 1, 1, NULL, 1, 1, 0),
    (6202, 2, '男', '1', 'sys_user_gender', NULL, 'primary', 0, 1, NULL, 1, 1, 0),
    (6203, 3, '女', '2', 'sys_user_gender', NULL, 'warning', 0, 1, NULL, 1, 1, 0),
    (6301, 1, '系统用户', '0', 'sys_user_type', NULL, 'primary', 1, 1, NULL, 1, 1, 0),
    (6302, 2, '普通用户', '1', 'sys_user_type', NULL, 'default', 0, 1, NULL, 1, 1, 0),
    (6303, 3, '微信用户', '2', 'sys_user_type', NULL, 'success', 0, 1, NULL, 1, 1, 0),
    (6401, 1, '管理员', 'admin', 'user_type', NULL, 'primary', 1, 1, '兼容测试样例', 1, 1, 0),
    (6402, 2, '普通用户', 'user', 'user_type', NULL, 'default', 0, 1, '兼容测试样例', 1, 1, 0),
    (6403, 3, '微信用户', 'wechat', 'user_type', NULL, 'success', 0, 1, '兼容测试样例', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    dict_sort = VALUES(dict_sort),
    dict_label = VALUES(dict_label),
    css_class = VALUES(css_class),
    list_class = VALUES(list_class),
    is_default = VALUES(is_default),
    status = VALUES(status),
    remark = VALUES(remark),
    update_by = VALUES(update_by),
    deleted = VALUES(deleted);
