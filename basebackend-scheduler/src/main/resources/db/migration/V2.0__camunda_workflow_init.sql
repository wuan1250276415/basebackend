-- =============================================
-- Camunda BPM 工作流引擎表初始化
-- =============================================
-- 注意：Camunda 会自动创建所需的数据库表
-- 本脚本仅用于 Flyway 版本管理和记录

-- Camunda 会创建以下类型的表：
-- 1. ACT_RE_* : Repository（流程定义和部署相关）
-- 2. ACT_RU_* : Runtime（运行时数据：流程实例、任务等）
-- 3. ACT_ID_* : Identity（用户和组）
-- 4. ACT_HI_* : History（历史数据）
-- 5. ACT_GE_* : General（通用数据：字节数组、属性等）

-- 如果需要手动创建表，请参考：
-- https://docs.camunda.org/manual/7.21/installation/database-schema/

-- 检查 Camunda 版本
-- SELECT * FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version';

-- 空迁移脚本（Camunda自动建表）
SELECT 'Camunda BPM tables will be created automatically by Camunda engine' as info;
