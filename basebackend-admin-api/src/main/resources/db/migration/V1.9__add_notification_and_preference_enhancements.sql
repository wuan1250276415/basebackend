-- V1.9: 增强偏好设置和添加通知系统
-- Author: Claude Code
-- Date: 2025-10-30

-- 1. 修改 user_preference 表，添加 dashboard_layout 字段
ALTER TABLE user_preference
ADD COLUMN dashboard_layout JSON COMMENT '仪表板布局配置（JSON格式）' AFTER page_size;

-- 2. 创建用户通知表
CREATE TABLE IF NOT EXISTS user_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type VARCHAR(50) DEFAULT 'system' COMMENT '通知类型：system-系统通知, announcement-公告, reminder-提醒',
    level VARCHAR(20) DEFAULT 'info' COMMENT '通知级别：info, warning, error, success',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    link_url VARCHAR(500) COMMENT '关联链接',
    extra_data JSON COMMENT '扩展数据（JSON格式）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_time DATETIME COMMENT '阅读时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';

-- 3. 创建通知模板表（用于邮件和系统通知模板）
CREATE TABLE IF NOT EXISTS notification_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    template_code VARCHAR(50) UNIQUE NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(20) NOT NULL COMMENT '模板类型：email-邮件, system-系统通知',
    subject VARCHAR(200) COMMENT '邮件主题（邮件模板使用）',
    content TEXT NOT NULL COMMENT '模板内容（支持变量占位符）',
    variables VARCHAR(500) COMMENT '可用变量列表（JSON数组）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知模板表';

-- 4. 插入默认通知模板
INSERT INTO notification_template (template_code, template_name, template_type, subject, content, variables) VALUES
('welcome', '欢迎邮件', 'email', '欢迎加入系统',
 '<h2>欢迎，{{username}}！</h2><p>您已成功注册，感谢您的加入。</p><p>您的账号信息：</p><ul><li>用户名：{{username}}</li><li>邮箱：{{email}}</li></ul>',
 '["username", "email"]'),
('password_changed', '密码修改通知', 'email', '您的密码已修改',
 '<h2>密码修改通知</h2><p>尊敬的 {{username}}，</p><p>您的账号密码已于 {{changeTime}} 成功修改。</p><p>如非本人操作，请立即联系管理员。</p>',
 '["username", "changeTime"]'),
('profile_updated', '资料更新通知', 'system', NULL,
 '您的个人资料已成功更新',
 '[]'),
('preference_updated', '偏好设置更新', 'system', NULL,
 '您的偏好设置已保存',
 '[]');
