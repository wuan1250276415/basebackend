-- 告警规则表
CREATE TABLE IF NOT EXISTS `alert_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `description` VARCHAR(500) COMMENT '规则描述',
  `metric_name` VARCHAR(100) NOT NULL COMMENT '指标名称',
  `threshold` DOUBLE NOT NULL COMMENT '阈值',
  `operator` VARCHAR(10) NOT NULL DEFAULT 'gt' COMMENT '操作符：gt,lt,eq,gte,lte',
  `duration` INT NOT NULL DEFAULT 60 COMMENT '持续时间（秒）',
  `severity` VARCHAR(20) NOT NULL DEFAULT 'warning' COMMENT '告警级别：info,warning,error,critical',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `notification_channels` VARCHAR(200) COMMENT '通知渠道：email,sms,webhook',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` VARCHAR(50) COMMENT '创建人',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_metric_name` (`metric_name`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 告警事件表
CREATE TABLE IF NOT EXISTS `alert_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_id` BIGINT NOT NULL COMMENT '规则ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `metric_name` VARCHAR(100) NOT NULL COMMENT '指标名称',
  `current_value` DOUBLE NOT NULL COMMENT '当前值',
  `threshold` DOUBLE NOT NULL COMMENT '阈值',
  `severity` VARCHAR(20) NOT NULL COMMENT '告警级别',
  `message` TEXT COMMENT '告警消息',
  `status` VARCHAR(20) NOT NULL DEFAULT 'firing' COMMENT '告警状态：firing,resolved',
  `fired_at` DATETIME NOT NULL COMMENT '触发时间',
  `resolved_at` DATETIME COMMENT '解决时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_status` (`status`),
  KEY `idx_fired_at` (`fired_at`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件表';

-- 插入示例告警规则
INSERT INTO `alert_rule` (`rule_name`, `description`, `metric_name`, `threshold`, `operator`, `duration`, `severity`, `enabled`, `notification_channels`, `created_by`)
VALUES 
('High CPU Usage', 'CPU使用率超过80%', 'system.cpu.usage', 0.8, 'gt', 300, 'warning', 1, 'email', 'system'),
('High Memory Usage', 'JVM堆内存使用率超过90%', 'jvm.memory.used', 0.9, 'gt', 300, 'critical', 1, 'email,sms', 'system'),
('High Error Rate', 'HTTP错误率超过5%', 'http.server.requests.error.rate', 0.05, 'gt', 120, 'critical', 1, 'email,webhook', 'system'),
('Slow Response Time', 'P95响应时间超过1秒', 'http.server.requests.p95', 1.0, 'gt', 300, 'warning', 1, 'email', 'system');
