-- =====================================================
-- BaseBackend Scheduler - 工作流表单模板表
-- 版本: 2.1
-- 描述: 创建表单模板管理相关的数据库表
-- 依赖: V2.0__camunda_workflow_init.sql
-- =====================================================

-- 表单模板表
-- 用于管理动态表单模板，与流程定义关联
CREATE TABLE IF NOT EXISTS WF_FORM_TEMPLATE (
    ID_ BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键ID',
    FORM_CODE_ VARCHAR(100) NOT NULL COMMENT '表单编码',
    FORM_NAME_ VARCHAR(200) NOT NULL COMMENT '表单名称',
    FORM_TYPE_ VARCHAR(50) DEFAULT 'default' COMMENT '表单类型',
    FORM_SCHEMA_ LONGTEXT COMMENT '表单Schema（JSON格式）',
    FORM_CONFIG_ LONGTEXT COMMENT '表单配置（JSON格式）',
    PROCESS_DEFINITION_KEY_ VARCHAR(255) COMMENT '关联的流程定义Key',
    VERSION_ INT DEFAULT 1 COMMENT '版本号',
    STATUS_ TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    DESCRIPTION_ TEXT COMMENT '描述',
    CREATE_TIME_ DATETIME(3) NOT NULL COMMENT '创建时间',
    UPDATE_TIME_ DATETIME(3) NOT NULL COMMENT '更新时间',
    CREATE_USER_ VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    UPDATE_USER_ VARCHAR(100) DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (ID_),
    UNIQUE KEY UK_FORM_TEMPLATE_CODE (FORM_CODE_),
    INDEX IDX_FORM_TEMPLATE_PROC_KEY (PROCESS_DEFINITION_KEY_),
    INDEX IDX_FORM_TEMPLATE_STATUS (STATUS_),
    INDEX IDX_FORM_TEMPLATE_CREATE_TIME (CREATE_TIME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单模板表';

-- 表单模板版本历史表
-- 记录表单模板的版本变更历史
CREATE TABLE IF NOT EXISTS WF_FORM_TEMPLATE_HISTORY (
    ID_ BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键ID',
    TEMPLATE_ID_ BIGINT NOT NULL COMMENT '表单模板ID',
    VERSION_ INT NOT NULL COMMENT '版本号',
    FORM_SCHEMA_ LONGTEXT COMMENT '表单Schema（JSON格式）',
    FORM_CONFIG_ LONGTEXT COMMENT '表单配置（JSON格式）',
    CHANGE_DESC_ VARCHAR(500) COMMENT '变更说明',
    CREATE_TIME_ DATETIME(3) NOT NULL COMMENT '创建时间',
    CREATE_USER_ VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    PRIMARY KEY (ID_),
    UNIQUE KEY UK_FORM_TEMPLATE_HISTORY (TEMPLATE_ID_, VERSION_),
    INDEX IDX_FORM_TEMPLATE_HISTORY_TIME (CREATE_TIME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单模板版本历史表';

-- 表单模板使用记录表
-- 记录表单模板在流程中的使用情况
CREATE TABLE IF NOT EXISTS WF_FORM_TEMPLATE_USAGE (
    ID_ BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键ID',
    TEMPLATE_ID_ BIGINT NOT NULL COMMENT '表单模板ID',
    PROCESS_DEFINITION_ID_ VARCHAR(64) COMMENT '流程定义ID',
    PROCESS_DEFINITION_KEY_ VARCHAR(255) COMMENT '流程定义Key',
    PROCESS_INSTANCE_ID_ VARCHAR(64) COMMENT '流程实例ID',
    TASK_ID_ VARCHAR(64) COMMENT '任务ID',
    TASK_KEY_ VARCHAR(255) COMMENT '任务Key',
    USAGE_TIME_ DATETIME(3) NOT NULL COMMENT '使用时间',
    USAGE_RESULT_ TINYINT DEFAULT 1 COMMENT '使用结果：0-失败，1-成功',
    ERROR_MSG_ TEXT COMMENT '错误信息',
    PRIMARY KEY (ID_),
    INDEX IDX_FORM_TEMPLATE_USAGE_TEMPLATE (TEMPLATE_ID_),
    INDEX IDX_FORM_TEMPLATE_USAGE_PROC (PROCESS_INSTANCE_ID_),
    INDEX IDX_FORM_TEMPLATE_USAGE_TASK (TASK_ID_),
    INDEX IDX_FORM_TEMPLATE_USAGE_TIME (USAGE_TIME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单模板使用记录表';

-- 工作流任务扩展表
-- 存储任务的扩展属性，如自定义字段、业务数据等
CREATE TABLE IF NOT EXISTS WF_TASK_EXT (
    ID_ VARCHAR(64) NOT NULL COMMENT '主键ID（使用Camunda任务ID）',
    TASK_ID_ VARCHAR(64) NOT NULL COMMENT '任务ID',
    PROC_INST_ID_ VARCHAR(64) COMMENT '流程实例ID',
    TASK_KEY_ VARCHAR(255) COMMENT '任务Key',
    EXT_DATA_ LONGTEXT COMMENT '扩展数据（JSON格式）',
    BUSINESS_DATA_ LONGTEXT COMMENT '业务数据（JSON格式）',
    ATTACHMENT_COUNT_ INT DEFAULT 0 COMMENT '附件数量',
    COMMENT_COUNT_ INT DEFAULT 0 COMMENT '评论数量',
    PRIORITY_EXT_ INT DEFAULT 0 COMMENT '扩展优先级',
    DUE_DATE_EXT_ DATETIME(3) COMMENT '扩展截止时间',
    CREATE_TIME_ DATETIME(3) NOT NULL COMMENT '创建时间',
    UPDATE_TIME_ DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (ID_),
    UNIQUE KEY UK_TASK_EXT_TASK_ID (TASK_ID_),
    INDEX IDX_TASK_EXT_PROC_INST (PROC_INST_ID_),
    INDEX IDX_TASK_EXT_TASK_KEY (TASK_KEY_),
    INDEX IDX_TASK_EXT_CREATE_TIME (CREATE_TIME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流任务扩展表';

-- 工作流实例扩展表
-- 存储流程实例的扩展属性和业务数据
CREATE TABLE IF NOT EXISTS WF_INSTANCE_EXT (
    ID_ VARCHAR(64) NOT NULL COMMENT '主键ID（使用Camunda流程实例ID）',
    PROC_INST_ID_ VARCHAR(64) NOT NULL COMMENT '流程实例ID',
    PROC_DEF_ID_ VARCHAR(64) COMMENT '流程定义ID',
    PROC_DEF_KEY_ VARCHAR(255) COMMENT '流程定义Key',
    BUSINESS_KEY_ VARCHAR(255) COMMENT '业务Key',
    BUSINESS_DATA_ LONGTEXT COMMENT '业务数据（JSON格式）',
    METADATA_ LONGTEXT COMMENT '元数据（JSON格式）',
    STATUS_ VARCHAR(50) COMMENT '扩展状态',
    START_USER_ID_ VARCHAR(255) COMMENT '发起人',
    ORG_UNIT_ID_ VARCHAR(100) COMMENT '组织单元ID',
    URGENT_LEVEL_ TINYINT DEFAULT 1 COMMENT '紧急程度：1-普通，2-重要，3-紧急',
    IS_CALLBACK_ BOOLEAN DEFAULT FALSE COMMENT '是否需要回写',
    CALLBACK_TIME_ DATETIME(3) COMMENT '回写时间',
    CALLBACK_RESULT_ TINYINT COMMENT '回写结果：0-失败，1-成功',
    CREATE_TIME_ DATETIME(3) NOT NULL COMMENT '创建时间',
    UPDATE_TIME_ DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (ID_),
    UNIQUE KEY UK_INSTANCE_EXT_PROC_INST (PROC_INST_ID_),
    INDEX IDX_INSTANCE_EXT_PROC_DEF (PROC_DEF_KEY_),
    INDEX IDX_INSTANCE_EXT_BUSINESS_KEY (BUSINESS_KEY_),
    INDEX IDX_INSTANCE_EXT_STATUS (STATUS_),
    INDEX IDX_INSTANCE_EXT_START_USER (START_USER_ID_),
    INDEX IDX_INSTANCE_EXT_CREATE_TIME (CREATE_TIME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例扩展表';

-- 工作流操作日志表
-- 记录工作流的关键操作日志
CREATE TABLE IF NOT EXISTS WF_OPERATION_LOG (
    ID_ BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键ID',
    PROC_INST_ID_ VARCHAR(64) COMMENT '流程实例ID',
    TASK_ID_ VARCHAR(64) COMMENT '任务ID',
    OPERATOR_ID_ VARCHAR(255) COMMENT '操作人ID',
    OPERATOR_NAME_ VARCHAR(255) COMMENT '操作人姓名',
    OPERATION_TYPE_ VARCHAR(50) NOT NULL COMMENT '操作类型：start/approve/reject/complete/claim/delegate',
    OPERATION_DESC_ VARCHAR(500) COMMENT '操作描述',
    BEFORE_STATUS_ VARCHAR(50) COMMENT '操作前状态',
    AFTER_STATUS_ VARCHAR(50) COMMENT '操作后状态',
    COMMENT_ TEXT COMMENT '操作意见',
    EXT_DATA_ LONGTEXT COMMENT '扩展数据（JSON格式）',
    IP_ADDRESS_ VARCHAR(50) COMMENT 'IP地址',
    USER_AGENT_ VARCHAR(500) COMMENT '用户代理',
    CREATE_TIME_ DATETIME(3) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (ID_),
    INDEX IDX_OP_LOG_PROC_INST (PROC_INST_ID_),
    INDEX IDX_OP_LOG_TASK (TASK_ID_),
    INDEX IDX_OP_LOG_OPERATOR (OPERATOR_ID_),
    INDEX IDX_OP_LOG_TYPE (OPERATION_TYPE_),
    INDEX IDX_OP_LOG_TIME (CREATE_TIME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流操作日志表';

-- 初始化默认数据

-- 插入默认管理员用户（如果不存在）
INSERT IGNORE INTO ACT_ID_USER (ID_, REV_, FIRST_, LAST_, DISPLAY_NAME_, EMAIL_, PWD_)
VALUES ('admin', 1, '系统', '管理员', '系统管理员', 'admin@basebackend.com', 'admin');

-- 插入管理员组（如果不存在）
INSERT IGNORE INTO ACT_ID_GROUP (ID_, REV_, NAME_, TYPE_)
VALUES ('admin-group', 1, '管理员组', 'ADMIN');

-- 插入管理员与组的关联（如果不存在）
INSERT IGNORE INTO ACT_ID_MEMBERSHIP (USER_ID_, GROUP_ID_)
VALUES ('admin', 'admin-group');

-- 插入默认表单模板配置数据
INSERT IGNORE INTO WF_FORM_TEMPLATE (
    ID_, FORM_CODE_, FORM_NAME_, FORM_TYPE_, FORM_SCHEMA_, FORM_CONFIG_,
    PROCESS_DEFINITION_KEY_, VERSION_, STATUS_, DESCRIPTION_,
    CREATE_TIME_, UPDATE_TIME_, CREATE_USER_, UPDATE_USER_
) VALUES (
    1,
    'DEFAULT_TASK_FORM',
    '默认任务表单',
    'standard',
    '{
        "components": [
            {
                "type": "textfield",
                "key": "taskTitle",
                "label": "任务标题",
                "input": true,
                "validate": {"required": true}
            },
            {
                "type": "textarea",
                "key": "taskDescription",
                "label": "任务描述",
                "input": true,
                "rows": 5
            },
            {
                "type": "select",
                "key": "priority",
                "label": "优先级",
                "input": true,
                "data": {
                    "values": [
                        {"label": "普通", "value": "normal"},
                        {"label": "重要", "value": "important"},
                        {"label": "紧急", "value": "urgent"}
                    ]
                }
            }
        ]
    }',
    '{
        "display": "form",
        "settings": {
            "showTitle": true,
            "titleField": "taskTitle",
            "descriptionField": "taskDescription"
        }
    }',
    NULL,
    1,
    1,
    '系统默认任务表单模板',
    NOW(),
    NOW(),
    'system',
    'system'
);

-- 创建视图：表单模板完整信息视图
CREATE OR REPLACE VIEW VW_FORM_TEMPLATE_COMPLETE AS
SELECT
    t.ID_,
    t.FORM_CODE_,
    t.FORM_NAME_,
    t.FORM_TYPE_,
    t.VERSION_,
    t.STATUS_,
    t.DESCRIPTION_,
    t.CREATE_TIME_,
    t.UPDATE_TIME_,
    t.CREATE_USER_,
    t.UPDATE_USER_,
    CASE WHEN t.PROCESS_DEFINITION_KEY_ IS NOT NULL THEN pd.NAME_ ELSE NULL END AS PROCESS_DEFINITION_NAME_,
    (SELECT COUNT(*) FROM WF_FORM_TEMPLATE_HISTORY h WHERE h.TEMPLATE_ID_ = t.ID_) AS HISTORY_COUNT_,
    (SELECT COUNT(*) FROM WF_FORM_TEMPLATE_USAGE u WHERE u.TEMPLATE_ID_ = t.ID_) AS USAGE_COUNT_
FROM WF_FORM_TEMPLATE t
LEFT JOIN ACT_RE_PROCDEF pd ON t.PROCESS_DEFINITION_KEY_ = pd.KEY_
WHERE t.STATUS_ = 1;

-- 注释：表和索引的说明
-- 1. WF_FORM_TEMPLATE: 主表单模板表，存储表单的Schema和配置
-- 2. WF_FORM_TEMPLATE_HISTORY: 版本历史表，记录Schema变更
-- 3. WF_FORM_TEMPLATE_USAGE: 使用记录表，统计表单使用情况
-- 4. WF_TASK_EXT: 任务扩展表，存储任务的业务数据和扩展属性
-- 5. WF_INSTANCE_EXT: 实例扩展表，存储流程实例的业务数据
-- 6. WF_OPERATION_LOG: 操作日志表，记录所有关键操作
-- 7. VW_FORM_TEMPLATE_COMPLETE: 视图，提供表单模板的完整信息
