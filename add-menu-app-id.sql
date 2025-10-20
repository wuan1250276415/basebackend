-- 为菜单管理添加应用隔离功能
-- 执行时间：请在非高峰期执行

-- 1. 为 sys_menu 表添加 app_id 字段
ALTER TABLE sys_menu
ADD COLUMN app_id BIGINT DEFAULT NULL COMMENT '应用ID（为空表示系统菜单）' AFTER id;

-- 2. 为 app_id 字段添加索引
CREATE INDEX idx_app_id ON sys_menu(app_id);

-- 3. 更新表注释
ALTER TABLE sys_menu COMMENT='系统菜单表（支持应用级隔离）';

-- 验证脚本
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'sys_menu'
AND COLUMN_NAME = 'app_id';
