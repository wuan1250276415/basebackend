package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单查询条件
 * <p>使用 @Data 类而非 record，以兼容 MyBatis XML 中的 OGNL 表达式</p>
 */
@Data
public class TicketQueryDTO {

    @SafeString(maxLength = 20)
    private String status;

    private Integer priority;

    private Long categoryId;

    private Long assigneeId;

    private Long reporterId;

    private Long deptId;

    @SafeString(maxLength = 100)
    private String keyword;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
