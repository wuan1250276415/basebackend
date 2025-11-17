package com.basebackend.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知实体类
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
@Data
@TableName("user_notification")
public class UserNotification {

    /**
     * 通知ID
     */
    @TableId(type = IdType.AUTO)
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
     * 通知类型：system-系统通知, announcement-公告, reminder-提醒
     */
    private String type;

    /**
     * 通知级别：info, warning, error, success
     */
    private String level;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 关联链接
     */
    private String linkUrl;

    /**
     * 扩展数据（JSON格式）
     */
    private String extraData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
}
