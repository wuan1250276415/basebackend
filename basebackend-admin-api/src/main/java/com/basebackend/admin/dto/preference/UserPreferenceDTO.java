package com.basebackend.admin.dto.preference;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户偏好设置 DTO
 *
 * @author Claude Code
 * @since 2025-10-30
 */
@Schema(description = "用户偏好设置")
public record UserPreferenceDTO(
    // ==================== 界面设置 ====================
    @Schema(description = "主题（light, dark, auto）") String theme,
    @Schema(description = "主题色") String primaryColor,
    @Schema(description = "布局（side, top）") String layout,
    @Schema(description = "菜单收起状态") Integer menuCollapse,

    // ==================== 语言与地区 ====================
    @Schema(description = "语言（zh-CN, en-US）") String language,
    @Schema(description = "时区") String timezone,
    @Schema(description = "日期格式") String dateFormat,
    @Schema(description = "时间格式") String timeFormat,

    // ==================== 通知偏好 ====================
    @Schema(description = "是否启用邮件通知") Integer emailNotification,
    @Schema(description = "是否启用短信通知") Integer smsNotification,
    @Schema(description = "是否启用系统通知") Integer systemNotification,

    // ==================== 其他偏好 ====================
    @Schema(description = "分页大小") Integer pageSize,
    @Schema(description = "仪表板布局配置（JSON格式）") String dashboardLayout,
    @Schema(description = "自动保存") Integer autoSave
) {}
