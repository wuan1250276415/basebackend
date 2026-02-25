package com.basebackend.admin.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户通知 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Schema(description = "用户通知")
public record UserNotificationDTO(
    @Schema(description = "通知ID") Long id,
    @Schema(description = "通知标题") String title,
    @Schema(description = "通知内容") String content,
    @Schema(description = "通知类型") String type,
    @Schema(description = "通知级别") String level,
    @Schema(description = "是否已读") Integer isRead,
    @Schema(description = "关联链接") String linkUrl,
    @Schema(description = "创建时间") LocalDateTime createTime,
    @Schema(description = "阅读时间") LocalDateTime readTime
) {}
