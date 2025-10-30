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

    // ==================== 界面设置 ====================

    @Schema(description = "主题（light, dark, auto）")
    private String theme;

    @Schema(description = "主题色")
    private String primaryColor;

    @Schema(description = "布局（side, top）")
    private String layout;

    @Schema(description = "菜单收起状态（0-展开，1-收起）")
    private Integer menuCollapse;

    // ==================== 语言与地区 ====================

    @Schema(description = "语言（zh-CN, en-US）")
    private String language;

    @Schema(description = "时区")
    private String timezone;

    @Schema(description = "日期格式")
    private String dateFormat;

    @Schema(description = "时间格式")
    private String timeFormat;

    // ==================== 通知偏好 ====================

    @Schema(description = "是否启用邮件通知（0-否，1-是）")
    private Integer emailNotification;

    @Schema(description = "是否启用短信通知（0-否，1-是）")
    private Integer smsNotification;

    @Schema(description = "是否启用系统通知（0-否，1-是）")
    private Integer systemNotification;

    // ==================== 其他偏好 ====================

    @Schema(description = "分页大小")
    private Integer pageSize;

    @Schema(description = "仪表板布局配置（JSON格式）")
    private String dashboardLayout;

    @Schema(description = "自动保存（0-否，1-是）")
    private Integer autoSave;
}

