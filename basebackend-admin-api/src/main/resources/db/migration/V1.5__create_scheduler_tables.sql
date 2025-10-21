-- 任务调度模块数据表
-- V1.5版本

-- 1. 任务信息表
CREATE TABLE IF NOT EXISTS sys_job_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    power_job_id BIGINT COMMENT 'PowerJob任务ID',
    job_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    description VARCHAR(500) COMMENT '任务描述',
    job_type VARCHAR(20) NOT NULL COMMENT '任务类型: SCHEDULED/DELAY/WORKFLOW/IMMEDIATE',
    execute_type VARCHAR(20) NOT NULL DEFAULT 'STANDALONE' COMMENT '执行类型: STANDALONE/BROADCAST/MAP_REDUCE/SHARDING',
    time_expression_type VARCHAR(20) NOT NULL COMMENT '时间表达式类型: CRON/FIXED_RATE/FIXED_DELAY/API/WORKFLOW',
    time_expression VARCHAR(255) COMMENT '时间表达式',
    processor_type VARCHAR(255) NOT NULL COMMENT '处理器类名',
    job_params TEXT COMMENT '任务参数(JSON)',
    max_instance_num INT DEFAULT 1 COMMENT '最大实例数',
    max_retry_times INT DEFAULT 3 COMMENT '最大重试次数',
    retry_interval INT DEFAULT 60 COMMENT '重试间隔(秒)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    alert_config TEXT COMMENT '告警配置(JSON)',
    app_id BIGINT COMMENT '应用ID',
    tenant_id VARCHAR(64) COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(64) COMMENT '创建人',
    update_by VARCHAR(64) COMMENT '更新人',
    INDEX idx_job_name (job_name),
    INDEX idx_job_type (job_type),
    INDEX idx_app_id (app_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务信息表';

-- 2. 任务实例表
CREATE TABLE IF NOT EXISTS sys_job_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '实例ID',
    job_id BIGINT NOT NULL COMMENT '任务ID',
    power_job_instance_id BIGINT COMMENT 'PowerJob实例ID',
    job_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    status VARCHAR(20) NOT NULL COMMENT '状态: WAITING/RUNNING/SUCCESS/FAILED/CANCELLED/TIMEOUT/STOPPED',
    params TEXT COMMENT '执行参数',
    result TEXT COMMENT '执行结果',
    error_msg TEXT COMMENT '错误信息',
    worker_address VARCHAR(255) COMMENT '执行机器地址',
    retry_times INT DEFAULT 0 COMMENT '重试次数',
    expected_trigger_time DATETIME COMMENT '预期触发时间',
    actual_trigger_time DATETIME COMMENT '实际触发时间',
    start_time DATETIME COMMENT '开始执行时间',
    finish_time DATETIME COMMENT '完成时间',
    duration BIGINT COMMENT '执行耗时(毫秒)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_id (job_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例表';

-- 3. 工作流定义表
CREATE TABLE IF NOT EXISTS sys_workflow_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '工作流ID',
    workflow_name VARCHAR(100) NOT NULL COMMENT '工作流名称',
    description VARCHAR(500) COMMENT '工作流描述',
    workflow_json TEXT NOT NULL COMMENT '工作流定义(JSON格式)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    app_id BIGINT COMMENT '应用ID',
    tenant_id VARCHAR(64) COMMENT '租户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(64) COMMENT '创建人',
    update_by VARCHAR(64) COMMENT '更新人',
    INDEX idx_workflow_name (workflow_name),
    INDEX idx_app_id (app_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义表';

-- 4. 工作流实例表
CREATE TABLE IF NOT EXISTS sys_workflow_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '实例ID',
    workflow_id BIGINT NOT NULL COMMENT '工作流ID',
    workflow_name VARCHAR(100) NOT NULL COMMENT '工作流名称',
    status VARCHAR(20) NOT NULL COMMENT '状态: WAITING/RUNNING/SUCCESS/FAILED/CANCELLED',
    params TEXT COMMENT '执行参数',
    result TEXT COMMENT '执行结果',
    error_msg TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    finish_time DATETIME COMMENT '完成时间',
    duration BIGINT COMMENT '执行耗时(毫秒)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例表';

-- 5. 死信任务表
CREATE TABLE IF NOT EXISTS sys_job_dead_letter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '死信ID',
    job_id BIGINT NOT NULL COMMENT '任务ID',
    instance_id BIGINT COMMENT '实例ID',
    job_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    params TEXT COMMENT '任务参数',
    error_msg TEXT COMMENT '失败原因',
    retry_times INT DEFAULT 0 COMMENT '已重试次数',
    last_execute_time DATETIME COMMENT '最后执行时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    processed BOOLEAN DEFAULT FALSE COMMENT '是否已处理',
    processed_time DATETIME COMMENT '处理时间',
    processed_by VARCHAR(64) COMMENT '处理人',
    INDEX idx_job_id (job_id),
    INDEX idx_processed (processed),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='死信任务表';
