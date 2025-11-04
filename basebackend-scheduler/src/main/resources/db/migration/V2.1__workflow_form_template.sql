-- =============================================
-- 工作流表单模板表
-- =============================================

CREATE TABLE IF NOT EXISTS `workflow_form_template` (
    `id` BIGINT(20) NOT NULL COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description` VARCHAR(500) COMMENT '模板描述',
    `form_key` VARCHAR(100) NOT NULL COMMENT '表单Key（唯一标识）',
    `process_definition_key` VARCHAR(100) COMMENT '关联的流程定义Key',
    `schema_json` TEXT NOT NULL COMMENT '表单Schema JSON（JSON Schema格式）',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `version` INT(11) DEFAULT 1 COMMENT '版本号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` BIGINT(20) COMMENT '创建人ID',
    `update_by` BIGINT(20) COMMENT '更新人ID',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_form_key` (`form_key`, `deleted`),
    KEY `idx_process_def_key` (`process_definition_key`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流表单模板表';

-- 插入默认模板数据
INSERT INTO `workflow_form_template` (`id`, `name`, `description`, `form_key`, `process_definition_key`, `schema_json`, `status`, `version`, `create_by`)
VALUES
    (1, '请假申请表单', '员工请假申请表单模板', 'leave_approval_form', 'LeaveApprovalProcess',
     '{"title":"请假申请","type":"object","properties":{"leaveType":{"title":"请假类型","type":"string","enum":["annual","sick","personal","other"]},"startDate":{"title":"开始日期","type":"string","format":"date"},"endDate":{"title":"结束日期","type":"string","format":"date"},"reason":{"title":"请假事由","type":"string","minLength":10}}}',
     1, 1, 1),
    (2, '报销申请表单', '员工报销申请表单模板', 'expense_approval_form', 'ExpenseApprovalProcess',
     '{"title":"报销申请","type":"object","properties":{"expenseType":{"title":"报销类型","type":"string","enum":["travel","entertainment","office","other"]},"amount":{"title":"报销金额","type":"number","minimum":0},"items":{"title":"明细","type":"array","items":{"type":"object","properties":{"description":{"type":"string"},"amount":{"type":"number"}}}}}}',
     1, 1, 1),
    (3, '采购申请表单', '采购申请表单模板', 'purchase_approval_form', 'PurchaseApprovalProcess',
     '{"title":"采购申请","type":"object","properties":{"purchaseType":{"title":"采购类型","type":"string"},"items":{"title":"采购清单","type":"array","items":{"type":"object","properties":{"itemName":{"type":"string"},"quantity":{"type":"number"},"unitPrice":{"type":"number"}}}}}}',
     1, 1, 1);
