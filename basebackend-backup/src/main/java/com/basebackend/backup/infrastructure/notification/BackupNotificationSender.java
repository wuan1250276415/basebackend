package com.basebackend.backup.infrastructure.notification;

/**
 * 备份通知发送器接口
 */
public interface BackupNotificationSender {

    /**
     * 发送通知
     *
     * @param event 备份通知事件
     */
    void send(BackupNotificationEvent event);

    /**
     * 获取通道类型标识
     */
    String getChannelType();
}
