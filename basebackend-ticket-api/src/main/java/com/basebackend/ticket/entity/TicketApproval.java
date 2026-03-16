package com.basebackend.ticket.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单审批记录实体
 */
@Data
@TableName("ticket_approval")
public class TicketApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("task_id")
    private String taskId;

    @TableField("task_name")
    private String taskName;

    @TableField("approver_id")
    private Long approverId;

    @TableField("approver_name")
    private String approverName;

    @TableField("action")
    private String action;

    @TableField("opinion")
    private String opinion;

    @TableField("delegate_to_id")
    private Long delegateToId;

    @TableField("delegate_to_name")
    private String delegateToName;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
