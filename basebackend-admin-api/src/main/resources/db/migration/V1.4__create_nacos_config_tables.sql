-- ============================================
-- Nacos配置中心相关表
-- V1.4__create_nacos_config_tables.sql
-- ============================================

-- 1. Nacos配置表
CREATE TABLE IF NOT EXISTS sys_nacos_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    data_id VARCHAR(255) NOT NULL COMMENT '配置Data ID',
    group_name VARCHAR(128) NOT NULL DEFAULT 'DEFAULT_GROUP' COMMENT '配置分组',
    namespace VARCHAR(128) NOT NULL DEFAULT 'public' COMMENT '命名空间',
    content LONGTEXT COMMENT '配置内容',
    type VARCHAR(32) DEFAULT 'yaml' COMMENT '配置类型（yaml/properties/json/xml/text）',

    -- 多维度隔离
    environment VARCHAR(32) COMMENT '环境（dev/test/prod等）',
    tenant_id VARCHAR(64) COMMENT '租户ID',
    app_id BIGINT COMMENT '应用ID',

    -- 版本和状态
    version INT DEFAULT 1 COMMENT '配置版本号',
    status VARCHAR(32) DEFAULT 'draft' COMMENT '配置状态（draft/published/archived）',

    -- 发布控制
    is_critical TINYINT(1) DEFAULT 0 COMMENT '是否关键配置（0-否，1-是）',
    publish_type VARCHAR(32) DEFAULT 'auto' COMMENT '发布类型（auto/manual/gray）',

    -- 元数据
    description VARCHAR(500) COMMENT '配置描述',
    md5 VARCHAR(64) COMMENT '内容MD5值',

    -- 审计字段
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '删除标志（0-未删除，1-已删除）',

    INDEX idx_data_id (data_id),
    INDEX idx_group_namespace (group_name, namespace),
    INDEX idx_environment (environment),
    INDEX idx_tenant (tenant_id),
    INDEX idx_app (app_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_config (data_id, group_name, namespace, environment, tenant_id, app_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nacos配置表';

-- 2. Nacos配置历史表
CREATE TABLE IF NOT EXISTS sys_nacos_config_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '配置ID',
    data_id VARCHAR(255) NOT NULL COMMENT '配置Data ID',
    group_name VARCHAR(128) NOT NULL COMMENT '配置分组',
    namespace VARCHAR(128) NOT NULL COMMENT '命名空间',

    content LONGTEXT COMMENT '配置内容',
    version INT NOT NULL COMMENT '配置版本号',

    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型（create/update/delete/rollback/publish）',
    operator BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人姓名',
    rollback_from INT COMMENT '回滚来源版本',

    md5 VARCHAR(64) COMMENT '内容MD5值',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_config_id (config_id),
    INDEX idx_data_id (data_id),
    INDEX idx_version (version),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nacos配置历史表';

-- 3. Nacos灰度发布配置表
CREATE TABLE IF NOT EXISTS sys_nacos_gray_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '关联的配置ID',

    -- 灰度策略
    strategy_type VARCHAR(32) NOT NULL COMMENT '灰度策略类型（ip/percentage/label）',
    target_instances TEXT COMMENT '目标实例列表（IP列表，逗号分隔）',
    percentage INT COMMENT '灰度百分比（0-100）',
    labels VARCHAR(500) COMMENT '实例标签（JSON格式）',

    -- 灰度状态
    status VARCHAR(32) DEFAULT 'preparing' COMMENT '灰度状态（preparing/running/completed/rollback）',
    start_time DATETIME COMMENT '灰度开始时间',
    end_time DATETIME COMMENT '灰度结束时间',

    -- 灰度内容
    gray_content LONGTEXT COMMENT '灰度配置内容',

    -- 审计字段
    create_by BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_config_id (config_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nacos灰度发布配置表';

-- 4. Nacos服务注册表（监控用）
CREATE TABLE IF NOT EXISTS sys_nacos_service (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    service_name VARCHAR(255) NOT NULL COMMENT '服务名',
    group_name VARCHAR(128) NOT NULL DEFAULT 'DEFAULT_GROUP' COMMENT '分组名',
    namespace VARCHAR(128) NOT NULL DEFAULT 'public' COMMENT '命名空间',
    cluster_name VARCHAR(128) COMMENT '集群名',

    instance_count INT DEFAULT 0 COMMENT '实例总数',
    healthy_count INT DEFAULT 0 COMMENT '健康实例数',
    status VARCHAR(32) DEFAULT 'online' COMMENT '服务状态（online/offline）',

    metadata TEXT COMMENT '服务元数据（JSON格式）',

    -- 审计字段
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_service_name (service_name),
    INDEX idx_group_namespace (group_name, namespace),
    UNIQUE KEY uk_service (service_name, group_name, namespace)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nacos服务注册表';

-- 5. Nacos配置发布任务表
CREATE TABLE IF NOT EXISTS sys_nacos_publish_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_id BIGINT NOT NULL COMMENT '配置ID',

    publish_type VARCHAR(32) NOT NULL COMMENT '发布类型（auto/manual/gray）',
    status VARCHAR(32) DEFAULT 'pending' COMMENT '任务状态（pending/running/success/failed）',

    executor BIGINT COMMENT '执行人ID',
    executor_name VARCHAR(64) COMMENT '执行人姓名',
    target_instances TEXT COMMENT '目标实例列表',

    result TEXT COMMENT '执行结果（JSON格式）',
    error_message TEXT COMMENT '错误信息',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    start_time DATETIME COMMENT '开始时间',
    finish_time DATETIME COMMENT '完成时间',

    INDEX idx_config_id (config_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Nacos配置发布任务表';
