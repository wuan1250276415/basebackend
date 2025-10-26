/*
 * 补充Nacos 3.1.0引入的灰度发布相关表，便于在已有数据库上执行补丁。
 */

CREATE TABLE IF NOT EXISTS `config_info_gray` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) NOT NULL COMMENT 'group_id',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `gray_name` varchar(128) NOT NULL COMMENT 'gray_name',
    `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
    `content` longtext NOT NULL COMMENT 'content',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `src_user` text COMMENT 'source user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
    `effect` varchar(64) DEFAULT NULL COMMENT 'effect',
    `type` varchar(64) DEFAULT NULL COMMENT 'type',
    `c_schema` text,
    `encrypted_data_key` text COMMENT '秘钥',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfogray_datagrouptenantgrayname` (`data_id`,`group_id`,`tenant_id`,`gray_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info_gray';
