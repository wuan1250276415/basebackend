package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新工单请求
 */
public record TicketUpdateDTO(

        @NotBlank(message = "工单标题不能为空")
        @Size(max = 200, message = "工单标题长度不能超过200")
        @SafeString(maxLength = 200)
        String title,

        String description,

        Long categoryId,

        Integer priority,

        @SafeString(maxLength = 500)
        String tags
) {
}
