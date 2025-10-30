-- RabbitMQ to RocketMQ Migration
-- Add RocketMQ specific fields to dead letter table

-- 为 sys_dead_letter 表添加 RocketMQ 字段
ALTER TABLE sys_dead_letter
    ADD COLUMN tags VARCHAR(255) COMMENT 'RocketMQ 消息标签' AFTER routing_key,
    ADD COLUMN message_type VARCHAR(100) COMMENT '消息类型' AFTER tags;

-- 为 sys_message_log 表添加 RocketMQ 字段（如果表已存在）
ALTER TABLE sys_message_log
    ADD COLUMN mq_message_id VARCHAR(255) COMMENT 'RocketMQ 消息ID' AFTER message_id,
    ADD COLUMN error_message TEXT COMMENT '错误信息' AFTER status;

-- 更新注释
ALTER TABLE sys_dead_letter COMMENT = '死信表（支持 RocketMQ）';
ALTER TABLE sys_message_log COMMENT = '消息日志表（支持 RocketMQ）';
