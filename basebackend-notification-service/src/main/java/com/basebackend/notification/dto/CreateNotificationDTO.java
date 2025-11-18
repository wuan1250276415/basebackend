package com.basebackend.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建通知请求 DTO
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
@Schema(description = "创建通知请求")
public class CreateNotificationDTO {

    @Schema(description = "用户ID（为空则发送给所有用户）")
    private Long userId;

    @NotBlank(message = "通知标题不能为空")
    @Size(max = 200, message = "通知标题长度不能超过200个字符")
    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "通知类型（system, announcement, reminder）")
    private String type;

    @Schema(description = "通知级别（info, warning, error, success）")
    private String level;

    @Schema(description = "关联链接")
    private String linkUrl;
}
