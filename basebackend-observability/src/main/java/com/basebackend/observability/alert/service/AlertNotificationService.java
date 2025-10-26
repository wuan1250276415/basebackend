package com.basebackend.observability.alert.service;

import com.basebackend.observability.alert.AlertEvent;
import com.basebackend.observability.alert.notifier.AlertNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 告警通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final List<AlertNotifier> notifiers;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    /**
     * 发送告警
     */
    public void sendAlert(AlertEvent event) {
        if (event == null) {
            return;
        }
        
        log.info("Sending alert: {} - {}", event.getRuleName(), event.getMessage());
        
        // 异步发送告警到所有通知渠道
        for (AlertNotifier notifier : notifiers) {
            executor.submit(() -> {
                try {
                    notifier.sendAlert(event);
                } catch (Exception e) {
                    log.error("Failed to send alert via {}", 
                            notifier.getClass().getSimpleName(), e);
                }
            });
        }
    }

    /**
     * 批量发送告警
     */
    public void sendAlerts(List<AlertEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        for (AlertEvent event : events) {
            sendAlert(event);
        }
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        executor.shutdown();
    }
}
