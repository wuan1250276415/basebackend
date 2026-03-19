CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(64) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
    order_num INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    leader VARCHAR(64) DEFAULT NULL COMMENT '负责人',
    phone VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_sys_dept_parent_id (parent_id),
    KEY idx_sys_dept_status (status),
    KEY idx_sys_dept_deleted (deleted),
    UNIQUE KEY uk_sys_dept_name_parent (dept_name, parent_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别:0-未知,1-男,2-女',
    birthday DATE DEFAULT NULL COMMENT '生日',
    dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
    user_type TINYINT NOT NULL DEFAULT 0 COMMENT '用户类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_email (email),
    UNIQUE KEY uk_sys_user_phone (phone),
    KEY idx_sys_user_dept_id (dept_id),
    KEY idx_sys_user_status (status),
    KEY idx_sys_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父角色ID',
    app_id BIGINT DEFAULT NULL COMMENT '所属应用ID, NULL表示系统角色',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(100) NOT NULL COMMENT '角色标识',
    role_sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    data_scope TINYINT NOT NULL DEFAULT 1 COMMENT '数据范围',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_role_role_key (role_key),
    UNIQUE KEY uk_sys_role_role_name_app (role_name, app_id, deleted),
    KEY idx_sys_role_app_id (app_id),
    KEY idx_sys_role_status (status),
    KEY idx_sys_role_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_user_id (user_id),
    KEY idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    permission_key VARCHAR(100) NOT NULL COMMENT '权限标识',
    api_path VARCHAR(255) DEFAULT NULL COMMENT 'API路径',
    http_method VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法',
    permission_type TINYINT NOT NULL DEFAULT 3 COMMENT '权限类型:1-菜单,2-按钮,3-API',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_permission_key (permission_key),
    KEY idx_sys_permission_type (permission_type),
    KEY idx_sys_permission_status (status),
    KEY idx_sys_permission_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
    KEY idx_sys_role_permission_role_id (role_id),
    KEY idx_sys_role_permission_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS sys_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_name VARCHAR(64) NOT NULL COMMENT '应用名称',
    app_code VARCHAR(64) NOT NULL COMMENT '应用编码',
    app_type VARCHAR(32) NOT NULL DEFAULT 'web' COMMENT '应用类型',
    app_icon VARCHAR(128) DEFAULT NULL COMMENT '应用图标',
    app_url VARCHAR(255) DEFAULT NULL COMMENT '应用地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    order_num INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_application_app_code (app_code),
    KEY idx_sys_application_status (status),
    KEY idx_sys_application_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

CREATE TABLE IF NOT EXISTS sys_application_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id BIGINT NOT NULL COMMENT '所属应用ID',
    resource_name VARCHAR(64) NOT NULL COMMENT '资源名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父资源ID',
    resource_type CHAR(1) NOT NULL COMMENT '资源类型:M-目录,C-菜单,F-按钮',
    path VARCHAR(255) DEFAULT NULL COMMENT '路由地址',
    component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    perms VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    icon VARCHAR(128) DEFAULT NULL COMMENT '图标',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示:0-隐藏,1-显示',
    open_type VARCHAR(32) NOT NULL DEFAULT 'current' COMMENT '打开方式',
    order_num INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_sys_app_resource_app_id (app_id),
    KEY idx_sys_app_resource_parent_id (parent_id),
    KEY idx_sys_app_resource_status (status),
    KEY idx_sys_app_resource_deleted (deleted),
    KEY idx_sys_app_resource_perms (perms),
    UNIQUE KEY uk_sys_app_resource_name (app_id, parent_id, resource_name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用资源表';

CREATE TABLE IF NOT EXISTS sys_role_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_role_resource (role_id, resource_id),
    KEY idx_sys_role_resource_role_id (role_id),
    KEY idx_sys_role_resource_resource_id (resource_id),
    KEY idx_sys_role_resource_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色资源关联表';

CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_name VARCHAR(64) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_dict_type (dict_type),
    KEY idx_sys_dict_status (status),
    KEY idx_sys_dict_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    dict_label VARCHAR(64) NOT NULL COMMENT '字典标签',
    dict_value VARCHAR(100) NOT NULL COMMENT '字典值',
    dict_type VARCHAR(100) NOT NULL COMMENT '字典类型',
    css_class VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
    list_class VARCHAR(100) DEFAULT NULL COMMENT '表格回显样式',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认:0-否,1-是',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_dict_data_type_value (dict_type, dict_value),
    KEY idx_sys_dict_data_type (dict_type),
    KEY idx_sys_dict_data_status (status),
    KEY idx_sys_dict_data_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_key VARCHAR(100) NOT NULL COMMENT '菜单标识',
    order_num INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    path VARCHAR(255) DEFAULT NULL COMMENT '路由地址',
    component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
    query_param VARCHAR(255) DEFAULT NULL COMMENT '路由参数',
    is_frame TINYINT NOT NULL DEFAULT 1 COMMENT '是否内链:0-是,1-否',
    is_cache TINYINT NOT NULL DEFAULT 0 COMMENT '是否缓存:0-缓存,1-不缓存',
    menu_type CHAR(1) NOT NULL COMMENT '菜单类型:M-目录,C-菜单,F-按钮',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示:0-隐藏,1-显示',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    perms VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    icon VARCHAR(128) DEFAULT NULL COMMENT '图标',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    update_by BIGINT DEFAULT NULL,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_sys_menu_key (menu_key),
    KEY idx_sys_menu_parent_id (parent_id),
    KEY idx_sys_menu_status (status),
    KEY idx_sys_menu_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兼容菜单表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT NULL,
    UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
    KEY idx_sys_role_menu_role_id (role_id),
    KEY idx_sys_role_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    login_location VARCHAR(255) DEFAULT NULL COMMENT '登录地点',
    browser VARCHAR(128) DEFAULT NULL COMMENT '浏览器',
    os VARCHAR(128) DEFAULT NULL COMMENT '操作系统',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '登录状态:0-失败,1-成功',
    msg VARCHAR(500) DEFAULT NULL COMMENT '提示消息',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_sys_login_log_username (username),
    KEY idx_sys_login_log_status (status),
    KEY idx_sys_login_log_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    operation VARCHAR(255) DEFAULT NULL COMMENT '操作名称',
    method VARCHAR(255) DEFAULT NULL COMMENT '请求方法',
    params LONGTEXT DEFAULT NULL COMMENT '请求参数',
    time BIGINT DEFAULT NULL COMMENT '执行时长(毫秒)',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    location VARCHAR(255) DEFAULT NULL COMMENT '操作地点',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态:0-失败,1-成功',
    error_msg LONGTEXT DEFAULT NULL COMMENT '错误信息',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_sys_operation_log_username (username),
    KEY idx_sys_operation_log_status (status),
    KEY idx_sys_operation_log_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
