package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;

/**
 * 工单关闭请求
 */
public record TicketCloseDTO(

        @SafeString(maxLength = 500)
        String remark
) {
}
