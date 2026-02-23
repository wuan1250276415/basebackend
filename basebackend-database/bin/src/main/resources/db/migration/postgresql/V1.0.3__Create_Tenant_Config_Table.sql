-- 创建租户配置表
CREATE TABLE IF NOT EXISTS sys_tenant_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(128) NOT NULL,
    isolation_mode VARCHAR(32) NOT NULL DEFAULT 'SHARED_DB',
    data_source_key VARCHAR(64),
    schema_name VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(512),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    update_by VARCHAR(64),
    deleted SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id)
);

CREATE INDEX idx_tenant_config_status ON sys_tenant_config (status);
CREATE INDEX idx_tenant_config_isolation_mode ON sys_tenant_config (isolation_mode);

COMMENT ON TABLE sys_tenant_config IS '租户配置表';
COMMENT ON COLUMN sys_tenant_config.id IS '主键ID';
COMMENT ON COLUMN sys_tenant_config.tenant_id IS '租户ID（唯一标识）';
COMMENT ON COLUMN sys_tenant_config.tenant_name IS '租户名称';
COMMENT ON COLUMN sys_tenant_config.isolation_mode IS '隔离模式：SHARED_DB-共享数据库, SEPARATE_DB-独立数据库, SEPARATE_SCHEMA-独立Schema';
COMMENT ON COLUMN sys_tenant_config.data_source_key IS '数据源键（独立数据库模式使用）';
COMMENT ON COLUMN sys_tenant_config.schema_name IS 'Schema名称（独立Schema模式使用）';
COMMENT ON COLUMN sys_tenant_config.status IS '状态：ACTIVE-激活, INACTIVE-停用';
COMMENT ON COLUMN sys_tenant_config.remark IS '备注';
COMMENT ON COLUMN sys_tenant_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_tenant_config.update_time IS '更新时间';
COMMENT ON COLUMN sys_tenant_config.create_by IS '创建人';
COMMENT ON COLUMN sys_tenant_config.update_by IS '更新人';
COMMENT ON COLUMN sys_tenant_config.deleted IS '删除标志：0-未删除, 1-已删除';

-- 插入默认租户配置（用于测试）
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, status, remark)
VALUES ('default', '默认租户', 'SHARED_DB', 'ACTIVE', '系统默认租户，用于测试和开发');
