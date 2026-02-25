-- Create audit log archive table
CREATE TABLE IF NOT EXISTS sys_audit_log_archive (
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
    archive_time TIMESTAMP NOT NULL,
    original_log_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_archive_table_name ON sys_audit_log_archive (table_name);
CREATE INDEX idx_archive_operator_id ON sys_audit_log_archive (operator_id);
CREATE INDEX idx_archive_operate_time ON sys_audit_log_archive (operate_time);
CREATE INDEX idx_archive_archive_time ON sys_audit_log_archive (archive_time);
CREATE INDEX idx_archive_tenant_id ON sys_audit_log_archive (tenant_id);
CREATE INDEX idx_archive_original_log_id ON sys_audit_log_archive (original_log_id);

COMMENT ON TABLE sys_audit_log_archive IS '审计日志归档表';
COMMENT ON COLUMN sys_audit_log_archive.id IS '主键ID';
COMMENT ON COLUMN sys_audit_log_archive.operation_type IS '操作类型 (INSERT/UPDATE/DELETE)';
COMMENT ON COLUMN sys_audit_log_archive.table_name IS '表名';
COMMENT ON COLUMN sys_audit_log_archive.primary_key IS '主键值';
COMMENT ON COLUMN sys_audit_log_archive.before_data IS '变更前数据 (JSON)';
COMMENT ON COLUMN sys_audit_log_archive.after_data IS '变更后数据 (JSON)';
COMMENT ON COLUMN sys_audit_log_archive.changed_fields IS '变更字段';
COMMENT ON COLUMN sys_audit_log_archive.operator_id IS '操作人 ID';
COMMENT ON COLUMN sys_audit_log_archive.operator_name IS '操作人姓名';
COMMENT ON COLUMN sys_audit_log_archive.operator_ip IS '操作 IP';
COMMENT ON COLUMN sys_audit_log_archive.operate_time IS '操作时间';
COMMENT ON COLUMN sys_audit_log_archive.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_audit_log_archive.archive_time IS '归档时间';
COMMENT ON COLUMN sys_audit_log_archive.original_log_id IS '原始日志ID';
COMMENT ON COLUMN sys_audit_log_archive.create_time IS '创建时间';
COMMENT ON COLUMN sys_audit_log_archive.update_time IS '更新时间';
COMMENT ON COLUMN sys_audit_log_archive.create_by IS '创建人';
COMMENT ON COLUMN sys_audit_log_archive.update_by IS '更新人';
COMMENT ON COLUMN sys_audit_log_archive.deleted IS '逻辑删除标记（0：未删除，1：已删除）';
