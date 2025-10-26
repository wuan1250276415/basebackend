/*
 * 修复Nacos 3.1.0 HealthHandler bean创建问题的补充脚本
 * 这个脚本添加了Nacos 3.1.0版本中HealthHandler所需的数据库表
 */

-- 创建健康检查处理器相关的表
CREATE TABLE IF NOT EXISTS `health_checker` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `name` varchar(100) NOT NULL COMMENT 'name',
    `type` varchar(50) NOT NULL COMMENT 'type',
    `config` text COMMENT 'config',
    `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'enabled',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='health_checker';

-- 创建健康检查结果表
CREATE TABLE IF NOT EXISTS `health_check_result` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `checker_id` bigint(20) NOT NULL COMMENT 'checker_id',
    `target` varchar(200) NOT NULL COMMENT 'target',
    `status` varchar(20) NOT NULL COMMENT 'status',
    `message` text COMMENT 'message',
    `duration` bigint(20) DEFAULT NULL COMMENT 'duration',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_checker_id` (`checker_id`),
    KEY `idx_target` (`target`),
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='health_check_result';

-- 插入默认的健康检查器配置
INSERT IGNORE INTO `health_checker` (`name`, `type`, `config`, `enabled`) VALUES 
('nacos-server', 'HTTP', '{"url":"http://localhost:8848/nacos/v1/console/health","timeout":5000}', 1),
('nacos-cluster', 'CLUSTER', '{"checkInterval":5000}', 1);

-- 创建集群节点健康状态表
CREATE TABLE IF NOT EXISTS `cluster_node_health` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `node_ip` varchar(50) NOT NULL COMMENT 'node_ip',
    `node_port` int(11) NOT NULL COMMENT 'node_port',
    `node_id` varchar(100) NOT NULL COMMENT 'node_id',
    `cluster_name` varchar(100) NOT NULL COMMENT 'cluster_name',
    `status` varchar(20) NOT NULL COMMENT 'status',
    `last_heartbeat` bigint(20) NOT NULL COMMENT 'last_heartbeat',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_id` (`node_id`),
    KEY `idx_cluster_name` (`cluster_name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='cluster_node_health';
