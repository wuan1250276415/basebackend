package com.basebackend.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单评论实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_comment")
public class TicketComment extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("content")
    private String content;

    @TableField("type")
    private String type;

    @TableField("parent_id")
    private Long parentId;

    @TableField("is_internal")
    private Integer isInternal;

    @TableField("creator_name")
    private String creatorName;
}
