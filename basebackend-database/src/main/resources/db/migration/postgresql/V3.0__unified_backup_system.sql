-- ==================================================
-- 统一备份系统数据库迁移脚本 V3.0
-- 创建时间: 2025-11-26
-- 描述: 创建统一的备份任务、备份历史和恢复记录表
-- ==================================================

-- 备份任务表
CREATE TABLE backup_task (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    datasource_type VARCHAR(32) NOT NULL,
    backup_type VARCHAR(16) NOT NULL,
    schedule_cron VARCHAR(64),
    strategy_json TEXT,
    retention_policy_json TEXT,
    storage_policy_json TEXT,
    notify_policy_json TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_backup_task_datasource_type ON backup_task (datasource_type);
CREATE INDEX idx_backup_task_enabled ON backup_task (enabled);
CREATE INDEX idx_backup_task_schedule ON backup_task (schedule_cron, enabled);

COMMENT ON TABLE backup_task IS '备份任务表';
COMMENT ON COLUMN backup_task.id IS '任务ID';
COMMENT ON COLUMN backup_task.name IS '任务名称';
COMMENT ON COLUMN backup_task.datasource_type IS '数据源类型：mysql/postgres/redis';
COMMENT ON COLUMN backup_task.backup_type IS '备份类型：full/incremental';
COMMENT ON COLUMN backup_task.schedule_cron IS 'CRON表达式';
COMMENT ON COLUMN backup_task.strategy_json IS '策略配置JSON';
COMMENT ON COLUMN backup_task.retention_policy_json IS '保留策略JSON';
COMMENT ON COLUMN backup_task.storage_policy_json IS '存储策略JSON';
COMMENT ON COLUMN backup_task.notify_policy_json IS '通知策略JSON';
COMMENT ON COLUMN backup_task.enabled IS '是否启用';
COMMENT ON COLUMN backup_task.created_at IS '创建时间';
COMMENT ON COLUMN backup_task.updated_at IS '更新时间';

-- 备份历史表
CREATE TABLE backup_history (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    task_name VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    backup_type VARCHAR(16) NOT NULL,
    base_full_id BIGINT,
    binlog_start VARCHAR(128),
    binlog_end VARCHAR(128),
    wal_start VARCHAR(128),
    wal_end VARCHAR(128),
    file_size BIGINT,
    storage_locations TEXT,
    checksum_md5 VARCHAR(64),
    checksum_sha256 VARCHAR(128),
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    duration_seconds INTEGER,
    error_message TEXT,
    started_at_ms BIGINT,
    finished_at_ms BIGINT,
    FOREIGN KEY (task_id) REFERENCES backup_task (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_backup_history_task_time ON backup_history (task_id, started_at);
CREATE INDEX idx_backup_history_status ON backup_history (status);
CREATE INDEX idx_backup_history_type ON backup_history (backup_type);
CREATE INDEX idx_backup_history_base_full ON backup_history (base_full_id);

COMMENT ON TABLE backup_history IS '备份历史表';
COMMENT ON COLUMN backup_history.id IS '记录ID';
COMMENT ON COLUMN backup_history.task_id IS '关联任务ID';
COMMENT ON COLUMN backup_history.task_name IS '任务名称';
COMMENT ON COLUMN backup_history.status IS '备份状态：SUCCESS/FAILED/RUNNING';
COMMENT ON COLUMN backup_history.backup_type IS '备份类型：full/incremental';
COMMENT ON COLUMN backup_history.base_full_id IS '增量链基线全量备份ID';
COMMENT ON COLUMN backup_history.binlog_start IS 'MySQL binlog起始位置';
COMMENT ON COLUMN backup_history.binlog_end IS 'MySQL binlog结束位置';
COMMENT ON COLUMN backup_history.wal_start IS 'PostgreSQL WAL起始位置';
COMMENT ON COLUMN backup_history.wal_end IS 'PostgreSQL WAL结束位置';
COMMENT ON COLUMN backup_history.file_size IS '备份文件大小（字节）';
COMMENT ON COLUMN backup_history.storage_locations IS '存储位置列表（JSON格式）';
COMMENT ON COLUMN backup_history.checksum_md5 IS 'MD5校验和';
COMMENT ON COLUMN backup_history.checksum_sha256 IS 'SHA256校验和';
COMMENT ON COLUMN backup_history.started_at IS '备份开始时间';
COMMENT ON COLUMN backup_history.finished_at IS '备份结束时间';
COMMENT ON COLUMN backup_history.duration_seconds IS '备份耗时（秒）';
COMMENT ON COLUMN backup_history.error_message IS '错误信息';
COMMENT ON COLUMN backup_history.started_at_ms IS '备份开始时间戳（毫秒）';
COMMENT ON COLUMN backup_history.finished_at_ms IS '备份结束时间戳（毫秒）';

-- 恢复记录表
CREATE TABLE restore_record (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    history_id BIGINT,
    target_point VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    duration_seconds INTEGER,
    error_message TEXT,
    operator VARCHAR(64),
    remark TEXT,
    started_at_ms BIGINT,
    finished_at_ms BIGINT,
    FOREIGN KEY (task_id) REFERENCES backup_task (id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (history_id) REFERENCES backup_history (id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX idx_restore_record_task_time ON restore_record (task_id, started_at);
CREATE INDEX idx_restore_record_history ON restore_record (history_id);
CREATE INDEX idx_restore_record_status ON restore_record (status);
CREATE INDEX idx_restore_record_target_point ON restore_record (target_point);

COMMENT ON TABLE restore_record IS '恢复记录表';
COMMENT ON COLUMN restore_record.id IS '记录ID';
COMMENT ON COLUMN restore_record.task_id IS '关联任务ID';
COMMENT ON COLUMN restore_record.history_id IS '关联备份历史ID';
COMMENT ON COLUMN restore_record.target_point IS 'PITR目标时间点';
COMMENT ON COLUMN restore_record.status IS '恢复状态：SUCCESS/FAILED/RUNNING';
COMMENT ON COLUMN restore_record.started_at IS '恢复开始时间';
COMMENT ON COLUMN restore_record.finished_at IS '恢复结束时间';
COMMENT ON COLUMN restore_record.duration_seconds IS '恢复耗时（秒）';
COMMENT ON COLUMN restore_record.error_message IS '错误信息';
COMMENT ON COLUMN restore_record.operator IS '恢复操作者';
COMMENT ON COLUMN restore_record.remark IS '恢复备注';
COMMENT ON COLUMN restore_record.started_at_ms IS '恢复开始时间戳（毫秒）';
COMMENT ON COLUMN restore_record.finished_at_ms IS '恢复结束时间戳（毫秒）';

-- ==================================================
-- 插入示例数据（可选）
-- ==================================================

-- 示例：MySQL全量备份任务
INSERT INTO backup_task (
    name, datasource_type, backup_type, schedule_cron,
    strategy_json, retention_policy_json, storage_policy_json, notify_policy_json, enabled
) VALUES (
    'MySQL全量备份任务',
    'mysql',
    'full',
    '0 0 2 * * ?',
    '{"host":"localhost","port":3306,"username":"root","password":"","database":"test","tables":[],"excludeTables":[]}',
    '{"retentionDays":30,"tier1":7,"tier2":30,"tier3":90}',
    '{"replicas":[{"type":"local","enabled":true,"priority":1},{"type":"s3","enabled":true,"priority":2}]}',
    '{"onFailure":true,"onSuccess":false,"email":"admin@example.com"}',
    TRUE
);

-- 示例：MySQL增量备份任务
INSERT INTO backup_task (
    name, datasource_type, backup_type, schedule_cron,
    strategy_json, retention_policy_json, storage_policy_json, notify_policy_json, enabled
) VALUES (
    'MySQL增量备份任务',
    'mysql',
    'incremental',
    '0 0 * * * ?',
    '{"host":"localhost","port":3306,"username":"root","password":"","database":"test","binlogDir":"/var/lib/mysql"}',
    '{"retentionDays":7,"tier1":3,"tier2":7}',
    '{"replicas":[{"type":"local","enabled":true,"priority":1}]}',
    '{"onFailure":true,"email":"admin@example.com"}',
    TRUE
);

-- ==================================================
-- 创建视图（便于查询）
-- ==================================================

-- 备份任务统计视图
CREATE VIEW v_backup_task_stats AS
SELECT
    bt.id,
    bt.name,
    bt.datasource_type,
    bt.backup_type,
    bt.enabled,
    COUNT(bh.id) AS total_backups,
    COUNT(CASE WHEN bh.status = 'SUCCESS' THEN 1 END) AS success_backups,
    COUNT(CASE WHEN bh.status = 'FAILED' THEN 1 END) AS failed_backups,
    MAX(bh.started_at) AS last_backup_time,
    SUM(CASE WHEN bh.status = 'SUCCESS' THEN bh.file_size ELSE 0 END) AS total_size
FROM backup_task bt
LEFT JOIN backup_history bh ON bt.id = bh.task_id
GROUP BY bt.id, bt.name, bt.datasource_type, bt.backup_type, bt.enabled;

-- 最近的备份记录视图
CREATE VIEW v_recent_backups AS
SELECT
    bh.*,
    bt.datasource_type,
    bt.name AS task_name_from_task
FROM backup_history bh
INNER JOIN backup_task bt ON bh.task_id = bt.id
WHERE bh.started_at >= NOW() - INTERVAL '7 days'
ORDER BY bh.started_at DESC;

-- ==================================================
-- 清理过期备份的函数（替代MySQL存储过程）
-- ==================================================

CREATE OR REPLACE FUNCTION sp_cleanup_old_backups(p_task_id BIGINT, p_retention_days INTEGER)
RETURNS VOID AS $$
BEGIN
    DELETE FROM backup_history
    WHERE task_id = p_task_id
      AND status = 'SUCCESS'
      AND started_at < NOW() - (p_retention_days || ' days')::INTERVAL;
END;
$$ LANGUAGE plpgsql;

-- ==================================================
-- 结束
-- ==================================================
