package com.basebackend.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket")
public class Ticket extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("category_id")
    private Long categoryId;

    @TableField("priority")
    private Integer priority;

    @TableField("status")
    private String status;

    @TableField("source")
    private String source;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reporter_name")
    private String reporterName;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("assignee_name")
    private String assigneeName;

    @TableField("dept_id")
    private Long deptId;

    @TableField("sla_deadline")
    private LocalDateTime slaDeadline;

    @TableField("sla_breached")
    private Integer slaBreached;

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("process_instance_id")
    private String processInstanceId;

    @TableField("process_definition_key")
    private String processDefinitionKey;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("attachment_count")
    private Integer attachmentCount;

    @TableField("tags")
    private String tags;

    @TableField("extra_data")
    private String extraData;
}
