package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotBlank;

/**
 * 添加工单评论请求
 */
public record TicketCommentDTO(

        @NotBlank(message = "评论内容不能为空")
        String content,

        @SafeString(maxLength = 20)
        String type,

        Integer isInternal,

        Long parentId
) {
}
