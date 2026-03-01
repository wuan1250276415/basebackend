package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 工单分类创建/更新请求
 */
public record TicketCategoryDTO(

        @NotBlank(message = "分类名称不能为空")
        @Size(max = 100, message = "分类名称长度不能超过100")
        @SafeString(maxLength = 100)
        String name,

        Long parentId,

        @SafeString(maxLength = 100)
        String icon,

        Integer sortOrder,

        @SafeString(maxLength = 500)
        String description,

        Integer slaHours,

        Integer status
) {
}
