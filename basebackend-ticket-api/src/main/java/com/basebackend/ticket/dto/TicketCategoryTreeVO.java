package com.basebackend.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 工单分类树形结构
 */
@Data
public class TicketCategoryTreeVO {

    private Long id;

    private String name;

    private Long parentId;

    private String icon;

    private Integer sortOrder;

    private String description;

    private Integer slaHours;

    private Integer status;

    private List<TicketCategoryTreeVO> children;
}
