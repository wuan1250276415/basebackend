-- 领域事件存储表（MySQL）
-- 用于 JdbcEventStore 持久化领域事件

CREATE TABLE IF NOT EXISTS `domain_event` (
    `id`              VARCHAR(64)   NOT NULL COMMENT '事件ID（UUID）',
    `event_type`      VARCHAR(255)  NOT NULL COMMENT '事件类型（类名）',
    `event_data`      TEXT          NOT NULL COMMENT '事件数据（JSON）',
    `status`          VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PUBLISHED/CONSUMED/FAILED',
    `fail_reason`     TEXT          DEFAULT NULL COMMENT '失败原因',
    `source`          VARCHAR(255)  DEFAULT NULL COMMENT '事件来源',
    `retry_count`     INT           NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retries`     INT           NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `next_retry_time` DATETIME      DEFAULT NULL COMMENT '下次重试时间',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_status_retry` (`status`, `next_retry_time`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='领域事件存储表';
