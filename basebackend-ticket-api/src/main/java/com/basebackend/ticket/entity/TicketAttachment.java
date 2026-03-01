package com.basebackend.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单附件关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_attachment")
public class TicketAttachment extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("file_id")
    private Long fileId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_type")
    private String fileType;

    @TableField("file_url")
    private String fileUrl;

    @TableField("upload_by")
    private Long uploadBy;
}
