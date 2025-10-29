package com.basebackend.admin.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新用户偏好设置请求 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Data
@Schema(description = "更新用户偏好设置请求")
public class UpdatePreferenceDTO {

    @Schema(description = "主题（light, dark, auto）")
    private String theme;

    @Schema(description = "语言（zh-CN, en-US）")
    private String language;

    @Schema(description = "是否启用邮件通知（0-否，1-是）")
    private Integer emailNotification;

    @Schema(description = "是否启用短信通知（0-否，1-是）")
    private Integer smsNotification;

    @Schema(description = "是否启用系统通知（0-否，1-是）")
    private Integer systemNotification;
}
