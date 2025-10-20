-- 消息日志表
CREATE TABLE IF NOT EXISTS `sys_message_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
    `topic` VARCHAR(128) NOT NULL COMMENT '主题/交换机',
    `routing_key` VARCHAR(128) COMMENT '路由键',
    `tag` VARCHAR(64) COMMENT '消息标签',
    `payload` TEXT NOT NULL COMMENT '消息体(JSON格式)',
    `headers` TEXT COMMENT '消息头(JSON格式)',
    `send_time` DATETIME NOT NULL COMMENT '发送时间',
    `delay_millis` BIGINT COMMENT '延迟时间(毫秒)',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',
    `partition_key` VARCHAR(128) COMMENT '分区键(用于顺序消息)',
    `status` VARCHAR(32) NOT NULL COMMENT '状态:PENDING,SENT,DELIVERED,CONSUMING,CONSUMED,FAILED,DEAD_LETTER',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_topic` (`topic`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息日志表';

-- Webhook配置表
CREATE TABLE IF NOT EXISTS `sys_webhook_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(128) NOT NULL COMMENT 'Webhook名称',
    `url` VARCHAR(512) NOT NULL COMMENT 'Webhook URL',
    `event_types` VARCHAR(512) NOT NULL COMMENT '订阅的事件类型(多个用逗号分隔,*表示所有)',
    `secret` VARCHAR(128) COMMENT '签名密钥',
    `signature_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用签名验证:0-否,1-是',
    `method` VARCHAR(16) DEFAULT 'POST' COMMENT 'HTTP请求方法:POST,PUT',
    `headers` TEXT COMMENT '自定义请求头(JSON格式)',
    `timeout` INT DEFAULT 30 COMMENT '超时时间(秒)',
    `max_retries` INT DEFAULT 3 COMMENT '最大重试次数',
    `retry_interval` INT DEFAULT 60 COMMENT '重试间隔(秒)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用:0-否,1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT COMMENT '创建人ID',
    `update_by` BIGINT COMMENT '更新人ID',
    `remark` VARCHAR(512) COMMENT '备注',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记:0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook配置表';

-- Webhook调用日志表
CREATE TABLE IF NOT EXISTS `sys_webhook_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `webhook_id` BIGINT NOT NULL COMMENT 'Webhook配置ID',
    `event_id` VARCHAR(64) NOT NULL COMMENT '事件ID',
    `event_type` VARCHAR(128) NOT NULL COMMENT '事件类型',
    `request_url` VARCHAR(512) NOT NULL COMMENT '请求URL',
    `request_method` VARCHAR(16) NOT NULL COMMENT '请求方法',
    `request_headers` TEXT COMMENT '请求头(JSON格式)',
    `request_body` TEXT COMMENT '请求体(JSON格式)',
    `response_status` INT COMMENT '响应状态码',
    `response_body` TEXT COMMENT '响应体',
    `response_time` BIGINT COMMENT '响应时间(毫秒)',
    `success` TINYINT(1) DEFAULT 0 COMMENT '是否成功:0-失败,1-成功',
    `error_message` TEXT COMMENT '错误信息',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `call_time` DATETIME NOT NULL COMMENT '调用时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_webhook_id` (`webhook_id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_success` (`success`),
    KEY `idx_call_time` (`call_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook调用日志表';

-- 事件订阅表
CREATE TABLE IF NOT EXISTS `sys_event_subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `webhook_id` BIGINT NOT NULL COMMENT 'Webhook配置ID',
    `event_type` VARCHAR(128) NOT NULL COMMENT '事件类型',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用:0-否,1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_webhook_event` (`webhook_id`, `event_type`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件订阅表';

-- 死信表
CREATE TABLE IF NOT EXISTS `sys_dead_letter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
    `topic` VARCHAR(128) NOT NULL COMMENT '主题/交换机',
    `routing_key` VARCHAR(128) COMMENT '路由键',
    `payload` TEXT NOT NULL COMMENT '消息体(JSON格式)',
    `headers` TEXT COMMENT '消息头(JSON格式)',
    `retry_count` INT DEFAULT 0 COMMENT '重试次数',
    `error_message` TEXT COMMENT '错误信息',
    `original_queue` VARCHAR(128) COMMENT '原始队列',
    `status` VARCHAR(32) DEFAULT 'PENDING' COMMENT '状态:PENDING-待处理,REDELIVERED-已重投,DISCARDED-已丢弃',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `handled_time` DATETIME COMMENT '处理时间',
    `handled_by` BIGINT COMMENT '处理人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_topic` (`topic`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信表';

-- 消息队列监控表
CREATE TABLE IF NOT EXISTS `sys_message_queue_monitor` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `queue_name` VARCHAR(128) NOT NULL COMMENT '队列名称',
    `message_count` BIGINT DEFAULT 0 COMMENT '消息数量',
    `consumer_count` INT DEFAULT 0 COMMENT '消费者数量',
    `message_rate` DOUBLE DEFAULT 0 COMMENT '消息速率(条/秒)',
    `ack_rate` DOUBLE DEFAULT 0 COMMENT '确认速率(条/秒)',
    `state` VARCHAR(32) DEFAULT 'RUNNING' COMMENT '状态:RUNNING,IDLE,FLOW',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_queue_name` (`queue_name`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息队列监控表';
