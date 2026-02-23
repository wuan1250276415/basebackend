-- 创建迁移备份表
-- 用于在数据迁移前备份数据

CREATE TABLE IF NOT EXISTS sys_migration_backup (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    migration_version VARCHAR(50) NOT NULL COMMENT '迁移版本',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    backup_data LONGTEXT COMMENT '备份数据(JSON格式)',
    backup_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
    created_by VARCHAR(50) COMMENT '创建人',
    INDEX idx_migration_version (migration_version),
    INDEX idx_table_name (table_name),
    INDEX idx_backup_time (backup_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迁移备份表';
