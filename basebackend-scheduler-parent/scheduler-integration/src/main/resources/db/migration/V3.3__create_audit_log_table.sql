-- 1. Add field_permissions column to scheduler_node_config
ALTER TABLE scheduler_node_config ADD COLUMN field_permissions TEXT COMMENT '表单字段权限JSON';

-- 2. Create scheduler_audit_log table
CREATE TABLE `scheduler_audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `audit_type` varchar(50) NOT NULL COMMENT '审计类型(TASK_COMPLETE, TASK_DELEGATE, etc.)',
  `business_key` varchar(100) DEFAULT NULL COMMENT '业务Key',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `operator_id` varchar(64) NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
  `target_user_id` varchar(64) DEFAULT NULL COMMENT '目标用户ID(如被委托人)',
  `comment` varchar(1000) DEFAULT NULL COMMENT '操作备注/意见',
  `details` text DEFAULT NULL COMMENT '详细信息JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_process_instance` (`process_instance_id`),
  KEY `idx_audit_type` (`audit_type`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流审计日志表';
