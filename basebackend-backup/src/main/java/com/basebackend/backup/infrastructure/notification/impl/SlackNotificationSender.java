package com.basebackend.backup.infrastructure.notification.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.notification.BackupNotificationEvent;
import com.basebackend.backup.infrastructure.notification.BackupNotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Slack 通知发送器
 */
@Slf4j
public class SlackNotificationSender implements BackupNotificationSender {

    private final BackupProperties.Notify.Slack config;
    private final RestClient restClient = RestClient.create();

    public SlackNotificationSender(BackupProperties.Notify.Slack config) {
        this.config = config;
    }

    @Override
    public void send(BackupNotificationEvent event) {
        String webhookUrl = config.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Slack webhook URL 未配置，跳过发送");
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "channel", config.getChannel(),
                    "text", event.toSummary()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restClient.post()
                    .uri(webhookUrl)
                    .headers(h -> h.addAll(headers))
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Slack通知发送成功, channel: {}", config.getChannel());
            } else {
                log.warn("Slack通知发送异常, 状态码: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Slack通知发送失败", e);
        }
    }

    @Override
    public String getChannelType() {
        return "slack";
    }
}
