package com.basebackend.backup.infrastructure.notification.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.notification.BackupNotificationEvent;
import com.basebackend.backup.infrastructure.notification.BackupNotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 钉钉通知发送器
 */
@Slf4j
public class DingTalkNotificationSender implements BackupNotificationSender {

    private final BackupProperties.Notify.DingTalk config;
    private final RestTemplate restTemplate = new RestTemplate();

    public DingTalkNotificationSender(BackupProperties.Notify.DingTalk config) {
        this.config = config;
    }

    @Override
    public void send(BackupNotificationEvent event) {
        String webhookUrl = config.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("钉钉 webhook URL 未配置，跳过发送");
            return;
        }

        try {
            String url = buildSignedUrl(webhookUrl);

            Map<String, Object> payload = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", event.toSummary())
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("钉钉通知发送成功");
            } else {
                log.warn("钉钉通知发送异常, 状态码: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("钉钉通知发送失败", e);
        }
    }

    @Override
    public String getChannelType() {
        return "dingtalk";
    }

    private String buildSignedUrl(String webhookUrl) throws Exception {
        String secret = config.getSecret();
        if (secret == null || secret.isBlank()) {
            return webhookUrl;
        }

        long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);

        String separator = webhookUrl.contains("?") ? "&" : "?";
        return webhookUrl + separator + "timestamp=" + timestamp + "&sign=" + sign;
    }
}
