package com.basebackend.system.constants;

/**
 * 通知消息常量
 *
 * @author Claude Code
 * @since 2025-11-07
 */
public class NotificationConstants {

    /**
     * 通知 Topic
     */
    public static final String NOTIFICATION_TOPIC = "notification-topic";

    /**
     * 通知 Tag - 系统通知
     */
    public static final String TAG_SYSTEM = "SYSTEM";

    /**
     * 通知 Tag - 公告通知
     */
    public static final String TAG_ANNOUNCEMENT = "ANNOUNCEMENT";

    /**
     * 通知 Tag - 提醒通知
     */
    public static final String TAG_REMINDER = "REMINDER";

    /**
     * 通知 Consumer Group
     */
    public static final String NOTIFICATION_CONSUMER_GROUP = "notification-consumer-group";

    /**
     * SSE 连接超时时间（毫秒）- 5分钟
     */
    public static final long SSE_TIMEOUT = 5 * 60 * 1000;

    /**
     * SSE 心跳间隔（毫秒）- 30秒
     */
    public static final long SSE_HEARTBEAT_INTERVAL = 30 * 1000;
}
