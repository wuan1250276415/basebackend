-- 创建租户配置表
CREATE TABLE IF NOT EXISTS sys_tenant_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID（唯一标识）',
    tenant_name VARCHAR(128) NOT NULL COMMENT '租户名称',
    isolation_mode VARCHAR(32) NOT NULL DEFAULT 'SHARED_DB' COMMENT '隔离模式：SHARED_DB-共享数据库, SEPARATE_DB-独立数据库, SEPARATE_SCHEMA-独立Schema',
    data_source_key VARCHAR(64) COMMENT '数据源键（独立数据库模式使用）',
    schema_name VARCHAR(64) COMMENT 'Schema名称（独立Schema模式使用）',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-激活, INACTIVE-停用',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(64) COMMENT '创建人',
    update_by VARCHAR(64) COMMENT '更新人',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id),
    KEY idx_status (status),
    KEY idx_isolation_mode (isolation_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户配置表';

-- 插入默认租户配置（用于测试）
INSERT INTO sys_tenant_config (tenant_id, tenant_name, isolation_mode, status, remark)
VALUES ('default', '默认租户', 'SHARED_DB', 'ACTIVE', '系统默认租户，用于测试和开发');
