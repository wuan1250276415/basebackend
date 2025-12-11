-- 创建工作流实例表
CREATE TABLE `workflow_instance` (
  `id` varchar(64) NOT NULL COMMENT '实例ID',
  `definition_id` varchar(64) NOT NULL COMMENT '工作流定义ID',
  `status` varchar(32) NOT NULL COMMENT '实例状态',
  `active_nodes` text COMMENT '活跃节点集合(JSON)',
  `context` text COMMENT '上下文参数(JSON)',
  `start_time` timestamp NULL COMMENT '开始时间',
  `end_time` timestamp NULL COMMENT '结束时间',
  `error_message` text COMMENT '错误信息',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` bigint NOT NULL DEFAULT 1 COMMENT '版本号(乐观锁)',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除标志(0:未删除 1:已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_definition_id` (`definition_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例表';

-- 创建索引以优化查询性能
CREATE INDEX `idx_workflow_instance_status_create_time` ON `workflow_instance` (`status`, `create_time`);
CREATE INDEX `idx_workflow_instance_definition_status` ON `workflow_instance` (`definition_id`, `status`);

-- 插入示例数据（可选）
-- INSERT INTO `workflow_instance` (`id`, `definition_id`, `status`, `create_time`) VALUES
-- ('test-instance-001', 'test-definition-001', 'RUNNING', NOW());
