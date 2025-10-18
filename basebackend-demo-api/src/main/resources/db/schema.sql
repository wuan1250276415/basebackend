-- ============================
-- 数据库初始化脚本
-- ============================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS basebackend_demo
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE basebackend_demo;

-- ============================
-- 用户表
-- ============================
DROP TABLE IF EXISTS `demo_user`;
CREATE TABLE `demo_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================
-- 文章表
-- ============================
DROP TABLE IF EXISTS `demo_article`;
CREATE TABLE `demo_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT COMMENT '内容',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `author_id` BIGINT NOT NULL COMMENT '作者ID',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
  `tags` VARCHAR(200) DEFAULT NULL COMMENT '标签（逗号分隔）',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT DEFAULT 0 COMMENT '点赞次数',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已下架',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- ============================
-- 初始化测试数据
-- ============================

-- 插入测试用户
INSERT INTO `demo_user` (`username`, `password`, `nickname`, `email`, `phone`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'admin@example.com', '13800138000', 1),
('test001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户1', 'test001@example.com', '13800138001', 1),
('test002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户2', 'test002@example.com', '13800138002', 1),
('test003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户3', 'test003@example.com', '13800138003', 1),
('test004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户4', 'test004@example.com', '13800138004', 1);

-- 插入测试文章
INSERT INTO `demo_article` (`title`, `content`, `summary`, `author_id`, `category`, `tags`, `view_count`, `like_count`, `status`, `publish_time`) VALUES
('Spring Boot 快速入门指南', 'Spring Boot是一个基于Spring框架的快速开发工具...', 'Spring Boot快速入门教程，适合初学者', 1, '技术教程', 'Spring Boot,Java,后端', 150, 25, 1, NOW()),
('微服务架构设计最佳实践', '微服务架构是一种将应用程序构建为一组小型服务的方法...', '深入探讨微服务架构的设计原则和最佳实践', 1, '架构设计', '微服务,架构,分布式', 320, 68, 1, NOW()),
('Redis缓存应用场景详解', 'Redis是一个开源的内存数据结构存储系统...', '详细介绍Redis在实际项目中的应用场景', 2, '技术教程', 'Redis,缓存,性能优化', 210, 42, 1, NOW()),
('MySQL性能优化技巧', 'MySQL性能优化是数据库管理的重要内容...', 'MySQL数据库性能优化的实用技巧', 2, '数据库', 'MySQL,性能优化,索引', 185, 35, 1, NOW()),
('Docker容器化部署实战', 'Docker是一个开源的容器化平台...', 'Docker容器化部署的实战教程', 3, '运维部署', 'Docker,容器,DevOps', 275, 52, 1, NOW()),
('前端Vue3新特性解析', 'Vue3带来了许多令人兴奋的新特性...', 'Vue3框架的新特性和改进详解', 3, '前端开发', 'Vue3,前端,JavaScript', 198, 38, 1, NOW()),
('分布式事务解决方案', '分布式事务是微服务架构中的难点之一...', '探讨分布式事务的各种解决方案', 1, '架构设计', '分布式,事务,微服务', 410, 89, 1, NOW()),
('API网关设计与实现', 'API网关是微服务架构中的重要组件...', 'API网关的设计原理和实现方法', 2, '架构设计', 'API网关,微服务,网关', 156, 31, 1, NOW()),
('Kubernetes入门教程', 'Kubernetes是一个开源的容器编排平台...', 'Kubernetes容器编排入门教程', 4, '运维部署', 'Kubernetes,K8s,容器编排', 292, 58, 1, NOW()),
('Java并发编程实战', 'Java并发编程是高级开发者必备技能...', 'Java并发编程的核心概念和实战技巧', 5, '技术教程', 'Java,并发,多线程', 335, 71, 1, NOW());

-- 查看插入结果
SELECT COUNT(*) AS user_count FROM demo_user WHERE deleted = 0;
SELECT COUNT(*) AS article_count FROM demo_article WHERE deleted = 0;

-- 显示测试数据
SELECT id, username, nickname, email, phone, status, create_time FROM demo_user WHERE deleted = 0;
SELECT id, title, summary, author_id, category, view_count, like_count, status, publish_time FROM demo_article WHERE deleted = 0;
