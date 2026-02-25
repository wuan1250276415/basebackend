package com.basebackend.admin.dto.notification;

import java.io.Serializable;

/**
 * 通知消息 DTO（用于 RocketMQ 传输）
 *
 * @author Claude Code
 * @since 2025-11-07
 */
public record NotificationMessageDTO(
    /** 通知ID */
    Long id,
    /** 用户ID */
    Long userId,
    /** 通知标题 */
    String title,
    /** 通知内容 */
    String content,
    /** 通知类型：system-系统通知, announcement-公告通知, reminder-提醒通知 */
    String type,
    /** 通知级别：info-信息, warning-警告, error-错误, success-成功 */
    String level,
    /** 链接地址（可选） */
    String linkUrl,
    /** 额外数据（JSON格式） */
    String extraData,
    /** 创建时间 */
    String createTime
) implements Serializable {}
