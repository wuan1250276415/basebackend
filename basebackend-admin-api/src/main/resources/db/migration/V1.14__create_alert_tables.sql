-- =============================================
-- V1.14: 告警规则与告警事件持久化表
-- =============================================

-- 告警规则表
CREATE TABLE IF NOT EXISTS sys_alert_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    rule_name       VARCHAR(100)  NOT NULL COMMENT '规则名称',
    rule_type       VARCHAR(20)   NOT NULL COMMENT '规则类型: THRESHOLD, LOG, CUSTOM',
    metric_name     VARCHAR(200)  NULL     COMMENT '指标名称（THRESHOLD 类型使用）',
    threshold_value DOUBLE        NULL     COMMENT '阈值',
    comparison_operator VARCHAR(10) NULL   COMMENT '比较运算符: >, <, >=, <=, ==',
    duration_seconds INT          DEFAULT 0 COMMENT '持续时间（秒），指标需持续多久才触发',
    severity        VARCHAR(20)   NOT NULL DEFAULT 'WARNING' COMMENT '严重程度: INFO, WARNING, ERROR, CRITICAL',
    enabled         TINYINT(1)    DEFAULT 1 COMMENT '是否启用',
    notify_channels VARCHAR(200)  NULL     COMMENT '通知渠道（逗号分隔）: email, dingtalk, wechat',
    description     VARCHAR(500)  NULL     COMMENT '规则描述',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT        NULL     COMMENT '创建人',
    update_by       BIGINT        NULL     COMMENT '更新人',
    deleted         TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除标志',
    INDEX idx_rule_name (rule_name),
    INDEX idx_severity (severity),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- 告警事件表
CREATE TABLE IF NOT EXISTS sys_alert_event (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '事件ID',
    rule_id         BIGINT        NOT NULL COMMENT '规则ID',
    rule_name       VARCHAR(100)  NOT NULL COMMENT '规则名称',
    severity        VARCHAR(20)   NOT NULL COMMENT '严重程度: INFO, WARNING, ERROR, CRITICAL',
    message         VARCHAR(1000) NULL     COMMENT '告警消息',
    trigger_value   VARCHAR(100)  NULL     COMMENT '触发值',
    threshold_value VARCHAR(100)  NULL     COMMENT '阈值',
    alert_time      DATETIME      NOT NULL COMMENT '告警时间',
    notify_channels VARCHAR(200)  NULL     COMMENT '通知渠道',
    notify_status   VARCHAR(20)   DEFAULT 'PENDING' COMMENT '通知状态: PENDING, SUCCESS, FAILED',
    status          VARCHAR(20)   DEFAULT 'TRIGGERED' COMMENT '告警状态: TRIGGERED, NOTIFIED, ACKNOWLEDGED, RESOLVED',
    acknowledged_by BIGINT        NULL     COMMENT '确认人',
    acknowledged_at DATETIME      NULL     COMMENT '确认时间',
    resolved_by     BIGINT        NULL     COMMENT '解决人',
    resolved_at     DATETIME      NULL     COMMENT '解决时间',
    metadata        JSON          NULL     COMMENT '附加元数据（JSON）',
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_rule_id (rule_id),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_alert_time (alert_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警事件表';
