package com.basebackend.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单列表展示对象
 * <p>使用 @Data 类以兼容 MyBatis XML resultMap 属性映射</p>
 */
@Data
public class TicketListVO {

    private Long id;

    private String ticketNo;

    private String title;

    private String status;

    private Integer priority;

    private Long categoryId;

    private String categoryName;

    private Long reporterId;

    private String reporterName;

    private Long assigneeId;

    private String assigneeName;

    private Long deptId;

    private LocalDateTime slaDeadline;

    private Integer slaBreached;

    private Integer commentCount;

    private Integer attachmentCount;

    private String tags;

    private String source;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
