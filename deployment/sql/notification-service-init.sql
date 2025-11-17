-- ========================================
-- Phase 10.7: Notification Service 数据库初始化脚本
-- 通知服务独立数据库
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `basebackend_notification` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `basebackend_notification`;

-- ========================================
-- 表结构: user_notification (用户通知表)
-- ========================================
DROP TABLE IF EXISTS `user_notification`;

CREATE TABLE `user_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
    `content` TEXT DEFAULT NULL COMMENT '通知内容',
    `type` VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '通知类型(system-系统通知 announcement-公告 reminder-提醒)',
    `level` VARCHAR(20) NOT NULL DEFAULT 'info' COMMENT '通知级别(info warning error success)',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读(0-未读 1-已读)',
    `link_url` VARCHAR(500) DEFAULT NULL COMMENT '关联链接',
    `extra_data` TEXT DEFAULT NULL COMMENT '扩展数据(JSON格式)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_level` (`level`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入示例通知数据（3 个用户，共 20 条通知）
INSERT INTO `user_notification` (`user_id`, `title`, `content`, `type`, `level`, `is_read`, `link_url`, `create_time`) VALUES
-- 用户 1 的通知（10 条：7 未读，3 已读）
(1, '欢迎加入系统', '欢迎使用 BaseBackend 系统，祝您使用愉快！', 'system', 'success', 1, '/dashboard', '2025-11-10 09:00:00'),
(1, '系统维护通知', '系统将于今晚 23:00-24:00 进行维护升级，请提前保存数据。', 'announcement', 'warning', 1, NULL, '2025-11-11 14:30:00'),
(1, '密码修改成功', '您的账户密码已成功修改，如非本人操作请及时联系管理员。', 'system', 'info', 1, '/profile/security', '2025-11-12 10:15:00'),
(1, '新功能上线', '通知中心功能已上线，您可以实时接收系统消息啦！', 'announcement', 'success', 0, '/features/notification', '2025-11-13 16:00:00'),
(1, '待办提醒', '您有 3 个待办事项即将到期，请及时处理。', 'reminder', 'warning', 0, '/tasks', '2025-11-14 08:30:00'),
(1, '账户异常登录', '检测到您的账户在异地登录，如非本人操作请立即修改密码。', 'system', 'error', 0, '/profile/security', '2025-11-14 09:45:00'),
(1, '数据报表已生成', '您订阅的周报已生成，点击查看详情。', 'reminder', 'info', 0, '/reports/weekly', '2025-11-14 10:00:00'),
(1, '好友申请', '用户 "张三" 向您发送了好友申请。', 'system', 'info', 0, '/friends/requests', '2025-11-14 11:20:00'),
(1, '评论回复', '您的评论收到了新的回复，快去查看吧！', 'reminder', 'info', 0, '/comments/123', '2025-11-14 12:00:00'),
(1, '积分到账提醒', '恭喜您获得 100 积分奖励！', 'system', 'success', 0, '/points', '2025-11-14 13:15:00'),

-- 用户 2 的通知（7 条：4 未读，3 已读）
(2, '系统升级完成', '系统维护已完成，所有功能已恢复正常。', 'announcement', 'success', 1, NULL, '2025-11-12 00:30:00'),
(2, '权限变更通知', '您的权限已更新，现在可以访问更多功能模块。', 'system', 'info', 1, '/profile/permissions', '2025-11-12 10:00:00'),
(2, '文件上传成功', '您上传的文件 "项目方案.pdf" 已成功保存。', 'system', 'success', 1, '/files/456', '2025-11-13 15:30:00'),
(2, '会议提醒', '您预约的会议将于明天上午 10:00 开始，请准时参加。', 'reminder', 'warning', 0, '/meetings/789', '2025-11-14 08:00:00'),
(2, '消息通知', '您有 5 条新消息，请查收。', 'system', 'info', 0, '/messages', '2025-11-14 09:30:00'),
(2, '审批流程', '您提交的申请已通过审批。', 'system', 'success', 0, '/approvals/101', '2025-11-14 11:00:00'),
(2, '任务分配', '管理员为您分配了新的任务，请及时处理。', 'reminder', 'warning', 0, '/tasks/202', '2025-11-14 14:00:00'),

-- 用户 3 的通知（3 条：全部未读）
(3, '账户激活成功', '您的账户已成功激活，欢迎使用！', 'system', 'success', 0, '/dashboard', '2025-11-14 08:00:00'),
(3, '订阅确认', '您已成功订阅系统通知，将及时收到重要消息。', 'system', 'info', 0, '/subscriptions', '2025-11-14 09:00:00'),
(3, '活动邀请', '您被邀请参加 "技术分享会"，点击查看详情。', 'announcement', 'info', 0, '/events/303', '2025-11-14 10:30:00');

-- ========================================
-- 数据统计查询
-- ========================================

-- 统计各类型通知数量
SELECT
    type AS '通知类型',
    COUNT(*) AS '总数',
    SUM(CASE WHEN is_read = 1 THEN 1 ELSE 0 END) AS '已读',
    SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS '未读'
FROM user_notification
GROUP BY type
ORDER BY type;

-- 统计各级别通知数量
SELECT
    level AS '通知级别',
    COUNT(*) AS '总数',
    SUM(CASE WHEN is_read = 1 THEN 1 ELSE 0 END) AS '已读',
    SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS '未读'
FROM user_notification
GROUP BY level
ORDER BY level;

-- 统计各用户通知数量
SELECT
    user_id AS '用户ID',
    COUNT(*) AS '总通知',
    SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS '未读',
    SUM(CASE WHEN is_read = 1 THEN 1 ELSE 0 END) AS '已读'
FROM user_notification
GROUP BY user_id
ORDER BY user_id;

-- 查看最近10条通知
SELECT
    id AS 'ID',
    user_id AS '用户',
    title AS '标题',
    type AS '类型',
    level AS '级别',
    CASE is_read WHEN 1 THEN '已读' ELSE '未读' END AS '状态',
    create_time AS '创建时间'
FROM user_notification
ORDER BY create_time DESC
LIMIT 10;

-- ========================================
-- 初始化完成提示
-- ========================================
SELECT '✅ Notification Service 数据库初始化完成！' AS '状态',
       '已创建 user_notification 表并插入 20 条示例通知数据（3 个用户）' AS '说明';
