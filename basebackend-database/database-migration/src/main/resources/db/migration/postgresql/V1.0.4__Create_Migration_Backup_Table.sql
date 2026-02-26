-- 创建迁移备份表
-- 用于在数据迁移前备份数据

CREATE TABLE IF NOT EXISTS sys_migration_backup (
    id BIGSERIAL PRIMARY KEY,
    migration_version VARCHAR(50) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    backup_data TEXT,
    backup_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

CREATE INDEX idx_migration_backup_version ON sys_migration_backup (migration_version);
CREATE INDEX idx_migration_backup_table_name ON sys_migration_backup (table_name);
CREATE INDEX idx_migration_backup_time ON sys_migration_backup (backup_time);

COMMENT ON TABLE sys_migration_backup IS '迁移备份表';
COMMENT ON COLUMN sys_migration_backup.id IS '主键ID';
COMMENT ON COLUMN sys_migration_backup.migration_version IS '迁移版本';
COMMENT ON COLUMN sys_migration_backup.table_name IS '表名';
COMMENT ON COLUMN sys_migration_backup.backup_data IS '备份数据(JSON格式)';
COMMENT ON COLUMN sys_migration_backup.backup_time IS '备份时间';
COMMENT ON COLUMN sys_migration_backup.created_by IS '创建人';
