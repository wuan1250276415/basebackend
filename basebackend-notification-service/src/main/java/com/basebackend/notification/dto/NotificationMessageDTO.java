package com.basebackend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知消息 DTO（用于 RocketMQ 传输）
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型：system-系统通知, announcement-公告通知, reminder-提醒通知
     */
    private String type;

    /**
     * 通知级别：info-信息, warning-警告, error-错误, success-成功
     */
    private String level;

    /**
     * 链接地址（可选）
     */
    private String linkUrl;

    /**
     * 额外数据（JSON格式）
     */
    private String extraData;

    /**
     * 创建时间
     */
    private String createTime;
}
