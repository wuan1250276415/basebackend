package com.basebackend.ticket.dto;

import com.basebackend.common.validation.SafeString;
import jakarta.validation.constraints.NotNull;

/**
 * 关联附件请求
 */
public record TicketAttachmentAddDTO(

        @NotNull(message = "文件ID不能为空")
        Long fileId,

        @SafeString(maxLength = 255)
        String fileName,

        Long fileSize,

        @SafeString(maxLength = 50)
        String fileType,

        @SafeString(maxLength = 500)
        String fileUrl
) {
}
