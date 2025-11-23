-- ========================================
-- Phase 6.1: 数据库性能优化索引
-- 创建日期: 2025-01-01
-- 描述: 为常用查询字段添加索引，优化查询性能
-- ========================================

-- ========================================
-- 表单模板表索引优化
-- ========================================

-- 租户 ID + 模板类型联合索引（最常用查询场景）
CREATE INDEX idx_form_template_tenant_type
    ON camunda_form_template (tenant_id, form_type);

-- 租户 ID + 状态联合索引
CREATE INDEX idx_form_template_tenant_status
    ON camunda_form_template (tenant_id, status);

-- 创建时间倒序索引（分页查询）
CREATE INDEX idx_form_template_created_desc
    ON camunda_form_template (created_at DESC);

-- 名称模糊查询优化（前缀匹配）
CREATE INDEX idx_form_template_name_prefix
    ON camunda_form_template (name(191));

-- 复合索引：租户+类型+状态+创建时间（高级筛选）
CREATE INDEX idx_form_template_tenant_type_status_created
    ON camunda_form_template (tenant_id, form_type, status, created_at DESC);

-- ========================================
-- 用户操作日志表索引优化
-- ========================================

-- 流程实例 ID + 操作时间联合索引
CREATE INDEX idx_user_operation_log_instance_time
    ON camunda_user_operation_log (process_instance_id, time DESC);

-- 用户 ID + 操作时间联合索引
CREATE INDEX idx_user_operation_log_user_time
    ON camunda_user_operation_log (user_id, time DESC);

-- 操作类型 + 时间联合索引
CREATE INDEX idx_user_operation_log_operation_time
    ON camunda_user_operation_log (operation, time DESC);

-- ========================================
-- 历史活动实例表索引优化
-- ========================================

-- 流程实例 ID + 活动类型联合索引
CREATE INDEX idx_historic_activity_instance_type
    ON camunda_historic_activity_instance (process_instance_id, activity_type);

-- 流程实例 ID + 执行时间联合索引（查询活动历史）
CREATE INDEX idx_historic_activity_instance_instance_time
    ON camunda_historic_activity_instance (process_instance_id, start_time DESC);

-- 活动 ID + 开始时间联合索引
CREATE INDEX idx_historic_activity_activity_start
    ON camunda_historic_activity_instance (activity_id, start_time);

-- ========================================
-- 任务相关表索引优化
-- ========================================

-- 任务指派人 + 创建时间联合索引（查询待办任务）
CREATE INDEX idx_task_assignee_created
    ON camunda_task (assignee_, created_ DESC);

-- 任务候选用户 + 创建时间联合索引
CREATE INDEX idx_task_candidate_user_created
    ON camunda_task (candidate_user_, created_ DESC);

-- 任务候选组 + 创建时间联合索引
CREATE INDEX idx_task_candidate_group_created
    ON camunda_task (candidate_groups_, created_ DESC);

-- 流程定义键 + 状态联合索引
CREATE INDEX idx_task_proc_def_key_suspended
    ON camunda_task (proc_def_key_, suspended_);

-- 任务执行人 + 流程实例联合索引
CREATE INDEX idx_task_assignee_proc_inst
    ON camunda_task (assignee_, proc_inst_id_);

-- ========================================
-- 流程实例相关表索引优化
-- ========================================

-- 流程定义键 + 业务键联合索引
CREATE INDEX idx_proc_inst_def_key_business_key
    ON camunda_process_instance (proc_def_key_, business_key_);

-- 租户 ID + 流程状态联合索引
CREATE INDEX idx_proc_inst_tenant_state
    ON camunda_process_instance (tenant_id_, suspended_);

-- 流程启动人 + 启动时间联合索引
CREATE INDEX idx_proc_inst_starter_time
    ON camunda_process_instance (start_user_id_, start_time DESC);

-- 超级流程实例 ID + 创建时间（子流程查询）
CREATE INDEX idx_proc_inst_super_created
    ON camunda_process_instance (super_case_instance_id_, start_time DESC);

-- ========================================
-- 流程定义相关表索引优化
-- ========================================

-- 流程定义键 + 版本号联合索引（查询最新版本）
CREATE INDEX idx_proc_def_key_version
    ON camunda_process_definition (key_, version_ DESC);

-- 租户 ID + 部署时间联合索引
CREATE INDEX idx_proc_def_tenant_deployment
    ON camunda_process_definition (tenant_id_, deployment_time DESC);

-- 部署 ID + 资源名联合索引
CREATE INDEX idx_proc_def_deployment_resource
    ON camunda_process_definition (deployment_id_, resource_name_);

-- 流程定义键 + 租户 ID 联合索引
CREATE INDEX idx_proc_def_key_tenant
    ON camunda_process_definition (key_, tenant_id_);

-- ========================================
-- 变量表索引优化
-- ========================================

-- 流程实例 ID + 变量名联合索引
CREATE INDEX idx_variable_proc_inst_name
    ON camunda_variable_instance (proc_inst_id_, name_);

-- 任务 ID + 变量名联合索引（本地变量）
CREATE INDEX idx_variable_task_name
    ON camunda_variable_instance (task_id_, name_);

-- 变量类型 + 创建时间联合索引
CREATE INDEX idx_variable_type_time
    ON camunda_variable_instance (type_, created_time_ DESC);

-- ========================================
-- 部署表索引优化
-- ========================================

-- 部署时间倒序索引
CREATE INDEX idx_deployment_time_desc
    ON camunda_deployment (deployment_time DESC);

-- 部署名称 + 部署时间联合索引
CREATE INDEX idx_deployment_name_time
    ON camunda_deployment (name_, deployment_time DESC);

-- 租户 ID + 部署时间联合索引
CREATE INDEX idx_deployment_tenant_time
    ON camunda_deployment (tenant_id_, deployment_time DESC);

-- ========================================
-- 补充：覆盖查询场景的复合索引
-- ========================================

-- 表单模板高级查询场景：租户+类型+状态+关键词
-- 注意：由于包含 LIKE 查询，仍需注意前缀匹配问题
CREATE INDEX idx_form_template_complex_query
    ON camunda_form_template (tenant_id, form_type, status, name(191), created_at DESC);

-- 任务查询场景：指派人+状态+创建时间
CREATE INDEX idx_task_assignee_state_time
    ON camunda_task (assignee_, suspension_state_, created_ DESC);

-- 流程实例查询场景：租户+定义键+状态+启动时间
CREATE INDEX idx_proc_inst_tenant_def_state_time
    ON camunda_process_instance (tenant_id_, proc_def_key_, suspension_state_, start_time DESC);

-- ========================================
-- 索引创建完成提示
-- ========================================

-- 注意：
-- 1. 所有索引已针对常用查询场景优化
-- 2. 复合索引顺序按照查询频率和选择性排序
-- 3. 使用前缀索引优化 VARCHAR 类型的 LIKE 查询
-- 4. 倒序索引优化分页查询性能
-- 5. 建议定期分析索引使用情况，清理未使用的索引

-- 索引数量统计
-- 表单模板相关: 6 个
-- 用户操作日志相关: 3 个
-- 历史活动实例相关: 3 个
-- 任务相关: 5 个
-- 流程实例相关: 4 个
-- 流程定义相关: 4 个
-- 变量相关: 3 个
-- 部署相关: 3 个
-- 复合优化索引: 3 个
-- 总计: 34 个高性能索引
