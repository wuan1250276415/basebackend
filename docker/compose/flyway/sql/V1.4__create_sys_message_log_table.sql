-- 为当前 compose 启动链补齐事务消息表

CREATE TABLE IF NOT EXISTS `sys_message_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `message_id` VARCHAR(64) NOT NULL,
    `mq_message_id` VARCHAR(128) DEFAULT NULL,
    `topic` VARCHAR(128) NOT NULL,
    `routing_key` VARCHAR(128) DEFAULT NULL,
    `tag` VARCHAR(128) DEFAULT NULL,
    `payload` LONGTEXT,
    `headers` LONGTEXT,
    `send_time` DATETIME DEFAULT NULL,
    `delay_millis` BIGINT NOT NULL DEFAULT 0,
    `retry_count` INT NOT NULL DEFAULT 0,
    `max_retries` INT NOT NULL DEFAULT 3,
    `partition_key` VARCHAR(128) DEFAULT NULL,
    `status` VARCHAR(32) NOT NULL,
    `error_message` VARCHAR(1000) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_message_log_message_id` (`message_id`),
    KEY `idx_sys_message_log_status` (`status`),
    KEY `idx_sys_message_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事务消息日志表';
