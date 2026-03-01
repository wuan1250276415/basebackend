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
 * 工单抄送实体
 */
@Data
@TableName("ticket_cc")
public class TicketCc implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("is_read")
    private Integer isRead;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
