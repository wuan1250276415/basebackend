package com.basebackend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户通知 DTO
 *
 * @param id         通知ID
 * @param title      通知标题
 * @param content    通知内容
 * @param type       通知类型
 * @param level      通知级别
 * @param isRead     是否已读
 * @param linkUrl    关联链接
 * @param createTime 创建时间
 * @param readTime   阅读时间
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Schema(description = "用户通知")
public record UserNotificationDTO(
        @Schema(description = "通知ID")
        Long id,

        @Schema(description = "通知标题")
        String title,

        @Schema(description = "通知内容")
        String content,

        @Schema(description = "通知类型")
        String type,

        @Schema(description = "通知级别")
        String level,

        @Schema(description = "是否已读")
        Integer isRead,

        @Schema(description = "关联链接")
        String linkUrl,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "阅读时间")
        LocalDateTime readTime
) {
}
