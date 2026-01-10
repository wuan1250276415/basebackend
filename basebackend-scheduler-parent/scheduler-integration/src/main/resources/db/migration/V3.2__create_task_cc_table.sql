-- ----------------------------
-- Table structure for scheduler_task_cc
-- ----------------------------
CREATE TABLE IF NOT EXISTS `scheduler_task_cc` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `process_instance_id` varchar(64) NOT NULL COMMENT '流程实例ID',
  `process_definition_key` varchar(255) DEFAULT NULL COMMENT '流程定义Key',
  `task_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `user_id` varchar(64) NOT NULL COMMENT '抄送给某人',
  `initiator_id` varchar(64) DEFAULT NULL COMMENT '抄送发起人',
  `status` varchar(20) NOT NULL DEFAULT 'UNREAD' COMMENT '状态: UNREAD, READ',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务抄送表';
