-- =============================================
-- 工作流性能优化 - 数据库索引
-- =============================================

-- 为表单模板表添加性能索引
-- 注意：这些索引在V2.1版本的建表脚本中已经创建
-- 本脚本用于检查和补充索引

-- 检查并创建索引（如果不存在）
SET @exist_idx = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
    AND table_name = 'workflow_form_template'
    AND index_name = 'idx_status'
);

-- 如果索引不存在则创建
SET @sql_create_idx = IF(@exist_idx = 0,
    'ALTER TABLE `workflow_form_template` ADD INDEX `idx_status` (`status`)',
    'SELECT ''Index idx_status already exists'' AS info'
);
PREPARE stmt FROM @sql_create_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 Camunda 历史表添加性能索引（提升历史查询性能）
-- ACT_HI_PROCINST 表
CREATE INDEX IF NOT EXISTS idx_hi_procinst_business_key ON ACT_HI_PROCINST(BUSINESS_KEY_);
CREATE INDEX IF NOT EXISTS idx_hi_procinst_start_time ON ACT_HI_PROCINST(START_TIME_);
CREATE INDEX IF NOT EXISTS idx_hi_procinst_end_time ON ACT_HI_PROCINST(END_TIME_);
CREATE INDEX IF NOT EXISTS idx_hi_procinst_proc_def_key ON ACT_HI_PROCINST(PROC_DEF_KEY_);

-- ACT_HI_TASKINST 表
CREATE INDEX IF NOT EXISTS idx_hi_taskinst_proc_inst_id ON ACT_HI_TASKINST(PROC_INST_ID_);
CREATE INDEX IF NOT EXISTS idx_hi_taskinst_assignee ON ACT_HI_TASKINST(ASSIGNEE_);
CREATE INDEX IF NOT EXISTS idx_hi_taskinst_start_time ON ACT_HI_TASKINST(START_TIME_);
CREATE INDEX IF NOT EXISTS idx_hi_taskinst_end_time ON ACT_HI_TASKINST(END_TIME_);

-- ACT_HI_ACTINST 表
CREATE INDEX IF NOT EXISTS idx_hi_actinst_proc_inst_id ON ACT_HI_ACTINST(PROC_INST_ID_);
CREATE INDEX IF NOT EXISTS idx_hi_actinst_start_time ON ACT_HI_ACTINST(START_TIME_);
CREATE INDEX IF NOT EXISTS idx_hi_actinst_end_time ON ACT_HI_ACTINST(END_TIME_);

-- 性能优化说明
SELECT '工作流性能优化索引创建完成' AS info;
