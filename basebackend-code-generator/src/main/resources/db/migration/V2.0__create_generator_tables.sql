-- =============================================
-- 代码生成器模块数据库表结构
-- Version: 2.0
-- Author: System
-- Date: 2025-10-24
-- =============================================

-- 1. 数据源配置表
CREATE TABLE IF NOT EXISTS gen_datasource (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    db_type VARCHAR(20) NOT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE',
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT NOT NULL COMMENT '端口',
    database_name VARCHAR(100) NOT NULL COMMENT '数据库名',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    connection_params TEXT COMMENT '连接参数JSON',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器-数据源配置表';

-- 2. 模板分组表
CREATE TABLE IF NOT EXISTS gen_template_group (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '分组名称',
    code VARCHAR(50) NOT NULL COMMENT '分组编码',
    description VARCHAR(500) COMMENT '描述',
    engine_type VARCHAR(20) NOT NULL COMMENT '模板引擎：FREEMARKER/VELOCITY/THYMELEAF',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    UNIQUE KEY uk_code (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器-模板分组表';

-- 3. 代码模板表
CREATE TABLE IF NOT EXISTS gen_template (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    group_id BIGINT NOT NULL COMMENT '分组ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    code VARCHAR(50) NOT NULL COMMENT '模板编码：entity/mapper/service/controller等',
    template_content LONGTEXT NOT NULL COMMENT '模板内容',
    output_path VARCHAR(255) COMMENT '输出路径模板',
    file_suffix VARCHAR(20) COMMENT '文件后缀：.java/.tsx/.ts等',
    description VARCHAR(500) COMMENT '描述',
    is_builtin TINYINT DEFAULT 0 COMMENT '是否内置：0-否，1-是',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    INDEX idx_group_id (group_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器-代码模板表';

-- 4. 项目配置表
CREATE TABLE IF NOT EXISTS gen_project (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    package_name VARCHAR(255) NOT NULL COMMENT '包名',
    author VARCHAR(50) COMMENT '作者',
    version VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本',
    base_path VARCHAR(500) COMMENT '基础路径',
    module_name VARCHAR(100) COMMENT '模块名',
    table_prefix VARCHAR(50) COMMENT '表前缀（生成时去除）',
    template_group_id BIGINT COMMENT '使用的模板分组',
    config_json TEXT COMMENT '其他配置JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人',
    update_by BIGINT COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器-项目配置表';

-- 5. 生成历史表
CREATE TABLE IF NOT EXISTS gen_history (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    project_id BIGINT COMMENT '项目ID',
    datasource_id BIGINT NOT NULL COMMENT '数据源ID',
    table_names TEXT NOT NULL COMMENT '生成的表名（逗号分隔）',
    template_group_id BIGINT NOT NULL COMMENT '使用的模板分组',
    generate_type VARCHAR(20) NOT NULL COMMENT '生成类型：DOWNLOAD/PREVIEW/INCREMENT',
    file_path VARCHAR(500) COMMENT '生成文件路径',
    file_count INT COMMENT '生成文件数',
    status VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS/FAILED/PARTIAL',
    error_message TEXT COMMENT '错误信息',
    generate_config TEXT COMMENT '生成配置JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by BIGINT COMMENT '创建人',
    INDEX idx_datasource (datasource_id),
    INDEX idx_project (project_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器-生成历史表';

-- 6. 生成文件明细表
CREATE TABLE IF NOT EXISTS gen_history_detail (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    history_id BIGINT NOT NULL COMMENT '历史记录ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    file_path VARCHAR(500) COMMENT '文件路径',
    file_content LONGTEXT COMMENT '文件内容',
    status VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS/FAILED',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_history_id (history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器-生成文件明细表';

-- 7. 字段类型映射表
CREATE TABLE IF NOT EXISTS gen_type_mapping (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    db_type VARCHAR(20) NOT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE',
    column_type VARCHAR(50) NOT NULL COMMENT '数据库字段类型',
    java_type VARCHAR(100) NOT NULL COMMENT 'Java类型',
    ts_type VARCHAR(50) COMMENT 'TypeScript类型',
    import_package VARCHAR(255) COMMENT 'Java导入包',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_db_column_type (db_type, column_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成器-字段类型映射表';

-- 插入默认的MySQL类型映射
INSERT INTO gen_type_mapping (id, db_type, column_type, java_type, ts_type, import_package) VALUES
(1, 'MYSQL', 'bigint', 'Long', 'number', null),
(2, 'MYSQL', 'int', 'Integer', 'number', null),
(3, 'MYSQL', 'tinyint', 'Integer', 'number', null),
(4, 'MYSQL', 'smallint', 'Integer', 'number', null),
(5, 'MYSQL', 'varchar', 'String', 'string', null),
(6, 'MYSQL', 'char', 'String', 'string', null),
(7, 'MYSQL', 'text', 'String', 'string', null),
(8, 'MYSQL', 'longtext', 'String', 'string', null),
(9, 'MYSQL', 'datetime', 'LocalDateTime', 'string', 'java.time.LocalDateTime'),
(10, 'MYSQL', 'date', 'LocalDate', 'string', 'java.time.LocalDate'),
(11, 'MYSQL', 'time', 'LocalTime', 'string', 'java.time.LocalTime'),
(12, 'MYSQL', 'timestamp', 'LocalDateTime', 'string', 'java.time.LocalDateTime'),
(13, 'MYSQL', 'decimal', 'BigDecimal', 'number', 'java.math.BigDecimal'),
(14, 'MYSQL', 'double', 'Double', 'number', null),
(15, 'MYSQL', 'float', 'Float', 'number', null),
(16, 'MYSQL', 'bit', 'Boolean', 'boolean', null),
(17, 'MYSQL', 'blob', 'byte[]', 'Uint8Array', null),
(18, 'MYSQL', 'json', 'String', 'any', null);

-- 插入默认的PostgreSQL类型映射
INSERT INTO gen_type_mapping (id, db_type, column_type, java_type, ts_type, import_package) VALUES
(101, 'POSTGRESQL', 'bigint', 'Long', 'number', null),
(102, 'POSTGRESQL', 'integer', 'Integer', 'number', null),
(103, 'POSTGRESQL', 'smallint', 'Integer', 'number', null),
(104, 'POSTGRESQL', 'varchar', 'String', 'string', null),
(105, 'POSTGRESQL', 'char', 'String', 'string', null),
(106, 'POSTGRESQL', 'text', 'String', 'string', null),
(107, 'POSTGRESQL', 'timestamp', 'LocalDateTime', 'string', 'java.time.LocalDateTime'),
(108, 'POSTGRESQL', 'date', 'LocalDate', 'string', 'java.time.LocalDate'),
(109, 'POSTGRESQL', 'time', 'LocalTime', 'string', 'java.time.LocalTime'),
(110, 'POSTGRESQL', 'numeric', 'BigDecimal', 'number', 'java.math.BigDecimal'),
(111, 'POSTGRESQL', 'boolean', 'Boolean', 'boolean', null),
(112, 'POSTGRESQL', 'bytea', 'byte[]', 'Uint8Array', null),
(113, 'POSTGRESQL', 'json', 'String', 'any', null),
(114, 'POSTGRESQL', 'jsonb', 'String', 'any', null);
