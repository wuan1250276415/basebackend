package com.basebackend.api.model.ticket;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单基础DTO - 用于服务间通信
 */
@Data
public class TicketBasicDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private String processInstanceId;

    private Integer commentCount;

    private Integer attachmentCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
