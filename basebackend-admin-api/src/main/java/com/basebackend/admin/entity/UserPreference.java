package com.basebackend.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户偏好设置实体类
 *
 * @author Claude Code
 * @since 2025-10-29
 */
@Data
@TableName("user_preference")
public class UserPreference {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    // ==================== 界面设置 ====================

    /**
     * 主题: light-浅色, dark-深色
     */
    private String theme;

    /**
     * 主题色
     */
    private String primaryColor;

    /**
     * 布局: side-侧边, top-顶部
     */
    private String layout;

    /**
     * 菜单收起状态: 0-展开, 1-收起
     */
    private Integer menuCollapse;

    // ==================== 语言与地区 ====================

    /**
     * 语言: zh-CN-简体中文, en-US-English
     */
    private String language;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 日期格式
     */
    private String dateFormat;

    /**
     * 时间格式
     */
    private String timeFormat;

    // ==================== 通知偏好 ====================

    /**
     * 邮件通知: 0-关闭, 1-开启
     */
    private Integer emailNotification;

    /**
     * 短信通知: 0-关闭, 1-开启
     */
    private Integer smsNotification;

    /**
     * 系统通知: 0-关闭, 1-开启
     */
    private Integer systemNotification;

    // ==================== 其他偏好 ====================

    /**
     * 分页大小
     */
    private Integer pageSize;

    /**
     * 仪表板布局配置（JSON格式）
     */
    private String dashboardLayout;

    /**
     * 自动保存: 0-关闭, 1-开启
     */
    private Integer autoSave;

    // ==================== 基础字段 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
