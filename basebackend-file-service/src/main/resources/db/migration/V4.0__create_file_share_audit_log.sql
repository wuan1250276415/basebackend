-- 创建文件分享审计日志表
-- Version: 4.0
-- Date: 2025-11-28
-- Description: 记录所有文件分享相关的安全和管理操作

CREATE TABLE IF NOT EXISTS `sys_file_share_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '追踪ID（用于链路追踪）',
    `span_id` VARCHAR(64) DEFAULT NULL COMMENT '跨度ID（可选）',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `tenant_id` VARCHAR(64) DEFAULT NULL COMMENT '租户ID（可选）',
    `share_code` VARCHAR(64) NOT NULL COMMENT '分享码',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型（PREVIEW, DOWNLOAD, PASSWORD_FAIL等）',
    `outcome` VARCHAR(20) NOT NULL COMMENT '操作结果（SUCCESS, FAIL）',
    `error_code` VARCHAR(50) DEFAULT NULL COMMENT '错误码（失败时使用）',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息（失败时使用）',
    `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP地址',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    `referrer` VARCHAR(512) DEFAULT NULL COMMENT 'Referer',
    `rate_limit_hit` TINYINT(1) DEFAULT FALSE COMMENT '是否触发限流',
    `details` JSON DEFAULT NULL COMMENT '扩展信息（JSON格式）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享审计日志表';

-- 创建索引提高查询性能
CREATE INDEX idx_share_code_created ON sys_file_share_audit_log (share_code, created_at);
CREATE INDEX idx_user_id_created ON sys_file_share_audit_log (user_id, created_at);
CREATE INDEX idx_action_created ON sys_file_share_audit_log (action, created_at);
CREATE INDEX idx_outcome_created ON sys_file_share_audit_log (outcome, created_at);
CREATE INDEX idx_trace_id ON sys_file_share_audit_log (trace_id);

-- 分区建议（对于大数据量）
-- ALTER TABLE sys_file_share_audit_log PARTITION BY RANGE (TO_DAYS(created_at)) (
--     PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
--     PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
--     PARTITION pmax VALUES LESS THAN MAXVALUE
-- );
