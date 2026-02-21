-- ==============================================================
-- V1.13 - 创建审计日志表 (sys_audit_log)
-- 用于数据库级审计日志持久化存储
-- ==============================================================

CREATE TABLE IF NOT EXISTS `sys_audit_log` (
    `id`              VARCHAR(64)   NOT NULL COMMENT '审计日志唯一标识 (UUID)',
    `timestamp`       DATETIME(3)   NOT NULL COMMENT '事件发生时间戳',
    `user_id`         VARCHAR(64)   DEFAULT NULL COMMENT '操作用户 ID',
    `session_id`      VARCHAR(128)  DEFAULT NULL COMMENT '用户会话 ID',
    `event_type`      VARCHAR(64)   NOT NULL COMMENT '事件类型 (枚举名称)',
    `resource`        VARCHAR(512)  DEFAULT NULL COMMENT '操作资源 (URL/文件路径/表名)',
    `result`          VARCHAR(32)   DEFAULT NULL COMMENT '操作结果 (SUCCESS/FAILURE/PENDING)',
    `client_ip`       VARCHAR(64)   DEFAULT NULL COMMENT '客户端 IP 地址',
    `user_agent`      VARCHAR(512)  DEFAULT NULL COMMENT '用户代理',
    `device_info`     VARCHAR(256)  DEFAULT NULL COMMENT '设备信息',
    `location`        VARCHAR(256)  DEFAULT NULL COMMENT '地理位置',
    `entity_id`       VARCHAR(128)  DEFAULT NULL COMMENT '关联的业务实体 ID',
    `operation`       VARCHAR(128)  DEFAULT NULL COMMENT '业务操作类型',
    `details`         TEXT          DEFAULT NULL COMMENT '详细操作信息 (JSON)',
    `duration_ms`     BIGINT        DEFAULT NULL COMMENT '操作耗时 (毫秒)',
    `error_code`      VARCHAR(64)   DEFAULT NULL COMMENT '错误码',
    `error_message`   VARCHAR(1024) DEFAULT NULL COMMENT '错误消息',
    `trace_id`        VARCHAR(64)   DEFAULT NULL COMMENT '分布式追踪 ID',
    `span_id`         VARCHAR(64)   DEFAULT NULL COMMENT 'Span ID',
    `prev_hash`       VARCHAR(128)  DEFAULT NULL COMMENT '上一条记录哈希 (哈希链)',
    `entry_hash`      VARCHAR(128)  DEFAULT NULL COMMENT '当前记录哈希',
    `signature`       TEXT          DEFAULT NULL COMMENT '数字签名',
    `certificate_id`  VARCHAR(128)  DEFAULT NULL COMMENT '签名证书 ID',
    PRIMARY KEY (`id`),
    INDEX `idx_audit_timestamp`  (`timestamp`),
    INDEX `idx_audit_user_id`    (`user_id`),
    INDEX `idx_audit_event_type` (`event_type`),
    INDEX `idx_audit_trace_id`   (`trace_id`),
    INDEX `idx_audit_entity_id`  (`entity_id`),
    INDEX `idx_audit_result`     (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
