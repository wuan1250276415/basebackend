package com.basebackend.admin.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建通知请求 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Schema(description = "创建通知请求")
public record CreateNotificationDTO(
    @Schema(description = "用户ID（为空则发送给所有用户）") Long userId,
    @NotBlank(message = "通知标题不能为空") @Size(max = 200, message = "通知标题长度不能超过200个字符") @Schema(description = "通知标题") String title,
    @Schema(description = "通知内容") String content,
    @Schema(description = "通知类型（system, announcement, reminder）") String type,
    @Schema(description = "通知级别（info, warning, error, success）") String level,
    @Schema(description = "关联链接") String linkUrl
) {}
