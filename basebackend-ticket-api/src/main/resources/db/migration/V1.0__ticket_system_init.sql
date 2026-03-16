-- =============================================
-- 工单系统初始化脚本
-- V1.0__ticket_system_init.sql
-- =============================================

-- 1. 工单分类表
CREATE TABLE ticket_category (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    name            VARCHAR(100)    NOT NULL COMMENT '分类名称',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父分类ID(0=顶级)',
    icon            VARCHAR(100)    DEFAULT '' COMMENT '图标',
    sort_order      INT             DEFAULT 0 COMMENT '排序号',
    description     VARCHAR(500)    DEFAULT '' COMMENT '描述',
    sla_hours       INT             DEFAULT 24 COMMENT '默认SLA时限(小时)',
    status          TINYINT         DEFAULT 1 COMMENT '状态: 0=禁用 1=启用',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_parent (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单分类';

-- 2. 工单主表
CREATE TABLE ticket (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_no       VARCHAR(32)     NOT NULL COMMENT '工单编号(唯一)',
    title           VARCHAR(200)    NOT NULL COMMENT '工单标题',
    description     TEXT            COMMENT '工单描述(富文本)',
    category_id     BIGINT          NOT NULL COMMENT '分类ID',
    priority        TINYINT         DEFAULT 2 COMMENT '优先级: 1=紧急 2=高 3=中 4=低',
    status          VARCHAR(20)     DEFAULT 'OPEN' COMMENT '状态: OPEN/IN_PROGRESS/PENDING_APPROVAL/APPROVED/REJECTED/RESOLVED/CLOSED',
    source          VARCHAR(20)     DEFAULT 'WEB' COMMENT '来源: WEB/API/EMAIL/WECHAT',
    reporter_id     BIGINT          NOT NULL COMMENT '报告人ID',
    reporter_name   VARCHAR(50)     NOT NULL COMMENT '报告人姓名',
    assignee_id     BIGINT          COMMENT '当前处理人ID',
    assignee_name   VARCHAR(50)     COMMENT '当前处理人姓名',
    dept_id         BIGINT          COMMENT '所属部门ID',
    -- SLA 相关
    sla_deadline    DATETIME        COMMENT 'SLA截止时间',
    sla_breached    TINYINT         DEFAULT 0 COMMENT 'SLA是否超时: 0=否 1=是',
    resolved_at     DATETIME        COMMENT '解决时间',
    closed_at       DATETIME        COMMENT '关闭时间',
    -- 工作流相关
    process_instance_id VARCHAR(64) COMMENT 'Camunda流程实例ID',
    process_definition_key VARCHAR(64) COMMENT '流程定义Key',
    -- 统计
    comment_count   INT             DEFAULT 0 COMMENT '评论数',
    attachment_count INT            DEFAULT 0 COMMENT '附件数',
    -- 扩展
    tags            VARCHAR(500)    DEFAULT '' COMMENT '标签(逗号分隔)',
    extra_data      JSON            COMMENT '扩展数据(JSON)',
    -- 基础字段
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_ticket_no (ticket_no),
    INDEX idx_tenant (tenant_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_assignee (assignee_id),
    INDEX idx_reporter (reporter_id),
    INDEX idx_dept (dept_id),
    INDEX idx_sla_deadline (sla_deadline),
    INDEX idx_process_instance (process_instance_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单';

-- 3. 工单评论表
CREATE TABLE ticket_comment (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    content         TEXT            NOT NULL COMMENT '评论内容(富文本)',
    type            VARCHAR(20)     DEFAULT 'COMMENT' COMMENT '类型: COMMENT=评论 SYSTEM=系统消息 APPROVAL=审批意见',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父评论ID(0=顶级)',
    is_internal     TINYINT         DEFAULT 0 COMMENT '是否内部备注: 0=否 1=是',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    creator_name    VARCHAR(50)     DEFAULT '' COMMENT '创建人姓名',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单评论';

-- 4. 工单附件关联表
CREATE TABLE ticket_attachment (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    file_id         BIGINT          NOT NULL COMMENT '文件服务中的文件ID',
    file_name       VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    file_size       BIGINT          DEFAULT 0 COMMENT '文件大小(bytes)',
    file_type       VARCHAR(50)     DEFAULT '' COMMENT '文件MIME类型',
    file_url        VARCHAR(500)    DEFAULT '' COMMENT '文件访问URL',
    upload_by       BIGINT          DEFAULT 0 COMMENT '上传人ID',
    create_by       BIGINT          DEFAULT 0 COMMENT '创建人',
    update_by       BIGINT          DEFAULT 0 COMMENT '更新人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单附件';

-- 5. 工单状态变更日志
CREATE TABLE ticket_status_log (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    from_status     VARCHAR(20)     NOT NULL COMMENT '原状态',
    to_status       VARCHAR(20)     NOT NULL COMMENT '新状态',
    operator_id     BIGINT          NOT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(50)     NOT NULL COMMENT '操作人姓名',
    remark          VARCHAR(500)    DEFAULT '' COMMENT '变更说明',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单状态变更日志';

-- 6. 工单审批记录表
CREATE TABLE ticket_approval (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    task_id         VARCHAR(64)     NOT NULL COMMENT 'Camunda任务ID',
    task_name       VARCHAR(100)    NOT NULL COMMENT '审批节点名称',
    approver_id     BIGINT          NOT NULL COMMENT '审批人ID',
    approver_name   VARCHAR(50)     NOT NULL COMMENT '审批人姓名',
    action          VARCHAR(20)     NOT NULL COMMENT '动作: APPROVE/REJECT/RETURN/DELEGATE/COUNTERSIGN',
    opinion         VARCHAR(1000)   DEFAULT '' COMMENT '审批意见',
    delegate_to_id  BIGINT          COMMENT '转办目标人ID',
    delegate_to_name VARCHAR(50)    COMMENT '转办目标人姓名',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单审批记录';

-- 7. 工单抄送表
CREATE TABLE ticket_cc (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    ticket_id       BIGINT          NOT NULL COMMENT '工单ID',
    user_id         BIGINT          NOT NULL COMMENT '被抄送人ID',
    user_name       VARCHAR(50)     NOT NULL COMMENT '被抄送人姓名',
    is_read         TINYINT         DEFAULT 0 COMMENT '是否已读: 0=未读 1=已读',
    read_time       DATETIME        COMMENT '阅读时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
    PRIMARY KEY (id),
    INDEX idx_ticket (ticket_id),
    INDEX idx_user (user_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单抄送';
