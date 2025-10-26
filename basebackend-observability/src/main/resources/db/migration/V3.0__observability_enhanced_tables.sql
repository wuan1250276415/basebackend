-- =============================================
-- 可观测性增强模块数据库表
-- Version: 3.0
-- Description: 追踪、日志、性能分析、调试工具
-- =============================================

-- 1. 追踪 Span 扩展表
CREATE TABLE IF NOT EXISTS trace_span_ext (
    span_id VARCHAR(32) PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(32),
    service_name VARCHAR(100) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    start_time BIGINT NOT NULL,
    duration BIGINT NOT NULL,
    tags JSON,
    logs JSON,
    status VARCHAR(20) DEFAULT 'OK',
    error_message TEXT,
    stack_trace TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_service (service_name),
    INDEX idx_start_time (start_time),
    INDEX idx_duration (duration),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='追踪Span扩展表';

-- 2. 慢请求记录表
CREATE TABLE IF NOT EXISTS slow_trace_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) UNIQUE NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    duration BIGINT NOT NULL,
    threshold BIGINT NOT NULL,
    bottleneck_type VARCHAR(50),
    bottleneck_spans JSON,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_service_time (service_name, create_time),
    INDEX idx_duration (duration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='慢请求记录表';

-- 3. 服务调用依赖表
CREATE TABLE IF NOT EXISTS service_dependency (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_service VARCHAR(100) NOT NULL,
    to_service VARCHAR(100) NOT NULL,
    call_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    total_duration BIGINT DEFAULT 0,
    time_bucket TIMESTAMP NOT NULL,
    UNIQUE KEY uk_service_time (from_service, to_service, time_bucket),
    INDEX idx_from_service (from_service),
    INDEX idx_to_service (to_service)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务调用依赖表';

-- 4. 日志统计表
CREATE TABLE IF NOT EXISTS log_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_name VARCHAR(100) NOT NULL,
    log_level VARCHAR(20) NOT NULL,
    log_count BIGINT DEFAULT 0,
    time_bucket TIMESTAMP NOT NULL,
    UNIQUE KEY uk_service_level_time (service_name, log_level, time_bucket),
    INDEX idx_service (service_name),
    INDEX idx_time_bucket (time_bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志统计表';

-- 5. 异常聚合表
CREATE TABLE IF NOT EXISTS exception_aggregation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exception_class VARCHAR(255) NOT NULL,
    exception_message TEXT,
    stack_trace_hash VARCHAR(64) NOT NULL,
    occurrence_count BIGINT DEFAULT 1,
    first_seen TIMESTAMP NOT NULL,
    last_seen TIMESTAMP NOT NULL,
    sample_log_id VARCHAR(64),
    service_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'OPEN',
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    UNIQUE KEY uk_hash (stack_trace_hash),
    INDEX idx_exception_class (exception_class),
    INDEX idx_last_seen (last_seen),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常聚合表';

-- 6. JVM 性能指标表
CREATE TABLE IF NOT EXISTS jvm_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    heap_used BIGINT,
    heap_max BIGINT,
    heap_committed BIGINT,
    non_heap_used BIGINT,
    thread_count INT,
    daemon_thread_count INT,
    peak_thread_count INT,
    gc_count INT,
    gc_time BIGINT,
    cpu_usage DOUBLE,
    load_average DOUBLE,
    INDEX idx_instance_time (instance_id, timestamp),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JVM性能指标表';

-- 7. 慢SQL记录表
CREATE TABLE IF NOT EXISTS slow_sql_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    method_name VARCHAR(255) NOT NULL,
    sql_statement TEXT,
    duration BIGINT NOT NULL,
    parameters TEXT,
    trace_id VARCHAR(64),
    service_name VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_method (method_name),
    INDEX idx_duration (duration),
    INDEX idx_service (service_name),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='慢SQL记录表';

-- 8. 性能剖析会话表
CREATE TABLE IF NOT EXISTS profiling_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    instance_id VARCHAR(100) NOT NULL,
    profiling_type VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration BIGINT,
    flame_graph_path VARCHAR(500),
    hot_methods JSON,
    status VARCHAR(20) DEFAULT 'RUNNING',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_instance (instance_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='性能剖析会话表';

-- 9. 断点配置表
CREATE TABLE IF NOT EXISTS breakpoint_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    breakpoint_id VARCHAR(64) UNIQUE NOT NULL,
    class_name VARCHAR(255) NOT NULL,
    method_name VARCHAR(100) NOT NULL,
    condition_expr TEXT,
    max_hits INT DEFAULT 100,
    enabled BOOLEAN DEFAULT TRUE,
    hit_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_class_method (class_name, method_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='断点配置表';

-- 10. 热部署历史表
CREATE TABLE IF NOT EXISTS hot_deploy_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(255) NOT NULL,
    deploy_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL,
    message TEXT,
    user_id BIGINT,
    instance_id VARCHAR(100),
    INDEX idx_class (class_name),
    INDEX idx_deploy_time (deploy_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热部署历史表';

-- 11. 追踪统计汇总表
CREATE TABLE IF NOT EXISTS trace_service_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_name VARCHAR(100) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    time_bucket TIMESTAMP NOT NULL,
    call_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    p50_duration DOUBLE,
    p95_duration DOUBLE,
    p99_duration DOUBLE,
    max_duration BIGINT,
    UNIQUE KEY uk_service_op_time (service_name, operation_name, time_bucket),
    INDEX idx_service_time (service_name, time_bucket)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='追踪统计汇总表';

-- 12. 告警规则配置表
CREATE TABLE IF NOT EXISTS alert_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(100) UNIQUE NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100),
    threshold_value DOUBLE,
    comparison_operator VARCHAR(20),
    duration_seconds INT,
    severity VARCHAR(20) DEFAULT 'WARNING',
    enabled BOOLEAN DEFAULT TRUE,
    alert_channels VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_type (rule_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则配置表';

-- 13. 告警历史记录表
CREATE TABLE IF NOT EXISTS alert_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_id VARCHAR(64) UNIQUE NOT NULL,
    rule_id BIGINT,
    alert_level VARCHAR(20) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    metric_value DOUBLE,
    threshold_value DOUBLE,
    service_name VARCHAR(100),
    instance_id VARCHAR(100),
    trace_id VARCHAR(64),
    status VARCHAR(20) DEFAULT 'FIRING',
    fired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    INDEX idx_rule_id (rule_id),
    INDEX idx_alert_type (alert_type),
    INDEX idx_fired_at (fired_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警历史记录表';
