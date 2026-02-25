package com.basebackend.notification.dto;

import java.io.Serializable;

/**
 * 通知消息 DTO（用于 RocketMQ 传输）
 *
 * @param id         通知ID
 * @param userId     用户ID
 * @param title      通知标题
 * @param content    通知内容
 * @param type       通知类型：system-系统通知, announcement-公告通知, reminder-提醒通知
 * @param level      通知级别：info-信息, warning-警告, error-错误, success-成功
 * @param linkUrl    链接地址（可选）
 * @param extraData  额外数据（JSON格式）
 * @param createTime 创建时间
 * @author BaseBackend Team
 * @since 2025-11-18
 */
public record NotificationMessageDTO(
        Long id,
        Long userId,
        String title,
        String content,
        String type,
        String level,
        String linkUrl,
        String extraData,
        String createTime
) implements Serializable {
}
