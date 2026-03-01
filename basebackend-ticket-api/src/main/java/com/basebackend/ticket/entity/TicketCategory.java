package com.basebackend.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.basebackend.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单分类实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_category")
public class TicketCategory extends BaseEntity {

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("name")
    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("icon")
    private String icon;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("description")
    private String description;

    @TableField("sla_hours")
    private Integer slaHours;

    @TableField("status")
    private Integer status;
}
