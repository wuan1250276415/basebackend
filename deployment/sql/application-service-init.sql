-- ========================================
-- Phase 10.6: Application Service 数据库初始化脚本
-- 应用管理服务独立数据库
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_application` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_application`;

-- ========================================
-- 表结构: sys_application (应用信息表)
-- ========================================
DROP TABLE IF EXISTS `sys_application`;

CREATE TABLE `sys_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '应用ID',
    `app_name` VARCHAR(100) NOT NULL COMMENT '应用名称',
    `app_code` VARCHAR(50) NOT NULL COMMENT '应用编码(唯一标识)',
    `app_type` VARCHAR(20) NOT NULL COMMENT '应用类型(web/mobile/api)',
    `app_icon` VARCHAR(255) DEFAULT NULL COMMENT '应用图标',
    `app_url` VARCHAR(500) DEFAULT NULL COMMENT '应用访问地址',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态(0-禁用 1-启用)',
    `order_num` INT DEFAULT 0 COMMENT '显示排序',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志(0-正常 1-删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_code` (`app_code`),
    KEY `idx_app_type` (`app_type`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用信息表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入示例应用数据
INSERT INTO `sys_application` (`app_name`, `app_code`, `app_type`, `app_icon`, `app_url`, `status`, `order_num`, `remark`, `create_by`) VALUES
-- Web应用
('后台管理系统', 'admin-web', 'web', 'icon-admin', 'http://localhost:8080', 1, 1, '管理员后台管理系统', 'system'),
('用户门户', 'user-portal', 'web', 'icon-portal', 'http://localhost:8081', 1, 2, '面向终端用户的门户网站', 'system'),
('数据分析平台', 'data-analytics', 'web', 'icon-analytics', 'http://localhost:8082', 1, 3, '数据可视化和分析平台', 'system'),
('运营管理系统', 'operation-system', 'web', 'icon-operation', 'http://localhost:8083', 1, 4, '业务运营管理系统', 'system'),
('客服工作台', 'customer-service', 'web', 'icon-service', 'http://localhost:8084', 0, 5, '客户服务支持系统（暂未启用）', 'system'),

-- Mobile应用
('移动端应用', 'mobile-app', 'mobile', 'icon-mobile', 'https://app.example.com', 1, 6, 'iOS和Android移动应用', 'system'),
('企业微信小程序', 'wechat-mini', 'mobile', 'icon-wechat', 'weixin://miniprogram/example', 1, 7, '企业微信小程序应用', 'system'),
('支付宝小程序', 'alipay-mini', 'mobile', 'icon-alipay', 'alipays://miniprogram/example', 1, 8, '支付宝小程序应用', 'system'),
('钉钉应用', 'dingtalk-app', 'mobile', 'icon-dingtalk', 'dingtalk://example', 0, 9, '钉钉企业应用（开发中）', 'system'),

-- API应用
('开放平台API', 'open-api', 'api', 'icon-api', 'https://api.example.com', 1, 10, '对外开放的API服务', 'system'),
('第三方集成API', 'third-party-api', 'api', 'icon-integration', 'https://integration.example.com', 1, 11, '第三方系统集成接口', 'system'),
('数据同步服务', 'data-sync-api', 'api', 'icon-sync', 'https://sync.example.com', 1, 12, '数据同步API服务', 'system'),
('消息推送服务', 'notification-api', 'api', 'icon-notification', 'https://notify.example.com', 1, 13, '统一消息推送服务', 'system'),
('文件存储服务', 'file-storage-api', 'api', 'icon-storage', 'https://storage.example.com', 0, 14, '文件上传下载服务（维护中）', 'system'),
('支付网关API', 'payment-gateway', 'api', 'icon-payment', 'https://payment.example.com', 1, 15, '统一支付网关服务', 'system');

-- ========================================
-- 数据统计查询
-- ========================================

-- 统计各类型应用数量
SELECT
    app_type AS '应用类型',
    COUNT(*) AS '总数',
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS '启用',
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS '禁用'
FROM sys_application
WHERE deleted = 0
GROUP BY app_type
ORDER BY app_type;

-- 查看所有应用概览
SELECT
    id AS 'ID',
    app_name AS '应用名称',
    app_code AS '应用编码',
    app_type AS '类型',
    CASE status WHEN 1 THEN '启用' ELSE '禁用' END AS '状态',
    order_num AS '排序',
    create_time AS '创建时间'
FROM sys_application
WHERE deleted = 0
ORDER BY order_num ASC;

-- ========================================
-- 初始化完成提示
-- ========================================
SELECT '✅ Application Service 数据库初始化完成！' AS '状态',
       '已创建 sys_application 表并插入 15 条示例应用数据' AS '说明';
