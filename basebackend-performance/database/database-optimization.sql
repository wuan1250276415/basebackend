-- =====================================================================
-- 数据库性能优化脚本
-- 创建时间: 2025-11-15
-- 描述: 优化数据库表结构和索引以提升查询性能
-- =====================================================================

-- ============================================
-- 1. 用户服务优化
-- ============================================

USE basebackend;

-- 优化 sys_user 表索引
ALTER TABLE sys_user
ADD INDEX idx_username (username),
ADD INDEX idx_email (email),
ADD INDEX idx_phone (phone),
ADD INDEX idx_status (status),
ADD INDEX idx_dept_id (dept_id);

-- 添加复合索引优化常用查询
ALTER TABLE sys_user
ADD INDEX idx_status_dept (status, dept_id),
ADD INDEX idx_create_time (create_time);

-- ============================================
-- 2. 权限服务优化
-- ============================================

USE basebackend_auth;

-- 优化 sys_role 表索引
ALTER TABLE sys_role
ADD INDEX idx_role_name (role_name),
ADD INDEX idx_role_code (role_code),
ADD INDEX idx_status (status);

-- 优化 sys_permission 表索引
ALTER TABLE sys_permission
ADD INDEX idx_permission_code (permission_code),
ADD INDEX idx_parent_id (parent_id),
ADD INDEX idx_resource_type (resource_type),
ADD INDEX idx_status (status);

-- 优化关联表索引
ALTER TABLE sys_role_permission
ADD INDEX idx_role_id (role_id),
ADD INDEX idx_permission_id (permission_id);

ALTER TABLE sys_user_role
ADD INDEX idx_user_id (user_id),
ADD INDEX idx_role_id (role_id);

-- ============================================
-- 3. 部门服务优化
-- ============================================

USE basebackend_dept;

-- 优化 sys_dept 表索引
ALTER TABLE sys_dept
ADD INDEX idx_parent_id (parent_id),
ADD INDEX idx_dept_name (dept_name),
ADD INDEX idx_status (status),
ADD INDEX idx_order_num (order_num);

-- 优化层级查询
ALTER TABLE sys_dept
ADD INDEX idx_parent_status (parent_id, status),
ADD INDEX idx_dept_path (dept_path);

-- ============================================
-- 4. 字典服务优化
-- ============================================

USE basebackend_dict;

-- 优化 sys_dict_type 表索引
ALTER TABLE sys_dict_type
ADD INDEX idx_dict_name (dict_name),
ADD INDEX idx_dict_type (dict_type),
ADD INDEX idx_status (status);

-- 优化 sys_dict_data 表索引
ALTER TABLE sys_dict_data
ADD INDEX idx_dict_type (dict_type),
ADD INDEX idx_dict_label (dict_label),
ADD INDEX idx_dict_value (dict_value),
ADD INDEX idx_status (status);

-- 优化关联查询
ALTER TABLE sys_dict_data
ADD INDEX idx_type_status (dict_type, status),
ADD INDEX idx_order_num (order_num);

-- ============================================
-- 5. 通用优化
-- ============================================

-- 更新表统计信息
ANALYZE TABLE basebackend.sys_user;
ANALYZE TABLE basebackend_auth.sys_role;
ANALYZE TABLE basebackend_auth.sys_permission;
ANALYZE TABLE basebackend_dept.sys_dept;
ANALYZE TABLE basebackend_dict.sys_dict_type;
ANALYZE TABLE basebackend_dict.sys_dict_data;

-- 优化表缓存
ALTER TABLE sys_user
  ENGINE=InnoDB
  ROW_FORMAT=COMPRESSED
  KEY_BLOCK_SIZE=8;

-- ============================================
-- 6. 性能监控视图
-- ============================================

-- 创建慢查询监控视图
CREATE OR REPLACE VIEW v_slow_queries AS
SELECT
    digest_text,
    count_star,
    avg_timer_wait/1000000000000 AS avg_seconds,
    sum_timer_wait/1000000000000 AS sum_seconds,
    sum_rows_examined,
    sum_rows_sent
FROM performance_schema.events_statements_summary_by_digest
ORDER BY avg_timer_wait DESC
LIMIT 10;

-- 创建表使用统计视图
CREATE OR REPLACE VIEW v_table_stats AS
SELECT
    table_schema,
    table_name,
    table_rows,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb,
    engine
FROM information_schema.tables
WHERE table_schema IN ('basebackend', 'basebackend_auth', 'basebackend_dept', 'basebackend_dict')
ORDER BY (data_length + index_length) DESC;

-- ============================================
-- 7. 存储引擎优化
-- ============================================

-- 设置 InnoDB 参数优化
SET GLOBAL innodb_buffer_pool_size = 2147483648; -- 2GB
SET GLOBAL innodb_log_file_size = 536870912;     -- 512MB
SET GLOBAL innodb_flush_log_at_trx_commit = 2;
SET GLOBAL innodb_flush_method = 'O_DIRECT';
SET GLOBAL innodb_buffer_pool_instances = 8;

-- ============================================
-- 8. 查询优化建议
-- ============================================

-- 使用 EXPLAIN 分析查询
-- EXPLAIN SELECT * FROM sys_user WHERE username = 'admin';

-- 添加提示优化查询
-- SELECT /*+ MAX_EXECUTION_TIME(1000) */ * FROM sys_user;

-- ============================================
-- 9. 分区表优化 (可选)
-- ============================================

-- 按时间分区日志表
-- ALTER TABLE sys_log
-- PARTITION BY RANGE (YEAR(create_time)) (
--     PARTITION p2023 VALUES LESS THAN (2024),
--     PARTITION p2024 VALUES LESS THAN (2025),
--     PARTITION p2025 VALUES LESS THAN (2026),
--     PARTITION pmax VALUES LESS THAN MAXVALUE
-- );

-- ============================================
-- 10. 维护计划
-- ============================================

-- 每周执行
-- OPTIMIZE TABLE sys_user, sys_role, sys_permission;

-- 每月执行
-- ANALYZE TABLE sys_user, sys_role, sys_permission;

-- 检查表完整性
-- CHECK TABLE sys_user, sys_role, sys_permission;
