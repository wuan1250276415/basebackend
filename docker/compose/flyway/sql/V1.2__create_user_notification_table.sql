-- 将 notification-service 现有表结构并入 compose Flyway 主链

CREATE TABLE IF NOT EXISTS `user_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `type` VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT '通知类型: system-系统通知, announcement-公告, reminder-提醒',
    `level` VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '通知级别: info, warning, error, success',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `link_url` VARCHAR(2048) DEFAULT NULL COMMENT '关联链接',
    `extra_data` TEXT COMMENT '扩展数据(JSON格式)',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_is_read` (`user_id`, `is_read`),
    KEY `idx_user_create_time` (`user_id`, `create_time`),
    KEY `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';
