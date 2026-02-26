-- Create audit log table
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT NOT NULL PRIMARY KEY,
    operation_type VARCHAR(20) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    primary_key VARCHAR(100),
    before_data TEXT,
    after_data TEXT,
    changed_fields VARCHAR(500),
    operator_id BIGINT,
    operator_name VARCHAR(100),
    operator_ip VARCHAR(50),
    operate_time TIMESTAMP NOT NULL,
    tenant_id VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_audit_log_table_name ON sys_audit_log (table_name);
CREATE INDEX idx_audit_log_operator_id ON sys_audit_log (operator_id);
CREATE INDEX idx_audit_log_operate_time ON sys_audit_log (operate_time);
CREATE INDEX idx_audit_log_tenant_id ON sys_audit_log (tenant_id);

COMMENT ON TABLE sys_audit_log IS '审计日志表';
COMMENT ON COLUMN sys_audit_log.id IS '主键ID';
COMMENT ON COLUMN sys_audit_log.operation_type IS '操作类型 (INSERT/UPDATE/DELETE)';
COMMENT ON COLUMN sys_audit_log.table_name IS '表名';
COMMENT ON COLUMN sys_audit_log.primary_key IS '主键值';
COMMENT ON COLUMN sys_audit_log.before_data IS '变更前数据 (JSON)';
COMMENT ON COLUMN sys_audit_log.after_data IS '变更后数据 (JSON)';
COMMENT ON COLUMN sys_audit_log.changed_fields IS '变更字段';
COMMENT ON COLUMN sys_audit_log.operator_id IS '操作人 ID';
COMMENT ON COLUMN sys_audit_log.operator_name IS '操作人姓名';
COMMENT ON COLUMN sys_audit_log.operator_ip IS '操作 IP';
COMMENT ON COLUMN sys_audit_log.operate_time IS '操作时间';
COMMENT ON COLUMN sys_audit_log.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_audit_log.create_time IS '创建时间';
COMMENT ON COLUMN sys_audit_log.update_time IS '更新时间';
COMMENT ON COLUMN sys_audit_log.create_by IS '创建人';
COMMENT ON COLUMN sys_audit_log.update_by IS '更新人';
COMMENT ON COLUMN sys_audit_log.deleted IS '逻辑删除标记（0：未删除，1：已删除）';
