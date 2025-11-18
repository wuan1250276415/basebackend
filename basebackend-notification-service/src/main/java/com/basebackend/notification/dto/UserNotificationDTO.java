package com.basebackend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知 DTO
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
@Schema(description = "用户通知")
public class UserNotificationDTO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "通知类型")
    private String type;

    @Schema(description = "通知级别")
    private String level;

    @Schema(description = "是否已读")
    private Integer isRead;

    @Schema(description = "关联链接")
    private String linkUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "阅读时间")
    private LocalDateTime readTime;
}
