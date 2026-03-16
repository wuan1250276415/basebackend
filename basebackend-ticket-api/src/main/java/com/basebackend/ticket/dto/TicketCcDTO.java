package com.basebackend.ticket.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 工单抄送请求
 */
public record TicketCcDTO(

        @NotNull(message = "被抄送人ID列表不能为空")
        List<Long> userIds,

        List<String> userNames
) {
}
