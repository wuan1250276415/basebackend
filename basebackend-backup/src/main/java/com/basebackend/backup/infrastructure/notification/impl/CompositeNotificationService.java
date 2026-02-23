package com.basebackend.backup.infrastructure.notification.impl;

import com.basebackend.backup.infrastructure.notification.BackupNotificationEvent;
import com.basebackend.backup.infrastructure.notification.BackupNotificationSender;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 组合通知服务：遍历所有已启用的通知通道发送通知
 */
@Slf4j
public class CompositeNotificationService {

    private final List<BackupNotificationSender> senders;

    public CompositeNotificationService(List<BackupNotificationSender> senders) {
        this.senders = senders;
        log.info("初始化组合通知服务，已启用通道: {}",
                senders.stream().map(BackupNotificationSender::getChannelType).toList());
    }

    public void notify(BackupNotificationEvent event) {
        if (senders == null || senders.isEmpty()) {
            return;
        }

        for (BackupNotificationSender sender : senders) {
            try {
                sender.send(event);
            } catch (Exception e) {
                log.error("通知发送失败, 通道: {}", sender.getChannelType(), e);
            }
        }
    }
}
