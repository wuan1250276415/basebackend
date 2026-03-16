package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建工单请求
 */
public record TicketCreateDTO(

        @NotBlank(message = "工单标题不能为空")
        @Size(max = 200, message = "工单标题长度不能超过200")
        @SafeString(maxLength = 200)
        String title,

        String description,

        @NotNull(message = "工单分类不能为空")
        Long categoryId,

        Integer priority,

        @SafeString(maxLength = 20)
        String source,

        Long reporterId,

        @SafeString(maxLength = 50)
        String reporterName,

        Long assigneeId,

        @SafeString(maxLength = 50)
        String assigneeName,

        Long deptId,

        @SafeString(maxLength = 500)
        String tags,

        List<Long> attachmentIds
) {
}
