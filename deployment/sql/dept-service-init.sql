-- ========================================
-- BaseBackend Dept Service 数据库初始化脚本
-- ========================================
-- 功能：创建部门服务独立数据库
-- 包含：部门表及初始化数据
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_dept`
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_dept`;

-- ========================================
-- sys_dept - 系统部门表
-- ========================================
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_name` VARCHAR(30) NOT NULL COMMENT '部门名称',
    `parent_id` BIGINT(20) DEFAULT 0 COMMENT '父部门ID（0表示顶级部门）',
    `order_num` INT(4) DEFAULT 0 COMMENT '显示顺序',
    `leader` VARCHAR(20) DEFAULT NULL COMMENT '负责人',
    `phone` VARCHAR(11) DEFAULT NULL COMMENT '联系电话',
    `email` VARCHAR(50) DEFAULT NULL COMMENT '邮箱',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_dept_name` (`dept_name`),
    KEY `idx_status` (`status`),
    KEY `idx_order_num` (`order_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统部门表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入顶级部门：公司
INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `leader`, `phone`, `email`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(1, '某某科技有限公司', 0, 0, '张三', '13800138000', 'zhangsan@example.com', 1, '公司总部', 1, NOW(), 1, NOW(), 0);

-- 插入一级部门：技术部、市场部、行政部、财务部
INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `leader`, `phone`, `email`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(2, '技术部', 1, 1, '李四', '13800138001', 'lisi@example.com', 1, '负责技术研发', 1, NOW(), 1, NOW(), 0),
(3, '市场部', 1, 2, '王五', '13800138002', 'wangwu@example.com', 1, '负责市场营销', 1, NOW(), 1, NOW(), 0),
(4, '行政部', 1, 3, '赵六', '13800138003', 'zhaoliu@example.com', 1, '负责行政事务', 1, NOW(), 1, NOW(), 0),
(5, '财务部', 1, 4, '孙七', '13800138004', 'sunqi@example.com', 1, '负责财务管理', 1, NOW(), 1, NOW(), 0);

-- 插入二级部门：技术部下的子部门
INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `leader`, `phone`, `email`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(6, '研发一部', 2, 1, '周八', '13800138005', 'zhouba@example.com', 1, '前端开发团队', 1, NOW(), 1, NOW(), 0),
(7, '研发二部', 2, 2, '吴九', '13800138006', 'wujiu@example.com', 1, '后端开发团队', 1, NOW(), 1, NOW(), 0),
(8, '测试部', 2, 3, '郑十', '13800138007', 'zhengshi@example.com', 1, '质量测试团队', 1, NOW(), 1, NOW(), 0),
(9, '运维部', 2, 4, '钱一', '13800138008', 'qianyi@example.com', 1, '系统运维团队', 1, NOW(), 1, NOW(), 0);

-- 插入二级部门：市场部下的子部门
INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `leader`, `phone`, `email`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(10, '市场营销部', 3, 1, '孙二', '13800138009', 'suner@example.com', 1, '负责市场推广', 1, NOW(), 1, NOW(), 0),
(11, '销售部', 3, 2, '李三', '13800138010', 'lisan@example.com', 1, '负责产品销售', 1, NOW(), 1, NOW(), 0),
(12, '客户服务部', 3, 3, '周四', '13800138011', 'zhousi@example.com', 1, '负责客户服务', 1, NOW(), 1, NOW(), 0);

-- 插入三级部门：研发一部下的小组
INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `leader`, `phone`, `email`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(13, '前端小组一', 6, 1, '吴五', '13800138012', 'wuwu@example.com', 1, 'React 开发小组', 1, NOW(), 1, NOW(), 0),
(14, '前端小组二', 6, 2, '郑六', '13800138013', 'zhengliu@example.com', 1, 'Vue 开发小组', 1, NOW(), 1, NOW(), 0);

-- 插入三级部门：研发二部下的小组
INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `order_num`, `leader`, `phone`, `email`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES
(15, 'Java 开发组', 7, 1, '王七', '13800138014', 'wangqi@example.com', 1, 'Java 后端开发', 1, NOW(), 1, NOW(), 0),
(16, 'Python 开发组', 7, 2, '赵八', '13800138015', 'zhaoba@example.com', 1, 'Python 后端开发', 1, NOW(), 1, NOW(), 0);

-- ========================================
-- 数据统计
-- ========================================

-- 完成
SELECT '✓ Dept Service 数据库初始化完成' AS 'Status';
SELECT '✓ 已创建 1 张表：sys_dept' AS 'Tables';
SELECT '✓ 已初始化部门数据：1个公司、4个一级部门、7个二级部门、4个三级部门' AS 'Data';
SELECT CONCAT('✓ 总计 ', COUNT(*), ' 个部门') AS 'Summary' FROM sys_dept;

-- 部门层级结构预览
SELECT
    CASE
        WHEN parent_id = 0 THEN CONCAT('└─ ', dept_name, ' (顶级)')
        WHEN parent_id IN (SELECT id FROM sys_dept WHERE parent_id = 0) THEN CONCAT('  ├─ ', dept_name, ' (一级)')
        WHEN parent_id IN (SELECT id FROM sys_dept WHERE parent_id IN (SELECT id FROM sys_dept WHERE parent_id = 0)) THEN CONCAT('    ├─ ', dept_name, ' (二级)')
        ELSE CONCAT('      ├─ ', dept_name, ' (三级)')
    END AS '部门树形结构'
FROM sys_dept
ORDER BY
    CASE WHEN parent_id = 0 THEN id END,
    CASE WHEN parent_id IN (SELECT id FROM sys_dept WHERE parent_id = 0) THEN parent_id END,
    CASE WHEN parent_id IN (SELECT id FROM sys_dept WHERE parent_id IN (SELECT id FROM sys_dept WHERE parent_id = 0)) THEN parent_id END,
    order_num;
