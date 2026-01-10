-- 工作流模板表
CREATE TABLE IF NOT EXISTS `scheduler_process_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_key` varchar(64) NOT NULL COMMENT '模板标识(ProcessKey)',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `category` varchar(64) DEFAULT NULL COMMENT '分类',
  `version_tag` varchar(32) DEFAULT NULL COMMENT '版本标签',
  `deployment_id` varchar(64) DEFAULT NULL COMMENT 'Camunda部署ID',
  `resource_name` varchar(255) DEFAULT NULL COMMENT 'BPMN资源名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `owner` varchar(64) DEFAULT NULL COMMENT '负责人',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态(0:草稿 1:发布 2:停用)',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_key_tenant` (`template_key`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流模板表';

-- 节点配置表
CREATE TABLE IF NOT EXISTS `scheduler_node_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_id` bigint(20) NOT NULL COMMENT '模板ID',
  `node_key` varchar(64) NOT NULL COMMENT '节点Key(ActivityId)',
  `node_name` varchar(128) NOT NULL COMMENT '节点名称',
  `node_type` varchar(32) NOT NULL COMMENT '节点类型(UserTask/ServiceTask)',
  `form_key` varchar(64) DEFAULT NULL COMMENT '挂载表单Key',
  `candidate_rule` varchar(1024) DEFAULT NULL COMMENT '候选人规则JSON',
  `timeout_strategy` varchar(512) DEFAULT NULL COMMENT '超时策略JSON',
  `buttons_config` varchar(1024) DEFAULT NULL COMMENT '按钮配置JSON',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_node` (`template_id`, `node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点配置表';

-- 业务绑定配置表
CREATE TABLE IF NOT EXISTS `scheduler_business_binding` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `process_key` varchar(64) NOT NULL COMMENT '流程标识',
  `business_type` varchar(64) NOT NULL COMMENT '业务类型',
  `business_table` varchar(64) DEFAULT NULL COMMENT '关联业务表',
  `detail_url_template` varchar(255) DEFAULT NULL COMMENT '详情页URL模板',
  `callback_service` varchar(128) DEFAULT NULL COMMENT '回调服务名(BeanName)',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `created_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_process_business` (`process_key`, `business_type`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务绑定表';
