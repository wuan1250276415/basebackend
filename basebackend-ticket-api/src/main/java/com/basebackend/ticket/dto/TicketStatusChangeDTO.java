package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotBlank;

/**
 * 工单状态变更请求
 */
public record TicketStatusChangeDTO(

        @NotBlank(message = "目标状态不能为空")
        @SafeString(maxLength = 20)
        String toStatus,

        @SafeString(maxLength = 500)
        String remark
) {
}
