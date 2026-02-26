-- Create SQL statistics table
CREATE TABLE IF NOT EXISTS sys_sql_statistics (
    id BIGINT NOT NULL PRIMARY KEY,
    sql_md5 VARCHAR(32) NOT NULL,
    sql_template TEXT NOT NULL,
    execute_count BIGINT NOT NULL DEFAULT 0,
    total_time BIGINT NOT NULL DEFAULT 0,
    avg_time BIGINT NOT NULL DEFAULT 0,
    max_time BIGINT NOT NULL DEFAULT 0,
    min_time BIGINT NOT NULL DEFAULT 0,
    fail_count BIGINT NOT NULL DEFAULT 0,
    last_execute_time TIMESTAMP,
    data_source_name VARCHAR(100),
    tenant_id VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_sql_stats_md5 ON sys_sql_statistics (sql_md5);
CREATE INDEX idx_sql_stats_execute_count ON sys_sql_statistics (execute_count);
CREATE INDEX idx_sql_stats_avg_time ON sys_sql_statistics (avg_time);
CREATE INDEX idx_sql_stats_fail_count ON sys_sql_statistics (fail_count);
CREATE INDEX idx_sql_stats_last_execute_time ON sys_sql_statistics (last_execute_time);
CREATE INDEX idx_sql_stats_data_source ON sys_sql_statistics (data_source_name);
CREATE INDEX idx_sql_stats_tenant ON sys_sql_statistics (tenant_id);

COMMENT ON TABLE sys_sql_statistics IS 'SQL统计表';
COMMENT ON COLUMN sys_sql_statistics.id IS '主键ID';
COMMENT ON COLUMN sys_sql_statistics.sql_md5 IS 'SQL语句的MD5值';
COMMENT ON COLUMN sys_sql_statistics.sql_template IS 'SQL模板（参数化后的SQL语句）';
COMMENT ON COLUMN sys_sql_statistics.execute_count IS '执行次数';
COMMENT ON COLUMN sys_sql_statistics.total_time IS '总执行时间（毫秒）';
COMMENT ON COLUMN sys_sql_statistics.avg_time IS '平均执行时间（毫秒）';
COMMENT ON COLUMN sys_sql_statistics.max_time IS '最大执行时间（毫秒）';
COMMENT ON COLUMN sys_sql_statistics.min_time IS '最小执行时间（毫秒）';
COMMENT ON COLUMN sys_sql_statistics.fail_count IS '失败次数';
COMMENT ON COLUMN sys_sql_statistics.last_execute_time IS '最后执行时间';
COMMENT ON COLUMN sys_sql_statistics.data_source_name IS '数据源名称';
COMMENT ON COLUMN sys_sql_statistics.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_sql_statistics.create_time IS '创建时间';
COMMENT ON COLUMN sys_sql_statistics.update_time IS '更新时间';
COMMENT ON COLUMN sys_sql_statistics.create_by IS '创建人';
COMMENT ON COLUMN sys_sql_statistics.update_by IS '更新人';
COMMENT ON COLUMN sys_sql_statistics.deleted IS '逻辑删除标记（0：未删除，1：已删除）';
