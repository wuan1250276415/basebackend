-- 测试数据库Schema
-- 基于MyBatis-Plus的BaseEntity

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    permission_key VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识',
    api_path VARCHAR(200) COMMENT 'API路径',
    http_method VARCHAR(10) COMMENT 'HTTP方法',
    permission_type INT NOT NULL DEFAULT 1 COMMENT '权限类型：1-菜单权限，2-按钮权限，3-API权限',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted INT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除'
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time TIMESTAMP COMMENT '创建时间',
    create_by BIGINT COMMENT '创建人',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_permission_type ON sys_permission(permission_type);
CREATE INDEX IF NOT EXISTS idx_permission_status ON sys_permission(status);
CREATE INDEX IF NOT EXISTS idx_role_permission_role ON sys_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission ON sys_role_permission(permission_id);
