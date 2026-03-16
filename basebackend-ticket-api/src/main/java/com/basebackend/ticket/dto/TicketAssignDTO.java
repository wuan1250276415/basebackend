package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotNull;

/**
 * 工单分配处理人请求
 */
public record TicketAssignDTO(

        @NotNull(message = "处理人ID不能为空")
        Long assigneeId,

        @SafeString(maxLength = 50)
        String assigneeName
) {
}
